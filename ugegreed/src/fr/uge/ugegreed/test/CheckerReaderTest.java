package fr.uge.ugegreed.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import fr.uge.ugegreed.reader.CheckerReader;
import fr.uge.ugegreed.reader.Reader;
import fr.uge.ugegreed.records.Task;

public class CheckerReaderTest {
  
  public static final Charset UTF_8 = StandardCharsets.UTF_8;
  
  @Test
  public void simpleTest() {
    var URLString = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
    var pathString = "CheckerReaderTest.java";
    var buffer = ByteBuffer.allocate(1024);
    buffer.putInt(URLString.length());
    var encoded = UTF_8.encode(URLString);
    buffer.put(encoded);
    buffer.putInt(pathString.length());
    encoded = UTF_8.encode(pathString);
    buffer.put(encoded);
    var reader = new CheckerReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    var actual = reader.get();
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
    assertEquals(URLString, actual.javaUrl());
    assertEquals(pathString, actual.classPath());
  }
  
  @Test
  public void simpleTestWithRecord() {
    var URLString = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
    var pathString = "CheckerReaderTest.java";
    var buffer = ByteBuffer.allocate(1024);
    var expected = new Task.Checker(URLString, pathString);
    expected.encode(buffer);
    var reader = new CheckerReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    var actual = reader.get();
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
    assertEquals(expected.javaUrl(), actual.javaUrl());
    assertEquals(expected.classPath(), actual.classPath());
  }

  @Test
  public void smallBuffer() {
    var URLString = "https://www.youtube.com/watch?v=GieQq3eWSnE";
    var pathString = "CheckerReaderTest.java";
    var buffer = ByteBuffer.allocate(1024);
    buffer.putInt(URLString.length());
    var encoded = UTF_8.encode(URLString);
    buffer.put(encoded);
    buffer.putInt(pathString.length());
    encoded = UTF_8.encode(pathString);
    buffer.put(encoded);
    buffer.flip();
    var bbSmall = ByteBuffer.allocate(2);
    var reader = new CheckerReader();
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
    assertEquals(pathString, reader.get().classPath());
    assertEquals(URLString, reader.get().javaUrl());
  }

  @Test
  public void simpleTest2() {
    var URLString = "http://www-igm.univ-mlv.fr/~carayol/Factorizer.jar";
    var pathString = "fr.uge.factors.Factorizer";
    var buffer = ByteBuffer.allocate(1024);
    buffer.putInt(URLString.length());
    var encoded = UTF_8.encode(URLString);
    buffer.put(encoded);
    buffer.putInt(pathString.length());
    encoded = UTF_8.encode(pathString);
    buffer.put(encoded);
    var reader = new CheckerReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    var actual = reader.get();
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
    assertEquals(URLString, actual.javaUrl());
    assertEquals(pathString, actual.classPath());
  }

}
