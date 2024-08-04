package fr.uge.ugegreed.test;

import fr.uge.ugegreed.reader.PacketReader;
import fr.uge.ugegreed.reader.Reader;
import fr.uge.ugegreed.records.Id;
import fr.uge.ugegreed.records.Packet;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PacketReaderTest {
  @Test
  public void simpleConnectTest() {
    var bb = ByteBuffer.allocate(1024);
    var address = new byte[] { 127, 0, 0, 1 };
    var port = 666;
    bb.put((byte) 0).put((byte) 0);
    bb.put((byte) 1);
    bb.put((byte) 4);
    for (var oneByte : address) {
      bb.put(oneByte);
    }
    bb.putInt(port);
    var pr = new PacketReader();
    assertEquals(Reader.ProcessStatus.DONE, pr.process(bb));
    assertEquals(Packet.Connection.Connect.class, pr.get().getClass());
  }

  @Test
  public void simpleConnectKoTest() {
    var bb = ByteBuffer.allocate(1024);
    bb.put((byte) 0).put((byte) 0);
    bb.put((byte) 2);
    var pr = new PacketReader();
    assertEquals(Reader.ProcessStatus.DONE, pr.process(bb));
    assertEquals(Packet.Connection.ConnectKo.class, pr.get().getClass());
  }

  @Test
  void simpleConnectOkTest() {
    var port = 666;
    var port1 = 777;
    var port2 = 888;
    var idMother = new Id(new InetSocketAddress("127.0.0.1", port));
    var id1 = new Id(new InetSocketAddress("10.100.1.1", port1));
    var id2 = new Id(new InetSocketAddress("10.20.42.10", port2));
    var expected = new Packet.Connection.ConnectOk(idMother, List.of(id1, id2));
    var address = new byte[] { 127, 0, 0, 1 };
    var address1 = new byte[] { 10, 100, 1, 1 };
    var address2 = new byte[] { 10, 20, 42, 10 };
    var buffer = ByteBuffer.allocateDirect(1024);

    buffer.put((byte) 0).put((byte) 0);
    buffer.put((byte) 3);
    buffer.put((byte) 4);
    for (var oneByte : address) {
      buffer.put(oneByte);
    }

    buffer.putInt(port);
    buffer.putInt(2);
    buffer.put((byte) 4);
    for (var oneByte : address1) {
      buffer.put(oneByte);
    }

    buffer.putInt(port1);

    buffer.put((byte) 4);
    for (var oneByte : address2) {
      buffer.put(oneByte);
    }

    buffer.putInt(port2);

    var reader = new PacketReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    var actualResult = reader.get();
    assertEquals(expected, actualResult);
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
    assertEquals(Packet.Connection.ConnectOk.class, actualResult.getClass());
  }

  @Test
  void simpleAddNodeTest() {
    var port1 = 777;
    var port2 = 888;
    var id1 = new Id(new InetSocketAddress("10.100.1.1", port1));
    var id2 = new Id(new InetSocketAddress("10.20.42.10", port2));
    var address1 = new byte[] { 10, 100, 1, 1 };
    var address2 = new byte[] { 10, 20, 42, 10 };
    var buffer = ByteBuffer.allocateDirect(1024);

    buffer.put((byte) 2).put((byte) 1);

    buffer.put((byte) 4);
    buffer.put((byte) 4);
    for (var oneByte : address1) {
      buffer.put(oneByte);
    }

    buffer.putInt(port1);

    buffer.put((byte) 4);
    for (var oneByte : address2) {
      buffer.put(oneByte);
    }

    buffer.putInt(port2);

    var reader = new PacketReader();
    var expected = new Packet.Connection.AddNode(id1, id2);
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(expected, reader.get());
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
    assertEquals(Packet.Connection.AddNode.class, reader.get().getClass());
  }

  @Test
  void simpleDisconnectionDenied() {
    var expected = new Packet.Disconnection.DisconnectionDenied();
    var buffer = ByteBuffer.allocateDirect(1024);
    buffer.put((byte) 0).put((byte) 1);
    buffer.put((byte) 11);
    var reader = new PacketReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(expected, reader.get());
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
    assertEquals(Packet.Disconnection.DisconnectionDenied.class, reader.get().getClass());
  }

  @Test
  void simpleDisconnectionGranted() {
    var expected = new Packet.Disconnection.DisconnectionGranted();
    var buffer = ByteBuffer.allocateDirect(1024);
    buffer.put((byte) 0).put((byte) 1);
    buffer.put((byte) 12);
    var reader = new PacketReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(expected, reader.get());
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
    assertEquals(Packet.Disconnection.DisconnectionGranted.class, reader.get().getClass());
  }

  @Test
  void simpleDisconnectionRequest() {
    var port1 = 777;
    var port2 = 888;
    var id1 = new Id(new InetSocketAddress("10.100.1.1", port1));
    var id2 = new Id(new InetSocketAddress("10.20.42.10", port2));
    var expected = new Packet.Disconnection.DisconnectionRequest(List.of(id1, id2));
    var address1 = new byte[] { 10, 100, 1, 1 };
    var address2 = new byte[] { 10, 20, 42, 10 };
    var buffer = ByteBuffer.allocateDirect(1024);
    buffer.put((byte) 0).put((byte) 1);
    buffer.put((byte) 10);
    buffer.putInt(2);
    buffer.put((byte) 4);
    for (var oneByte : address1) {
      buffer.put(oneByte);
    }

    buffer.putInt(port1);

    buffer.put((byte) 4);
    for (var oneByte : address2) {
      buffer.put(oneByte);
    }

    buffer.putInt(port2);

    var reader = new PacketReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(expected, reader.get());
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
    assertEquals(Packet.Disconnection.DisconnectionRequest.class, reader.get().getClass());
  }

  @Test
  void simplePleaseReconnect() {
    var port = 666;
    var id = new Id(new InetSocketAddress("127.0.0.1", port));
    var expected = new Packet.Disconnection.PleaseReconnect(id);
    var address = new byte[] { 127, 0, 0, 1 };
    var buffer = ByteBuffer.allocateDirect(1024);
    buffer.put((byte) 0).put((byte) 1);
    buffer.put((byte) 13);
    buffer.put((byte) 4);
    for (var oneByte : address) {
      buffer.put(oneByte);
    }

    buffer.putInt(port);

    var reader = new PacketReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(expected, reader.get());
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
    assertEquals(Packet.Disconnection.PleaseReconnect.class, reader.get().getClass());
  }

  @Test
  void simpleReconnect() {
    var port = 666;
    var port1 = 777;
    var port2 = 888;
    var id = new Id(new InetSocketAddress("127.0.0.1", port));
    var id1 = new Id(new InetSocketAddress("10.100.1.1", port1));
    var id2 = new Id(new InetSocketAddress("10.20.42.10", port2));
    var expected = new Packet.Disconnection.Reconnect(id, List.of(id1, id2));
    var address = new byte[] { 127, 0, 0, 1 };
    var address1 = new byte[] { 10, 100, 1, 1 };
    var address2 = new byte[] { 10, 20, 42, 10 };
    var buffer = ByteBuffer.allocateDirect(1024);
    buffer.put((byte) 0).put((byte) 1);
    buffer.put((byte) 14);
    buffer.put((byte) 4);
    for (var oneByte : address) {
      buffer.put(oneByte);
    }

    buffer.putInt(port);
    buffer.putInt(2);
    buffer.put((byte) 4);
    for (var oneByte : address1) {
      buffer.put(oneByte);
    }

    buffer.putInt(port1);

    buffer.put((byte) 4);
    for (var oneByte : address2) {
      buffer.put(oneByte);
    }

    buffer.putInt(port2);

    var reader = new PacketReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(expected, reader.get());
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
    assertEquals(Packet.Disconnection.Reconnect.class, reader.get().getClass());
  }

  @Test
  void simpleDisconnected() {
    var port1 = 777;
    var port2 = 888;
    var id1 = new Id(new InetSocketAddress("10.100.1.1", port1));
    var id2 = new Id(new InetSocketAddress("10.20.42.10", port2));
    var address1 = new byte[] { 10, 100, 1, 1 };
    var address2 = new byte[] { 10, 20, 42, 10 };
    var buffer = ByteBuffer.allocateDirect(1024);

    buffer.put((byte) 2).put((byte) 0);

    buffer.put((byte) 15);
    buffer.put((byte) 4);
    for (var oneByte : address1) {
      buffer.put(oneByte);
    }

    buffer.putInt(port1);

    buffer.put((byte) 4);
    for (var oneByte : address2) {
      buffer.put(oneByte);
    }

    buffer.putInt(port2);

    var reader = new PacketReader();
    var expected = new Packet.Disconnection.Disconnected(id1, id2);
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(expected, reader.get());
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
    assertEquals(Packet.Disconnection.Disconnected.class, reader.get().getClass());
  }

  @Test
  void disconnectedError() {
    var port1 = 777;
    var port2 = 888;
    var address1 = new byte[] { 10, 100, 1, 1 };
    var address2 = new byte[] { 10, 20, 42, 10 };
    var buffer = ByteBuffer.allocateDirect(1024);

    buffer.put((byte) 0).put((byte) 1);

    buffer.put((byte) 15);
    buffer.put((byte) 4);
    for (var oneByte : address1) {
      buffer.put(oneByte);
    }

    buffer.putInt(port1);

    buffer.put((byte) 4);
    for (var oneByte : address2) {
      buffer.put(oneByte);
    }

    buffer.putInt(port2);

    var reader = new PacketReader();
    assertEquals(Reader.ProcessStatus.ERROR, reader.process(buffer));
  }

  @Test
  void disconnectedErrorWrongBroadcast() {
    var port1 = 777;
    var port2 = 888;
    var address1 = new byte[] { 10, 100, 1, 1 };
    var address2 = new byte[] { 10, 20, 42, 10 };
    var buffer = ByteBuffer.allocateDirect(1024);

    buffer.put((byte) 2).put((byte) 1);

    buffer.put((byte) 15);
    buffer.put((byte) 4);
    for (var oneByte : address1) {
      buffer.put(oneByte);
    }

    buffer.putInt(port1);

    buffer.put((byte) 4);
    for (var oneByte : address2) {
      buffer.put(oneByte);
    }

    buffer.putInt(port2);

    var reader = new PacketReader();
    assertEquals(Reader.ProcessStatus.ERROR, reader.process(buffer));
  }
}
