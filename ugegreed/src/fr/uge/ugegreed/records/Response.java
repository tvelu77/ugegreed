package fr.uge.ugegreed.records;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;

public record Response(long value, byte opcode, String response) implements Encoder {
  public static final byte JOB_DONE = 0;
  public static final byte JOB_EXCEPTION = 1;
  public static final byte JOB_TIMEOUT = 2;
  public static final byte FAILED_TO_RECOVER_CHECKER = 3;
  
  private final static Charset UTF_8 = StandardCharsets.UTF_8;

  @Override
  public boolean encode(ByteBuffer buffer) {
    if (buffer.remaining() < Long.BYTES + Byte.BYTES + Integer.BYTES + response.length()) {
      return false;
    }
    buffer.putLong(value);
    buffer.put(opcode);
    buffer.putInt(response.length());
    var message = UTF_8.encode(response);
    buffer.put(message);
    return true;
  }
}
