package io.github.marvinwiget.mucke;

enum TokenType {
    // Single-character tokens
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE, 
    COMMA, DOT, SLASH,
    
    // Literals
    IDENTIFIER, STRING, NUMBER, NOTE,

    // Keywords
    CONFIG, MAIN, INIT, WITH, REST, TRUE, FALSE,

    NEWLINE, EOF
}