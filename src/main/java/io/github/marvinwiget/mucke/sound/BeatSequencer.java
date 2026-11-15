package io.github.marvinwiget.mucke.sound;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.marvinwiget.mucke.music.PlaybackPlan;
import io.github.marvinwiget.mucke.music.PatternStep;
import io.github.marvinwiget.mucke.music.TrackPlan;

// One cursor per instrument; a single caller advances all cursors once per beat.
final class BeatSequencer {
    private static final class Track {
        List<PatternStep> pattern;
        List<PatternStep> pendingPattern;
        int nextStep;
        boolean synth;
        int transpose;
        int pendingTranspose;

        Track(TrackPlan plan, int nextStep) {
            this.pattern = plan.pattern;
            this.nextStep = nextStep;
            this.synth = plan.isSynth();
            this.transpose = plan.transpose;
        }
    }

    private Map<String, Track> active = new LinkedHashMap<>();

    void update(PlaybackPlan plan, long beat) {
        int barStep = (int) Math.floorMod(beat, plan.meterTop);
        Map<String, Track> next = new LinkedHashMap<>();

        for (TrackPlan incoming : plan.tracks) {
            Track track = active.get(incoming.name);
            if (track == null) {
                track = new Track(incoming, barStep % incoming.pattern.size());
            } else if (track.synth != incoming.isSynth()) {
                track.pattern = incoming.pattern;
                track.pendingPattern = null;
                track.nextStep = barStep % incoming.pattern.size();
                track.synth = incoming.isSynth();
                track.transpose = incoming.transpose;
            } else if (track.pattern.size() == incoming.pattern.size()
                    || track.nextStep == 0) {
                track.pattern = incoming.pattern;
                track.pendingPattern = null;
                track.transpose = incoming.transpose;
            } else {
                track.pendingPattern = incoming.pattern;
                track.pendingTranspose = incoming.transpose;
            }
            next.put(incoming.name, track);
        }

        active = next; // Removed instruments have no future hits.
    }

    Map<String, PatternStep> atBeat() {
        Map<String, PatternStep> steps = new LinkedHashMap<>();
        for (Map.Entry<String, Track> entry : active.entrySet()) {
            Track track = entry.getValue();
            if (track.pendingPattern != null && track.nextStep == 0) {
                track.pattern = track.pendingPattern;
                track.pendingPattern = null;
                track.transpose = track.pendingTranspose;
            }
            steps.put(entry.getKey(), track.pattern.get(track.nextStep));
            track.nextStep = (track.nextStep + 1) % track.pattern.size();
        }
        return steps;
    }

    int transposeFor(String name) {
        return active.get(name).transpose;
    }
}
