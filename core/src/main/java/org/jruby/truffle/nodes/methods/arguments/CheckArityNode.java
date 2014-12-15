/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods.arguments;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.frame.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.methods.*;

import java.util.Map;

/**
 * Check arguments meet the arity of the method.
 */
public class CheckArityNode extends RubyNode {

    private final Arity arity;
    private final String[] keywords;
    private final boolean keywordsRest;

    public CheckArityNode(RubyContext context, SourceSection sourceSection, Arity arity) {
        this(context, sourceSection, arity, new String[]{}, false);
    }

    public CheckArityNode(RubyContext context, SourceSection sourceSection, Arity arity, String[] keywords, boolean keywordsRest) {
        super(context, sourceSection);
        this.arity = arity;
        this.keywords = keywords;
        this.keywordsRest = keywordsRest;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        final int given = RubyArguments.getUserArgumentsCount(frame.getArguments());

        if (!checkArity(frame, given)) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError(given, arity.getRequired(), this));
        }

        if (!keywordsRest && arity.hasKeywords() && getKeywordsHash(frame) != null) {
            for (RubyHash.Entry entry : getKeywordsHash(frame).verySlowToEntries()) {
                for (String keyword : keywords) {
                    if (!keyword.toString().equals(entry.getKey().toString())) {
                        throw new RaiseException(getContext().getCoreLibrary().argumentError("unknown keyword: " + entry.getKey().toString(), this));
                    }
                }
            }
        }
    }

    private boolean checkArity(VirtualFrame frame, int given) {
        if (arity.hasKeywords() && getKeywordsHash(frame) != null) {
            given -= 1;
        }

        if (arity.getRequired() != 0 && given < arity.getRequired()) {
            return false;
        } else if (!arity.allowsMore() && given > arity.getRequired() + arity.getOptional()) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        executeVoid(frame);
        return getContext().getCoreLibrary().getNilObject();
    }

    private RubyHash getKeywordsHash(VirtualFrame frame) {
        // TODO(CS): duplicated in ReadKeywordArgumentNode

        if (RubyArguments.getUserArgumentsCount(frame.getArguments()) <= arity.getRequired()) {
            return null;
        }

        final Object lastArgument = RubyArguments.getUserArgument(frame.getArguments(), RubyArguments.getUserArgumentsCount(frame.getArguments()) - 1);

        if (lastArgument instanceof RubyHash) {
            return (RubyHash) lastArgument;
        }

        return null;
    }

}
