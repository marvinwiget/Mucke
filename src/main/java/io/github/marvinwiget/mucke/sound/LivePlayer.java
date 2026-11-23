package io.github.marvinwiget.mucke.sound;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.sound.midi.MidiUnavailableException;

import io.github.marvinwiget.mucke.music.PlaybackPlan;
import io.github.marvinwiget.mucke.music.PatternStep;
import io.github.marvinwiget.mucke.music.TrackPlan;
import javafx.animation.PauseTransition;
import javafx.scene.media.AudioClip;
import javafx.util.Duration;

public final class LivePlayer implements AutoCloseable {
    public static final class PreparedPlan {
        final PlaybackPlan plan;
        final Map<String, AudioClip> clips;

        PreparedPlan(PlaybackPlan plan, Map<String, AudioClip> clips) {
            this.plan = plan;
            this.clips = clips;
        }
    }

    private final Map<Path, AudioClip> samples = new ConcurrentHashMap<>();
    private final Map<String, Double> volumes = new HashMap<>();
    private final List<PauseTransition> queuedSamples = new ArrayList<>();
    private final SynthPlayer synth = new SynthPlayer();
    private final BeatSequencer sequencer = new BeatSequencer();
    private final BeatClock beatClock = new BeatClock();
    private final PauseTransition clock = new PauseTransition();
    private PreparedPlan current;
    private PreparedPlan pending;
    private long beat;
    private boolean closed;
    private double sampleDelayMillis = -1;
    private Consumer<PlaybackPlan> planListener;

    public LivePlayer() {
        clock.setOnFinished(event -> tick());
    }

    public void setPlanListener(Consumer<PlaybackPlan> listener) {
        planListener = listener;
    }

    public double getVolume(String name) {
        return volumes.getOrDefault(name, 1.0);
    }

    public double getSampleDelayMillis() {
        if (sampleDelayMillis >= 0) return sampleDelayMillis;
        return Math.max(0, Math.min(200, synth.latencyMicros() / 2000.0));
    }

    public void setSampleDelayMillis(double delay) {
        sampleDelayMillis = delay;
    }

    private double volumeFor(String name) {
        return current.plan.slots.containsValue(name) ? getVolume(name) : 1.0;
    }

    public void setVolume(String name, double volume) {
        volumes.put(name, volume);
        if (current == null || !current.plan.slots.containsValue(name)) return;

        int channel = 0;
        for (TrackPlan track : current.plan.tracks) {
            if (!track.isSynth()) continue;
            if (track.name.equals(name)) {
                synth.setVolume(channel, volume);
                return;
            }
            channel++;
        }
    }

    // The app's single worker calls prepare(), so sample loading stays off the UI thread.
    public PreparedPlan prepare(PlaybackPlan plan)
            throws IOException, MidiUnavailableException {
        Map<String, AudioClip> clips = new HashMap<>();
        int synthTracks = 0;
        for (TrackPlan track : plan.tracks) {
            if (track.isSynth()) {
                synthTracks++;
                continue;
            }
            if (!Files.isRegularFile(track.sample)) {
                throw new IOException("Sample not found: " + track.sample);
            }
            clips.put(track.name, samples.computeIfAbsent(track.sample,
                path -> new AudioClip(path.toUri().toString())));
        }
        synth.prepare(synthTracks);
        return new PreparedPlan(plan, clips);
    }

    // Called on the JavaFX thread. The latest valid plan takes effect on the next beat.
    public void submit(PreparedPlan plan) {
        if (closed) return;
        pending = plan;
        if (current == null) {
            beatClock.start(System.nanoTime());
            tick();
        }
    }

    private void tick() {
        PlaybackPlan appliedPlan = null;
        if (pending != null) {
            current = pending;
            pending = null;
            sequencer.update(current.plan, beat);
            appliedPlan = current.plan;
        }

        Map<String, PatternStep> steps = sequencer.atBeat();
        synth.endBeat();
        // Send MIDI first because the synth can buffer more audio than AudioClip.
        int channel = 0;
        for (TrackPlan track : current.plan.tracks) {
            if (track.isSynth()) {
                PatternStep step = steps.get(track.name);
                synth.play(channel++, track.synthProgram,
                    sequencer.transposeFor(track.name), volumeFor(track.name), step.notes);
            }
        }
        for (TrackPlan track : current.plan.tracks) {
            if (!track.isSynth() && steps.get(track.name).hit) {
                playSample(track.name, current.clips.get(track.name));
            }
        }
        beat++;
        long beatLengthNanos = Math.round(
            60_000_000_000.0 * 4 / (current.plan.bpm * current.plan.meterBottom));
        long delayNanos = beatClock.nextDelay(System.nanoTime(), beatLengthNanos);
        clock.setDuration(Duration.millis(delayNanos / 1_000_000.0));
        clock.playFromStart();
        if (appliedPlan != null && planListener != null) planListener.accept(appliedPlan);
    }

    private void playSample(String name, AudioClip clip) {
        double delay = getSampleDelayMillis();
        if (delay <= 0) {
            startSample(name, clip);
            return;
        }
        PauseTransition queued = new PauseTransition(Duration.millis(delay));
        queuedSamples.add(queued);
        queued.setOnFinished(event -> {
            queuedSamples.remove(queued);
            if (!closed && current.clips.get(name) == clip) startSample(name, clip);
        });
        queued.play();
    }

    private void startSample(String name, AudioClip clip) {
        double volume = volumeFor(name);
        if (volume > 0) clip.play(volume);
    }

    @Override
    public void close() {
        closed = true;
        clock.stop();
        for (PauseTransition queued : queuedSamples) queued.stop();
        queuedSamples.clear();
        samples.values().forEach(AudioClip::stop);
        synth.close();
        current = null;
        pending = null;
    }
}
