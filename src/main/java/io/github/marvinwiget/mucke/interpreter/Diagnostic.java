package io.github.marvinwiget.mucke.interpreter;

public final class Diagnostic {
    private final int line;
    private final String message;

    public Diagnostic(int line, String message) {
        this.line = line;
        this.message = message;
    }

    public int getLine() {
        return line;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "[line " + line + "] " + message;
    }
}