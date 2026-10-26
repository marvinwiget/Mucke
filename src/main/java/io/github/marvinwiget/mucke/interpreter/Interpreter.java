package io.github.marvinwiget.mucke.interpreter;

import java.util.ArrayList;
import java.util.List;

public final class Interpreter {
    public CompileResult compile(String source) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        List<Token> tokens = new Scanner(source, diagnostics).scanTokens();
        if (!diagnostics.isEmpty()) {
            return new CompileResult(false, diagnostics);
        }

        Program program = new Parser(tokens, diagnostics).parse();
        if (program == null || !diagnostics.isEmpty()) {
            return new CompileResult(false, diagnostics);
        }

        boolean valid = new Validator(diagnostics).validate(program);
        return new CompileResult(valid && diagnostics.isEmpty(), diagnostics);
    }
}