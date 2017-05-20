# TempoSync quark for Supercollider

This is a quark that provides a client for
[temposyncd](https://github.com/munshkr/temposyncd).

This is a *work in progress*.

## Usage

For now there is a `TempoSyncClock` class that roughly provides the same set of
methods that a `TempoClock` does.

You could set is as the default clock like this:

```supercollider
TempoClock.default = TempoSyncClock.new

(
Pdef(\test,
  Pbind(
    \degree, Pshuf([0, 2, 3, 5, 7, 6], inf),
    \delta, 0.25,
    \dur, 0.25,
    \instrument, \default,
  )
).play(c, quant: 4);
)
```

## Contributing

Bug reports and pull requests are welcome on GitHub at
https://github.com/munshkr/temposync-es. This project is intended to be a safe,
welcoming space for collaboration, and contributors are expected to adhere to
the [Contributor Covenant](http://contributor-covenant.org) code of conduct.

## License

temposync-es is under the Apache 2.0 license. See the [LICENSE](LICENSE) file
for details.
