package org.jruby.scanner;

public interface IScannerEventListener {
	public void scannerException(int line, int col, String message);
}