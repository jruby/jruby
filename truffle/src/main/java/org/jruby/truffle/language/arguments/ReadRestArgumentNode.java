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
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.array.ArrayUtils;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;

public class ReadRestArgumentNode extends RubyNode {

    private final int startIndex;
    private final int indexFromCount;
    private final boolean keywordArguments;

    private final BranchProfile noArgumentsLeftProfile = BranchProfile.create();
    private final BranchProfile subsetOfArgumentsProfile = BranchProfile.create();

    @Child private ReadUserKeywordsHashNode readUserKeywordsHashNode;

    public ReadRestArgumentNode(RubyContext context, SourceSection sourceSection, int startIndex, int indexFromCount,
                                boolean keywordArguments, int minimumForKWargs) {
        super(context, sourceSection);
        this.startIndex = startIndex;
        this.indexFromCount = indexFromCount;
        this.keywordArguments = keywordArguments;

        if (keywordArguments) {
            readUserKeywordsHashNode = new ReadUserKeywordsHashNode(context, sourceSection, minimumForKWargs);
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        int endIndex = RubyArguments.getArgumentsCount(frame) - indexFromCount;

        if (keywordArguments) {
            final Object lastArgument = RubyArguments.getArgument(frame, RubyArguments.getArgumentsCount(frame) - 1);

            if (RubyGuards.isRubyHash(lastArgument)) {
                endIndex -= 1;
            }
        }

        final int length = endIndex - startIndex;

        final Object resultStore;
        final int resultLength;

        if (startIndex == 0) {
            final Object[] arguments = RubyArguments.getArguments(frame);
            resultStore = arguments;
            resultLength = length;
        } else {
            if (startIndex >= endIndex) {
                noArgumentsLeftProfile.enter();
                resultStore = null;
                resultLength = 0;
            } else {
                subsetOfArgumentsProfile.enter();
                final Object[] arguments = RubyArguments.getArguments(frame);
                resultStore = ArrayUtils.extractRange(arguments, startIndex, endIndex);
                resultLength = length;
            }
        }

        final DynamicObject rest = Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(),
                resultStore, resultLength);

        if (keywordArguments) {
            CompilerDirectives.bailout("Ruby keyword arguments not optimized yet");

            Object kwargsHash = readUserKeywordsHashNode.execute(frame);

            if (kwargsHash == null) {
                kwargsHash = nil();
            }

            getContext().getCodeLoader().inline(this,
                    "Truffle::Primitive.add_rejected_kwargs_to_rest(rest, kwargs)",
                    "rest", rest,
                    "kwargs", kwargsHash);
        }

        return rest;
    }
}
