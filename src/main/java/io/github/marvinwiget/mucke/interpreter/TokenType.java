package io.github.marvinwiget.mucke.interpreter;

enum TokenType {
    // Single-character tokens
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, SLASH, COLON, LEFT_BRACKET, RIGHT_BRACKET, DOLLAR,

    // Literals
    IDENTIFIER, STRING, NUMBER, NOTE,

    // Keywords
    CONFIG, MAIN, INIT, WITH, HIT, REST,

    NEWLINE, EOF
}
