package io.github.marvinwiget.mucke.interpreter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Interpreter {
    public CompileResult compile(String source) {
        return compile(source, Path.of("").toAbsolutePath());
    }

    public CompileResult compile(String source, Path baseDir) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        List<Token> tokens = new Scanner(source, diagnostics).scanTokens();
        if (!diagnostics.isEmpty()) return new CompileResult(null, diagnostics);

        Program program = new Parser(tokens, diagnostics).parse();
        if (program == null || !diagnostics.isEmpty()) {
            return new CompileResult(null, diagnostics);
        }

        if (!new Validator(diagnostics).validate(program)) {
            return new CompileResult(null, diagnostics);
        }

        try {
            return new CompileResult(
                new ScoreCompiler().compile(program, baseDir), diagnostics);
        } catch (ScoreCompiler.CompileError error) {
            diagnostics.add(new Diagnostic(error.line, error.getMessage()));
            return new CompileResult(null, diagnostics);
        }
    }
}