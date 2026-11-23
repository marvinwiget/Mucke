package io.github.marvinwiget.mucke.interpreter;

class Activation {
    Activation(Token name, Token slot) {
        this.name = name;
        this.slot = slot;
    }

    final Token name;
    final Token slot;
}

