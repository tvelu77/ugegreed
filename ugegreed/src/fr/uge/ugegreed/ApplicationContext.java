package fr.uge.ugegreed;

import fr.uge.ugegreed.packetsProcessors.Calculator;
import fr.uge.ugegreed.packetsProcessors.Connector;
import fr.uge.ugegreed.packetsProcessors.Disconnector;
import fr.uge.ugegreed.packetsProcessors.PacketsProcessor;
import fr.uge.ugegreed.reader.PacketReader;
import fr.uge.ugegreed.records.Packet;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.logging.Logger;

public final class ApplicationContext {
    private static final Logger logger = Logger.getLogger(Application.class.getName());
    private static final int BUFFER_SIZE = 1024;

    private final SelectionKey key;
    private final SocketChannel sc;
    private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
    private final ArrayDeque<Packet> queue = new ArrayDeque<>();
    private final PacketReader packetReader = new PacketReader();
    private final Application application;
    private PacketsProcessor.State state = PacketsProcessor.State.CONNECTING;

    ApplicationContext(Application application, SelectionKey key) {
        this.application = application;
        this.key = key;
        this.sc = (SocketChannel) key.channel();
    }

    /**
     * Process the content of bufferIn.<br>
     *
     * The convention is that bufferIn is in write-mode before the call to process
     * and after the call
     *
     */
    private void processIn(RouteTable routeTable, PacketsProcessor.PacketsHandler packetsHandler) throws IOException {
        for (;;) {
            var status = packetReader.process(bufferIn);
            switch (status) {
                case DONE -> {
                    var value = packetReader.get();
                    logger.info(value.toString());
                    switch (value) {
                        case Packet.Connection connection -> connectProcessIn(connection, routeTable, packetsHandler.connector());
                        case Packet.Disconnection disconnection -> disconnectProcessIn(disconnection, routeTable, packetsHandler.disconnector());
                        case Packet.Work workPacket -> workProcessIn(workPacket, routeTable, packetsHandler.calculator());
                        default -> throw new IllegalStateException("unknown packet !");
                    }
                    packetReader.reset();
                }
                case REFILL -> {
                    return;
                }
                case ERROR -> {
                    System.out.println("DECODING ERROR");
                    packetReader.reset();
                    return;
                }
            }
        }
    }

    private void connectProcessIn(Packet.Connection connectPacket, RouteTable routeTable, Connector connector) {
        state = connector.process(this, connectPacket, routeTable, state);
        if(state == PacketsProcessor.State.CONNEXION_REFUSED) {
            System.out.println("NOT CONNECTED");
        }
    }

    private void disconnectProcessIn(Packet.Disconnection disconnectionPacket, RouteTable routeTable, Disconnector disconnector) throws IOException {
        state = disconnector.process(this, disconnectionPacket, routeTable, state);
        switch (state) {
            case DISCONNECTION_DENIED -> System.out.println("Disconnection denied. You may retry later");
            case RECONNECTING -> {
                if(disconnectionPacket instanceof Packet.Disconnection.PleaseReconnect pleaseReconnect) {
                    var oldMother = routeTable.getMother();
                    var newMother = pleaseReconnect.idMother();
                    application.connectToNewMother(newMother, routeTable.getDaughters());
                    routeTable.remove(oldMother);
                    routeTable.changeMother(newMother);
                } else {
                    throw new IllegalStateException();
                }
            }
            case CLOSED -> silentlyClose();
        }
    }

    private void workProcessIn(Packet.Work workPacket, RouteTable routeTable, Calculator calculator) {
        state = calculator.process(this, workPacket, routeTable, state);
        // TODO gérer répartition des charges en fonction de state
    }

    /**
     * Try to fill bufferOut from the message queue
     *
     */
    private void processOut() {
        while (!queue.isEmpty()) {
            var currentPacket = queue.peekFirst();
            // TODO a changer par un Writter.process() !!!!!!!!
            if (!currentPacket.encode(bufferOut)) {
                break;
            }

            queue.poll();
        }
    }

    /**
     * Update the interestOps of the key looking only at values of the boolean
     * closed and of both ByteBuffers.<br>
     *
     * The convention is that both buffers are in write-mode before the call to
     * updateInterestOps and after the call. Also, it is assumed that process has
     * been called just before updateInterestOps.
     */
    private void updateInterestOps() {
        var newInterestOps = 0;
        if (state != PacketsProcessor.State.CLOSED && bufferIn.hasRemaining()) {
            newInterestOps |= SelectionKey.OP_READ;
        }
        if (bufferOut.position() != 0) {
            newInterestOps |= SelectionKey.OP_WRITE;
        } else if (state == PacketsProcessor.State.CLOSED) {
            silentlyClose();
            return;
        }

        key.interestOps(newInterestOps);
    }

    private void silentlyClose() {
        try {
            sc.close();
            logger.info("Disconnected");
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // ignore exception
        }
    }

    public void queueMessage(Packet msg) {
        queue.add(msg);
        processOut();
        updateInterestOps();
    }

    void doConnect() throws IOException {
        if (!sc.finishConnect()) {
            logger.warning("The selector lied");
            return;
        }

        updateInterestOps();
    }

    /**
     * Performs the read action on sc.<br>
     *
     * The convention is that both buffers are in write-mode before the call to
     * doRead and after the call
     *
     * @throws IOException If the connection had a problem during reading.
     */
    void doRead(RouteTable routeTable, PacketsProcessor.PacketsHandler packetsHandler) throws IOException {
        if (sc.read(bufferIn) == -1) {
            logger.info("Connection is closed");
            key.cancel();
            return;
        }
        logger.info("Received something");
        processIn(routeTable, packetsHandler);
        updateInterestOps();
    }

    /**
     * Performs the write action on sc.<br>
     *
     * The convention is that both buffers are in write-mode before the call to
     * doWrite and after the call
     *
     * @throws IOException If the connection had a problem during writing.
     */
    void doWrite() throws IOException {
        var remaining = bufferOut.remaining();
        bufferOut.flip();
        try {
            sc.write(bufferOut);
            if (remaining == bufferOut.remaining()) {
                logger.warning("The selector has lied");
            }
        } finally {
            bufferOut.compact();
            processOut();
            updateInterestOps();
        }

        logger.info("sending packet to : " + sc.getRemoteAddress());
    }

    void enqueue(Packet packet) {
        queue.addLast(packet);
    }

    Packet peek() {
        return queue.peekFirst();
    }

    void nextPacket() {
        queue.poll();
    }

    PacketsProcessor.State currentState() {
        return state;
    }
}
