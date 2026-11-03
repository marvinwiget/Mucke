package io.github.marvinwiget.mucke.sound;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

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

    private final Map<Path, AudioClip> samples = new HashMap<>();
    private final SynthPlayer synth = new SynthPlayer();
    private final BeatSequencer sequencer = new BeatSequencer();
    private final PauseTransition clock = new PauseTransition();
    private PreparedPlan current;
    private PreparedPlan pending;
    private long beat;
    private boolean running;
    private boolean closed;

    public LivePlayer() {
        clock.setOnFinished(event -> tick());
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
        if (!running) {
            running = true;
            tick();
        }
    }

    private void tick() {
        if (pending != null) {
            current = pending;
            pending = null;
            sequencer.update(current.plan, beat);
        }

        Map<String, PatternStep> steps = sequencer.atBeat();
        synth.endBeat();
        int channel = 0;
        for (TrackPlan track : current.plan.tracks) {
            PatternStep step = steps.get(track.name);
            if (track.isSynth()) {
                synth.play(channel++, track.synthProgram, track.transpose, step.notes);
            } else if (step.hit) {
                current.clips.get(track.name).play();
            }
        }
        beat++;
        clock.setDuration(Duration.millis(
            60_000.0 * 4 / (current.plan.bpm * current.plan.meterBottom)));
        clock.playFromStart();
    }

    @Override
    public void close() {
        closed = true;
        clock.stop();
        running = false;
        if (current != null) {
            current.clips.values().forEach(AudioClip::stop);
        }
        synth.close();
        current = null;
        pending = null;
    }
}
