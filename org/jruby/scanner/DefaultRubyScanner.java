package org.jruby.scanner;

public class DefaultRubyScanner implements IRubyScanner {
	private ICharacterScanner characterScanner = null;
	private IScannerEventListener eventListener = null;
	
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
    		return DefaultToken.TOKEN_EOF;
    	}

    	switch (cs.readChar()) {
    		case '#':
    			String comment = ScannerUtil.getLine(cs);
    			// return new LineCommentToken(comment);
   			//case '':
    	}
    	
        return null;
    }

	/**
	 * Gets the characterScanner
	 * @return Returns a ICharacterScanner
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
	 * @return Returns a IScannerEventListener
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