package io.github.marvinwiget.mucke.app;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.marvinwiget.mucke.interpreter.CompileResult;
import io.github.marvinwiget.mucke.interpreter.Interpreter;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Duration;

public final class App extends Application {
    private final Interpreter interpreter = new Interpreter();

    private ExecutorService worker;
    private PauseTransition debounce;
    private TextArea editor;
    private Label status;
    private long revision;

    @Override
    public void start(Stage stage) {
        worker = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "mucke-checker");
            thread.setDaemon(true);
            return thread;
        });

        editor = new TextArea("config {\n}\n\nmain {\n}\n");
        status = new Label("Checking...");
        status.setWrapText(true);

        debounce = new PauseTransition(Duration.millis(250));
        debounce.setOnFinished(event -> checkCurrentText());

        editor.textProperty().addListener((observable, oldText, newText) -> {
            revision++;
            status.setText("Checking...");
            debounce.playFromStart();
        });

        BorderPane root = new BorderPane();
        root.setCenter(editor);
        root.setBottom(status);
        BorderPane.setMargin(status, new Insets(8));

        stage.setTitle("Mucke");
        stage.setScene(new Scene(root, 1080, 720));
        stage.show();

        debounce.playFromStart(); // Check the initial text.
    }

    private void checkCurrentText() {
        final String source = editor.getText();
        final long submittedRevision = revision;

        worker.execute(() -> {
            try {
                CompileResult result = interpreter.compile(source);

                Platform.runLater(() -> {
                    // Ignore a result if the user has typed something newer.
                    if (submittedRevision != revision) return;

                    if (result.isValid()) {
                        status.setText("Valid Mucke source");
                    } else if (!result.getDiagnostics().isEmpty()) {
                        status.setText(result.getDiagnostics().get(0).toString());
                    } else {
                        status.setText("Invalid Mucke source");
                    }
                });
            } catch (RuntimeException error) {
                error.printStackTrace();
                Platform.runLater(() -> {
                    if (submittedRevision == revision) {
                        status.setText("Internal interpreter error");
                    }
                });
            }
        });
    }

    @Override
    public void stop() {
        if (debounce != null) debounce.stop();
        if (worker != null) worker.shutdownNow();
    }

    public static void main(String[] args) {
        launch(args);
    }
}