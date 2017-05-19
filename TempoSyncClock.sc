// An almost TempoClock that syncs to a TempoSync cluster
// by munshkr@gmail.com
//
// A lot of this was based on the MIDISyncClock class
// by H. James Harkins -- jamshark70@dewdrop-world.net

TempoSyncClock {
  var  <>ticksPerBeat = 4;

  var  <ticks, <beats, <startTime,
       <tempo, <beatDur,
       <beatsPerBar = 4, <barsPerBeat = 0.25, <baseBar, <baseBarBeat;

  // private vars
  var  lastTickTime, <queue;

  *new {
    ^super.new.init;
  }

  init {
    queue = PriorityQueue.new;
    startTime = lastTickTime = Main.elapsedTime;
    beats = ticks = baseBar = baseBarBeat = 0;

    // Register Tick handler
    OSCdef(\tempoclocktick, { |msg, time, addr, recvPort|
      this.tick;
    }, "/temposync/tick");
  }

  start {
    // FIXME should send message /start
    startTime = lastTickTime = Main.elapsedTime;
    ticks = beats = baseBar = baseBarBeat = 0;
  }

  stop {
    // FIXME should send message /stop
    this.clear;
  }

  schedAbs { |beat, task|
    beat.debug("beat (schedAbs)");
    ticksPerBeat.debug("ticksPerBeat (schedAbs)");
    queue.put(beat * ticksPerBeat, task);
  }

  sched { |delta, item, adjustment = 0|
    delta.debug("delta (sched)");
    ticksPerBeat.debug("ticksPerBeat (sched)");
    queue.put((delta * ticksPerBeat) + ticks + adjustment, item);
  }

  tick {
    var  lastTickDelta, nextTime, task, tickIndex;
    // use nextTime as temp var to calculate tempo
    // this is inherently inaccurate; tempo will fluctuate slightly around base
    nextTime = Main.elapsedTime;
    lastTickDelta = nextTime - (lastTickTime ? 0);
    lastTickTime = nextTime;
    lastTickDelta.debug("lastTickDelta (tick)");
    ticksPerBeat.debug("ticksPerBeat (tick)");
    tempo = (beatDur = lastTickDelta * ticksPerBeat).reciprocal;

    ticks = ticks + 1;
    beats = ticks / ticksPerBeat;

    // while loop needed because more than one thing may be scheduled for this tick
    { (queue.topPriority ?? { inf }) < ticks }.while({
      // perform the action, and check if it should be rescheduled
      (nextTime = (task = queue.pop).value(beats)).isNumber.if({
        this.sched(nextTime, task, -1)
      });
    });
  }

  play { |task, quant = 1|
    "play".postln;
    this.schedAbs(quant.nextTimeOnGrid(this), task);
  }

  nextTimeOnGrid { |quant = 1, phase = 0|
    var offset;
    "nextTimeOnGrid".postln;
    beatsPerBar.debug("beatsPerBar (nextTimeOnGrid)");
    quant.debug("quant (nextTimeOnGrid)");
    if (quant < 0) { quant = beatsPerBar * quant.neg };
    offset = baseBarBeat + phase;
    ^roundUp(this.beats - offset, quant) + offset;
  }

  beatsPerBar_ { |newBeatsPerBar = 4|
    this.setMeterAtBeat(newBeatsPerBar, beats)
  }

  setMeterAtBeat { |newBeatsPerBar, beats|
    // bar must be integer valued when meter changes or confusion results later.
    baseBar = round((beats - baseBarBeat) * barsPerBeat + baseBar, 1);
    baseBarBeat = beats;
    beatsPerBar = newBeatsPerBar;
    barsPerBeat = beatsPerBar.reciprocal;
    this.changed;
  }

  beats2secs { |beats|
    beats.debug("beats (beats2sec)");
    beatDur.debug("beatDur (beats2sec)");
    ^beats * beatDur;
  }

  secs2beats { |seconds|
    seconds.debug("seconds (secs2beats)");
    tempo.debug("tempo (secs2beats)");
    if (tempo.isNil, {
      ^0;
    }, {
      ^seconds * tempo;
    });
  }

  // elapsed time doesn't make sense because this clock only advances when told
  // from outside - but, -play methods need elapsedBeats to calculate quant
  elapsedBeats { ^beats }
  seconds { ^startTime.notNil.if(Main.elapsedTime - startTime, nil) }

  clear { queue.clear }

  // for debugging
  dumpQueue {
    { queue.topPriority.notNil }.while({
      Post << "\n" << queue.topPriority << "\n";
      queue.pop.dumpFromQueue;
    });
  }
}
