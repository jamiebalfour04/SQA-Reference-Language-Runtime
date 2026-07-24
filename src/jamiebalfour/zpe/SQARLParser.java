package jamiebalfour.zpe;

import jamiebalfour.helpers.FileHelperFunctions;
import jamiebalfour.helpers.HelperFunctions;
import jamiebalfour.ui.UIUtils;
import jamiebalfour.zpe.core.ZPE;
import jamiebalfour.zpe.core.ZPEHelperFunctions;
import jamiebalfour.zpe.core.ZPEKit;
import jamiebalfour.zpe.core.exceptions.BreakPointHalt;
import jamiebalfour.zpe.core.exceptions.CompileException;
import jamiebalfour.zpe.core.exceptions.ExitHalt;
import jamiebalfour.zpe.core.exceptions.ZPERuntimeException;
import jamiebalfour.zpe.parser.v6.ZenithParsingEngine;
import jamiebalfour.zpe.transpilers.PythonTranspiler;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class SQARLParser {

  ZenithParsingEngine parser;

  ArrayList<String> classes = new ArrayList<>();
  ArrayList<String> records = new ArrayList<>();
  HashMap<String, ArrayList<String>> classFieldTypes = new HashMap<>();
  HashMap<String, ArrayList<String>> classFieldNames = new HashMap<>();
  HashMap<String, String> openFiles = new HashMap<>();
  int fileHandleCounter;
  int repeatCounter;
  
  


  public static void main(String[] args) throws HelperFunctions.NoArgumentValueProvided {

    if(HelperFunctions.isMac()) {
      HelperFunctions.setMacOSApplicationName("SQARL Runtime");
    }

    HashMap<String, String> argv = jamiebalfour.helpers.HelperFunctions.generateArgumentMap(args);
    String first;



    if (args.length == 0) {
      if (HelperFunctions.isHeadless() || ZPEHelperFunctions.isTrulyCommandLine()) {
        // If nothing has been provided
        System.out.println("If you are running this from the console, please provide at least one command line argument. You can use -r to run an SQARL program directly.");
        System.exit(0);


      } else {
        new SQARLEditorMain().setVisible(true);
      }


    } else {
      first = args[0];

      if (first.equals("-r") && argv.containsKey("-r")) {
        // Run
        try {
          String s = jamiebalfour.helpers.FileHelperFunctions.readFileAsString(argv.get("-r"), "utf-8");
          try {
            String output = compileAndRunSQARL(s);
            if (!output.isEmpty()) {
              System.out.println(output);
            }
          } catch (ExitHalt e) {
            System.exit(HelperFunctions.stringToInteger(e.getMessage()));
          } catch (BreakPointHalt e) {
            System.out.println(e.getMessage());
          } catch (CompileException | ZPERuntimeException e) {
            throw new RuntimeException(e);
          }


        } catch (IOException e) {
          System.err.println("File not found!");
        }
      } else if (first.equals("-e")) {
        String s;
        try {
          s = jamiebalfour.helpers.FileHelperFunctions.readFileAsString(argv.get("-e").toString(), "utf-8");
          String output = compileSQARL(s);
          if (!output.isEmpty()) {
            System.out.println(output);
          }
        } catch (IOException | CompileException e) {
          ZPE.log("SQARL Runtime error: " + e.getMessage());
        }
      } else if (first.equals("-python")) {
        String s;
        try {
          s = jamiebalfour.helpers.FileHelperFunctions.readFileAsString(argv.get("-python").toString(), "utf-8");
          PythonTranspiler t = new PythonTranspiler();
          String output = compileSQARL(s);
          String code = t.Transpile(ZPEKit.compile(output), "");
          System.out.print(code);
        } catch (IOException | CompileException e) {
          ZPE.log("SQARL Runtime error: " + e.getMessage());
          System.out.println("Transpile error in the compiler");
        }

      } else if (first.equals("-g")) {
        if(argv.containsKey("--console")){
          //Pass to the internal ZPE instance to handle this - so easy!
          boolean debugging = false;
          boolean top = false;
          if(argv.containsKey("--debugging")){
            debugging = true;
          }
          if(argv.containsKey("--top")){
            top = true;
          }


          try {
            String code = FileHelperFunctions.readFileAsString(argv.get("-g").toString());
            ZPE.startConsole(code, debugging, top);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }


        } else{
          SwingUtilities.invokeLater(() -> {
            SQARLEditorMain editor = new SQARLEditorMain();
            editor.setLocationRelativeTo(null);
            editor.setVisible(true);
          });
        }

      } else {
        System.out.println("You have provided incorrect arguments to the application.");
      }
    }


  }

  private String varProcess(String s) {
    if (!s.startsWith("$")) {
      s = "$" + s;
    }

    return s;
  }

  public static String compileSQARL(String s) throws CompileException {
    SQARLParser sqarl = new SQARLParser();
    return sqarl.parseToYASS(s);
  }

  public static String compileAndRunSQARL(String s) throws CompileException, ZPERuntimeException, ExitHalt, BreakPointHalt {
    SQARLParser sqarl = new SQARLParser();
    String yass = sqarl.parseToYASS(s);

    Object out = ZPEKit.interpret(yass, 5);
    if (out != null) {
      return out.toString();
    }
    return "";
  }

  public String parseToYASS(String code) throws CompileException {
    StringBuilder output = new StringBuilder();
    code = replaceElisions(code);
    parser = new ZenithParsingEngine(code, false, new SQARLParserByteCodes());

    parser.getNextSymbol();
    while (parser.getCurrentSymbol() != -2) {
      output.append(parseOne());
      parser.getNextSymbol();
    }

    return output.toString();
  }

  private String replaceElisions(String code) {
    // Elisions deliberately describe computation that has not been specified.
    // Replacing them with a no-op lets the surrounding, fully-defined SQARL be
    // compiled and exercised without inventing behaviour for the omitted part.
    code = code.replaceAll("(?m)^(\\s*)<[^>]*>", "$1do_nothing()");
    return code.replaceAll("(?i)(\\b(?:INITIALLY|TO)\\s*)<[^>]*>", "$1do_nothing()");
  }


  // Simple method to get a single block
  private String parseOne() throws CompileException {
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.SEMICOLON) {
      return "";
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.DECLARE) {
      return compileDeclare() + System.lineSeparator();
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.SET) {
      return compileSet() + System.lineSeparator();
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.SEND) {
      return compileSend() + System.lineSeparator();
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.RECEIVE) {
      return compileReceive() + System.lineSeparator();
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.RECORD) {
      return compileRecord() + System.lineSeparator();
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.OPEN
        || parser.getCurrentSymbol() == SQARLParserByteCodes.CLOSE
        || parser.getCurrentSymbol() == SQARLParserByteCodes.CREATE) {
      return compileFileCommand() + System.lineSeparator();
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.REPEAT) {
      return compileRepeat() + System.lineSeparator();
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.IF && parser.getPreviousSymbol() != SQARLParserByteCodes.END) {
      return compileIf() + System.lineSeparator();
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.WHILE) {
      return compileWhile() + System.lineSeparator();
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.FOR) {
      return compileFor() + System.lineSeparator();
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.PROCEDURE) {
      return compileProcedure() + System.lineSeparator();
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.FUNCTION) {
      return compileFunction() + System.lineSeparator();
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.CLASS) {
      return compileClass() + System.lineSeparator();
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.IDENTIFIER && parser.peekAhead() == SQARLParserByteCodes.LBRA) {
      return compileFunctionCall() + System.lineSeparator();
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.IDENTIFIER && parser.peekAhead() == SQARLParserByteCodes.DOT) {
      return compileVar() + System.lineSeparator();
    }

    throw new CompileException(parser, "Unsupported SQARL command `" + parser.getCurrentWord() + "`.");
  }

  private boolean isJoin(byte symb) {
    return symb == SQARLParserByteCodes.AND || symb == SQARLParserByteCodes.OR || symb == SQARLParserByteCodes.PLUS || symb == SQARLParserByteCodes.MINUS || symb == SQARLParserByteCodes.MULT || symb == SQARLParserByteCodes.DIVIDE || symb == SQARLParserByteCodes.MOD || symb == SQARLParserByteCodes.POWER;
  }

  private boolean isValue(byte symb) {
    return symb == SQARLParserByteCodes.IDENTIFIER || symb == SQARLParserByteCodes.INT || symb == SQARLParserByteCodes.STRING || symb == SQARLParserByteCodes.BOOLEAN || symb == SQARLParserByteCodes.REAL;
  }

  private boolean isComparison(byte symb) {
    return symb == SQARLParserByteCodes.GT || symb == SQARLParserByteCodes.LT || symb == SQARLParserByteCodes.GTE || symb == SQARLParserByteCodes.LTE || symb == SQARLParserByteCodes.EQUAL || symb == SQARLParserByteCodes.NEQUAL;
  }

  private boolean isExpressionStart(byte symbol) {
    return isValue(symbol)
        || symbol == SQARLParserByteCodes.LBRA
        || symbol == SQARLParserByteCodes.LSQBR
        || symbol == SQARLParserByteCodes.LBRACE
        || symbol == SQARLParserByteCodes.MINUS
        || symbol == SQARLParserByteCodes.NOT;
  }

  private String compileComparisonOperator() {
    switch (parser.getCurrentSymbol()) {
      case SQARLParserByteCodes.EQUAL:
        return "==";
      case SQARLParserByteCodes.NEQUAL:
        return "!=";
      case SQARLParserByteCodes.GTE:
        return ">=";
      case SQARLParserByteCodes.LTE:
        return "<=";
      default:
        return parser.getCurrentWord();
    }
  }

  //DECLARE first INITIALLY [10, 11, 12] # an array
  //DECLARE second INITIALLY first # still just 1 array
  //SET first[0] TO 20
  //SEND second TO DISPLAY # update to first is seen
  private String compileValue() {
    StringBuilder output = new StringBuilder();
    if (isValue(parser.getCurrentSymbol())) {
      if (parser.getCurrentSymbol() == SQARLParserByteCodes.IDENTIFIER && parser.peekAhead() != SQARLParserByteCodes.LBRA)
        output.append(compileVar()).append(" ");
      else if (parser.getCurrentSymbol() == SQARLParserByteCodes.IDENTIFIER && parser.peekAhead() == SQARLParserByteCodes.LBRA)
        output.append(compileFunctionCall());
      else if (parser.getCurrentSymbol() == SQARLParserByteCodes.BOOLEAN)
        output.append(parser.getCurrentWord().toLowerCase()).append(parser.getWhitespace());
      else if (parser.getCurrentSymbol() == SQARLParserByteCodes.STRING) {
        output.append("\"").append(parser.getCurrentWord()).append("\"").append(parser.getWhitespace());
      } else
        output.append(parser.getWhitespace()).append(parser.getCurrentWord());
    } else if (parser.getCurrentSymbol() == SQARLParserByteCodes.LSQBR) {
      output = new StringBuilder("[");
      parser.getNextSymbol();
      while (parser.getCurrentSymbol() != SQARLParserByteCodes.RSQBR) {
        output.append(compileValue());
        parser.getNextSymbol();
        if (parser.getCurrentSymbol() == SQARLParserByteCodes.COMMA) {
          output.append(",");
          parser.getNextSymbol();
        }
      }
      output.append("]");
    } else if (parser.getCurrentSymbol() == SQARLParserByteCodes.LBRACE) {
      output.append("{");
      parser.getNextSymbol();
      while (parser.getCurrentSymbol() != SQARLParserByteCodes.RBRACE) {
        if (parser.getCurrentSymbol() != SQARLParserByteCodes.IDENTIFIER) {
          return output.toString();
        }

        output.append(parser.getCurrentWord()).append(" : ");
        parser.getNextSymbol();
        if (parser.getCurrentSymbol() != SQARLParserByteCodes.EQUAL) {
          return output.toString();
        }

        parser.getNextSymbol();
        output.append(compileExpression());
        if (parser.peekAhead() == SQARLParserByteCodes.COMMA) {
          parser.getNextSymbol();
          output.append(", ");
          parser.getNextSymbol();
        } else if (parser.peekAhead() == SQARLParserByteCodes.RBRACE) {
          parser.getNextSymbol();
        } else if (parser.getCurrentSymbol() != SQARLParserByteCodes.RBRACE) {
          parser.getNextSymbol();
        }
      }
      output.append("}");
    }

    return output.toString();
  }

  private String compileVar(){
    boolean isThis = parser.getCurrentWord().equalsIgnoreCase("THIS");
    StringBuilder output = new StringBuilder(isThis ? "this" : varProcess(parser.getCurrentWord()));

    while (parser.peekAhead() == SQARLParserByteCodes.DOT) {
      parser.getNextSymbol();
      parser.getNextSymbol();

      if (parser.getCurrentSymbol() != SQARLParserByteCodes.IDENTIFIER) {
        return output.toString();
      }

      boolean method = parser.peekAhead() == SQARLParserByteCodes.LBRA;
      output.append(isThis || method ? "->" : ".");
      if (method) {
        output.append(compileFunctionCall());
      } else {
        output.append(parser.getCurrentWord());
      }
      isThis = false;
    }

    while(parser.peekAhead() == SQARLParserByteCodes.LSQBR){
      parser.getNextSymbol();
      output.append(compileIndex());
      parser.getNextSymbol();
    }
    return output.toString();
  }

  private String compileExpression() {
    StringBuilder output = new StringBuilder();
    while (true) {

      if (parser.getCurrentSymbol() == SQARLParserByteCodes.NOT) {
        output.append("!");
        parser.getNextSymbol();
      } else if (parser.getCurrentSymbol() == SQARLParserByteCodes.MINUS) {
        output.append("-");
        parser.getNextSymbol();
      }

      if (parser.getCurrentSymbol() == SQARLParserByteCodes.LBRA) {
        output.append("(");
        parser.getNextSymbol();
        output.append(compileExpression());
      }
      output.append(compileValue());

      if (parser.peekAhead() == SQARLParserByteCodes.RBRA) {
        output.append(")");
        parser.getNextSymbol();
        return output.toString();
      }
      if (isComparison(parser.peekAhead())) {

        parser.getNextSymbol();
        output.append(compileComparisonOperator()).append(parser.getWhitespace());
        parser.getNextSymbol();
        output.append(compileValue());

        if (parser.getCurrentSymbol() == SQARLParserByteCodes.RBRA) {
          output.append(")");
          parser.getNextSymbol();
        }
      }

      if (parser.peekAhead() == SQARLParserByteCodes.CONCAT) {
        parser.getNextSymbol();
        parser.getNextSymbol();
        return output.append(" & ").append(compileExpression()).toString();
      }
      if (!isJoin(parser.peekAhead()) && !isJoin(parser.getCurrentSymbol())) {
        if (parser.peekAhead(2) == SQARLParserByteCodes.RBRA) {
          parser.getNextSymbol();
          output.append(")");
        }
        return output.toString();
      } else {
        // Jump to
        if (isJoin(parser.peekAhead()))
          parser.getNextSymbol();
        if (parser.getCurrentSymbol() == SQARLParserByteCodes.MOD) {
          output.append(" % ").append(parser.getWhitespace());
          parser.getNextSymbol();
        } else {
          output.append(" ").append(parser.getCurrentWord().toLowerCase()).append(" ").append(parser.getWhitespace());
          parser.getNextSymbol();
        }
      }

    }

  }

  private String compileFunctionCall() {
    StringBuilder output = new StringBuilder();

    output.append(parser.getCurrentWord());

    parser.getNextSymbol();
    output.append("(");
    parser.getNextSymbol();

    while (parser.getCurrentSymbol() != SQARLParserByteCodes.RBRA) {
      String argument = compileExpression();
      if (parser.getCurrentSymbol() == SQARLParserByteCodes.RBRA && argument.endsWith(")")) {
        argument = argument.substring(0, argument.length() - 1);
      }
      output.append(argument);

      if (parser.peekAhead() == SQARLParserByteCodes.COMMA) {
        parser.getNextSymbol();
        output.append(", ");
        parser.getNextSymbol();
      } else if (parser.peekAhead() == SQARLParserByteCodes.RBRA) {
        parser.getNextSymbol();
      } else if (parser.getCurrentSymbol() != SQARLParserByteCodes.RBRA) {
        parser.getNextSymbol();
      }
    }

    output.append(")");

    return output.toString();
  }

  private String compileRepeat() throws CompileException {
    parser.getNextSymbol();

    // Fixed repetition: REPEAT expression TIMES ... END REPEAT.
    // A fixed YASS for loop does not require a named counter, so generated
    // programs cannot collide with a user-defined SQARL identifier.
    if (isExpressionStart(parser.getCurrentSymbol()) && hasTimesAhead()) {
      String count = compileExpression();
      parser.getNextSymbol();

      if (parser.getCurrentSymbol() != SQARLParserByteCodes.TIMES) {
        throw new CompileException(parser, "Error. Expected TIMES.");
      }

      StringBuilder body = new StringBuilder();
      parser.getNextSymbol();
      while (!(parser.getCurrentSymbol() == SQARLParserByteCodes.END
          && parser.peekAhead() == SQARLParserByteCodes.REPEAT)) {
        body.append(parseOne());
        parser.getNextSymbol();
      }

      parser.getNextSymbol();
      String counter = "$__sqarl_repeat_" + repeatCounter++;
      return "for (" + counter + " = 0 to (" + count + ") - 1)"
          + System.lineSeparator() + body + "end for";
    }

    String first = "loop until (";

    StringBuilder body = new StringBuilder();

    while (parser.getCurrentSymbol() != SQARLParserByteCodes.UNTIL) {
      body.append(parseOne());
      parser.getNextSymbol();
    }

    if (parser.getCurrentSymbol() == SQARLParserByteCodes.UNTIL) {
      parser.getNextSymbol();
      first += compileExpression() + ")";
    }

    return first + " " + body + "end loop ";

  }

  private boolean hasTimesAhead() {
    for (int offset = 1; ; offset++) {
      byte symbol = parser.peekAhead(offset);
      if (symbol == SQARLParserByteCodes.TIMES) {
        return true;
      }
      if (symbol == -2
          || symbol == SQARLParserByteCodes.DECLARE
          || symbol == SQARLParserByteCodes.SET
          || symbol == SQARLParserByteCodes.SEND
          || symbol == SQARLParserByteCodes.RECEIVE
          || symbol == SQARLParserByteCodes.IF
          || symbol == SQARLParserByteCodes.WHILE
          || symbol == SQARLParserByteCodes.FOR
          || symbol == SQARLParserByteCodes.REPEAT
          || symbol == SQARLParserByteCodes.END
          || symbol == SQARLParserByteCodes.UNTIL) {
        return false;
      }
    }
  }

  private String compileIf() throws CompileException {
    StringBuilder output = new StringBuilder();
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.IF) {
      parser.getNextSymbol();
      output.append("if (");
    }

    output.append(compileExpression());

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.THEN) {
      throw new CompileException(parser, "Error. Expected THEN.");
    }

    output.append(")").append(System.lineSeparator());

    parser.getNextSymbol();



    while ((parser.getCurrentSymbol() != SQARLParserByteCodes.END && parser.peekAhead() != SQARLParserByteCodes.IF) && parser.getCurrentSymbol() != SQARLParserByteCodes.ELSE) {
      output.append(parseOne());
      parser.getNextSymbol();
    }

    if(parser.getCurrentSymbol() == SQARLParserByteCodes.ELSE){
      while(parser.getCurrentSymbol() == SQARLParserByteCodes.ELSE){
        if(parser.peekAhead() == SQARLParserByteCodes.IF) {
          if(parser.peekAhead() == SQARLParserByteCodes.IF){
            output.append(compileElseIf());
          }
        } else{
          output.append("else").append(System.lineSeparator());
          parser.getNextSymbol();
          while (parser.getCurrentSymbol() != SQARLParserByteCodes.END && parser.peekAhead() != SQARLParserByteCodes.IF) {
            output.append(parseOne());
            parser.getNextSymbol();
          }
        }
      }

    }

    parser.getNextSymbol();

    output.append("end if");

    return output.toString();

  }

  private String compileElseIf() throws CompileException {
    StringBuilder output = new StringBuilder();

    parser.getNextSymbol();
    parser.getNextSymbol();
    output.append("elseif (");


    output.append(compileExpression());

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.THEN) {
      throw new CompileException(parser, "Error. Expected THEN.");
    }

    output.append(")").append(System.lineSeparator());

    parser.getNextSymbol();

    while ((parser.getCurrentSymbol() != SQARLParserByteCodes.END && parser.peekAhead() != SQARLParserByteCodes.IF) && parser.getCurrentSymbol() != SQARLParserByteCodes.ELSE) {
      output.append(parseOne());
      parser.getNextSymbol();
    }

    return output.toString();
  }

  private String compileWhile() throws CompileException {
    StringBuilder output = new StringBuilder();

    if (parser.getCurrentSymbol() == SQARLParserByteCodes.WHILE) {
      parser.getNextSymbol();
      output.append("while (");
    }

    output.append(compileExpression());

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.DO) {
      throw new CompileException(parser, "Error. Expected DO.");
    }

    output.append(")").append(System.lineSeparator());

    parser.getNextSymbol();

    while (parser.getCurrentSymbol() != SQARLParserByteCodes.END && parser.peekAhead() != SQARLParserByteCodes.WHILE) {
      output.append(parseOne());
      parser.getNextSymbol();
    }

    parser.getNextSymbol();

    output.append("end while");

    return output.toString();

  }

  private String compileIndex() {
    String output = "";

    output += "[";
    parser.getNextSymbol();
    output += compileExpression();
    output += "]";

    return output;
  }

  private String compileFor() throws CompileException {
    StringBuilder output = new StringBuilder();
    parser.getNextSymbol();

    boolean each = parser.getCurrentSymbol() == SQARLParserByteCodes.EACH;
    if (each) {
      parser.getNextSymbol();
    }

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.IDENTIFIER) {
      throw new CompileException(parser, "Error. Expected loop variable.");
    }

    String variable = compileVar();
    parser.getNextSymbol();
    if (parser.getCurrentSymbol() != SQARLParserByteCodes.FROM) {
      throw new CompileException(parser, "Expected FROM.");
    }

    parser.getNextSymbol();
    String firstExpression = compileExpression();
    parser.getNextSymbol();

    if (each) {
      if (parser.getCurrentSymbol() != SQARLParserByteCodes.DO) {
        throw new CompileException(parser, "Error. Expected DO.");
      }
      output.append("for each (").append(variable).append(" in ")
          .append(firstExpression).append(")").append(System.lineSeparator());
    } else {
      if (parser.getCurrentSymbol() != SQARLParserByteCodes.TO) {
        throw new CompileException(parser, "Error. Expected TO.");
      }

      parser.getNextSymbol();
      String upperBound = compileExpression();
      parser.getNextSymbol();

      if (parser.getCurrentSymbol() == SQARLParserByteCodes.STEP) {
        parser.getNextSymbol();
        String step = compileExpression();
        parser.getNextSymbol();
        output.append("for (").append(variable).append(" = ")
            .append(firstExpression).append(", ").append(variable).append(" <= ")
            .append(upperBound).append(", ").append(step).append(")")
            .append(System.lineSeparator());
      } else {
        output.append("for (").append(variable).append(" = ")
            .append(firstExpression).append(" to ").append(upperBound).append(")")
            .append(System.lineSeparator());
      }

      if (parser.getCurrentSymbol() != SQARLParserByteCodes.DO) {
        throw new CompileException(parser, "Error. Expected DO.");
      }
    }

    parser.getNextSymbol();

    while (!(parser.getCurrentSymbol() == SQARLParserByteCodes.END
        && parser.peekAhead() == SQARLParserByteCodes.FOR)) {
      output.append(parseOne());
      parser.getNextSymbol();
    }

    parser.getNextSymbol();
    if (each && parser.peekAhead() == SQARLParserByteCodes.EACH) {
      parser.getNextSymbol();
    }

    output.append("end for");

    return output.toString();

  }

  private String compileAssignable() throws CompileException {
    String output = compileVar();

    parser.getNextSymbol();

    while (parser.getCurrentSymbol() == SQARLParserByteCodes.LSQBR) {
      output += "[";

      parser.getNextSymbol();
      output += compileExpression();

      if (parser.getCurrentSymbol() != SQARLParserByteCodes.RSQBR) {
        throw new CompileException(parser, "Error. Expected ].");
      }

      output += "]";
      parser.getNextSymbol();
    }

    return output;
  }

  private String compileSet() throws CompileException {
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.SET) {
      parser.getNextSymbol();
    }

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.IDENTIFIER) {
      throw new CompileException(parser, "Error. Expected IDENTIFIER.");
    }

    String var = compileAssignable();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.TO) {
      throw new CompileException(parser, "Error. Expected TO IN SET.");
    }

    parser.getNextSymbol();

    if (!isExpressionStart(parser.getCurrentSymbol())) {
      throw new CompileException(parser, "Error. Expected TYPE.");
    }

    String expression = compileExpression().trim();

    /*
     * SQARL array append:
     *
     * SET values TO values & [value]
     *
     * YASS:
     *
     * values = list_append(values, value)
     */
    int concatenationPosition = expression.indexOf('&');

    if (concatenationPosition >= 0) {
      String left = expression
              .substring(0, concatenationPosition)
              .trim();

      String right = expression
              .substring(concatenationPosition + 1)
              .trim();

      boolean appendsToSameVariable =
              left.equals(var.trim());

      boolean hasSingletonList =
              right.startsWith("[")
                      && right.endsWith("]")
                      && !right.substring(1, right.length() - 1).contains(",");

      if (appendsToSameVariable && hasSingletonList) {
        String value = right
                .substring(1, right.length() - 1)
                .trim();

        return var
                + " = list_add_element("
                + var
                + ", "
                + value
                + ")";
      }
    }

    return var + " = " + expression;
  }

  private String compileDeclare() throws CompileException {
    StringBuilder output = new StringBuilder();
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.DECLARE) {
      parser.getNextSymbol();
    }
    if (parser.getCurrentSymbol() != SQARLParserByteCodes.IDENTIFIER) {
      throw new CompileException(parser, "Error. Expected IDENTIFIER.");
    }

    String var = compileVar();
    output.append(var).append(parser.getWhitespace()).append("=");

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() == SQARLParserByteCodes.AS) {
      output.append(parser.getWhitespace());
      parser.getNextSymbol();
      if (!isType()) {
        throw new CompileException(parser, "Error. Expected TYPE.");
      }
      compileDeclaredType();
      parser.getNextSymbol();
    }


    if (parser.getCurrentSymbol() != SQARLParserByteCodes.INITIALLY) {
      throw new CompileException(parser, "Error. Expected INITIALLY.");
    }

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() == SQARLParserByteCodes.FROM) {
      parser.getNextSymbol();

      if (parser.getCurrentSymbol() == SQARLParserByteCodes.KEYBOARD) {
        output.append("auto_input()");
      } else {
        String source = compileExpression().trim();
        output.append(fileHandleFor(source)).append("->read_line()");
      }
    } else if (parser.getCurrentSymbol() == SQARLParserByteCodes.IDENTIFIER
        && parser.peekAhead() == SQARLParserByteCodes.LBRA
        && (classes.contains(parser.getCurrentWord()) || records.contains(parser.getCurrentWord()))) {
      output.append(" new ").append(compileFunctionCall()).append(" ");
    } else if (parser.getCurrentSymbol() == SQARLParserByteCodes.LSQBR) {
      //Array
      output.append(compileValue());

    } else if (parser.getCurrentSymbol() == SQARLParserByteCodes.ARRAY) {
      parser.getNextSymbol();

      if (parser.getCurrentSymbol() != SQARLParserByteCodes.LSQBR) {
        throw new CompileException(parser, "Error. Expected [ after ARRAY.");
      }

      output.append(compileValue());
    } else {
      if (!isExpressionStart(parser.getCurrentSymbol())) {


        throw new CompileException(parser, "Error. Expected TYPE.");
      }

      output.append(compileExpression());

    }

    output.append(parser.getWhitespace());

    return output.toString();

  }

  private String compileProcedure() throws CompileException {
    StringBuilder output = new StringBuilder("function ");

    parser.getNextSymbol();

    output.append(parser.getCurrentWord());

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.LBRA) {
      throw new CompileException(parser, "Error. Expected LBRACKET.");
    }

    output.append(" (");

    parser.getNextSymbol();

    while (parser.getCurrentSymbol() != SQARLParserByteCodes.RBRA) {
      if (isType()) {
        output.append(compileDeclaredType()).append(" ");
        parser.getNextSymbol();
      } else {
        throw new CompileException(parser, "Expected type in PROCEDURE signature parameters.");
      }


      output.append(varProcess(parser.getCurrentWord()));

      parser.getNextSymbol();

      if (parser.getCurrentSymbol() == SQARLParserByteCodes.COMMA) {
        output.append(", ");
        parser.getNextSymbol();
      }
    }

    output.append(")").append(System.lineSeparator());

    parser.getNextSymbol();
    while (parser.getCurrentSymbol() != SQARLParserByteCodes.END && parser.peekAhead() != SQARLParserByteCodes.PROCEDURE) {
      output.append(parseOne());
      parser.getNextSymbol();
    }

    parser.getNextSymbol();

    output.append("end function");

    return output.toString();
  }

  private String compileFunction() throws CompileException {
    boolean returnFound = false;

    StringBuilder output = new StringBuilder("function ");

    parser.getNextSymbol();

    output.append(parser.getCurrentWord());

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.LBRA) {
      throw new CompileException(parser, "Error. Expected LBRACKET.");
    }

    output.append(" (");

    parser.getNextSymbol();

    while (parser.getCurrentSymbol() != SQARLParserByteCodes.RBRA) {
      if (isType()) {
        output.append(compileDeclaredType()).append(" ");
        parser.getNextSymbol();
      } else {
        throw new CompileException(parser, "Expected type in PROCEDURE signature parameters.");
      }


      output.append(varProcess(parser.getCurrentWord()));

      parser.getNextSymbol();

      if (parser.getCurrentSymbol() == SQARLParserByteCodes.COMMA) {
        output.append(", ");
        parser.getNextSymbol();
      }
    }

    output.append(")").append(System.lineSeparator());

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() == SQARLParserByteCodes.RETURNS) {
      parser.getNextSymbol();
      parser.getNextSymbol();
    }

    while (parser.getCurrentSymbol() != SQARLParserByteCodes.END && parser.peekAhead() != SQARLParserByteCodes.FUNCTION) {
      if (parser.getCurrentSymbol() == SQARLParserByteCodes.RETURN) {
        output.append("return ");
        parser.getNextSymbol();
        output.append(compileExpression()).append(System.lineSeparator());
        returnFound = true;
      } else {
        output.append(parseOne());
      }

      parser.getNextSymbol();
    }

    parser.getNextSymbol();

    output.append("end function");

    if (!returnFound) {
      throw new CompileException(parser, "RETURN not provided in a function");
    }

    return output.toString();
  }

  private String compileClass() throws CompileException {
    parser.getNextSymbol();
    if (parser.getCurrentSymbol() != SQARLParserByteCodes.IDENTIFIER) {
      throw new CompileException(parser, "IDENTIFIER not provided for a class");
    }

    String className = parser.getCurrentWord();
    classes.add(className);

    ArrayList<String> fieldTypes = new ArrayList<>();
    ArrayList<String> fieldNames = new ArrayList<>();
    String parentClass = null;
    int inheritedFieldCount = 0;

    parser.getNextSymbol();
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.INHERITS) {
      parser.getNextSymbol();
      if (parser.getCurrentSymbol() != SQARLParserByteCodes.IDENTIFIER) {
        throw new CompileException(parser, "Superclass identifier expected.");
      }
      parentClass = parser.getCurrentWord();
      if (classFieldNames.containsKey(parentClass)) {
        fieldNames.addAll(classFieldNames.get(parentClass));
        fieldTypes.addAll(classFieldTypes.get(parentClass));
        inheritedFieldCount = fieldNames.size();
      }
      if (parser.getNextSymbol() != SQARLParserByteCodes.WITH) {
        throw new CompileException(parser, "WITH expected after superclass.");
      }
    } else if (parser.getCurrentSymbol() != SQARLParserByteCodes.IS) {
      throw new CompileException(parser, "IS or INHERITS expected in class definition.");
    }

    if (parser.getNextSymbol() != SQARLParserByteCodes.LBRACE) {
      throw new CompileException(parser, "LBRACE expected");
    }

    parser.getNextSymbol();
    while (parser.getCurrentSymbol() != SQARLParserByteCodes.RBRACE) {
      if (!isType()) {
        throw new CompileException(parser, "Expected type in class field definition.");
      }

      String type = compileDeclaredType();
      parser.getNextSymbol();
      if (parser.getCurrentSymbol() != SQARLParserByteCodes.IDENTIFIER) {
        throw new CompileException(parser, "Expected identifier in class field definition.");
      }

      fieldTypes.add(type);
      fieldNames.add(parser.getCurrentWord());
      parser.getNextSymbol();
      if (parser.getCurrentSymbol() == SQARLParserByteCodes.COMMA) {
        parser.getNextSymbol();
      }
    }

    classFieldTypes.put(className, new ArrayList<>(fieldTypes));
    classFieldNames.put(className, new ArrayList<>(fieldNames));

    StringBuilder output = new StringBuilder();
    output.append("class ").append(className);
    if (parentClass != null) {
      output.append(" inherits ").append(parentClass);
    }
    output.append(" ").append(System.lineSeparator());
    for (int i = inheritedFieldCount; i < fieldNames.size(); i++) {
      output.append("protected ").append(fieldTypes.get(i)).append(" ")
          .append(varProcess(fieldNames.get(i))).append(" = ")
          .append(defaultValueForType(fieldTypes.get(i))).append(" ");
    }

    // SQARL supplies an implicit constructor unless an explicit one appears.
    if (!hasExplicitConstructorAhead()) {
      output.append("function _construct(");
      for (int i = 0; i < fieldNames.size(); i++) {
        if (i > 0) {
          output.append(", ");
        }
        output.append(fieldTypes.get(i)).append(" ").append(varProcess(fieldNames.get(i)));
      }
      output.append(") ");
      for (String fieldName : fieldNames) {
        output.append("this->").append(fieldName).append(" = ")
            .append(varProcess(fieldName)).append(" ");
      }
      output.append("end function ");
    }

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.METHODS) {
      throw new CompileException(parser, "METHODS expected in class definition.");
    }


    parser.getNextSymbol();

    while (!(parser.getCurrentSymbol() == SQARLParserByteCodes.END && parser.peekAhead() == SQARLParserByteCodes.CLASS)) {
      if (parser.getCurrentSymbol() == SQARLParserByteCodes.OVERRIDE) {
        parser.getNextSymbol();
      }
      if (parser.getCurrentSymbol() == SQARLParserByteCodes.FUNCTION) {
        output.append(compileFunction()).append(System.lineSeparator());
        parser.getNextSymbol();
      } else if (parser.getCurrentSymbol() == SQARLParserByteCodes.PROCEDURE) {
        output.append(compileProcedure()).append(System.lineSeparator());
        parser.getNextSymbol();
      } else if (parser.getCurrentSymbol() == SQARLParserByteCodes.CONSTRUCTOR) {
        output.append(compileConstructor()).append(System.lineSeparator());
        parser.getNextSymbol();
      }
    }

    output.append(" end class");
    parser.getNextSymbol();
    //parser.getNextSymbol();

    return output.toString();
  }

  private boolean hasExplicitConstructorAhead() {
    for (int offset = 1; ; offset++) {
      byte symbol = parser.peekAhead(offset);
      if (symbol == -2) {
        return false;
      }
      if (symbol == SQARLParserByteCodes.CONSTRUCTOR) {
        return true;
      }
      if (symbol == SQARLParserByteCodes.END
          && parser.peekAhead(offset + 1) == SQARLParserByteCodes.CLASS) {
        return false;
      }
    }
  }

  private String compileConstructor() throws CompileException {
    StringBuilder output = new StringBuilder("function _construct(");
    if (parser.getNextSymbol() != SQARLParserByteCodes.LBRA) {
      throw new CompileException(parser, "Error. Expected ( after CONSTRUCTOR.");
    }

    parser.getNextSymbol();
    while (parser.getCurrentSymbol() != SQARLParserByteCodes.RBRA) {
      if (!isType()) {
        throw new CompileException(parser, "Expected type in CONSTRUCTOR parameter.");
      }
      output.append(compileDeclaredType()).append(" ");
      parser.getNextSymbol();
      output.append(varProcess(parser.getCurrentWord()));
      parser.getNextSymbol();
      if (parser.getCurrentSymbol() == SQARLParserByteCodes.COMMA) {
        output.append(", ");
        parser.getNextSymbol();
      }
    }
    output.append(") ");

    parser.getNextSymbol();
    while (!(parser.getCurrentSymbol() == SQARLParserByteCodes.END
        && parser.peekAhead() == SQARLParserByteCodes.CONSTRUCTOR)) {
      output.append(parseOne());
      parser.getNextSymbol();
    }
    parser.getNextSymbol();
    output.append("end function ");
    return output.toString();
  }

  private String defaultValueForType(String type) {
    switch (type) {
      case "number":
        return "0";
      case "string":
        return "\"\"";
      case "boolean":
        return "false";
      case "list":
        return "[]";
      default:
        return "null";
    }
  }

  private String compileSend() throws CompileException {
    parser.getNextSymbol();
    if (!isExpressionStart(parser.getCurrentSymbol())) {
      throw new CompileException(parser, "Error. Expected VALUE.");
    }

    String value = compileExpression();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.TO)
      parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.TO) {
      throw new CompileException(parser, "Error. Expected TO.");
    }

    parser.getNextSymbol();

    String output;
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.DISPLAY) {
      output = "print(" + value + ")";
    } else {
      String destination = compileExpression();
      output = "file_put_contents(" + destination + ", file_get_contents("
          + destination + ") & " + value + " & std_new_line())";
    }

    return output + parser.getWhitespace();

  }

  private String compileRecord() throws CompileException {

    StringBuilder output = new StringBuilder();

    if (parser.getCurrentSymbol() == SQARLParserByteCodes.RECORD) {
      parser.getNextSymbol();
    }

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.IDENTIFIER) {
      throw new CompileException(parser, "Error. Expected IDENTIFIER.");
    }

    String name = parser.getCurrentWord();
    records.add(name);

    output.append("record structure ").append(name).append(" {");

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.IS) {
      throw new CompileException(parser, "Error. Expected IS.");
    }

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.LBRACE) {
      throw new CompileException(parser, "Error. Expected LBRACE.");
    }

    parser.getNextSymbol();

    boolean hasLooped = false;

    while (parser.getCurrentSymbol() != SQARLParserByteCodes.RBRACE) {

      if (parser.getCurrentSymbol() == SQARLParserByteCodes.COMMA) {
        parser.getNextSymbol();
      }

      //Check if the first word is a data type

      if (parser.getCurrentSymbol() != SQARLParserByteCodes.TYPE) {
        throw new CompileException(parser, "Error. Expected TYPE.");
      }

      String t = parser.getCurrentWord().toLowerCase();

      if (t.equalsIgnoreCase("integer") || t.equalsIgnoreCase("real")) {
        t = "number";
      }

      if (hasLooped) {
        output.append(", ");
      }

      output.append(t).append(" ");

      parser.getNextSymbol();

      if (parser.getCurrentSymbol() != SQARLParserByteCodes.IDENTIFIER) {
        throw new CompileException(parser, "Error. Expected IDENTIFIER.");
      }

      output.append(parser.getCurrentWord());

      parser.getNextSymbol();

      hasLooped = true;


    }

    output.append("}");

    return output.toString();


  }

  private String compileReceive() throws CompileException {
    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.IDENTIFIER) {
      throw new CompileException(parser, "Error. Expected IDENTIFIER.");
    }

    String target = compileAssignable();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.FROM) {
      throw new CompileException(parser, "Error. Expected FROM.");
    }

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() == SQARLParserByteCodes.KEYBOARD) {
      return target + " = auto_input()" + parser.getWhitespace();
    }

    String source = compileExpression().trim();
    return target + " = " + fileHandleFor(source) + "->read_line()" + parser.getWhitespace();
  }

  private String compileFileCommand() throws CompileException {
    byte command = parser.getCurrentSymbol();
    parser.getNextSymbol();
    if (!isExpressionStart(parser.getCurrentSymbol())) {
      throw new CompileException(parser, "File name or path expected.");
    }

    String file = compileExpression().trim();
    if (command == SQARLParserByteCodes.OPEN) {
      String handle = "$__sqarl_file_" + fileHandleCounter++;
      openFiles.put(file, handle);
      return handle + " = new SequentialFile()" + System.lineSeparator()
          + handle + "->open(" + file + ")";
    }
    if (command == SQARLParserByteCodes.CREATE) {
      return "file_put_contents(" + file + ", \"\")";
    }

    String handle = fileHandleFor(file);
    openFiles.remove(file);
    return handle + " = null";
  }

  private String fileHandleFor(String file) throws CompileException {
    String handle = openFiles.get(file);
    if (handle == null) {
      throw new CompileException(parser, "File `" + file + "` must be opened before it is read or closed.");
    }
    return handle;
  }

  private boolean isType() {
    return parser.getCurrentSymbol() == SQARLParserByteCodes.TYPE
        || parser.getCurrentSymbol() == SQARLParserByteCodes.ARRAY
        || (parser.getCurrentSymbol() == SQARLParserByteCodes.IDENTIFIER
            && (classes.contains(parser.getCurrentWord()) || records.contains(parser.getCurrentWord())))
        || (parser.getCurrentSymbol() == SQARLParserByteCodes.LBRA
            && parser.peekAhead() == SQARLParserByteCodes.TYPE);

  }

  private String compileDeclaredType() throws CompileException {
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.ARRAY) {
      if (parser.getNextSymbol() != SQARLParserByteCodes.OF) {
        throw new CompileException(parser, "Expected OF after ARRAY.");
      }
      parser.getNextSymbol();
      if (!isType()) {
        throw new CompileException(parser, "Expected element type after ARRAY OF.");
      }
      compileDeclaredType();
      return "list";
    }

    if (parser.getCurrentSymbol() == SQARLParserByteCodes.IDENTIFIER) {
      return "object";
    }
    return convertType();
  }

  private String convertType() {
    String output = "";

    if (parser.getCurrentWord().equals("INTEGER") || parser.getCurrentWord().equals("REAL")) {
      return "number";
    }

    if (parser.getCurrentWord().equals("STRING") || parser.getCurrentWord().equals("CHARACTER")) {
      return "string";
    }

    if (parser.getCurrentWord().equals("BOOLEAN")) {
      return "boolean";
    }

    return output;
  }

}
