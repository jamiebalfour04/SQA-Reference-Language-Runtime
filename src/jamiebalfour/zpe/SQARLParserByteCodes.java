package jamiebalfour.zpe;

import java.util.ArrayList;

import jamiebalfour.zpe.parser.ZenithParsingEngine.MultilineComment;


public class SQARLParserByteCodes implements jamiebalfour.zpe.parser.Tokeniser {
	
	
	final static String INTEGER_REGEX = "(((-?[1-9]+)|[0-9][0-9]*)(E[0-9]+)?)";
	final static String DOUBLE_REGEX = "(-?[0-9]+((\\.)[0-9]+))(E[0-9]+)?";
	final static String STRING_REGEX = "(\".*\")|('.*')";
	final static String NULL_REGEX = "null";
	final static String BOOLEAN_REGEX = "(true|false)";
	//A string of characters outwith quotes
	final static String IDENTIFIER_REGEX = "([A-z][A-z0-9_]*)";

  final static byte ARRAY = -1;
	final static byte DECLARE = 0;
	final static byte AS = 1;
	final static byte TYPE = 2;
	final static byte INITIALLY = 3;
	final static byte FROM = 4;
	final static byte KEYBOARD = 5;
	
	final static byte RECEIVE = 6;
	
	final static byte SEND = 7;
	final static byte TO = 8;
	final static byte DISPLAY = 9;
	
	final static byte SET = 10;
	final static byte IDENTIFIER = 11;
	final static byte REPEAT = 12;
	final static byte UNTIL = 13;
	final static byte IF = 14;
	final static byte THEN = 15;
	final static byte END = 16;
	final static byte WHILE = 17;
	final static byte DO = 18;
	final static byte FOR = 19;
	final static byte EACH = 20;
	
	
	final static byte INT = 29;
	final static byte STRING = 30;
	final static byte BOOLEAN = 31;
	final static byte REAL = 32;

	
	final static byte AND = 40;
	final static byte OR = 41;	
	final static byte NOT = 42;
	final static byte NEQUAL = 43;	
	final static byte EQUAL = 44;
	final static byte GT = 45;
	final static byte LT = 46;
	final static byte GTE = 47;
	final static byte LTE = 48;
	

	final static byte PLUS = 49;	
	final static byte MINUS = 50;
	final static byte DIVIDE = 51;
	final static byte MULT = 52;
	final static byte MOD = 53;
	final static byte CONCAT = 54;

	final static byte LBRACE = 58;
	final static byte RBRACE = 59;
	final static byte LBRA = 60;
	final static byte RBRA = 61;
	final static byte LSQBR = 62;
	final static byte RSQBR = 63;
	final static byte COMMA = 70;

	final static byte PROCEDURE = 71;
	final static byte FUNCTION = 72;
	final static byte RETURN = 73;
	final static byte CLASS = 74;
	final static byte IS = 75;
  final static byte OF = 76;
	final static byte METHODS = 77;
	final static byte CONSTRUCTOR = 78;
	final static byte RETURNS = 79;

	final static byte RECORD = 80;


	
	
	
	
	

	@Override
	public byte stringToByteCode(String w) {

    switch (w) {
      case "DECLARE":
        return SQARLParserByteCodes.DECLARE;
      case "AS":
        return SQARLParserByteCodes.AS;
      case "INTEGER":
      case "BOOLEAN":
      case "REAL":
      case "STRING":
        return SQARLParserByteCodes.TYPE;
      case "ARRAY":
        return SQARLParserByteCodes.ARRAY;
      case "INITIALLY":
        return SQARLParserByteCodes.INITIALLY;
      case "FROM":
        return SQARLParserByteCodes.FROM;
      case "KEYBOARD":
        return SQARLParserByteCodes.KEYBOARD;
      case "RECEIVE":
        return SQARLParserByteCodes.RECEIVE;
      case "CLASS":
        return SQARLParserByteCodes.CLASS;
      case "RECORD":
        return SQARLParserByteCodes.RECORD;
      case "IS":
        return SQARLParserByteCodes.IS;
      case "OF":
        return SQARLParserByteCodes.OF;
      case "METHODS":
        return SQARLParserByteCodes.METHODS;
      case "CONSTRUCTOR":
        return SQARLParserByteCodes.CONSTRUCTOR;
      case "SEND":
        return SQARLParserByteCodes.SEND;
      case "TO":
        return SQARLParserByteCodes.TO;
      case "DISPLAY":
        return SQARLParserByteCodes.DISPLAY;
      case "SET":
        return SQARLParserByteCodes.SET;
      case "AND":
        return SQARLParserByteCodes.AND;
      case "OR":
        return SQARLParserByteCodes.OR;
      case "NOT":
        return SQARLParserByteCodes.NOT;
      case "&":
        return SQARLParserByteCodes.CONCAT;
      case "!=":
        return SQARLParserByteCodes.NEQUAL;
      case "=":
        return SQARLParserByteCodes.EQUAL;
      case ">":
        return SQARLParserByteCodes.GT;
      case "<":
        return SQARLParserByteCodes.LT;
      case ">=":
        return SQARLParserByteCodes.GTE;
      case "<=":
        return SQARLParserByteCodes.LTE;
      case "(":
        return SQARLParserByteCodes.LBRA;
      case ")":
        return SQARLParserByteCodes.RBRA;
      case "{":
        return SQARLParserByteCodes.LBRACE;
      case "}":
        return SQARLParserByteCodes.RBRACE;
      case "[":
        return SQARLParserByteCodes.LSQBR;
      case "]":
        return SQARLParserByteCodes.RSQBR;
      case ",":
        return SQARLParserByteCodes.COMMA;
      case "+":
        return SQARLParserByteCodes.PLUS;
      case "-":
        return SQARLParserByteCodes.MINUS;
      case "/":
        return SQARLParserByteCodes.DIVIDE;
      case "*":
        return SQARLParserByteCodes.MULT;
      case "MOD":
        return SQARLParserByteCodes.MOD;
      case "REPEAT":
        return SQARLParserByteCodes.REPEAT;
      case "IF":
        return SQARLParserByteCodes.IF;
      case "THEN":
        return SQARLParserByteCodes.THEN;
      case "WHILE":
        return SQARLParserByteCodes.WHILE;
      case "FOR":
        return SQARLParserByteCodes.FOR;
      case "EACH":
        return SQARLParserByteCodes.EACH;
      case "DO":
        return SQARLParserByteCodes.DO;
      case "UNTIL":
        return SQARLParserByteCodes.UNTIL;
      case "PROCEDURE":
        return SQARLParserByteCodes.PROCEDURE;
      case "FUNCTION":
        return SQARLParserByteCodes.FUNCTION;
      case "END":
        return SQARLParserByteCodes.END;
      case "RETURN":
        return SQARLParserByteCodes.RETURN;
      case "RETURNS":
        return SQARLParserByteCodes.RETURNS;
    }

		if(w.matches(INTEGER_REGEX))
			return SQARLParserByteCodes.INT;
		if(w.matches(BOOLEAN_REGEX))
			return SQARLParserByteCodes.BOOLEAN;
		if(w.matches(DOUBLE_REGEX))
			return SQARLParserByteCodes.REAL;
		if(w.matches(STRING_REGEX))
			return SQARLParserByteCodes.STRING;
    if(w.matches(IDENTIFIER_REGEX))
			return SQARLParserByteCodes.IDENTIFIER;
		
		return -2;
	}

	@Override
	public String symbolToString(int i) {
		// This is never needed because the code is transpiled to YASS
		return null;
	}

	@Override
	public String[] listOfSubsequentCharacters() {
        return new String[]{};
	}

	@Override
	public String[] listOfBoundWords() {
        return new String[]{};
	}

	public String[] listOfWhitespaces() {
        return new String[]{" ", "" + '\n', "" + '\r', "\r\n", "" + '\t', System.getProperty("line.separator")};
	}

	@Override
	public ArrayList<MultilineComment> listOfComments() {
		ArrayList<MultilineComment> a = new ArrayList<MultilineComment>();
		a.add(new MultilineComment("#", System.getProperty("line.separator")));
		a.add(new MultilineComment("(<", ")"));
		return a;
	}

	@Override
	public String delimiterCharacters() {
		return " (),[]=<>!&|{}+-/*%@.;?:$^∧";
	}

	@Override
	public String quoteTypes() {
		return "\"'";
	}


}
