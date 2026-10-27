package io.github.marvinwiget.mucke.sound;

public final class Transport {
    public static final int SAMPLE_RATE = 48_000;

    private long anchorFrame;
    private long anchorBeat;
    private double bpm;

    public Transport(double bpm) {
        this.bpm = bpm;
    }

    public long frameAtBeat(long beat) {
        return anchorFrame
            + Math.round((beat - anchorBeat) * 60.0 * SAMPLE_RATE / bpm);
    }

    public double beatAtFrame(long frame) {
        return anchorBeat
            + (frame - anchorFrame) * bpm / (60.0 * SAMPLE_RATE);
    }

    public long nextBeat(long frame) {
        return (long) Math.floor(beatAtFrame(frame) + 1e-9) + 1;
    }

    public long nextBeatFrame(long frame) {
        return frameAtBeat(nextBeat(frame));
    }

    public void changeTempoAt(long frame, long beat, double newBpm) {
        anchorFrame = frame;
        anchorBeat = beat;
        bpm = newBpm;
    }
}