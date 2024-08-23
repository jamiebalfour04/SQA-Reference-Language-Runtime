# SQA Reference Language Runtime powered by ZPE and YASS
This is a preprocessor for the YASS programming language. The program is designed to read SQA Reference Language, parse it, transpile it to YASS, and run it through the ZPE Runtime Environment (ZRE) or compile it within the ZPE Programming Environment (https://www.jamiebalfour.scot/projects/zpe/). It's fast due to ZPE's performance and can also be compiled to run natively on a system with a GraalVM native image. 

Since it now supports transpilation, the SQA Reference Language Runtime combines two of my other projects: ZPE Programming Environment and ZenPy. 

It features a built-in UI editor similar to but not as functional as the ZPE UI Editor, a command-line interface, and the ability to compile SQARL files directly into a binary and run them later.

**It now also supports transpiling to both Python and PHP.**

To download the latest version, you can go [here](https://github.com/jamiebalfour04/SQA-Reference-Language-Runtime/tree/main/build).

If you want to learn more about the compilation and transpilation process, you can find out more [here](https://www.jamiebalfour.scot/projects/zpe/conversion/)
