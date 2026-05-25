package jamiebalfour.zpe;

import jamiebalfour.FileHelperFunctions;
import jamiebalfour.HelperFunctions;
import jamiebalfour.ui.UIUtils;
import jamiebalfour.zpe.core.ZPE;
import jamiebalfour.zpe.core.ZPEHelperFunctions;
import jamiebalfour.zpe.core.ZPEKit;
import jamiebalfour.zpe.core.exceptions.BreakPointHalt;
import jamiebalfour.zpe.core.exceptions.CompileException;
import jamiebalfour.zpe.core.exceptions.ExitHalt;
import jamiebalfour.zpe.core.exceptions.ZPERuntimeException;
import jamiebalfour.zpe.parser.v5.ZenithParsingEngine;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class SQARLParser {

  ZenithParsingEngine parser;

  ArrayList<String> classes = new ArrayList<>();
  
  


  public static void main(String[] args) throws HelperFunctions.NoArgumentValueProvided {


    if(HelperFunctions.isMac()) {
      HelperFunctions.setMacOSApplicationName("SQARL Runtime");
    }

    HashMap<String, String> argv = jamiebalfour.HelperFunctions.generateArgumentMap(args);
    String first;



    if (args.length == 0) {
      if (System.console() == null) {

        if (!HelperFunctions.isHeadless()) {
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
          String s = jamiebalfour.FileHelperFunctions.readFileAsString(argv.get("-r"), "utf-8");
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
          s = jamiebalfour.FileHelperFunctions.readFileAsString(argv.get("-e").toString(), "utf-8");
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
          s = jamiebalfour.FileHelperFunctions.readFileAsString(argv.get("-python").toString(), "utf-8");
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
    s = s.replace("count", "_count");
    s = s.replace("list", "_list");

    if (!s.startsWith("_")) {
      s = "_" + s;
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
    parser = new ZenithParsingEngine(code, false, new SQARLParserByteCodes());

    parser.getNextSymbol();
    while (parser.getCurrentSymbol() != -2) {
      output.append(parseOne());
      parser.getNextSymbol();
    }

    return output.toString();
  }


  // Simple method to get a single block
  private String parseOne() throws CompileException {
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

  private String compileRepeat() throws CompileException {
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

    output.append(") ");

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

  private String compileFor() throws CompileException {
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
        throw new CompileException(parser, "Expected FROM.");
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
      throw new CompileException(parser, "Error. Expected DO.");
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
    String output = "";

    if (parser.getCurrentSymbol() == SQARLParserByteCodes.SET) {
      parser.getNextSymbol();
    }

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.IDENTIFIER) {
      throw new CompileException(parser, "Error. Expected IDENTIFIER.");
    }

    String var = compileAssignable();
    output += var + " = ";

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.TO) {
      throw new CompileException(parser, "Error. Expected TO IN SET.");
    }

    parser.getNextSymbol();

    if (!isValue(parser.getCurrentSymbol()) && parser.getCurrentSymbol() != SQARLParserByteCodes.LBRA) {
      throw new CompileException(parser, "Error. Expected TYPE.");
    }

    output += compileExpression();

    return output;
  }

  private String compileDeclare() throws CompileException {
    StringBuilder output = new StringBuilder();
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.DECLARE) {
      parser.getNextSymbol();
    }
    if (parser.getCurrentSymbol() != SQARLParserByteCodes.IDENTIFIER) {
      throw new CompileException(parser, "Error. Expected IDENTIFIER.");
    }

    // Add the name as a string
    String var = varProcess(parser.getCurrentWord());
    output.append(var).append(parser.getWhitespace()).append("=");

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() == SQARLParserByteCodes.AS) {
      output.append(parser.getWhitespace());

      parser.getNextSymbol();

      if (parser.getCurrentSymbol() != SQARLParserByteCodes.TYPE) {
        throw new CompileException(parser, "Error. Expected TYPE.");
      }

      if (parser.getCurrentSymbol() == SQARLParserByteCodes.ARRAY) {

        parser.getNextSymbol();

        if (parser.getCurrentSymbol() != SQARLParserByteCodes.OF) {
          throw new CompileException(parser, "Error. Expected OF.");
        }

        parser.getNextSymbol();

        if (parser.getCurrentSymbol() != SQARLParserByteCodes.TYPE && parser.getCurrentSymbol() != SQARLParserByteCodes.ARRAY) {
          throw new CompileException(parser, "Error. Expected TYPE.");
        }

        if (parser.getCurrentSymbol() == SQARLParserByteCodes.ARRAY) {

          parser.getNextSymbol();

          if (parser.getCurrentSymbol() != SQARLParserByteCodes.OF) {
            throw new CompileException(parser, "Error. Expected OF.");
          }

          parser.getNextSymbol();

          if (parser.getCurrentSymbol() != SQARLParserByteCodes.TYPE && parser.getCurrentSymbol() != SQARLParserByteCodes.ARRAY) {
            throw new CompileException(parser, "Error. Expected TYPE.");
          }

          parser.getNextSymbol();

          if (parser.getCurrentSymbol() != SQARLParserByteCodes.INITIALLY) {
            throw new CompileException(parser, "Error. Expected INITIALLY.");
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
            throw new CompileException(parser, "Error. Expected INITIALLY.");
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
          throw new CompileException(parser, "Error. Expected MULT.");
        }

        parser.getNextSymbol();

        output.append(" * ").append(parser.getCurrentWord());


        return output.toString();


      }

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

    } else if (parser.getCurrentSymbol() == SQARLParserByteCodes.ARRAY) {
      parser.getNextSymbol();

      if (parser.getCurrentSymbol() != SQARLParserByteCodes.LSQBR) {
        throw new CompileException(parser, "Error. Expected [ after ARRAY.");
      }

      output.append(compileValue());
    } else {
      if (!isValue(parser.getCurrentSymbol())) {


        throw new CompileException(parser, "Error. Expected TYPE.");
      }

      // Multiple values
      // compile_values();
      output.append(compileValue());

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
        output.append(convertType()).append(" ");
        parser.getNextSymbol();
      } else {
        throw new CompileException(parser, "Expected type in PROCEDURE signature parameters.");
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
        output.append(convertType()).append(" ");
        parser.getNextSymbol();
      } else {
        throw new CompileException(parser, "Expected type in PROCEDURE signature parameters.");
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
      throw new CompileException(parser, "RETURN not provided in a function");
    }

    return output.toString();
  }

  private String compileClass() throws CompileException {
    parser.getNextSymbol();
    if (parser.getCurrentSymbol() != SQARLParserByteCodes.IDENTIFIER) {
      throw new CompileException(parser, "IDENTIFIER not provided for a class");
    }

    StringBuilder output = new StringBuilder();
    output.append("class ").append(parser.getCurrentWord()).append(" ").append(System.lineSeparator());

    classes.add(parser.getCurrentWord());

    if (parser.getNextSymbol() == SQARLParserByteCodes.IS) {
      if (parser.getNextSymbol() != SQARLParserByteCodes.LBRACE) {
        throw new CompileException(parser, "LBRACE expected");
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
          throw new CompileException(parser, "Expected type in PROCEDURE signature parameters.");
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
      throw new CompileException(parser, "METHODS expected in class definition.");
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

  private String compileSend() throws CompileException {
    String output = "";
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.SEND) {
      parser.getNextSymbol();
    }

    output += "print(";

    if (!isValue(parser.getCurrentSymbol())) {
      throw new CompileException(parser, "Error. Expected VALUE.");
    }

    output += compileExpression();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.TO)
      parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.TO) {
      throw new CompileException(parser, "Error. Expected TO.");
    }

    parser.getNextSymbol();

    output += ")";

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.DISPLAY) {
      throw new CompileException(parser, "Error. Expected DISPLAY.");
    }

    output += parser.getWhitespace();

    return output;

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
    String output = "";
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.RECEIVE) {
      parser.getNextSymbol();
    }

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.IDENTIFIER) {
      throw new CompileException(parser, "Error. Expected IDENTIFIER.");
    }

    output += varProcess(parser.getCurrentWord());

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.FROM) {
      throw new CompileException(parser, "Error. Expected FROM.");
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
        throw new CompileException(parser, "Error. Expected INPUT.");
      }

      if (close) {
        output += ")";
      }
    } else {
      if (parser.getCurrentSymbol() != SQARLParserByteCodes.KEYBOARD) {
        throw new CompileException(parser, "Error. Expected INPUT.");
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
