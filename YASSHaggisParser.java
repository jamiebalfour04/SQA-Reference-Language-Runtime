import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import jamiebalfour.zpe.core.YASSByteCodes;
import jamiebalfour.zpe.core.YASSRuntime;
import jamiebalfour.zpe.core.ZPE;
import jamiebalfour.zpe.core.ZPEKit;
import jamiebalfour.zpe.core.errors.CompileError;
import jamiebalfour.zpe.parser.ZenithParsingEngine;

public class YASSHaggisParser {

  ZenithParsingEngine parser;
  ArrayList<String> procedures = new ArrayList<>();

  public static void main(String[] args) {

    HashMap<String, Object> argv = jamiebalfour.HelperFunctions.GenerateArgumentMap(args);
    String first = "";

    if (args.length == 0) {

      if (System.console() == null) {
    	  
        new HaggisEditorMain().setVisible(true);
      } else{

        // If nothing has been provided
        System.out.println("If you are running this from the console, please provide at least one command line argument. You can use -r to run a HAGGIS program directly.");
        System.exit(0);
      }


    }

    if (args.length > 0) {
      first = args[0];
    }

    if (first.equals("-r") && argv.containsKey("-r")) {
      // Run
      try {
        String s = jamiebalfour.HelperFunctions.ReadFileAsString(argv.get("-r").toString(), "utf-8");
        String output = compileAndRunHaggis(s);
        if (!output.isEmpty()) {
          System.out.println(output);
        }

      } catch (IOException e) {
        System.err.println("File not found!");
      }
    } else if (first.equals("-e")) {
    	String s;
		try {
			s = jamiebalfour.HelperFunctions.ReadFileAsString(argv.get("-e").toString(), "utf-8");
			String output = compileHaggis(s);
	        if (!output.isEmpty()) {
	          System.out.println(output);
	        }
		} catch (IOException e) {
          ZPE.Log("Haggis Runtime error: " + e.getMessage());
		}
        
    } else if (first.equals("-g")) {
  	  new HaggisEditorMain().setVisible(true);
    } else {
      System.out.println("You have provided incorrect arguments to the application.");
    }

  }

  private void printError(String err) {
    System.err.println(err);
    System.exit(-1);
  }
  
  public static String compileHaggis(String s) {
	  YASSHaggisParser haggis = new YASSHaggisParser();
      return haggis.parseToYASS(s);
  }

  public static String compileAndRunHaggis(String s) {
    YASSHaggisParser haggis = new YASSHaggisParser();
    String yass = haggis.parseToYASS(s);

    YASSRuntime z = new YASSRuntime();
    Object out = z.Interpret(yass);
    if (out != null) {
      return out.toString();
    }
    return "";
  }

  public static void compileAndRunHaggisGUI(String code) {

    YASSHaggisParser haggis = new YASSHaggisParser();
    String yass = haggis.parseToYASS(code);

    try {
      ZPEKit.compile(yass);
    } catch (CompileError e) {
      ZPE.Log("Haggis Runtime error: " + e.getMessage());
    }

  }

  public String parseToYASS(String code) {
    StringBuilder output = new StringBuilder();
    parser = new ZenithParsingEngine(code, false, new YASSHaggisParserByteCodes());

    parser.getNextSymbol();
    while (parser.getCurrentSymbol() != -2) {
      output.append(parse_one());
      parser.getNextSymbol();
    }

    return output.toString();
  }
  
  

  // Simple method to get a single block
  private String parse_one() {
    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.DECLARE) {
      return compile_declare() + System.getProperty("line.separator");
    }
    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.SET) {
      return compile_set() + System.getProperty("line.separator");
    }
    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.SEND) {
      return compile_send() + System.getProperty("line.separator");
    }
    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.RECEIVE) {
      return compile_receive() + System.getProperty("line.separator");
    }
    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.REPEAT) {
      return compile_repeat() + System.getProperty("line.separator");
    }
    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.IF) {
      return compile_if() + System.getProperty("line.separator");
    }
    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.WHILE) {
      return compile_while() + System.getProperty("line.separator");
    }
    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.FOR) {
        return compile_for() + System.getProperty("line.separator");
    }
    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.PROCEDURE){
      return compile_procedure() + System.getProperty("line.separator");
    }
    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.NAME && parser.peekAhead() == YASSHaggisParserByteCodes.LBRA){
      return compile_function_call() + System.getProperty("line.separator");
    }

    return "-1";
  }

  private boolean is_join(byte symb) {
    return symb == YASSHaggisParserByteCodes.AND || symb == YASSHaggisParserByteCodes.OR || symb == YASSHaggisParserByteCodes.PLUS || symb == YASSHaggisParserByteCodes.MINUS || symb == YASSHaggisParserByteCodes.MULT || symb == YASSHaggisParserByteCodes.DIVIDE || symb == YASSHaggisParserByteCodes.MOD;
  }

  private boolean is_value(byte symb) {
    return symb == YASSHaggisParserByteCodes.NAME || symb == YASSHaggisParserByteCodes.INT || symb == YASSHaggisParserByteCodes.STRING || symb == YASSHaggisParserByteCodes.BOOLEAN || symb == YASSHaggisParserByteCodes.REAL;
  }

  private boolean is_comparison(byte symb) {
    return symb == YASSHaggisParserByteCodes.GT || symb == YASSHaggisParserByteCodes.LT || symb == YASSHaggisParserByteCodes.GTE || symb == YASSHaggisParserByteCodes.LTE || symb == YASSHaggisParserByteCodes.EQUAL || symb == YASSHaggisParserByteCodes.NEQUAL;
  }

  private String compile_value() {
    StringBuilder output = new StringBuilder();
    if (is_value(parser.getCurrentSymbol())) {
      if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.NAME && parser.peekAhead() != YASSHaggisParserByteCodes.LBRA)
        output.append("$").append(parser.getCurrentWord()).append(" ");
      else if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.NAME && parser.peekAhead() == YASSHaggisParserByteCodes.LBRA)
        output.append(compile_function_call());
      else if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.BOOLEAN)
        output.append(parser.getCurrentWord().toLowerCase()).append(parser.getWhitespace());
      else if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.STRING) {
        output.append("\"").append(parser.getCurrentWord()).append("\"").append(parser.getWhitespace());
      } else
        output.append(parser.getWhitespace()).append(parser.getCurrentWord());
    } else if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.LSQBR) {
    	output = new StringBuilder("[");
    	parser.getNextSymbol();
    	while(parser.getCurrentSymbol() != YASSHaggisParserByteCodes.RSQBR) {
    		output.append(compile_value());
    		parser.getNextSymbol();
    		if(parser.getCurrentSymbol() == YASSHaggisParserByteCodes.COMMA) {
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

      if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.LBRA) {
        output.append("(");
        parser.getNextSymbol();
      }
      output.append(compile_value());

      if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.RBRA) {
        output.append(")");
        parser.getNextSymbol();
      }
      if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.LBRA) {
        output.append("(");
        parser.getNextSymbol();
      }
      if (is_comparison(parser.peekAhead())) {

        parser.getNextSymbol();
        output.append(parser.getCurrentWord()).append(parser.getWhitespace());
        parser.getNextSymbol();
        output.append(compile_value());

        if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.RBRA) {
          output.append(")");
          parser.getNextSymbol();
        }
      }

      if (!is_join(parser.peekAhead()) && !is_join(parser.getCurrentSymbol())) {
        return output.toString();
      } else {
        // Jump to
        if (is_join(parser.peekAhead()))
          parser.getNextSymbol();
        if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.MOD) {
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

    while(parser.getCurrentSymbol() != YASSHaggisParserByteCodes.RBRA){
      output.append(parser.getCurrentWord());
      parser.getNextSymbol();

      if(parser.getCurrentSymbol() == YASSHaggisParserByteCodes.COMMA){
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
    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.REPEAT) {
      parser.getNextSymbol();
      first = "loop until (";
    }

    StringBuilder body = new StringBuilder();

    while (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.UNTIL) {
      body.append(parse_one());
      parser.getNextSymbol();
    }

    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.UNTIL) {
      parser.getNextSymbol();
      first += compile_expression() + ")";
    }

    output += first + " " + body + "end loop ";

    return output;

  }

  private String compile_if() {
    StringBuilder output = new StringBuilder();
    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.IF) {
      parser.getNextSymbol();
      output.append("if (");
    }

    output.append(compile_expression());

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.THEN) {
      printError("Error. Expected THEN.");
    }

    output.append(") ");

    parser.getNextSymbol();

    while (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.END && parser.peekAhead() != YASSHaggisParserByteCodes.IF) {
      output.append(parse_one());
      parser.getNextSymbol();
    }

    parser.getNextSymbol();

    output.append("end if ");

    return output.toString();

  }

  private String compile_while() {
    StringBuilder output = new StringBuilder();

    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.WHILE) {
      parser.getNextSymbol();
      output.append("while (");
    }

    output.append(compile_expression());

    parser.getNextSymbol();

    while (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.DO) {
      printError("Error. Expected DO.");
    }

    output.append(") ");

    parser.getNextSymbol();

    while (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.END && parser.peekAhead() != YASSHaggisParserByteCodes.WHILE) {
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

	    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.FOR) {
	      parser.getNextSymbol();
	      output.append("for ");
	    }
	    
	    if (parser.peekAhead() == YASSHaggisParserByteCodes.EACH) {
	    	output.append("each ");
	    	each = true;
	    }
	    
	    output.append("(");
	    

	    output.append(compile_expression());

	    parser.getNextSymbol();
	    
	    if(each) {
	    	if (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.FROM) {
	  	      parser.getNextSymbol();
	  	      output.append(" in ");
	  	    }
	    	
	    	output.append(compile_expression());
	    }
	    

	    while (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.DO) {
	      printError("Error. Expected DO.");
	    }

	    output.append(") ");

	    parser.getNextSymbol();

	    while (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.END && parser.peekAhead() != YASSHaggisParserByteCodes.FOR) {
	      output.append(parse_one());
	      parser.getNextSymbol();
	    }

	    parser.getNextSymbol();

	    output.append("end for ");

	    return output.toString();

	  }

  private String compile_set() {
    String output = "";
    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.SET) {
      parser.getNextSymbol();
    }

    if (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.NAME) {
      printError("Error. Expected NAME_STRING.");
    }

    // Add the name as a string
    String var = "$" + parser.getCurrentWord();
    output += var + " = ";

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.TO) {
      printError("Error. Expected TO.");
    }

    parser.getNextSymbol();

    if (!is_value(parser.getCurrentSymbol()) && !(parser.getCurrentSymbol() == YASSHaggisParserByteCodes.LBRA)) {
      printError("Error. Expected TYPE.");
    }

    output += compile_expression();

    return output;

  }

  private String compile_declare() {
    String output = "";
    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.DECLARE) {
      parser.getNextSymbol();
    }
    if (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.NAME) {
      printError("Error. Expected NAME_STRING.");
    }
    // Add the name as a string
    String var = "$" + parser.getCurrentWord();
    output += var + parser.getWhitespace() + "=";

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.AS) {
      output += parser.getWhitespace();

      parser.getNextSymbol();

      if (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.TYPE) {
        printError("Error. Expected TYPE.");
      }

      parser.getNextSymbol();
    }

    


    if (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.INITIALLY) {
      printError("Error. Expected INITIALLY.");
    }

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.FROM) {
      parser.getNextSymbol();

      if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.KEYBOARD) {
        output += "auto_input()";
      }
    } else if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.LSQBR) {
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

    if(parser.getCurrentSymbol() != YASSHaggisParserByteCodes.LBRA){
      printError("Error. Expected LBRACKET.");
    }

    output.append(" (");

    parser.getNextSymbol();

    while(parser.getCurrentSymbol() != YASSHaggisParserByteCodes.RBRA){
      if(is_type()){
        output.append(convertType()).append(" ");
        parser.getNextSymbol();
      } else{
        printError("Expected type in PROCEDURE signature parameters.");
      }


      output.append("$").append(parser.getCurrentWord());

      parser.getNextSymbol();

      if(parser.getCurrentSymbol() == YASSHaggisParserByteCodes.COMMA){
        output.append(", ");
        parser.getNextSymbol();
      }
    }

    output.append(") ");

    parser.getNextSymbol();
    while (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.END && parser.peekAhead() != YASSHaggisParserByteCodes.PROCEDURE) {
      output.append(parse_one());
      parser.getNextSymbol();
    }

    parser.getNextSymbol();

    output.append(" end function");

    return output.toString();
  }

  private String compile_send() {
    String output = "";
    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.SEND) {
      parser.getNextSymbol();
    }

    output += "print(";

    if (!is_value(parser.getCurrentSymbol())) {
      printError("Error. Expected VALUE.");
    }

    output += compile_expression();

    if(parser.getCurrentSymbol() != YASSHaggisParserByteCodes.TO)
      parser.getNextSymbol();

    if (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.TO) {
      printError("Error. Expected TO.");
    }

    parser.getNextSymbol();

    output += ")";

    if (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.DISPLAY) {
      printError("Error. Expected DISPLAY.");
    }

    output += parser.getWhitespace();

    return output;

  }

  private String compile_receive() {
    String output = "";
    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.RECEIVE) {
      parser.getNextSymbol();
    }

    if (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.NAME) {
      printError("Error. Expected NAME_STRING.");
    }

    output += "$" + parser.getCurrentWord();

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.FROM) {
      printError("Error. Expected FROM.");
    }

    parser.getNextSymbol();

    if (is_type()) {
      boolean brackets = false;
      if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.LBRA) {
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
        if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.RBRA) {
          parser.getNextSymbol();
        }
      }

      if (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.KEYBOARD) {
        printError("Error. Expected INPUT.");
      }

      if (close) {
        output += ")";
      }
    } else {
      if (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.KEYBOARD) {
        printError("Error. Expected INPUT.");
      }
    }

    output += parser.getWhitespace() + " = auto_input()";

    return output;
  }

  private boolean is_type() {
    return parser.getCurrentSymbol() == YASSHaggisParserByteCodes.TYPE || (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.LBRA && parser.peekAhead() == YASSHaggisParserByteCodes.TYPE);

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
