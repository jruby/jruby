package org.jruby.scanner;

public class DefaultToken implements IToken {
	public static final int TT_EOF = 0;
	public static final int TT_WHITESPACE = 1;
	
	public static final IToken TOKEN_EOF = new DefaultToken(TT_EOF);
	public static final IToken TOKEN_WHITESPACE = new DefaultToken(TT_WHITESPACE);
	
	private int type;
	
	public DefaultToken(int type) {
		this.type = type;
	}
	
    /**
     * @see IToken#getType()
     */
    public int getType() {
        return type;
    }
}