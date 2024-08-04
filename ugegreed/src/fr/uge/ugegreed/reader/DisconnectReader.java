package fr.uge.ugegreed.reader;

import fr.uge.ugegreed.records.Id;
import fr.uge.ugegreed.records.Packet;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reads a disconnect packet.<br>
 * This class reads a connect packet and instantiate the correct Disconnection packet.
 *
 * @author Axel BELIN and Thomas VELU.
 */
public class DisconnectReader implements Reader<Packet.Disconnection> {

  /**
   * Represents the state of the Reader.
   */
  private enum State {
    DONE, WAITING_TYPE, WAITING, WAITING_ID, WAITING_REQUEST_IDS, WAITING_RECONNECT_IDS, ERROR
  }

  private final ByteReader typeReader = new ByteReader();
  private final IdReader idReader = new IdReader();
  private final IdReader.ListIdReader idsReader = new IdReader.ListIdReader();
  private final ArrayList<Id> ids = new ArrayList<>();

  private State state = State.WAITING_TYPE;
  private byte type;
  private Id id;
  private Id idMother;
  private Packet.Disconnection disconnectionPacket;

  /**
   * Reads and instantiate a DisconnectionDenied packet.<br>
   * This packet has nothing special in it, so the return should be DONE.
   *
   * @return ProcessStatus, DONE.
   */
  private ProcessStatus processDenied() {
    disconnectionPacket = new Packet.Disconnection.DisconnectionDenied();
    state = State.DONE;
    return ProcessStatus.DONE;
  }

  /**
   * Reads and instantiate a DisconnectGranted packet.<br>
   * This packet has nothing special in it, so the return should be DONE.
   *
   * @return ProcessStatus, DONE.
   */
  private ProcessStatus processGranted() {
    disconnectionPacket = new Packet.Disconnection.DisconnectionGranted();
    state = State.DONE;
    return ProcessStatus.DONE;
  }

  /**
   * Reads and instantiate a DisconnectRequest packet.<br>
   * This method reads a list of IDs (the daughter's of the machine which want to disconnect).
   *
   * @param buffer ByteBuffer, the buffer in write-mode.
   * @return ProcessStatus (DONE, REFILL, ERROR).
   */
  private ProcessStatus processRequest(ByteBuffer buffer) {
    var idsStatus = idsReader.process(buffer);
    return switch (idsStatus) {
      case DONE -> {
        ids.addAll(idsReader.get());
        idsReader.reset();
        disconnectionPacket = new Packet.Disconnection.DisconnectionRequest(List.copyOf(ids));
        state = State.DONE;
        yield ProcessStatus.DONE;
      }
      case ERROR -> {
        state = State.ERROR;
        yield ProcessStatus.ERROR;
      }
      case REFILL -> {
        state = State.WAITING_REQUEST_IDS;
        yield ProcessStatus.REFILL;
      }
    };
  }

  /**
   * Reads and instantiate a PleaseReconnect packet.<br>
   * This method reads an ID (the target machine).
   *
   * @param buffer ByteBuffer, the buffer in write-mode.
   * @return ProcessStatus (DONE, REFILL, ERROR).
   */
  private ProcessStatus processPleaseReconnect(ByteBuffer buffer) {
    var idStatus = idReader.process(buffer);
    return switch (idStatus) {
      case DONE -> {
        var id = idReader.get();
        disconnectionPacket = new Packet.Disconnection.PleaseReconnect(id);
        idReader.reset();
        state = State.DONE;
        yield ProcessStatus.DONE;
      }
      case ERROR -> {
        state = State.ERROR;
        yield ProcessStatus.ERROR;
      }
      case REFILL -> ProcessStatus.REFILL;
    };
  }

  /**
   * Reads and instantiate a Reconnect packet.<br>
   * This method reads an ID (a daughter's id in the list) and a list (daughters' id)
   *
   * @param buffer ByteBuffer, the buffer in write-mode.
   * @return ProcessStatus (DONE, REFILL, ERROR).
   */
  private ProcessStatus processReconnect(ByteBuffer buffer) {
    if(state == State.WAITING) {
      var idStatus = idReader.process(buffer);
      switch (idStatus) {
        case DONE -> {
          id = idReader.get();
          idReader.reset();
          state = State.WAITING_RECONNECT_IDS;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
      }
    }

    if(state == State.WAITING_RECONNECT_IDS) {
      var idsStatus = idsReader.process(buffer);
      return switch (idsStatus) {
        case DONE -> {
          ids.addAll(idsReader.get());
          idsReader.reset();
          disconnectionPacket = new Packet.Disconnection.Reconnect(id, List.copyOf(ids));
          state = State.DONE;
          yield ProcessStatus.DONE;
        }
        case ERROR -> {
          state = State.ERROR;
          yield ProcessStatus.ERROR;
        }
        case REFILL -> ProcessStatus.REFILL;
      };
    }

    return ProcessStatus.REFILL;
  }

  /**
   * Reads and instantiates the Disconnected packet.<br>
   * This method reads an ID (the mother's id) and a list of IDs (daughters' ID).
   *
   * @param buffer ByteBuffer, the buffer in write-mode.
   * @return ProcessStatus (DONE, REFILL, ERROR).
   */
  private ProcessStatus processDisconnected(ByteBuffer buffer) {
    if(state == State.WAITING) {
      var idStatus = idReader.process(buffer);
      switch (idStatus) {
        case DONE -> {
          idMother = idReader.get();
          idReader.reset();
          state = State.WAITING_ID;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
      }
    }

    if(state == State.WAITING_ID) {
      var idStatus = idReader.process(buffer);
      return switch (idStatus) {
        case DONE -> {
          id = idReader.get();
          disconnectionPacket = new Packet.Disconnection.Disconnected(idMother, id);
          idReader.reset();
          state = State.DONE;
          yield ProcessStatus.DONE;
        }
        case ERROR -> {
          state = State.ERROR;
          yield ProcessStatus.ERROR;
        }
        case REFILL -> ProcessStatus.REFILL;
      };
    }

    return ProcessStatus.REFILL;
  }

  @Override
  public ProcessStatus process(ByteBuffer buffer) {
    Objects.requireNonNull(buffer);
    if(state == State.DONE || state == State.ERROR) {
      throw new IllegalStateException();
    }

    if(state == State.WAITING_TYPE) {
      var typeStatus = typeReader.process(buffer);
      switch (typeStatus) {
        case DONE -> {
          type = typeReader.get();
          typeReader.reset();
          state = State.WAITING;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
      }
    }

    return switch (state) {
      case WAITING -> switch (type) {
        case 10 -> processRequest(buffer);
        case 11 -> processDenied();
        case 12 -> processGranted();
        case 13 -> processPleaseReconnect(buffer);
        case 14 -> processReconnect(buffer);
        case 15 -> processDisconnected(buffer);
        default -> {
          state = State.ERROR;
          yield ProcessStatus.ERROR;
        }
      };
      case WAITING_ID -> processDisconnected(buffer);
      case WAITING_REQUEST_IDS -> processRequest(buffer);
      case WAITING_RECONNECT_IDS -> processReconnect(buffer);
      default -> ProcessStatus.REFILL;
    };
  }

  @Override
  public Packet.Disconnection get() {
    if(state != State.DONE) {
      throw new IllegalStateException();
    }

    return disconnectionPacket;
  }

  @Override
  public void reset() {
    state = State.WAITING_TYPE;
    typeReader.reset();
    idReader.reset();
    idsReader.reset();
    ids.clear();
  }
}
