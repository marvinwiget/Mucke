package io.github.marvinwiget.mucke;

import io.github.marvinwiget.mucke.app.App;
import javafx.application.Application;

public final class Mucke {
    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Usage: mucke [file.mucke]");
            return;
        }
        Application.launch(App.class, args);
    }
}