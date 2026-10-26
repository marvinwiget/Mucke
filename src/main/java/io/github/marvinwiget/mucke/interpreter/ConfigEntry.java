package io.github.marvinwiget.mucke.interpreter;

import java.util.List;

abstract class ConfigEntry {
    static class Setting extends ConfigEntry {
        Setting(Token name, Expr value) {
            this.name = name;
            this.value = value;
        }

        final Token name;
        final Expr value;
    }

    static class Instrument extends ConfigEntry {
        Instrument(Token name, List<Field> fields) {
            this.name = name;
            this.fields = fields;
        }

        final Token name;
        final List<Field> fields;
    }

}

