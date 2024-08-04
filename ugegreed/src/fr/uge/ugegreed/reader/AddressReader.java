package fr.uge.ugegreed.reader;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * Reads an IP address (0.0.0.0 or 0000:0000:...).
 *
 * @author Axel BELIN and Thomas VELU.
 */
public class AddressReader implements Reader<InetAddress> {

  /**
   * Represents the state of the Reader.
   */
  private enum State {
    DONE, WAITING, ERROR
  }
  private State state = State.WAITING;
  private InetAddress value;
  private ByteBuffer addressBuffer = ByteBuffer.allocate(Byte.BYTES * 16); // dégueu mais j'ai pas trouvé mieux pour l'instant. Forax serait pas fier de moi

  /**
   * Sets the address type to allocate the correct amount of bytes.
   *
   * @param type Byte, 4 for ipv4 and 6 for ipv6.
   */
  public void setAddressType(byte type) {
    switch (type) {
      case 4 -> addressBuffer = ByteBuffer.allocate(Byte.BYTES * 4);
      case 6 -> addressBuffer = ByteBuffer.allocate(Byte.BYTES * 16);
      default -> throw new IllegalArgumentException("type should be 4 or 6 !");
    }
  }

  @Override
  public ProcessStatus process(ByteBuffer buffer) {
    if (state == State.DONE || state == State.ERROR) {
      throw new IllegalStateException();
    }
    buffer.flip();
    try {
      if (buffer.remaining() <= addressBuffer.remaining()) {
        addressBuffer.put(buffer);
      } else {
        var oldLimit = buffer.limit();
        buffer.limit(addressBuffer.remaining());
        addressBuffer.put(buffer);
        buffer.limit(oldLimit);
      }
    } finally {
      buffer.compact();
    }
    if (addressBuffer.hasRemaining()) {
      return ProcessStatus.REFILL;
    }
    state = State.DONE;
    addressBuffer.flip();
    try {
      value = InetAddress.getByAddress(addressBuffer.array());
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
    return ProcessStatus.DONE;
  }

  @Override
  public InetAddress get() {
    if (state != State.DONE) {
      throw new IllegalStateException("The reader is not done yet !");
    }
    return value;
  }

  @Override
  public void reset() {
    state = State.WAITING;
    value = null;
    addressBuffer.clear();
  }
}
