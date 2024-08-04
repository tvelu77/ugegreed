package fr.uge.ugegreed.reader;

import fr.uge.ugegreed.records.Frame;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Reads the header of a packet.<br>
 * The header is composed of an IDFrame (Byte) and an opcode (Byte).<br>
 * This reader is to define the type of packet (LOCAL, TRANSFERT, BROADCAST).
 */
public class FrameReader implements Reader<Frame> {

  /**
   * Represents the state of the Reader.
   */
  private enum State {DONE, WAITING_FRAME, WAITING_OPCODE, ERROR}

  private final ByteReader frameReader = new ByteReader();
  private final ByteReader opcodeReader = new ByteReader();

  private State state = State.WAITING_FRAME;
  private byte frameId;
  private byte opcode;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        if(state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if(state == State.WAITING_FRAME) {
            var frameStatus = frameReader.process(buffer);
            switch (frameStatus) {
                case DONE -> {
                    frameId = frameReader.get();
                    frameReader.reset();
                    if(frameId < 0 || frameId > 3) {
                        state = State.ERROR;
                        return ProcessStatus.ERROR;
                    }

                    state = State.WAITING_OPCODE;
                }
                case ERROR -> {
                    state = State.ERROR;
                    return ProcessStatus.ERROR;
                }
            }
        }

        if(state == State.WAITING_OPCODE) {
            var opcodeStatus = opcodeReader.process(buffer);
            return switch (opcodeStatus) {
                case DONE -> {
                    opcode = opcodeReader.get();
                    opcodeReader.reset();

                    if(opcode < 0 || opcode > 2) {
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
  public Frame get() {
    if(state != State.DONE) {
      throw new IllegalStateException();
    }

    return new Frame(frameId, opcode);
  }

  @Override
  public void reset() {
    state = State.WAITING_FRAME;
    frameReader.reset();
    opcodeReader.reset();
  }
}
