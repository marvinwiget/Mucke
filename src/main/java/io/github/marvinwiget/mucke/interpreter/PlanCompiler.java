package io.github.marvinwiget.mucke.interpreter;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.marvinwiget.mucke.music.PlaybackPlan;
import io.github.marvinwiget.mucke.music.PatternStep;
import io.github.marvinwiget.mucke.music.TrackPlan;

final class PlanCompiler {
    static final class CompileError extends RuntimeException {
        final int line;

        CompileError(int line, String message) {
            super(message);
            this.line = line;
        }
    }

    PlaybackPlan compile(Program program, Path baseDir) {
        double bpm = 120;
        long meterTop = 4;
        long meterBottom = 4;
        Map<String, TrackPlan> instruments = new HashMap<>();

        for (ConfigEntry entry : program.config) {
            if (entry instanceof ConfigEntry.Setting setting) {
                if (setting.name.lexeme.equals("bpm")) {
                    bpm = (Double) ((Expr.Literal) setting.value).value;
                } else {
                    Expr.Ratio ratio = (Expr.Ratio) setting.value;
                    meterTop = ((Double) ratio.numerator.literal).longValue();
                    meterBottom = ((Double) ratio.denominator.literal).longValue();
                }
            } else {
                ConfigEntry.Instrument instrument = (ConfigEntry.Instrument) entry;
                instruments.put(instrument.name.lexeme,
                    compileInstrument(instrument, baseDir));
            }
        }

        List<TrackPlan> tracks = new ArrayList<>();
        for (Token name : program.main) {
            tracks.add(instruments.get(name.lexeme));
        }
        return new PlaybackPlan(bpm, meterTop, meterBottom, tracks);
    }

    private TrackPlan compileInstrument(ConfigEntry.Instrument instrument, Path baseDir) {
        Path sample = null;
        Integer program = null;
        int transpose = 0;
        Expr.ListLiteral pattern = null;

        for (Field field : instrument.fields) {
            switch (field.name.lexeme) {
                case "sample" -> {
                    String path = (String) ((Expr.Literal) field.value).value;
                    try {
                        sample = baseDir.resolve(path).normalize();
                    } catch (InvalidPathException error) {
                        throw new CompileError(field.name.line,
                            "Invalid sample path: " + error.getInput());
                    }
                }
                case "synth" ->
                    program = ((Double) ((Expr.Literal) field.value).value).intValue();
                case "transpose" ->
                    transpose = ((Double) ((Expr.Literal) field.value).value).intValue();
                case "pattern" -> pattern = (Expr.ListLiteral) field.value;
            }
        }

        List<PatternStep> steps = new ArrayList<>();
        for (Expr element : pattern.elements) {
            if (element instanceof Expr.Rest) {
                steps.add(PatternStep.rest());
            } else if (element instanceof Expr.Hit) {
                steps.add(PatternStep.hit());
            } else if (element instanceof Expr.Note note) {
                steps.add(PatternStep.notes(List.of(midi(note.note, transpose))));
            } else {
                List<Integer> chord = new ArrayList<>();
                for (Expr note : ((Expr.ListLiteral) element).elements) {
                    chord.add(midi(((Expr.Note) note).note, transpose));
                }
                steps.add(PatternStep.notes(chord));
            }
        }

        return program == null
            ? TrackPlan.sample(instrument.name.lexeme, sample, steps)
            : TrackPlan.synth(instrument.name.lexeme, program, transpose, steps);
    }

    private static int midi(Token note, int transpose) {
        String text = note.lexeme;
        int pitch = switch (text.charAt(0)) {
            case 'C' -> 0;
            case 'D' -> 2;
            case 'E' -> 4;
            case 'F' -> 5;
            case 'G' -> 7;
            case 'A' -> 9;
            default -> 11;
        };
        int index = 1;
        if (text.charAt(index) == '#') {
            pitch++;
            index++;
        } else if (text.charAt(index) == 'b') {
            pitch--;
            index++;
        }
        try {
            int octave = Integer.parseInt(text.substring(index));
            long midi = ((long) octave + 1) * 12 + pitch;
            if (midi < 0 || midi > 127 || midi + transpose < 0
                    || midi + transpose > 127) {
                throw new CompileError(note.line,
                    "Note is outside MIDI range after transposition: " + text);
            }
            return (int) midi;
        } catch (NumberFormatException error) {
            throw new CompileError(note.line, "Invalid note: " + text);
        }
    }
}
