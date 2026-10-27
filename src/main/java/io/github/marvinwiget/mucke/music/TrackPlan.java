package io.github.marvinwiget.mucke.music;

import java.nio.file.Path;
import java.util.List;

public final class TrackPlan {
    public final String name;
    public final Path sample;
    public final int rootNote;
    public final double gain;
    public final double pitch;
    public final long startBeat;
    public final long repeats; // -1 means forever.
    public final int patternBeats;
    public final List<NoteEvent> notes;

    public TrackPlan(String name, Path sample, int rootNote, double gain,
                     double pitch, long startBeat, long repeats,
                     int patternBeats, List<NoteEvent> notes) {
        this.name = name;
        this.sample = sample;
        this.rootNote = rootNote;
        this.gain = gain;
        this.pitch = pitch;
        this.startBeat = startBeat;
        this.repeats = repeats;
        this.patternBeats = patternBeats;
        this.notes = List.copyOf(notes);
    }
}