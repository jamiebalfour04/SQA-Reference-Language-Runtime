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
    IASTCanBeTranspiled();
    IASTCanBeUnfolded();
    IASTCanBecomeAStandardCompiledApplication();
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

  private static void IASTCanBeTranspiled() throws Exception {
    String python = ZPEKit.transpileCode(application(), "SQARLApplication", new ZPEPythonTranspiler());
    assertTrue(python.contains("main"), "Python output did not contain main.");
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
