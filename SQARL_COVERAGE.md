# SQARL coverage

This matrix tracks the September 2016, version 1.0 SQA Reference Language
specification. A feature is only marked as covered when a regression test
transpiles the SQARL and compiles the generated YASS with ZPE.

## Core language

| Feature | Status |
| --- | --- |
| INTEGER, REAL, BOOLEAN, CHARACTER and STRING values | Covered |
| Explicit and inferred declarations | Covered |
| Keyword-named variables such as `count` and `list` | Covered |
| Arrays, empty arrays and nested array values | Covered |
| Array indexing and indexed assignment | Covered |
| Arithmetic, comparison and logical expressions | Covered |
| Unary minus, exponentiation and NOT | Covered |
| String/array concatenation | Covered |
| IF, ELSE IF and ELSE | Covered |
| WHILE and REPEAT ... UNTIL | Covered |
| REPEAT ... TIMES | Covered, including compound count expressions |
| FOR, FOR ... STEP and FOR EACH | Covered |
| Keyboard input and display output | Covered |
| Semicolon-separated commands | Covered |
| Hash comments | Covered by the tokenizer |
| Elisions (`<...>`) | Covered as explicit no-op placeholders |

## Higher

| Feature | Status |
| --- | --- |
| Procedures and functions | Covered for built-in scalar and array parameter types |
| Function calls with expression arguments | Covered |
| RETURN | Covered; nested/multiple-return tests remain |
| Named records | Covered |
| Anonymous record values | Covered |
| Record construction and field access/update | Covered |
| File OPEN, CREATE, CLOSE, RECEIVE and SEND | Covered at transpilation/compilation level |
| File execution semantics | Covered with a temporary-file behavioural test |
| Full static block-scope validation | Not implemented by the transpiler |

## Advanced Higher

| Feature | Status |
| --- | --- |
| Classes, fields and methods | Covered |
| THIS field access | Covered |
| Implicit constructors | Covered |
| Explicit constructors | Covered |
| Inheritance with additional fields | Covered |
| Method overriding | Covered |
| Polymorphic assignment | Relies on YASS runtime; behavioural test remains |
| Nested arrays and array-typed class fields | Covered |

## Verification

Run `jamiebalfour.zpe.SQARLParserSpecificationTest` with the project source,
test source and current ZPE JAR on the classpath. The test runner deliberately
has no JUnit or other external test-framework dependency.
