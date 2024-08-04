package fr.uge.ugegreed.reader;

import fr.uge.ugegreed.records.Frame;
import fr.uge.ugegreed.records.Packet;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class PacketReader implements Reader<Packet> {

  private enum State {
    DONE, WAITING_FRAME, WAITING_COMMAND, ERROR
  }

  private final FrameReader frameReader = new FrameReader();
  private final ConnectReader connectReader = new ConnectReader();
  private final DisconnectReader disconnectReader = new DisconnectReader();
  private final WorkReader workReader = new WorkReader();

  private State state = State.WAITING_FRAME;
  private Frame frame;
  private Packet packet;
  private byte frameOpcode = -1;

  @SafeVarargs
  private static boolean isAuthorized(Packet recoveredPacket, Class<? extends Packet> ... authorizedTypes) {
    return Arrays.stream(authorizedTypes)
            .anyMatch(packetClass -> recoveredPacket.getClass().equals(packetClass));
  }

  @SafeVarargs
  private ProcessStatus readConnect(ByteBuffer buffer, Class<? extends Packet.Connection> ... authorizedTypes) {
    var connectState = connectReader.process(buffer);
    return switch (connectState) {
      case DONE -> {
        var recoveredPacket = connectReader.get();
        if(!isAuthorized(recoveredPacket, authorizedTypes)) {
          state = State.ERROR;
          yield ProcessStatus.ERROR;
        }

        packet = recoveredPacket;
        connectReader.reset();
        state = State.DONE;
        yield ProcessStatus.DONE;
      }
      case ERROR -> {
        state = State.ERROR;
        yield ProcessStatus.ERROR;
      }
      default -> {
        state = State.WAITING_COMMAND;
        yield ProcessStatus.REFILL;
      }
    };
  }

  @SafeVarargs
  private ProcessStatus readDisconnect(ByteBuffer buffer, Class<? extends Packet.Disconnection> ... authorizedTypes) {
    var disconnectState = disconnectReader.process(buffer);
    return switch (disconnectState) {
      case DONE -> {
        var recoveredPacket = disconnectReader.get();
        if(!isAuthorized(recoveredPacket, authorizedTypes)) {
          state = State.ERROR;
          yield ProcessStatus.ERROR;
        }

        packet = recoveredPacket;
        disconnectReader.reset();
        state = State.DONE;
        yield ProcessStatus.DONE;
      }
      case ERROR -> {
        state = State.ERROR;
        yield ProcessStatus.ERROR;
      }
      default -> {
        state = State.WAITING_COMMAND;
        yield ProcessStatus.REFILL;
      }
    };
  }

  @SafeVarargs
  private ProcessStatus readWork(ByteBuffer buffer, Class<? extends Packet.Work> ... authorizedTypes) {
    var workState = workReader.process(buffer);
    return switch (workState) {
      case DONE -> {
        var recoveredPacket = workReader.get();
        if (!isAuthorized(recoveredPacket, authorizedTypes)) {
          state = State.ERROR;
          yield ProcessStatus.ERROR;
        }
        packet = recoveredPacket;
        workReader.reset();
        state = State.DONE;
        yield ProcessStatus.DONE;
      }
      case ERROR -> {
        state = State.ERROR;
        yield ProcessStatus.ERROR;
      }
      default -> {
        state = State.WAITING_COMMAND;
        yield ProcessStatus.REFILL;
      }
    };
  }

  private ProcessStatus readLocalPacket(ByteBuffer buffer) {
    return switch (frameOpcode) {
      case 0 -> readConnect(
              buffer,
              Packet.Connection.Connect.class,
              Packet.Connection.ConnectKo.class,
              Packet.Connection.ConnectOk.class
      );
      case 1 -> readDisconnect(
              buffer,
              Packet.Disconnection.DisconnectionRequest.class,
              Packet.Disconnection.DisconnectionDenied.class,
              Packet.Disconnection.DisconnectionGranted.class,
              Packet.Disconnection.PleaseReconnect.class,
              Packet.Disconnection.Reconnect.class

      );
      default -> throw new IllegalStateException("unknown local packet !");
    };
  }

  private ProcessStatus readBroadcastPacket(ByteBuffer buffer) {
    return switch (frameOpcode) {
      case 0 -> readDisconnect(buffer, Packet.Disconnection.Disconnected.class);
      case 1 -> readConnect(buffer, Packet.Connection.AddNode.class);
      default -> {
        state = State.ERROR;
        yield ProcessStatus.ERROR;
      }
    };
  }

  private ProcessStatus readTransfert(ByteBuffer buffer) {
    return readWork(buffer,
            Packet.Work.WorkAssignment.class,
            Packet.Work.WorkRequest.class,
            Packet.Work.WorkResponse.class,
            Packet.Work.WorkAvailability.class);
  }

  @Override
  public ProcessStatus process(ByteBuffer bb) {
    if (state == State.DONE || state == State.ERROR) {
      throw new IllegalStateException();
    }
    var frameState = frameReader.process(bb);
    switch (frameState) {
      case DONE -> {
        frame = frameReader.get();
        frameReader.reset();
        state = State.WAITING_COMMAND;
      }
      case ERROR -> {
        state = State.ERROR;
        return ProcessStatus.ERROR;
      }
      case REFILL -> {
        state = State.WAITING_FRAME;
        return ProcessStatus.REFILL;
      }
    }

    frameOpcode = frame.opcode();
    return switch (frame.frameId()) {
      case 0 -> readLocalPacket(bb); // LOCAL
      case 1 -> readTransfert(bb); // TRANSFERT
      case 2 -> readBroadcastPacket(bb); // BROADCAST
      default -> throw new IllegalStateException("unknown packet !");
    };
  }

  @Override
  public Packet get() {
    if (state != State.DONE) {
      throw new IllegalStateException("the reader is not done yet !");
    }
    return packet;
  }

  @Override
  public void reset() {
    state = State.WAITING_FRAME;
    frame = null;
    packet = null;
    frameOpcode = -1;
    frameReader.reset();
    connectReader.reset();
    disconnectReader.reset();
    workReader.reset();
  }
}
