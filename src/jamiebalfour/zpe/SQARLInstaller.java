package jamiebalfour.zpe;

import jamiebalfour.zpe.core.ZPEInstance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;

/** Installs the packaged SQARL runtime and its command-line launcher. */
final class SQARLInstaller {
  private SQARLInstaller() { }

  static void install() throws Exception {
    Path source = Path.of(SQARLParser.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    if (!Files.isRegularFile(source) || !source.getFileName().toString().toLowerCase().endsWith(".jar")) {
      throw new IOException("SQARL --install must be run from the packaged sqarl-runtime.jar.");
    }
    Path installed = install(source, Path.of(ZPEInstance.getInstallPath()));
    System.out.println("Installed SQARL to " + installed + ".");
    Path launcher = installCommand(installed);
    System.out.println("Installed SQARL command to " + launcher + ". Open a new terminal and run: sqarl");
  }

  static Path install(Path source, Path zpeDirectory) throws IOException {
    Path installation = zpeDirectory.resolve("sqarl");
    Path destination = installation.resolve("sqarl-runtime.jar");
    Files.createDirectories(installation);
    if (Files.exists(destination) && Files.isSameFile(source, destination)) return destination;
    Path temporary = Files.createTempFile(installation, "sqarl-runtime-", ".tmp");
    try {
      Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
      try {
        Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException exception) {
        Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
    return destination;
  }

  private static Path installCommand(Path jar) throws IOException {
    boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
    String path = System.getenv("PATH");
    if (path == null || path.isBlank()) throw new IOException("PATH is empty; cannot install the sqarl command.");
    String separator = windows ? ";" : ":";
    List<Path> candidates = new ArrayList<>();
    for (String entry : path.split(java.util.regex.Pattern.quote(separator))) {
      String value = entry.trim();
      if (windows && value.length() > 1 && value.startsWith("\"") && value.endsWith("\"")) {
        value = value.substring(1, value.length() - 1);
      }
      if (!value.isEmpty()) {
        try { candidates.add(Path.of(value)); } catch (Exception ignored) { }
      }
    }
    candidates.sort((left, right) -> Boolean.compare(!preferredCommandDirectory(left, windows), !preferredCommandDirectory(right, windows)));
    for (Path candidate : candidates) {
      if (Files.isDirectory(candidate) && Files.isWritable(candidate)) return writeCommand(jar, candidate, windows);
    }
    throw new IOException("No writable directory on PATH; add a user-writable bin directory and retry.");
  }

  private static boolean preferredCommandDirectory(Path directory, boolean windows) {
    String value = directory.toString().toLowerCase();
    return windows ? value.contains("windows")
            : value.equals("/usr/local/bin") || value.equals("/opt/homebrew/bin") || value.equals("/usr/bin");
  }

  static Path writeCommand(Path jar, Path directory, boolean windows) throws IOException {
    Files.createDirectories(directory);
    Path launcher = directory.resolve(windows ? "sqarl.cmd" : "sqarl");
    String jarPath = jar.toAbsolutePath().normalize().toString();
    String contents = windows
            ? "@echo off\r\njava -Xmx2048M -jar \"" + jarPath.replace("\"", "\"\"") + "\" %*\r\n"
            : "#!/bin/sh\nexec java -Xmx2048M -jar '" + jarPath.replace("'", "'\"'\"'") + "' \"$@\"\n";
    Path temporary = Files.createTempFile(directory, "sqarl-command-", ".tmp");
    try {
      Files.writeString(temporary, contents, StandardCharsets.UTF_8);
      try {
        Files.move(temporary, launcher, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException exception) {
        Files.move(temporary, launcher, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
    if (!windows) {
      try { Files.setPosixFilePermissions(launcher, PosixFilePermissions.fromString("rwxr-xr-x")); }
      catch (UnsupportedOperationException exception) { launcher.toFile().setExecutable(true, false); }
    }
    return launcher;
  }
}
