package fr.uge.ugegreed.reader;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import fr.uge.ugegreed.records.Task;
import fr.uge.ugegreed.records.Task.Range;

public class RangeReader implements Reader<Task.Range> {
  
  public static class ListRangeReader implements Reader<List<Range>> {
    
    /**
     * Represents the state of the Reader.
     */
    private enum State{DONE, WAITING_SIZE, WAITING_RANGES, ERROR}
    private final IntReader sizeReader = new IntReader();
    private final RangeReader rangeReader = new RangeReader();
    private final ArrayList<Range> ranges = new ArrayList<>();
    
    private State state = State.WAITING_SIZE;
    private int remainingRanges;
    
    @Override
    public ProcessStatus process(ByteBuffer buffer) {
      Objects.requireNonNull(buffer);
      if(state == State.DONE || state == State.ERROR) {
        throw new IllegalStateException();
      }
      if (state == State.WAITING_SIZE) {
        var sizeStatus = sizeReader.process(buffer);
        switch (sizeStatus) {
          case DONE -> {
            var size = sizeReader.get();
            sizeReader.reset();
            if (size < 0 || size > BUFFER_SIZE) {
              state = State.ERROR;
              return ProcessStatus.ERROR;
            }
            if (size == 0) {
              state = State.DONE;
            }
            remainingRanges = size;
            state = State.WAITING_RANGES;
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
      if (state == State.WAITING_RANGES) {
        var remainingRanges = this.remainingRanges;
        for (var i = 0; i < remainingRanges; i++) {
          var currentRangeStatus = rangeReader.process(buffer);
          switch (currentRangeStatus) {
            case DONE -> {
              var currentRange = rangeReader.get();
              ranges.add(currentRange);
              this.remainingRanges--;
              rangeReader.reset();
              if (this.remainingRanges == 0) {
                state = State.DONE;
                break;
              }
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
      }
      state = State.DONE;
      return ProcessStatus.DONE;
    }

    @Override
    public List<Range> get() {
      if (state != State.DONE) {
        throw new IllegalStateException();
      }
      return List.copyOf(ranges);
    }

    @Override
    public void reset() {
      state = State.WAITING_SIZE;
      sizeReader.reset();
      rangeReader.reset();
      ranges.clear();
    }
    
  }
  
  private enum State {
    DONE, WAITING_INF, WAITING_SUP, ERROR
  }
  private final LongReader longReader = new LongReader();
  private long inf;
  private long sup;
  private State state = State.WAITING_INF;
  private Task.Range range;

  @Override
  public ProcessStatus process(ByteBuffer bb) {
    if (state == State.WAITING_INF) {
      var infState = longReader.process(bb);
      switch (infState) {
        case DONE -> {
          inf = longReader.get();
          longReader.reset();
          state = State.WAITING_SUP;
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
    if (state == State.WAITING_SUP) {
      var supState = longReader.process(bb);
      switch (supState) {
        case DONE -> {
          sup = longReader.get();
          longReader.reset();
          range = new Task.Range(inf, sup);
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
  public Range get() {
    if (state != State.DONE) {
      throw new IllegalStateException("The LongReader is not done yet !");
    }
    return range;
  }

  @Override
  public void reset() {
    longReader.reset();
    inf = 0;
    sup = 0;
    state = State.WAITING_INF;
    range = null; 
  }

}
