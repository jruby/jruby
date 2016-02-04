/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.arguments;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.core.hash.HashOperations;
import org.jruby.truffle.language.methods.Arity;

import java.util.Map;

/**
 * Check arguments meet the arity of the method.
 */
public abstract class CheckArityNode {

    public static RubyNode create(RubyContext context, SourceSection sourceSection, Arity arity) {
        if (!arity.acceptsKeywords()) {
            return new CheckAritySimple(context, sourceSection, arity);
        } else {
            return new CheckArityKeywords(context, sourceSection, arity);
        }
    }

    private static class CheckAritySimple extends RubyNode {

        private final Arity arity;

        public CheckAritySimple(RubyContext context, SourceSection sourceSection, Arity arity) {
            super(context, sourceSection);
            this.arity = arity;
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            final int given = RubyArguments.getArgumentsCount(frame.getArguments());
            if (!checkArity(given)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError(given, arity.getRequired(), this));
            }
        }

        @Override
        public Object execute(VirtualFrame frame) {
            throw new UnsupportedOperationException("CheckArity should be call with executeVoid()");
        }

        private boolean checkArity(int given) {
            final int required = arity.getRequired();
            if (required != 0 && given < required) {
                return false;
            } else if (!arity.hasRest() && given > required + arity.getOptional()) {
                return false;
            } else {
                return true;
            }
        }

    }

    private static class CheckArityKeywords extends RubyNode {

        private final Arity arity;
        @Child private ReadUserKeywordsHashNode readUserKeywordsHashNode;

        private CheckArityKeywords(RubyContext context, SourceSection sourceSection, Arity arity) {
            super(context, sourceSection);
            this.arity = arity;
            readUserKeywordsHashNode = new ReadUserKeywordsHashNode(context, sourceSection, arity.getRequired());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            throw new UnsupportedOperationException("CheckArity should be call with executeVoid()");
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            final Object[] frameArguments = frame.getArguments();
            int given;

            given = RubyArguments.getArgumentsCount(frame.getArguments());

            final DynamicObject keywordArguments = (DynamicObject) readUserKeywordsHashNode.execute(frame);

            if (!checkArity(frame, given, keywordArguments)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError(given, arity.getRequired(), this));
            }

            if (!arity.hasKeywordsRest() && keywordArguments != null) {
                for (Map.Entry<Object, Object> keyValue : HashOperations.iterableKeyValues(keywordArguments)) {
                    if (RubyGuards.isRubySymbol(keyValue.getKey())) {
                        if (!keywordAllowed(keyValue.getKey().toString())) {
                            CompilerDirectives.transferToInterpreter();
                            throw new RaiseException(getContext().getCoreLibrary().argumentError("unknown keyword: " + keyValue.getKey().toString(), this));
                        }
                    } else {
                        given++;

                        if (given > arity.getRequired() && !arity.hasRest() && arity.getOptional() == 0) {
                            throw new RaiseException(getContext().getCoreLibrary().argumentError(given, arity.getRequired(), this));
                        }
                    }
                }
            }

            if (arity.hasKeywordsRest() && keywordArguments != null) {
                for (Map.Entry<Object, Object> keyValue : HashOperations.iterableKeyValues(keywordArguments)) {
                    if (!RubyGuards.isRubySymbol(keyValue.getKey())) {
                        given++;

                        if (given > arity.getRequired() && !arity.hasRest() && arity.getOptional() == 0) {
                            throw new RaiseException(getContext().getCoreLibrary().argumentError(given, arity.getRequired(), this));
                        }
                    }
                }
            }
        }

        private boolean checkArity(VirtualFrame frame, int given, DynamicObject keywordArguments) {
            if (keywordArguments != null) {
                given -= 1;
            }

            final int required = arity.getRequired();
            if (required != 0 && given < required) {
                return false;
            } else if (!arity.hasRest() && given > required + arity.getOptional()) {
                return false;
            } else {
                return true;
            }
        }

        private boolean keywordAllowed(String keyword) {
            for (String allowedKeyword : arity.getKeywordArguments()) {
                if (keyword.equals(allowedKeyword)) {
                    return true;
                }
            }
            return false;
        }
    }

}
