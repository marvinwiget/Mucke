# Mucke syntax

The grammar below summarizes the structure. The parser also requires a newline between neighboring config items and between neighboring activations. Blank lines are allowed.

```text
program      → configBlock mainBlock EOF
configBlock  → "config" "{" configItem* "}"
configItem   → setting | instrument
setting      → IDENTIFIER ":" value
instrument   → "init" IDENTIFIER "with" "(" fieldList? ")"
fieldList    → field ("," field)* ","?
field        → IDENTIFIER ":" value

mainBlock    → "main" "{" activation* "}"
activation   → "$" IDENTIFIER

value        → NUMBER ("/" NUMBER)? | STRING | NOTE | "hit" | "rest" | list
list         → "[" (value ("," value)* ","?)? "]"
```

Fields and list entries can span lines. `bpm` and `meter` are the supported settings. `bpm` defaults to 120 and must be between 1 and 1000. `meter` defaults to `4/4`; each number must be a whole number from 1 to 1000. Each instrument needs a non-empty `pattern` and exactly one source: `sample` with a quoted file path, or `synth` with a MIDI program number from 0 to 127.

- Sample patterns contain only `hit` and `rest`. Samples play at their original pitch.
- Synth patterns contain notes such as `C3`, chords such as `[C3, E3, G3]`, and `rest`. A one-note chord `[B3]` and a bare `B3` have the same effect. Notes use `A`–`G`, an optional `#` or `b`, and an octave number; `C4` is MIDI note 60. `transpose` shifts only synth notes by a whole number of semitones. Notes must remain in the MIDI range 0–127 after shifting.

An activation such as `$bass` loops that pattern until it is removed from `main`. Each outer pattern entry lasts one beat: a quarter note in `4/4`, or an eighth note in `6/8`. Pattern length is independent of meter, so an eight-step pattern spans two `4/4` bars. New instruments join at the current beat of the bar. Synth notes end at the next beat; WAV hits play for their natural duration.

Valid edits join at the next beat once sounds are ready. If a pattern's length changes, that track switches at its next cycle boundary. An invalid edit keeps the previous version playing. See [`examples/`](../examples/) for complete files.
