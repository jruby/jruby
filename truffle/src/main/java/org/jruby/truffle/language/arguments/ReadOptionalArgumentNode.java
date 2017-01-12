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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.Log;
import org.jruby.truffle.core.array.ArrayReadNormalizedNode;
import org.jruby.truffle.core.array.ArrayReadNormalizedNodeGen;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;

public class ReadOptionalArgumentNode extends RubyNode {

    private final int index;
    private final int minimum;
    private final boolean considerRejectedKWArgs;
    private final boolean reduceMinimumWhenNoKWargs;

    @Child private ReadRestArgumentNode readRestArgumentNode;
    @Child private RubyNode defaultValue;
    @Child private ReadUserKeywordsHashNode readUserKeywordsHashNode;
    @Child private ArrayReadNormalizedNode arrayReadNode;

    private final BranchProfile defaultValueProfile = BranchProfile.create();

    public ReadOptionalArgumentNode(int index, int minimum,
                                    boolean considerRejectedKWArgs, boolean reduceMinimumWhenNoKWargs,
                                    int requiredForKWArgs, ReadRestArgumentNode readRestArgumentNode,
                                    RubyNode defaultValue) {
        this.index = index;
        this.minimum = minimum;
        this.considerRejectedKWArgs = considerRejectedKWArgs;
        this.defaultValue = defaultValue;
        this.readRestArgumentNode = readRestArgumentNode;
        this.reduceMinimumWhenNoKWargs = reduceMinimumWhenNoKWargs;

        if (reduceMinimumWhenNoKWargs) {
            readUserKeywordsHashNode = new ReadUserKeywordsHashNode(requiredForKWArgs);
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        int effectiveMinimum = minimum;

        if (reduceMinimumWhenNoKWargs) {
            if (readUserKeywordsHashNode.execute(frame) == null) {
                effectiveMinimum--;
            }
        }

        if (RubyArguments.getArgumentsCount(frame) >= effectiveMinimum) {
            return RubyArguments.getArgument(frame, index);
        }

        defaultValueProfile.enter();

        if (considerRejectedKWArgs) {
            Log.notOptimizedOnce(Log.KWARGS_NOT_OPTIMIZED_YET);

            final Object rest = readRestArgumentNode.execute(frame);

            if (RubyGuards.isRubyArray(rest) && Layouts.ARRAY.getSize((DynamicObject) rest) > 0) {
                if (arrayReadNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    arrayReadNode = insert(ArrayReadNormalizedNodeGen.create(null, null));
                }

                return arrayReadNode.executeRead((DynamicObject) rest, 0);
            }
        }

        return defaultValue.execute(frame);
    }

}
