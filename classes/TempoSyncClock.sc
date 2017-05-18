// An almost TempoClock that syncs to a TempoSync cluster
// by munshkr@gmail.com
//
// A lot of this was based on the MIDISyncClock class
// by H. James Harkins -- jamshark70@dewdrop-world.net

TempoSyncClock {
  classvar  <>ticksPerBeat = 4;
  classvar  responseFuncs;

  classvar  <ticks, <beats, <startTime,
            <tempo, <beatDur,
            <beatsPerBar = 4, <barsPerBeat = 0.25, <baseBar, <baseBarBeat;

  // private vars
  classvar  lastTickTime, <queue;

  *initClass {}

  *init {
    queue = PriorityQueue.new;
    beats = ticks = baseBar = baseBarBeat = 0;

    // Register Tick handler
    n = NetAddr.localAddr;
    OSCdef(\tick, { |msg, time, addr, recvPort| self.tick() }, '/temposync/tick', n);
  }

  *start {
    // FIXME should send message /start
    //startTime = lastTickTime = Main.elapsedTime;
    //ticks = beats = baseBar = baseBarBeat = 0;
  }

  *stop {
    // FIXME should send message /stop
    //this.clear;
  }

  *schedAbs { arg when, task;
    queue.put(when * ticksPerBeat, task);
  }

  *sched { arg when, task, adjustment = 0;
    queue.put((when * ticksPerBeat) + ticks + adjustment, task);
  }

  *tick {
    var  lastTickDelta, nextTime, task, tickIndex;
    // use nextTime as temp var to calculate tempo
    // this is inherently inaccurate; tempo will fluctuate slightly around base
    nextTime = Main.elapsedTime;
    lastTickDelta = nextTime - (lastTickTime ? 0);
    lastTickTime = nextTime;
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

  *play { arg task, when;
    this.schedAbs(when.nextTimeOnGrid(this), task);
  }

  *nextTimeOnGrid { arg quant = 1, phase = 0;
    var offset;
    if (quant < 0) { quant = beatsPerBar * quant.neg };
    offset = baseBarBeat + phase;
    ^roundUp(this.beats - offset, quant) + offset;
  }

  *beatsPerBar_ { |newBeatsPerBar = 4|
    this.setMeterAtBeat(newBeatsPerBar, beats)
  }

  *setMeterAtBeat { arg newBeatsPerBar, beats;
    // bar must be integer valued when meter changes or confusion results later.
    baseBar = round((beats - baseBarBeat) * barsPerBeat + baseBar, 1);
    baseBarBeat = beats;
    beatsPerBar = newBeatsPerBar;
    barsPerBeat = beatsPerBar.reciprocal;
    this.changed;
  }

  *beats2secs { |beats|
    ^beats * beatDur;
  }

  *secs2beats { |seconds|
    ^seconds * tempo;
  }

  // elapsed time doesn't make sense because this clock only advances when told
  // from outside - but, -play methods need elapsedBeats to calculate quant
  *elapsedBeats { ^beats }
  *seconds { ^startTime.notNil.if(Main.elapsedTime - startTime, nil) }

  *clear { queue.clear }

  // for debugging
  *dumpQueue {
    { queue.topPriority.notNil }.while({
      Post << "\n" << queue.topPriority << "\n";
      queue.pop.dumpFromQueue;
    });
  }
}
