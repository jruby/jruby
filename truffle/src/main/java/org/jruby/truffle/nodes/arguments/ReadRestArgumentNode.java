/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.array.ArrayUtils;
import org.jruby.truffle.runtime.layouts.Layouts;

/**
 * Read the rest of arguments after a certain point into an array.
 */
public class ReadRestArgumentNode extends RubyNode {

    private final int startIndex;
    private final int negativeEndIndex;
    private final boolean keywordArguments;
    private final int minimumForKWargs;

    private final BranchProfile noArgumentsLeftProfile = BranchProfile.create();
    private final BranchProfile subsetOfArgumentsProfile = BranchProfile.create();

    @Child private ReadUserKeywordsHashNode readUserKeywordsHashNode;

    public ReadRestArgumentNode(RubyContext context, SourceSection sourceSection, int startIndex, int negativeEndIndex, boolean keywordArguments, int minimumForKWargs) {
        super(context, sourceSection);
        this.startIndex = startIndex;
        this.negativeEndIndex = negativeEndIndex;
        this.keywordArguments = keywordArguments;
        this.minimumForKWargs = minimumForKWargs;
        readUserKeywordsHashNode = new ReadUserKeywordsHashNode(context, sourceSection, minimumForKWargs);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        int count = RubyArguments.getArgumentsCount(frame.getArguments());

        int endIndex = count + negativeEndIndex;

        if (keywordArguments) {
            final Object lastArgument = RubyArguments.getArgument(frame.getArguments(), RubyArguments.getArgumentsCount(frame.getArguments()) - 1);

            if (RubyGuards.isRubyHash(lastArgument)) {
                endIndex -= 1;
            }
        }

        final int length = endIndex - startIndex;

        final Object resultStore;
        final int resultLength;

        if (startIndex == 0) {
            final Object[] arguments = RubyArguments.getArguments(frame.getArguments());
            resultStore = arguments;
            resultLength = length;
        } else {
            if (startIndex >= endIndex) {
                noArgumentsLeftProfile.enter();
                resultStore = null;
                resultLength = 0;
            } else {
                subsetOfArgumentsProfile.enter();
                final Object[] arguments = RubyArguments.getArguments(frame.getArguments());
                resultStore = ArrayUtils.extractRange(arguments, startIndex, endIndex);
                resultLength = length;
            }
        }

        final DynamicObject rest = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), resultStore, resultLength);

        if (keywordArguments) {
            CompilerDirectives.transferToInterpreter();

            Object kwargsHash = readUserKeywordsHashNode.execute(frame);

            if (kwargsHash == null) {
                kwargsHash = nil();
            }

            getContext().inlineRubyHelper(this, "Truffle::Primitive.add_rejected_kwargs_to_rest(rest, kwargs)",
                    "rest", rest,
                    "kwargs", kwargsHash);
        }

        return rest;
    }
}
