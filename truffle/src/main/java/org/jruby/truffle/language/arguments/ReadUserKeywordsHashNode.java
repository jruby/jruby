/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DoesRespondDispatchHeadNode;

public class ReadUserKeywordsHashNode extends RubyNode {

    private final int minArgumentCount;

    @Child private DoesRespondDispatchHeadNode respondToToHashNode;
    @Child private CallDispatchHeadNode callToHashNode;

    private final ConditionProfile notEnoughArgumentsProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile lastArgumentIsHashProfile = ConditionProfile.createBinaryProfile();

    public ReadUserKeywordsHashNode(int minArgumentCount) {
        this.minArgumentCount = minArgumentCount;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final int argumentCount = RubyArguments.getArgumentsCount(frame);

        if (notEnoughArgumentsProfile.profile(argumentCount <= minArgumentCount)) {
            return null;
        }

        final Object lastArgument = RubyArguments.getArgument(frame, argumentCount - 1);

        if (lastArgumentIsHashProfile.profile(RubyGuards.isRubyHash(lastArgument))) {
            return lastArgument;
        }

        CompilerDirectives.bailout("Ruby keyword arguments aren't optimized yet");

        if (respondToToHashNode == null) {
            respondToToHashNode = insert(new DoesRespondDispatchHeadNode(getContext(), false));
        }

        if (respondToToHashNode.doesRespondTo(frame, "to_hash", lastArgument)) {
            if (callToHashNode == null) {
                callToHashNode = insert(CallDispatchHeadNode.createMethodCall());
            }

            final Object converted = callToHashNode.call(frame, lastArgument, "to_hash", null);

            if (RubyGuards.isRubyHash(converted)) {
                RubyArguments.setArgument(frame.getArguments(), argumentCount - 1, converted);
                return converted;
            }
        }

        return null;
    }

}
