package org.jruby.parser;

/**
 * Each scope while parsing contains extra information as part of the parsing process.
 * This class holds this instead of StaticScope which will outlive parsing.
 */
public class ScopedParserState {
    private ScopedParserState enclosingScope;
    private long commandArgumentStack;
    private long condArgumentStack;
    // A list of booleans indicating which variables are named captures from regexp
    private boolean[] namedCaptures;

    // block scope constructor
    public ScopedParserState(ScopedParserState enclosingScope) {
        this.enclosingScope = enclosingScope;
        this.commandArgumentStack = 0;
        this.condArgumentStack = 0;
    }
    // local scope constructor
    public ScopedParserState(ScopedParserState enclosingScope, long commandArgumentStack, long condArgumentStack) {
        this.enclosingScope = enclosingScope;
        this.commandArgumentStack = commandArgumentStack;
        this.condArgumentStack = condArgumentStack;
    }

    public void setCondArgumentStack(long condArgumentStack) {
        this.condArgumentStack = condArgumentStack;
    }

    public long getCondArgumentStack() {
        return condArgumentStack;
    }

    public void setCommandArgumentStack(long commandArgumentStack) {
        this.commandArgumentStack = commandArgumentStack;
    }

    public long getCommandArgumentStack() {
        return commandArgumentStack;
    }

    public ScopedParserState getEnclosingScope() {
        return enclosingScope;
    }

    public void growNamedCaptures(int index) {
        boolean[] namedCaptures = this.namedCaptures;
        boolean[] newNamedCaptures;
        if (namedCaptures != null) {
            newNamedCaptures = new boolean[Math.max(index + 1, namedCaptures.length)];
            System.arraycopy(namedCaptures, 0, newNamedCaptures, 0, namedCaptures.length);
        } else {
            newNamedCaptures = new boolean[index + 1];
        }
        newNamedCaptures[index] = true;
        this.namedCaptures = newNamedCaptures;
    }

    public boolean isNamedCapture(int index) {
        boolean[] namedCaptures = this.namedCaptures;
        return namedCaptures != null && index < namedCaptures.length && namedCaptures[index];
    }
}
