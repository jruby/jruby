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
    
    public static final boolean startsWith(ICharacterScanner cs, char[] chars) {
    	int len = chars.length;
    	int i = 0;
    	
    	for (;i < len && cs.readChar() != chars[i]; i++);
    	
    	for (int j = 0; j <= i; j++) {
    		cs.unreadChar();
    	}
    	
    	return i == len;
    }

    public static final boolean startsWithWord(ICharacterScanner cs, char[] chars) {
    	int len = chars.length;
    	int i = 0;
    	
    	for (;i < len && cs.readChar() != chars[i]; i++);
    	
    	boolean result = i == len;
    	
    	if (!cs.isEol()) {
    		if (!Character.isWhitespace(cs.readChar())) {
    			result = false;
    		}
    		cs.unreadChar();
    	}
    	    	
    	for (int j = 0; j <= i; j++) {
    		cs.unreadChar();
    	}
    	
    	return result;
    }
    
    public static void skip(ICharacterScanner cs, int chars) {
    	for (int i = 0; i < chars && !cs.isEof(); i++) {
    		cs.readChar();
    	}
    }
}