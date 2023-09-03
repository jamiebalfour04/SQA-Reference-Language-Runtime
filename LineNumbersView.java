import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Utilities;

import jamiebalfour.zpe.core.RunningInstance;

class LineNumbersView extends JComponent implements DocumentListener, CaretListener, ComponentListener {
	//http://rememberjava.com/ui/2017/02/19/line_numbers.html
	private static final long serialVersionUID = 1L;

	private JTextComponent editor;

	private Font font;
	int marginSize = 50;

	public LineNumbersView(JTextComponent editor) {
		this.editor = editor;

		editor.getDocument().addDocumentListener(this);
		editor.addComponentListener(this);
		editor.addCaretListener(this);
	}

	public void updateFont(Font f) {
		this.font = f;
		
		if(f.getSize() > 30) {
			marginSize = 100;
		} else if(f.getSize() > 20) {
			marginSize = 80;
		} else {
			marginSize = 50;
		}
		updateSize();
		repaint();
	}
	
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Rectangle clip = g.getClipBounds();
		int startOffset = editor.viewToModel2D(new Point(0, clip.y));
		int endOffset = editor.viewToModel2D(new Point(0, clip.y + clip.height));
		g.setColor(editor.getBackground());
		g.fillRect(0, 0, this.getWidth(), this.getHeight());
		while (startOffset <= endOffset) {
			try {
				String lineNumber = getLineNumber(startOffset);
				if (lineNumber != null) {
					int x = getInsets().left + 2;
					int y = getOffsetY(startOffset);

					font = font != null ? font : new Font(editor.getFont().getFamily(), Font.PLAIN, editor.getFont().getSize());
					
					g.setFont(font);
					g.setColor(isCurrentLine(startOffset) ? new Color(0, 200, 0) : new Color(130, 130, 130));
					g.drawString(lineNumber, x, y);
					
				}

				startOffset = Utilities.getRowEnd(editor, startOffset) + 1;
			} catch (BadLocationException e) {
				if(RunningInstance.ERROR_LEVEL == 2) {
					e.printStackTrace();
				}
				
				// ignore and continue
			}
		}
	}

	/**
	 * Returns the line number of the element based on the given (start) offset in
	 * the editor model. Returns null if no line number should or could be provided
	 * (e.g. for wrapped lines).
	 */
	private String getLineNumber(int offset) {
		Element root = editor.getDocument().getDefaultRootElement();
		int index = root.getElementIndex(offset);
		Element line = root.getElement(index);

		return line.getStartOffset() == offset ? String.format("%3d", index + 1) : null;
	}

	/**
	 * Returns the y axis position for the line number belonging to the element at
	 * the given (start) offset in the model.
	 */
	private int getOffsetY(int offset) throws BadLocationException {
		FontMetrics fontMetrics = editor.getFontMetrics(editor.getFont());
		int descent = fontMetrics.getDescent();

		@SuppressWarnings("deprecation")
		Rectangle r = editor.modelToView(offset);
		int y = r.y + r.height - descent;

		return y;
	}

	/**
	 * Returns true if the given start offset in the model is the selected (by
	 * cursor position) element.
	 */
	private boolean isCurrentLine(int offset) {
		int caretPosition = editor.getCaretPosition();
		Element root = editor.getDocument().getDefaultRootElement();
		return root.getElementIndex(offset) == root.getElementIndex(caretPosition);
	}

	/**
	 * Schedules a refresh of the line number margin on a separate thread.
	 */
	private void documentChanged() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				repaint();
			}
		});
	}

	/**
	 * Updates the size of the line number margin based on the editor height.
	 */
	private void updateSize() {
		Dimension size = new Dimension(marginSize, editor.getHeight());
		setPreferredSize(size);
		setSize(size);
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		documentChanged();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		documentChanged();
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		documentChanged();
	}

	@Override
	public void caretUpdate(CaretEvent e) {
		documentChanged();
	}

	@Override
	public void componentResized(ComponentEvent e) {
		updateSize();
		documentChanged();
	}

	@Override
	public void componentMoved(ComponentEvent e) {
	}

	@Override
	public void componentShown(ComponentEvent e) {
		updateSize();
		documentChanged();
	}

	@Override
	public void componentHidden(ComponentEvent e) {
	}
}
