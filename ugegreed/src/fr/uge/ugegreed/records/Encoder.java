package fr.uge.ugegreed.records;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * This interface lets permitted class/record encode into a buffer.<br>
 * This interface defines encode method such as encodeLocal, encodeBroadcast and encodeTransfert.<br>
 * The methods expect the buffer given in parameter to be in write-mode.
 *
 * @author Axel BELIN and Thomas VELU.
 */
public sealed interface Encoder permits Id, Task, Packet, Response {
  short HEADER_SIZE = 3 * Byte.BYTES;

  // buffer is in write mode

  /**
   * Encodes into the buffer.<br>
   * Returns a boolean to represent if the content could have been encoded into the buffer.
   *
   * @param buffer ByteBuffer, the buffer in write-mode.
   * @return True if the content could be encoded into the buffer,
   * false if not.
   */
  boolean encode(ByteBuffer buffer);

  /**
   * Returns the total size of the encoded content.
   *
   * @return Int, the size of the encoded content.
   */
  default int size() {
    return HEADER_SIZE;
  }

  /**
   * Encodes the Local Packet into the given buffer.<br>
   * Returns a boolean to represent if the content could have been encoded into the buffer.
   *
   * @param buffer ByteBuffer, the buffer in write-mode.
   * @return True if the content could have been encoded into the buffer,
   * false if not.
   */
  default boolean encodeLocal(ByteBuffer buffer) {
    throw new IllegalStateException("Not a Local Packet");
  }

  /**
   * Encodes the Broadcast Packet into the given buffer.<br>
   * Returns a boolean to represent if the content could have been encoded into the buffer.
   *
   * @param buffer ByteBuffer, the buffer in write-mode.
   * @return True if the content could have been encoded into the buffer,
   * false if not.
   */
  default boolean encodeBroadcast(ByteBuffer buffer) {
    throw new IllegalStateException("Not a Broadcast Packet");
  }

  /**
   * Encodes the Transfert Packet into the given buffer.<br>
   * Returns a boolean to represent if the content could have been encoded into the buffer.
   *
   * @param buffer ByteBuffer, the buffer in write-mode.
   * @return True if the content could have been encoded into the buffer,
   * false if not.
   */
  default boolean encodeTransfert(ByteBuffer buffer) {
    throw new IllegalStateException("Not a Transfert packet");
  }

  /**
   * Encodes the header into the buffer.<br>
   * The header is the Local Packet, Transfert Packet or Broadcast Packet.
   *
   * @param buffer ByteBuffer, the buffer in write-mode.
   * @param frameId Byte, the type of packet.
   * @param opcode Byte, the packet's opcode.
   * @return True if the content could have been encoded into the buffer,
   * false if not.
   */
  static boolean encodeHeader(ByteBuffer buffer, byte frameId, byte opcode) {
    if(buffer.remaining() < HEADER_SIZE) {
      return false;
    }

    buffer.put(frameId);
    buffer.put(opcode);
    return true;
  }

  /**
   * Encodes an ID into the given buffer.<br>
   * The ID is a record containing the inetSocketAddress of the client.
   *
   * @param buffer ByteBuffer, the buffer in write-mode.
   * @param id Id, the Id.
   * @return True if the content could have been encoded into the buffer,
   * false if not.
   */
  static boolean encodeId(ByteBuffer buffer, Id id) {
    return buffer.remaining() >= id.size() && id.encode(buffer);
  }

  /**
   * Encodes a list of IDs into the given buffer.<br>
   * 
   * @param buffer ByteBuffer, the buffer in write-mode.
   * @param ids List of IDs.
   * @return True if the content could have been encoded into the buffer,
   * false if not.
   */
  static boolean encodeListIds(ByteBuffer buffer, List<Id> ids) {
    buffer.putInt(ids.size());
    for(var id : ids) {
      if(!encodeId(buffer, id)) {
        return false;
      }
    }

    return true;
  }
}
