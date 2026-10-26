package io.github.marvinwiget.mucke.interpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CompileResult {
    private final boolean valid;
    private final List<Diagnostic> diagnostics;

    CompileResult(boolean valid, List<Diagnostic> diagnostics) {
        this.valid = valid;
        this.diagnostics = Collections.unmodifiableList(new ArrayList<>(diagnostics));
    }

    public boolean isValid() {
        return valid;
    }

    public List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }
}