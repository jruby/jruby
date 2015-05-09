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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.util.ArrayUtils;

/**
 * Read the rest of arguments after a certain point into an array.
 */
public class ReadRestArgumentNode extends RubyNode {

    private final int startIndex;
    private final int negativeEndIndex;
    private final boolean keywordArguments;

    private final BranchProfile noArgumentsLeftProfile = BranchProfile.create();
    private final BranchProfile subsetOfArgumentsProfile = BranchProfile.create();

    public ReadRestArgumentNode(RubyContext context, SourceSection sourceSection, int startIndex, int negativeEndIndex, boolean keywordArguments) {
        super(context, sourceSection);
        this.startIndex = startIndex;
        this.negativeEndIndex = negativeEndIndex;
        this.keywordArguments = keywordArguments;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final RubyClass arrayClass = getContext().getCoreLibrary().getArrayClass();

        int count = RubyArguments.getUserArgumentsCount(frame.getArguments());

        int endIndex = count + negativeEndIndex;

        if (keywordArguments) {
            final Object lastArgument = RubyArguments.getUserArgument(frame.getArguments(), RubyArguments.getUserArgumentsCount(frame.getArguments()) - 1);

            if (lastArgument instanceof RubyHash) {
                endIndex -= 1;
            }
        }

        final int length = endIndex - startIndex;

        if (startIndex == 0) {
            final Object[] arguments = RubyArguments.extractUserArguments(frame.getArguments());
            return new RubyArray(arrayClass, arguments, length);
        } else {
            if (startIndex >= endIndex) {
                noArgumentsLeftProfile.enter();
                return new RubyArray(arrayClass);
            } else {
                subsetOfArgumentsProfile.enter();
                final Object[] arguments = RubyArguments.extractUserArguments(frame.getArguments());
                // TODO(CS): risk here of widening types too much - always going to be Object[] - does seem to be something that does happen
                return new RubyArray(arrayClass, ArrayUtils.extractRange(arguments, startIndex, endIndex), length);
            }
        }
    }
}
