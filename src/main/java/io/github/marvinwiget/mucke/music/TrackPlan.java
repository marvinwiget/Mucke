package io.github.marvinwiget.mucke.music;

import java.nio.file.Path;
import java.util.List;

public final class TrackPlan {
    public final String name;
    public final Path sample;
    public final Integer synthProgram;
    public final int transpose;
    public final List<PatternStep> pattern;

    private TrackPlan(String name, Path sample, Integer synthProgram,
                      int transpose, List<PatternStep> pattern) {
        this.name = name;
        this.sample = sample;
        this.synthProgram = synthProgram;
        this.transpose = transpose;
        this.pattern = List.copyOf(pattern);
    }

    public static TrackPlan sample(String name, Path path,
                                   List<PatternStep> pattern) {
        return new TrackPlan(name, path, null, 0, pattern);
    }

    public static TrackPlan synth(String name, int program, int transpose,
                                  List<PatternStep> pattern) {
        return new TrackPlan(name, null, program, transpose, pattern);
    }

    public boolean isSynth() {
        return synthProgram != null;
    }
}
