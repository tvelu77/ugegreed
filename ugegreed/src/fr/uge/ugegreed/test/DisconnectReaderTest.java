package fr.uge.ugegreed.test;

import fr.uge.ugegreed.reader.DisconnectReader;
import fr.uge.ugegreed.reader.Reader;
import fr.uge.ugegreed.records.Id;
import fr.uge.ugegreed.records.Packet;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DisconnectReaderTest {
    @Test
    void disconnectionDenied() {
        var expected = new Packet.Disconnection.DisconnectionDenied();
        var buffer = ByteBuffer.allocateDirect(1024);
        buffer.put((byte) 11);
        var reader = new DisconnectReader();
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(expected, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    void disconnectionGranted() {
        var expected = new Packet.Disconnection.DisconnectionGranted();
        var buffer = ByteBuffer.allocateDirect(1024);
        buffer.put((byte) 12);
        var reader = new DisconnectReader();
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(expected, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    void disconnectionRequest() {
        var port1 = 777;
        var port2 = 888;
        var id1 = new Id(new InetSocketAddress("10.100.1.1", port1));
        var id2 = new Id(new InetSocketAddress("10.20.42.10", port2));
        var expected = new Packet.Disconnection.DisconnectionRequest(List.of(id1, id2));
        var address1 = new byte[] { 10, 100, 1, 1 };
        var address2 = new byte[] { 10, 20, 42, 10 };
        var buffer = ByteBuffer.allocateDirect(1024);
        buffer.put((byte) 10);
        buffer.putInt(2);
        buffer.put((byte) 4);
        for (var oneByte : address1) {
            buffer.put(oneByte);
        }

        buffer.putInt(port1);

        buffer.put((byte) 4);
        for (var oneByte : address2) {
            buffer.put(oneByte);
        }

        buffer.putInt(port2);

        var reader = new DisconnectReader();
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(expected, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    void pleaseReconnect() {
        var port = 666;
        var id = new Id(new InetSocketAddress("127.0.0.1", port));
        var expected = new Packet.Disconnection.PleaseReconnect(id);
        var address = new byte[] { 127, 0, 0, 1 };
        var buffer = ByteBuffer.allocateDirect(1024);
        buffer.put((byte) 13);
        buffer.put((byte) 4);
        for (var oneByte : address) {
            buffer.put(oneByte);
        }

        buffer.putInt(port);

        var reader = new DisconnectReader();
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(expected, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    void reconnect() {
        var port = 666;
        var port1 = 777;
        var port2 = 888;
        var id = new Id(new InetSocketAddress("127.0.0.1", port));
        var id1 = new Id(new InetSocketAddress("10.100.1.1", port1));
        var id2 = new Id(new InetSocketAddress("10.20.42.10", port2));
        var expected = new Packet.Disconnection.Reconnect(id, List.of(id1, id2));
        var address = new byte[] { 127, 0, 0, 1 };
        var address1 = new byte[] { 10, 100, 1, 1 };
        var address2 = new byte[] { 10, 20, 42, 10 };
        var buffer = ByteBuffer.allocateDirect(1024);
        buffer.put((byte) 14);
        buffer.put((byte) 4);
        for (var oneByte : address) {
            buffer.put(oneByte);
        }

        buffer.putInt(port);
        buffer.putInt(2);
        buffer.put((byte) 4);
        for (var oneByte : address1) {
            buffer.put(oneByte);
        }

        buffer.putInt(port1);

        buffer.put((byte) 4);
        for (var oneByte : address2) {
            buffer.put(oneByte);
        }

        buffer.putInt(port2);

        var reader = new DisconnectReader();
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(expected, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    void disconnected() {
        var port1 = 777;
        var port2 = 888;
        var id1 = new Id(new InetSocketAddress("10.100.1.1", port1));
        var id2 = new Id(new InetSocketAddress("10.20.42.10", port2));
        var address1 = new byte[] { 10, 100, 1, 1 };
        var address2 = new byte[] { 10, 20, 42, 10 };
        var buffer = ByteBuffer.allocateDirect(1024);
        buffer.put((byte) 15);
        buffer.put((byte) 4);
        for (var oneByte : address1) {
            buffer.put(oneByte);
        }

        buffer.putInt(port1);

        buffer.put((byte) 4);
        for (var oneByte : address2) {
            buffer.put(oneByte);
        }

        buffer.putInt(port2);

        var reader = new DisconnectReader();
        var expected = new Packet.Disconnection.Disconnected(id1, id2);
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(expected, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    void reset() {
        var port1_1 = 666;
        var port1_2 = 777;
        var id1_1 = new Id(new InetSocketAddress("127.0.0.1", port1_1));
        var id1_2 = new Id(new InetSocketAddress("10.100.1.1", port1_2));
        var port2_1 = 888;
        var port2_2 = 999;
        var id2_1 = new Id(new InetSocketAddress("10.20.42.10", port2_1));
        var id2_2 = new Id(new InetSocketAddress("12.24.0.78", port2_2));
        var address1_1 = new byte[] { 127, 0, 0, 1 };
        var address1_2 = new byte[] { 10, 100, 1, 1 };
        var address2_1 = new byte[] { 10, 20, 42, 10 };
        var address2_2 = new byte[] { 12, 24, 0, 78 };
        var buffer = ByteBuffer.allocateDirect(1024);
        buffer.put((byte) 15);
        buffer.put((byte) 4);
        for (var oneByte : address1_1) {
            buffer.put(oneByte);
        }

        buffer.putInt(port1_1);

        buffer.put((byte) 4);
        for (var oneByte : address1_2) {
            buffer.put(oneByte);
        }

        buffer.putInt(port1_2);


        buffer.put((byte) 15);
        buffer.put((byte) 4);
        for (var oneByte : address2_1) {
            buffer.put(oneByte);
        }

        buffer.putInt(port2_1);

        buffer.put((byte) 4);
        for (var oneByte : address2_2) {
            buffer.put(oneByte);
        }

        buffer.putInt(port2_2);

        var reader = new DisconnectReader();
        var expected1 = new Packet.Disconnection.Disconnected(id1_1, id1_2);
        var expected2 = new Packet.Disconnection.Disconnected(id2_1, id2_2);
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(expected1, reader.get());
        assertEquals(buffer.capacity(), buffer.limit());
        reader.reset();
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(expected2, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    void resetReconnect() {
        var portM1 = 666;
        var portM2 = 777;
        var port1 = 888;
        var port2 = 888;
        var idMother1 = new Id(new InetSocketAddress("127.0.0.1", portM1));
        var idMother2 = new Id(new InetSocketAddress("127.0.42.12", portM2));
        var id1 = new Id(new InetSocketAddress("10.100.1.1", port1));
        var id2 = new Id(new InetSocketAddress("10.20.42.10", port2));
        var addressM1 = new byte[] { 127, 0, 0, 1 };
        var addressM2 = new byte[] { 127, 0, 42, 12 };
        var address1 = new byte[] { 10, 100, 1, 1 };
        var address2 = new byte[] { 10, 20, 42, 10 };
        var buffer = ByteBuffer.allocateDirect(1024);
        buffer.put((byte) 14);
        buffer.put((byte) 4);
        for (var oneByte : addressM1) {
            buffer.put(oneByte);
        }

        buffer.putInt(portM1);
        buffer.putInt(1);
        buffer.put((byte) 4);
        for (var oneByte : address1) {
            buffer.put(oneByte);
        }

        buffer.putInt(port1);

        buffer.put((byte) 14);
        buffer.put((byte) 4);
        for (var oneByte : addressM2) {
            buffer.put(oneByte);
        }

        buffer.putInt(portM2);
        buffer.putInt(1);
        buffer.put((byte) 4);
        for (var oneByte : address2) {
            buffer.put(oneByte);
        }

        buffer.putInt(port2);

        var reader = new DisconnectReader();
        var expected1 = new Packet.Disconnection.Reconnect(idMother1, List.of(id1));
        var expected2 = new Packet.Disconnection.Reconnect(idMother2, List.of(id2));
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(expected1, reader.get());
        assertEquals(buffer.capacity(), buffer.limit());
        reader.reset();
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(expected2, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    public void smallBuffer() {
        var port1 = 777;
        var port2 = 888;
        var id1 = new Id(new InetSocketAddress("10.100.1.1", port1));
        var id2 = new Id(new InetSocketAddress("10.20.42.10", port2));
        var address1 = new byte[] { 10, 100, 1, 1 };
        var address2 = new byte[] { 10, 20, 42, 10 };
        var buffer = ByteBuffer.allocateDirect(1024);
        buffer.put((byte) 15);
        buffer.put((byte) 4);
        for (var oneByte : address1) {
            buffer.put(oneByte);
        }

        buffer.putInt(port1);

        buffer.put((byte) 4);
        for (var oneByte : address2) {
            buffer.put(oneByte);
        }

        buffer.putInt(port2);
        buffer.flip();
        var bbSmall = ByteBuffer.allocate(2);
        var reader = new DisconnectReader();
        while (buffer.hasRemaining()) {
            while (buffer.hasRemaining() && bbSmall.hasRemaining()) {
                bbSmall.put(buffer.get());
            }
            if (buffer.hasRemaining()) {
                assertEquals(Reader.ProcessStatus.REFILL, reader.process(bbSmall));
            } else {
                assertEquals(Reader.ProcessStatus.DONE, reader.process(bbSmall));
            }
        }

        var expected = new Packet.Disconnection.Disconnected(id1, id2);
        assertEquals(expected, reader.get());
    }

    @Test
    public void smallBufferReconnect() {
        var port = 666;
        var port1 = 888;
        var port2 = 888;
        var idMother1 = new Id(new InetSocketAddress("127.0.0.1", port));
        var id1 = new Id(new InetSocketAddress("10.100.1.1", port1));
        var id2 = new Id(new InetSocketAddress("10.20.42.10", port2));
        var address = new byte[] { 127, 0, 0, 1 };
        var address1 = new byte[] { 10, 100, 1, 1 };
        var address2 = new byte[] { 10, 20, 42, 10 };
        var buffer = ByteBuffer.allocateDirect(1024);
        buffer.put((byte) 14);
        buffer.put((byte) 4);
        for (var oneByte : address) {
            buffer.put(oneByte);
        }

        buffer.putInt(port);
        buffer.putInt(2);
        buffer.put((byte) 4);
        for (var oneByte : address1) {
            buffer.put(oneByte);
        }

        buffer.putInt(port1);

        buffer.put((byte) 4);
        for (var oneByte : address2) {
            buffer.put(oneByte);
        }

        buffer.putInt(port2);
        buffer.flip();

        var bbSmall = ByteBuffer.allocate(2);
        var reader = new DisconnectReader();
        while (buffer.hasRemaining()) {
            while (buffer.hasRemaining() && bbSmall.hasRemaining()) {
                bbSmall.put(buffer.get());
            }
            if (buffer.hasRemaining()) {
                assertEquals(Reader.ProcessStatus.REFILL, reader.process(bbSmall));
            } else {
                assertEquals(Reader.ProcessStatus.DONE, reader.process(bbSmall));
            }
        }

        var expected = new Packet.Disconnection.Reconnect(idMother1, List.of(id1, id2));
        assertEquals(expected, reader.get());
    }

    @Test
    public void errorGet() {
        var reader = new DisconnectReader();
        assertThrows(IllegalStateException.class, () -> {
            var res = reader.get();
        });
    }

    @Test
    public void errorHeaderByte() {
        var port = 666;
        var address = new byte[] { 127, 0, 0, 1 };
        var buffer = ByteBuffer.allocateDirect(1024);
        buffer.put((byte) 42);
        buffer.put((byte) 4);
        for (var oneByte : address) {
            buffer.put(oneByte);
        }

        buffer.putInt(port);

        var reader = new DisconnectReader();
        assertEquals(Reader.ProcessStatus.ERROR, reader.process(buffer));
    }
}
