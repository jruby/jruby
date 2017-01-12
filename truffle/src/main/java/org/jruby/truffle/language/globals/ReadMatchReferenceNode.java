/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.globals;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.threadlocal.GetFromThreadLocalNode;

public class ReadMatchReferenceNode extends RubyNode {

    public static final int PRE = -1;
    public static final int POST = -2;
    public static final int GLOBAL = -3;
    public static final int HIGHEST = -4;

    private final int index;

    @Child private GetFromThreadLocalNode readMatchNode;

    private final ConditionProfile matchNilProfile = ConditionProfile.createBinaryProfile();

    public ReadMatchReferenceNode(GetFromThreadLocalNode readMatchNode, int index) {
        this.readMatchNode = readMatchNode;
        this.index = index;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (readMatchNode == null) {
            return nil();
        }

        final Object match = readMatchNode.execute(frame);

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

}
