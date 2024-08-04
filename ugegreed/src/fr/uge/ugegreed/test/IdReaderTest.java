package fr.uge.ugegreed.test;

import fr.uge.ugegreed.reader.IdReader;
import fr.uge.ugegreed.reader.Reader;
import fr.uge.ugegreed.records.Id;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IdReaderTest {
    @Test
    void simple() {
        var port = 666;
        var expected = new Id(new InetSocketAddress("127.0.0.1", port));
        var address = new byte[] { 127, 0, 0, 1 };
        var buffer = ByteBuffer.allocateDirect(1024);
        buffer.put((byte) 4);
        buffer.put(address);
        buffer.putInt(port);

        var reader = new IdReader();
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(expected, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    public void reset() {
        var port1 = 666;
        var port2 = 777;
        var expected1 = new Id(new InetSocketAddress("127.0.0.1", port1));
        var expected2 = new Id(new InetSocketAddress("10.100.1.1", port2));
        var address1 = new byte[] { 127, 0, 0, 1 };
        var address2 = new byte[] { 10, 100, 1, 1 };
        var buffer = ByteBuffer.allocateDirect(1024);
        buffer.put((byte) 4);
        buffer.put(address1);
        buffer.putInt(port1);

        buffer.put((byte) 4);
        buffer.put(address2);
        buffer.putInt(port2);

        var reader = new IdReader();
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(expected1, reader.get());
        assertEquals(buffer.capacity(), buffer.limit());
        reader.reset();
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(expected2, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    public void smallBuffer() {
        var port = 666;
        var expected = new Id(new InetSocketAddress("127.0.0.1", port));
        var address = new byte[] { 127, 0, 0, 1 };
        var buffer = ByteBuffer.allocateDirect(1024);
        buffer.put((byte) 4);
        buffer.put(address);
        buffer.putInt(port);
        buffer.flip();

        var bbSmall = ByteBuffer.allocate(2);
        var reader = new IdReader();
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

        assertEquals(expected, reader.get());
    }

    @Test
    public void errorGet() {
        var reader = new IdReader();
        assertThrows(IllegalStateException.class, () -> {
            var res = reader.get();
        });
    }

    @Test
    public void errorAddr() {
        var port = 666;
        var address = new byte[] { 127, 0, 0, 1, 42 };
        var buffer = ByteBuffer.allocateDirect(1024);
        buffer.put((byte) 4);
        buffer.put(address);
        buffer.putInt(port);

        var reader = new IdReader();
        assertEquals(Reader.ProcessStatus.ERROR, reader.process(buffer));
    }

    @Test
    public void errorPort() {
        var port = -42;
        var address = new byte[] { 127, 0, 0, 1 };
        var buffer = ByteBuffer.allocateDirect(1024);
        buffer.put((byte) 4);
        buffer.put(address);
        buffer.putInt(port);

        var reader = new IdReader();
        assertEquals(Reader.ProcessStatus.ERROR, reader.process(buffer));
    }

    @Test
    void simpleList() {
        var port1 = 666;
        var port2 = 777;
        var port3 = 888;
        var id1 = new Id(new InetSocketAddress("127.0.0.1", port1));
        var id2 = new Id(new InetSocketAddress("10.100.1.1", port2));
        var id3 = new Id(new InetSocketAddress("10.20.42.10", port3));
        var address1 = new byte[] { 127, 0, 0, 1 };
        var address2 = new byte[] { 10, 100, 1, 1 };
        var address3 = new byte[] { 10, 20, 42, 10 };
        var expected = List.of(id1, id2, id3);
        var buffer = ByteBuffer.allocateDirect(1024);
        buffer.putInt(3);
        buffer.put((byte) 4);
        buffer.put(address1);
        buffer.putInt(port1);

        buffer.put((byte) 4);
        buffer.put(address2);
        buffer.putInt(port2);

        buffer.put((byte) 4);
        buffer.put(address3);
        buffer.putInt(port3);

        var reader = new IdReader.ListIdReader();
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(expected, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    void uniqueList() {
        var port = 666;
        var id = new Id(new InetSocketAddress("127.0.0.1", port));
        var address = new byte[] { 127, 0, 0, 1 };
        var expected = List.of(id);
        var buffer = ByteBuffer.allocateDirect(1024);
        buffer.putInt(1);
        buffer.put((byte) 4);
        buffer.put(address);
        buffer.putInt(port);

        var reader = new IdReader.ListIdReader();
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(expected, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    void emptyList() {
        var port = 666;
        var address = new byte[] { 127, 0, 0, 1 };
        var expected = List.of();
        var buffer = ByteBuffer.allocateDirect(1024);
        buffer.putInt(0);
        buffer.put((byte) 4);
        buffer.put(address);
        buffer.putInt(port);

        var reader = new IdReader.ListIdReader();
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(expected, reader.get());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    public void resetList() {
        var port1 = 666;
        var port2 = 777;
        var port3 = 888;
        var id1 = new Id(new InetSocketAddress("127.0.0.1", port1));
        var id2 = new Id(new InetSocketAddress("10.100.1.1", port2));
        var id3 = new Id(new InetSocketAddress("10.20.42.10", port3));
        var address1 = new byte[] { 127, 0, 0, 1 };
        var address2 = new byte[] { 10, 100, 1, 1 };
        var address3 = new byte[] { 10, 20, 42, 10 };
        var expected1 = List.of(id1, id2);
        var expected2 = List.of(id3);
        var buffer = ByteBuffer.allocateDirect(1024);
        buffer.putInt(2);
        buffer.put((byte) 4);
        buffer.put(address1);
        buffer.putInt(port1);

        buffer.put((byte) 4);
        buffer.put(address2);
        buffer.putInt(port2);

        buffer.putInt(1);
        buffer.put((byte) 4);
        buffer.put(address3);
        buffer.putInt(port3);

        var reader = new IdReader.ListIdReader();
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(expected1, reader.get());
        assertEquals(buffer.capacity(), buffer.limit());
        reader.reset();
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(expected2, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    public void smallBufferList() {
        var port1 = 666;
        var port2 = 777;
        var port3 = 888;
        var id1 = new Id(new InetSocketAddress("127.0.0.1", port1));
        var id2 = new Id(new InetSocketAddress("10.100.1.1", port2));
        var id3 = new Id(new InetSocketAddress("10.20.42.10", port3));
        var address1 = new byte[] { 127, 0, 0, 1 };
        var address2 = new byte[] { 10, 100, 1, 1 };
        var address3 = new byte[] { 10, 20, 42, 10 };
        var expected = List.of(id1, id2, id3);
        var buffer = ByteBuffer.allocateDirect(1024);
        buffer.putInt(3);
        buffer.put((byte) 4);
        buffer.put(address1);
        buffer.putInt(port1);

        buffer.put((byte) 4);
        buffer.put(address2);
        buffer.putInt(port2);

        buffer.put((byte) 4);
        buffer.put(address3);
        buffer.putInt(port3);
        buffer.flip();

        var bbSmall = ByteBuffer.allocate(2);
        var reader = new IdReader.ListIdReader();
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

        assertEquals(expected, reader.get());
    }

    @Test
    public void errorTooBig() {
        var port = 666;
        var address = new byte[] { 127, 0, 0, 1 };
        var buffer = ByteBuffer.allocateDirect(1024);
        buffer.putInt(1025);
        buffer.put((byte) 4);
        buffer.put(address);
        buffer.putInt(port);

        var reader = new IdReader.ListIdReader();
        assertEquals(Reader.ProcessStatus.ERROR, reader.process(buffer));
    }

    @Test
    void errorWrongListSize() {
        var port1 = 666;
        var port2 = 777;
        var port3 = 888;
        var address1 = new byte[] { 127, 0, 0, 1 };
        var address2 = new byte[] { 10, 100, 1, 1 };
        var address3 = new byte[] { 10, 20, 42, 10 };
        var buffer = ByteBuffer.allocateDirect(1024);
        buffer.putInt(42);
        buffer.put((byte) 4);
        buffer.put(address1);
        buffer.putInt(port1);

        buffer.put((byte) 4);
        buffer.put(address2);
        buffer.putInt(port2);

        buffer.put((byte) 4);
        buffer.put(address3);
        buffer.putInt(port3);

        var reader = new IdReader.ListIdReader();
        assertEquals(Reader.ProcessStatus.REFILL, reader.process(buffer));
    }
}
