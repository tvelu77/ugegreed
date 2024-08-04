package fr.uge.ugegreed.test;

import fr.uge.ugegreed.reader.ByteReader;
import fr.uge.ugegreed.reader.Reader;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

public class ByteReaderTest {

  @Test
  public void simple() {
    byte byteSequence = 1;
    var bb = ByteBuffer.allocate(1024);
    bb.put(byteSequence);
    var br = new ByteReader();
    assertEquals(Reader.ProcessStatus.DONE, br.process(bb));
    assertEquals(byteSequence, br.get());
    assertEquals(0, bb.position());
    assertEquals(bb.capacity(), bb.limit());
  }

  @Test
  public void errorGet() {
    var br = new ByteReader();
    assertThrows(IllegalStateException.class, br::get);
  }
}
