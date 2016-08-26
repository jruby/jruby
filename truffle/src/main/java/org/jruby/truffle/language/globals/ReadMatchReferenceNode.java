/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.globals;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;

public class ReadMatchReferenceNode extends RubyNode {

    public static final int PRE = -1;
    public static final int POST = -2;
    public static final int GLOBAL = -3;
    public static final int HIGHEST = -4;

    private final int index;

    @Child private ReadThreadLocalGlobalVariableNode readMatchNode;

    private final ConditionProfile matchNilProfile = ConditionProfile.createBinaryProfile();

    public ReadMatchReferenceNode(RubyContext context, SourceSection sourceSection, int index) {
        super(context, sourceSection);
        this.index = index;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object match = getReadMatchNode().execute(frame);

        if (matchNilProfile.profile(match == nil())) {
            return nil();
        }

        final DynamicObject matchData = (DynamicObject) match;

        switch (index) {
            case PRE:
                return Layouts.MATCH_DATA.getPre(matchData);

            case POST:
                return Layouts.MATCH_DATA.getPost(matchData);

            case GLOBAL:
                return Layouts.MATCH_DATA.getGlobal(matchData);

            case HIGHEST: {
                final Object[] values = Layouts.MATCH_DATA.getValues(matchData);

                for (int n = values.length - 1; n >= 0; n--)
                    if (values[n] != nil()) {
                        return values[n];
                    }

                return nil();
            }

            default: {
                final Object[] values = Layouts.MATCH_DATA.getValues(matchData);

                if (index >= values.length) {
                    return nil();
                } else {
                    return values[index];
                }
            }
        }
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        if (isNil(execute(frame))) {
            return nil();
        } else {
            return coreStrings().GLOBAL_VARIABLE.createInstance();
        }
    }

    private ReadThreadLocalGlobalVariableNode getReadMatchNode() {
        if (readMatchNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            readMatchNode = insert(new ReadThreadLocalGlobalVariableNode(getContext(), null, "$~", true));
        }

        return readMatchNode;
    }

}
