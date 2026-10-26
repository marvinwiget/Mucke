package io.github.marvinwiget.mucke;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import javafx.application.Application;
import io.github.marvinwiget.mucke.app.App;

public class Mucke {
    static boolean hadError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: mucke [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runApp("");
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        String source = new String(bytes, Charset.defaultCharset());

        if (hadError) System.exit(65);
        runApp(source);
    }

    private static void runApp(String source) {
        Application.launch(App.class, source);
    }

    public static void error(int line, String message) {
        hadError = true;
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
    }
}
