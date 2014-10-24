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

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.RubyArray;

/**
 * Read the rest of arguments after a certain point into an array.
 */
public class ReadRestArgumentNode extends RubyNode {

    private final int index;

    private final BranchProfile noArgumentsLeftProfile = BranchProfile.create();
    private final BranchProfile subsetOfArgumentsProfile = BranchProfile.create();

    public ReadRestArgumentNode(RubyContext context, SourceSection sourceSection, int index) {
        super(context, sourceSection);
        this.index = index;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final RubyClass arrayClass = getContext().getCoreLibrary().getArrayClass();

        if (index == 0) {
            final Object[] arguments = RubyArguments.extractUserArguments(frame.getArguments());
            return new RubyArray(arrayClass, arguments, arguments.length);
        } else {
            if (RubyArguments.getUserArgumentsCount(frame.getArguments()) <= index) {
                noArgumentsLeftProfile.enter();
                return new RubyArray(arrayClass);
            } else {
                subsetOfArgumentsProfile.enter();
                final Object[] arguments = RubyArguments.extractUserArguments(frame.getArguments());
                // TODO(CS): risk here of widening types too much - always going to be Object[] - does seem to be something that does happen
                return new RubyArray(arrayClass, Arrays.copyOfRange(arguments, index, arguments.length), arguments.length - index);
            }
        }
    }
}
