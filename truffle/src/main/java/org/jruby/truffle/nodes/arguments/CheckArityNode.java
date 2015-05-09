/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.arguments;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.hash.HashOperations;
import org.jruby.truffle.runtime.hash.KeyValue;
import org.jruby.truffle.runtime.methods.Arity;

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
        final Object[] frameArguments = frame.getArguments();
        final int given;
        final RubyHash keywordArguments;
        
        //TODO (MS): Check merge 
        if (RubyArguments.isKwOptimized(frame.getArguments())) {
            given = RubyArguments.getUserArgumentsCount(frame.getArguments())
                    - arity.getCountKeywords() - 2;
        } else {
            given = RubyArguments.getUserArgumentsCount(frame.getArguments());
        }

        if (arity.hasKeywords()) {
            keywordArguments = RubyArguments.getUserKeywordsHash(frameArguments, arity.getRequired());
        } else {
            keywordArguments = null;
        }

        if (!checkArity(frame, given, keywordArguments)) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError(given, arity.getRequired(), this));
        }

        if (!keywordsRest && keywordArguments != null) {
            for (KeyValue keyValue : HashOperations.verySlowToKeyValues(keywordArguments)) {
                if (!keywordAllowed(keyValue.getKey().toString())) {
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().argumentError("unknown keyword: " + keyValue.getKey().toString(), this));
                }
            }
        }
    }

    private boolean checkArity(VirtualFrame frame, int given, RubyHash keywordArguments) {
        if (keywordArguments != null) {
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

    private boolean keywordAllowed(String keyword) {
        for (String allowedKeyword : keywords) {
            if (keyword.equals(allowedKeyword)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        executeVoid(frame);
        return nil();
    }

}
