package io.github.marvinwiget.mucke.interpreter;

import java.util.ArrayList;
import java.util.List;

import static io.github.marvinwiget.mucke.interpreter.TokenType.*;

class Parser {
  private static class ParseError extends RuntimeException {}

  private final List<Token> tokens;
  private int current = 0;

  private final List<Diagnostic> diagnostics;

  Parser(List<Token> tokens) {
      this(tokens, new ArrayList<>());
  }

  Parser(List<Token> tokens, List<Diagnostic> diagnostics) {
      this.tokens = tokens;
      this.diagnostics = diagnostics;
  }

  // program → "config" "{" configEntry* "}"
  //           "main"   "{" statement* "}" EOF
  Program parse() {
    try {
      skipNewlines();

      consume(CONFIG, "Expect 'config'.");
      consume(LEFT_BRACE, "Expect '{' after 'config'.");
      List<ConfigEntry> config = configBlock();
      consume(RIGHT_BRACE, "Expect '}' after config block.");

      skipNewlines();

      consume(MAIN, "Expect 'main'.");
      consume(LEFT_BRACE, "Expect '{' after 'main'.");
      List<Stmt> main = mainBlock();
      consume(RIGHT_BRACE, "Expect '}' after main block.");

      skipNewlines();

      if (!isAtEnd()) {
        throw error(peek(), "Expect end of file.");
      }

      return new Program(config, main);
    } catch (ParseError error) {
      return null;
    }
  }

  // A prompt line is one statement from the main block.
  Stmt parsePromptStatement() {
    try {
      skipNewlines();
      Stmt result = statement();
      skipNewlines();
      if (!isAtEnd()) throw error(peek(), "Expect end of input.");
      return result;
    } catch (ParseError error) {
      return null;
    }
  }

  // configEntry → setting | instrument
  private List<ConfigEntry> configBlock() {
    List<ConfigEntry> entries = new ArrayList<>();
    skipNewlines();

    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      entries.add(configEntry());
      endOfLine();
    }

    return entries;
  }

  private ConfigEntry configEntry() {
    if (match(INIT)) return instrument();
    return setting();
  }

  // setting → IDENTIFIER ":" value
  private ConfigEntry setting() {
    Token name = consume(IDENTIFIER, "Expect setting name.");
    consume(COLON, "Expect ':' after setting name.");

    Expr value = value();
    return new ConfigEntry.Setting(name, value);
  }

  // instrument → "init" IDENTIFIER "with" "(" field* ")"
  // INIT was already consumed by configEntry().
  private ConfigEntry instrument() {
    Token name = consume(IDENTIFIER, "Expect instrument name.");
    consume(WITH, "Expect 'with' after instrument name.");
    consume(LEFT_PAREN, "Expect '(' after 'with'.");

    List<Field> fields = new ArrayList<>();
    skipNewlines();

    while (!check(RIGHT_PAREN) && !isAtEnd()) {
      Token fieldName = consume(IDENTIFIER, "Expect field name.");
      consume(COLON, "Expect ':' after field name.");

      Expr fieldValue = value();
      fields.add(new Field(fieldName, fieldValue));

      skipNewlines();
      if (!match(COMMA)) break;
      skipNewlines();
    }

    consume(RIGHT_PAREN, "Expect ')' after instrument fields.");
    return new ConfigEntry.Instrument(name, fields);
  }

  // main block → statement*
  private List<Stmt> mainBlock() {
    List<Stmt> statements = new ArrayList<>();
    skipNewlines();

    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(statement());
      endOfLine();
    }

    return statements;
  }

  // statement → IDENTIFIER ":" value
  //           | IDENTIFIER "." IDENTIFIER ":" value
  //           | IDENTIFIER "." IDENTIFIER "(" arguments? ")"
  private Stmt statement() {
    Token name = consume(IDENTIFIER, "Expect a name.");

    if (match(COLON)) {
      return new Stmt.Assign(name, value());
    }

    consume(DOT, "Expect '.' or ':' after name.");
    Token member = consume(IDENTIFIER, "Expect property or method name.");

    if (match(COLON)) {
      return new Stmt.Set(name, member, value());
    }

    consume(LEFT_PAREN, "Expect ':' or '(' after member name.");
    List<Expr> arguments = new ArrayList<>();
    skipNewlines();

    if (!check(RIGHT_PAREN)) {
      arguments.add(value());
      skipNewlines();

      while (match(COMMA)) {
        skipNewlines();
        if (check(RIGHT_PAREN)) break; // Optional trailing comma.
        arguments.add(value());
        skipNewlines();
      }
    }

    consume(RIGHT_PAREN, "Expect ')' after arguments.");
    return new Stmt.Call(name, member, arguments);
  }

  // value → NUMBER ("/" NUMBER)?
  //       | STRING | NOTE | "rest" | "true" | "false" | list
  private Expr value() {
    if (match(TRUE)) return new Expr.Literal(true);
    if (match(FALSE)) return new Expr.Literal(false);

    if (match(NUMBER)) {
      Token numerator = previous();

      if (match(SLASH)) {
        Token denominator = consume(NUMBER, "Expect number after '/'.");
        return new Expr.Ratio(numerator, denominator);
      }

      return new Expr.Literal(numerator.literal);
    }

    if (match(STRING)) {
      return new Expr.Literal(previous().literal);
    }

    if (match(NOTE)) {
      return new Expr.Note(previous());
    }

    if (match(REST)) {
      return new Expr.Rest(previous());
    }

    if (match(LEFT_BRACKET)) {
      return list();
    }

    throw error(peek(), "Expect a value.");
  }

  // list → "[" (value ("," value)*)? "]"
  // LEFT_BRACKET was already consumed by value().
  private Expr list() {
    List<Expr> elements = new ArrayList<>();
    skipNewlines();

    if (!check(RIGHT_BRACKET)) {
      elements.add(value());
      skipNewlines();

      while (match(COMMA)) {
        skipNewlines();
        if (check(RIGHT_BRACKET)) break; // Optional trailing comma.
        elements.add(value());
        skipNewlines();
      }
    }

    consume(RIGHT_BRACKET, "Expect ']' after list.");
    return new Expr.ListLiteral(elements);
  }

  // A newline separates entries in config and statements in main.
  private void endOfLine() {
    if (check(RIGHT_BRACE) || isAtEnd()) return;

    consume(NEWLINE, "Expect newline after this item.");
    skipNewlines();
  }

  private void skipNewlines() {
    while (match(NEWLINE)) {
      // Blank lines are allowed.
    }
  }

  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }

    return false;
  }

  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();
    throw error(peek(), message);
  }

  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().type == type;
  }

  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }

  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  private ParseError error(Token token, String message) {
      diagnostics.add(new Diagnostic(token.line, message));
      return new ParseError();
  }
}
