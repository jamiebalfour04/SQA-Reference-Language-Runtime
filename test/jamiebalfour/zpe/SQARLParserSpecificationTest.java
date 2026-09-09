package jamiebalfour.zpe;

import jamiebalfour.generic.JBPair;
import jamiebalfour.zpe.core.IAST;
import jamiebalfour.zpe.core.ZPEKit;
import jamiebalfour.zpe.transpilers.ZPEPythonTranspiler;

import java.nio.file.Files;
import java.nio.file.Path;

/** Regression tests for SQARL's direct IAST frontend. */
public final class SQARLParserSpecificationTest {
  private static int tests;

  private SQARLParserSpecificationTest() {
  }

  public static void main(String[] args) throws Exception {
    emitsIASTWithoutYASS();
    controlFlowAndExpressionsCompile();
    routinesAndRecordsCompile();
    keyboardAverageProgramCompiles();
    typedKeyboardLinearSearchCompiles();
    IASTCanBeTranspiled();
    pythonCanBeWrittenToAFile();
    IASTCanBeUnfolded();
    IASTCanBecomeAStandardCompiledApplication();
    packagedRuntimeCanBeInstalled();
    System.out.println("Passed " + tests + " direct SQARL IAST tests.");
  }

  private static void emitsIASTWithoutYASS() throws Exception {
    assertProgram(SQARLParser.compileSQARL(
        "DECLARE count AS INTEGER INITIALLY 1\n" +
        "SET count TO count + 1\n" +
        "SEND count TO DISPLAY\n"));
  }

  private static void controlFlowAndExpressionsCompile() throws Exception {
    assertProgram(SQARLParser.compileSQARL(
        "DECLARE count AS INTEGER INITIALLY 0\n" +
        "WHILE count < 3 DO\n" +
        "SET count TO count + 1\n" +
        "END WHILE\n" +
        "IF count = 3 THEN\n" +
        "SEND \"done\" TO DISPLAY\n" +
        "ELSE\n" +
        "SEND \"failed\" TO DISPLAY\n" +
        "END IF\n"));
  }

  private static void routinesAndRecordsCompile() throws Exception {
    assertProgram(SQARLParser.compileSQARL(
        "RECORD Result IS { INTEGER value, BOOLEAN valid }\n" +
        "FUNCTION double(INTEGER value) RETURNS INTEGER\n" +
        "RETURN value * 2\n" +
        "END FUNCTION\n" +
        "PROCEDURE main()\n" +
        "SEND double(4) TO DISPLAY\n" +
        "END PROCEDURE\n"));
  }

  private static void keyboardAverageProgramCompiles() throws Exception {
    assertProgram(SQARLParser.compileSQARL(
        "RECORD pupil IS {STRING name, INTEGER age}\n" +
        "DECLARE total INITIALLY 0\n" +
        "DECLARE counter INITIALLY 0\n" +
        "DECLARE nextInput INITIALLY 0\n" +
        "WHILE counter < 10 DO\n" +
        "  SEND \"Insert a number\" TO DISPLAY\n" +
        "  RECEIVE nextInput FROM KEYBOARD\n" +
        "  SET total TO total + nextInput\n" +
        "  SET counter TO counter + 1\n" +
        "END WHILE\n" +
        "SEND total / 10.0 TO DISPLAY\n"));
  }

  private static void typedKeyboardLinearSearchCompiles() throws Exception {
    assertProgram(SQARLParser.compileSQARL(
        "DECLARE nation AS ARRAY OF STRING INITIALLY [\"Scotland\", \"Wales\", \"England\", \"Ireland\", \"France\", \"Spain\", \"Italy\", \"Germany\", \"Norway\", \"Sweden\"]\n" +
        "SET counter TO 0\n" +
        "SET item_found TO FALSE\n" +
        "RECEIVE desired_item FROM (STRING) KEYBOARD\n" +
        "REPEAT\n" +
        "  IF desired_item = nation[counter] THEN\n" +
        "    SEND \"The program found  \" & desired_item & \" at position \" & counter & \" of the nation array.\" TO DISPLAY\n" +
        "    SET item_found TO TRUE\n" +
        "  END IF\n" +
        "  SET counter TO counter + 1\n" +
        "UNTIL counter = 10\n" +
        "IF item_found = FALSE THEN\n" +
        "  SEND \"The program did not find a match for \" & desired_item & \" within the nation array.\" TO DISPLAY\n" +
        "END IF\n"));
  }

  private static void IASTCanBeTranspiled() throws Exception {
    String python = ZPEKit.transpileCode(application(), "SQARLApplication", new ZPEPythonTranspiler());
    assertTrue(python.contains("main"), "Python output did not contain main.");
  }

  private static void pythonCanBeWrittenToAFile() throws Exception {
    Path directory = Files.createTempDirectory("sqarl-python-");
    Path output = directory.resolve("application.py");
    SQARLParser.transpilePython(output,
            "PROCEDURE main()\nSEND \"Hello\" TO DISPLAY\nEND PROCEDURE\n");
    String python = Files.readString(output);
    assertTrue(Files.isRegularFile(output) && python.contains("main"),
            "SQARL did not write its Python output file.");
    SQARLParser.transpilePython(output,
            "SET found TO FALSE\nIF found = FALSE THEN\nSET found TO TRUE\nEND IF\n");
    python = Files.readString(output);
    assertTrue(python.contains("False") && python.contains("True")
                    && !python.contains("FALSE") && !python.contains("TRUE"),
            "SQARL boolean literals were not converted to Python booleans.");
  }

  private static void IASTCanBeUnfolded() throws Exception {
    String report = ZPEKit.unfold(application(), false);
    assertTrue(report.contains("FUNCTION: main"), "Unfold did not describe SQARL's IAST.");
  }

  private static void IASTCanBecomeAStandardCompiledApplication() throws Exception {
    Path directory = Files.createTempDirectory("sqarl-compiled-");
    Path output = directory.resolve("application");
    JBPair result = ZPEKit.compile(application(), output.toString(), "SQARL application", "SQARL");
    assertTrue(Integer.valueOf(0).equals(result.getName()), "ZPE compilation failed with status " + result.getName());
    assertTrue(Files.isRegularFile(directory.resolve("application.yex")), "ZPE did not write the .yex file.");
  }

  private static void packagedRuntimeCanBeInstalled() throws Exception {
    Path directory = Files.createTempDirectory("sqarl-install-");
    Path source = directory.resolve("source.jar");
    Files.writeString(source, "sqarl-runtime");
    Path settings = directory.resolve("zpe/sqarl/gui.properties");
    Files.createDirectories(settings.getParent());
    Files.writeString(settings, "theme=system");

    Path installed = SQARLInstaller.install(source, directory.resolve("zpe"));
    assertTrue(installed.equals(directory.resolve("zpe/sqarl/sqarl-runtime.jar")),
            "SQARL was installed to the wrong directory.");
    assertTrue("sqarl-runtime".equals(Files.readString(installed)),
            "The installed SQARL JAR did not match its source.");
    assertTrue("theme=system".equals(Files.readString(settings)),
            "Installing SQARL replaced its existing settings.");
    Path unixCommand = SQARLInstaller.writeCommand(installed, directory.resolve("bin"), false);
    assertTrue(unixCommand.getFileName().toString().equals("sqarl")
                    && Files.readString(unixCommand).contains(installed.toAbsolutePath().toString()),
            "The Unix sqarl command was not created correctly.");
    Path windowsCommand = SQARLInstaller.writeCommand(installed, directory.resolve("bin"), true);
    assertTrue(windowsCommand.getFileName().toString().equals("sqarl.cmd")
                    && Files.readString(windowsCommand).contains(installed.toAbsolutePath().toString()),
            "The Windows sqarl command was not created correctly.");
  }

  private static IAST application() throws Exception {
    return SQARLParser.compileSQARL(
        "PROCEDURE main()\n" +
        "SEND \"Hello from SQARL\" TO DISPLAY\n" +
        "END PROCEDURE\n");
  }

  private static void assertProgram(IAST program) {
    assertTrue(program != null && program.next != null, "SQARL did not emit a program IAST.");
  }

  private static void assertTrue(boolean condition, String message) {
    tests++;
    if (!condition) throw new AssertionError(message);
  }
}
