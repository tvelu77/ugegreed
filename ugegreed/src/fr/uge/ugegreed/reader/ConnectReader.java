package fr.uge.ugegreed.reader;

import fr.uge.ugegreed.records.Id;
import fr.uge.ugegreed.records.Packet;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reads a connect packet.<br>
 * This class reads a connect packet and instantiate the correct Connect packet.
 *
 * @author Axel BELIN and Thomas VELU.
 */
public class ConnectReader implements Reader<Packet.Connection> {

  /**
   * Represents the state of the Reader.
   */
  private enum State {DONE, WAITING_TYPE, WAITING, WAITING_ID_DAUGHTER, WAITING_IDS, ERROR}
  private final ByteReader typeReader = new ByteReader();
  private final IdReader idReader = new IdReader();
  private final IdReader.ListIdReader idsReader = new IdReader.ListIdReader();
  private final ArrayList<Id> ids = new ArrayList<>();

  private State state = State.WAITING_TYPE;
  private byte type;
  private Id id;
  private Id idMotherOrDaughter;
  private Packet.Connection connectionPacket;

  /**
   * Processes a ConnectKo packet.<br>
   * Normally, this method should return DONE in every case as
   * connectKo doesn't have anything particular in it.
   *
   * @return ProcessStatus, DONE.
   */
  private ProcessStatus processConnectKo() {
    connectionPacket = new Packet.Connection.ConnectKo();
    state = State.DONE;
    return ProcessStatus.DONE;
  }

  /**
   * Processes the connect packet.<br>
   * This method reads an Id :<br>
   * - DONE, the Id has been read, so we can create the packet Connect.<br>
   * - REFILL, the Id couldn't be read, so we ask for a REFILL.<br>
   * - ERROR, the reader had a problem or the packet was badly formatted.
   *
   * @param buffer ByteBuffer, the buffer in write-mode.
   * @return ProcessStatus (DONE, REFILL, ERROR).
   */
  private ProcessStatus processConnect(ByteBuffer buffer) {
    var idStatus = idReader.process(buffer);
    return switch (idStatus) {
      case DONE -> {
        var id = idReader.get();
        connectionPacket = new Packet.Connection.Connect(id);
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
   * Processes the connectOk packet.<br>
   * This method reads an Id :<br>
   * - DONE, the Id has been read, so we can create the packet Connect.<br>
   * - REFILL, the Id couldn't be read, so we ask for a REFILL.<br>
   * - ERROR, the reader had a problem or the packet was badly formatted.<br>
   * It also reads a list of Ids (connected machine to the application).<br>
   * This reader acts the same way of the IdReader.
   *
   * @param buffer ByteBuffer, the buffer in write-mode.
   * @return ProcessStatus (DONE, REFILL, ERROR).
   */
  private ProcessStatus processConnectOk(ByteBuffer buffer) {
    if(state == State.WAITING) {
      var idStatus = idReader.process(buffer);
      switch (idStatus) {
        case DONE -> {
          idMotherOrDaughter = idReader.get();
          idReader.reset();
          state = State.WAITING_IDS;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
      }
    }

    if(state == State.WAITING_IDS) {
      var idsStatus = idsReader.process(buffer);
      return switch (idsStatus) {
        case DONE -> {
          ids.addAll(idsReader.get());
          idsReader.reset();
          connectionPacket = new Packet.Connection.ConnectOk(idMotherOrDaughter, List.copyOf(ids));
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
   * Processes the AddNode packet.<br>
   * This method uses an IdReader to read the source id and the daughter id
   * (the machine we want to add into the RouteTable).
   *
   * @param buffer ByteBuffer, the buffer in write-mode.
   * @return ProcessStatus (DONE, REFILL, ERROR).
   */
  private ProcessStatus processAddNode(ByteBuffer buffer) {
    if(state == State.WAITING) {
      var idStatus = idReader.process(buffer);
      switch (idStatus) {
        case DONE -> {
          id = idReader.get();
          idReader.reset();
          state = State.WAITING_ID_DAUGHTER;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
      }
    }

    if(state == State.WAITING_ID_DAUGHTER) {
      var idStatus = idReader.process(buffer);
      return switch (idStatus) {
        case DONE -> {
          idMotherOrDaughter = idReader.get();
          connectionPacket = new Packet.Connection.AddNode(id, idMotherOrDaughter);
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
        case 1 -> processConnect(buffer);
        case 2 -> processConnectKo();
        case 3 -> processConnectOk(buffer);
        case 4 -> processAddNode(buffer);
        default -> {
          state = State.ERROR;
          yield ProcessStatus.ERROR;
        }
      };
      case WAITING_ID_DAUGHTER -> processAddNode(buffer);
      case WAITING_IDS -> processConnectOk(buffer);
      default -> ProcessStatus.REFILL;
    };
  }

  @Override
  public Packet.Connection get() {
    if(state != State.DONE) {
      throw new IllegalStateException();
    }

    return connectionPacket;
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
