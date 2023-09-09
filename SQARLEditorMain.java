import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JPanel;
import java.awt.GridLayout;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Properties;

import javax.swing.JMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import jamiebalfour.HelperFunctions;
import jamiebalfour.zpe.core.ZPE;
import jamiebalfour.zpe.core.ZPEKit;
import jamiebalfour.zpe.core.errors.CompileError;
import jamiebalfour.zpe.editor.CodeEditorView;
import jamiebalfour.zpe.editor.ZPEEditorConsole;
import jamiebalfour.zpe.interfaces.GenericEditor;
import jamiebalfour.zpe.types.CompileDetails;

class SQARLEditorMain extends JFrame implements GenericEditor {

	SQARLEditorMain _this = this;


	CodeEditorView mainSyntax;
	protected UndoHandler undoHandler = new UndoHandler();
	protected UndoManager undoManager = new UndoManager();
	
	ZPEEditorConsole AttachedConsole;
	

	boolean propertiesChanged = false;

	Properties mainProperties;

	private final UndoAction undoAction;
	private final RedoAction redoAction;
	JEditorPane contentEditor;
	
	String lastFileOpened = "";
	

	JMenuItem mntmClearConsoleBeforeRunMenuItem;


	static FileNameExtensionFilter filter1 = new FileNameExtensionFilter("Text files (*.txt)", "txt");
	static FileNameExtensionFilter filter2 = new FileNameExtensionFilter("YASS Executable files (*.yex)", "yex");
	private final JFrame editor;

	JCheckBoxMenuItem chckbxmntmCaseSensitiveCompileCheckItem;
	

	boolean dontUndo = true;
	
	public SQARLEditorMain() {
		setTitle("SQARL Editor");

		final HashMap<String, SimpleAttributeSet> SQARL_KEYWORDS = new HashMap<>(16);
		SQARL_KEYWORDS.put("DECLARE", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("INITIALLY", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("WHILE", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("RECEIVE", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("FROM", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("KEYBOARD", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("END", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("SEND", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("FOR", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("EACH", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("DO", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("IF", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("THEN", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("SET", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("TO", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("DISPLAY", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("ARRAY", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("STRING", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("RECORD", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("CLASS", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("INTEGER", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("REAL", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("BOOLEAN", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("CHARACTER", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("FUNCTION", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("RETURN", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("PROCEDURE", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("AND", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("OR", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("NOT", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("MOD", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("OPEN", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("CLOSE", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("CREATE", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("METHODS", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("THIS", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("WITH", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("OVERRIDE", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("INHERITS", CodeEditorView.DEFAULT_KEYWORD);
		SQARL_KEYWORDS.put("CONSTRUCTOR", CodeEditorView.DEFAULT_KEYWORD);

		mainSyntax = new CodeEditorView(SQARL_KEYWORDS, "\"'", "");
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				
				int confirmed = JOptionPane.showConfirmDialog(editor, "Are you sure you want to exit the program?", "Exit Program Message Box", JOptionPane.YES_NO_OPTION);
				if (confirmed == JOptionPane.YES_OPTION) {
					dispose();
				}
				
				setProperty("HEIGHT", "" + editor.getHeight());
				setProperty("WIDTH", "" + editor.getWidth());
				setProperty("XPOS", "" + editor.getX());
				setProperty("YPOS", "" + editor.getY());
				if (editor.getExtendedState() == JFrame.MAXIMIZED_BOTH) {
					setProperty("MAXIMISED", "true");
				} else {
					setProperty("MAXIMISED", "false");
				}
				saveGUISettings(mainProperties);
			}
		});

		File f = new File(System.getProperty("user.home") + "/zpe/sqarl/");

		if(!f.exists()){
			f.mkdirs();
		}
		
		String path = System.getProperty("user.home") + "/zpe/sqarl/" + "gui.sqarl.properties";

		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e2) {
			System.out.println(e2.getMessage());
		}
		

		mainProperties = HelperFunctions.ReadProperties(path);

		this.editor = this;
		getContentPane().setLayout(new GridLayout(0, 1, 0, 0));
		
		
		this.setSize(new Dimension(600, 400));
		
		JPanel mainPanel = new JPanel();
		getContentPane().add(mainPanel);
		mainPanel.setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPane.setEnabled(false);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.setBackground(Color.WHITE);
		mainPanel.add(scrollPane, BorderLayout.CENTER);
		
		
		contentEditor = (JEditorPane) mainSyntax.getEditPane();
		contentEditor.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));

		contentEditor.setFont(new Font("Monospaced", Font.PLAIN, 18));
		
		contentEditor.setText("DECLARE total INITIALLY 0\r\n"
				+ "DECLARE counter INITIALLY 0\r\n"
				+ "DECLARE nextInput INITIALLY 0\r\n"
				+ "WHILE counter < 10 DO\r\n"
				+ "  SEND \"Insert a number\" TO DISPLAY\r\n"
				+ "  RECEIVE nextInput FROM KEYBOARD\r\n"
				+ "  SET total TO total + nextInput\r\n"
				+ "  SET counter TO counter + 1\r\n"
				+ "END WHILE\r\n"
				+ "SEND total / 10.0 TO DISPLAY");
		scrollPane.setViewportView(contentEditor);

		scrollPane.setRowHeaderView(mainSyntax.getEditor());
		
		JMenuBar menuBar = new JMenuBar();
		
		setJMenuBar(menuBar);
		
		int modifier = InputEvent.CTRL_DOWN_MASK;
		if (HelperFunctions.isMac()) {
			modifier = InputEvent.META_DOWN_MASK;
		}

		
		
		JMenu mnFileMenu = new JMenu("File");
		mnFileMenu.setMnemonic('F');
		menuBar.add(mnFileMenu);
		
		

		JMenuItem mntmNewMenuItem = new JMenuItem("New");
		mntmNewMenuItem.setAccelerator(KeyStroke.getKeyStroke('N', modifier));
		mntmNewMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearUndoRedoManagers();
				setTextProperly("");
			}
		});
		mnFileMenu.add(mntmNewMenuItem);
		
		JMenuItem mntmSaveMenuItem = new JMenuItem("Save");
		mntmSaveMenuItem.setAccelerator(KeyStroke.getKeyStroke('S', modifier));
		mntmSaveMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (lastFileOpened.isEmpty()) {
					saveAsDialog();
				} else {
					try {
						HelperFunctions.WriteFile(lastFileOpened, contentEditor.getText(), false);
					} catch (IOException ex) {
						ZPE.Log("SQARL Runtime error: " + ex.getMessage());
					}
				}
			}
		});
		mnFileMenu.add(mntmSaveMenuItem);

		JMenuItem mntmSaveAsMenuItem = new JMenuItem("Save As");
		mntmSaveAsMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveAsDialog();
			}
		});
		mnFileMenu.add(mntmSaveAsMenuItem);

		JMenuItem mntmOpenMenuItem = new JMenuItem("Open");
		mntmOpenMenuItem.setAccelerator(KeyStroke.getKeyStroke('O', modifier));
		mntmOpenMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				open();
			}
		});
		mnFileMenu.add(mntmOpenMenuItem);

		mnFileMenu.add(new JSeparator());

		JMenuItem mntmPrintMenuItem = new JMenuItem("Print");
		mntmPrintMenuItem.setAccelerator(KeyStroke.getKeyStroke('P', modifier));
		mntmPrintMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					contentEditor.print();
				} catch (PrinterException e1) {
					JOptionPane.showMessageDialog(editor, "An error was encountered whilst trying to print.", "Error",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		mnFileMenu.add(mntmPrintMenuItem);

		mnFileMenu.add(new JSeparator());

		JMenuItem mntmExitMenuItem = new JMenuItem("Exit");
		mntmExitMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		
		
		mnFileMenu.add(mntmExitMenuItem);
		
		
		this.contentEditor.getDocument().addUndoableEditListener(undoHandler);

		KeyStroke undoKeystroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z, modifier);
		KeyStroke redoKeystroke = KeyStroke.getKeyStroke(KeyEvent.VK_Y, modifier);

		undoAction = new UndoAction();
		contentEditor.getInputMap().put(undoKeystroke, "undoKeystroke");
		contentEditor.getActionMap().put("undoKeystroke", undoAction);

		redoAction = new RedoAction();
		contentEditor.getInputMap().put(redoKeystroke, "redoKeystroke");
		contentEditor.getActionMap().put("redoKeystroke", redoAction);
		

		JMenu mnEditMenu = new JMenu("Edit");
		mnEditMenu.setMnemonic('E');
		menuBar.add(mnEditMenu);
		

		JMenuItem mntmUndoMenuItem = new JMenuItem(undoAction);
		mntmUndoMenuItem.setAccelerator(KeyStroke.getKeyStroke('Z', modifier));
		
		mnEditMenu.add(mntmUndoMenuItem);

		JMenuItem mntmRedoMenuItem = new JMenuItem(redoAction);
		mntmRedoMenuItem.setAccelerator(KeyStroke.getKeyStroke('Y', modifier));
		mnEditMenu.add(mntmRedoMenuItem);

		mnEditMenu.add(new JSeparator());

		JMenuItem mntmCutMenuItem = new JMenuItem("Cut");
		mntmCutMenuItem.setAccelerator(KeyStroke.getKeyStroke('X', modifier));
		mntmCutMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				contentEditor.cut();

			}
		});
		mnEditMenu.add(mntmCutMenuItem);

		JMenuItem mntmCopyMenuItem = new JMenuItem("Copy");
		mntmCopyMenuItem.setAccelerator(KeyStroke.getKeyStroke('C', modifier));
		mntmCopyMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				contentEditor.copy();

			}
		});
		mnEditMenu.add(mntmCopyMenuItem);

		JMenuItem mntmPasteMenuItem = new JMenuItem("Paste");
		mntmPasteMenuItem.setAccelerator(KeyStroke.getKeyStroke('V', modifier));
		mntmPasteMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				contentEditor.paste();

			}
		});
		mnEditMenu.add(mntmPasteMenuItem);

		JMenuItem mntmDeleteMenuItem = new JMenuItem("Delete");
		mntmDeleteMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int start = contentEditor.getSelectionStart();
				int end = contentEditor.getSelectionEnd();

				String current = contentEditor.getText();

				String newText = current.substring(0, start) + current.substring(end);
				contentEditor.setText(newText);

			}
		});
		mnEditMenu.add(mntmDeleteMenuItem);

		mnEditMenu.add(new JSeparator());

		JMenuItem mntmSelectAllMenuItem = new JMenuItem("Select All");
		mntmSelectAllMenuItem.setAccelerator(KeyStroke.getKeyStroke('A', modifier));
		mntmSelectAllMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				contentEditor.selectAll();

			}
		});

		mnEditMenu.add(mntmSelectAllMenuItem);
		

		JMenu mnScriptMenu = new JMenu("Script");
		mnScriptMenu.setMnemonic('S');
		menuBar.add(mnScriptMenu);

		chckbxmntmCaseSensitiveCompileCheckItem = new JCheckBoxMenuItem("Case sensitive compile");
		chckbxmntmCaseSensitiveCompileCheckItem.setSelected(true);
		mnScriptMenu.add(chckbxmntmCaseSensitiveCompileCheckItem);
		
		mntmClearConsoleBeforeRunMenuItem = new JCheckBoxMenuItem("Clear console before running");
		mntmClearConsoleBeforeRunMenuItem.setSelected(true);
		mnScriptMenu.add(mntmClearConsoleBeforeRunMenuItem);

		mnScriptMenu.add(new JSeparator());

		JMenuItem mntmRunCodeMenuItem = new JMenuItem("Run code");
		mntmRunCodeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
		mntmRunCodeMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				/*
				 * running = new ConsoleThread(); running.startConsole(contentEditor.getText(),
				 * runtimeArgs, !chckbxmntmCaseSensitiveCompileCheckItem.isSelected());
				 */
				if (AttachedConsole == null) {
					AttachedConsole = new ZPEEditorConsole(_this, "");
				} else {
					AttachedConsole.stop(0);
				}
				
				SQARLParser sqarl = new SQARLParser();
			    String yass = sqarl.parseToYASS(contentEditor.getText());
				
				AttachedConsole.runCode(yass, new String[0],
						chckbxmntmCaseSensitiveCompileCheckItem.isSelected());
			}
		});
		mnScriptMenu.add(mntmRunCodeMenuItem);

		JMenuItem mntmStopCodeMenuItem = new JMenuItem("Stop code");
		mntmStopCodeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
		mntmStopCodeMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AttachedConsole.stop();
			}
		});

		mnScriptMenu.add(mntmStopCodeMenuItem);

		mnScriptMenu.add(new JSeparator());

		JMenuItem mntmCompileCodeMenuItem = new JMenuItem("Compile code");
		mntmCompileCodeMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String name = JOptionPane.showInputDialog(editor,
						"Please insert the name of the compiled application.");
				CompileDetails details = new CompileDetails();

				details.name = name;
				File file;
				String extension;

				final JFileChooser fc = new JFileChooser();

				fc.addChoosableFileFilter(filter2);
				fc.setAcceptAllFileFilterUsed(false);

				int returnVal = fc.showSaveDialog(editor.getContentPane());

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					file = fc.getSelectedFile();
					extension = getSaveExtension(fc.getFileFilter());
				} else {
					return;
				}

				try {


					SQARLParser sqarl = new SQARLParser();
				    String yass = sqarl.parseToYASS(contentEditor.getText());
					// null for no password
					jamiebalfour.zpe.core.ZPEKit.compile(yass, file.toString() + "." + extension, details,
							!chckbxmntmCaseSensitiveCompileCheckItem.isSelected(), false, null, null);

					JOptionPane.showMessageDialog(editor,
							"YASS compile success. The file has been successfully compiled to " + file + ".",
							"YASS compiler", JOptionPane.WARNING_MESSAGE);

				} catch (IOException ex) {
					JOptionPane.showMessageDialog(editor,
							"YASS compile failure. The YASS compiler could not compile the code given. The error was -1.",
							"YASS compiler", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		mnScriptMenu.add(mntmCompileCodeMenuItem);

		mnScriptMenu.add(new JSeparator());
		
		JMenuItem mntmAnalyseCodeMenuItem = new JMenuItem("Analyse code");
		mntmAnalyseCodeMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {


				SQARLParser sqarl = new SQARLParser();
			    String yass = sqarl.parseToYASS(contentEditor.getText());

				try {
					if (ZPEKit.validateCode(yass)) {
						JOptionPane.showMessageDialog(editor, "Code is valid", "Code analysis",
								JOptionPane.INFORMATION_MESSAGE);
					} else {
						JOptionPane.showMessageDialog(editor, "Code is invalid", "Code analysis",
								JOptionPane.INFORMATION_MESSAGE);
					}
				} catch (CompileError ex) {
					JOptionPane.showMessageDialog(editor, "Code is invalid", "Code analysis",
							JOptionPane.INFORMATION_MESSAGE);
				}

			}
		});

		mnScriptMenu.add(mntmAnalyseCodeMenuItem);
		

		JMenuItem mntmToByteCodeFileMenuItem = new JMenuItem("Compile to byte codes");
		mntmToByteCodeFileMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				final JFileChooser fc = new JFileChooser();

				fc.addChoosableFileFilter(filter2);
				fc.setAcceptAllFileFilterUsed(false);

				int returnVal = fc.showSaveDialog(editor.getContentPane());

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					String extension = getSaveExtension(fc.getFileFilter());
					// This is where a real application would open the file.
					try {

						SQARLParser sqarl = new SQARLParser();
					    String yass = sqarl.parseToYASS(contentEditor.getText());
					    
						StringBuilder text = new StringBuilder();
						for (byte s : jamiebalfour.zpe.core.ZPEKit.parseToBytes(yass)) {
							text.append(s).append(" ");
						}
						HelperFunctions.WriteFile(file.getAbsolutePath() + "." + extension, text.toString(), false);
					} catch (IOException ex) {
						JOptionPane.showMessageDialog(editor, "The file could not be saved.", "Error",
								JOptionPane.ERROR_MESSAGE);
					}
				}

			}
		});

		mnScriptMenu.add(mntmToByteCodeFileMenuItem);
	}
	
	private void clearUndoRedoManagers() {
		undoManager.die();
		undoAction.update();
		redoAction.update();
	}
	
	class UndoHandler implements UndoableEditListener {

		/**
		 * Messaged when the Document has created an edit, the edit is added to
		 * <code>undoManager</code>, an instance of UndoManager.
		 */
		public void undoableEditHappened(UndoableEditEvent e) {
			if (!e.getEdit().getPresentationName().equals("style change") && !dontUndo) {
				undoManager.addEdit(e.getEdit());
				undoAction.update();
				redoAction.update();
			}

		}
	}

	class UndoAction extends AbstractAction {

		private static final long serialVersionUID = -3804879849241500100L;

		public UndoAction() {
			super("Undo");
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent e) {
			try {
				undoManager.undo();
			} catch (CannotUndoException ex) {
				JOptionPane.showMessageDialog(editor, "Cannot undo.", "Error", JOptionPane.ERROR_MESSAGE);
			}
			update();
			redoAction.update();
		}

		protected void update() {
			if (undoManager.canUndo()) {
				setEnabled(true);
				putValue(Action.NAME, undoManager.getUndoPresentationName());
			} else {
				setEnabled(false);
				putValue(Action.NAME, "Undo");
			}
		}
	}

	class RedoAction extends AbstractAction {

		private static final long serialVersionUID = -2308035050104867155L;

		public RedoAction() {
			super("Redo");
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent e) {
			try {
				undoManager.redo();
			} catch (CannotRedoException ex) {
				JOptionPane.showMessageDialog(editor, "Cannot redo.", "Error", JOptionPane.ERROR_MESSAGE);
			}
			update();
			undoAction.update();
		}

		protected void update() {
			if (undoManager.canRedo()) {
				setEnabled(true);
				putValue(Action.NAME, undoManager.getRedoPresentationName());
			} else {
				setEnabled(false);
				putValue(Action.NAME, "Redo");
			}
		}
	}
	
	private void open() {
		final JFileChooser fc = new JFileChooser();

		fc.addChoosableFileFilter(filter1);

		int returnVal = fc.showOpenDialog(this.getContentPane());

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			// This is where a real application would open the file.
			try {
				clearUndoRedoManagers();
				setTextProperly(HelperFunctions.ReadFileAsString(file.getAbsolutePath()));
				editor.setTitle("ZPE Editor " + file.getAbsolutePath());
			} catch (IOException e) {
				JOptionPane.showMessageDialog(editor, "The file could not be opened.", "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void saveAsDialog() {
		final JFileChooser fc = new JFileChooser();

		fc.addChoosableFileFilter(filter1);
		fc.setAcceptAllFileFilterUsed(false);

		int returnVal = fc.showSaveDialog(this.getContentPane());

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			String extension = getSaveExtension(fc.getFileFilter());
			// This is where a real application would open the file.
			try {
				HelperFunctions.WriteFile(file.getAbsolutePath() + "." + extension, contentEditor.getText(), false);
				lastFileOpened = file.getAbsolutePath();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(editor, "The file could not be saved.", "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	public void saveGUISettings(Properties props) {
		if (propertiesChanged) {
			File f = new File(System.getProperty("user.home") + "/zpe/sqarl/");
			if(!f.exists()){
				f.mkdirs();
			}
			OutputStream output;
			String path = System.getProperty("user.home") + "/zpe/sqarl/" + "gui.properties";

			try {
				output = new FileOutputStream(path);
				// save properties to project root folder
				props.store(output, null);
			} catch (Exception e) {
				ZPE.Log("SQARL Runtime error: GUI cannot save" + e.getMessage());
			}
		}
	}
	
	public void setProperty(String name, String value) {
		this.mainProperties.setProperty(name, value);
		this.propertiesChanged = true;
	}
	
	static String getSaveExtension(FileFilter f) {
		if (f.equals(filter1)) {
			return "txt";
		}

		return null;
	}
	
	void setTextProperly(String text) {
		dontUndo = true;
		contentEditor.setText(text);
		dontUndo = false;
		//contentEditor.setCaretPosition(0);
	}

	@Override
	public boolean clearTextBeforeRunning() {
		return mntmClearConsoleBeforeRunMenuItem.isSelected();
	}

	@Override
	public void destroyConsole() {
		this.AttachedConsole = null;
		
	}

	@Override
	public Properties getProperties() {
		return this.mainProperties;
	}

}
