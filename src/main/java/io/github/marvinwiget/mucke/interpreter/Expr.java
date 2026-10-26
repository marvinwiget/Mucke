package io.github.marvinwiget.mucke.interpreter;

import java.util.List;

abstract class Expr {
    static class Literal extends Expr {
        Literal(Object value) {
            this.value = value;
        }

        final Object value;
    }

    static class Ratio extends Expr {
        Ratio(Token numerator, Token denominator) {
            this.numerator = numerator;
            this.denominator = denominator;
        }

        final Token numerator;
        final Token denominator;
    }

    static class Note extends Expr {
        Note(Token note) {
            this.note = note;
        }

        final Token note;
    }

    static class Rest extends Expr {
        Rest(Token keyword) {
            this.keyword = keyword;
        }

        final Token keyword;
    }

    static class ListLiteral extends Expr {
        ListLiteral(List<Expr> elements) {
            this.elements = elements;
        }

        final List<Expr> elements;
    }

}

