package fr.uge.ugegreed.reader;

import fr.uge.ugegreed.records.IpAddress;

import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * Reads the type of IP address and the IP.
 *
 * @author Axel BELIN and Thomas VELU.
 */
public class IpAddressReader implements Reader<IpAddress> {
  private enum State {DONE, WAITING_TYPE, WAITING_ADDRESS, ERROR}

  private final ByteReader typeReader = new ByteReader();
  private final AddressReader addressReader = new AddressReader();

  private State state = State.WAITING_TYPE;
  private byte type;
  private InetAddress address;

  @Override
  public ProcessStatus process(ByteBuffer bb) {
    if (state == State.DONE || state == State.ERROR) {
      throw new IllegalArgumentException("the reader cannot start processing !");
    }

    if(state == State.WAITING_TYPE) {
      var typeStatus = typeReader.process(bb);
      switch (typeStatus) {
        case DONE -> {
          type = typeReader.get();
          typeReader.reset();
          if(type != 4 && type != 6) {
            state = State.ERROR;
            break;
          }

          addressReader.setAddressType(type);
          state = State.WAITING_ADDRESS;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
      }
    }

    if(state == State.WAITING_ADDRESS) {
      var addressStatus = addressReader.process(bb);
      return switch (addressStatus) {
        case DONE -> {
          address = addressReader.get();
          addressReader.reset();
          state = State.DONE;
          yield ProcessStatus.DONE;
        }
        case ERROR -> {
          state = State.ERROR;
          yield  ProcessStatus.ERROR;
        }
        case REFILL -> ProcessStatus.REFILL;
      };
    }

    if(state == State.ERROR) {
      return ProcessStatus.ERROR;
    }

    return ProcessStatus.REFILL;
  }

  @Override
  public IpAddress get() {
    if (state != State.DONE) {
      throw new IllegalStateException("The reader is not done yet !");
    }

    return new IpAddress(type, address);
  }

  @Override
  public void reset() {
    state = State.WAITING_TYPE;
    typeReader.reset();
  }
}
