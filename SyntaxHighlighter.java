import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

import javax.swing.*;
import javax.swing.text.*;



//Downloaded from https://jahnke.im/java/syntax-highlighter/
/**
 * Highlights syntax in a DefaultStyledDocument. Allows any number of keywords.
 * 
 * @author camickr (primary author; java sun forums user)
 * @author David Underhill
 */
class SyntaxHighlighter extends DefaultStyledDocument {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String DEFAULT_FONT_FAMILY = "Courier New";
	public static final int DEFAULT_FONT_SIZE = 18;
	public static final SimpleAttributeSet DEFAULT_NULL;
	public static final SimpleAttributeSet DEFAULT_NORMAL;
	public static final SimpleAttributeSet DEFAULT_COMMENT;
	public static final SimpleAttributeSet DEFAULT_STRING;
	public static final SimpleAttributeSet DEFAULT_KEYWORD;
	public static final SimpleAttributeSet DEFAULT_PREDEFINED_FUNCTION;
	public static final SimpleAttributeSet DEFAULT_HEREDOC;
	public static final SimpleAttributeSet DEFAULT_BOOLEAN;
	public static final SimpleAttributeSet DEFAULT_VAR;
	public static final SimpleAttributeSet DEFAULT_DOC;
	public static final SimpleAttributeSet DEFAULT_TYPE;
	
	private static Graphics2D g;

	static {
		DEFAULT_NULL = new SimpleAttributeSet();
		StyleConstants.setFontSize(DEFAULT_NULL, DEFAULT_FONT_SIZE);		
		
		DEFAULT_NORMAL = new SimpleAttributeSet();
		StyleConstants.setForeground(DEFAULT_NORMAL, Color.BLACK);
		StyleConstants.setFontFamily(DEFAULT_NORMAL, DEFAULT_FONT_FAMILY);
		StyleConstants.setFontSize(DEFAULT_NORMAL, DEFAULT_FONT_SIZE);

		DEFAULT_COMMENT = new SimpleAttributeSet();
		StyleConstants.setForeground(DEFAULT_COMMENT, new Color(170, 170, 170)); // gray
		StyleConstants.setItalic(DEFAULT_COMMENT, true);
		StyleConstants.setFontFamily(DEFAULT_COMMENT, DEFAULT_FONT_FAMILY);
		StyleConstants.setFontSize(DEFAULT_COMMENT, DEFAULT_FONT_SIZE);

		DEFAULT_STRING = new SimpleAttributeSet();
		StyleConstants.setForeground(DEFAULT_STRING, new java.awt.Color(0, 150, 0)); // green
		StyleConstants.setFontFamily(DEFAULT_STRING, DEFAULT_FONT_FAMILY);
		StyleConstants.setFontSize(DEFAULT_STRING, DEFAULT_FONT_SIZE);

		// default style for new keyword types
		DEFAULT_KEYWORD = new SimpleAttributeSet();
		StyleConstants.setForeground(DEFAULT_KEYWORD, new java.awt.Color(200, 0, 255)); // purple
		StyleConstants.setBold(DEFAULT_KEYWORD, false);
		StyleConstants.setFontFamily(DEFAULT_KEYWORD, DEFAULT_FONT_FAMILY);
		StyleConstants.setFontSize(DEFAULT_KEYWORD, DEFAULT_FONT_SIZE);
		
		DEFAULT_PREDEFINED_FUNCTION = new SimpleAttributeSet();
		StyleConstants.setForeground(DEFAULT_PREDEFINED_FUNCTION, new java.awt.Color(24, 155, 220)); // dark blue
		StyleConstants.setFontFamily(DEFAULT_PREDEFINED_FUNCTION, DEFAULT_FONT_FAMILY);
		StyleConstants.setFontSize(DEFAULT_PREDEFINED_FUNCTION, DEFAULT_FONT_SIZE);
		
		DEFAULT_HEREDOC = new SimpleAttributeSet();
		StyleConstants.setForeground(DEFAULT_HEREDOC, new java.awt.Color(24, 155, 220)); // yellow
		StyleConstants.setFontFamily(DEFAULT_HEREDOC, DEFAULT_FONT_FAMILY);
		StyleConstants.setFontSize(DEFAULT_HEREDOC, DEFAULT_FONT_SIZE);
		
		DEFAULT_BOOLEAN = new SimpleAttributeSet();
		StyleConstants.setForeground(DEFAULT_BOOLEAN, new java.awt.Color(255, 0, 0)); // red
		StyleConstants.setFontFamily(DEFAULT_BOOLEAN, DEFAULT_FONT_FAMILY);
		StyleConstants.setFontSize(DEFAULT_BOOLEAN, DEFAULT_FONT_SIZE);
		
		DEFAULT_VAR = new SimpleAttributeSet();
		StyleConstants.setForeground(DEFAULT_VAR, new java.awt.Color(200, 100, 0)); // ORANGE
		StyleConstants.setFontFamily(DEFAULT_VAR, DEFAULT_FONT_FAMILY);
		StyleConstants.setFontSize(DEFAULT_VAR, DEFAULT_FONT_SIZE);
		
		DEFAULT_DOC = new SimpleAttributeSet();
		StyleConstants.setForeground(DEFAULT_DOC, new java.awt.Color(46, 159, 175)); // TEAL
		StyleConstants.setBold(DEFAULT_DOC, true);
		StyleConstants.setFontFamily(DEFAULT_DOC, DEFAULT_FONT_FAMILY);
		StyleConstants.setFontSize(DEFAULT_DOC, DEFAULT_FONT_SIZE);
		
		DEFAULT_TYPE = new SimpleAttributeSet();
		StyleConstants.setForeground(DEFAULT_TYPE, new java.awt.Color(150, 0, 150)); // TEAL
		StyleConstants.setBold(DEFAULT_TYPE, false);
		StyleConstants.setFontFamily(DEFAULT_TYPE, DEFAULT_FONT_FAMILY);
		StyleConstants.setFontSize(DEFAULT_TYPE, DEFAULT_FONT_SIZE);
	}
	private DefaultStyledDocument doc;
	private Element rootElement;
	private boolean multiLineComment;
	private MutableAttributeSet dnull = DEFAULT_NULL;
	private MutableAttributeSet normal = DEFAULT_NORMAL;
	private MutableAttributeSet comment = DEFAULT_COMMENT;
	private MutableAttributeSet quote = DEFAULT_STRING;
	private MutableAttributeSet keyword = DEFAULT_KEYWORD;
	private MutableAttributeSet function = DEFAULT_PREDEFINED_FUNCTION;
	private MutableAttributeSet heredoc = DEFAULT_HEREDOC;
	private MutableAttributeSet bool = DEFAULT_BOOLEAN;
	private MutableAttributeSet var = DEFAULT_VAR;
	private MutableAttributeSet docword = DEFAULT_DOC;
	private MutableAttributeSet type = DEFAULT_TYPE;
	private HashMap<String, MutableAttributeSet> keywords;
	private int fontSize = DEFAULT_FONT_SIZE;
	private String fontName = DEFAULT_FONT_FAMILY;
	JEditorPane edit;

	public SyntaxHighlighter(final HashMap<String, MutableAttributeSet> keywords) {
		doc = this;
		rootElement = doc.getDefaultRootElement();
		putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");
		this.keywords = keywords;
	}
	
	private void embolden(MutableAttributeSet x, boolean v) {
		Font f = this.getFont(x);
		if(v) {
			Font nFont = new Font(f.getName(), Font.BOLD, f.getSize());
			setAttributeFont(x, nFont);
		} else {
			Font nFont = new Font(f.getName(), Font.PLAIN, f.getSize());
			setAttributeFont(x, nFont);
		}
		
	}
	
	public void makeBold(boolean v) {
		embolden(dnull, v);
		embolden(normal, v);
		embolden(comment, v);
		embolden(quote, v);
		embolden(function, v);
		embolden(heredoc, v);
		embolden(bool, v);
		embolden(var, v);
	}

	public enum ATTR_TYPE {

		Normal, Comment, Quote, Keyword, Function, Heredoc, Bool, Var, Doc, Type;
	}

	/**
	 * Sets the font of the specified attribute
	 * 
	 * @param attr  the attribute to apply this font to (normal, comment, string)
	 * @param style font style (Font.BOLD, Font.ITALIC, Font.PLAIN)
	 */
	public void setAttributeFont(ATTR_TYPE attr, int style) {
		Font f = new Font(fontName, style, fontSize);
		if (attr == ATTR_TYPE.Comment) {
			setAttributeFont(comment, f);
		} else if (attr == ATTR_TYPE.Quote) {
			setAttributeFont(quote, f);
		} else if (attr == ATTR_TYPE.Keyword) {
			setAttributeFont(keyword, f);
		} else if (attr == ATTR_TYPE.Function) {
			setAttributeFont(function, f);
		} else if (attr == ATTR_TYPE.Heredoc) {
			setAttributeFont(heredoc, f);
		} else if (attr == ATTR_TYPE.Bool) {
			setAttributeFont(bool, f);
		} else if (attr == ATTR_TYPE.Var) {
			setAttributeFont(var, f);
		} else if (attr == ATTR_TYPE.Doc) {
			setAttributeFont(docword, f);
		} else if (attr == ATTR_TYPE.Type) {
			setAttributeFont(type, f);
		} else {
			setAttributeFont(normal, f);
		}
	}
	
	MutableAttributeSet getCommentSet() {
		return this.comment;
	}

	/**
	 * Sets the font of the specified attribute
	 * 
	 * @param attr attribute to apply this font to
	 * @param f    the font to use
	 */
	public static void setAttributeFont(MutableAttributeSet attr, Font f) {
		StyleConstants.setBold(attr, f.isBold());
		StyleConstants.setItalic(attr, f.isItalic());
		StyleConstants.setFontFamily(attr, f.getFamily());
		StyleConstants.setFontSize(attr, f.getSize());
	}

	/**
	 * Sets the foreground (font) color of the specified attribute
	 * 
	 * @param attr the attribute to apply this font to (normal, comment, string)
	 * @param c    the color to use
	 */
	public void setAttributeColor(ATTR_TYPE attr, Color c) {
		if (attr == ATTR_TYPE.Comment) {
			setAttributeColor(comment, c);
		} else if (attr == ATTR_TYPE.Quote) {
			setAttributeColor(quote, c);
		} else if (attr == ATTR_TYPE.Keyword) {
			setAttributeColor(keyword, c);
		} else if (attr == ATTR_TYPE.Function) {
			setAttributeColor(function, c);
		} else if (attr == ATTR_TYPE.Heredoc) {
			setAttributeColor(heredoc, c);
		} else if (attr == ATTR_TYPE.Bool) {
			setAttributeColor(bool, c);
		} else if (attr == ATTR_TYPE.Var) {
			setAttributeColor(var, c);
		} else if (attr == ATTR_TYPE.Doc) {
			setAttributeColor(docword, c);
		} else if (attr == ATTR_TYPE.Type) {
			setAttributeColor(type, c);
		} else {
			setAttributeColor(normal, c);
		}
	}

	/**
	 * Sets the foreground (font) color of the specified attribute
	 * 
	 * @param attr attribute to apply this color to
	 * @param c    the color to use
	 */
	public static void setAttributeColor(MutableAttributeSet attr, Color c) {
		StyleConstants.setForeground(attr, c);
	}

	/**
	 * Associates a keyword with a particular formatting style
	 * 
	 * @param keyword the token or word to format
	 * @param attr    how to format keyword
	 */
	public void addKeyword(String keyword, MutableAttributeSet attr) {
		keywords.put(keyword, attr);
	}

	/**
	 * Gets the formatting for a keyword
	 *
	 * @param keyword the token or word to stop formatting
	 * @return how keyword is formatted, or null if no formatting is applied to it
	 */
	public MutableAttributeSet getKeywordFormatting(String keyword) {
		return keywords.get(keyword);
	}

	/**
	 * Removes an association between a keyword with a particular formatting style
	 * 
	 * @param keyword the token or word to stop formatting
	 */
	public void removeKeyword(String keyword) {
		keywords.remove(keyword);
	}

	/** sets the number of chardsacters per tab */
	public void setTabs(int charactersPerTab) {
		Font f = new Font(fontName, Font.PLAIN, fontSize);
		//@SuppressWarnings("deprecation")		
		
		FontMetrics fm = g.getFontMetrics(f);//java.awt.Toolkit.getDefaultToolkit().getFontMetrics(f);
		int charWidth = fm.charWidth('w');
		int tabWidth = charWidth * charactersPerTab;
		TabStop[] tabs = new TabStop[35];
		for (int j = 0; j < tabs.length; j++) {
			int tab = j + 1;
			tabs[j] = new TabStop(tab * tabWidth);
		}
		TabSet tabSet = new TabSet(tabs);
		SimpleAttributeSet attributes = new SimpleAttributeSet();
		StyleConstants.setTabSet(attributes, tabSet);
		int length = this.getLength();
		this.setParagraphAttributes(0, length, attributes, false);
	}

	@Override
	public void insertString(int offset, String str, AttributeSet a) throws BadLocationException {
		if (str.equals("{")) {
			str = addMatchingBrace(offset, "{", "}", true);
		} else if (str.equals("(")) {
			str = addMatchingBrace(offset, "(", ")", false);
		} else if (str.equals("[")) {
			str = addMatchingBrace(offset, "[", "]", false);
		}
		super.insertString(offset, str, a);
		processChangedLines(offset, str.length());
	}

	@Override
	public void remove(int offset, int length) throws BadLocationException {
		super.remove(offset, length);
		processChangedLines(offset, 0);
	}

	/*
	 * Determine how many lines have been changed, then apply highlighting to each
	 * line
	 */
	public void processChangedLines(int offset, int length) throws BadLocationException {
		String content = doc.getText(0, doc.getLength());
		// The lines affected by the latest document update
		int startLine = rootElement.getElementIndex(offset);
		int endLine = rootElement.getElementIndex(offset + length);
		// Make sure all comment lines prior to the start line are commented
		// and determine if the start line is still in a multi line comment
		setMultiLineComment(commentLinesBefore(content, startLine));
		// Do the actual highlighting
		for (int i = startLine; i <= endLine; i++) {
			applyHighlighting(content, i);
		}
		// Resolve highlighting to the next end multi line delimiter
		if (isMultiLineComment()) {
			commentLinesAfter(content, endLine);
		} else {
			highlightLinesAfter(content, endLine);
		}
	}
	
	private boolean commentLinesBefore(String content, int line) {
		int offset = rootElement.getElement(line).getStartOffset();
		// Start of comment not found, nothing to do
		int startDelimiter = lastIndexOf(content, getStartDelimiter(), offset - 2);
		if (startDelimiter < 0) {
			return false;
		}
		// Matching start/end of comment found, nothing to do
		int endDelimiter = indexOf(content, getEndDelimiter(), startDelimiter);
		if (endDelimiter < offset & endDelimiter != -1) {
			return false;
		}
		// End of comment not found, highlight the lines
		doc.setCharacterAttributes(startDelimiter, offset - startDelimiter + 1, comment, false);
		return true;
	}

	private void commentLinesAfter(String content, int line) {
		int offset = rootElement.getElement(line).getEndOffset();
		// End of comment not found, nothing to do
		int endDelimiter = indexOf(content, getEndDelimiter(), offset);
		if (endDelimiter < 0) {
			return;
		}
		// Matching start/end of comment found, comment the lines
		int startDelimiter = lastIndexOf(content, getStartDelimiter(), endDelimiter);
		if (startDelimiter < 0 || startDelimiter <= offset) {
			doc.setCharacterAttributes(offset, endDelimiter - offset + 1, comment, false);
		}
	}

	private void highlightLinesAfter(String content, int line) throws BadLocationException {
		int offset = rootElement.getElement(line).getEndOffset();
		// Start/End delimiter not found, nothing to do
		int startDelimiter = indexOf(content, getStartDelimiter(), offset);
		int endDelimiter = indexOf(content, getEndDelimiter(), offset);
		if (startDelimiter < 0) {
			startDelimiter = content.length();
		}
		if (endDelimiter < 0) {
			endDelimiter = content.length();
		}
		int delimiter = Math.min(startDelimiter, endDelimiter);
		if (delimiter < offset) {
			return;
		}
		// Start/End delimiter found, reapply highlighting
		int endLine = rootElement.getElementIndex(delimiter);
		for (int i = line + 1; i < endLine; i++) {
			Element branch = rootElement.getElement(i);
			Element leaf = doc.getCharacterElement(branch.getStartOffset());
			AttributeSet as = leaf.getAttributes();
			if (as.isEqual(comment)) {
				applyHighlighting(content, i);
			}
		}
	}

	private void applyHighlighting(String content, int line) throws BadLocationException {
		int startOffset = rootElement.getElement(line).getStartOffset();
		int endOffset = rootElement.getElement(line).getEndOffset() - 1;
		int lineLength = endOffset - startOffset;
		int contentLength = content.length();
		if (endOffset >= contentLength) {
			endOffset = contentLength - 1;
		}
		if (endingMultiLineComment(content, startOffset, endOffset) || isMultiLineComment()
				|| startingMultiLineComment(content, startOffset, endOffset)) {
			doc.setCharacterAttributes(startOffset, endOffset - startOffset + 1, comment, false);
			return;
		}
		doc.setCharacterAttributes(startOffset, lineLength, normal, true);
		int index = content.indexOf(getSingleLineDelimiter(), startOffset);
		if ((index > -1) && (index < endOffset)) {
			doc.setCharacterAttributes(index, endOffset - index + 1, comment, false);
			endOffset = index - 1;
		}
		checkForTokens(content, startOffset, endOffset);
	}

	private boolean startingMultiLineComment(String content, int startOffset, int endOffset)
			throws BadLocationException {
		int index = indexOf(content, getStartDelimiter(), startOffset);
		if ((index < 0) || (index > endOffset)) {
			return false;
		} else {
			setMultiLineComment(true);
			return true;
		}
	}

	private boolean endingMultiLineComment(String content, int startOffset, int endOffset) throws BadLocationException {
		int index = indexOf(content, getEndDelimiter(), startOffset);
		if ((index < 0) || (index > endOffset)) {
			return false;
		} else {
			setMultiLineComment(false);
			return true;
		}
	}

	private boolean isMultiLineComment() {
		return multiLineComment;
	}

	private void setMultiLineComment(boolean value) {
		multiLineComment = value;
	}

	private void checkForTokens(String content, int startOffset, int endOffset) {
		while (startOffset <= endOffset) {
			// skip the delimiters to find the start of a new token
			while (isDelimiter(content.substring(startOffset, startOffset + 1))) {
				doc.setCharacterAttributes(startOffset, 1, keyword, false);
				if (startOffset < endOffset) {
					startOffset++;
				} else {
					return;
				}
			}
			// Extract and process the entire token
			if (isQuoteDelimiter(content.substring(startOffset, startOffset + 1))) {
				startOffset = getQuoteToken(content, startOffset, endOffset);
			} else if (isVarDelimiter(content.substring(startOffset, startOffset + 1))) {
				startOffset = getVarToken(content, startOffset, endOffset);
			} else {
				startOffset = getOtherToken(content, startOffset, endOffset);
			}
			
			//JB: Fixes the issue with lines with nothing on them
			doc.setCharacterAttributes(startOffset, endOffset, dnull, false);
		}
	}

	private int getQuoteToken(String content, int startOffset, int endOffset) {
		String quoteDelimiter = content.substring(startOffset, startOffset + 1);
		String escapeString = getEscapeString(quoteDelimiter);
		int index;
		int endOfQuote = startOffset;
		index = content.indexOf(escapeString, endOfQuote + 1);
		while ((index > -1) && (index < endOffset)) {
			endOfQuote = index + 1;
			index = content.indexOf(escapeString, endOfQuote);
		}
		index = content.indexOf(quoteDelimiter, endOfQuote + 1);
		if ((index < 0) || (index > endOffset)) {
			endOfQuote = endOffset;
		} else {
			endOfQuote = index;
		}
		doc.setCharacterAttributes(startOffset, endOfQuote - startOffset + 1, quote, false);
		return endOfQuote + 1;
	}
	
	private int getVarToken(String content, int startOffset, int endOffset) {
		String quoteDelimiter = content.substring(startOffset, startOffset + 1);
		int index = startOffset;
		while (index < content.length()) {
			String c = content.substring(index, index + 1);
			if(isDelimiter(c) && !c.equals(quoteDelimiter) || c.equals(" ")){
				break;
				
				
			}
			index++;
		}
		
		doc.setCharacterAttributes(startOffset, index - startOffset, var, false);
		
		return index;
	}

	private int getOtherToken(String content, int startOffset, int endOffset) {
		int endOfToken = startOffset + 1;
		while (endOfToken <= endOffset) {
			if (isDelimiter(content.substring(endOfToken, endOfToken + 1))) {
				break;
			}
			endOfToken++;
		}
		String token = content.substring(startOffset, endOfToken);
		MutableAttributeSet attr = keywords.get(token);
		if (attr != null) {
			doc.setCharacterAttributes(startOffset, endOfToken - startOffset, attr, false);
		}
		return endOfToken + 1;
	}

	@SuppressWarnings({})
	private int indexOf(String content, String needle, int offset) {
		int index;
		while ((index = content.indexOf(needle, offset)) != -1) {
			String text = getLine(content, index).trim();
			if (text.startsWith(needle) || text.endsWith(needle)) {
				break;
			} else {
				offset = index + 1;
			}
		}

		return index;
	}

	@SuppressWarnings({})
	private int lastIndexOf(String content, String needle, int offset) {
		int index;
		while ((index = content.lastIndexOf(needle, offset)) != -1) {
			String text = getLine(content, index).trim();
			if (text.startsWith(needle) || text.endsWith(needle)) {
				break;
			} else {
				offset = index - 1;
			}
		}

		return index;
	}

	private String getLine(String content, int offset) {
		int line = rootElement.getElementIndex(offset);
		Element lineElement = rootElement.getElement(line);
		int start = lineElement.getStartOffset();
		int end = lineElement.getEndOffset();
		return content.substring(start, end - 1);
	}

	/*
	 * Override for other languages
	 */
	protected boolean isDelimiter(String character) {
		String operands = "();&{}[],:+-*/!";
		if (Character.isWhitespace(character.charAt(0)) || operands.indexOf(character) != -1) {
			return true;
		} else {
			return false;
		}
	}

	/*
	 * Override for other languages
	 */
	protected boolean isQuoteDelimiter(String character) {
		String quoteDelimiters = "\"'";
		if (quoteDelimiters.indexOf(character) < 0) {
			return false;
		} else {
			return true;
		}
	}
	
	protected boolean isVarDelimiter(String character) {
		String varDelimiters = "$";
		if (varDelimiters.indexOf(character) < 0) {
			return false;
		} else {
			return true;
		}
	}

	/*
	 * Override for other languages
	 */
	protected String getStartDelimiter() {
		return "/*";
	}

	/*
	 * Override for other languages
	 */
	protected String getEndDelimiter() {
		return "*/";
	}

	/*
	 * Override for other languages
	 */
	protected String getSingleLineDelimiter() {
		return "//";
	}

	/*
	 * Override for other languages
	 */
	protected String getEscapeString(String quoteDelimiter) {
		return "\\" + quoteDelimiter;
	}

	protected String addMatchingBrace(int offset, String startBrace, String endBrace, boolean addLines) throws BadLocationException {
		StringBuilder whiteSpace = new StringBuilder(16);
		int line = rootElement.getElementIndex(offset);
		int i = rootElement.getElement(line).getStartOffset();
		while (true) {
			String temp = doc.getText(i, 1);
			if (temp.equals(" ") || temp.equals("\t")) {
				whiteSpace.append(temp);
				i++;
			} else {
				break;
			}
		}
		
		String lineSpace = "";
		if(addLines) {
			lineSpace = "\n" + whiteSpace.toString() + "\t\n" + whiteSpace.toString();
		}
		
		return startBrace + lineSpace + endBrace;
	}

	/** gets the current font size */
	public int getFontSize() {
		return fontSize;
	}

	/** sets the current font size (affects all built-in styles) */
	public void setFontSize(int fontSize) {
		this.fontSize = fontSize;
		StyleConstants.setFontSize(dnull, fontSize);
		StyleConstants.setFontSize(normal, fontSize);
		StyleConstants.setFontSize(quote, fontSize);
		StyleConstants.setFontSize(comment, fontSize);
		StyleConstants.setFontSize(keyword, fontSize);
		StyleConstants.setFontSize(function, fontSize);
		StyleConstants.setFontSize(heredoc, fontSize);
		StyleConstants.setFontSize(bool, fontSize);
		StyleConstants.setFontSize(var, fontSize);
		StyleConstants.setFontSize(docword, fontSize);
		StyleConstants.setFontSize(type, fontSize);
	}

	/** gets the current font family */
	public String getFontName() {
		return fontName;
	}

	/** sets the current font family (affects all built-in styles) */
	public void setFontName(String fontName) {
		this.fontName = fontName;
		StyleConstants.setFontFamily(normal, fontName);
		StyleConstants.setFontFamily(quote, fontName);
		StyleConstants.setFontFamily(comment, fontName);
		StyleConstants.setFontFamily(keyword, fontName);
		StyleConstants.setFontFamily(function, fontName);
		StyleConstants.setFontFamily(heredoc, fontName);
		StyleConstants.setFontFamily(bool, fontName);
		StyleConstants.setFontFamily(var, fontName);
		StyleConstants.setFontFamily(docword, fontName);
		StyleConstants.setFontFamily(type, fontName);
	}
	
	public SyntaxHighlighter() {

		final HashMap<String, MutableAttributeSet> YASSKeywords = new HashMap<String, MutableAttributeSet>(16);
		YASSKeywords.put("DECLARE", keyword);
		YASSKeywords.put("INITIALLY", keyword);
		YASSKeywords.put("WHILE", keyword);
		YASSKeywords.put("RECEIVE", keyword);
		YASSKeywords.put("FROM", keyword);
		YASSKeywords.put("KEYBOARD", keyword);
		YASSKeywords.put("END", keyword);
		YASSKeywords.put("SEND", keyword);
		YASSKeywords.put("FOR", keyword);
		YASSKeywords.put("EACH", keyword);
		YASSKeywords.put("DO", keyword);
		YASSKeywords.put("IF", keyword);
		YASSKeywords.put("THEN", keyword);
		YASSKeywords.put("SET", keyword);
		YASSKeywords.put("TO", keyword);
		YASSKeywords.put("DISPLAY", keyword);
		YASSKeywords.put("ARRAY", keyword);
		YASSKeywords.put("STRING", keyword);
		YASSKeywords.put("RECORD", keyword);
		YASSKeywords.put("CLASS", keyword);
		YASSKeywords.put("INTEGER", keyword);
		YASSKeywords.put("REAL", keyword);
		YASSKeywords.put("BOOLEAN", keyword);
		YASSKeywords.put("CHARACTER", keyword);
		YASSKeywords.put("FUNCTION", keyword);
		YASSKeywords.put("RETURN", keyword);
		YASSKeywords.put("PROCEDURE", keyword);
		YASSKeywords.put("AND", keyword);
		YASSKeywords.put("OR", keyword);
		YASSKeywords.put("NOT", keyword);
		YASSKeywords.put("MOD", keyword);
		YASSKeywords.put("OPEN", keyword);
		YASSKeywords.put("CLOSE", keyword);
		YASSKeywords.put("CREATE", keyword);
		YASSKeywords.put("METHODS", keyword);
		YASSKeywords.put("THIS", keyword);
		YASSKeywords.put("WITH", keyword);
		YASSKeywords.put("OVERRIDE", keyword);
		YASSKeywords.put("INHERITS", keyword);
		YASSKeywords.put("CONSTRUCTOR", keyword);
		
		@SuppressWarnings("serial")
		EditorKit editorKit = new StyledEditorKit() {

			@Override
			public Document createDefaultDocument() {
				return new SyntaxHighlighter(YASSKeywords);
			}
		};
		
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		SyntaxHighlighter.g = ge.createGraphics(new BufferedImage(1, 1, 1));

		edit = new JEditorPane();
		edit.setEditorKitForContentType("text/java", editorKit);
		edit.setContentType("text/java");
	}
	

	public Component getEditPane() {
		return edit;
	}
}