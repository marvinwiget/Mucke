# Mucke

Mucke is a Java desktop app for writing looping music in `.mucke` files. Sample tracks play WAV hits; synth tracks play notes and chords through Java's MIDI synthesizer. The editor checks changes as you type and applies valid changes on the beat. Inspired by [Crafting Interpreters](https://craftinginterpreters.com/) and [Strudel](https://strudel.cc/).

## Requirements

- JDK 21. If you have several JDKs, point `JAVA_HOME` to JDK 21.
- Internet access for the first build. The Maven wrapper downloads Maven, JavaFX, and RichTextFX.

## Build and run

From the project root, on macOS or Linux:

```sh
./mvnw package
./mvnw javafx:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd package
.\mvnw.cmd javafx:run
```

The package command creates `target/mucke-0.1.0-SNAPSHOT.jar`. It does not bundle JavaFX or the other dependencies, so launch the app with `javafx:run`.

In the app, click **Open** and select a file from [`examples/`](examples/). You can edit while it plays and save with **Save**. Sample paths are relative to the `.mucke` file; for a new, unsaved file they are relative to the directory where you launched the app. The example below assumes an unsaved file launched from the project root.

The bundled WAV files and their credits are documented in [`samples/`](samples/README.md).

```mucke
config {
    bpm: 120
    meter: 4/4
    init kick with (
        sample: "samples/drums/kick/bd_808.wav",
        pattern: [hit, rest, hit, rest]
    )
    init keys with (
        synth: 0,
        transpose: 12,
        pattern: [[C3, E3, G3], B3, rest, [A3, C4, E4]]
    )
}
main {
    $kick
    $keys
}
```

`sample` tracks use `hit` and `rest`; the WAV plays unchanged. `synth` tracks use notes, chords, and `rest`; all notes in a chord start together and stop at the next beat. MIDI programs are numbered 0–127: `synth: 0` is piano and `synth: 80` is a square-wave lead. `transpose` shifts only synth notes by whole semitones. `[B3]` and `B3` both play one note.

Each outer pattern entry lasts one beat: in `4/4`, one quarter note. A pattern loops at its own length, independently of the meter. `$kick` activates a track; remove that line to stop its future steps. A valid edit takes effect on the next beat once its sounds are ready. If you change a pattern's length, that track switches at its next cycle boundary. Invalid edits leave the last valid version playing. The JavaFX beat clock has approximate timing, and synth tracks need an available MIDI synthesizer and soundbank.

To check the examples and beat logic from the project root, run `./mvnw test-compile` followed by `java -cp target/classes:target/test-classes io.github.marvinwiget.mucke.sound.MusicPatternTest`. On Windows, use `java -cp "target\classes;target\test-classes" io.github.marvinwiget.mucke.sound.MusicPatternTest` for the second command.
