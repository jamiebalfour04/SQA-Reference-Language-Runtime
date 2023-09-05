import java.util.ArrayList;

import jamiebalfour.zpe.parser.ZenithParsingEngine.MultilineComment;


public class HaggisParserByteCodes implements jamiebalfour.zpe.parser.Tokeniser {
	
	
	final static String TYPE_INT = "(((-?[1-9]+)|[0-9][0-9]*)(E[0-9]+)?)";
	final static String TYPE_DOUBLE = "(-?[0-9]+((\\.)[0-9]+))(E[0-9]+)?";
	final static String TYPE_STRING = "(\".*\")|('.*')";
	final static String TYPE_NULL = "null";
	final static String TYPE_BOOLEAN = "(true|false)";
	//A string of characters outwith quotes
	final static String NAME_STRING = "([a-z][A-z0-9_]*)";
	
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
	
	final static byte LBRA = 60;
	final static byte RBRA = 61;
	final static byte LSQBR = 62;
	final static byte RSQBR = 63;
	final static byte COMMA = 70;

	final static byte PROCEDURE = 71;
	final static byte FUNCTION = 72;
	final static byte RETURN = 73;
	
	
	
	
	
	

	@Override
	public byte stringToByteCode(String w) {
		if (w.equals("DECLARE"))
			return HaggisParserByteCodes.DECLARE;
		if (w.equals("AS"))
			return HaggisParserByteCodes.AS;
		if (w.equals("INTEGER") || w.equals("BOOLEAN") || w.equals("REAL") || w.equals("STRING") || w.equals("ARRAY"))
			return HaggisParserByteCodes.TYPE;
		if (w.equals("INITIALLY"))
			return HaggisParserByteCodes.INITIALLY;
		if (w.equals("FROM"))
			return HaggisParserByteCodes.FROM;
		if (w.equals("KEYBOARD"))
			return HaggisParserByteCodes.KEYBOARD;
		if (w.equals("RECEIVE"))
			return HaggisParserByteCodes.RECEIVE;
		if (w.equals("SEND"))
			return HaggisParserByteCodes.SEND;
		if (w.equals("TO"))
			return HaggisParserByteCodes.TO;
		if (w.equals("DISPLAY"))
			return HaggisParserByteCodes.DISPLAY;
		if (w.equals("SET"))
			return HaggisParserByteCodes.SET;
		if(w.matches(TYPE_INT)) 
			return HaggisParserByteCodes.INT;
		if(w.matches(TYPE_BOOLEAN))
			return HaggisParserByteCodes.BOOLEAN;
		if(w.matches(TYPE_DOUBLE)) 
			return HaggisParserByteCodes.REAL;
		if(w.matches(TYPE_STRING)) 
			return HaggisParserByteCodes.STRING;
		if(w.matches(NAME_STRING))
			return HaggisParserByteCodes.NAME;
		if(w.equals("AND"))
			return HaggisParserByteCodes.AND;
		if(w.equals("OR"))
			return HaggisParserByteCodes.OR;
		if(w.equals("NOT"))
			return HaggisParserByteCodes.NOT;
		if(w.equals("!="))
			return HaggisParserByteCodes.NEQUAL;
		if(w.equals("="))
			return HaggisParserByteCodes.EQUAL;
		if(w.equals(">"))
			return HaggisParserByteCodes.GT;
		if(w.equals("<"))
			return HaggisParserByteCodes.LT;
		if(w.equals(">="))
			return HaggisParserByteCodes.GTE;
		if(w.equals("<="))
			return HaggisParserByteCodes.LTE;
		if(w.equals("("))
			return HaggisParserByteCodes.LBRA;
		if(w.equals(")"))
			return HaggisParserByteCodes.RBRA;
		if(w.equals("["))
			return HaggisParserByteCodes.LSQBR;
		if(w.equals("]"))
			return HaggisParserByteCodes.RSQBR;
		if(w.equals(","))
			return HaggisParserByteCodes.COMMA;

		if(w.equals("+"))
			return HaggisParserByteCodes.PLUS;
		if(w.equals("-"))
			return HaggisParserByteCodes.MINUS;
		if(w.equals("/"))
			return HaggisParserByteCodes.DIVIDE;
		if(w.equals("*"))
			return HaggisParserByteCodes.MULT;
		if(w.equals("MOD"))
			return HaggisParserByteCodes.MOD;
		if(w.equals("REPEAT"))
			return HaggisParserByteCodes.REPEAT;
		if(w.equals("IF"))
			return HaggisParserByteCodes.IF;
		if(w.equals("THEN"))
			return HaggisParserByteCodes.THEN;
		if(w.equals("WHILE"))
			return HaggisParserByteCodes.WHILE;
		if(w.equals("FOR"))
			return HaggisParserByteCodes.FOR;
		if(w.equals("EACH"))
			return HaggisParserByteCodes.EACH;
		if(w.equals("DO"))
			return HaggisParserByteCodes.DO;
		if(w.equals("UNTIL"))
			return HaggisParserByteCodes.UNTIL;
		if(w.equals("PROCEDURE")){
			return HaggisParserByteCodes.PROCEDURE;
		}
		if(w.equals("FUNCTION")){
			return HaggisParserByteCodes.FUNCTION;
		}
		if(w.equals("RETURN")){
			return HaggisParserByteCodes.RETURN;
		}
		
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
