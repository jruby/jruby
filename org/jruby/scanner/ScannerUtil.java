package org.jruby.scanner;

public final class ScannerUtil {
	public static final boolean isWhiteSpace(ICharacterScanner cs) {
		char c = cs.readChar();
		cs.unreadChar();
		return Character.isWhitespace(c);
	}

	public static final void skipLine(ICharacterScanner cs) {
		while (!cs.isEol()) {
			cs.readChar();
		}
	}
	
	public static final String getLine(ICharacterScanner cs) {
		StringBuffer sb = new StringBuffer(50);
		while (!cs.isEol()) {
			sb.append(cs.readChar());
		}
		return sb.toString();
	}
}