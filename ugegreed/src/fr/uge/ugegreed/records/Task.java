package fr.uge.ugegreed.records;

import fr.uge.ugegreed.Console;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents the task given in a work packet.<br>
 * In this record, we have a checker (URL and String) and a Range (a long and a long).
 * 
 * @author Axel BELIN and Thomas VELU.
 */
public record Task(Checker checker, Range range, Path resultsFilename) implements Encoder {

  public static final Charset UTF_8 = StandardCharsets.UTF_8;
  
    public record Checker(String javaUrl, String classPath) {
      
      public Checker {
        Objects.requireNonNull(javaUrl);
        Objects.requireNonNull(classPath);
      }
      
      public boolean encode(ByteBuffer buffer) {
        var totalLength = Integer.BYTES + javaUrl.length() + Integer.BYTES + classPath.length();
        if (buffer.remaining() < totalLength) {
          return false;
        }
        buffer.putInt(javaUrl.length());
        var javaUrlEncoded = UTF_8.encode(javaUrl);
        buffer.put(javaUrlEncoded);
        buffer.putInt(classPath.length());
        var classPathEncoded = UTF_8.encode(classPath);
        buffer.put(classPathEncoded);
        return true;
      }
      
    }

    public record Range(long inf, long sup) {
      
      public Range {
        if (inf < 0 || inf > sup || sup < 0) {
          throw new IllegalArgumentException();
        }
        
      }
      
      /**
       * Encodes a range (two long values) into the given buffer.
       * 
       * @param buffer ByteBuffer, the buffer in write-mode.
       * @return True if the content could have been encoded into the buffer,
       * false if not.
       */
      public boolean encode(ByteBuffer buffer) {
        if (buffer.remaining() < Long.BYTES * 2) {
          return false;
        }
        buffer.putLong(inf);
        buffer.putLong(sup);
        return true;
      }
    }

    public static Task fromCommand(Console.StartCommand command) {
      var checker = new Checker(command.jarUrl(), command.className());
      var range = new Range(command.startRange(), command.endRange());
      return new Task(checker, range, command.fileName());
    }

    @Override
    public boolean encode(ByteBuffer buffer) {
      Objects.requireNonNull(buffer);
      if (!range.encode(buffer)) {
        return false;
      }
      if (!checker.encode(buffer)) {
        return false;
      }
      return true;
    }
}
