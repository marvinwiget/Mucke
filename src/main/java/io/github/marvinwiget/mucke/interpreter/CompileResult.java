package io.github.marvinwiget.mucke.interpreter;

import java.util.List;
import io.github.marvinwiget.mucke.music.PlaybackPlan;

public final class CompileResult {
    private final PlaybackPlan plan;
    private final List<Diagnostic> diagnostics;

    CompileResult(PlaybackPlan plan, List<Diagnostic> diagnostics) {
        this.plan = plan;
        this.diagnostics = List.copyOf(diagnostics);
    }

    public boolean isValid() {
        return plan != null;
    }

    public PlaybackPlan getPlan() {
        return plan;
    }

    public List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }
}