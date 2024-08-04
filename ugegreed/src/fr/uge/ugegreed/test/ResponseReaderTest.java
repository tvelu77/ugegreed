package fr.uge.ugegreed.test;

import fr.uge.ugegreed.reader.Reader;
import fr.uge.ugegreed.reader.ResponseReader;
import fr.uge.ugegreed.records.Response;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResponseReaderTest {

  @Test
  public void simpleTest() {
    var bb = ByteBuffer.allocate(1024);
    var response = new Response(822, (byte) 0, "hello, i am working");
    response.encode(bb);
    var reader = new ResponseReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(bb));
    assertEquals(Response.class, reader.get().getClass());
    assertEquals(response.value(), reader.get().value());
    assertEquals(response.opcode(), reader.get().opcode());
    assertEquals(response.response(), reader.get().response());
  }

}
