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

        Track(List<PatternStep> pattern, int nextStep, boolean synth) {
            this.pattern = pattern;
            this.nextStep = nextStep;
            this.synth = synth;
        }
    }

    private Map<String, Track> active = new LinkedHashMap<>();

    void update(PlaybackPlan plan, long beat) {
        int barStep = (int) Math.floorMod(beat, plan.meterTop);
        Map<String, Track> next = new LinkedHashMap<>();

        for (TrackPlan incoming : plan.tracks) {
            Track track = active.get(incoming.name);
            if (track == null) {
                track = new Track(incoming.pattern,
                    barStep % incoming.pattern.size(), incoming.isSynth());
            } else if (track.synth != incoming.isSynth()) {
                track.pattern = incoming.pattern;
                track.pendingPattern = null;
                track.nextStep = barStep % incoming.pattern.size();
                track.synth = incoming.isSynth();
            } else if (track.pattern.size() == incoming.pattern.size()
                    || track.nextStep == 0) {
                track.pattern = incoming.pattern;
                track.pendingPattern = null;
            } else {
                track.pendingPattern = incoming.pattern;
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
            }
            steps.put(entry.getKey(), track.pattern.get(track.nextStep));
            track.nextStep = (track.nextStep + 1) % track.pattern.size();
        }
        return steps;
    }
}
