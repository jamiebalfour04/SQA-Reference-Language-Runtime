package jamiebalfour.zpe;

import jamiebalfour.HelperFunctions;
import jamiebalfour.zpe.core.ZPE;
import jamiebalfour.zpe.core.ZPEHelperFunctions;
import jamiebalfour.zpe.core.ZPEKit;
import jamiebalfour.zpe.exceptions.BreakPointHalt;
import jamiebalfour.zpe.exceptions.CompileException;
import jamiebalfour.zpe.exceptions.ExitHalt;
import jamiebalfour.zpe.exceptions.ZPERuntimeException;
import jamiebalfour.zpe.parser.ZenithParsingEngine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class SQARLParser {

  ZenithParsingEngine parser;

  ArrayList<String> classes = new ArrayList<>();


  public static void main(String[] args) throws HelperFunctions.NoArgumentValueProvided {

    HashMap<String, String> argv = jamiebalfour.HelperFunctions.generateArgumentMap(args);
    String first;

    if (args.length == 0) {

      if (System.console() == null) {

        if (!ZPEHelperFunctions.isHeadless()) {
          new SQARLEditorMain().setVisible(true);
        }

      } else {
        // If nothing has been provided
        System.out.println("If you are running this from the console, please provide at least one command line argument. You can use -r to run an SQARL program directly.");
        System.exit(0);
      }


    } else {
      first = args[0];

      if (first.equals("-r") && argv.containsKey("-r")) {
        // Run
        try {
          String s = jamiebalfour.HelperFunctions.readFileAsString(argv.get("-r").toString(), "utf-8");
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
          s = jamiebalfour.HelperFunctions.readFileAsString(argv.get("-e").toString(), "utf-8");
          String output = compileSQARL(s);
          if (!output.isEmpty()) {
            System.out.println(output);
          }
        } catch (IOException e) {
          ZPE.log("SQARL Runtime error: " + e.getMessage());
        }
      } else if (first.equals("-python")) {
        String s;
        try {
          s = jamiebalfour.HelperFunctions.readFileAsString(argv.get("-python").toString(), "utf-8");
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
           ZPE.startConsole(args);
        } else{
          new SQARLEditorMain().setVisible(true);
        }

      } else {
        System.out.println("You have provided incorrect arguments to the application.");
      }
    }


  }

  private String varProcess(String s) {
    s = s.replace("count", "_count");
    s = s.replace("list", "_list");

    if (!s.startsWith("_")) {
      s = "_" + s;
    }

    return s;
  }

  private void printError(String err) {
    System.err.println(err);
    //System.exit(-1);
  }

  public static String compileSQARL(String s) {
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

  public String parseToYASS(String code) {
    StringBuilder output = new StringBuilder();
    parser = new ZenithParsingEngine(code, false, new SQARLParserByteCodes());

    parser.getNextSymbol();
    while (parser.getCurrentSymbol() != -2) {
      output.append(parseOne());
      parser.getNextSymbol();
    }

    return output.toString();
  }


  // Simple method to get a single block
  private String parseOne() {
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
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.REPEAT) {
      return compileRepeat() + System.lineSeparator();
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.IF) {
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

    return "-1";
  }

  private boolean isJoin(byte symb) {
    return symb == SQARLParserByteCodes.AND || symb == SQARLParserByteCodes.OR || symb == SQARLParserByteCodes.PLUS || symb == SQARLParserByteCodes.MINUS || symb == SQARLParserByteCodes.MULT || symb == SQARLParserByteCodes.DIVIDE || symb == SQARLParserByteCodes.MOD;
  }

  private boolean isValue(byte symb) {
    return symb == SQARLParserByteCodes.IDENTIFIER || symb == SQARLParserByteCodes.INT || symb == SQARLParserByteCodes.STRING || symb == SQARLParserByteCodes.BOOLEAN || symb == SQARLParserByteCodes.REAL;
  }

  private boolean isComparison(byte symb) {
    return symb == SQARLParserByteCodes.GT || symb == SQARLParserByteCodes.LT || symb == SQARLParserByteCodes.GTE || symb == SQARLParserByteCodes.LTE || symb == SQARLParserByteCodes.EQUAL || symb == SQARLParserByteCodes.NEQUAL;
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
    }

    return output.toString();
  }

  private String compileVar(){
    String output = varProcess(parser.getCurrentWord());

    if(parser.peekAhead() == SQARLParserByteCodes.LSQBR){
      parser.getNextSymbol();
      output += compileIndex();
      parser.getNextSymbol();
    }
    return output;
  }

  private String compileExpression() {
    StringBuilder output = new StringBuilder();
    while (true) {

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
        output.append(parser.getCurrentWord()).append(parser.getWhitespace());
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
      output.append(parser.getCurrentWord());
      parser.getNextSymbol();

      if (parser.getCurrentSymbol() == SQARLParserByteCodes.COMMA) {
        parser.getNextSymbol();
        output.append(", ");
      }
    }

    output.append(")");

    parser.getNextSymbol();

    return output.toString();
  }

  private String compileRepeat() {
    String output = "";
    String first = "";
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.REPEAT) {
      parser.getNextSymbol();
      first = "loop until (";
    }

    StringBuilder body = new StringBuilder();

    while (parser.getCurrentSymbol() != SQARLParserByteCodes.UNTIL) {
      body.append(parseOne());
      parser.getNextSymbol();
    }

    if (parser.getCurrentSymbol() == SQARLParserByteCodes.UNTIL) {
      parser.getNextSymbol();
      first += compileExpression() + ")";
    }

    output += first + " " + body + "end loop ";

    return output;

  }

  private String compileIf() {
    StringBuilder output = new StringBuilder();
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.IF) {
      parser.getNextSymbol();
      output.append("if (");
    }

    output.append(compileExpression());

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.THEN) {
      printError("Error. Expected THEN.");
    }

    output.append(") ");

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
          output.append("else ");
          parser.getNextSymbol();
          while (parser.getCurrentSymbol() != SQARLParserByteCodes.END && parser.peekAhead() != SQARLParserByteCodes.IF) {
            output.append(parseOne());
            parser.getNextSymbol();
          }
        }
      }

    }

    parser.getNextSymbol();

    output.append("end if ");

    return output.toString();

  }

  private String compileElseIf(){
    StringBuilder output = new StringBuilder();

    parser.getNextSymbol();
    parser.getNextSymbol();
    output.append("elseif (");


    output.append(compileExpression());

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.THEN) {
      printError("Error. Expected THEN.");
    }

    output.append(") ");

    parser.getNextSymbol();

    while ((parser.getCurrentSymbol() != SQARLParserByteCodes.END && parser.peekAhead() != SQARLParserByteCodes.IF) && parser.getCurrentSymbol() != SQARLParserByteCodes.ELSE) {
      output.append(parseOne());
      parser.getNextSymbol();
    }

    return output.toString();
  }

  private String compileWhile() {
    StringBuilder output = new StringBuilder();

    if (parser.getCurrentSymbol() == SQARLParserByteCodes.WHILE) {
      parser.getNextSymbol();
      output.append("while (");
    }

    output.append(compileExpression());

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.DO) {
      printError("Error. Expected DO.");
    }

    output.append(") ");

    parser.getNextSymbol();

    while (parser.getCurrentSymbol() != SQARLParserByteCodes.END && parser.peekAhead() != SQARLParserByteCodes.WHILE) {
      output.append(parseOne());
      parser.getNextSymbol();
    }

    parser.getNextSymbol();

    output.append("end while ");

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

  private String compileFor() {
    StringBuilder output = new StringBuilder();

    boolean each = false;

    if (parser.getCurrentSymbol() == SQARLParserByteCodes.FOR) {
      parser.getNextSymbol();
      output.append("for ");
    }

    if (parser.peekAhead() == SQARLParserByteCodes.EACH) {
      output.append("each ");
      each = true;
    }

    output.append("(");

    if (parser.getCurrentSymbol() == SQARLParserByteCodes.IDENTIFIER && !each) {
      String var = compileVar();
      output.append(var).append(" = ");
      parser.getNextSymbol();

      if (parser.getCurrentSymbol() != SQARLParserByteCodes.FROM) {
        printError("Expected FROM.");
      }
      parser.getNextSymbol();
    }


    /*if (!each) {
      //output.append(parser.getCurrentWord());
    }*/


    output.append(compileExpression());

    parser.getNextSymbol();

    if (each) {
      if (parser.getCurrentSymbol() != SQARLParserByteCodes.FROM) {
        parser.getNextSymbol();
        output.append(" in ");
      }

      output.append(compileExpression());
    } else {
      parser.getNextSymbol();
      output.append(" to ");

      output.append(compileExpression());
      parser.getNextSymbol();
    }


    while (each && parser.getCurrentSymbol() != SQARLParserByteCodes.DO) {
      printError("Error. Expected DO.");
    }

    output.append(") ");

    parser.getNextSymbol();

    while (parser.getCurrentSymbol() != SQARLParserByteCodes.END && parser.peekAhead() != SQARLParserByteCodes.FOR) {
      output.append(parseOne());
      parser.getNextSymbol();
    }

    parser.getNextSymbol();

    output.append("end for ");

    return output.toString();

  }

  private String compileSet() {
    String output = "";
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.SET) {
      parser.getNextSymbol();
    }

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.IDENTIFIER) {
      printError("Error. Expected IDENTIFIER.");
    }

    // Add the name as a string
    String var = compileVar();
    output += var + " = ";

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.TO) {
      printError("Error. Expected TO IN SET.");
    }

    parser.getNextSymbol();

    if (!isValue(parser.getCurrentSymbol()) && !(parser.getCurrentSymbol() == SQARLParserByteCodes.LBRA)) {
      printError("Error. Expected TYPE.");
    }

    output += compileExpression();

    return output;

  }

  private String compileDeclare() {
    StringBuilder output = new StringBuilder();
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.DECLARE) {
      parser.getNextSymbol();
    }
    if (parser.getCurrentSymbol() != SQARLParserByteCodes.IDENTIFIER) {
      printError("Error. Expected IDENTIFIER.");
    }

    // Add the name as a string
    String var = varProcess(parser.getCurrentWord());
    output.append(var).append(parser.getWhitespace()).append("=");

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() == SQARLParserByteCodes.AS) {
      output.append(parser.getWhitespace());

      parser.getNextSymbol();

      if (parser.getCurrentSymbol() != SQARLParserByteCodes.TYPE) {
        printError("Error. Expected TYPE.");
      }

      if (parser.getCurrentSymbol() == SQARLParserByteCodes.ARRAY) {

        parser.getNextSymbol();

        if (parser.getCurrentSymbol() != SQARLParserByteCodes.OF) {
          printError("Error. Expected OF.");
        }

        parser.getNextSymbol();

        if (parser.getCurrentSymbol() != SQARLParserByteCodes.TYPE && parser.getCurrentSymbol() != SQARLParserByteCodes.ARRAY) {
          printError("Error. Expected TYPE.");
        }

        if (parser.getCurrentSymbol() == SQARLParserByteCodes.ARRAY) {

          parser.getNextSymbol();

          if (parser.getCurrentSymbol() != SQARLParserByteCodes.OF) {
            printError("Error. Expected OF.");
          }

          parser.getNextSymbol();

          if (parser.getCurrentSymbol() != SQARLParserByteCodes.TYPE && parser.getCurrentSymbol() != SQARLParserByteCodes.ARRAY) {
            printError("Error. Expected TYPE.");
          }

          parser.getNextSymbol();

          if (parser.getCurrentSymbol() != SQARLParserByteCodes.INITIALLY) {
            printError("Error. Expected INITIALLY.");
          }


          parser.getNextSymbol();
          //[

          if (parser.peekAhead() == SQARLParserByteCodes.RSQBR) {
            return output.append("[[]]").toString();
          }


          if (parser.getCurrentWord().isEmpty()) {
            output.append("[[").append("\"\"").append("]]");
          } else {
            output.append("[[").append(parser.getCurrentWord()).append("]]");
          }

        } else {
          parser.getNextSymbol();

          if (parser.getCurrentSymbol() != SQARLParserByteCodes.INITIALLY) {
            printError("Error. Expected INITIALLY.");
          }

          parser.getNextSymbol();
          //[

          parser.getNextSymbol();
          //Type of array


          if (parser.getCurrentWord().isEmpty()) {
            output.append("[").append("\"\"").append("]");
          } else {
            output.append("[").append(parser.getCurrentWord()).append("]");
          }

        }


        parser.getNextSymbol();
        //]

        parser.getNextSymbol();
        //*
        if (parser.getCurrentSymbol() != SQARLParserByteCodes.MULT) {
          printError("Error. Expected MULT.");
        }

        parser.getNextSymbol();

        output.append(" * ").append(parser.getCurrentWord());


        return output.toString();


      }

      parser.getNextSymbol();
    }


    if (parser.getCurrentSymbol() != SQARLParserByteCodes.INITIALLY) {
      printError("Error. Expected INITIALLY.");
    }

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() == SQARLParserByteCodes.FROM) {
      parser.getNextSymbol();

      if (parser.getCurrentSymbol() == SQARLParserByteCodes.KEYBOARD) {
        output.append("auto_input()");
      }
    } else if (parser.getCurrentSymbol() == SQARLParserByteCodes.IDENTIFIER && parser.peekAhead() == SQARLParserByteCodes.LBRA && classes.contains(parser.getCurrentWord())) {
      output.append(" new ").append(parser.getCurrentWord()).append(" (");
      parser.getNextSymbol();
      parser.getNextSymbol();

      while (parser.getCurrentSymbol() != SQARLParserByteCodes.RBRA) {
        output.append(parser.getCurrentWord());
        parser.getNextSymbol();

        if (parser.getCurrentSymbol() == SQARLParserByteCodes.COMMA) {
          output.append(", ");
          parser.getNextSymbol();
        }
      }

      output.append(") ");
    } else if (parser.getCurrentSymbol() == SQARLParserByteCodes.LSQBR) {
      //Array
      output.append(compileValue());

    } else {
      if (!isValue(parser.getCurrentSymbol())) {


        printError("Error. Expected TYPE.");
      }

      // Multiple values
      // compile_values();
      output.append(compileValue());

    }

    output.append(parser.getWhitespace());

    return output.toString();

  }

  private String compileProcedure() {
    StringBuilder output = new StringBuilder("function ");

    parser.getNextSymbol();

    output.append(parser.getCurrentWord());

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.LBRA) {
      printError("Error. Expected LBRACKET.");
    }

    output.append(" (");

    parser.getNextSymbol();

    while (parser.getCurrentSymbol() != SQARLParserByteCodes.RBRA) {
      if (isType()) {
        output.append(convertType()).append(" ");
        parser.getNextSymbol();
      } else {
        printError("Expected type in PROCEDURE signature parameters.");
      }


      output.append(parser.getCurrentWord());

      parser.getNextSymbol();

      if (parser.getCurrentSymbol() == SQARLParserByteCodes.COMMA) {
        output.append(", ");
        parser.getNextSymbol();
      }
    }

    output.append(") ");

    parser.getNextSymbol();
    while (parser.getCurrentSymbol() != SQARLParserByteCodes.END && parser.peekAhead() != SQARLParserByteCodes.PROCEDURE) {
      output.append(parseOne());
      parser.getNextSymbol();
    }

    parser.getNextSymbol();

    output.append(" end function ");

    return output.toString();
  }

  private String compileFunction() {
    boolean returnFound = false;

    StringBuilder output = new StringBuilder("function ");

    parser.getNextSymbol();

    output.append(parser.getCurrentWord());

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.LBRA) {
      printError("Error. Expected LBRACKET.");
    }

    output.append(" (");

    parser.getNextSymbol();

    while (parser.getCurrentSymbol() != SQARLParserByteCodes.RBRA) {
      if (isType()) {
        output.append(convertType()).append(" ");
        parser.getNextSymbol();
      } else {
        printError("Expected type in PROCEDURE signature parameters.");
      }


      output.append(parser.getCurrentWord());

      parser.getNextSymbol();

      if (parser.getCurrentSymbol() == SQARLParserByteCodes.COMMA) {
        output.append(", ");
        parser.getNextSymbol();
      }
    }

    output.append(") ");

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() == SQARLParserByteCodes.RETURNS) {
      parser.getNextSymbol();
      parser.getNextSymbol();
    }

    while (parser.getCurrentSymbol() != SQARLParserByteCodes.END && parser.peekAhead() != SQARLParserByteCodes.FUNCTION) {
      if (parser.getCurrentSymbol() == SQARLParserByteCodes.RETURN) {
        output.append("return ");
        parser.getNextSymbol();
        output.append(compileExpression());
        returnFound = true;
      } else {
        output.append(parseOne());
      }

      parser.getNextSymbol();
    }

    parser.getNextSymbol();

    output.append(" end function");

    if (!returnFound) {
      printError("RETURN not provided in a function");
    }

    return output.toString();
  }

  private String compileClass() {
    parser.getNextSymbol();
    if (parser.getCurrentSymbol() != SQARLParserByteCodes.IDENTIFIER) {
      printError("IDENTIFIER not provided for a class");
    }

    StringBuilder output = new StringBuilder();
    output.append("class ").append(parser.getCurrentWord()).append(" ").append(System.lineSeparator());

    classes.add(parser.getCurrentWord());

    if (parser.getNextSymbol() == SQARLParserByteCodes.IS) {
      if (parser.getNextSymbol() != SQARLParserByteCodes.LBRACE) {
        printError("LBRACE expected");
      }

      parser.getNextSymbol();

      StringBuilder params = new StringBuilder();

      params.append("( ");
      //Parser params
      while (parser.getCurrentSymbol() != SQARLParserByteCodes.RBRACE) {
        if (isType()) {
          params.append(convertType()).append(" ");
          parser.getNextSymbol();
        } else {
          printError("Expected type in PROCEDURE signature parameters.");
        }


        params.append(parser.getCurrentWord());

        parser.getNextSymbol();

        if (parser.getCurrentSymbol() == SQARLParserByteCodes.COMMA) {
          params.append(", ");
          parser.getNextSymbol();
        }
      }
      params.append(") ");
    }


    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.METHODS) {
      printError("METHODS expected in class definition.");
    }


    parser.getNextSymbol();

    while (!(parser.getCurrentSymbol() == SQARLParserByteCodes.END && parser.peekAhead() == SQARLParserByteCodes.CLASS)) {
      if (parser.getCurrentSymbol() == SQARLParserByteCodes.FUNCTION) {
        output.append(compileFunction()).append(System.lineSeparator());
        parser.getNextSymbol();
      } else if (parser.getCurrentSymbol() == SQARLParserByteCodes.PROCEDURE) {
        output.append(compileProcedure()).append(System.lineSeparator());
        parser.getNextSymbol();
      }
    }

    output.append(" end class");
    parser.getNextSymbol();
    //parser.getNextSymbol();

    return output.toString();
  }

  private String compileSend() {
    String output = "";
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.SEND) {
      parser.getNextSymbol();
    }

    output += "print(";

    if (!isValue(parser.getCurrentSymbol())) {
      printError("Error. Expected VALUE.");
    }

    output += compileExpression();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.TO)
      parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.TO) {
      printError("Error. Expected TO.");
    }

    parser.getNextSymbol();

    output += ")";

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.DISPLAY) {
      printError("Error. Expected DISPLAY.");
    }

    output += parser.getWhitespace();

    return output;

  }

  private String compileRecord() {

    StringBuilder output = new StringBuilder();

    if (parser.getCurrentSymbol() == SQARLParserByteCodes.RECORD) {
      parser.getNextSymbol();
    }

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.IDENTIFIER) {
      printError("Error. Expected IDENTIFIER.");
    }

    String name = parser.getCurrentWord();

    output.append("record structure ").append(name).append(" {");

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.IS) {
      printError("Error. Expected IS.");
    }

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.LBRACE) {
      printError("Error. Expected LBRACE.");
    }

    parser.getNextSymbol();

    boolean hasLooped = false;

    while (parser.getCurrentSymbol() != SQARLParserByteCodes.RBRACE) {

      if (parser.getCurrentSymbol() == SQARLParserByteCodes.COMMA) {
        parser.getNextSymbol();
      }

      //Check if the first word is a data type

      if (parser.getCurrentSymbol() != SQARLParserByteCodes.TYPE) {
        printError("Error. Expected TYPE.");
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
        printError("Error. Expected IDENTIFIER.");
      }

      output.append(parser.getCurrentWord());

      parser.getNextSymbol();

      hasLooped = true;


    }

    output.append("}");

    return output.toString();


  }

  private String compileReceive() {
    String output = "";
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.RECEIVE) {
      parser.getNextSymbol();
    }

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.IDENTIFIER) {
      printError("Error. Expected IDENTIFIER.");
    }

    output += varProcess(parser.getCurrentWord());

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.FROM) {
      printError("Error. Expected FROM.");
    }

    parser.getNextSymbol();

    if (isType()) {
      boolean brackets = false;
      if (parser.getCurrentSymbol() == SQARLParserByteCodes.LBRA) {
        parser.getNextSymbol();
        brackets = true;
      }

      boolean close = false;

      if (parser.getCurrentWord().equalsIgnoreCase("integer")) {
        output += " ceiling (";
        close = true;
      }

      parser.getNextSymbol();

      if (brackets) {
        if (parser.getCurrentSymbol() == SQARLParserByteCodes.RBRA) {
          parser.getNextSymbol();
        }
      }

      if (parser.getCurrentSymbol() != SQARLParserByteCodes.KEYBOARD) {
        printError("Error. Expected INPUT.");
      }

      if (close) {
        output += ")";
      }
    } else {
      if (parser.getCurrentSymbol() != SQARLParserByteCodes.KEYBOARD) {
        printError("Error. Expected INPUT.");
      }
    }

    output += parser.getWhitespace() + " = auto_input()";

    return output;
  }

  private boolean isType() {
    return parser.getCurrentSymbol() == SQARLParserByteCodes.TYPE || (parser.getCurrentSymbol() == SQARLParserByteCodes.LBRA && parser.peekAhead() == SQARLParserByteCodes.TYPE);

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
