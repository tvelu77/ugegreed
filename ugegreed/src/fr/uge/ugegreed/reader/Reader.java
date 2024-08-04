package fr.uge.ugegreed.reader;

import java.nio.ByteBuffer;

/**
 * Interface which defines multiple methods.<br>
 * These methods are used by Readers of T type.<br>
 * It helps to correctly read a buffer and avoid using a bytebuffer directly in the application.
 *
 * @param <T> Type, any type technically.
 * @author Axel BELIN and Thomas VELU.
 */
public interface Reader<T> {

  int BUFFER_SIZE = 1024;

  /**
   * Represents the global state of a reader.
   */
  enum ProcessStatus { DONE, REFILL, ERROR }

  /**
   * Reads the given buffer and returns the current state of the reading process.<br>
   * For example :<br>
   * - DONE means the reader is done, and everything has been read correctly,<br>
   * - REFILL means the reader is incomplete, and should recall process,<br>
   * - ERROR means the reader has encountered an error, internal or badly formatted packet.
   *
   * @param bb ByteBuffer, the buffer in write-mode.
   * @return ProcessStatus (DONE, REFILL, ERROR).
   */
  ProcessStatus process(ByteBuffer bb);

  /**
   * Returns the value that has been read in the buffer.<br>
   * This method should be called if the reader is DONE.
   *
   * @return T value.
   */
  T get();

  /**
   * Resets the reader, emptying the internal buffer and removing the value read.
   */
  void reset();

}
