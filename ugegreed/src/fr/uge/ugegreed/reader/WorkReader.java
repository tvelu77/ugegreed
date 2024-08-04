package fr.uge.ugegreed.reader;

import fr.uge.ugegreed.records.Id;
import fr.uge.ugegreed.records.Packet;
import fr.uge.ugegreed.records.Response;
import fr.uge.ugegreed.records.Task;
import java.nio.ByteBuffer;
import java.util.Objects;

public class WorkReader implements Reader<Packet.Work> {
  private enum State {
    DONE, WAITING_TYPE, WAITING, WAITING_ID_SRC, WAITING_REQUEST_ID, WAITING_NB_COMPUTATION, ERROR,
    WAITING_CHECKER, WAITING_RANGE, WAITING_RESPONSE
  }
  private final ByteReader typeReader = new ByteReader();
  private final LongReader longReader = new LongReader();
  private final IdReader idReader = new IdReader();
  private final CheckerReader checkerReader = new CheckerReader();
  private final RangeReader rangeReader = new RangeReader();
  private final ResponseReader responseReader = new ResponseReader();
  private State state = State.WAITING_TYPE;
  private byte type;
  private Id idSrc;
  private Id idDest;
  private long requestId;
  private long nbComputation;
  private Packet.Work workPacket;
  private Task.Checker checker;
  private Task.Range range;
  private Response response;

  private ProcessStatus processWorkRequest(ByteBuffer buffer) {
    if (state == State.WAITING) {
      var idState = idReader.process(buffer);
      switch (idState) {
        case DONE -> {
          idDest = idReader.get();
          idReader.reset();
          state = State.WAITING_ID_SRC;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
      }
    }
    if (state == State.WAITING_ID_SRC) {
      var idState = idReader.process(buffer);
      switch (idState) {
        case DONE -> {
          idSrc = idReader.get();
          idReader.reset();
          state = State.WAITING_REQUEST_ID;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
      }
    }
    if (state == State.WAITING_REQUEST_ID) {
      var longState = longReader.process(buffer);
      switch (longState) {
        case DONE -> {
          requestId = longReader.get();
          longReader.reset();
          state = State.WAITING_CHECKER;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
      }
    }
    if (state == State.WAITING_CHECKER) {
      var checkerState = checkerReader.process(buffer);
      switch (checkerState) {
        case DONE -> {
          checker = checkerReader.get();
          checkerReader.reset();
          state = State.WAITING_RANGE;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
      }
    }
    if (state == State.WAITING_RANGE) {
      var rangeState = rangeReader.process(buffer);
      switch (rangeState) {
        case DONE -> {
          range = rangeReader.get();
          rangeReader.reset();
          state = State.WAITING_NB_COMPUTATION;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
      }
    }
    if (state == State.WAITING_NB_COMPUTATION) {
      var longState = longReader.process(buffer);
      switch (longState) {
        case DONE -> {
          nbComputation = longReader.get();
          longReader.reset();
          workPacket = new Packet.Work.WorkRequest(idSrc,
              idDest,
              requestId,
              checker,
              range,
              nbComputation);
          state = State.DONE;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
      }
    }
    return ProcessStatus.DONE;
  }

  private ProcessStatus processWorkAvailability(ByteBuffer buffer) {
    if (state == State.WAITING) {
      var idState = idReader.process(buffer);
      switch (idState) {
        case DONE -> {
          idDest = idReader.get();
          idReader.reset();
          state = State.WAITING_ID_SRC;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
      }
    }
    if (state == State.WAITING_ID_SRC) {
      var idState = idReader.process(buffer);
      switch (idState) {
        case DONE -> {
          idSrc = idReader.get();
          idReader.reset();
          state = State.WAITING_REQUEST_ID;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
      }
    }
    if (state == State.WAITING_REQUEST_ID) {
      var longState = longReader.process(buffer);
      switch (longState) {
        case DONE -> {
          requestId = longReader.get();
          longReader.reset();
          state = State.WAITING_NB_COMPUTATION;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
      }
    }
    if (state == State.WAITING_NB_COMPUTATION) {
      var longState = longReader.process(buffer);
      switch (longState) {
        case DONE -> {
          nbComputation = longReader.get();
          longReader.reset();
          workPacket = new Packet.Work.WorkAvailability(idSrc, idDest, requestId, nbComputation);
          state = State.DONE;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
      }
    }
    return ProcessStatus.DONE;
  }

  private ProcessStatus processWorkAssignment(ByteBuffer buffer) {
    if (state == State.WAITING) {
      var idState = idReader.process(buffer);
      switch (idState) {
        case DONE -> {
          idDest = idReader.get();
          idReader.reset();
          state = State.WAITING_ID_SRC;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
      }
    }
    if (state == State.WAITING_ID_SRC) {
      var idState = idReader.process(buffer);
      switch (idState) {
        case DONE -> {
          idSrc = idReader.get();
          idReader.reset();
          state = State.WAITING_REQUEST_ID;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
      }
    }
    if (state == State.WAITING_REQUEST_ID) {
      var longState = longReader.process(buffer);
      switch (longState) {
        case DONE -> {
          requestId = longReader.get();
          longReader.reset();
          state = State.WAITING_RANGE;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
      }
    }
    if (state == State.WAITING_RANGE) {
      var rangeState = rangeReader.process(buffer);
      switch (rangeState) {
        case DONE -> {
          range = rangeReader.get();
          rangeReader.reset();
          workPacket = new Packet.Work.WorkAssignment(idSrc,
              idDest,
              requestId,
              range);
          state = State.DONE;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
      }
    }
    return ProcessStatus.DONE;
  }

  private ProcessStatus processWorkResponse(ByteBuffer buffer) {
    if (state == State.WAITING) {
      var idState = idReader.process(buffer);
      switch (idState) {
        case DONE -> {
          idDest = idReader.get();
          idReader.reset();
          state = State.WAITING_ID_SRC;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
      }
    }
    if (state == State.WAITING_ID_SRC) {
      var idState = idReader.process(buffer);
      switch (idState) {
        case DONE -> {
          idSrc = idReader.get();
          idReader.reset();
          state = State.WAITING_REQUEST_ID;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
      }
    }
    if (state == State.WAITING_REQUEST_ID) {
      var longState = longReader.process(buffer);
      switch (longState) {
        case DONE -> {
          requestId = longReader.get();
          longReader.reset();
          state = State.WAITING_RESPONSE;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
      }
    }
    if (state == State.WAITING_RESPONSE) {
      var responseState = responseReader.process(buffer);
      switch (responseState) {
        case DONE -> {
          response = responseReader.get();
          responseReader.reset();
          workPacket = new Packet.Work.WorkResponse(idSrc,
              idDest,
              requestId,
              response);
          state = State.DONE;
        }
        case ERROR -> {
          state = State.ERROR;
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
      }
    }
    return ProcessStatus.DONE;
  }

  @Override
  public ProcessStatus process(ByteBuffer buffer) {
    Objects.requireNonNull(buffer);
    if (state == State.DONE || state == State.ERROR) {
      throw new IllegalStateException();
    }
    if (state == State.WAITING_TYPE) {
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
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
      }
    }
    return switch (state) {
      case WAITING, WAITING_ID_SRC, WAITING_REQUEST_ID -> switch (type) {
        case 1 -> processWorkRequest(buffer);
        case 2 -> processWorkAvailability(buffer);
        case 3 -> processWorkAssignment(buffer);
        case 4 -> processWorkResponse(buffer);
        default -> {
          state = State.ERROR;
          yield ProcessStatus.ERROR;
        }
      };
      case WAITING_CHECKER, WAITING_RANGE -> switch (type) {
        case 1 -> processWorkRequest(buffer);
        case 3 -> processWorkAssignment(buffer);
        default -> {
          state = State.ERROR;
          yield ProcessStatus.ERROR;
        }
      };
      case WAITING_NB_COMPUTATION -> switch (type) {
        case 1 -> processWorkRequest(buffer);
        case 2 -> processWorkAvailability(buffer);
        default -> {
          state = State.ERROR;
          yield ProcessStatus.ERROR;
        }
      };
      case WAITING_RESPONSE -> switch (type) {
      case 4 -> processWorkResponse(buffer);
      default -> {
        state = State.ERROR;
        yield ProcessStatus.ERROR;
      }
    };
      default -> ProcessStatus.ERROR;
    };
  }

  @Override
  public Packet.Work get() {
    if (state != State.DONE) {
      throw new IllegalStateException("the reader is not done yet !");
    }
    return workPacket;
  }

  @Override
  public void reset() {
    typeReader.reset();
    longReader.reset();
    idReader.reset();
    checkerReader.reset();
    rangeReader.reset();
    responseReader.reset();
    state = State.WAITING_TYPE;
    type = 0;
    idSrc = null;
    idDest = null;
    requestId = 0;
    nbComputation = 0;
    workPacket = null;
    checker = null;
    range = null;
    response = null;
  }
}
