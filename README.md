# SQA Reference Language Runtime powered by ZPE and YASS
This is a preprocessor for the YASS programming language. The program is designed to read SQA Reference Language, parse it, transpile it to YASS, and run it through the ZPE Runtime Environment (ZRE) or compile it within the ZPE Programming Environment (https://www.jamiebalfour.scot/projects/zpe/). It's fast due to ZPE's performance and can also be compiled to run natively on a system with a GraalVM native image. 

Since it now supports transpilation, the SQA Reference Language Runtime combines two of my other projects: ZPE Programming Environment and ZenPy. 

Keyboard input may include an explicit conversion, for example
`RECEIVE name FROM (STRING) KEYBOARD`.

Transpile a SQARL source file directly to a Python file with:

```console
sqarl -e program.sqarl -python program.py
```

The shorter `sqarl -python program.sqarl` form continues to print generated
Python to standard output.

It features a built-in UI editor similar to but not as functional as the ZPE UI Editor, a command-line interface, and the ability to compile SQARL files directly into a binary and run them later.

**It now also supports transpiling to both Python and PHP.**

To download the latest version, you can go [here](https://github.com/jamiebalfour04/SQA-Reference-Language-Runtime/tree/main/build).

## Requirements

The SQARL Runtime requires Java 11 or newer. Ensure `java` is available from
your terminal before installing or running the packaged JAR:

```console
java -version
```

## Installation

Install the packaged runtime into ZPE's application-data directory with:

```console
java -jar sqarl-runtime.jar --install
```

This installs the JAR as `zpe/sqarl/sqarl-runtime.jar` without replacing SQARL
settings already stored in that directory. It also creates `sqarl` on
macOS/Linux or `sqarl.cmd` on Windows in a writable directory already on
`PATH`.

## Running the GUI

After installation, open the SQARL editor with:

```console
sqarl -g
```

You can open a SQARL file in the editor at launch by supplying its path:

```console
sqarl -g program.sqarl
```

The GUI can also be launched directly from the downloaded JAR without
installing the `sqarl` command:

```console
java -jar sqarl-runtime.jar -g
```

If you want to learn more about the compilation and transpilation process, you can find out more [here](https://www.jamiebalfour.scot/projects/zpe/conversion/)
