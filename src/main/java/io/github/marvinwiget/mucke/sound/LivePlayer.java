package io.github.marvinwiget.mucke.sound;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import io.github.marvinwiget.mucke.music.PlaybackPlan;
import io.github.marvinwiget.mucke.music.TrackPlan;

public final class LivePlayer implements AutoCloseable {
    private static final int BLOCK_FRAMES = 256;
    private static final AudioFormat FORMAT =
        new AudioFormat(Transport.SAMPLE_RATE, 16, 2, true, false);

    public static final class PreparedPlan {
        final PlaybackPlan plan;
        final List<PreparedTrack> tracks;

        PreparedPlan(PlaybackPlan plan, List<PreparedTrack> tracks) {
            this.plan = plan;
            this.tracks = tracks;
        }
    }

    static final class PreparedTrack {
        final TrackPlan plan;
        final float[] sample;

        PreparedTrack(TrackPlan plan, float[] sample) {
            this.plan = plan;
            this.sample = sample;
        }
    }

    private static final class Voice {
        final float[] sample;
        final double rate;
        final double gain;
        double position;

        Voice(float[] sample, double rate, double gain) {
            this.sample = sample;
            this.rate = rate;
            this.gain = gain;
        }

        boolean done() {
            return position >= sample.length;
        }

        double next() {
            int first = (int) position;
            int second = Math.min(first + 1, sample.length - 1);
            double fraction = position - first;
            double value = sample[first] * (1 - fraction)
                + sample[second] * fraction;
            position += rate;
            return value * gain;
        }
    }

    private final SampleLibrary samples = new SampleLibrary();
    private final AtomicReference<PreparedPlan> pending = new AtomicReference<>();
    private volatile boolean running;
    private SourceDataLine line;
    private Thread audioThread;

    // Call from the app's worker thread, never the JavaFX thread.
    public PreparedPlan prepare(PlaybackPlan plan)
            throws IOException, UnsupportedAudioFileException {
        List<PreparedTrack> tracks = new ArrayList<>();
        for (TrackPlan track : plan.tracks) {
            tracks.add(new PreparedTrack(track, samples.load(track.sample)));
        }
        return new PreparedPlan(plan, List.copyOf(tracks));
    }

    public synchronized void submit(PreparedPlan plan)
            throws LineUnavailableException {
        pending.set(plan);
        if (running || plan.tracks.isEmpty()) return;

        line = AudioSystem.getSourceDataLine(FORMAT);
        line.open(FORMAT, BLOCK_FRAMES * FORMAT.getFrameSize() * 4);
        line.start();
        running = true;
        audioThread = new Thread(this::render, "mucke-audio");
        audioThread.setDaemon(true);
        audioThread.start();
    }

    private void render() {
        Transport clock = new Transport(120);
        BeatSequencer sequencer = new BeatSequencer();
        List<Voice> voices = new ArrayList<>();
        byte[] buffer = new byte[BLOCK_FRAMES * FORMAT.getFrameSize()];
        long frame = 0;
        long nextBeatToSchedule = 0;
        long switchFrame = Long.MAX_VALUE;
        long switchBeat = 0;
        boolean hasPlan = false;
        PreparedPlan staged = null;

        while (running) {
            PreparedPlan newest = pending.getAndSet(null);
            if (newest != null) {
                staged = newest;
                switchBeat = hasPlan ? nextBeatToSchedule : 0;
                switchFrame = hasPlan ? clock.frameAtBeat(switchBeat) : frame;
            }

            for (int i = 0; i < BLOCK_FRAMES; i++, frame++) {
                if (staged != null && frame >= switchFrame) {
                    clock.changeTempoAt(frame, switchBeat, staged.plan.bpm);
                    sequencer.update(staged, switchBeat);
                    staged = null;
                    hasPlan = true;
                }

                while (hasPlan && frame >= clock.frameAtBeat(nextBeatToSchedule)) {
                    for (BeatSequencer.Trigger trigger :
                            sequencer.atBeat(nextBeatToSchedule)) {
                        TrackPlan track = trigger.track.plan;
                        double rate = track.pitch * Math.pow(2,
                            (trigger.note.midiNote - track.rootNote) / 12.0);
                        voices.add(new Voice(trigger.track.sample, rate, track.gain));
                    }
                    nextBeatToSchedule++;
                }

                double mixed = 0;
                for (int voice = voices.size() - 1; voice >= 0; voice--) {
                    Voice current = voices.get(voice);
                    if (current.done()) {
                        voices.remove(voice);
                    } else {
                        mixed += current.next();
                    }
                }

                int pcm = (int) Math.round(Math.max(-1, Math.min(1, mixed)) * 32767);
                int offset = i * 4;
                buffer[offset] = (byte) pcm;
                buffer[offset + 1] = (byte) (pcm >> 8);
                buffer[offset + 2] = (byte) pcm;
                buffer[offset + 3] = (byte) (pcm >> 8);
            }

            int written = 0;
            while (running && written < buffer.length) {
                int count = line.write(buffer, written, buffer.length - written);
                if (count <= 0) break;
                written += count;
            }
        }
    }

    @Override
    public synchronized void close() {
        running = false;
        pending.set(null);
        if (line != null) {
            line.stop();
            line.flush();
            line.close();
        }
    }
}
