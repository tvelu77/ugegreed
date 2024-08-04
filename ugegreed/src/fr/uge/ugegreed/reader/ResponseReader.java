package fr.uge.ugegreed.reader;

import java.nio.ByteBuffer;

import fr.uge.ugegreed.records.Response;

import static fr.uge.ugegreed.records.Response.*;

public class ResponseReader implements Reader<Response> {
  
  private enum State {
    DONE, WAITING_VALUE, WAITING_TYPE, WAITING_STRING, ERROR 
  }
  private final StringReader stringReader = new StringReader();
  private final ByteReader byteReader = new ByteReader();
  private final LongReader longReader = new LongReader();
  private long value;
  private byte type;
  private String response = "";
  private Response result;
  private State state = State.WAITING_VALUE;
  
  private void changeState() {
    switch (type) {
      case JOB_DONE, JOB_EXCEPTION, JOB_TIMEOUT, FAILED_TO_RECOVER_CHECKER -> state = State.WAITING_STRING;
      default -> {
        System.out.println("error change state");
        state = State.ERROR;
      }
    }
  }

  @Override
  public ProcessStatus process(ByteBuffer buffer) {
    if (state == State.WAITING_VALUE) {
      var longState = longReader.process(buffer);
      switch (longState) {
        case DONE -> {
          value = longReader.get();
          longReader.reset();
          state = State.WAITING_TYPE;
        }
        case ERROR -> {
          System.out.println("error value");
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
      }
    }
    if (state == State.WAITING_TYPE) {
      var byteState = byteReader.process(buffer);
      switch (byteState) {
        case DONE -> {
          type = byteReader.get();
          changeState();
          byteReader.reset();
//          if (state == State.ERROR) {
//            return ProcessStatus.ERROR;
//          }
        }
        case ERROR -> {
          System.out.println("error state 2");
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
      }
    }
    if (state == State.WAITING_STRING) {
      var stringState = stringReader.process(buffer);
      switch (stringState) {
        case DONE -> {
          response = stringReader.get();
          stringReader.reset();
          state = State.DONE;
        }
        case ERROR -> {
          System.out.println("error string");
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
      }
    }
    result = new Response(value, type, response);
    return ProcessStatus.DONE;
  }
  

  @Override
  public Response get() {
    if (state != State.DONE) {
      throw new IllegalStateException("The ResponseReader is not done yet !");
    }
    return result;
  }

  @Override
  public void reset() {
    stringReader.reset();
    byteReader.reset();
    longReader.reset();
    value = 0;
    type = 0;
    response = "";
    result = null;
    state = State.WAITING_VALUE;   
  }

}
