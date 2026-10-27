package io.github.marvinwiget.mucke.sound;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.marvinwiget.mucke.music.NoteEvent;
import io.github.marvinwiget.mucke.music.TrackPlan;

// Chooses notes for one absolute beat at a time; it never queues future notes.
final class BeatSequencer {
    static final class Trigger {
        final LivePlayer.PreparedTrack track;
        final NoteEvent note;

        Trigger(LivePlayer.PreparedTrack track, NoteEvent note) {
            this.track = track;
            this.note = note;
        }
    }

    private static final class Group {
        List<LivePlayer.PreparedTrack> tracks;
        long originBeat;
        List<LivePlayer.PreparedTrack> queued;
        long queuedBeat;
        int queuedIndex;

        Group(List<LivePlayer.PreparedTrack> tracks, long originBeat) {
            this.tracks = tracks;
            this.originBeat = originBeat;
        }
    }

    private Map<String, Group> active = new LinkedHashMap<>();

    void update(LivePlayer.PreparedPlan plan, long beat) {
        Map<String, List<LivePlayer.PreparedTrack>> incoming = new LinkedHashMap<>();
        for (LivePlayer.PreparedTrack track : plan.tracks) {
            incoming.computeIfAbsent(track.plan.name, ignored -> new ArrayList<>())
                .add(track);
        }

        Map<String, Group> next = new LinkedHashMap<>();
        for (Map.Entry<String, List<LivePlayer.PreparedTrack>> entry : incoming.entrySet()) {
            List<LivePlayer.PreparedTrack> tracks = entry.getValue();
            Group group = active.get(entry.getKey());
            if (group == null) {
                next.put(entry.getKey(), new Group(tracks,
                    originForNewGroup(tracks, beat, plan.plan.meterTop)));
                continue;
            }

            int current = activeSegment(group, beat);
            if (current < 0) {
                // A finished or not-yet-started segment has no cycle to finish.
                group.tracks = tracks;
                group.queued = null;
            } else {
                TrackPlan oldTrack = group.tracks.get(current).plan;
                int replacementIndex = Math.min(current, tracks.size() - 1);
                TrackPlan newTrack = tracks.get(replacementIndex).plan;
                long oldStart = group.originBeat + oldTrack.startBeat;

                if (oldTrack.patternBeats == newTrack.patternBeats) {
                    group.originBeat = oldStart - newTrack.startBeat;
                    group.tracks = tracks;
                    group.queued = null;
                } else {
                    long elapsed = beat - oldStart;
                    long remainder = elapsed % oldTrack.patternBeats;
                    long boundary = remainder == 0
                        ? beat : beat + oldTrack.patternBeats - remainder;
                    if (boundary == beat) {
                        group.originBeat = beat - newTrack.startBeat;
                        group.tracks = tracks;
                        group.queued = null;
                    } else {
                        // Another valid edit replaces this pending version.
                        group.queued = tracks;
                        group.queuedBeat = boundary;
                        group.queuedIndex = replacementIndex;
                    }
                }
            }
            next.put(entry.getKey(), group);
        }
        active = next; // Missing instruments stop producing future notes.
    }

    private long originForNewGroup(List<LivePlayer.PreparedTrack> tracks,
                                   long beat, long beatsPerBar) {
        TrackPlan first = tracks.get(0).plan;
        Group barReference = null;
        TrackPlan barTrack = null;
        for (Group group : active.values()) {
            int current = activeSegment(group, beat);
            if (current < 0) continue;
            TrackPlan reference = group.tracks.get(current).plan;
            if (reference.patternBeats != first.patternBeats) {
                if (barReference == null) {
                    barReference = group;
                    barTrack = reference;
                }
                continue;
            }

            long position = beat - group.originBeat - reference.startBeat;
            long step = Math.floorMod(position, first.patternBeats);
            return beat - step - first.startBeat;
        }
        if (barReference != null) {
            long position = beat - barReference.originBeat - barTrack.startBeat;
            long barStep = Math.floorMod(position, barTrack.patternBeats) % beatsPerBar;
            return beat - barStep % first.patternBeats - first.startBeat;
        }
        return beat - Math.floorMod(beat, first.patternBeats);
    }

    List<Trigger> atBeat(long beat) {
        List<Trigger> result = new ArrayList<>();
        for (Group group : active.values()) {
            if (group.queued != null && beat >= group.queuedBeat) {
                group.originBeat = group.queuedBeat
                    - group.queued.get(group.queuedIndex).plan.startBeat;
                group.tracks = group.queued;
                group.queued = null;
            }

            for (LivePlayer.PreparedTrack track : group.tracks) {
                TrackPlan plan = track.plan;
                long position = beat - group.originBeat - plan.startBeat;
                if (position < 0) continue;
                if (plan.repeats >= 0
                        && position / plan.patternBeats >= plan.repeats) continue;

                int step = (int) (position % plan.patternBeats);
                for (NoteEvent note : plan.notes) {
                    if (note.step == step) result.add(new Trigger(track, note));
                }
            }
        }
        return result;
    }

    private static int activeSegment(Group group, long beat) {
        for (int i = 0; i < group.tracks.size(); i++) {
            TrackPlan plan = group.tracks.get(i).plan;
            long position = beat - group.originBeat - plan.startBeat;
            if (position >= 0 && (plan.repeats < 0
                    || position / plan.patternBeats < plan.repeats)) return i;
        }
        return -1;
    }
}
