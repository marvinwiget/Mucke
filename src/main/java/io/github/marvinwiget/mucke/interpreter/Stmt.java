package io.github.marvinwiget.mucke.interpreter;

import java.util.List;

abstract class Stmt {
    static class Assign extends Stmt {
        Assign(Token name, Expr value) {
            this.name = name;
            this.value = value;
        }

        final Token name;
        final Expr value;
    }

    static class Set extends Stmt {
        Set(Token object, Token property, Expr value) {
            this.object = object;
            this.property = property;
            this.value = value;
        }

        final Token object;
        final Token property;
        final Expr value;
    }

    static class Call extends Stmt {
        Call(Token object, Token method, List<Expr> arguments) {
            this.object = object;
            this.method = method;
            this.arguments = arguments;
        }

        final Token object;
        final Token method;
        final List<Expr> arguments;
    }

}

