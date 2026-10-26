package io.github.marvinwiget.mucke.interpreter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// import io.github.marvinwiget.mucke.interpreter.ConfigEntry.Instrument;
// import io.github.marvinwiget.mucke.interpreter.ConfigEntry.Setting;
// import io.github.marvinwiget.mucke.interpreter.Expr.ListLiteral;
// import io.github.marvinwiget.mucke.interpreter.Expr.Literal;
// import io.github.marvinwiget.mucke.interpreter.Expr.Note;
// import io.github.marvinwiget.mucke.interpreter.Expr.Ratio;
// import io.github.marvinwiget.mucke.interpreter.Expr.Rest;
// import io.github.marvinwiget.mucke.interpreter.Stmt.Assign;
// import io.github.marvinwiget.mucke.interpreter.Stmt.Call;

class Validator {
    private final Set<String> instruments = new HashSet<>();
    private boolean valid = true;
    private final List<Diagnostic> diagnostics;

    Validator() {
        this(new ArrayList<>());
    }

    Validator(List<Diagnostic> diagnostics) {
        this.diagnostics = diagnostics;
    }

    boolean validate(Program program) {
        Set<String> settings = new HashSet<>();

        for (ConfigEntry entry : program.config) {
            if (entry instanceof ConfigEntry.Setting) {
                ConfigEntry.Setting setting = (ConfigEntry.Setting) entry;
                if (!settings.add(setting.name.lexeme)) {
                    error(setting.name, "Duplicate setting '" + setting.name.lexeme + "'.");
                }
                validateSetting(setting.name, setting.value);
            } else {
                ConfigEntry.Instrument instrument = (ConfigEntry.Instrument) entry;
                if (instrument.name.lexeme.equals("all")) {
                    error(instrument.name, "'all' is reserved for group calls.");
                }
                if (!instruments.add(instrument.name.lexeme)) {
                    error(instrument.name, "Duplicate instrument '" + instrument.name.lexeme + "'.");
                }
                validateInstrument(instrument);
            }
        }

        for (Stmt statement : program.main) {
            if (statement instanceof Stmt.Assign) {
                Stmt.Assign assignment = (Stmt.Assign) statement;
                validateSetting(assignment.name, assignment.value);
            } else if (statement instanceof Stmt.Set) {
                Stmt.Set set = (Stmt.Set) statement;
                requireInstrument(set.object);
                validateField(set.property, set.value);
            } else {
                validateCall((Stmt.Call) statement);
            }
        }

        return valid;
    }

    private void validateInstrument(ConfigEntry.Instrument instrument) {
        Set<String> fields = new HashSet<>();

        for (Field field : instrument.fields) {
            if (!fields.add(field.name.lexeme)) {
                error(field.name, "Duplicate field '" + field.name.lexeme + "'.");
            }
            validateField(field.name, field.value);
        }

        if (!fields.contains("sample")) {
            error(instrument.name, "Instrument requires a sample.");
        }
        if (!fields.contains("pattern")) {
            error(instrument.name, "Instrument requires a pattern.");
        }
    }

    private void validateSetting(Token name, Expr value) {
        switch (name.lexeme) {
            case "bpm":
                requirePositiveNumber(name, value);
                break;
            case "meter":
                requireMeter(name, value);
                break;
            case "volume":
                requireVolume(name, value);
                break;
            default:
                error(name, "Unknown setting '" + name.lexeme + "'.");
        }
    }

    private void validateField(Token name, Expr value) {
        switch (name.lexeme) {
            case "sample":
                Object sample = literal(value);
                if (!(sample instanceof String) || ((String) sample).isEmpty()) {
                    error(name, "Sample must be a non-empty file path string.");
                }
                break;
            case "pattern":
                requirePattern(name, value);
                break;
            case "root_note":
                if (!(value instanceof Expr.Note)) {
                    error(name, "Root note must be a note, such as C2.");
                }
                break;
            case "volume":
                requireVolume(name, value);
                break;
            case "pitch":
                requirePositiveNumber(name, value);
                break;
            case "reverb":
                if (!(literal(value) instanceof Boolean)) {
                    error(name, "Reverb must be true or false.");
                }
                break;
            default:
                error(name, "Unknown instrument field '" + name.lexeme + "'.");
        }
    }

    private void validateCall(Stmt.Call call) {
        if (call.object.lexeme.equals("all")) {
            if (instruments.isEmpty()) {
                error(call.object, "'all' requires at least one instrument.");
            }
        } else {
            requireInstrument(call.object);
        }

        switch (call.method.lexeme) {
            case "play":
            case "stop":
                if (!call.arguments.isEmpty()) {
                    error(call.method, call.method.lexeme + "() takes no arguments.");
                }
                break;
            case "loop":
                if (call.arguments.size() > 1
                        || (call.arguments.size() == 1
                        && !isPositiveWholeNumber(call.arguments.get(0)))) {
                    error(call.method, "loop() takes one positive whole-number count or no arguments.");
                }
                break;
            default:
                error(call.method, "Unknown method '" + call.method.lexeme + "'.");
        }
    }

    private void requireInstrument(Token name) {
        if (!instruments.contains(name.lexeme)) {
            error(name, "Unknown instrument '" + name.lexeme + "'.");
        }
    }

    private void requirePattern(Token name, Expr value) {
        if (!(value instanceof Expr.ListLiteral)) {
            error(name, "Pattern must be a list of notes and rests.");
            return;
        }

        Expr.ListLiteral list = (Expr.ListLiteral) value;
        if (list.elements.isEmpty()) {
            error(name, "Pattern cannot be empty.");
        }
        for (Expr element : list.elements) {
            if (!(element instanceof Expr.Note) && !(element instanceof Expr.Rest)) {
                error(name, "Pattern can contain only notes and rests.");
                break;
            }
        }
    }

    private void requireMeter(Token name, Expr value) {
        if (!(value instanceof Expr.Ratio)) {
            error(name, "Meter must be a ratio such as 4/4.");
            return;
        }

        Expr.Ratio ratio = (Expr.Ratio) value;
        if (!isPositiveWholeNumber((Double) ratio.numerator.literal)
                || !isPositiveWholeNumber((Double) ratio.denominator.literal)) {
            error(name, "Meter must contain positive whole numbers.");
        }
    }

    private void requireVolume(Token name, Expr value) {
        Double number = number(value);
        if (number == null || !Double.isFinite(number)
                || number < 0 || number > 1) {
            error(name, "Volume must be between 0 and 1.");
        }
    }

    private void requirePositiveNumber(Token name, Expr value) {
        Double number = number(value);
        if (number == null || !Double.isFinite(number) || number <= 0) {
            error(name, "Value must be a positive number.");
        }
    }

    private Double number(Expr value) {
        Object literal = literal(value);
        return literal instanceof Double ? (Double) literal : null;
    }

    private Object literal(Expr value) {
        return value instanceof Expr.Literal ? ((Expr.Literal) value).value : null;
    }

    private boolean isPositiveWholeNumber(Expr value) {
        Double number = number(value);
        return number != null && isPositiveWholeNumber(number);
    }

    private boolean isPositiveWholeNumber(Double number) {
        return Double.isFinite(number) && number > 0 && number == Math.floor(number);
    }

    private void error(Token token, String message) {
        valid = false;
        diagnostics.add(new Diagnostic(token.line, message));
    }
}
