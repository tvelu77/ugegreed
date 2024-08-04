package fr.uge.ugegreed.reader;

import fr.uge.ugegreed.records.Id;
import fr.uge.ugegreed.records.IpAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reads an ID (InetSocketAddress).
 *
 * @author Axel BELIN and Thomas VELU.
 */
public class IdReader implements Reader<Id> {

  /**
   * Reads a list of ID.
   *
   * @author Axel BELIN and Thomas VELU.
   */
  public static class ListIdReader implements Reader<List<Id>> {

    /**
     * Represents the state of the Reader.
     */
    private enum State{DONE, WAITING_SIZE, WAITING_IDS, ERROR}

    private final IntReader sizeReader = new IntReader();
    private final IdReader idReader = new IdReader();
    private final ArrayList<Id> ids = new ArrayList<>();

    private State state = State.WAITING_SIZE;
    private int remainingIds;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
      Objects.requireNonNull(buffer);
      if(state == State.DONE || state == State.ERROR) {
        throw new IllegalStateException();
      }

      if(state == State.WAITING_SIZE) {
        var sizeStatus = sizeReader.process(buffer);
        switch (sizeStatus) {
          case DONE -> {
            int size = sizeReader.get();
            sizeReader.reset();
            if (size < 0 || size > BUFFER_SIZE) {
              state = State.ERROR;
              return ProcessStatus.ERROR;
            }

            if(size == 0) {
              state = State.DONE;
              return ProcessStatus.DONE;
            }

            remainingIds = size;
            state = State.WAITING_IDS;
          }
          case ERROR -> {
            state = State.ERROR;
            return ProcessStatus.ERROR;
          }
        }
      }

      if(state == State.WAITING_IDS) {
        var remainingIds = this.remainingIds;
        for(var i = 0; i < remainingIds; i++) { // only process the remaining ids
          var currentIdStatus = idReader.process(buffer); // process ids one by one
          switch (currentIdStatus) {
            case DONE -> {
              var currentId = idReader.get();
              ids.add(currentId);
              this.remainingIds--;
              idReader.reset();
              if(this.remainingIds == 0) {
                state = State.DONE;
                return ProcessStatus.DONE;
              }
            }
            case ERROR -> {
              state = State.ERROR;
              return ProcessStatus.ERROR;
            }
          }
        }
      }

      return ProcessStatus.REFILL;
    }

    @Override
    public List<Id> get() {
      if(state != State.DONE) {
        throw new IllegalStateException();
      }

      return List.copyOf(ids);
    }

    @Override
    public void reset() {
      state = State.WAITING_SIZE;
      sizeReader.reset();
      idReader.reset();
      ids.clear();
    }
  }


  /**
   * Represents the state of the ID Reader.
   */
  private enum State{DONE, WAITING_IP, WAITING_PORT, ERROR}

  private final IpAddressReader ipReader = new IpAddressReader();
  private final IntReader portReader = new IntReader();
  private State state = State.WAITING_IP;
  private IpAddress ip;
  private int port;

  @Override
  public ProcessStatus process(ByteBuffer buffer) {
    Objects.requireNonNull(buffer);
    if(state == State.DONE || state == State.ERROR) {
      throw new IllegalStateException();
    }

    if(state == State.WAITING_IP) {
      var ipStatus = ipReader.process(buffer);
      switch (ipStatus) {
        case DONE -> {
          ip = ipReader.get();
          ipReader.reset();
          state = State.WAITING_PORT;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
      }
    }

    if(state == State.WAITING_PORT) {
      var portStatus = portReader.process(buffer);
      return switch (portStatus) {
        case DONE -> {
          port = portReader.get();
          portReader.reset();
          if (port < 0 || port > 0xFFFF) {
            state = State.ERROR;
            yield ProcessStatus.ERROR;
          }

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
  public Id get() {
    if(state != State.DONE) {
      throw new IllegalStateException();
    }

    var address = new InetSocketAddress(ip.address(), port);
    return new Id(address);
  }

  @Override
  public void reset() {
    state = State.WAITING_IP;
    ipReader.reset();
    portReader.reset();
  }
}
