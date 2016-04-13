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
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.hash.HashOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.methods.Arity;

import java.util.Map;

public class CheckKeywordArityNode extends RubyNode {

    private final Arity arity;

    @Child private ReadUserKeywordsHashNode readUserKeywordsHashNode;

    private final BranchProfile receivedKeywordsProfile = BranchProfile.create();
    private final BranchProfile basicArityCheckFailedProfile = BranchProfile.create();

    public CheckKeywordArityNode(RubyContext context, SourceSection sourceSection, Arity arity) {
        super(context, sourceSection);
        this.arity = arity;
        readUserKeywordsHashNode = new ReadUserKeywordsHashNode(context, sourceSection, arity.getRequired());
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
            throw new RaiseException(coreLibrary().argumentError(given, arity.getRequired(), this));
        }

        if (keywordArguments != null) {
            receivedKeywordsProfile.enter();

            CompilerDirectives.bailout("Ruby keyword arguments aren't optimized");

            final DynamicObject keywordHash = (DynamicObject) keywordArguments;

            for (Map.Entry<Object, Object> keyValue : HashOperations.iterableKeyValues(keywordHash)) {
                if (arity.hasKeywordsRest()) {
                    if (RubyGuards.isRubySymbol(keyValue.getKey())) {
                        continue;
                    }
                } else {
                    if (RubyGuards.isRubySymbol(keyValue.getKey())) {
                        if (!keywordAllowed(keyValue.getKey().toString())) {
                            throw new RaiseException(coreLibrary().argumentErrorUnknownKeyword(
                                    keyValue.getKey(), this));
                        }

                        continue;
                    }
                }

                given++;

                if (given > arity.getRequired() && !arity.hasRest() && arity.getOptional() == 0) {
                    throw new RaiseException(coreLibrary().argumentError(given, arity.getRequired(), this));
                }
            }
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        executeVoid(frame);
        return nil();
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
