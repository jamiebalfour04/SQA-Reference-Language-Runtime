package jamiebalfour.zpe;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

import jamiebalfour.HelperFunctions;
import jamiebalfour.zpe.core.*;
import jamiebalfour.zpe.core.errors.CompileError;
import jamiebalfour.zpe.parser.ZenithParsingEngine;

public class SQARLParser {

  ZenithParsingEngine parser;

  ArrayList<String> classes = new ArrayList<>();


  public static void main(String[] args) {

    HashMap<String, Object> argv = jamiebalfour.HelperFunctions.GenerateArgumentMap(args);
    String first = "";

    if (args.length == 0) {

      if (System.console() == null) {

        if(!ZPEHelperFunctions.isHeadless()){
          new SQARLEditorMain().setVisible(true);
        }

      } else{
        // If nothing has been provided
        System.out.println("If you are running this from the console, please provide at least one command line argument. You can use -r to run an SQARL program directly.");
        System.exit(0);
      }


    } else{
      first = args[0];

      if (first.equals("-r") && argv.containsKey("-r")) {
        // Run
        try {
          String s = jamiebalfour.HelperFunctions.ReadFileAsString(argv.get("-r").toString(), "utf-8");
          try{
            String output = compileAndRunSQARL(s);
            if (!output.isEmpty()) {
              System.out.println(output);
            }
          } catch(CompileError e){
            System.out.println(e.getMessage());
          }


        } catch (IOException e) {
          System.err.println("File not found!");
        }
      } else if (first.equals("-e")) {
        String s;
        try {
          s = jamiebalfour.HelperFunctions.ReadFileAsString(argv.get("-e").toString(), "utf-8");
          String output = compileSQARL(s);
          if (!output.isEmpty()) {
            System.out.println(output);
          }
        } catch (IOException e) {
          ZPE.Log("SQARL Runtime error: " + e.getMessage());
        }

      } else if (first.equals("-g")) {
        new SQARLEditorMain().setVisible(true);
      } else {
        System.out.println("You have provided incorrect arguments to the application.");
      }
    }



  }

  private String varProcess(String s){
    s = s.replace("count", "_count");
    s = s.replace("list", "_list");

    if(!s.startsWith("_")){
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

  public static String compileAndRunSQARL(String s) throws CompileError {
    SQARLParser sqarl = new SQARLParser();
    String yass = sqarl.parseToYASS(s);

    ZPERuntimeEnvironment z = new ZPERuntimeEnvironment();
    Object out = z.Interpret(yass);
    if (out != null) {
      return out.toString();
    }
    return "";
  }

  public static void compileAndRunSQARLGUI(String code) {

    SQARLParser sqarl = new SQARLParser();
    String yass = sqarl.parseToYASS(code);

    try {
      ZPEKit.compile(yass);
    } catch (CompileError e) {
      ZPE.Log("SQARL Runtime error: " + e.getMessage());
    }

  }

  public String parseToYASS(String code) {
    StringBuilder output = new StringBuilder();
    parser = new ZenithParsingEngine(code, false, new SQARLParserByteCodes());

    parser.getNextSymbol();
    while (parser.getCurrentSymbol() != -2) {
      output.append(parse_one());
      parser.getNextSymbol();
    }

    return output.toString();
  }
  
  

  // Simple method to get a single block
  private String parse_one() {
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.DECLARE) {
      return compile_declare() + System.getProperty("line.separator");
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.SET) {
      return compile_set() + System.getProperty("line.separator");
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.SEND) {
      return compile_send() + System.getProperty("line.separator");
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.RECEIVE) {
      return compile_receive() + System.getProperty("line.separator");
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.REPEAT) {
      return compile_repeat() + System.getProperty("line.separator");
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.IF) {
      return compile_if() + System.getProperty("line.separator");
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.WHILE) {
      return compile_while() + System.getProperty("line.separator");
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.FOR) {
        return compile_for() + System.getProperty("line.separator");
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.PROCEDURE){
      return compile_procedure() + System.getProperty("line.separator");
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.FUNCTION){
      return compile_function() + System.getProperty("line.separator");
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.CLASS){
      return compile_class() + System.getProperty("line.separator");
    }
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.NAME && parser.peekAhead() == SQARLParserByteCodes.LBRA){
      return compile_function_call() + System.getProperty("line.separator");
    }

    return "-1";
  }

  private boolean is_join(byte symb) {
    return symb == SQARLParserByteCodes.AND || symb == SQARLParserByteCodes.OR || symb == SQARLParserByteCodes.PLUS || symb == SQARLParserByteCodes.MINUS || symb == SQARLParserByteCodes.MULT || symb == SQARLParserByteCodes.DIVIDE || symb == SQARLParserByteCodes.MOD;
  }

  private boolean is_value(byte symb) {
    return symb == SQARLParserByteCodes.NAME || symb == SQARLParserByteCodes.INT || symb == SQARLParserByteCodes.STRING || symb == SQARLParserByteCodes.BOOLEAN || symb == SQARLParserByteCodes.REAL;
  }

  private boolean is_comparison(byte symb) {
    return symb == SQARLParserByteCodes.GT || symb == SQARLParserByteCodes.LT || symb == SQARLParserByteCodes.GTE || symb == SQARLParserByteCodes.LTE || symb == SQARLParserByteCodes.EQUAL || symb == SQARLParserByteCodes.NEQUAL;
  }

  private String compile_value() {
    StringBuilder output = new StringBuilder();
    if (is_value(parser.getCurrentSymbol())) {
      if (parser.getCurrentSymbol() == SQARLParserByteCodes.NAME && parser.peekAhead() != SQARLParserByteCodes.LBRA)
        output.append(varProcess(parser.getCurrentWord())).append(" ");
      else if (parser.getCurrentSymbol() == SQARLParserByteCodes.NAME && parser.peekAhead() == SQARLParserByteCodes.LBRA)
        output.append(compile_function_call());
      else if (parser.getCurrentSymbol() == SQARLParserByteCodes.BOOLEAN)
        output.append(parser.getCurrentWord().toLowerCase()).append(parser.getWhitespace());
      else if (parser.getCurrentSymbol() == SQARLParserByteCodes.STRING) {
        output.append("\"").append(parser.getCurrentWord()).append("\"").append(parser.getWhitespace());
      } else
        output.append(parser.getWhitespace()).append(parser.getCurrentWord());
    } else if (parser.getCurrentSymbol() == SQARLParserByteCodes.LSQBR) {
    	output = new StringBuilder("[");
    	parser.getNextSymbol();
    	while(parser.getCurrentSymbol() != SQARLParserByteCodes.RSQBR) {
    		output.append(compile_value());
    		parser.getNextSymbol();
    		if(parser.getCurrentSymbol() == SQARLParserByteCodes.COMMA) {
    			output.append(",");
    			parser.getNextSymbol();
    		}
    	}
    	output.append("]");
    	System.out.println(output);
    }

    return output.toString();
  }

  private String compile_expression() {
    StringBuilder output = new StringBuilder();
    while (true) {

      if (parser.getCurrentSymbol() == SQARLParserByteCodes.LBRA) {
        output.append("(");
        parser.getNextSymbol();
        output.append(compile_expression());
      }
      output.append(compile_value());

      if (parser.peekAhead() == SQARLParserByteCodes.RBRA) {
        output.append(")");
        parser.getNextSymbol();
        return output.toString();
      }
      if (is_comparison(parser.peekAhead())) {

        parser.getNextSymbol();
        output.append(parser.getCurrentWord()).append(parser.getWhitespace());
        parser.getNextSymbol();
        output.append(compile_value());

        if (parser.getCurrentSymbol() == SQARLParserByteCodes.RBRA) {
          output.append(")");
          parser.getNextSymbol();
        }
      }

      if (parser.peekAhead() == SQARLParserByteCodes.CONCAT){
        parser.getNextSymbol();
        parser.getNextSymbol();
        return output.append(" & ").append(compile_expression()).toString();
      }
      if (!is_join(parser.peekAhead()) && !is_join(parser.getCurrentSymbol())) {
        if(parser.peekAhead(2) == SQARLParserByteCodes.RBRA){
          parser.getNextSymbol();
          output.append(")");
        }
        return output.toString();
      } else {
        // Jump to
        if (is_join(parser.peekAhead()))
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

  private String compile_function_call(){
    StringBuilder output = new StringBuilder();

    output.append(parser.getCurrentWord());

    parser.getNextSymbol();

    output.append("(");

    parser.getNextSymbol();

    while(parser.getCurrentSymbol() != SQARLParserByteCodes.RBRA){
      output.append(parser.getCurrentWord());
      parser.getNextSymbol();

      if(parser.getCurrentSymbol() == SQARLParserByteCodes.COMMA){
        parser.getNextSymbol();
        output.append(", ");
      }
    }

    output.append(")");

    parser.getNextSymbol();

    return output.toString();
  }

  private String compile_repeat() {
    String output = "";
    String first = "";
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.REPEAT) {
      parser.getNextSymbol();
      first = "loop until (";
    }

    StringBuilder body = new StringBuilder();

    while (parser.getCurrentSymbol() != SQARLParserByteCodes.UNTIL) {
      body.append(parse_one());
      parser.getNextSymbol();
    }

    if (parser.getCurrentSymbol() == SQARLParserByteCodes.UNTIL) {
      parser.getNextSymbol();
      first += compile_expression() + ")";
    }

    output += first + " " + body + "end loop ";

    return output;

  }

  private String compile_if() {
    StringBuilder output = new StringBuilder();
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.IF) {
      parser.getNextSymbol();
      output.append("if (");
    }

    output.append(compile_expression());

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.THEN) {
      printError("Error. Expected THEN.");
    }

    output.append(") ");

    parser.getNextSymbol();

    while (parser.getCurrentSymbol() != SQARLParserByteCodes.END && parser.peekAhead() != SQARLParserByteCodes.IF) {
      output.append(parse_one());
      parser.getNextSymbol();
    }

    parser.getNextSymbol();

    output.append("end if ");

    return output.toString();

  }

  private String compile_while() {
    StringBuilder output = new StringBuilder();

    if (parser.getCurrentSymbol() == SQARLParserByteCodes.WHILE) {
      parser.getNextSymbol();
      output.append("while (");
    }

    output.append(compile_expression());

    parser.getNextSymbol();

    while (parser.getCurrentSymbol() != SQARLParserByteCodes.DO) {
      printError("Error. Expected DO.");
    }

    output.append(") ");

    parser.getNextSymbol();

    while (parser.getCurrentSymbol() != SQARLParserByteCodes.END && parser.peekAhead() != SQARLParserByteCodes.WHILE) {
      output.append(parse_one());
      parser.getNextSymbol();
    }

    parser.getNextSymbol();

    output.append("end while ");

    return output.toString();

  }
  
  private String compile_for() {
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
	    

	    output.append(compile_expression());

	    parser.getNextSymbol();
	    
	    if(each) {
	    	if (parser.getCurrentSymbol() != SQARLParserByteCodes.FROM) {
	  	      parser.getNextSymbol();
	  	      output.append(" in ");
	  	    }
	    	
	    	output.append(compile_expression());
	    }
	    

	    while (parser.getCurrentSymbol() != SQARLParserByteCodes.DO) {
	      printError("Error. Expected DO.");
	    }

	    output.append(") ");

	    parser.getNextSymbol();

	    while (parser.getCurrentSymbol() != SQARLParserByteCodes.END && parser.peekAhead() != SQARLParserByteCodes.FOR) {
	      output.append(parse_one());
	      parser.getNextSymbol();
	    }

	    parser.getNextSymbol();

	    output.append("end for ");

	    return output.toString();

	  }

  private String compile_set() {
    String output = "";
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.SET) {
      parser.getNextSymbol();
    }

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.NAME) {
      printError("Error. Expected NAME_STRING.");
    }

    // Add the name as a string
    String var = varProcess(parser.getCurrentWord());
    output += var + " = ";

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.TO) {
      printError("Error. Expected TO IN SET.");
    }

    parser.getNextSymbol();

    if (!is_value(parser.getCurrentSymbol()) && !(parser.getCurrentSymbol() == SQARLParserByteCodes.LBRA)) {
      printError("Error. Expected TYPE.");
    }

    output += compile_expression();

    return output;

  }

  private String compile_declare() {
    String output = "";
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.DECLARE) {
      parser.getNextSymbol();
    }
    if (parser.getCurrentSymbol() != SQARLParserByteCodes.NAME) {
      printError("Error. Expected NAME_STRING.");
    }

    // Add the name as a string
    String var = varProcess(parser.getCurrentWord());
    output += var + parser.getWhitespace() + "=";

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() == SQARLParserByteCodes.AS) {
      output += parser.getWhitespace();

      parser.getNextSymbol();

      if (parser.getCurrentSymbol() != SQARLParserByteCodes.TYPE) {
        printError("Error. Expected TYPE.");
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
        output += "auto_input()";
      }
    } else if(parser.getCurrentSymbol() == SQARLParserByteCodes.NAME && parser.peekAhead() == SQARLParserByteCodes.LBRA && classes.contains(parser.getCurrentWord())) {
      output += " new " + parser.getCurrentWord() + " (";
      parser.getNextSymbol();
      parser.getNextSymbol();

      while(parser.getCurrentSymbol() != SQARLParserByteCodes.RBRA){
        output += parser.getCurrentWord();
        parser.getNextSymbol();

        if(parser.getCurrentSymbol() == SQARLParserByteCodes.COMMA){
          output += ", ";
          parser.getNextSymbol();
        }
      }

      output += ") ";
    } else if (parser.getCurrentSymbol() == SQARLParserByteCodes.LSQBR) {
    	//Array
    	output += compile_value();
    	
    } else {
      if (!is_value(parser.getCurrentSymbol())) {
    	  
    	  
    	  
        printError("Error. Expected TYPE.");
      }

      // Multiple values
      // compile_values();
      output += compile_value();

    }

    output += parser.getWhitespace();

    return output;

  }

  private String compile_procedure(){
    StringBuilder output = new StringBuilder("function ");

    parser.getNextSymbol();

    output.append(parser.getCurrentWord());

    parser.getNextSymbol();

    if(parser.getCurrentSymbol() != SQARLParserByteCodes.LBRA){
      printError("Error. Expected LBRACKET.");
    }

    output.append(" (");

    parser.getNextSymbol();

    while(parser.getCurrentSymbol() != SQARLParserByteCodes.RBRA){
      if(is_type()){
        output.append(convertType()).append(" ");
        parser.getNextSymbol();
      } else{
        printError("Expected type in PROCEDURE signature parameters.");
      }


      output.append(parser.getCurrentWord());

      parser.getNextSymbol();

      if(parser.getCurrentSymbol() == SQARLParserByteCodes.COMMA){
        output.append(", ");
        parser.getNextSymbol();
      }
    }

    output.append(") ");

    parser.getNextSymbol();
    while (parser.getCurrentSymbol() != SQARLParserByteCodes.END && parser.peekAhead() != SQARLParserByteCodes.PROCEDURE) {
      output.append(parse_one());
      parser.getNextSymbol();
    }

    parser.getNextSymbol();

    output.append(" end function ");

    return output.toString();
  }

  private String compile_function(){
    boolean returnFound = false;

    StringBuilder output = new StringBuilder("function ");

    parser.getNextSymbol();

    output.append(parser.getCurrentWord());

    parser.getNextSymbol();

    if(parser.getCurrentSymbol() != SQARLParserByteCodes.LBRA){
      printError("Error. Expected LBRACKET.");
    }

    output.append(" (");

    parser.getNextSymbol();

    while(parser.getCurrentSymbol() != SQARLParserByteCodes.RBRA){
      if(is_type()){
        output.append(convertType()).append(" ");
        parser.getNextSymbol();
      } else{
        printError("Expected type in PROCEDURE signature parameters.");
      }


      output.append(parser.getCurrentWord());

      parser.getNextSymbol();

      if(parser.getCurrentSymbol() == SQARLParserByteCodes.COMMA){
        output.append(", ");
        parser.getNextSymbol();
      }
    }

    output.append(") ");

    parser.getNextSymbol();

    if(parser.getCurrentSymbol() == SQARLParserByteCodes.RETURNS){
      parser.getNextSymbol();
      parser.getNextSymbol();
    }

    while (parser.getCurrentSymbol() != SQARLParserByteCodes.END && parser.peekAhead() != SQARLParserByteCodes.FUNCTION) {
      if(parser.getCurrentSymbol() == SQARLParserByteCodes.RETURN){
        output.append("return ");
        parser.getNextSymbol();
        output.append(compile_expression());
        returnFound = true;
      } else{
        output.append(parse_one());
      }

      parser.getNextSymbol();
    }

    parser.getNextSymbol();

    output.append(" end function");

    if(!returnFound){
      printError("RETURN not provided in a function");
    }

    return output.toString();
  }

  private String compile_class(){
    parser.getNextSymbol();
    if(parser.getCurrentSymbol() != SQARLParserByteCodes.NAME){
      //printError("IDENTIFIER not provided for a class");
    }

    StringBuilder output = new StringBuilder();
    output.append("class " + parser.getCurrentWord() + " " + System.getProperty("line.separator"));

    classes.add(parser.getCurrentWord());

    if(parser.getNextSymbol() == SQARLParserByteCodes.IS){
      if(parser.getNextSymbol() != SQARLParserByteCodes.LBRACE){
        printError("LBRACE expected");
      }

      parser.getNextSymbol();

      StringBuilder params = new StringBuilder();

      params.append("( ");
      //Parser params
      while(parser.getCurrentSymbol() != SQARLParserByteCodes.RBRACE){
        if(is_type()){
          params.append(convertType()).append(" ");
          parser.getNextSymbol();
        } else{
          printError("Expected type in PROCEDURE signature parameters.");
        }


        params.append(parser.getCurrentWord());

        parser.getNextSymbol();

        if(parser.getCurrentSymbol() == SQARLParserByteCodes.COMMA){
          params.append(", ");
          parser.getNextSymbol();
        }
      }
      params.append(") ");
    }



    parser.getNextSymbol();

    if(parser.getCurrentSymbol() != SQARLParserByteCodes.METHODS){
      printError("METHODS expected in class definition.");
    }


    parser.getNextSymbol();

    while (!(parser.getCurrentSymbol() == SQARLParserByteCodes.END && parser.peekAhead() == SQARLParserByteCodes.CLASS)){
      if(parser.getCurrentSymbol() == SQARLParserByteCodes.FUNCTION){
        output.append(compile_function()).append(System.getProperty("line.separator"));
        parser.getNextSymbol();
      } else if (parser.getCurrentSymbol() == SQARLParserByteCodes.PROCEDURE){
        output.append(compile_procedure()).append(System.getProperty("line.separator"));
        parser.getNextSymbol();
      }
    }

    output.append(" end class");
    parser.getNextSymbol();
    //parser.getNextSymbol();

    return output.toString();
  }

  private String compile_send() {
    String output = "";
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.SEND) {
      parser.getNextSymbol();
    }

    output += "print(";

    if (!is_value(parser.getCurrentSymbol())) {
      printError("Error. Expected VALUE.");
    }

    output += compile_expression();

    if(parser.getCurrentSymbol() != SQARLParserByteCodes.TO)
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

  private String compile_receive() {
    String output = "";
    if (parser.getCurrentSymbol() == SQARLParserByteCodes.RECEIVE) {
      parser.getNextSymbol();
    }

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.NAME) {
      printError("Error. Expected NAME_STRING.");
    }

    output += varProcess(parser.getCurrentWord());

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != SQARLParserByteCodes.FROM) {
      printError("Error. Expected FROM.");
    }

    parser.getNextSymbol();

    if (is_type()) {
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

  private boolean is_type() {
    return parser.getCurrentSymbol() == SQARLParserByteCodes.TYPE || (parser.getCurrentSymbol() == SQARLParserByteCodes.LBRA && parser.peekAhead() == SQARLParserByteCodes.TYPE);

  }

  private String convertType(){
    String output = "";

    if(parser.getCurrentWord().equals("INTEGER") || parser.getCurrentWord().equals("REAL")){
      return "number";
    }

    if(parser.getCurrentWord().equals("STRING") || parser.getCurrentWord().equals("CHARACTER")){
      return "string";
    }

    if(parser.getCurrentWord().equals("BOOLEAN")){
      return "boolean";
    }

    return output;
  }

}
