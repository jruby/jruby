package org.jruby.parser;

/**
 * Each scope while parsing contains extra information as part of the parsing process.
 * This class holds this instead of StaticScope which will outlive parsing.
 */
public class ScopedParserState {
    private ScopedParserState enclosingScope;
    private long commandArgumentStack;
    private long condArgumentStack;

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
}
