package io.github.marvinwiget.mucke.interpreter;

import java.util.List;

class Program {
    Program(List<ConfigEntry> config, List<Activation> main) {
        this.config = config;
        this.main = main;
    }

    final List<ConfigEntry> config;
    final List<Activation> main;
}

