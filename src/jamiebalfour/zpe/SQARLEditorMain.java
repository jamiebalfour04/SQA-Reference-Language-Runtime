package jamiebalfour.zpe;

import jamiebalfour.HelperFunctions;
import jamiebalfour.ui.BorderedRoot;
import jamiebalfour.ui.JBUI;
import jamiebalfour.zpe.core.RunningInstance;
import jamiebalfour.zpe.core.ZPE;
import jamiebalfour.zpe.core.ZPEHelperFunctions;
import jamiebalfour.zpe.core.ZPEKit;
import jamiebalfour.zpe.editor.CodeEditorView;
import jamiebalfour.zpe.editor.ZPEEditor;
import jamiebalfour.zpe.editor.ZPEEditorConsole;
import jamiebalfour.zpe.exceptions.CompileException;
import jamiebalfour.zpe.interfaces.GenericEditor;
import jamiebalfour.zpe.os.macos.macOS;
import jamiebalfour.zpe.types.ZPEString;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

class SQARLEditorMain extends JFrame implements GenericEditor {

  SQARLEditorMain _this = this;


  CodeEditorView mainSyntax;
  protected UndoHandler undoHandler = new UndoHandler();
  protected UndoManager undoManager = new UndoManager();

  ZPEEditorConsole AttachedConsole;
  Process currentProcess;

  private final jamiebalfour.ui.CustomTitleBar _titleBar;


  boolean propertiesChanged = false;

  Properties mainProperties;

  private final UndoAction undoAction;
  private final RedoAction redoAction;
  JEditorPane contentEditor;
  JScrollPane scrollPane;
  JCheckBoxMenuItem mnDarkModeMenuItem;
  JMenuItem mntmRecentMenuItem;

  String lastFileOpened = "";
  private boolean darkMode = false;
  JMenuItem mntmStopCodeMenuItem;

  BorderedRoot borderedRoot;

  JMenuItem mntmClearConsoleBeforeRunMenuItem;


  static FileNameExtensionFilter filter1 = new FileNameExtensionFilter("Text files (*.txt)", "txt");
  static FileNameExtensionFilter filter2 = new FileNameExtensionFilter("YASS Executable files (*.yex)", "yex");
  private final JFrame editor;

  JCheckBoxMenuItem chckbxmntmCaseSensitiveCompileCheckItem;
  ArrayList<String> recents = ZPEEditor.getRecentFiles("sqarl/");

  ImageIcon lighterLogo;
  ImageIcon lighterLogoFull;

  boolean dontUndo = true;

  Color borderColor = new Color(40, 75, 99);

  boolean isMaximised = false;


  private void maximiseButtonClicked() {
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
  }

  public void maximiseToCurrentScreen(JFrame frame) {
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
  }

  public SQARLEditorMain() {

    this.setLayout(new BorderLayout());

    borderedRoot = new BorderedRoot(this, borderColor, 3, 0);



    _titleBar = JBUI.generateCustomTitleBar(this, 0);
    _titleBar.setLabelText("Untitled");
    _titleBar.setMaximiseButtonListener(e -> {
      maximiseButtonClicked();
    });
    _titleBar.setCloseListener(e -> {
      closeUp();
      System.exit(0);

    });

    JPanel topContainer = new JPanel();
    topContainer.setOpaque(false);
    topContainer.setLayout(new BorderLayout());
    topContainer.add(_titleBar, BorderLayout.NORTH);

    setBackground(borderColor);

    getContentPane().add(topContainer, BorderLayout.NORTH);


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
    SQARL_KEYWORDS.put("IS", CodeEditorView.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("AS", CodeEditorView.DEFAULT_KEYWORD);
    SQARL_KEYWORDS.put("ELSE", CodeEditorView.DEFAULT_KEYWORD);

    mainSyntax = new CodeEditorView(SQARL_KEYWORDS, "\"'", "");

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {

        int confirmed = JOptionPane.showConfirmDialog(editor, "Are you sure you want to exit the program?", "Exit Program Message Box", JOptionPane.YES_NO_OPTION);
        if (confirmed == JOptionPane.YES_OPTION) {
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
        editor.setExtendedState(JFrame.MAXIMIZED_BOTH);
      }
    }


    this.setSize(new Dimension(600, 400));

    JPanel mainPanel = new JPanel();
    getContentPane().add(mainPanel, BorderLayout.CENTER);
    mainPanel.setLayout(new BorderLayout(0, 0));

    scrollPane = new JScrollPane();
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    scrollPane.setEnabled(false);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.setBackground(Color.WHITE);
    mainPanel.add(scrollPane, BorderLayout.CENTER);
    mainPanel.setBorder(new LineBorder(Color.black, 3));
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);


    contentEditor = (JEditorPane) mainSyntax.getEditPane();
    contentEditor.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));

    contentEditor.setFont(new Font("Monospaced", Font.PLAIN, 18));

    contentEditor.setText("RECORD pupil IS {STRING name, INTEGER age}\r\n" +
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
    scrollPane.setViewportView(contentEditor);

    scrollPane.setRowHeaderView(mainSyntax.getEditor());

    // === Footer setup ===
    JLabel footerLabel = JBUI.generateJBFooter(("<html>&copy; J Balfour 2019 - 2025</html>"));
    getContentPane().add(footerLabel, BorderLayout.SOUTH);


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
        macOS a = new macOS();
        a.addAboutDialog(this::showAbout);
      } catch (Exception e) {
        //Don't do anything
      }
    }

    JMenuBar menuBar = new JMenuBar();

    setJMenuBar(menuBar);

    int modifier = InputEvent.CTRL_DOWN_MASK;
    if (HelperFunctions.isMac()) {
      modifier = InputEvent.META_DOWN_MASK;
    }

    this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    JMenu mnFileMenu = new JMenu("File");
    mnFileMenu.setMnemonic('F');
    menuBar.add(mnFileMenu);


    JMenuItem mntmNewMenuItem = new JMenuItem("New");
    mntmNewMenuItem.setAccelerator(KeyStroke.getKeyStroke('N', modifier));
    mntmNewMenuItem.addActionListener(e -> {
      clearUndoRedoManagers();
      setTextProperly("");
    });
    mnFileMenu.add(mntmNewMenuItem);

    JMenuItem mntmSaveMenuItem = new JMenuItem("Save");
    mntmSaveMenuItem.setAccelerator(KeyStroke.getKeyStroke('S', modifier));
    mntmSaveMenuItem.addActionListener(e -> {
      if (lastFileOpened.isEmpty()) {
        saveAsDialog();
      } else {
        try {
          HelperFunctions.writeFile(lastFileOpened, contentEditor.getText(), false);
        } catch (IOException ex) {
          ZPE.log("SQARL Runtime error: " + ex.getMessage());
        }
      }
    });
    mnFileMenu.add(mntmSaveMenuItem);

    JMenuItem mntmSaveAsMenuItem = new JMenuItem("Save As");
    mntmSaveAsMenuItem.addActionListener(e -> saveAsDialog());
    mnFileMenu.add(mntmSaveAsMenuItem);

    mnFileMenu.add(new JSeparator());

    JMenuItem mntmOpenMenuItem = new JMenuItem("Open");
    mntmOpenMenuItem.setAccelerator(KeyStroke.getKeyStroke('O', modifier));
    mntmOpenMenuItem.addActionListener(e -> open());
    mnFileMenu.add(mntmOpenMenuItem);

    mntmRecentMenuItem = new JMenu("Recent files");

    updateRecentFiles();

    if(!recents.isEmpty()) {
      mnFileMenu.add(mntmRecentMenuItem);
    }


    mnFileMenu.add(new JSeparator());

    JMenuItem mntmPrintMenuItem = new JMenuItem("Print");
    mntmPrintMenuItem.setAccelerator(KeyStroke.getKeyStroke('P', modifier));
    mntmPrintMenuItem.addActionListener(e -> {
      try {
        contentEditor.print();
      } catch (PrinterException e1) {
        JOptionPane.showMessageDialog(editor, "An error was encountered whilst trying to print.", "Error",
                JOptionPane.ERROR_MESSAGE);
      }
    });
    mnFileMenu.add(mntmPrintMenuItem);

    mnFileMenu.add(new JSeparator());

    JMenuItem mntmExitMenuItem = new JMenuItem("Exit");
    mntmExitMenuItem.addActionListener(e -> {
      closeUp();
      System.exit(0);
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
    mntmCutMenuItem.addActionListener(e -> contentEditor.cut());
    mnEditMenu.add(mntmCutMenuItem);

    JMenuItem mntmCopyMenuItem = new JMenuItem("Copy");
    mntmCopyMenuItem.setAccelerator(KeyStroke.getKeyStroke('C', modifier));
    mntmCopyMenuItem.addActionListener(e -> contentEditor.copy());
    mnEditMenu.add(mntmCopyMenuItem);

    JMenuItem mntmPasteMenuItem = new JMenuItem("Paste");
    mntmPasteMenuItem.setAccelerator(KeyStroke.getKeyStroke('V', modifier));
    mntmPasteMenuItem.addActionListener(e -> contentEditor.paste());
    mnEditMenu.add(mntmPasteMenuItem);

    JMenuItem mntmDeleteMenuItem = new JMenuItem("Delete");
    mntmDeleteMenuItem.addActionListener(e -> {
      int start = contentEditor.getSelectionStart();
      int end = contentEditor.getSelectionEnd();

      String current = contentEditor.getText();

      String newText = current.substring(0, start) + current.substring(end);
      contentEditor.setText(newText);

    });
    mnEditMenu.add(mntmDeleteMenuItem);

    mnEditMenu.add(new JSeparator());

    JMenuItem mntmSelectAllMenuItem = new JMenuItem("Select All");
    mntmSelectAllMenuItem.setAccelerator(KeyStroke.getKeyStroke('A', modifier));
    mntmSelectAllMenuItem.addActionListener(e -> contentEditor.selectAll());

    mnEditMenu.add(mntmSelectAllMenuItem);

    JMenu mnViewMenu = new JMenu("View");
    mnEditMenu.setMnemonic('V');
    menuBar.add(mnViewMenu);

    mnDarkModeMenuItem = new JCheckBoxMenuItem("Dark Mode");
    mnViewMenu.add(mnDarkModeMenuItem);
    mnDarkModeMenuItem.addActionListener(e -> {
      if (!darkMode) {
        switchOnDarkMode();
      } else {
        switchOffDarkMode();
      }

      setProperty("DARK_MODE", "" + darkMode);
      saveGUISettings(mainProperties);

      updateEditor();
    });



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
    mntmRunCodeMenuItem.addActionListener(e -> {
      /*
       * running = new ConsoleThread(); running.startConsole(contentEditor.getText(),
       * runtimeArgs, !chckbxmntmCaseSensitiveCompileCheckItem.isSelected());
       */
      /*if (AttachedConsole == null) {
        AttachedConsole = new ZPEEditorConsole(_this, "", new Font("Consolas", Font.PLAIN, 18), 5);
      }





      AttachedConsole.runCode(yass, new ZPEString[0], chckbxmntmCaseSensitiveCompileCheckItem.isSelected());*/

      try{
        String extras = "";
        if (chckbxmntmCaseSensitiveCompileCheckItem.isSelected()) {
          extras += " --case_insensitive";
        }
        SQARLParser sqarl = new SQARLParser();
        String yass = sqarl.parseToYASS(contentEditor.getText());
        HelperFunctions.writeFile(RunningInstance.getInstallPath() + "/tmp.yas", yass, false);
        if (!RunningInstance.getJarExecPath().isEmpty()) {
          if (new File(RunningInstance.getJarExecPath()).exists()) {
            currentProcess = Runtime.getRuntime().exec("java -jar " + RunningInstance.getJarExecPath() + " -g " + RunningInstance.getInstallPath() + "/tmp.yas --console" + extras);
            mntmStopCodeMenuItem.setEnabled(true);
            mntmStopCodeMenuItem.setVisible(true);
          }
        } else {

          AttachedConsole = new ZPEEditorConsole(this, "", this.contentEditor.getFont(), 5);
          AttachedConsole.runCode(yass, new ZPEString[0], this.chckbxmntmCaseSensitiveCompileCheckItem.isSelected());
          mntmStopCodeMenuItem.setVisible(false);
        }
      } catch(IOException ex){
        //Do nothing
      }


    });
    mnScriptMenu.add(mntmRunCodeMenuItem);


    mntmStopCodeMenuItem = new JMenuItem("Stop code");
    mntmStopCodeMenuItem.setEnabled(false);
    mntmStopCodeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
    mntmStopCodeMenuItem.addActionListener(e -> {
      currentProcess.destroy();
      currentProcess = null;
      mntmStopCodeMenuItem.setEnabled(false);
    });

    mnScriptMenu.add(mntmStopCodeMenuItem);

    mnScriptMenu.add(new JSeparator());

    JMenuItem mntmCompileCodeMenuItem = getjMenuItem();
    mnScriptMenu.add(mntmCompileCodeMenuItem);

    JMenuItem mntmTranspileCodeMenuItem = new JMenuItem("Transpile code to Python");
    mntmTranspileCodeMenuItem.addActionListener(e -> {
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


        SQARLParser sqarl = new SQARLParser();
        String yass = sqarl.parseToYASS(contentEditor.getText());
        PythonTranspiler t = new PythonTranspiler();
        String code = t.Transpile(ZPEKit.compile(yass), "");
        String path1 = file.getPath();
        if(!path1.endsWith(extension)){
          path1 = path1 + extension;
        }
        HelperFunctions.writeFile(path1, code, false);

        JOptionPane.showMessageDialog(editor,
                "Python transpile success. The file has been successfully compiled to " + path1 + ".",
                "Python transpiler", JOptionPane.WARNING_MESSAGE);

      } catch(Exception ex){
        System.out.println(ex.getMessage());
      }
    });
    mnScriptMenu.add(mntmTranspileCodeMenuItem);

    mnScriptMenu.add(new JSeparator());

    JMenuItem mntmAnalyseCodeMenuItem = new JMenuItem("Analyse code");
    mntmAnalyseCodeMenuItem.addActionListener(e -> {


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
      } catch (CompileException ex) {
        JOptionPane.showMessageDialog(editor, "Code is invalid", "Code analysis",
                JOptionPane.INFORMATION_MESSAGE);
      }

    });

    mnScriptMenu.add(mntmAnalyseCodeMenuItem);


    JMenuItem mntmToByteCodeFileMenuItem = new JMenuItem("Compile to byte codes");
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

          SQARLParser sqarl = new SQARLParser();
          String yass = sqarl.parseToYASS(contentEditor.getText());

          StringBuilder text = new StringBuilder();
          for (byte s : ZPEKit.parseToBytes(yass)) {
            text.append(s).append(" ");
          }
          HelperFunctions.writeFile(file.getAbsolutePath() + "." + extension, text.toString(), false);
        } catch (IOException ex) {
          JOptionPane.showMessageDialog(editor, "The file could not be saved.", "Error",
                  JOptionPane.ERROR_MESSAGE);
        }
      }

    });

    mnScriptMenu.add(mntmToByteCodeFileMenuItem);

    JMenuItem mntmUnfoldCodeMenuItem = new JMenuItem("Unfold (explain) code");
    mntmUnfoldCodeMenuItem.addActionListener(e -> {

      String result;
      try {
        SQARLParser sqarl = new SQARLParser();
        String yass = sqarl.parseToYASS(contentEditor.getText());
        result = ZPEKit.unfold(yass, false);
        JOptionPane.showMessageDialog(editor, ZPEHelperFunctions.smartSplit(result, 100), "Code Explanation",
                JOptionPane.INFORMATION_MESSAGE);

      } catch (CompileException ex) {
        throw new RuntimeException(ex);
      }

    });

    mnScriptMenu.add(mntmUnfoldCodeMenuItem);

    //Help menu
    JMenu mnHelpMenu = new JMenu("Help");
    mnHelpMenu.setMnemonic('H');
    menuBar.add(mnHelpMenu);

    if (!HelperFunctions.isMac()) {
      JMenuItem mntmAboutFileMenuItem = new JMenuItem("About");
      mntmAboutFileMenuItem.addActionListener(e -> showAbout());
      mnHelpMenu.add(mntmAboutFileMenuItem);
      mnHelpMenu.add(new JSeparator());
    }

    JMenuItem mntmSQARLSpecificationWebsiteMenuItem = new JMenuItem("Read the SQARL Specification");
    mntmSQARLSpecificationWebsiteMenuItem.addActionListener(e -> {try{
      HelperFunctions.openWebsite("https://www.sqa.org.uk/sqa/files_ccc/Reference-language-for-Computing-Science-Sep2016.pdf");
    } catch (Exception ex){
      JOptionPane.showMessageDialog(editor, "Could not open SQA website", "Failure", JOptionPane.ERROR_MESSAGE);
    }});

    mnHelpMenu.add(mntmSQARLSpecificationWebsiteMenuItem);

    JMenuItem mntmSQAWebsiteMenuItem = new JMenuItem("Visit SQA Website");

      mntmSQAWebsiteMenuItem.addActionListener(e -> {try {
        HelperFunctions.openWebsite("https://www.sqa.org.uk/sqa/48486.html");
      } catch (Exception ex){
        JOptionPane.showMessageDialog(editor, "Could not open SQA website", "Failure", JOptionPane.ERROR_MESSAGE);
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

    RunningInstance.setErrorLevel(1);

    if (mainProperties.containsKey("DARK_MODE")) {
      if (mainProperties.get("DARK_MODE").equals("true")) {
        switchOnDarkMode();
      } else {
        switchOffDarkMode();
      }
    }

  }

  private void closeUp(){

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

  private JMenuItem getjMenuItem() {
    JMenuItem mntmCompileCodeMenuItem = new JMenuItem("Compile code");
    mntmCompileCodeMenuItem.addActionListener(e -> {
      String name = JOptionPane.showInputDialog(editor,
              "Please insert the name of the compiled application.");
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
        ZPEKit.compile(yass, file.toString() + "." + extension, name, "",
                !chckbxmntmCaseSensitiveCompileCheckItem.isSelected(), false, null, null);

        JOptionPane.showMessageDialog(editor,
                "YASS compile success. The file has been successfully compiled to " + file + ".",
                "YASS compiler", JOptionPane.WARNING_MESSAGE);

      } catch (IOException ex) {
        JOptionPane.showMessageDialog(editor,
                "YASS compile failure. The YASS compiler could not compile the code given due to an IOException.",
                "YASS compiler", JOptionPane.ERROR_MESSAGE);
      } catch (CompileException ex) {
        JOptionPane.showMessageDialog(editor,
                "YASS compile failure. The YASS compiler could not compile the code given. The error was: " + ex.getMessage(),
                "YASS compiler", JOptionPane.ERROR_MESSAGE);
      }
    });
    return mntmCompileCodeMenuItem;
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
        setTextProperly(HelperFunctions.readFileAsString(file.getAbsolutePath()));
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            contentEditor.setCaretPosition(0);
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
        HelperFunctions.writeFile(file.getAbsolutePath() + "." + extension, contentEditor.getText(), false);
        lastFileOpened = file.getAbsolutePath();
      } catch (IOException e) {
        JOptionPane.showMessageDialog(editor, "The file could not be saved.", "Error",
                JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void showAbout() {

    JOptionPane op = new JOptionPane(new SQARLAboutDialog().getContentPane(), JOptionPane.PLAIN_MESSAGE,
            JOptionPane.DEFAULT_OPTION, null, new String[]{});

    JDialog dlg = op.createDialog(editor, "About SQA Reference Language Runtime");

    dlg.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);

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
    contentEditor.setText(text);
    dontUndo = false;
    //contentEditor.setCaretPosition(0);
  }

  @Override
  public void destroyConsole() {
    this.AttachedConsole = null;

  }

  @Override
  public Properties getProperties() {
    return this.mainProperties;
  }

  private void updateEditor() {

    contentEditor = (JEditorPane) mainSyntax.getEditPane();
    setTextProperly(contentEditor.getText());

  }

  private void setUpScrollBar(String trackColour, String thumbColour) {
    scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
      @Override
      protected void configureScrollBarColors() {
        this.thumbColor = Color.decode(thumbColour);
        this.trackColor = Color.decode(trackColour);
        this.scrollBarWidth = 7;
      }

      @Override
      protected JButton createDecreaseButton(int orientation) {
        return createZeroButton();
      }

      @Override
      protected JButton createIncreaseButton(int orientation) {
        return createZeroButton();
      }

      protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        // your code
        Graphics2D g2 = (Graphics2D) g.create();

        // Enable anti-aliasing for smooth edges
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Set the color of the thumb
        g2.setColor(thumbColor);

        // Set the thumb width and height
        int arc = 10; // This value sets the roundness of the corners. Increase or decrease it as needed.

        // Draw a rounded rectangle
        g2.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, arc, arc);

        // Dispose of the graphics context
        g2.dispose();
      }
    });
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
    int caretPosition = contentEditor.getCaretPosition();
    int scrollPosition = scrollPane.getVerticalScrollBar().getValue();
    contentEditor.setText(contentEditor.getText());
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        contentEditor.requestFocus();
        contentEditor.setCaretPosition(caretPosition);
        scrollPane.getVerticalScrollBar().setValue(scrollPosition);
      }
    });
  }

  private void switchOnDarkMode() {

    mnDarkModeMenuItem.setSelected(true);

    Color dark = Color.decode("#282D37");
    scrollPane.setBackground(dark);
    contentEditor.setBackground(dark);
    contentEditor.setForeground(Color.white);
    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Normal, Color.white);
    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Quote, new Color(152, 195, 119));
    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Keyword, new Color(198, 120, 222));
    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Function, new Color(97, 172, 231));
    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Var, new Color(224, 108, 117));
    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Type, new Color(105, 143, 163));
    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Bool, new Color(208, 154, 102));
    contentEditor.setCaretColor(Color.white);
    resetScroll();
    setUpScrollBar("#282D37", "#444444");

    darkMode = true;
  }

  private void switchOffDarkMode() {

    mnDarkModeMenuItem.setSelected(false);

    Color light = new Color(255, 255, 255);
    contentEditor.setBackground(light);
    contentEditor.setForeground(Color.black);
    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Normal, Color.black);
    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Quote, new Color(0, 128, 0));
    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Keyword, new Color(200, 0, 255));
    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Var, new Color(255, 138, 0));
    mainSyntax.setAttributeColor(CodeEditorView.ATTR_TYPE.Type, new Color(150, 0, 150));
    contentEditor.setCaretColor(Color.black);
    resetScroll();
    setUpScrollBar("#dddddd", "#aaaaaa");

    darkMode = false;
  }

  private void updateRecentFiles(){
    mntmRecentMenuItem.removeAll();
    for(String fStr : recents){
      JMenuItem item = new JMenuItem(new File(fStr).getName());
      item.addActionListener(e -> {
        try {
          clearUndoRedoManagers();
          setTextProperly(HelperFunctions.readFileAsString(new File(fStr).getAbsolutePath()));
          editor.setTitle("ZPE Editor " + new File(fStr).getAbsolutePath());
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              contentEditor.setCaretPosition(0);
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
