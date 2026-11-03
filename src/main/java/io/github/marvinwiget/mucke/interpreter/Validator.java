package io.github.marvinwiget.mucke.interpreter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class Validator {
    private final List<Diagnostic> diagnostics;

    Validator(List<Diagnostic> diagnostics) {
        this.diagnostics = diagnostics;
    }

    boolean validate(Program program) {
        Set<String> settings = new HashSet<>();
        Set<String> instruments = new HashSet<>();
        int errorsBefore = diagnostics.size();

        for (ConfigEntry entry : program.config) {
            if (entry instanceof ConfigEntry.Setting setting) {
                if (!settings.add(setting.name.lexeme)) {
                    error(setting.name, "Duplicate setting '" + setting.name.lexeme + "'.");
                }
                validateSetting(setting.name, setting.value);
            } else {
                ConfigEntry.Instrument instrument = (ConfigEntry.Instrument) entry;
                if (!instruments.add(instrument.name.lexeme)) {
                    error(instrument.name, "Duplicate instrument '" + instrument.name.lexeme + "'.");
                }
                validateInstrument(instrument);
            }
        }

        Set<String> active = new HashSet<>();
        for (Token name : program.main) {
            if (!instruments.contains(name.lexeme)) {
                error(name, "Unknown instrument '" + name.lexeme + "'.");
            } else if (!active.add(name.lexeme)) {
                error(name, "Instrument '" + name.lexeme + "' is already active.");
            }
        }

        return diagnostics.size() == errorsBefore;
    }

    private void validateInstrument(ConfigEntry.Instrument instrument) {
        Set<String> fields = new HashSet<>();
        Field pattern = null;
        for (Field field : instrument.fields) {
            if (!fields.add(field.name.lexeme)) {
                error(field.name, "Duplicate field '" + field.name.lexeme + "'.");
            }
            switch (field.name.lexeme) {
                case "sample" -> {
                    if (!(literal(field.value) instanceof String path) || path.isBlank()) {
                        error(field.name, "Sample must be a non-empty file path string.");
                    }
                }
                case "synth" -> {
                    Double program = number(field.value);
                    if (!wholeNumber(program, 0, 127)) {
                        error(field.name, "Synth program must be a whole number from 0 to 127.");
                    }
                }
                case "transpose" -> {
                    Double semitones = number(field.value);
                    if (!wholeNumber(semitones, -127, 127)) {
                        error(field.name, "Transpose must be a whole number of semitones.");
                    }
                }
                case "pattern" -> pattern = field;
                default -> error(field.name,
                    "Unknown instrument field '" + field.name.lexeme + "'.");
            }
        }
        boolean sample = fields.contains("sample");
        boolean synth = fields.contains("synth");
        if (sample == synth) {
            error(instrument.name, "Instrument requires exactly one of sample or synth.");
        }
        if (fields.contains("transpose") && !synth) {
            error(instrument.name, "Only synth instruments can use transpose.");
        }
        if (pattern == null) {
            error(instrument.name, "Instrument requires a pattern.");
        } else if (sample != synth) {
            validatePattern(pattern.name, pattern.value, synth);
        }
    }

    private void validateSetting(Token name, Expr value) {
        switch (name.lexeme) {
            case "bpm" -> {
                Double bpm = number(value);
                if (bpm == null || !Double.isFinite(bpm) || bpm < 1 || bpm > 1000) {
                    error(name, "BPM must be between 1 and 1000.");
                }
            }
            case "meter" -> {
                if (!(value instanceof Expr.Ratio ratio)
                        || !positiveWholeNumber(ratio.numerator)
                        || !positiveWholeNumber(ratio.denominator)) {
                    error(name, "Meter must be a ratio such as 4/4.");
                }
            }
            default -> error(name, "Unknown setting '" + name.lexeme + "'.");
        }
    }

    private void validatePattern(Token name, Expr value, boolean synth) {
        if (!(value instanceof Expr.ListLiteral list) || list.elements.isEmpty()) {
            error(name, "Pattern must be a non-empty list.");
            return;
        }
        for (Expr element : list.elements) {
            if (synth) {
                if (element instanceof Expr.Rest || element instanceof Expr.Note) continue;
                if (element instanceof Expr.ListLiteral chord && !chord.elements.isEmpty()
                        && chord.elements.stream().allMatch(note -> note instanceof Expr.Note)) {
                    continue;
                }
                error(name, "Synth pattern steps must be notes, chords, or rest.");
                return;
            } else if (!(element instanceof Expr.Hit) && !(element instanceof Expr.Rest)) {
                error(name, "Sample pattern steps must be hit or rest.");
                return;
            }
        }
    }

    private static boolean wholeNumber(Double value, int minimum, int maximum) {
        return value != null && Double.isFinite(value)
            && value >= minimum && value <= maximum
            && value == Math.floor(value);
    }

    private static Object literal(Expr value) {
        return value instanceof Expr.Literal literal ? literal.value : null;
    }

    private static Double number(Expr value) {
        Object valueObject = literal(value);
        return valueObject instanceof Double number ? number : null;
    }

    private static boolean positiveWholeNumber(Token token) {
        Double number = (Double) token.literal;
        return Double.isFinite(number) && number >= 1
            && number <= 1000 && number == Math.floor(number);
    }

    private void error(Token token, String message) {
        diagnostics.add(new Diagnostic(token.line, message));
    }
}
