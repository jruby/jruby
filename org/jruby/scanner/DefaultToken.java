package org.jruby.scanner;

public class DefaultToken implements IToken {
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