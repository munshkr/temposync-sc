// Register /tick responder
n = NetAddr("127.0.0.1");
OSCdef(\tick, {|msg, time, addr, recvPort| time.postln}, "/tempoclock/tick", NetAddr("127.0.0.1"));

// Free responder
OSCdef(\tick).clear
OSCdef(\tick).free

// To debug all incoming OSC messages
OSCFunc.trace
OSCFunc.trace(false)

//

c = TempoSyncClock.new

(
t = Pbind(
	\midinote, Pseq([60, 62, 64, 65, 67, 65, 64, 62], inf),
	\delta, 0.25,
	\dur, 0.25,
	\instrument, \default,
).play(c, quant: 1);
)
t.stop

// Test .sched

(
c.sched(0.0, { |time|
	["playing for ", time].postln;
	1.0
});
)