import java.io.IOException;
import java.util.HashMap;

import jamiebalfour.zpe.core.YASSRuntime;
import jamiebalfour.zpe.core.ZPEKit;
import jamiebalfour.zpe.core.errors.CompileError;
import jamiebalfour.zpe.parser.ZenithParsingEngine;

public class YASSHaggisParser {

  ZenithParsingEngine parser;

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
      first = args[0].toString();
    }

    if (first.equals("-r") && argv.containsKey("-r")) {
      // Run
      try {
        String s = jamiebalfour.HelperFunctions.ReadFileAsString(argv.get("-r").toString(), "utf-8");
        String output = compileAndRunHaggis(s);
        if (!output.equals("")) {
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
			// TODO Auto-generated catch block
			e.printStackTrace();
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
	  String yass = haggis.parseToYASS(s);
	  return yass;
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
      ZPEKit.Compile(yass);
    } catch (CompileError e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  public String parseToYASS(String code) {
    String output = "";
    parser = new ZenithParsingEngine(code, false, new YASSHaggisParserByteCodes());

    parser.getNextSymbol();
    while (parser.getCurrentSymbol() != -2) {
      output += parse_one();
      parser.getNextSymbol();
    }

    return output;
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

    return "-1";
  }

  private boolean is_join(byte symb) {

    if (symb == YASSHaggisParserByteCodes.AND || symb == YASSHaggisParserByteCodes.OR || symb == YASSHaggisParserByteCodes.PLUS || symb == YASSHaggisParserByteCodes.MINUS || symb == YASSHaggisParserByteCodes.MULT || symb == YASSHaggisParserByteCodes.DIVIDE || symb == YASSHaggisParserByteCodes.MOD) {
      return true;
    }

    return false;
  }

  private boolean is_value(byte symb) {
    return symb == YASSHaggisParserByteCodes.NAME || symb == YASSHaggisParserByteCodes.INT || symb == YASSHaggisParserByteCodes.STRING || symb == YASSHaggisParserByteCodes.BOOLEAN || symb == YASSHaggisParserByteCodes.REAL;
  }

  private boolean is_comparison(byte symb) {
    return symb == YASSHaggisParserByteCodes.GT || symb == YASSHaggisParserByteCodes.LT || symb == YASSHaggisParserByteCodes.GTE || symb == YASSHaggisParserByteCodes.LTE || symb == YASSHaggisParserByteCodes.EQUAL || symb == YASSHaggisParserByteCodes.NEQUAL;
  }

  private String compile_value() {
    String output = "";
    if (is_value(parser.getCurrentSymbol())) {
      if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.NAME)
        output += "$" + parser.getCurrentWord() + parser.getWhitespace();
      else if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.BOOLEAN)
        output += parser.getCurrentWord().toLowerCase() + parser.getWhitespace();
      else if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.STRING) {
        output += "\"" + parser.getCurrentWord() + "\"" + parser.getWhitespace();
      } else
        output += parser.getWhitespace() + parser.getCurrentWord();
    } else if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.LSQBR) {
    	output = "[";
    	parser.getNextSymbol();
    	while(parser.getCurrentSymbol() != YASSHaggisParserByteCodes.RSQBR) {
    		output += compile_value();
    		parser.getNextSymbol();
    		if(parser.getCurrentSymbol() == YASSHaggisParserByteCodes.COMMA) {
    			output += ",";
    			parser.getNextSymbol();
    		}
    	}
    	output += "]";
    	System.out.println(output);
    }

    return output;
  }

  private String compile_expression() {
    String output = "";
    while (true) {

      if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.LBRA) {
        output += "(";
        parser.getNextSymbol();
      }
      output += compile_value();

      if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.RBRA) {
        output += ")";
        parser.getNextSymbol();
      }
      if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.LBRA) {
        output += "(";
        parser.getNextSymbol();
      }
      if (is_comparison(parser.peekAhead())) {

        parser.getNextSymbol();
        output += parser.getCurrentWord() + parser.getWhitespace();
        parser.getNextSymbol();
        output += compile_value();

        if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.RBRA) {
          output += ")";
          parser.getNextSymbol();
        }
      }

      if (!is_join(parser.peekAhead()) && !is_join(parser.getCurrentSymbol())) {
        return output;
      } else {
        // Jump to
        if (is_join(parser.peekAhead()))
          parser.getNextSymbol();
        if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.MOD) {
          output += " % " + parser.getWhitespace();
          parser.getNextSymbol();
        } else {
          output += " " + parser.getCurrentWord().toLowerCase() + " " + parser.getWhitespace();
          parser.getNextSymbol();
        }
      }

    }

  }

  private String compile_repeat() {
    String output = "";
    String first = "";
    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.REPEAT) {
      parser.getNextSymbol();
      first = "loop until (";
    }

    String body = "";

    while (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.UNTIL) {
      body += parse_one();
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
    String output = "";
    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.IF) {
      parser.getNextSymbol();
      output += "if (";
    }

    output += compile_expression();

    parser.getNextSymbol();

    if (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.THEN) {
      printError("Error. Expected THEN.");
    }

    output += ") ";

    parser.getNextSymbol();

    while (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.END && parser.peekAhead() != YASSHaggisParserByteCodes.IF) {
      output += parse_one();
      parser.getNextSymbol();
    }

    parser.getNextSymbol();

    output += "end if ";

    return output;

  }

  private String compile_while() {
    String output = "";

    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.WHILE) {
      parser.getNextSymbol();
      output += "while (";
    }

    output += compile_expression();

    parser.getNextSymbol();

    while (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.DO) {
      printError("Error. Expected DO.");
    }

    output += ") ";

    parser.getNextSymbol();

    while (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.END && parser.peekAhead() != YASSHaggisParserByteCodes.WHILE) {
      output += parse_one();
      parser.getNextSymbol();
    }

    parser.getNextSymbol();

    output += "end while ";

    return output;

  }
  
  private String compile_for() {
	    String output = "";
	    
	    boolean each = false;

	    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.FOR) {
	      parser.getNextSymbol();
	      output += "for ";
	    }
	    
	    if (parser.peekAhead() == YASSHaggisParserByteCodes.EACH) {
	    	output += "each ";
	    	each = true;
	    }
	    
	    output += "(";
	    

	    output += compile_expression();

	    parser.getNextSymbol();
	    
	    if(each) {
	    	if (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.FROM) {
	  	      parser.getNextSymbol();
	  	      output += " in ";
	  	    }
	    	
	    	output += compile_expression();
	    }
	    

	    while (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.DO) {
	      printError("Error. Expected DO.");
	    }

	    output += ") ";

	    parser.getNextSymbol();

	    while (parser.getCurrentSymbol() != YASSHaggisParserByteCodes.END && parser.peekAhead() != YASSHaggisParserByteCodes.FOR) {
	      output += parse_one();
	      parser.getNextSymbol();
	    }

	    parser.getNextSymbol();

	    output += "end for ";

	    return output;

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

  private String compile_send() {
    String output = "";
    if (parser.getCurrentSymbol() == YASSHaggisParserByteCodes.SEND) {
      parser.getNextSymbol();
    }

    output += "print(";

    // TODO: Update with value etc.
    if (!is_value(parser.getCurrentSymbol())) {
      printError("Error. Expected VALUE.");
    }

    output += compile_expression();

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

      if (parser.getCurrentWord().toLowerCase() == "integer") {
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

}
