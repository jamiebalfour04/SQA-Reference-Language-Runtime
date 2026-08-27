package jamiebalfour.zpe;

import jamiebalfour.helpers.*;
import jamiebalfour.codeeditor.CodeEditorView;
import jamiebalfour.ui.BalfLafManager;
import jamiebalfour.ui.components.BalfButton;
import jamiebalfour.ui.components.BalfMenuBar;
import jamiebalfour.ui.components.BalfScrollbarPane;
import jamiebalfour.ui.dialogs.BalfAboutDialog;
import jamiebalfour.ui.windows.BalfWindow;
import jamiebalfour.zpe.core.ZPE;
import jamiebalfour.zpe.core.IAST;
import jamiebalfour.zpe.core.ZPEInstance;
import jamiebalfour.zpe.core.ZPEKit;
import jamiebalfour.zpe.gui.editor.ConsoleOutputTextArea;
import jamiebalfour.zpe.gui.editor.YASSUnfoldDialog;
import jamiebalfour.zpe.gui.editor.ZPEEditor;
import jamiebalfour.zpe.core.exceptions.CompileException;
import jamiebalfour.zpe.core.interfaces.GenericEditor;
import jamiebalfour.zpe.parser.v6.ZenithParsingEngine;
import jamiebalfour.zpe.transpilers.PythonTranspiler;
import jamiebalfour.zpe.transpilers.ZPEJavascriptTranspiler;
import jamiebalfour.zpe.transpilers.ZPEPhpTranspiler;
import jamiebalfour.zpe.transpilers.ZPEPythonTranspiler;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

class SQARLEditorMain extends BalfWindow implements GenericEditor {

  SQARLEditorMain _this = this;


  CodeEditorView mainSyntax;
  protected UndoHandler undoHandler = new UndoHandler();
  protected UndoManager undoManager = new UndoManager();
  SQARLEditorMain _frame = this;

  ConsoleOutputTextArea AttachedConsole;
  Process currentProcess;


  boolean propertiesChanged = false;

  Properties mainProperties;

  private final UndoAction undoAction;
  private final RedoAction redoAction;
  BalfScrollbarPane scrollPane;
  BalfMenuBar.CheckBoxMenuItem mnDarkModeMenuItem;
  BalfMenuBar.Menu mntmRecentMenuItem;
  BalfMenuBar menuBar;

  String lastFileOpened = "";
  private boolean darkMode = false;
  BalfMenuBar.MenuItem mntmStopCodeMenuItem;


  BalfMenuBar.MenuItem mntmClearConsoleBeforeRunMenuItem;


  static FileNameExtensionFilter filter1 = new FileNameExtensionFilter("Text files (*.txt)", "txt");
  static FileNameExtensionFilter filter2 = new FileNameExtensionFilter("YASS Executable files (*.yex)", "yex");
  static FileNameExtensionFilter winExe = new FileNameExtensionFilter("Executable files (*.exe)", "exe");
  private final JFrame editor;

  BalfMenuBar.CheckBoxMenuItem chckbxmntmCaseSensitiveCompileCheckItem;
  ArrayList<String> recents = ZPEEditor.getRecentFiles("sqarl/");

  ImageIcon lighterLogo;
  ImageIcon lighterLogoFull;

  boolean dontUndo = true;

  Color borderColor = new Color(40, 75, 99);

  boolean isMaximised = false;
  
  static final Color themeColor = new Color(36, 41, 56);


  /*private void maximiseButtonClicked() {
    if (isMaximised) {
      // Restore to normal size
      setExtendedState(JFrame.NORMAL);
      setSize(800, 600); // Reset to default size
      setLocationRelativeTo(null);
      //setShape(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), rounded ? 20 : 0, rounded ? 20 : 0));
      getContentPane().setPreferredSize(new Dimension(800, 600)); // Ensure layout updates
      getContentPane().revalidate();
      getContentPane().repaint();
    } else {
      // Maximise


      maximiseToCurrentScreen(_this);
      getContentPane().setPreferredSize(null); // Let it auto-resize
      getContentPane().revalidate();
      getContentPane().repaint();
    }

    isMaximised = !isMaximised;
  }*/

  /*public void maximiseToCurrentScreen(JFrame frame) {
    GraphicsConfiguration gc = frame.getGraphicsConfiguration();
    Rectangle screenBounds = gc.getBounds();
    Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);

    // Calculate usable area (excluding taskbar/dock)
    int x = screenBounds.x + screenInsets.left;
    int y = screenBounds.y + screenInsets.top;
    int width = screenBounds.width - screenInsets.left - screenInsets.right;
    int height = screenBounds.height - screenInsets.top - screenInsets.bottom;

    frame.setBounds(x, y, width, height);
    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
  }*/

  public SQARLEditorMain() {
    super("SQARL Runtime", 9, themeColor, Color.white, null);



    getTitleBar().setCloseListener(e -> {closeUp(); System.exit(0);});
    getTitleBar().setLabelText("SQARL Runtime");

    JPanel topContainer = new JPanel();
    topContainer.setOpaque(false);
    topContainer.setLayout(new BorderLayout());

    add(topContainer, BorderLayout.NORTH);


    setTitle("SQARL Editor");

    URL imagePath;
    if (HelperFunctions.isMac()) {
      imagePath = SQARLEditorMain.class.getResource("/files/SQARLLogoMacOS.png");
    } else {
      imagePath = SQARLEditorMain.class.getResource("/files/SQARLLogoMacOS.png");
    }
    assert imagePath != null;
    lighterLogoFull = new ImageIcon(imagePath);
    Image newimg = lighterLogoFull.getImage().getScaledInstance(60, 60, java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
    lighterLogo = new ImageIcon(newimg);

    mainSyntax = new CodeEditorView(false);


    final HashMap<String, SimpleAttributeSet> SQARL_KEYWORDS = new HashMap<>(16);
    SQARL_KEYWORDS.put("DECLARE", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("INITIALLY", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("WHILE", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("RECEIVE", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("FROM", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("KEYBOARD", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("END", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("SEND", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("FOR", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("EACH", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("DO", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("IF", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("THEN", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("SET", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("TO", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("DISPLAY", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("ARRAY", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("STRING", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("RECORD", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("CLASS", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("INTEGER", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("REAL", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("BOOLEAN", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("CHARACTER", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("FUNCTION", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("RETURN", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("PROCEDURE", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("AND", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("OR", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("NOT", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("MOD", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("OPEN", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("CLOSE", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("CREATE", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("METHODS", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("THIS", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("WITH", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("OVERRIDE", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("INHERITS", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("CONSTRUCTOR", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("IS", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("AS", mainSyntax.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("ELSE", mainSyntax.DEFAULT_KEYWORD);




    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {

        boolean confirmed = BalfLafManager.showConfirm(editor, "Are you sure you want to exit the program?", "Exit Program");
        if (confirmed) {
          dispose();

          closeUp();
          System.exit(0);
        }
      }
    });

    File f = new File(ZPEKit.getInstallPath() + "/sqarl/");

    if (!f.exists()) {
      if(!f.mkdirs()){
        ZPE.log(f + " could not be created");
      }
    }

    String path = ZPEKit.getInstallPath() + "/sqarl/" + "gui.properties";


    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e2) {
      System.out.println(e2.getMessage());
    }


    try{
      mainProperties = HelperFunctions.readProperties(path);
    } catch (Exception e){
      //Ignore
    }



    this.editor = this;

    this.setSize(new Dimension(600, 400));

    editor.setMinimumSize(new Dimension(600, 600));

    if (mainProperties.containsKey("HEIGHT")) {
      editor.setSize(editor.getWidth(), HelperFunctions.stringToInteger(mainProperties.get("HEIGHT").toString()));
    }
    if (mainProperties.containsKey("WIDTH")) {
      editor.setSize(HelperFunctions.stringToInteger(mainProperties.get("WIDTH").toString()), editor.getHeight());
    }
    if (mainProperties.containsKey("XPOS")) {
      editor.setLocation(
              new Point(HelperFunctions.stringToInteger(mainProperties.get("XPOS").toString()), editor.getY()));
    }
    if (mainProperties.containsKey("YPOS")) {
      editor.setLocation(
              new Point(editor.getX(), HelperFunctions.stringToInteger(mainProperties.get("YPOS").toString())));
    }
    if (mainProperties.containsKey("MAXIMISED")) {
      if (mainProperties.get("MAXIMISED").toString().equals("true")) {
        maximiseAction();
      }
    }




    JPanel mainPanel = new JPanel();
    //getContentPane().add(mainPanel, BorderLayout.CENTER);
    mainPanel.setLayout(new BorderLayout(0, 0));

    scrollPane = new BalfScrollbarPane();
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.setLightColour(Color.white);
    mainPanel.add(scrollPane, BorderLayout.CENTER);
    mainPanel.setBorder(new LineBorder(Color.black, 3));
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    AttachedConsole = new ConsoleOutputTextArea("", Color.WHITE);
    AttachedConsole.addProcessFinishedListener(() -> SwingUtilities.invokeLater(() -> {
      if (mntmStopCodeMenuItem != null) mntmStopCodeMenuItem.setEnabled(false);
    }));
    BalfScrollbarPane consolePane = new BalfScrollbarPane(AttachedConsole, 0);
    consolePane.setLightColour(Color.BLACK);
    consolePane.setBorder(BorderFactory.createEmptyBorder());
    JSplitPane editorAndConsole = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainPanel, consolePane);
    editorAndConsole.setResizeWeight(0.72);
    editorAndConsole.setDividerLocation(0.72);
    editorAndConsole.setDividerSize(4);
    add(editorAndConsole, BorderLayout.CENTER);

    scrollPane.setRowHeaderView(mainSyntax.getEditor());

    mainSyntax.setWrapper(scrollPane);



    mainSyntax.setFont(new Font("Monospaced", Font.PLAIN, 18));

    mainSyntax.setText("RECORD pupil IS {STRING name, INTEGER age}\r\n" +
            "DECLARE total INITIALLY 0\r\n"
            + "DECLARE counter INITIALLY 0\r\n"
            + "DECLARE nextInput INITIALLY 0\r\n"
            + "WHILE counter < 10 DO\r\n"
            + "  SEND \"Insert a number\" TO DISPLAY\r\n"
            + "  RECEIVE nextInput FROM KEYBOARD\r\n"
            + "  SET total TO total + nextInput\r\n"
            + "  SET counter TO counter + 1\r\n"
            + "END WHILE\r\n"
            + "SEND total / 10.0 TO DISPLAY");



    for(String keyword : SQARL_KEYWORDS.keySet()){
      mainSyntax.addAutoCompleteItem(keyword, CodeEditorView.AutoCompleteItemType.Keyword);
    }

    this.mainSyntax.clearKeywords();

    for(String s : SQARL_KEYWORDS.keySet()){
      mainSyntax.addKeyword(s, SQARL_KEYWORDS.get(s));
    }



    //scrollPane.add(mainSyntax.getEditor());
    //scrollPane.setViewportView(mainSyntax.getEditor());

    //mainSyntax.set

    //scrollPane.setRowHeaderView(mainSyntax.getEditor());

    // === Footer setup ===
    getFooter().setText("<html>&copy; J Balfour 2019 - 2025</html>");


    /*try {
      if (java.awt.Taskbar.isTaskbarSupported()) {
        final java.awt.Taskbar taskbar = java.awt.Taskbar.getTaskbar();
      }
    } catch (Exception e) {
      //Don't do anything
    }*/
    if (HelperFunctions.isMac()) {
      System.setProperty("apple.laf.useScreenMenuBar", "true");
      try {
        macOS.addAboutDialog(this::showAbout);
      } catch (Exception e) {
        //Don't do anything
      }
    }

    menuBar = new BalfMenuBar(themeColor, Color.white);
    menuBar.setPaneColour(new Color(63, 71, 89));

    topContainer.add(menuBar, BorderLayout.NORTH);

    //setJMenuBar(menuBar);
    
    getTitleBar().attachMenu(menuBar);

    int modifier = InputEvent.CTRL_DOWN_MASK;
    if (HelperFunctions.isMac()) {
      modifier = InputEvent.META_DOWN_MASK;
    }


    this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    BalfMenuBar.Menu mnFileMenu = new BalfMenuBar.Menu("File", menuBar);
    mnFileMenu.setMnemonic('F');
    menuBar.add(mnFileMenu);


    BalfMenuBar.MenuItem mntmNewMenuItem = new BalfMenuBar.MenuItem("New", menuBar);
    mntmNewMenuItem.setAccelerator(KeyStroke.getKeyStroke('N', modifier));
    mntmNewMenuItem.addActionListener(e -> {
      clearUndoRedoManagers();
      setTextProperly("");
    });
    mnFileMenu.add(mntmNewMenuItem);

    BalfMenuBar.MenuItem mntmSaveMenuItem = new BalfMenuBar.MenuItem("Save", menuBar);
    mntmSaveMenuItem.setAccelerator(KeyStroke.getKeyStroke('S', modifier));
    mntmSaveMenuItem.addActionListener(e -> {
      if (lastFileOpened.isEmpty()) {
        saveAsDialog();
      } else {
        try {
          FileHelperFunctions.writeFile(lastFileOpened, mainSyntax.getText(), false);
        } catch (IOException ex) {
          ZPE.log("SQARL Runtime error: " + ex.getMessage());
        }
      }
    });
    mnFileMenu.add(mntmSaveMenuItem);

    BalfMenuBar.MenuItem mntmSaveAsMenuItem = new BalfMenuBar.MenuItem("Save As", menuBar);
    mntmSaveAsMenuItem.addActionListener(e -> saveAsDialog());
    mnFileMenu.add(mntmSaveAsMenuItem);

    mnFileMenu.add(new BalfMenuBar.Separator(menuBar));

    BalfMenuBar.MenuItem mntmOpenMenuItem = new BalfMenuBar.MenuItem("Open", menuBar);
    mntmOpenMenuItem.setAccelerator(KeyStroke.getKeyStroke('O', modifier));
    mntmOpenMenuItem.addActionListener(e -> open());
    mnFileMenu.add(mntmOpenMenuItem);

    mntmRecentMenuItem = new BalfMenuBar.Menu("Recent files", menuBar);

    updateRecentFiles();

    if(!recents.isEmpty()) {
      mnFileMenu.add(mntmRecentMenuItem);
    }


    mnFileMenu.add(new BalfMenuBar.Separator(menuBar));

    BalfMenuBar.MenuItem mntmPrintMenuItem = new BalfMenuBar.MenuItem("Print", menuBar);
    mntmPrintMenuItem.setAccelerator(KeyStroke.getKeyStroke('P', modifier));
    mntmPrintMenuItem.addActionListener(e -> {
      try {
        mainSyntax.print();
      } catch (PrinterException e1) {
        BalfLafManager.showAlert(editor, "An error was encountered whilst trying to print.", "Error");
      }
    });
    mnFileMenu.add(mntmPrintMenuItem);

    mnFileMenu.add(new BalfMenuBar.Separator(menuBar));

    BalfMenuBar.MenuItem mntmExitMenuItem = new BalfMenuBar.MenuItem("Exit", menuBar);
    mntmExitMenuItem.addActionListener(e -> {
      closeUp();
      System.exit(0);
    });


    mnFileMenu.add(mntmExitMenuItem);


    mainSyntax.getDocument().addUndoableEditListener(undoHandler);

    KeyStroke undoKeystroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z, modifier);
    KeyStroke redoKeystroke = KeyStroke.getKeyStroke(KeyEvent.VK_Y, modifier);

    undoAction = new UndoAction();
    mainSyntax.getInputMap().put(undoKeystroke, "undoKeystroke");
    mainSyntax.getActionMap().put("undoKeystroke", undoAction);

    redoAction = new RedoAction();
    mainSyntax.getInputMap().put(redoKeystroke, "redoKeystroke");
    mainSyntax.getActionMap().put("redoKeystroke", redoAction);


    BalfMenuBar.Menu mnEditMenu = new BalfMenuBar.Menu("Edit", menuBar);
    mnEditMenu.setMnemonic('E');
    menuBar.add(mnEditMenu);


    BalfMenuBar.MenuItem mntmUndoMenuItem = new BalfMenuBar.MenuItem(undoAction, menuBar);
    mntmUndoMenuItem.setAccelerator(KeyStroke.getKeyStroke('Z', modifier));

    mnEditMenu.add(mntmUndoMenuItem);

    BalfMenuBar.MenuItem mntmRedoMenuItem = new BalfMenuBar.MenuItem(redoAction, menuBar);
    mntmRedoMenuItem.setAccelerator(KeyStroke.getKeyStroke('Y', modifier));
    mnEditMenu.add(mntmRedoMenuItem);

    mnEditMenu.add(new BalfMenuBar.Separator(menuBar));

    BalfMenuBar.MenuItem mntmCutMenuItem = new BalfMenuBar.MenuItem("Cut", menuBar);
    mntmCutMenuItem.setPseudoAccelerator(KeyStroke.getKeyStroke('X', modifier));
    mntmCutMenuItem.addActionListener(e -> mainSyntax.cut());
    mnEditMenu.add(mntmCutMenuItem);

    BalfMenuBar.MenuItem mntmCopyMenuItem = new BalfMenuBar.MenuItem("Copy", menuBar);
    mntmCopyMenuItem.setPseudoAccelerator(KeyStroke.getKeyStroke('C', modifier));
    mntmCopyMenuItem.addActionListener(e -> mainSyntax.copy());
    mnEditMenu.add(mntmCopyMenuItem);

    BalfMenuBar.MenuItem mntmPasteMenuItem = new BalfMenuBar.MenuItem("Paste", menuBar);
    mntmPasteMenuItem.setPseudoAccelerator(KeyStroke.getKeyStroke('V', modifier));
    mntmPasteMenuItem.addActionListener(e -> mainSyntax.paste());
    mnEditMenu.add(mntmPasteMenuItem);

    BalfMenuBar.MenuItem mntmDeleteMenuItem = new BalfMenuBar.MenuItem("Delete", menuBar);
    mntmDeleteMenuItem.addActionListener(e -> {
      int start = mainSyntax.getSelectionStart();
      int end = mainSyntax.getSelectionEnd();

      String current = mainSyntax.getText();

      String newText = current.substring(0, start) + current.substring(end);
      mainSyntax.setText(newText);

    });
    mnEditMenu.add(mntmDeleteMenuItem);

    mnEditMenu.add(new BalfMenuBar.Separator(menuBar));

    BalfMenuBar.MenuItem mntmSelectAllMenuItem = new BalfMenuBar.MenuItem("Select All", menuBar);
    mntmSelectAllMenuItem.setAccelerator(KeyStroke.getKeyStroke('A', modifier));
    mntmSelectAllMenuItem.addActionListener(e -> mainSyntax.selectAll());

    mnEditMenu.add(mntmSelectAllMenuItem);

    BalfMenuBar.Menu mnViewMenu = new BalfMenuBar.Menu("View", menuBar);
    mnEditMenu.setMnemonic('V');
    menuBar.add(mnViewMenu);

    mnDarkModeMenuItem = new BalfMenuBar.CheckBoxMenuItem("Dark Mode", menuBar);
    mnViewMenu.add(mnDarkModeMenuItem);
    mnDarkModeMenuItem.addActionListener(e -> {
      if (!darkMode) {
        switchOnDarkMode();
      } else {
        switchOffDarkMode();
      }

      setProperty("DARK_MODE", "" + darkMode);
      saveGUISettings(mainProperties);

    });



    BalfMenuBar.Menu mnScriptMenu = new BalfMenuBar.Menu("Script", menuBar);
    mnScriptMenu.setMnemonic('S');
    menuBar.add(mnScriptMenu);

    chckbxmntmCaseSensitiveCompileCheckItem = new BalfMenuBar.CheckBoxMenuItem("Case sensitive compile", menuBar);
    chckbxmntmCaseSensitiveCompileCheckItem.setSelected(true);
    mnScriptMenu.add(chckbxmntmCaseSensitiveCompileCheckItem);

    mntmClearConsoleBeforeRunMenuItem = new BalfMenuBar.CheckBoxMenuItem("Clear console before running", menuBar);
    mntmClearConsoleBeforeRunMenuItem.setSelected(true);
    mnScriptMenu.add(mntmClearConsoleBeforeRunMenuItem);

    mnScriptMenu.add(new BalfMenuBar.Separator(menuBar));

    BalfMenuBar.MenuItem mntmRunCodeMenuItem = new BalfMenuBar.MenuItem("Run code", menuBar);
    mntmRunCodeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
    mntmRunCodeMenuItem.addActionListener(e -> {
      try {
        Path source = Files.createTempFile("sqarl-editor-", ".sqarl");
        Files.writeString(source, mainSyntax.getText(), StandardCharsets.UTF_8);
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin",
                HelperFunctions.isWindows() ? "java.exe" : "java").toString();
        AttachedConsole.runCommand(Arrays.asList(
                javaExecutable,
                "-cp", System.getProperty("java.class.path"),
                SQARLParser.class.getName(),
                "-r", source.toString()),
                null, "Running SQARL directly through its IAST frontend.",
                mntmClearConsoleBeforeRunMenuItem.isSelected());
        mntmStopCodeMenuItem.setVisible(true);
        mntmStopCodeMenuItem.setEnabled(true);
      } catch (IOException exception) {
        BalfLafManager.showAlert(editor,
                "SQARL Runtime error: " + exception.getMessage(), "SQARL Runtime error");
      }
    });
    mnScriptMenu.add(mntmRunCodeMenuItem);


    mntmStopCodeMenuItem = new BalfMenuBar.MenuItem("Stop code", menuBar);
    mntmStopCodeMenuItem.setEnabled(false);
    mntmStopCodeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
    mntmStopCodeMenuItem.addActionListener(e -> {
      if (AttachedConsole != null) AttachedConsole.destroyCurrentProcess();
      mntmStopCodeMenuItem.setEnabled(false);
    });

    mnScriptMenu.add(mntmStopCodeMenuItem);

    mnScriptMenu.add(new BalfMenuBar.Separator(menuBar));

    BalfMenuBar.MenuItem mntmCompileCodeMenuItem = new BalfMenuBar.MenuItem("Compile code", menuBar);
    mntmCompileCodeMenuItem.addActionListener(e -> {
      String name = BalfLafManager.showInput(editor,"Please insert the name of the compiled application.", "Compiled code name");
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


        IAST program = SQARLParser.compileSQARL(mainSyntax.getText());
        ZPEKit.compile(program, file.toString() + "." + extension, name, "");

        BalfLafManager.showAlert(editor,
                "SQARL compile success. The file has been successfully compiled to " + file + ".",
                "SQARL compiler");

      } catch (IOException ex) {
        BalfLafManager.showAlert(editor,
                "SQARL compile failure. The compiled file could not be written due to an IOException.",
                "SQARL compiler");
      } catch (CompileException ex) {
        BalfLafManager.showAlert(editor,
                "SQARL compile failure. The error was: " + ex.getMessage(),
                "SQARL compiler");
      }
    });

    mnScriptMenu.add(mntmCompileCodeMenuItem);

    BalfMenuBar.MenuItem mntmCompileCodeNativeMenuItem = new BalfMenuBar.MenuItem("Compile code to binary", menuBar);
    mntmCompileCodeNativeMenuItem.addActionListener(e -> {
      String applicationName = BalfLafManager.showInput(_frame, "Please insert the name of the compiled application.", "Application name");
      boolean wait = false;

      File file12;
      final JFileChooser fc = new JFileChooser();
      fc.setAcceptAllFileFilterUsed(false);

      if(HelperFunctions.isWindows()){
        fc.addChoosableFileFilter(winExe);
      } else{
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Binary", " ");

        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(filter);
        fc.setFileFilter(filter);
      }


      int returnVal = fc.showSaveDialog(_frame.getContentPane());
      if (returnVal != JFileChooser.APPROVE_OPTION) return;

      String fpath = fc.getSelectedFile().getAbsolutePath();

      if(HelperFunctions.isWindows()){
        String extension = getSaveExtension(fc.getFileFilter());

        if(!fpath.endsWith(extension)){
          fpath = fpath + extension;
        }
      }



      ZPEInstance.setErrorLevel(4);

      try {
        IAST program = SQARLParser.compileSQARL(mainSyntax.getText());
        ZPEKit.compileNativeBinary(program, applicationName, fpath, wait);
      } catch (CompileException ex) {
        BalfLafManager.showAlert(editor, "SQARL Runtime error: " + ex.getMessage(), "SQARL Runtime error");
      }
    });

    mnScriptMenu.add(mntmCompileCodeNativeMenuItem);

    mnScriptMenu.add(new BalfMenuBar.Separator(menuBar));

    BalfMenuBar.MenuItem mntmTranspileCodeToPythonMenuItem = new BalfMenuBar.MenuItem("Transpile code to Python", menuBar);
    mntmTranspileCodeToPythonMenuItem.addActionListener(e -> {
      File file;
      String extension = ".py";

      final JFileChooser fc = new JFileChooser();
      FileNameExtensionFilter pythonFilter = new FileNameExtensionFilter("Python files (*.py)", "py");
      fc.addChoosableFileFilter(pythonFilter);
      fc.setAcceptAllFileFilterUsed(false);

      int returnVal = fc.showSaveDialog(editor.getContentPane());

      if (returnVal == JFileChooser.APPROVE_OPTION) {
        file = fc.getSelectedFile();
      } else {
        return;
      }

      try {


        IAST program = SQARLParser.compileSQARL(mainSyntax.getText());
        String code = ZPEKit.transpileCode(program, "", new ZPEPythonTranspiler());
        String path1 = file.getPath();
        if(!path1.endsWith(extension)){
          path1 = path1 + extension;
        }
        FileHelperFunctions.writeFile(path1, code, false);

        BalfLafManager.showAlert(editor,"Python transpile success. The file has been successfully compiled to " + path1 + ".", "Python transpiler");

      } catch(Exception ex){
        System.out.println(ex.getMessage());
      }
    });

    mnScriptMenu.add(mntmTranspileCodeToPythonMenuItem);

    BalfMenuBar.MenuItem mntmTranspileCodeToJavaScriptMenuItem = new BalfMenuBar.MenuItem("Transpile code to JavaScript", menuBar);
    mntmTranspileCodeToJavaScriptMenuItem.addActionListener(e -> {
      File file;
      String extension = ".js";

      final JFileChooser fc = new JFileChooser();
      FileNameExtensionFilter jsFilter = new FileNameExtensionFilter("JavaScript files (*.js)", "js");
      fc.addChoosableFileFilter(jsFilter);
      fc.setAcceptAllFileFilterUsed(false);

      int returnVal = fc.showSaveDialog(editor.getContentPane());

      if (returnVal == JFileChooser.APPROVE_OPTION) {
        file = fc.getSelectedFile();
      } else {
        return;
      }

      try {


        IAST program = SQARLParser.compileSQARL(mainSyntax.getText());
        String code = ZPEKit.transpileCode(program, "", new ZPEJavascriptTranspiler());
        String path1 = file.getPath();
        if(!path1.endsWith(extension)){
          path1 = path1 + extension;
        }
        FileHelperFunctions.writeFile(path1, code, false);

        BalfLafManager.showAlert(editor,"JavaScript transpile success. The file has been successfully compiled to " + path1 + ".", "JavaScript transpiler");

      } catch(Exception ex){
        System.out.println(ex.getMessage());
      }
    });

    mnScriptMenu.add(mntmTranspileCodeToJavaScriptMenuItem);

    BalfMenuBar.MenuItem mntmTranspileCodeToPHPMenuItem = new BalfMenuBar.MenuItem("Transpile code to PHP", menuBar);
    mntmTranspileCodeToPHPMenuItem.addActionListener(e -> {
      File file;
      String extension = ".php";

      final JFileChooser fc = new JFileChooser();
      FileNameExtensionFilter phpFilter = new FileNameExtensionFilter("PHP files (*.php)", "php");
      fc.addChoosableFileFilter(phpFilter);
      fc.setAcceptAllFileFilterUsed(false);

      int returnVal = fc.showSaveDialog(editor.getContentPane());

      if (returnVal == JFileChooser.APPROVE_OPTION) {
        file = fc.getSelectedFile();
      } else {
        return;
      }

      try {


        IAST program = SQARLParser.compileSQARL(mainSyntax.getText());
        String code = ZPEKit.transpileCode(program, "", new ZPEPhpTranspiler());
        String path1 = file.getPath();
        if(!path1.endsWith(extension)){
          path1 = path1 + extension;
        }
        FileHelperFunctions.writeFile(path1, code, false);

        BalfLafManager.showAlert(editor,"PHP transpile success. The file has been successfully compiled to " + path1 + ".", "PHP transpiler");

      } catch(Exception ex){
        System.out.println(ex.getMessage());
      }
    });

    mnScriptMenu.add(mntmTranspileCodeToPHPMenuItem);

    mnScriptMenu.add(new BalfMenuBar.Separator(menuBar));

    BalfMenuBar.MenuItem mntmAnalyseCodeMenuItem = new BalfMenuBar.MenuItem("Analyse code", menuBar);
    mntmAnalyseCodeMenuItem.addActionListener(e -> {


      try {
        SQARLParser.compileSQARL(mainSyntax.getText());
        BalfLafManager.showAlert(editor, "Code is valid", "Code analysis");
      } catch (CompileException ex) {
        BalfLafManager.showAlert(editor, "Code is invalid", "Code analysis");
      }

    });

    mnScriptMenu.add(mntmAnalyseCodeMenuItem);


    BalfMenuBar.MenuItem mntmToByteCodeFileMenuItem = new BalfMenuBar.MenuItem("Compile to byte codes", menuBar);
    mntmToByteCodeFileMenuItem.addActionListener(e -> {

      final JFileChooser fc = new JFileChooser();

      fc.addChoosableFileFilter(filter2);
      fc.setAcceptAllFileFilterUsed(false);

      int returnVal = fc.showSaveDialog(editor.getContentPane());

      if (returnVal == JFileChooser.APPROVE_OPTION) {
        File file = fc.getSelectedFile();
        String extension = getSaveExtension(fc.getFileFilter());
        // This is where a real application would open the file.
        try {

          IAST program = SQARLParser.compileSQARL(mainSyntax.getText());

          StringBuilder text = new StringBuilder();
          for (byte s : ZPEKit.parseToBytes(program)) {
            text.append(s).append(" ");
          }
          FileHelperFunctions.writeFile(file.getAbsolutePath() + "." + extension, text.toString(), false);
        } catch (IOException ex) {
          BalfLafManager.showAlert(editor, "The file could not be saved.", "Error");
        } catch (CompileException ex) {
          BalfLafManager.showAlert(editor, "SQARL Runtime error: " + ex.getMessage(), "SQARL Runtime error");
          return;
        }
      }

    });

    mnScriptMenu.add(mntmToByteCodeFileMenuItem);

    BalfMenuBar.MenuItem mntmUnfoldCodeMenuItem = new BalfMenuBar.MenuItem("Unfold (explain) code", menuBar);
    mntmUnfoldCodeMenuItem.addActionListener(e -> {

      String result;
      try {
        IAST program = SQARLParser.compileSQARL(mainSyntax.getText());
        result = ZPEKit.unfold(program, false);
        JOptionPane op = new JOptionPane(new YASSUnfoldDialog(_frame, result, themeColor, getForegroundColour()).getContentPane(), JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, lighterLogo, new String[]{});
        JDialog dlg = op.createDialog(_this, "YASS Unfold code explainer");
        dlg.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setVisible(true);
      } catch (CompileException ex) {
        throw new RuntimeException(ex);
      }

    });

    mnScriptMenu.add(mntmUnfoldCodeMenuItem);

    //Help menu
    BalfMenuBar.Menu mnHelpMenu = new BalfMenuBar.Menu("Help", menuBar);
    mnHelpMenu.setMnemonic('H');
    menuBar.add(mnHelpMenu);

    if (!HelperFunctions.isMac()) {
      BalfMenuBar.MenuItem mntmAboutFileMenuItem = new BalfMenuBar.MenuItem("About", menuBar);
      mntmAboutFileMenuItem.addActionListener(e -> showAbout());
      mnHelpMenu.add(mntmAboutFileMenuItem);
      mnHelpMenu.add(new BalfMenuBar.Separator(menuBar));
    }

    BalfMenuBar.MenuItem mntmSQARLSpecificationWebsiteMenuItem = new BalfMenuBar.MenuItem("Read the SQARL Specification", menuBar);
    mntmSQARLSpecificationWebsiteMenuItem.addActionListener(e -> {try{
      HelperFunctions.openWebsite("https://www.sqa.org.uk/sqa/files_ccc/Reference-language-for-Computing-Science-Sep2016.pdf");
    } catch (Exception ex){
      BalfLafManager.showAlert(editor, "Could not open SQA website", "Failure");
    }});

    mnHelpMenu.add(mntmSQARLSpecificationWebsiteMenuItem);

    BalfMenuBar.MenuItem mntmSQAWebsiteMenuItem = new BalfMenuBar.MenuItem("Visit SQA Website", menuBar);

      mntmSQAWebsiteMenuItem.addActionListener(e -> {try {
        HelperFunctions.openWebsite("https://www.sqa.org.uk/sqa/48486.html");
      } catch (Exception ex){
        BalfLafManager.showAlert(editor, "Could not open SQA website", "Failure");
      }});



    try {
      setIconImage(lighterLogoFull.getImage());
    } catch (Exception ignored) {

    }

    try {
      //Attempts to set the icon in the Dock/taskbar
      if (java.awt.Taskbar.isTaskbarSupported()) {
        final Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        final java.awt.Taskbar taskbar = java.awt.Taskbar.getTaskbar();
        final Image image = lighterLogoFull.getImage();
        taskbar.setIconImage(image);
      }
    } catch (Exception e) {
      //Ignore
    }

    mnHelpMenu.add(mntmSQAWebsiteMenuItem);


    ZPEInstance.setErrorLevel(1);

    if (mainProperties.containsKey("DARK_MODE")) {
      if (mainProperties.get("DARK_MODE").equals("true")) {
        switchOnDarkMode();
      } else {
        switchOffDarkMode();
      }
    }

  }


  private void closeUp(){

    if(currentProcess != null){
      currentProcess.destroy();
      currentProcess = null;
    }

    setProperty("HEIGHT", "" + editor.getHeight());
    setProperty("WIDTH", "" + editor.getWidth());
    setProperty("XPOS", "" + editor.getX());
    setProperty("YPOS", "" + editor.getY());
    if (isMaximised) {
      setProperty("MAXIMISED", "true");
    } else {
      setProperty("MAXIMISED", "false");
    }
    saveGUISettings(mainProperties);
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
        BalfLafManager.showAlert(editor, "Cannot undo.", "Error");
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
        BalfLafManager.showAlert(editor, "Cannot redo.", "Error");
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
        setTextProperly(FileHelperFunctions.readFileAsString(file.getAbsolutePath()));
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            mainSyntax.setCaretPosition(0);
            scrollPane.getVerticalScrollBar().setValue(0);
          }
        });
        recents.add(file.getAbsolutePath());
        try {
          ZPEEditor.storeRecentFiles(recents, "sqarl/");
          recents = ZPEEditor.getRecentFiles("sqarl/");
          updateRecentFiles();
        } catch (IOException ex) {
          ZPE.log(ex.getMessage());
        }
        editor.setTitle("ZPE Editor " + file.getAbsolutePath());
      } catch (IOException e) {
        BalfLafManager.showAlert(editor, "The file could not be opened.", "Error");
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
        FileHelperFunctions.writeFile(file.getAbsolutePath() + "." + extension, mainSyntax.getText(), false);
        lastFileOpened = file.getAbsolutePath();
      } catch (IOException e) {
        BalfLafManager.showAlert(editor, "The file could not be saved.", "Error");
      }
    }
  }

  private void showAbout() {

    String msg = "";
    msg += "SQARL Language Runtime";
    msg += "SQARL Runtime powered by ZPE copyright Jamie Balfour 2020 - " + ZPE.VERSION_DATE + "\n\n";

    msg += "Powered by Zenith Parsing Engine version " +
            ZenithParsingEngine.VERSION;
    msg += "\n\nFor more information visit\n" +
            "https://www.jamiebalfour.scot/projects/zpe/";

    final BalfButton btnVisitWebsiteButton =
            new BalfButton("More", 15);

    btnVisitWebsiteButton.addActionListener(e -> {

      try {

        HelperFunctions.openWebsite(
                "https://www.jamiebalfour.scot/projects/zpe/"
        );

      } catch (Exception ex) {

        BalfLafManager.showAlert(this,"Could not open the ZPE website", "Failure");
      }
    });

    JDialog dlg = new JDialog(
            _frame,
            "About ZPE",
            true // modal
    );

    dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

    dlg = new BalfAboutDialog(_frame, "SQARL Language Runtime", msg, btnVisitWebsiteButton, lighterLogoFull, themeColor, Color.white);
    dlg.setLocationRelativeTo(_frame);
    dlg.setVisible(true);
  }

  public void saveGUISettings(Properties props) {
    if (propertiesChanged) {
      File f = new File(ZPEKit.getInstallPath() + "/sqarl/");
      if (!f.exists()) {
        if(!f.mkdirs()){
          ZPE.log(f + " could not be created.");
        }
      }
      OutputStream output;
      String path = ZPEKit.getInstallPath() + "/sqarl/" + "gui.properties";

      try {
        output = new FileOutputStream(path);
        // save properties to project root folder
        props.store(output, null);
      } catch (Exception e) {
        ZPE.log("SQARL Runtime error: GUI cannot save" + e.getMessage());
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
    mainSyntax.setText(text);
    dontUndo = false;
    //mainSyntax.setCaretPosition(0);
  }

  @Override
  public void destroyConsole() {
    this.AttachedConsole = null;

  }

  @Override
  public Properties getProperties() {
    return this.mainProperties;
  }

  private JButton createZeroButton() {
    JButton button = new JButton();
    Dimension zeroDim = new Dimension(0, 0);
    button.setPreferredSize(zeroDim);
    button.setMinimumSize(zeroDim);
    button.setMaximumSize(zeroDim);
    return button;
  }

  private void resetScroll(){
    int caretPosition = mainSyntax.getCaretPosition();
    int scrollPosition = scrollPane.getVerticalScrollBar().getValue();
    mainSyntax.setText(mainSyntax.getText());
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        mainSyntax.requestFocus();
        mainSyntax.setCaretPosition(caretPosition);
        scrollPane.getVerticalScrollBar().setValue(scrollPosition);
      }
    });
  }

  private void switchOnDarkMode() {

    mnDarkModeMenuItem.setSelected(true);

    BalfLafManager.getInstance().toggleDarkMode(true);
    Color dark = Color.decode("#282D37");
    scrollPane.setDarkColour(dark);
    mainSyntax.setBackground(dark);
    mainSyntax.setForeground(Color.white);
    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Normal, Color.white);
    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Quote, new Color(152, 195, 119));
    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Keyword, new Color(198, 120, 222));
    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Function, new Color(97, 172, 231));
    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Var, new Color(224, 108, 117));
    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Type, new Color(105, 143, 163));
    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Bool, new Color(208, 154, 102));
    mainSyntax.setCaretColor(Color.white);
    resetScroll();




    darkMode = true;
  }

  private void switchOffDarkMode() {

    mnDarkModeMenuItem.setSelected(false);

    BalfLafManager.getInstance().toggleDarkMode(false);

    Color light = new Color(255, 255, 255);
    mainSyntax.setBackground(light);
    mainSyntax.setForeground(Color.black);

    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Normal,
            Color.black);

    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Quote,
            new Color(152, 195, 121)); // Soft green

    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Keyword,
            new Color(198, 120, 221)); // IntelliJ-style purple

    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Var,
            new Color(224, 108, 117)); // Soft coral/red

    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Type,
            new Color(97, 175, 239)); // VS Code blue

    mainSyntax.setCaretColor(Color.black);
    resetScroll();

    darkMode = false;
  }

  private void updateRecentFiles(){
    mntmRecentMenuItem.removeAll();
    for(String fStr : recents){
      BalfMenuBar.MenuItem item = new BalfMenuBar.MenuItem(new File(fStr).getName(), menuBar);
      item.addActionListener(e -> {
        try {
          clearUndoRedoManagers();
          setTextProperly(FileHelperFunctions.readFileAsString(new File(fStr).getAbsolutePath()));
          editor.setTitle("ZPE Editor " + new File(fStr).getAbsolutePath());
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              mainSyntax.setCaretPosition(0);
              scrollPane.getVerticalScrollBar().setValue(0);
            }
          });
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      });
      mntmRecentMenuItem.add(item, 0);
    }
  }

}
