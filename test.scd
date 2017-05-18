// Register /tick URL
n = NetAddr.localAddr;
OSCdef(\tick, {|msg, time, addr, recvPort| \tick.postln}, "/temposync/tick", n);

// To test, configure a client and send a tick message to supercollider port
n.sendMsg("/temposync/tick", "que tal");

thisProcess.openPorts