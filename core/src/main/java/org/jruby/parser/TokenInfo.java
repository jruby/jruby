package org.jruby.parser;

public class TokenInfo {
    public TokenInfo next;
    String name;
    int line;
    public boolean nonspc;

    public TokenInfo(TokenInfo previous, String name, int line) {
        this.next = previous;
        this.name = name;
        this.line = line;
    }
}
