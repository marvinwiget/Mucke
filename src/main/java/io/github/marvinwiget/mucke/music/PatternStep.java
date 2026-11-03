package io.github.marvinwiget.mucke.music;

import java.util.List;

public final class PatternStep {
    private static final PatternStep HIT = new PatternStep(true, List.of());
    private static final PatternStep REST = new PatternStep(false, List.of());

    public final boolean hit;
    public final List<Integer> notes;

    private PatternStep(boolean hit, List<Integer> notes) {
        this.hit = hit;
        this.notes = List.copyOf(notes);
    }

    public static PatternStep hit() {
        return HIT;
    }

    public static PatternStep rest() {
        return REST;
    }

    public static PatternStep notes(List<Integer> notes) {
        return new PatternStep(false, notes);
    }
}
