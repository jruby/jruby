package org.jruby.scanner;

public interface ICharacterScanner {
	public boolean isEof();
	public boolean isEol();
	
	public char readChar();
	public void unreadChar();

	public int getLine();
	public int getColumn();
}