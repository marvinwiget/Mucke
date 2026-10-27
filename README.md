# Mucke

Mucke is a Java language project for composing music. The desktop app checks source code as you edit and plays each valid version using the samples named in the program. Invalid edits leave the last valid version playing.

## Build and run the desktop app

Use JDK 21. The Maven wrapper downloads Maven on first use; Maven then resolves JavaFX 21.0.13, RichTextFX 0.11.7, and their dependencies. No separate JavaFX SDK installation is needed. Downloaded dependencies are kept in the ignored `.mvn/repository` directory.

```sh
./mvnw compile
./mvnw javafx:run
```

`./run-app.sh` also starts the app. In VS Code, import the Maven project before using the **Run Mucke** launch configuration. The editor uses RichTextFX `CodeArea`. The active build excludes `interpreter/old` and four tests that still target that archived implementation.

## Live playback timing

All instruments share one beat count. A valid edit takes effect on the next beat without restarting that count. A changed note plays when its step next occurs; a pattern length change waits for that instrument's next cycle boundary. New instruments join the current beat within the bar, and removed instruments stop starting new notes on the next beat. Notes already sounding finish naturally. A BPM edit changes the length of the next beat while keeping the beat count.

Each pattern element lasts one beat. Pattern length is independent of `meter`: an eight-element pattern in `4/4` spans two bars, with step 5 on beat 1 of the second bar, and loops after step 8. When that pattern joins a four-step track, it starts in its first bar at the matching beat. Use `loop()` in `main` to repeat it continuously.

The timing check runs without an audio output device:

```sh
./mvnw test-compile
java -cp target/classes:target/test-classes io.github.marvinwiget.mucke.sound.BeatSequencerTest
```

## Archived prototype notes

The following commands document an earlier command-line and audio prototype. They use source files and classes that are not in the active Maven build and will not run as written.

### Play or render a file

```sh
java -cp out io.github.marvinwiget.mucke.interpreter.Mucke src/main/java/io/github/marvinwiget/mucke/source.mucke
java -cp out io.github.marvinwiget.mucke.interpreter.Mucke --render demo.wav src/main/java/io/github/marvinwiget/mucke/source.mucke
java -cp out io.github.marvinwiget.mucke.interpreter.Mucke src/main/java/io/github/marvinwiget/mucke/live_demo.mucke
```

The examples use the small WAV samples beside the `.mucke` files. Sample paths are relative to the program's directory. `live_demo.mucke` contains `all.loop()` and keeps playing until you press Ctrl+C. Rendering a WAV works without an audio output device; an endless `loop()` cannot be rendered to a finite WAV.

### Edit while music plays

Open `live_demo.mucke` in your editor. In a separate terminal, run:

```sh
java -cp out io.github.marvinwiget.mucke.interpreter.Mucke --watch src/main/java/io/github/marvinwiget/mucke/live_demo.mucke
```

Save the file to queue the new version for the next bar with enough time to prepare it. The current version keeps playing until then, including when the edit changes the tempo. If the edited file has an error, Mucke reports it and keeps the last valid version queued or playing. Press Ctrl+C to stop. You can use any text editor; the terminal handles audio and watches the file.

### Live prompt

```sh
java -cp out io.github.marvinwiget.mucke.interpreter.Mucke
```

In the prompt, enter `:load src/main/java/io/github/marvinwiget/mucke/source.mucke` to start playback. Then enter one statement per line:

```text
bass.loop()
all.play()
all.stop()
bass.pattern: [C2, rest, G2, rest]
bass.volume: 0.5
bpm: 130
bass.stop()
:quit
```

`play()` queues one pattern; `loop(4)` queues four total plays; `loop()` repeats until `stop()` or another play/loop call on that instrument. `all.play()`, `all.loop()`, and `all.stop()` apply those methods to every declared instrument. New play calls, loop changes, and stops take effect at each instrument's next pattern boundary. A field change affects later calls and future repetitions of an active loop. Prompt tempo changes take effect when entered. `stop()` cancels future notes; notes already sounding finish naturally.

The live prompt keeps a bounded queue of future notes. Use `loop()` for long-running repetitions instead of a very large `loop(n)` count.

Each pattern element currently lasts one beat. Instruments share the same beat clock, while repeated calls on one instrument queue after its previous pattern. A `rest` occupies a beat without starting a sample. The default tempo is 120 BPM, and the default `root_note` is C4. `main` runs once; put `loop()` in it for continuous playback. In file mode, the last `bpm` assignment applies to the whole score because the language has no statement timing yet.

`meter` sets the bar length for watch-mode reloads; it does not change note lengths or pattern scheduling yet. To stop a running file without closing the program, edit its `main` to call `all.stop()` and save it in watch mode, or enter `all.stop()` in the live prompt. A `stop()` immediately after `loop()` in the same `main` cancels that loop before playback begins. Speaker playback requires an audio output device available to Java Sound; `--render` still works without one.

`reverb: true` is recognized by the language but is not implemented by the sample engine yet. The current pattern format does not include note lengths or chords.

### Timing tests

```sh
javac -d out src/main/java/io/github/marvinwiget/mucke/interpreter/*.java src/test/java/io/github/marvinwiget/mucke/interpreter/*.java
java -cp out io.github.marvinwiget.mucke.interpreter.ScoreCompilerTest
java -cp out io.github.marvinwiget.mucke.interpreter.LiveSessionTest
java -cp out io.github.marvinwiget.mucke.interpreter.SourceWatcherTest
java -cp out io.github.marvinwiget.mucke.interpreter.SampleAudioEngineTest
```
