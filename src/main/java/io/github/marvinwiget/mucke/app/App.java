package io.github.marvinwiget.mucke.app;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import io.github.marvinwiget.mucke.interpreter.CompileResult;
import io.github.marvinwiget.mucke.interpreter.Interpreter;
import io.github.marvinwiget.mucke.sound.LivePlayer;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

public final class App extends Application {
    private final Interpreter interpreter = new Interpreter();
    private final LivePlayer player = new LivePlayer();
    private final AtomicLong revision = new AtomicLong();

    private ExecutorService worker;
    private PauseTransition debounce;
    private CodeArea editor;
    private Label status;
    private Path file;

    @Override
    public void start(Stage stage) {
        worker = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "mucke-compiler");
            thread.setDaemon(true);
            return thread;
        });

        editor = new CodeArea();
        editor.setParagraphGraphicFactory(LineNumberFactory.get(editor));
        editor.setWrapText(false);
        status = new Label("Checking...");
        status.setWrapText(true);

        String initial = "config {\n}\n\nmain {\n}\n";
        boolean fileLoadFailed = false;
        if (!getParameters().getRaw().isEmpty()) {
            file = Path.of(getParameters().getRaw().get(0)).toAbsolutePath();
            try {
                initial = Files.readString(file, StandardCharsets.UTF_8);
            } catch (IOException error) {
                status.setText("Cannot open file: " + error.getMessage());
                file = null;
                fileLoadFailed = true;
            }
        }
        editor.replaceText(initial);

        debounce = new PauseTransition(Duration.millis(250));
        debounce.setOnFinished(event -> checkCurrentText());
        editor.textProperty().addListener((obs, oldText, newText) -> {
            revision.incrementAndGet();
            status.setText("Checking...");
            debounce.playFromStart();
        });

        Button open = new Button("Open");
        open.setOnAction(event -> openFile(stage));
        Button save = new Button("Save");
        save.setOnAction(event -> saveFile(stage));

        BorderPane root = new BorderPane();
        root.setTop(new ToolBar(open, save));
        root.setCenter(new VirtualizedScrollPane<>(editor));
        root.setBottom(status);
        BorderPane.setMargin(status, new Insets(8));

        stage.setTitle(file == null ? "Mucke" : "Mucke - " + file.getFileName());
        stage.setScene(new Scene(root, 1080, 720));
        stage.show();
        if (!fileLoadFailed) debounce.playFromStart();
    }

    private void openFile(Stage stage) {
        File selected = chooser().showOpenDialog(stage);
        if (selected == null) return;

        try {
            String source = Files.readString(selected.toPath(), StandardCharsets.UTF_8);
            file = selected.toPath().toAbsolutePath();
            editor.replaceText(source);
            revision.incrementAndGet();
            debounce.playFromStart();
            stage.setTitle("Mucke - " + file.getFileName());
        } catch (IOException error) {
            status.setText("Cannot open file: " + error.getMessage());
        }
    }

    private void saveFile(Stage stage) {
        if (file == null) {
            File selected = chooser().showSaveDialog(stage);
            if (selected == null) return;
            file = selected.toPath().toAbsolutePath();
        }

        try {
            Files.writeString(file, editor.getText(), StandardCharsets.UTF_8);
            stage.setTitle("Mucke - " + file.getFileName());
            revision.incrementAndGet(); // Relative sample paths may have changed.
            debounce.playFromStart();
            status.setText("Saved " + file.getFileName());
        } catch (IOException error) {
            status.setText("Cannot save file: " + error.getMessage());
        }
    }

    private FileChooser chooser() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Mucke files", "*.mucke"));
        return chooser;
    }

    private void checkCurrentText() {
        String source = editor.getText();
        long submitted = revision.get();
        Path baseDir = file == null
            ? Path.of("").toAbsolutePath()
            : file.getParent();

        worker.execute(() -> {
            try {
                CompileResult result = interpreter.compile(source, baseDir);
                if (submitted != revision.get()) return;

                if (!result.isValid()) {
                    String message = result.getDiagnostics().isEmpty()
                        ? "Invalid source"
                        : result.getDiagnostics().get(0).toString();
                    showStatus(submitted, message);
                    return; // Keep the last valid plan playing.
                }

                LivePlayer.PreparedPlan prepared = player.prepare(result.getPlan());
                if (submitted != revision.get()) return;
                player.submit(prepared);
                showStatus(submitted, result.getPlan().tracks.isEmpty()
                    ? "Valid source (silent)" : "Playing");
            } catch (Exception error) {
                showStatus(submitted, "Cannot play: " + error.getMessage());
            }
        });
    }

    private void showStatus(long submitted, String message) {
        Platform.runLater(() -> {
            if (submitted == revision.get()) status.setText(message);
        });
    }

    @Override
    public void stop() {
        if (debounce != null) debounce.stop();
        if (worker != null) worker.shutdownNow();
        player.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}