package jamiebalfour.zpe;

import jamiebalfour.helpers.FileHelperFunctions;
import jamiebalfour.helpers.HelperFunctions;
import jamiebalfour.zpe.core.IAST;
import jamiebalfour.zpe.core.YASSByteCodes;
import jamiebalfour.zpe.core.ZPE;
import jamiebalfour.zpe.core.ZPECompilerBytecodeBuilder;
import jamiebalfour.zpe.core.ZPEHelperFunctions;
import jamiebalfour.zpe.core.ZPEKit;
import jamiebalfour.zpe.core.exceptions.CompileException;
import jamiebalfour.zpe.core.interfaces.ZPEType;
import jamiebalfour.zpe.parser.v6.ZenithParsingEngine;
import jamiebalfour.zpe.transpilers.ZPEPythonTranspiler;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Direct SQARL frontend for ZPE. It emits IAST and never generates YASS source. */
public final class SQARLParser {
  private final ZPECompilerBytecodeBuilder bytecode;
  private final Set<String> constructedTypes = new HashSet<>();

  public SQARLParser() {
    this(new ZPECompilerBytecodeBuilder());
  }

  public SQARLParser(ZPECompilerBytecodeBuilder bytecode) {
    if (bytecode == null) throw new IllegalArgumentException("Bytecode builder is required.");
    this.bytecode = bytecode;
  }

  public IAST compile(String source) throws CompileException {
    if (source == null) throw error(1, "Source cannot be null.");
    constructedTypes.clear();
    List<Line> lines = tokenise(source.replaceAll("(?m)^(\\s*)<[^>]*>", "$1do_nothing()"));
    Position position = new Position();
    List<IAST> statements = compileBlock(lines, position, Stop.NONE);
    if (position.index != lines.size()) throw error(lines.get(position.index).number, "Unexpected block terminator.");
    return bytecode.program(statements.toArray(new IAST[0]));
  }

  public static IAST compileSQARL(String source) throws CompileException {
    return new SQARLParser().compile(source);
  }

  public static String compileAndRunSQARL(String source) throws Exception {
    ZPEType output = ZPEKit.runCode(compileSQARL(source), new HashMap<>(), 5);
    return output == null ? "" : output.toString();
  }

  public static void main(String[] args) {
    configureApplication();
    try {
      if (args.length == 0) openDefaultMode();
      else dispatchCommand(args);
    } catch (Exception exception) {
      ZPE.log("SQARL Runtime error: " + exception.getMessage());
    }
  }

  private static void configureApplication() {
    if (HelperFunctions.isMac()) HelperFunctions.setMacOSApplicationName("SQARL Runtime");
  }

  private static void openDefaultMode() {
    if (HelperFunctions.isHeadless() || ZPEHelperFunctions.isTrulyCommandLine()) printStartupInformation();
    else launchEditor();
  }

  private static void dispatchCommand(String[] args) throws Exception {
    String command = args[0];
    if ("-h".equals(command) || "--help".equals(command)) {
      printStartupInformation();
      return;
    }
    if ("--install".equals(command)) {
      SQARLInstaller.install();
      return;
    }

    HashMap<String, String> arguments = HelperFunctions.generateArgumentMap(args);
    switch (command) {
      case "-r": run(read(arguments, "-r")); break;
      case "-e": compileOrTranspile(arguments); break;
      case "-python": printPython(read(arguments, "-python")); break;
      case "-g": openEditorOrConsole(arguments); break;
      default:
        System.out.println("Unknown SQARL option `" + command + "`.\n");
        printStartupInformation();
    }
  }

  private static void compileOrTranspile(HashMap<String, String> arguments) throws Exception {
    String source = read(arguments, "-e");
    if (arguments.containsKey("-python")) transpilePython(Path.of(arguments.get("-python")), source);
    else compileApplication(arguments.get("-e"), source);
  }

  private static void printPython(String source) throws Exception {
    System.out.print(ZPEKit.transpileCode(compileSQARL(source), "", new ZPEPythonTranspiler()));
  }

  private static void openEditorOrConsole(HashMap<String, String> arguments) throws Exception {
    if (arguments.containsKey("--console")) run(read(arguments, "-g"));
    else {
      String filename = arguments.get("-g");
      launchEditor(filename == null ? null : Path.of(filename));
    }
  }

  private static void launchEditor() {
    launchEditor(null);
  }

  private static void launchEditor(Path file) {
    SwingUtilities.invokeLater(() -> {
      SQARLEditorMain editor = new SQARLEditorMain();
      if (file != null) editor.openFile(file);
      editor.setVisible(true);
    });
  }

  private static void printStartupInformation() {
    System.out.println("SQARL Runtime\n");
    printCommand("-r <file>", "Run a SQARL program.");
    printCommand("-e <file>", "Compile a SQARL program to a ZPE executable.");
    printCommand("-e <file> -python <output>", "Transpile SQARL to a Python file.");
    printCommand("-python <file>", "Print transpiled Python to standard output.");
    printCommand("-g [file] [--console]", "Open the editor, or run the file in console mode.");
    printCommand("--install", "Install SQARL into ZPE and create the sqarl command.");
    printCommand("-h, --help", "Show this help screen.");
  }

  private static void printCommand(String usage, String description) {
    System.out.printf("  %-31s %s%n", usage, description);
  }

  private static String read(HashMap<String, String> arguments, String option) throws IOException {
    String file = arguments.get(option);
    if (file == null) throw new IOException("No input file was supplied for " + option + ".");
    return FileHelperFunctions.readFileAsString(file, "utf-8");
  }

  private static void run(String source) throws Exception {
    String output = compileAndRunSQARL(source);
    if (!output.isEmpty()) System.out.println(output);
  }

  private static void compileApplication(String input, String source) throws Exception {
    Path inputPath = Path.of(input).toAbsolutePath();
    String filename = inputPath.getFileName().toString();
    int extension = filename.lastIndexOf('.');
    if (extension > 0) filename = filename.substring(0, extension);
    String output = inputPath.getParent().resolve(filename).toString();
    Object status = ZPEKit.compile(compileSQARL(source), output, filename, "SQARL").getName();
    if (!Integer.valueOf(0).equals(status)) {
      throw new IllegalStateException("A compiled SQARL application requires PROCEDURE main or FUNCTION main.");
    }
    System.out.println("Compiled SQARL application to " + output + ".yex");
  }

  static void transpilePython(Path output, String source) throws Exception {
    String python = ZPEKit.transpileCode(compileSQARL(source), "", new ZPEPythonTranspiler());
    Path destination = output.toAbsolutePath().normalize();
    if (destination.getParent() != null) Files.createDirectories(destination.getParent());
    Files.writeString(destination, python, StandardCharsets.UTF_8);
    System.out.println("Transpiled SQARL to " + destination + ".");
  }

  private List<Line> tokenise(String source) {
    ZenithParsingEngine parser = new ZenithParsingEngine(source, false, new SQARLParserByteCodes());
    List<Line> lines = new ArrayList<>();
    Line current = null;
    while (parser.getNextSymbol() != -2) {
      if (current == null || current.number != parser.getCurrentLine()
              || parser.getCurrentSymbol() == SQARLParserByteCodes.SEMICOLON) {
        if (parser.getCurrentSymbol() == SQARLParserByteCodes.SEMICOLON) {
          current = null;
          continue;
        }
        current = new Line(parser.getCurrentLine());
        lines.add(current);
      }
      current.tokens.add(new Token(parser.getCurrentSymbol(), parser.getCurrentWord()));
    }
    lines.removeIf(line -> line.tokens.isEmpty());
    return lines;
  }

  private List<IAST> compileBlock(List<Line> lines, Position position, Stop stop) throws CompileException {
    List<IAST> output = new ArrayList<>();
    while (position.index < lines.size() && !stop.matches(lines.get(position.index))) {
      output.add(compileStatement(lines, position));
    }
    return output;
  }

  private IAST compileStatement(List<Line> lines, Position position) throws CompileException {
    Line line = lines.get(position.index++);
    Tokens tokens = new Tokens(line);
    if (tokens.match(SQARLParserByteCodes.DECLARE)) return compileDeclare(tokens);
    if (tokens.match(SQARLParserByteCodes.SET)) return compileSet(tokens);
    if (tokens.match(SQARLParserByteCodes.SEND)) return compileSend(tokens);
    if (tokens.match(SQARLParserByteCodes.RECEIVE)) return compileReceive(tokens);
    if (tokens.match(SQARLParserByteCodes.IF)) return compileIf(lines, position, tokens);
    if (tokens.match(SQARLParserByteCodes.WHILE)) return compileWhile(lines, position, tokens);
    if (tokens.match(SQARLParserByteCodes.REPEAT)) return compileRepeat(lines, position, tokens);
    if (tokens.match(SQARLParserByteCodes.FOR)) return compileFor(lines, position, tokens);
    if (tokens.match(SQARLParserByteCodes.PROCEDURE)) return compileRoutine(lines, position, tokens, false);
    if (tokens.match(SQARLParserByteCodes.FUNCTION)) return compileRoutine(lines, position, tokens, true);
    if (tokens.match(SQARLParserByteCodes.RECORD)) return compileRecord(tokens);
    if (tokens.match(SQARLParserByteCodes.RETURN)) {
      IAST value = expression(tokens, 0);
      tokens.end();
      return bytecode.returnValue(value);
    }
    IAST expression = expression(tokens, 0);
    tokens.end();
    return expression;
  }

  private IAST compileDeclare(Tokens tokens) throws CompileException {
    String name = tokens.identifier("Expected an identifier after DECLARE.");
    byte type = YASSByteCodes.MIXED_TYPE;
    if (tokens.match(SQARLParserByteCodes.AS)) type = declaredType(tokens);
    tokens.require(SQARLParserByteCodes.INITIALLY, "Expected INITIALLY in declaration.");
    IAST value;
    if (tokens.match(SQARLParserByteCodes.FROM)) {
      if (tokens.match(SQARLParserByteCodes.KEYBOARD)) value = bytecode.call("auto_input");
      else throw tokens.error("Direct file RECEIVE support is not implemented yet.");
    } else value = expression(tokens, 0);
    tokens.end();
    return bytecode.declare(name, type, value);
  }

  private IAST compileSet(Tokens tokens) throws CompileException {
    IAST target = assignable(tokens);
    tokens.require(SQARLParserByteCodes.TO, "Expected TO in SET.");
    IAST value = expression(tokens, 0);
    tokens.end();
    return bytecode.assignment(target, null, value, YASSByteCodes.PROTECTED, false);
  }

  private IAST compileSend(Tokens tokens) throws CompileException {
    IAST value = expression(tokens, 0, SQARLParserByteCodes.TO);
    tokens.require(SQARLParserByteCodes.TO, "Expected TO in SEND.");
    if (!tokens.match(SQARLParserByteCodes.DISPLAY)) {
      throw tokens.error("Direct file SEND support is not implemented yet.");
    }
    tokens.end();
    return bytecode.printWithValueNames("nothing", "unknown", value);
  }

  private IAST compileReceive(Tokens tokens) throws CompileException {
    IAST target = assignable(tokens);
    tokens.require(SQARLParserByteCodes.FROM, "Expected FROM in RECEIVE.");
    byte inputType = YASSByteCodes.MIXED_TYPE;
    if (tokens.match(SQARLParserByteCodes.LBRA)) {
      inputType = declaredType(tokens);
      tokens.require(SQARLParserByteCodes.RBRA, "Expected ) after RECEIVE input type.");
    }
    if (!tokens.match(SQARLParserByteCodes.KEYBOARD)) {
      throw tokens.error("RECEIVE currently expects KEYBOARD.");
    }
    tokens.end();
    IAST value = bytecode.call("auto_input");
    if (inputType != YASSByteCodes.MIXED_TYPE) value = bytecode.cast(value, inputType);
    return bytecode.assignment(target, null, value,
            YASSByteCodes.PROTECTED, false);
  }

  private IAST compileIf(List<Line> lines, Position position, Tokens header) throws CompileException {
    IAST condition = expression(header, 0, SQARLParserByteCodes.THEN);
    header.require(SQARLParserByteCodes.THEN, "Expected THEN after IF condition.");
    header.end();
    List<IAST> thenBody = compileBlock(lines, position, Stop.IF_BRANCH);
    List<ZPECompilerBytecodeBuilder.ElseIfBranch> alternatives = new ArrayList<>();
    IAST elseBody = null;
    while (position.index < lines.size() && lines.get(position.index).starts(SQARLParserByteCodes.ELSE)) {
      Tokens branch = new Tokens(lines.get(position.index++));
      branch.require(SQARLParserByteCodes.ELSE, "Expected ELSE.");
      if (branch.match(SQARLParserByteCodes.IF)) {
        IAST branchCondition = expression(branch, 0, SQARLParserByteCodes.THEN);
        branch.require(SQARLParserByteCodes.THEN, "Expected THEN after ELSE IF.");
        branch.end();
        alternatives.add(bytecode.elseIf(branchCondition,
                bytecode.statements(compileBlock(lines, position, Stop.IF_BRANCH).toArray(new IAST[0]))));
      } else {
        branch.end();
        elseBody = bytecode.statements(compileBlock(lines, position, Stop.END_IF).toArray(new IAST[0]));
        break;
      }
    }
    requireEnd(lines, position, SQARLParserByteCodes.IF, "IF");
    return bytecode.ifStatement(condition, bytecode.statements(thenBody.toArray(new IAST[0])), alternatives, elseBody);
  }

  private IAST compileWhile(List<Line> lines, Position position, Tokens header) throws CompileException {
    IAST condition = expression(header, 0, SQARLParserByteCodes.DO);
    header.require(SQARLParserByteCodes.DO, "Expected DO after WHILE condition.");
    header.end();
    List<IAST> body = compileBlock(lines, position, Stop.END_WHILE);
    requireEnd(lines, position, SQARLParserByteCodes.WHILE, "WHILE");
    return bytecode.whileLoop(condition, bytecode.statements(body.toArray(new IAST[0])));
  }

  private IAST compileRepeat(List<Line> lines, Position position, Tokens header) throws CompileException {
    if (!header.atEnd()) {
      IAST count = expression(header, 0, SQARLParserByteCodes.TIMES);
      header.require(SQARLParserByteCodes.TIMES, "Expected TIMES after REPEAT count.");
      header.end();
      List<IAST> body = compileBlock(lines, position, Stop.END_REPEAT);
      requireEnd(lines, position, SQARLParserByteCodes.REPEAT, "REPEAT");
      return bytecode.repeatLoop(count, bytecode.statements(body.toArray(new IAST[0])));
    }
    List<IAST> body = compileBlock(lines, position, Stop.UNTIL);
    if (position.index >= lines.size()) throw error(header.line.number, "REPEAT requires UNTIL.");
    Tokens until = new Tokens(lines.get(position.index++));
    until.require(SQARLParserByteCodes.UNTIL, "Expected UNTIL.");
    IAST condition = expression(until, 0);
    until.end();
    return bytecode.untilLoop(condition, bytecode.statements(body.toArray(new IAST[0])));
  }

  private IAST compileFor(List<Line> lines, Position position, Tokens header) throws CompileException {
    boolean each = header.match(SQARLParserByteCodes.EACH);
    String variable = header.identifier("Expected FOR loop variable.");
    header.require(SQARLParserByteCodes.FROM, "Expected FROM in FOR.");
    if (each) {
      IAST iterable = expression(header, 0, SQARLParserByteCodes.DO);
      header.require(SQARLParserByteCodes.DO, "Expected DO in FOR EACH.");
      header.end();
      List<IAST> body = compileBlock(lines, position, Stop.END_FOR);
      requireEndFor(lines, position, true);
      return bytecode.forEveryLoop(variable, iterable, bytecode.statements(body.toArray(new IAST[0])));
    }
    IAST lower = expression(header, 0, SQARLParserByteCodes.TO);
    header.require(SQARLParserByteCodes.TO, "Expected TO in FOR.");
    IAST upper = expression(header, 0, SQARLParserByteCodes.STEP, SQARLParserByteCodes.DO);
    IAST step = bytecode.integer(1);
    if (header.match(SQARLParserByteCodes.STEP)) step = expression(header, 0, SQARLParserByteCodes.DO);
    header.require(SQARLParserByteCodes.DO, "Expected DO in FOR.");
    header.end();
    List<IAST> body = compileBlock(lines, position, Stop.END_FOR);
    requireEndFor(lines, position, false);
    IAST bodyNode = bytecode.block(bytecode.statements(body.toArray(new IAST[0])));
    return bytecode.forLoop(bytecode.assign(variable, lower), bytecode.lessThanOrEqual(bytecode.variable(variable), upper),
            bytecode.assign(variable, bytecode.add(bytecode.variable(variable), step)), bodyNode);
  }

  private IAST compileRoutine(List<Line> lines, Position position, Tokens header, boolean function) throws CompileException {
    String name = header.identifier("Expected routine name.");
    header.require(SQARLParserByteCodes.LBRA, "Expected ( after routine name.");
    List<ZPECompilerBytecodeBuilder.Parameter> parameters = new ArrayList<>();
    while (!header.check(SQARLParserByteCodes.RBRA)) {
      byte type = declaredType(header);
      parameters.add(bytecode.parameter(header.identifier("Expected parameter name."), type));
      if (!header.match(SQARLParserByteCodes.COMMA)) break;
    }
    header.require(SQARLParserByteCodes.RBRA, "Expected ).");
    byte returnType = YASSByteCodes.MIXED_TYPE;
    if (function && header.match(SQARLParserByteCodes.RETURNS)) returnType = declaredType(header);
    header.end();
    Stop stop = function ? Stop.END_FUNCTION : Stop.END_PROCEDURE;
    List<IAST> body = compileBlock(lines, position, stop);
    requireEnd(lines, position, function ? SQARLParserByteCodes.FUNCTION : SQARLParserByteCodes.PROCEDURE,
            function ? "FUNCTION" : "PROCEDURE");
    return bytecode.function(name, parameters, new byte[]{returnType},
            bytecode.statements(body.toArray(new IAST[0])), YASSByteCodes.PROTECTED);
  }

  private IAST compileRecord(Tokens tokens) throws CompileException {
    String name = tokens.identifier("Expected record name.");
    tokens.require(SQARLParserByteCodes.IS, "Expected IS after record name.");
    tokens.require(SQARLParserByteCodes.LBRACE, "Expected { in record.");
    List<ZPECompilerBytecodeBuilder.RecordField> fields = new ArrayList<>();
    while (!tokens.check(SQARLParserByteCodes.RBRACE)) {
      byte type = declaredType(tokens);
      fields.add(bytecode.recordField(tokens.identifier("Expected record field name."), type, null));
      if (!tokens.match(SQARLParserByteCodes.COMMA)) break;
    }
    tokens.require(SQARLParserByteCodes.RBRACE, "Expected }.");
    tokens.end();
    constructedTypes.add(name);
    return bytecode.record(name, fields, YASSByteCodes.PROTECTED);
  }

  private IAST assignable(Tokens tokens) throws CompileException {
    if (!tokens.check(SQARLParserByteCodes.IDENTIFIER)) throw tokens.error("Expected assignment target.");
    IAST value = bytecode.variable(tokens.take().word);
    return suffix(tokens, value);
  }

  private IAST expression(Tokens tokens, int minimum, byte... terminators) throws CompileException {
    IAST left = unary(tokens);
    while (!tokens.atEnd() && !contains(terminators, tokens.peek().type)) {
      int precedence = precedence(tokens.peek().type);
      if (precedence < minimum) break;
      byte operation = operation(tokens.take().type);
      IAST right = expression(tokens, precedence + (operation == YASSByteCodes.CIRCUMFLEX ? 0 : 1), terminators);
      left = operation == YASSByteCodes.CONCAT ? bytecode.concatenate(left, right) : bytecode.binary(operation, left, right);
    }
    return left;
  }

  private IAST unary(Tokens tokens) throws CompileException {
    if (tokens.match(SQARLParserByteCodes.NOT)) return bytecode.negation("!", unary(tokens));
    if (tokens.match(SQARLParserByteCodes.MINUS)) return bytecode.negative(unary(tokens));
    return primary(tokens);
  }

  private IAST primary(Tokens tokens) throws CompileException {
    Token token = tokens.takeOrError("Expected a value.");
    IAST value;
    switch (token.type) {
      case SQARLParserByteCodes.INT: value = bytecode.integer(Long.parseLong(token.word)); break;
      case SQARLParserByteCodes.REAL: value = bytecode.decimal(Double.parseDouble(token.word)); break;
      case SQARLParserByteCodes.STRING: value = bytecode.string(token.word); break;
      case SQARLParserByteCodes.BOOLEAN: value = bytecode.bool(Boolean.parseBoolean(token.word.toLowerCase())); break;
      case SQARLParserByteCodes.IDENTIFIER:
        if (tokens.match(SQARLParserByteCodes.LBRA)) {
          List<IAST> arguments = arguments(tokens);
          value = constructedTypes.contains(token.word)
                  ? bytecode.newInstance(token.word, arguments.toArray(new IAST[0]))
                  : bytecode.call(token.word, arguments.toArray(new IAST[0]));
        } else value = bytecode.variable(token.word.equalsIgnoreCase("THIS") ? "this" : token.word);
        break;
      case SQARLParserByteCodes.LBRA:
        value = expression(tokens, 0, SQARLParserByteCodes.RBRA);
        tokens.require(SQARLParserByteCodes.RBRA, "Expected ).");
        break;
      case SQARLParserByteCodes.LSQBR:
        List<IAST> values = new ArrayList<>();
        while (!tokens.check(SQARLParserByteCodes.RSQBR)) {
          values.add(expression(tokens, 0, SQARLParserByteCodes.COMMA, SQARLParserByteCodes.RSQBR));
          if (!tokens.match(SQARLParserByteCodes.COMMA)) break;
        }
        tokens.require(SQARLParserByteCodes.RSQBR, "Expected ].");
        value = bytecode.list(values.toArray(new IAST[0]));
        break;
      default: throw tokens.error("Unexpected value `" + token.word + "`.");
    }
    return suffix(tokens, value);
  }

  private IAST suffix(Tokens tokens, IAST value) throws CompileException {
    while (true) {
      if (tokens.match(SQARLParserByteCodes.LSQBR)) {
        IAST index = expression(tokens, 0, SQARLParserByteCodes.RSQBR);
        tokens.require(SQARLParserByteCodes.RSQBR, "Expected ].");
        value = bytecode.indexAccess(value, index);
      } else if (tokens.match(SQARLParserByteCodes.DOT)) {
        String member = tokens.identifier("Expected member name after '.'.");
        if (tokens.match(SQARLParserByteCodes.LBRA)) {
          List<IAST> args = arguments(tokens);
          value = bytecode.objectAccess(value, bytecode.call(member, args.toArray(new IAST[0])));
        } else value = bytecode.memberAccess(value, bytecode.variable(member));
      } else return value;
    }
  }

  private List<IAST> arguments(Tokens tokens) throws CompileException {
    List<IAST> arguments = new ArrayList<>();
    while (!tokens.check(SQARLParserByteCodes.RBRA)) {
      arguments.add(expression(tokens, 0, SQARLParserByteCodes.COMMA, SQARLParserByteCodes.RBRA));
      if (!tokens.match(SQARLParserByteCodes.COMMA)) break;
    }
    tokens.require(SQARLParserByteCodes.RBRA, "Expected ).");
    return arguments;
  }

  private byte declaredType(Tokens tokens) throws CompileException {
    if (tokens.match(SQARLParserByteCodes.ARRAY)) {
      tokens.match(SQARLParserByteCodes.OF);
      declaredType(tokens);
      return YASSByteCodes.LIST_TYPE;
    }
    Token token = tokens.takeOrError("Expected type.");
    if (token.type != SQARLParserByteCodes.TYPE) throw tokens.error("Expected type.");
    switch (token.word.toUpperCase()) {
      case "INTEGER": case "REAL": return YASSByteCodes.NUMBER_TYPE;
      case "BOOLEAN": return YASSByteCodes.BOOLEAN_TYPE;
      case "STRING": case "CHARACTER": return YASSByteCodes.STRING_TYPE;
      default: return YASSByteCodes.MIXED_TYPE;
    }
  }

  private void requireEnd(List<Line> lines, Position position, byte kind, String name) throws CompileException {
    if (position.index >= lines.size()) throw error(lines.isEmpty() ? 1 : lines.get(lines.size() - 1).number, "Missing END " + name + ".");
    Tokens end = new Tokens(lines.get(position.index++));
    end.require(SQARLParserByteCodes.END, "Expected END " + name + ".");
    end.require(kind, "Expected END " + name + ".");
    end.end();
  }

  private void requireEndFor(List<Line> lines, Position position, boolean each) throws CompileException {
    if (position.index >= lines.size()) throw error(lines.isEmpty() ? 1 : lines.get(lines.size() - 1).number, "Missing END FOR.");
    Tokens end = new Tokens(lines.get(position.index++));
    end.require(SQARLParserByteCodes.END, "Expected END FOR.");
    end.require(SQARLParserByteCodes.FOR, "Expected END FOR.");
    if (each) end.match(SQARLParserByteCodes.EACH);
    end.end();
  }

  private static int precedence(byte type) {
    if (type == SQARLParserByteCodes.OR) return 1;
    if (type == SQARLParserByteCodes.AND) return 2;
    if (type >= SQARLParserByteCodes.NEQUAL && type <= SQARLParserByteCodes.LTE) return 3;
    if (type == SQARLParserByteCodes.CONCAT) return 4;
    if (type == SQARLParserByteCodes.PLUS || type == SQARLParserByteCodes.MINUS) return 5;
    if (type == SQARLParserByteCodes.MULT || type == SQARLParserByteCodes.DIVIDE || type == SQARLParserByteCodes.MOD) return 6;
    if (type == SQARLParserByteCodes.POWER) return 7;
    return -1;
  }

  private static byte operation(byte type) {
    switch (type) {
      case SQARLParserByteCodes.OR: return YASSByteCodes.LOR;
      case SQARLParserByteCodes.AND: return YASSByteCodes.LAND;
      case SQARLParserByteCodes.NEQUAL: return YASSByteCodes.NEQUAL;
      case SQARLParserByteCodes.EQUAL: return YASSByteCodes.EQUAL;
      case SQARLParserByteCodes.GT: return YASSByteCodes.GT;
      case SQARLParserByteCodes.LT: return YASSByteCodes.LT;
      case SQARLParserByteCodes.GTE: return YASSByteCodes.GTE;
      case SQARLParserByteCodes.LTE: return YASSByteCodes.LTE;
      case SQARLParserByteCodes.CONCAT: return YASSByteCodes.CONCAT;
      case SQARLParserByteCodes.PLUS: return YASSByteCodes.PLUS;
      case SQARLParserByteCodes.MINUS: return YASSByteCodes.MINUS;
      case SQARLParserByteCodes.MULT: return YASSByteCodes.MULT;
      case SQARLParserByteCodes.DIVIDE: return YASSByteCodes.DIVIDE;
      case SQARLParserByteCodes.MOD: return YASSByteCodes.MODULO;
      case SQARLParserByteCodes.POWER: return YASSByteCodes.CIRCUMFLEX;
      default: throw new IllegalArgumentException("Unknown SQARL operator " + type);
    }
  }

  private static boolean contains(byte[] values, byte value) {
    for (byte candidate : values) if (candidate == value) return true;
    return false;
  }

  private static CompileException error(int line, String message) {
    return new CompileException(null, "SQARL compilation error on line " + line + ": " + message);
  }

  private enum Stop {
    NONE, IF_BRANCH, END_IF, END_WHILE, END_REPEAT, END_FOR, END_PROCEDURE, END_FUNCTION, UNTIL;
    boolean matches(Line line) {
      if (this == NONE || line.tokens.isEmpty()) return false;
      if (this == IF_BRANCH) return line.starts(SQARLParserByteCodes.ELSE) || end(line, SQARLParserByteCodes.IF);
      if (this == UNTIL) return line.starts(SQARLParserByteCodes.UNTIL);
      byte kind;
      switch (this) {
        case END_IF: kind = SQARLParserByteCodes.IF; break;
        case END_WHILE: kind = SQARLParserByteCodes.WHILE; break;
        case END_REPEAT: kind = SQARLParserByteCodes.REPEAT; break;
        case END_FOR: kind = SQARLParserByteCodes.FOR; break;
        case END_PROCEDURE: kind = SQARLParserByteCodes.PROCEDURE; break;
        case END_FUNCTION: kind = SQARLParserByteCodes.FUNCTION; break;
        default: return false;
      }
      return end(line, kind);
    }
    private static boolean end(Line line, byte kind) {
      return line.tokens.size() >= 2 && line.tokens.get(0).type == SQARLParserByteCodes.END
              && line.tokens.get(1).type == kind;
    }
  }

  private static final class Position { int index; }
  private static final class Token { final byte type; final String word; Token(byte type, String word){this.type=type;this.word=word;} }
  private static final class Line {
    final int number; final List<Token> tokens = new ArrayList<>();
    Line(int number){this.number=number;}
    boolean starts(byte type){return !tokens.isEmpty() && tokens.get(0).type == type;}
  }
  private static final class Tokens {
    final Line line; int index;
    Tokens(Line line){this.line=line;}
    boolean atEnd(){return index >= line.tokens.size();}
    Token peek(){return atEnd()?new Token((byte)-2,""):line.tokens.get(index);}
    boolean check(byte type){return !atEnd() && peek().type == type;}
    boolean match(byte type){if(check(type)){index++;return true;}return false;}
    Token take(){return line.tokens.get(index++);}
    Token takeOrError(String message) throws CompileException {if(atEnd())throw error(message);return take();}
    void require(byte type,String message)throws CompileException{if(!match(type))throw error(message);}
    String identifier(String message)throws CompileException{if(!check(SQARLParserByteCodes.IDENTIFIER))throw error(message);return take().word;}
    void end()throws CompileException{if(!atEnd())throw error("Unexpected token `"+peek().word+"`.");}
    CompileException error(String message){return SQARLParser.error(line.number,message);}
  }
}
