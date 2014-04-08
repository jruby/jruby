package org.jruby.runtime.profile.builtin;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;


public class ProfileOutput {
  private final PrintStream stream;

  private boolean headerPrinted = false;

  
  public ProfileOutput(PrintStream out) {
    this.stream = out;
  }

  public ProfileOutput(File out) throws FileNotFoundException {
    this.stream = new PrintStream(new FileOutputStream(out));
  }

  public void printProfile(ProfilePrinter printer) {
    if (headerPrinted) {
      printer.printProfile(stream, false);
    } else {
      printer.printHeader(stream);
      printer.printProfile(stream, true);
      headerPrinted = true;
      footerAndCleanupOnShutdown(printer);
    }
  }

  private void footerAndCleanupOnShutdown(final ProfilePrinter printer) {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        printer.printFooter(stream);
        stream.close();
      }
    });
  }
}