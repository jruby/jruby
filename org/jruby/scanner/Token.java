package org.jruby.scanner;

public final class Token implements IToken {
    private int type;
    private int line;
    private int column;
    private Object data;

    public Token(int type, int line, int column) {
        this(type, line, column, null);
    }

    public Token(int type, int line, int column, Object data) {
        this.type = type;
        this.line = line;
        this.column = column;
        this.data = data;
    }

    /**
     * @see IToken#getType()
     */
    public int getType() {
        return type;
    }

    /**
     * @see IToken#getLine()
     */
    public int getLine() {
        return line;
    }

    /**
     * @see IToken#getColumn()
     */
    public int getColumn() {
        return column;
    }

    /**
     * @see IToken#getData()
     */
    public Object getData() {
        return data;
    }

    public int hashCode() {
        return getType() ^ getLine() ^ getColumn() ^ getData().hashCode();
    }

    public boolean equals(Object other) {
        if (other instanceof IToken) {
            return getType() == ((IToken) other).getType()
                && getLine() == ((IToken) other).getLine()
                && getColumn() == ((IToken) other).getColumn()
                && (getData() != null ? getData().equals(((IToken) other).getData()) : ((IToken) other).getData() == null);
        }
        return false;
    }
    
    public String toString() {
        return "Token: Type = " + getType() + "; Line = " + getLine() + "; Column = " + getColumn() + "; Data = \"" + String.valueOf(getData()) + "\"";
    }
}