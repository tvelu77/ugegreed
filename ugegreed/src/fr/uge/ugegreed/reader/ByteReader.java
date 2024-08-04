package fr.uge.ugegreed.reader;

import java.nio.ByteBuffer;

/**
 * Reads a byte from a buffer.<br>
 * It is mostly used to read frame id and the opcode.
 *
 * @author Axel BELIN and Thomas VELU.
 */
public class ByteReader implements Reader<Byte> {

  /**
   * Represents the state of the Reader.
   */
  private enum State {
    DONE, WAITING, ERROR
  }

  private State state = State.WAITING;
  private final ByteBuffer internalBuffer = ByteBuffer.allocate(Byte.BYTES); // write-mode
  private byte value;

  @Override
  public ProcessStatus process(ByteBuffer buffer) {
    if (state == State.DONE || state == State.ERROR) {
      throw new IllegalStateException();
    }
    buffer.flip();
    try {
      if (buffer.remaining() <= internalBuffer.remaining()) {
        internalBuffer.put(buffer);
      } else {
        var oldLimit = buffer.limit();
        buffer.limit(internalBuffer.remaining());
        internalBuffer.put(buffer);
        buffer.limit(oldLimit);
      }
    } finally {
      buffer.compact();
    }
    if (internalBuffer.hasRemaining()) {
      return ProcessStatus.REFILL;
    }
    state = State.DONE;
    internalBuffer.flip();
    value = internalBuffer.get();
    return ProcessStatus.DONE;
  }

  @Override
  public Byte get() {
    if (state != State.DONE) {
      throw new IllegalStateException("The ByteReader is not done yet !");
    }
    return value;
  }

  @Override
  public void reset() {
    state = State.WAITING;
    value = 0;
    internalBuffer.clear();
  }
}
