package org.jruby.ext.ripper;

import org.jruby.runtime.builtin.IRubyObject;

/**
 * Simple struct to temporarily hold values during part of parsing.
 */
public class ArgsTailHolder {
    private final IRubyObject keywordArgs;
    private final IRubyObject keywordRestArg;
    private final IRubyObject blockArg;

    public ArgsTailHolder(IRubyObject keywordArgs, IRubyObject keywordRestArg, IRubyObject blockArg) {
        this.keywordArgs = keywordArgs;
        this.keywordRestArg = keywordRestArg;
        this.blockArg = blockArg;

    }

    public IRubyObject getBlockArg() {
        return blockArg;
    }

    public IRubyObject getKeywordArgs() {
        return keywordArgs;
    }

    public IRubyObject getKeywordRestArg() {
        return keywordRestArg;
    }
}
