package io.github.marvinwiget.mucke.interpreter;

import java.util.List;

class Program {
    Program(List<ConfigEntry> config, List<Token> main) {
        this.config = config;
        this.main = main;
    }

    final List<ConfigEntry> config;
    final List<Token> main;
}

