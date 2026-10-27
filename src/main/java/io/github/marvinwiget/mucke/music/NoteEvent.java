package io.github.marvinwiget.mucke.music;

public final class NoteEvent {
    public final int step;
    public final int midiNote;

    public NoteEvent(int step, int midiNote) {
        this.step = step;
        this.midiNote = midiNote;
    }
}