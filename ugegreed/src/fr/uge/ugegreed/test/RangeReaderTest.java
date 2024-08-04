package fr.uge.ugegreed.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import fr.uge.ugegreed.reader.RangeReader;
import fr.uge.ugegreed.reader.Reader;
import fr.uge.ugegreed.records.Task;

public class RangeReaderTest {
  
  @Test
  public void simpleTest() {
    var inf = 10;
    var sup = 20;
    var buffer = ByteBuffer.allocate(1024);
    buffer.putLong(inf).putLong(sup);
    var reader = new RangeReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    var actual = reader.get();
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
    assertEquals(inf, actual.inf());
    assertEquals(sup, actual.sup());  
  }
  
  @Test
  public void simpleTestWithRecord() {
    var inf = 10;
    var sup = 20;
    var expected = new Task.Range(inf, sup);
    var buffer = ByteBuffer.allocate(1024);
    expected.encode(buffer);
    var reader = new RangeReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    var actual = reader.get();
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
    assertEquals(expected.inf(), actual.inf());
    assertEquals(expected.sup(), actual.sup());  
  }
  
  @Test
  public void smallBuffer() {
    var inf = 10;
    var sup = 20;
    var expected = new Task.Range(inf, sup);
    var buffer = ByteBuffer.allocate(1024);
    expected.encode(buffer);
    buffer.flip();
    var bbSmall = ByteBuffer.allocate(2);
    var reader = new RangeReader();
    while (buffer.hasRemaining()) {
      while (buffer.hasRemaining() && bbSmall.hasRemaining()) {
        bbSmall.put(buffer.get());
      }
        if (buffer.hasRemaining()) {
            assertEquals(Reader.ProcessStatus.REFILL, reader.process(bbSmall));
        } else {
            assertEquals(Reader.ProcessStatus.DONE, reader.process(bbSmall));
        }
    }
    assertEquals(inf, reader.get().inf());
    assertEquals(sup, reader.get().sup());
  }

}
