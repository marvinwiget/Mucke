package io.github.marvinwiget.mucke.interpreter;

class Field {
    Field(Token name, Expr value) {
        this.name = name;
        this.value = value;
    }

    final Token name;
    final Expr value;
}

