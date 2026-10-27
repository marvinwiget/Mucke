package io.github.marvinwiget.mucke.interpreter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.marvinwiget.mucke.music.NoteEvent;
import io.github.marvinwiget.mucke.music.PlaybackPlan;
import io.github.marvinwiget.mucke.music.TrackPlan;

final class ScoreCompiler {
    static final class CompileError extends RuntimeException {
        final int line;

        CompileError(int line, String message) {
            super(message);
            this.line = line;
        }
    }

    private static final class Instrument {
        final String name;
        Path sample;
        int rootNote = 60; // C4
        double volume = 1;
        double pitch = 1;
        int patternBeats;
        List<NoteEvent> notes = List.of();
        long cursor;
        boolean looping;

        Instrument(String name) {
            this.name = name;
        }
    }

    private final Map<String, Instrument> instruments = new LinkedHashMap<>();
    private final List<TrackPlan> tracks = new ArrayList<>();
    private double bpm = 120;
    private double volume = 1;
    private long meterTop = 4;
    private long meterBottom = 4;

    PlaybackPlan compile(Program program, Path baseDir) {
        for (ConfigEntry entry : program.config) {
            if (entry instanceof ConfigEntry.Setting setting) {
                setting(setting.name, setting.value);
            } else {
                ConfigEntry.Instrument declaration = (ConfigEntry.Instrument) entry;
                Instrument instrument = new Instrument(declaration.name.lexeme);
                for (Field field : declaration.fields) {
                    field(instrument, field.name, field.value, baseDir);
                }
                instruments.put(instrument.name, instrument);
            }
        }

        for (Stmt statement : program.main) {
            if (statement instanceof Stmt.Assign assign) {
                if (assign.name.lexeme.equals("bpm")
                        || assign.name.lexeme.equals("meter")) {
                    throw new CompileError(assign.name.line,
                        "Set bpm and meter in config; edit config to change them live.");
                }
                setting(assign.name, assign.value);
            } else if (statement instanceof Stmt.Set set) {
                field(instruments.get(set.object.lexeme), set.property,
                      set.value, baseDir);
            } else {
                Stmt.Call call = (Stmt.Call) statement;
                if (call.object.lexeme.equals("all")) {
                    for (Instrument instrument : instruments.values()) {
                        call(instrument, call);
                    }
                } else {
                    call(instruments.get(call.object.lexeme), call);
                }
            }
        }

        return new PlaybackPlan(bpm, meterTop, meterBottom, tracks);
    }

    private void setting(Token name, Expr value) {
        switch (name.lexeme) {
            case "bpm" -> {
                bpm = number(value);
                if (bpm < 1 || bpm > 1000) {
                    throw new CompileError(name.line, "BPM must be between 1 and 1000.");
                }
            }
            case "volume" -> volume = number(value);
            case "meter" -> {
                Expr.Ratio ratio = (Expr.Ratio) value;
                meterTop = ((Double) ratio.numerator.literal).longValue();
                meterBottom = ((Double) ratio.denominator.literal).longValue();
            }
        }
    }

    private void field(Instrument instrument, Token name, Expr value, Path baseDir) {
        switch (name.lexeme) {
            case "sample" -> instrument.sample =
                baseDir.resolve((String) ((Expr.Literal) value).value).normalize();
            case "root_note" -> instrument.rootNote = midi(((Expr.Note) value).note);
            case "volume" -> instrument.volume = number(value);
            case "pitch" -> instrument.pitch = number(value);
            case "pattern" -> {
                Expr.ListLiteral list = (Expr.ListLiteral) value;
                instrument.patternBeats = list.elements.size();
                List<NoteEvent> notes = new ArrayList<>();
                for (int step = 0; step < list.elements.size(); step++) {
                    Expr element = list.elements.get(step);
                    if (element instanceof Expr.Note note) {
                        notes.add(new NoteEvent(step, midi(note.note)));
                    }
                }
                instrument.notes = notes;
            }
            case "reverb" -> {
                if ((Boolean) ((Expr.Literal) value).value) {
                    throw new CompileError(name.line, "Reverb is not implemented yet.");
                }
            }
        }
    }

    private void call(Instrument instrument, Stmt.Call call) {
        String method = call.method.lexeme;
        if (method.equals("stop")) {
            tracks.removeIf(track -> track.name.equals(instrument.name));
            instrument.cursor = 0;
            instrument.looping = false;
            return;
        }

        if (instrument.looping) {
            tracks.removeIf(track ->
                track.name.equals(instrument.name) && track.repeats == -1);
            instrument.looping = false;
        }

        long repeats = 1;
        if (method.equals("loop")) {
            repeats = call.arguments.isEmpty()
                ? -1
                : ((Double) ((Expr.Literal) call.arguments.get(0)).value).longValue();
        }

        if (repeats > 0 && repeats > Long.MAX_VALUE / instrument.patternBeats) {
            throw new CompileError(call.method.line, "Loop is too long.");
        }

        tracks.add(new TrackPlan(
            instrument.name, instrument.sample, instrument.rootNote,
            volume * instrument.volume, instrument.pitch, instrument.cursor,
            repeats, instrument.patternBeats, instrument.notes));

        if (repeats == -1) {
            instrument.looping = true;
        } else {
            long duration = repeats * instrument.patternBeats;
            if (instrument.cursor > Long.MAX_VALUE - duration) {
                throw new CompileError(call.method.line, "Score is too long.");
            }
            instrument.cursor += duration;
        }
    }

    private static double number(Expr value) {
        return (Double) ((Expr.Literal) value).value;
    }

    private static int midi(Token token) {
        String name = token.lexeme;
        int pitch = switch (name.charAt(0)) {
            case 'C' -> 0;
            case 'D' -> 2;
            case 'E' -> 4;
            case 'F' -> 5;
            case 'G' -> 7;
            case 'A' -> 9;
            default -> 11; // B
        };
        int index = 1;
        if (name.charAt(index) == '#') {
            pitch++;
            index++;
        } else if (name.charAt(index) == 'b') {
            pitch--;
            index++;
        }

        try {
            int octave = Integer.parseInt(name.substring(index));
            if (octave > 9) {
                throw new CompileError(token.line, "Note is outside MIDI range: " + name);
            }
            int midi = (octave + 1) * 12 + pitch;
            if (midi < 0 || midi > 127) {
                throw new CompileError(token.line, "Note is outside MIDI range: " + name);
            }
            return midi;
        } catch (NumberFormatException error) {
            throw new CompileError(token.line, "Invalid note: " + name);
        }
    }
}