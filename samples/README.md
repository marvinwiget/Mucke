# Mucke samples

These 170 sounds come from [Sample Pi](https://github.com/alex-esc/sample-pi) at commit `f14596be37abf227bf77d198c9b0b60d9eb4dca9`. Its [README](SAMPLE_PI_CREDITS.md) identifies the samples as public domain under [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) and lists their original creators.

The playable samples were converted to 48 kHz, 16-bit mono PCM WAV for Mucke's audio player. Original names and folders are preserved, except `noise loops` became `noise_loops`. The upstream one-cycle synthesizer waveforms are omitted because Mucke currently plays samples as one-shots; the `bass/` folder contains pitched synth sounds.

Sample paths are relative to the `.mucke` file. For a file at the project root, use `sample: "samples/drums/one-shots/kick/bd_808.wav"` or `sample: "samples/bass/bass_thick_c.wav"`.
