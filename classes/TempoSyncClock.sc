// An almost TempoClock that syncs to a TempoSync cluster
// by munshkr@gmail.com
//
// A lot of this was based on the MIDISyncClock class
// by H. James Harkins -- jamshark70@dewdrop-world.net

TempoSyncClock {
  classvar  <barsPerBeat = 0.25,
            <baseBar = 0,
            <baseBarBeat = 0,
            <beats = 0,
            <beatsPerBar = 4,
            <started = false,
            <tempo = 1,
            <ticks = 0,
            <ticksPerBeat = 4;

  classvar  <startTime, <beatDur;

  // Private vars
  classvar  <queue;

  *initClass {
    queue = PriorityQueue.new;
    startTime = Main.elapsedTime;
  }

  // If user tries to construct an instance, return singleton
  *new { ^this }

  *start {
    // Register Tick responder
    OSCdef(\tempoclocktick, { |msg, time, addr, recvPort|
      started = true;
      this.prTick(msg[1]);
    }, "/temposync/tick");
  }

  *stop {
    started = false;
    // Unregister Tick responder
    OSCdef(\tempoclocktick).free;
  }

  *schedAbs { |beat, task|
    queue.put(beat * ticksPerBeat, task);
    this.prScheduleFromQueue;
  }

  *sched { |delta, task|
    queue.put((delta * ticksPerBeat) + ticks, task);
    this.prScheduleFromQueue;
  }

  *play { |task, quant = 1|
    this.schedAbs(quant.nextTimeOnGrid(this), task);
  }

  *clear {
    ticks = beats = 0;
    this.queue.clear;
  }

  *nextTimeOnGrid { |quant = 1, phase = 0|
    var offset;

    if (quant < 0) { quant = beatsPerBar * quant.neg };
    offset = baseBarBeat + phase;
    ^roundUp(this.beats - offset, quant) + offset;
  }

  *beatsPerBar_ { |newBeatsPerBar = 4|
    this.setMeterAtBeat(newBeatsPerBar, beats)
  }

  *setMeterAtBeat { |newBeatsPerBar, beats|
    // Bar must be integer valued when meter changes or confusion results later.
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
    if (tempo.isNil, {
      ^0;
    }, {
      ^seconds * tempo;
    });
  }

  *elapsedBeats { ^beats }
  *seconds { ^startTime.notNil.if(Main.elapsedTime - startTime, nil) }

  // Updates current clock state based on TICK message from server
  *prTick { |sTempo|
    tempo = sTempo;
    ticks = ticks + 1;
    beats = beats + ticksPerBeat.reciprocal;
    this.prScheduleFromQueue;
  }

  // Check queue if there is any Task that should be scheduled using the
  // SystemClock.  Only tasks that need to be executed within the current tick
  // and the next one are scheduled with that clock.
  *prScheduleFromQueue {
    if (started.not, {^nil});

    //[].debug("prScheduleFromQueue");
    while ({(queue.topPriority??{inf}) - ticks < 1}, {
      var task, delta, accumDelta, tickDelta;

      //(queue.topPriority - ticks).debug("queue.topPriority - ticks");
      //(tempo * ticksPerBeat).debug("tempo * ticksPerBeat == 1/tickDelta");

      delta = (queue.topPriority - ticks) / (tempo * ticksPerBeat);
      tickDelta = (tempo * ticksPerBeat).reciprocal;
      accumDelta = delta;
      task = queue.pop;

      // Schedule a new Task with SystemClock, for all Tasks that must be
      // executed until the next tick.
      SystemClock.sched(delta, {
        // FIXME: beats should include elapsed seconds from last tick
        delta = task.value(this.beats);

        if (delta.isNumber, {
          accumDelta = accumDelta + delta;

          //accumDelta.debug("[accumDelta] < tickDelta");
          //tickDelta.debug("accumDelta < [tickDelta]");

          if (accumDelta < tickDelta, {
            // Return delta so that SystemClock reschedules task
            delta;
          }, {
            // Schedule again using TempoSyncClock
            //accumDelta.debug("going to schedule using queue");
            queue.put((accumDelta * ticksPerBeat) + ticks, task);
            // and return nil to avoid SystemClock to reschedule
            nil;
          });
        }, {
          nil;
        });
      });

    }); // while
  }
}
