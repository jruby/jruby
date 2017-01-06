/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.arguments;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.jruby.truffle.Log;
import org.jruby.truffle.core.hash.HashOperations;
import org.jruby.truffle.core.hash.KeyValue;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.methods.Arity;

public class CheckKeywordArityNode extends RubyNode {

    private final Arity arity;

    @Child private ReadUserKeywordsHashNode readUserKeywordsHashNode;

    private final BranchProfile receivedKeywordsProfile = BranchProfile.create();
    private final BranchProfile basicArityCheckFailedProfile = BranchProfile.create();

    public CheckKeywordArityNode(Arity arity) {
        this.arity = arity;
        readUserKeywordsHashNode = new ReadUserKeywordsHashNode(arity.getRequired());
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        final Object keywordArguments = readUserKeywordsHashNode.execute(frame);

        int given = RubyArguments.getArgumentsCount(frame);

        if (keywordArguments != null) {
            receivedKeywordsProfile.enter();
            given -= 1;
        }

        if (!CheckArityNode.checkArity(arity, given)) {
            basicArityCheckFailedProfile.enter();
            throw new RaiseException(coreExceptions().argumentError(given, arity.getRequired(), this));
        }

        if (keywordArguments != null) {
            receivedKeywordsProfile.enter();
            Log.notOptimizedOnce(Log.KWARGS_NOT_OPTIMIZED_YET);
            checkArityKeywordArguments(keywordArguments, given);
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        executeVoid(frame);
        return nil();
    }

    @TruffleBoundary
    private void checkArityKeywordArguments(Object keywordArguments, int given) {
        final DynamicObject keywordHash = (DynamicObject) keywordArguments;

        for (KeyValue keyValue : HashOperations.iterableKeyValues(keywordHash)) {
            if (arity.hasKeywordsRest()) {
                if (RubyGuards.isRubySymbol(keyValue.getKey())) {
                    continue;
                }
            } else {
                if (RubyGuards.isRubySymbol(keyValue.getKey())) {
                    if (!keywordAllowed(keyValue.getKey().toString())) {
                        throw new RaiseException(coreExceptions().argumentErrorUnknownKeyword(
                                keyValue.getKey(), this));
                    }

                    continue;
                }
            }

            given++;

            if (given > arity.getRequired() && !arity.hasRest() && arity.getOptional() == 0) {
                throw new RaiseException(coreExceptions().argumentError(given, arity.getRequired(), this));
            }
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
