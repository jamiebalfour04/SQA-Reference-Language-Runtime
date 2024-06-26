package jamiebalfour.zpe;

import java.util.ArrayList;

import jamiebalfour.zpe.parser.ZenithParsingEngine.MultilineComment;


public class SQARLParserByteCodes implements jamiebalfour.zpe.parser.Tokeniser {
	
	
	final static String TYPE_INT = "(((-?[1-9]+)|[0-9][0-9]*)(E[0-9]+)?)";
	final static String TYPE_DOUBLE = "(-?[0-9]+((\\.)[0-9]+))(E[0-9]+)?";
	final static String TYPE_STRING = "(\".*\")|('.*')";
	final static String TYPE_NULL = "null";
	final static String TYPE_BOOLEAN = "(true|false)";
	//A string of characters outwith quotes
	final static String NAME_STRING = "([A-z][A-z0-9_]*)";
	
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
	final static byte NAME = 11;
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
	final static byte METHODS = 76;
	final static byte CONSTRUCTOR = 77;
	final static byte RETURNS = 78;
	
	
	
	
	
	

	@Override
	public byte stringToByteCode(String w) {
		if (w.equals("DECLARE"))
			return SQARLParserByteCodes.DECLARE;
		if (w.equals("AS"))
			return SQARLParserByteCodes.AS;
		if (w.equals("INTEGER") || w.equals("BOOLEAN") || w.equals("REAL") || w.equals("STRING") || w.equals("ARRAY"))
			return SQARLParserByteCodes.TYPE;
		if (w.equals("INITIALLY"))
			return SQARLParserByteCodes.INITIALLY;
		if (w.equals("FROM"))
			return SQARLParserByteCodes.FROM;
		if (w.equals("KEYBOARD"))
			return SQARLParserByteCodes.KEYBOARD;
		if (w.equals("RECEIVE"))
			return SQARLParserByteCodes.RECEIVE;
		if (w.equals("CLASS"))
			return SQARLParserByteCodes.CLASS;
		if (w.equals("IS"))
			return SQARLParserByteCodes.IS;
		if (w.equals("METHODS"))
			return SQARLParserByteCodes.METHODS;
		if (w.equals("CONSTRUCTOR"))
			return SQARLParserByteCodes.CONSTRUCTOR;
		if (w.equals("SEND"))
			return SQARLParserByteCodes.SEND;
		if (w.equals("TO"))
			return SQARLParserByteCodes.TO;
		if (w.equals("DISPLAY"))
			return SQARLParserByteCodes.DISPLAY;
		if (w.equals("SET"))
			return SQARLParserByteCodes.SET;
		if(w.matches(TYPE_INT)) 
			return SQARLParserByteCodes.INT;
		if(w.matches(TYPE_BOOLEAN))
			return SQARLParserByteCodes.BOOLEAN;
		if(w.matches(TYPE_DOUBLE)) 
			return SQARLParserByteCodes.REAL;
		if(w.matches(TYPE_STRING)) 
			return SQARLParserByteCodes.STRING;

		if(w.equals("AND"))
			return SQARLParserByteCodes.AND;
		if(w.equals("OR"))
			return SQARLParserByteCodes.OR;
		if(w.equals("NOT"))
			return SQARLParserByteCodes.NOT;
		if(w.equals("&"))
			return SQARLParserByteCodes.CONCAT;
		if(w.equals("!="))
			return SQARLParserByteCodes.NEQUAL;
		if(w.equals("="))
			return SQARLParserByteCodes.EQUAL;
		if(w.equals(">"))
			return SQARLParserByteCodes.GT;
		if(w.equals("<"))
			return SQARLParserByteCodes.LT;
		if(w.equals(">="))
			return SQARLParserByteCodes.GTE;
		if(w.equals("<="))
			return SQARLParserByteCodes.LTE;
		if(w.equals("("))
			return SQARLParserByteCodes.LBRA;
		if(w.equals(")"))
			return SQARLParserByteCodes.RBRA;
		if(w.equals("{"))
			return SQARLParserByteCodes.LBRACE;
		if(w.equals("}"))
			return SQARLParserByteCodes.RBRACE;
		if(w.equals("["))
			return SQARLParserByteCodes.LSQBR;
		if(w.equals("]"))
			return SQARLParserByteCodes.RSQBR;
		if(w.equals(","))
			return SQARLParserByteCodes.COMMA;

		if(w.equals("+"))
			return SQARLParserByteCodes.PLUS;
		if(w.equals("-"))
			return SQARLParserByteCodes.MINUS;
		if(w.equals("/"))
			return SQARLParserByteCodes.DIVIDE;
		if(w.equals("*"))
			return SQARLParserByteCodes.MULT;
		if(w.equals("MOD"))
			return SQARLParserByteCodes.MOD;
		if(w.equals("REPEAT"))
			return SQARLParserByteCodes.REPEAT;
		if(w.equals("IF"))
			return SQARLParserByteCodes.IF;
		if(w.equals("THEN"))
			return SQARLParserByteCodes.THEN;
		if(w.equals("WHILE"))
			return SQARLParserByteCodes.WHILE;
		if(w.equals("FOR"))
			return SQARLParserByteCodes.FOR;
		if(w.equals("EACH"))
			return SQARLParserByteCodes.EACH;
		if(w.equals("DO"))
			return SQARLParserByteCodes.DO;
		if(w.equals("UNTIL"))
			return SQARLParserByteCodes.UNTIL;
		if(w.equals("PROCEDURE"))
			return SQARLParserByteCodes.PROCEDURE;
		if(w.equals("FUNCTION"))
			return SQARLParserByteCodes.FUNCTION;
		if(w.equals("END"))
			return SQARLParserByteCodes.END;
		if(w.equals("RETURN"))
			return SQARLParserByteCodes.RETURN;
		if(w.equals("RETURNS"))
			return SQARLParserByteCodes.RETURNS;
		if(w.matches(NAME_STRING))
			return SQARLParserByteCodes.NAME;
		
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
