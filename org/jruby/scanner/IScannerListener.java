package org.jruby.scanner;

public interface IScannerListener {
    public void scannerError(int line, int col, String message);
    public void scannerWarning(int line, int col, String message);
}