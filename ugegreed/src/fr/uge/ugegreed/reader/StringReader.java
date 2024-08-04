package fr.uge.ugegreed.reader;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class StringReader implements Reader<String> {

    private static final Charset UTF_8 = StandardCharsets.UTF_8;
    private static final int BUFFER_SIZE = 1024;

    private enum State {
        DONE, WAITING, ERROR
    }

    private State state = State.WAITING;
    private final ByteBuffer internalStringBuffer = ByteBuffer.allocate(BUFFER_SIZE); // write-mode
    private final ByteBuffer internalIntBuffer = ByteBuffer.allocate(Integer.BYTES);
    private int size = -1;
    private String value;


    private boolean notFilledBuffer(ByteBuffer source, ByteBuffer destination) {
        source.flip();
        try {
            if (destination.hasRemaining()) {
                var oldLimit = source.limit();
                var newLimit = destination.remaining();
                if (newLimit > oldLimit) {
                    newLimit = oldLimit;
                }
                source.limit(newLimit);
                destination.put(source);
                source.limit(oldLimit);
                return destination.hasRemaining();
            }
            return false;
        } finally {
            source.compact();
        }
    }

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        if (notFilledBuffer(buffer, internalIntBuffer)) {
            return ProcessStatus.REFILL;
        }
        internalIntBuffer.flip();
        size = internalIntBuffer.getInt();
        if (size < 0 || size > BUFFER_SIZE) {
            state = State.ERROR;
            return ProcessStatus.ERROR;
        }
        internalStringBuffer.limit(size);
        if (notFilledBuffer(buffer, internalStringBuffer)) {
            return ProcessStatus.REFILL;
        }
        internalStringBuffer.flip();
        value = UTF_8.decode(internalStringBuffer).toString();
        internalStringBuffer.limit(BUFFER_SIZE);
        internalIntBuffer.compact();
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public String get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING;
        value = "";
        size = -1;
        internalIntBuffer.clear();
        internalStringBuffer.clear();
    }
}
