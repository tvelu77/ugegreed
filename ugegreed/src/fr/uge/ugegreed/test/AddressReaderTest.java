package fr.uge.ugegreed.test;

import fr.uge.ugegreed.reader.AddressReader;
import fr.uge.ugegreed.reader.Reader;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AddressReaderTest {
  @Test
  public void simpleIpV4Test() {
    var address = new byte[] { 127, 0, 0, 0 };
    var bb = ByteBuffer.allocate(1024);
    for (var oneByte : address) {
      bb.put(oneByte);
    }
    var ar = new AddressReader();
    ar.setAddressType((byte) 4);
    try {
      var expected = InetAddress.getByName("127.0.0.0");
      assertEquals(Reader.ProcessStatus.DONE, ar.process(bb));
      assertEquals(expected.toString(), ar.get().toString());
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
    for (var oneByte : address) {
      bb.put(oneByte);
    }
    var ar = new AddressReader();
    ar.setAddressType((byte) 6);
    try {
      var expected = InetAddress.getByName("::1");
      assertEquals(Reader.ProcessStatus.DONE, ar.process(bb));
      assertEquals(expected.toString(), ar.get().toString());
      assertEquals(0, bb.position());
      assertEquals(bb.capacity(), bb.limit());
    } catch (UnknownHostException e) {
      throw new AssertionError("Expected value couldn't be found !");
    }
  }

  @Test
  public void wrongType() {
    assertThrows(IllegalArgumentException.class, () -> {
      var ar = new AddressReader();
      ar.setAddressType((byte) 0);
    });
  }
}
