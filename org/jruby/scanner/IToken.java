package org.jruby.scanner;

public interface IToken {
    public int getType();
    
    public int getLine();
    public int getColumn();
    
    public Object getData();
}