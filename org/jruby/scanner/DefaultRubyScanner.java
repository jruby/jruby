package org.jruby.scanner;

public class DefaultRubyScanner implements IRubyScanner {
    private ICharacterScanner characterScanner = null;
    private IScannerEventListener eventListener = null;

    // keywords:
    private static final char[] cmtBegin = { 'e', 'g', 'i', 'n' };
    private static final char[] cmtEnd = { '=', 'e', 'n', 'd' };

    public DefaultRubyScanner(ICharacterScanner characterScanner, IScannerEventListener eventListener) {
        this.characterScanner = characterScanner;
        this.eventListener = eventListener;
    }

    /**
     * @see IRubyScanner#getNextToken()
     */
    public IToken getNextToken() {
        ICharacterScanner cs = characterScanner;

        if (cs.isEof()) {
            return new DefaultToken(TokenTypes.TOKEN_EOF);
        }

        while (!cs.isEof()) {
            switch (cs.readChar()) {
                case '#' :
                    String comment = ScannerUtil.getLine(cs);
                    return new StringToken(TokenTypes.TOKEN_LINE_COMMENT, comment);
                case '=' :
                    char next = cs.readChar();
                    if (cs.getColumn() == 2 && next == 'b' && ScannerUtil.startsWithWord(cs, cmtBegin)) {
                        ScannerUtil.skipLine(cs);
                        cs.skipEol();

                        StringBuffer sb = new StringBuffer(200);

                        while (!ScannerUtil.startsWithWord(cs, cmtEnd)) {
                            sb.append(ScannerUtil.getLine(cs)).append('\n');
                            cs.skipEol();
                        }

                        if (cs.isEof()) {
                            eventListener.scannerException(cs.getLine(), cs.getColumn(), "embedded document meets end of file.");
                        } else {
                            ScannerUtil.skipLine(cs);
                            cs.skipEol();
                        }

                        return new StringToken(TokenTypes.TOKEN_MULTI_LINE_COMMENT, sb.toString());
                    } else if (next == '=') {
                    	if (cs.readChar() == '=') {
                    		return new OperatorToken(TokenTypes.TOKEN_EQQ);
                    	} else {
                    		cs.unreadChar();
                    		return new OperatorToken(TokenTypes.TOKEN_EQ);
                    	}
                    } else if (next == '~') {
                    	return new OperatorToken(TokenTypes.TOKEN_MATCH);
                    } else if (next == '>') {
                    	return new OperatorToken(TokenTypes.TOKEN_ASSOC);
                    } else {
                    	cs.unreadChar();
                    	return new OperatorToken(TokenTypes.TOKEN_ASSIGN);
                    }
                case '!':
                	switch (cs.readChar()) {
                		case '=':
                			return new OperatorToken(TokenTypes.TOKEN_NEQ);
                		case '~':
                			return new OperatorToken(TokenTypes.TOKEN_NMATCH);
                		default:
                			cs.unreadChar();
                			return new OperatorToken(TokenTypes.TOKEN_NOT);
                	}
                default :
                    ScannerUtil.skipLine(cs);
                    cs.skipEol();
            }
        }

        return new DefaultToken(TokenTypes.TOKEN_EOF);
    }

    /**
     * Gets the characterScanner
     * @return The ICharacterScanner
     */
    public ICharacterScanner getCharacterScanner() {
        return characterScanner;
    }

    /**
     * Sets the characterScanner
     * @param characterScanner The characterScanner to set
     */
    public void setCharacterScanner(ICharacterScanner characterScanner) {
        this.characterScanner = characterScanner;
    }

    /**
     * Gets the eventListener
     * @return The  IScannerEventListener
     */
    public IScannerEventListener getEventListener() {
        return eventListener;
    }

    /**
     * Sets the eventListener
     * @param eventListener The eventListener to set
     */
    public void setEventListener(IScannerEventListener eventListener) {
        this.eventListener = eventListener;
    }
}