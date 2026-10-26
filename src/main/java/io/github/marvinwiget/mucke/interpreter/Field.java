package io.github.marvinwiget.mucke.interpreter;

import java.util.List;

class Field {
    Field(Token name, Expr value) {
        this.name = name;
        this.value = value;
    }

    final Token name;
    final Expr value;
}

