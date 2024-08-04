package fr.uge.ugegreed.test;

import fr.uge.ugegreed.reader.IpAddressReader;
import fr.uge.ugegreed.reader.Reader;
import fr.uge.ugegreed.records.IpAddress;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IpAddressReaderTest {
  @Test
  public void simpleIpV4Test() {
    var address = new byte[] { 127, 0, 0, 0 };
    var bb = ByteBuffer.allocate(1024);
    bb.put((byte) 4);
    for (var oneByte : address) {
      bb.put(oneByte);
    }
    var ar = new IpAddressReader();
    try {
      var expectedAddress = InetAddress.getByName("127.0.0.0");
      var expectedType = (byte) 4;
      var expected = new IpAddress(expectedType, expectedAddress);
      assertEquals(Reader.ProcessStatus.DONE, ar.process(bb));
      var actual = ar.get();
      assertEquals(expected.address().toString(), actual.address().toString());
      assertEquals(expected.type(), actual.type());
      assertEquals(0, bb.position());
      assertEquals(bb.capacity(), bb.limit());
    } catch (UnknownHostException e) {
      throw new AssertionError("Expected value couldn't be found !");
    }
  }

  @Test
  public void simpleIpV6Test() {
    var address = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
    var bb = ByteBuffer.allocate(1024);
    bb.put((byte) 6);
    for (var oneByte : address) {
      bb.put(oneByte);
    }
    var ar = new IpAddressReader();
    try {
      var expectedAddress = InetAddress.getByName("::1");
      var expectedType = (byte) 6;
      var expected = new IpAddress(expectedType, expectedAddress);
      assertEquals(Reader.ProcessStatus.DONE, ar.process(bb));
      var actual = ar.get();
      assertEquals(expected.address().toString(), actual.address().toString());
      assertEquals(expected.type(), actual.type());
      assertEquals(0, bb.position());
      assertEquals(bb.capacity(), bb.limit());
    } catch (UnknownHostException e) {
      throw new AssertionError("Expected value couldn't be found !");
    }
  }

  @Test
  public void smallBuffer() throws UnknownHostException {
    var address = new byte[] { 127, 0, 0, 0 };
    var bb = ByteBuffer.allocate(1024);
    bb.put((byte) 4);
    for (var oneByte : address) {
      bb.put(oneByte);
    }

    bb.flip();

    var bbSmall = ByteBuffer.allocate(2);
    var ar = new IpAddressReader();
    while (bb.hasRemaining()) {
      while (bb.hasRemaining() && bbSmall.hasRemaining()) {
        bbSmall.put(bb.get());
      }
      if (bb.hasRemaining()) {
        assertEquals(Reader.ProcessStatus.REFILL, ar.process(bbSmall));
      } else {
        assertEquals(Reader.ProcessStatus.DONE, ar.process(bbSmall));
      }
    }

    var expectedAddress = InetAddress.getByName("127.0.0.0");
    var expectedType = (byte) 4;
    var expected = new IpAddress(expectedType, expectedAddress);
    var actual = ar.get();
    assertEquals(expected.address().toString(), actual.address().toString());
    assertEquals(expected.type(), actual.type());
  }
}
