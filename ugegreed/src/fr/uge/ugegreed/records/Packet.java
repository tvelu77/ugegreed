package fr.uge.ugegreed.records;

import java.nio.ByteBuffer;
import java.util.List;

public sealed interface Packet extends Encoder {
  sealed interface Connection extends Packet {
    @Override
    default boolean encodeLocal(ByteBuffer buffer) {
      return Encoder.encodeHeader(buffer, (byte) 0, (byte) 0);
    }

    record Connect(Id idDaughter) implements Connection {
      @Override
      public boolean encode(ByteBuffer buffer) {
        if(!encodeLocal(buffer)) {
          return false;
        }

        buffer.put((byte) 1);
        return Encoder.encodeId(buffer, idDaughter);
      }

      @Override
      public int size() {
        return HEADER_SIZE + idDaughter.size();
      }
    }

    record ConnectKo() implements Connection {
      @Override
      public boolean encode(ByteBuffer buffer) {
        if(!encodeLocal(buffer)) {
          return false;
        }

        buffer.put((byte) 2);
        return true;
      }
    }

    record ConnectOk(Id idMother, List<Id> ids) implements Connection {
      @Override
      public boolean encode(ByteBuffer buffer) {
        if(!encodeLocal(buffer)) {
          return false;
        }

        buffer.put((byte) 3);
        return Encoder.encodeId(buffer, idMother) && Encoder.encodeListIds(buffer, ids);
      }

      @Override
      public int size() {
        return HEADER_SIZE + idMother.size() + ids.size();
      }
    }

    record AddNode(Id id, Id idDaughter) implements Connection {
      @Override
      public boolean encodeBroadcast(ByteBuffer buffer) {
        return Encoder.encodeHeader(buffer, (byte) 2, (byte) 1);
      }

      @Override
      public boolean encode(ByteBuffer buffer) {
        if(!encodeBroadcast(buffer)) {
          return false;
        }

        buffer.put((byte) 4);
        return Encoder.encodeId(buffer, id) && Encoder.encodeId(buffer, idDaughter);
      }

      @Override
      public int size() {
        return HEADER_SIZE + id.size() + idDaughter.size();
      }
    }
  }

  sealed interface Disconnection extends Packet {
    @Override
    default boolean encodeLocal(ByteBuffer buffer) {
      return Encoder.encodeHeader(buffer, (byte) 0, (byte) 1);
    }

    record DisconnectionRequest(List<Id> listDaughters) implements Disconnection {
      @Override
      public boolean encode(ByteBuffer buffer) {
        if(!encodeLocal(buffer)) {
          return false;
        }

        buffer.put((byte) 10);
        return Encoder.encodeListIds(buffer, listDaughters);
      }

      @Override
      public int size() {
        return HEADER_SIZE + listDaughters.size();
      }
    }

    record DisconnectionDenied() implements Disconnection {
      @Override
      public boolean encode(ByteBuffer buffer) {
        if(!encodeLocal(buffer)) {
          return false;
        }

        buffer.put((byte) 11);
        return true;
      }
    }

    record DisconnectionGranted() implements Disconnection {
      @Override
      public boolean encode(ByteBuffer buffer) {
        if(!encodeLocal(buffer)) {
          return false;
        }

        buffer.put((byte) 12);
        return true;
      }
    }

    record PleaseReconnect(Id idMother) implements Disconnection {
      @Override
      public boolean encode(ByteBuffer buffer) {
        if(!encodeLocal(buffer)) {
          return false;
        }

        buffer.put((byte) 13);
        return Encoder.encodeId(buffer, idMother);
      }

      @Override
      public int size() {
        return HEADER_SIZE + idMother.size();
      }
    }

    record Reconnect(Id id, List<Id> ids) implements Disconnection {
      @Override
      public boolean encode(ByteBuffer buffer) {
        if(!encodeLocal(buffer)) {
          return false;
        }

        buffer.put((byte) 14);
        return Encoder.encodeId(buffer, id) && Encoder.encodeListIds(buffer, ids);
      }

      @Override
      public int size() {
        return HEADER_SIZE + ids.size();
      }
    }

    record Disconnected(Id sourceId, Id id) implements Disconnection {
      @Override
      public boolean encodeBroadcast(ByteBuffer buffer) {
        return Encoder.encodeHeader(buffer, (byte) 2, (byte) 0);
      }

      @Override
      public boolean encode(ByteBuffer buffer) {
        if(!encodeBroadcast(buffer)) {
          return false;
        }

        buffer.put((byte) 15);
        return Encoder.encodeId(buffer, sourceId) && Encoder.encodeId(buffer, id);
      }

      @Override
      public int size() {
        return HEADER_SIZE + sourceId.size() + id.size();
      }
    }
  }

  sealed interface Work extends Packet {
    Id idSrc();
    Id idDest();

    record WorkRequest(Id idSrc, Id idDest,
                       long requestId,
                       Task.Checker checker,
                       Task.Range range,
                       long nbComputation) implements Work {

      @Override
      public boolean encodeTransfert(ByteBuffer buffer) {
        return Encoder.encodeHeader(buffer, (byte) 1, (byte) 1);
      }

      @Override
      public boolean encode(ByteBuffer buffer) {
        if (!encodeTransfert(buffer)) {
          return false;
        }
        buffer.put((byte) 1);
        if (!Encoder.encodeId(buffer, idDest) || !Encoder.encodeId(buffer, idSrc)) {
          return false;
        }
        if (buffer.remaining() < Long.BYTES) {
          return false;
        }
        buffer.putLong(requestId);
        checker.encode(buffer);
        range.encode(buffer);
        if (buffer.remaining() < Long.BYTES) {
          return false;
        }
        buffer.putLong(nbComputation);
        return true;
      }

      @Override
      public int size() {
        return HEADER_SIZE + idSrc().size() + idDest.size() + Long.BYTES * 2; // TODO : + TASK.SIZE + RANGE.SIZE
      }
    }

    record WorkAvailability(Id idSrc, Id idDest, long requestId, long nbComputation) implements Work {

      @Override
      public boolean encodeTransfert(ByteBuffer buffer) {
        return Encoder.encodeHeader(buffer, (byte) 1, (byte) 1);
      }

      @Override
      public boolean encode(ByteBuffer buffer) {
        if (!encodeTransfert(buffer)) {
          return false;
        }
        buffer.put((byte) 2);
        if (!Encoder.encodeId(buffer, idDest) || !Encoder.encodeId(buffer, idSrc)) {
          return false;
        }
        if (buffer.remaining() < Long.BYTES) {
          return false;
        }
        buffer.putLong(requestId);
        if (buffer.remaining() < Long.BYTES) {
          return false;
        }
        buffer.putLong(nbComputation);
        return true;
      }

      @Override
      public int size() {
        return HEADER_SIZE + idSrc().size() + idDest.size() + Long.BYTES * 2;
      }
    }

    record WorkAssignment(Id idSrc, Id idDest, long requestId, Task.Range range) implements Work {

      @Override
      public boolean encodeTransfert(ByteBuffer buffer) {
        return Encoder.encodeHeader(buffer, (byte) 1, (byte) 1);
      }

      @Override
      public boolean encode(ByteBuffer buffer) {
        if (!encodeTransfert(buffer)) {
          return false;
        }
        buffer.put((byte) 3);
        if (!Encoder.encodeId(buffer, idDest) || !Encoder.encodeId(buffer, idSrc)) {
          return false;
        }
        if (buffer.remaining() < Long.BYTES) {
          return false;
        }
        buffer.putLong(requestId);
        range.encode(buffer);
        return true;
      }

      @Override
      public int size() {
        return HEADER_SIZE + idSrc().size() + idDest.size() + Long.BYTES; // TODO : + TASK RANGE SIZE
      }
    }

    record WorkResponse(Id idSrc, Id idDest, long requestId, Response response) implements Work {

      @Override
      public boolean encodeTransfert(ByteBuffer buffer) {
        return Encoder.encodeHeader(buffer, (byte) 1, (byte) 1);
      }

      @Override
      public boolean encode(ByteBuffer buffer) {
        if (!encodeTransfert(buffer)) {
          return false;
        }
        buffer.put((byte) 4);
        if (!Encoder.encodeId(buffer, idDest) || !Encoder.encodeId(buffer, idSrc)) {
          return false;
        }
        if (buffer.remaining() < Long.BYTES) {
          return false;
        }
        buffer.putLong(requestId);
        response.encode(buffer);
        return true;
      }

      @Override
      public int size() {
        return HEADER_SIZE + idSrc().size() + idDest.size() + Long.BYTES + response.size();
      }

    }
  }
}
