package io.github.marvinwiget.mucke.sound;

// Keep beat deadlines independent of how long a tick takes to run.
final class BeatClock {
    private long nextBeatNanos;

    void start(long now) {
        nextBeatNanos = now;
    }

    long nextDelay(long now, long beatLengthNanos) {
        nextBeatNanos += beatLengthNanos;
        if (nextBeatNanos <= now) {
            // A long UI stall cannot be recovered by playing several beats at once.
            nextBeatNanos = now + beatLengthNanos;
        }
        return nextBeatNanos - now;
    }
}
