package fr.uge.ugegreed.reader;

import java.nio.ByteBuffer;
import fr.uge.ugegreed.records.Task;
import fr.uge.ugegreed.records.Task.Checker;

public class CheckerReader implements Reader<Task.Checker> {
  
  private final StringReader URLReader = new StringReader();
  private final StringReader classReader = new StringReader();
  
  /**
   * Represents the state of the Reader.
   */
  private enum State {DONE, WAITING_URL, WAITING_CLASS, ERROR}
  private State state = State.WAITING_URL;
  private String URL;
  private String classPath;
  private Task.Checker checker;

  @Override
  public ProcessStatus process(ByteBuffer bb) {
    if (state == State.WAITING_URL) {
      var urlState = URLReader.process(bb);
      switch (urlState) {
        case DONE -> {
          URL = URLReader.get();
          URLReader.reset();
          state = State.WAITING_CLASS;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
      }
    }
    if (state == State.WAITING_CLASS) {
      var classState = classReader.process(bb);
      switch (classState) {
        case DONE -> {
          classPath = classReader.get();
          classReader.reset();
          checker = new Task.Checker(URL, classPath);
          state = State.DONE;
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
      }
    }
    return ProcessStatus.DONE;
  }

  @Override
  public Checker get() {
    if(state != State.DONE) {
      throw new IllegalStateException();
    }
    return checker;
  }

  @Override
  public void reset() {
    classReader.reset();
    URLReader.reset();
    state = State.WAITING_URL;
    URL = null;
    classPath = null;
    checker = null;
    
  }

}
