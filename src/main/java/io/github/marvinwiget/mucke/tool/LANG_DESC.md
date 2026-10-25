# Mucke - EBNF

program      → configBlock mainBlock EOF

configBlock  → "config" "{" configItem* "}"
configItem   → setting | instrument
setting      → IDENTIFIER ":" value
instrument   → "init" IDENTIFIER "with" "(" fieldList? ")"
fieldList    → field ("," field)* ","?

mainBlock    → "main" "{" statement* "}"
statement    → call | assignment
assignment   → IDENTIFIER ":" value
             | IDENTIFIER "." IDENTIFIER ":" value
call         → IDENTIFIER "." IDENTIFIER "(" arguments? ")"
arguments    → value ("," value)*

field        → IDENTIFIER ":" value

value        → NUMBER ("/" NUMBER)?
             | STRING | NOTE | "rest" | "true" | "false"
             | list
list         → "[" (value ("," value)*)? "]"
