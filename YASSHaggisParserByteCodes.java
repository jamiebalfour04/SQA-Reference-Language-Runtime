import java.util.ArrayList;

import jamiebalfour.zpe.parser.ZenithParsingEngine.MultilineComment;


public class YASSHaggisParserByteCodes implements jamiebalfour.zpe.parser.Tokeniser {
	
	
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
	
	
	
	
	
	

	@Override
	public byte stringToByteCode(String w) {
		if (w.equals("DECLARE"))
			return YASSHaggisParserByteCodes.DECLARE;
		if (w.equals("AS"))
			return YASSHaggisParserByteCodes.AS;
		if (w.equals("INTEGER") || w.equals("BOOLEAN") || w.equals("REAL") || w.equals("STRING") || w.equals("ARRAY"))
			return YASSHaggisParserByteCodes.TYPE;
		if (w.equals("INITIALLY"))
			return YASSHaggisParserByteCodes.INITIALLY;
		if (w.equals("FROM"))
			return YASSHaggisParserByteCodes.FROM;
		if (w.equals("KEYBOARD"))
			return YASSHaggisParserByteCodes.KEYBOARD;
		if (w.equals("RECEIVE"))
			return YASSHaggisParserByteCodes.RECEIVE;
		if (w.equals("SEND"))
			return YASSHaggisParserByteCodes.SEND;
		if (w.equals("TO"))
			return YASSHaggisParserByteCodes.TO;
		if (w.equals("DISPLAY"))
			return YASSHaggisParserByteCodes.DISPLAY;
		if (w.equals("SET"))
			return YASSHaggisParserByteCodes.SET;
		if(w.matches(TYPE_INT)) 
			return YASSHaggisParserByteCodes.INT;
		if(w.matches(TYPE_DOUBLE)) 
			return YASSHaggisParserByteCodes.REAL;
		if(w.matches(TYPE_STRING)) 
			return YASSHaggisParserByteCodes.STRING;	
		if(w.matches(NAME_STRING))
			return YASSHaggisParserByteCodes.NAME;
		if(w.equals("AND"))
			return YASSHaggisParserByteCodes.AND;
		if(w.equals("OR"))
			return YASSHaggisParserByteCodes.OR;
		if(w.equals("NOT"))
			return YASSHaggisParserByteCodes.NOT;		
		if(w.equals("!="))
			return YASSHaggisParserByteCodes.NEQUAL;
		if(w.equals("="))
			return YASSHaggisParserByteCodes.EQUAL;
		if(w.equals(">"))
			return YASSHaggisParserByteCodes.GT;
		if(w.equals("<"))
			return YASSHaggisParserByteCodes.LT;
		if(w.equals(">="))
			return YASSHaggisParserByteCodes.GTE;
		if(w.equals("<="))
			return YASSHaggisParserByteCodes.LTE;
		if(w.equals("("))
			return YASSHaggisParserByteCodes.LBRA;
		if(w.equals(")"))
			return YASSHaggisParserByteCodes.RBRA;
		if(w.equals("["))
			return YASSHaggisParserByteCodes.LSQBR;
		if(w.equals("]"))
			return YASSHaggisParserByteCodes.RSQBR;
		if(w.equals(","))
			return YASSHaggisParserByteCodes.COMMA;
		if(w.toLowerCase().equals("true"))
			return YASSHaggisParserByteCodes.BOOLEAN;
		if(w.toLowerCase().equals("false"))
			return YASSHaggisParserByteCodes.BOOLEAN;
		if(w.equals("+"))
			return YASSHaggisParserByteCodes.PLUS;	
		if(w.equals("-"))
			return YASSHaggisParserByteCodes.MINUS;	
		if(w.equals("/"))
			return YASSHaggisParserByteCodes.DIVIDE;	
		if(w.equals("*"))
			return YASSHaggisParserByteCodes.MULT;	
		if(w.equals("MOD"))
			return YASSHaggisParserByteCodes.MOD;	
		if(w.equals("REPEAT"))
			return YASSHaggisParserByteCodes.REPEAT;
		if(w.equals("IF"))
			return YASSHaggisParserByteCodes.IF;	
		if(w.equals("THEN"))
			return YASSHaggisParserByteCodes.THEN;
		if(w.equals("WHILE"))
			return YASSHaggisParserByteCodes.WHILE;
		if(w.equals("FOR"))
			return YASSHaggisParserByteCodes.FOR;
		if(w.equals("EACH"))
			return YASSHaggisParserByteCodes.EACH;
		if(w.equals("DO"))
			return YASSHaggisParserByteCodes.DO;
		if(w.equals("UNTIL"))
			return YASSHaggisParserByteCodes.UNTIL;	
		
		return -2;
	}

	@Override
	public String symbolToString(int i) {
		// TODO Auto-generated method stub
		return null;
	}

	/*@Override
	public String[] listOfSpecialCharacters() {
		String[] characters = {" ", "+", "-", "*", "/", "(", ")"};
		return characters;
	}*/

	@Override
	public String[] listOfSubsequentCharacters() {
		String[] characters = {};
		return characters;
	}

	@Override
	public String[] listOfBoundWords() {
		String[] characters = {};
		return characters;
	}

	public String[] listOfWhitespaces() {
		String[] whitespaces = {" ", "" + '\n', "" + '\r', "\r\n", "" + '\t', System.getProperty("line.separator")};
		return whitespaces;
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
