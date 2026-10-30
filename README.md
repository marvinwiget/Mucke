# Mucke

Mucke is a Java desktop app for writing sample-based music in `.mucke` files. Its editor checks changes as you type: valid versions join the shared beat, while an invalid edit leaves the last valid version playing. The project was inspired by [Crafting Interpreters](https://craftinginterpreters.com/) and [Strudel](https://strudel.cc/).

## Requirements

- JDK 21. If you have several JDKs, point `JAVA_HOME` to JDK 21.
- Internet access for the first build. The wrapper downloads Maven; Maven then downloads JavaFX and RichTextFX. No separate installation of those tools is needed.

## Build and run

From the project root, on macOS or Linux:

```sh
./mvnw -DskipTests package
./mvnw javafx:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd -DskipTests package
.\mvnw.cmd javafx:run
```

The package command creates `target/mucke-0.1.0-SNAPSHOT.jar`. It does not bundle JavaFX or the other dependencies, so launch the app with `javafx:run`. Tests are skipped during packaging because the current timing check is a standalone Java program rather than a Maven test.

In the app, click **Open** and select a file from [`examples/`](examples/). The examples use the WAV files in [`samples/`](samples/); sample paths are relative to each `.mucke` file. Edit a pattern while it plays to hear a valid change on the next applicable beat.
