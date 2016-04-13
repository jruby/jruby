/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.time;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.joda.time.DateTimeZone;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.constants.ReadLiteralConstantNode;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.literal.ObjectLiteralNode;

public class ReadTimeZoneNode extends RubyNode {
    
    @Child private CallDispatchHeadNode hashNode;
    @Child private ReadLiteralConstantNode envNode;

    private final ConditionProfile tzNilProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile tzStringProfile = ConditionProfile.createBinaryProfile();

    private static final Rope defaultZone = StringOperations.encodeRope(DateTimeZone.getDefault().toString(), UTF8Encoding.INSTANCE);
    private final DynamicObject TZ;
    
    public ReadTimeZoneNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        hashNode = DispatchHeadNodeFactory.createMethodCall(context);
        envNode = new ReadLiteralConstantNode(context, sourceSection,
                new ObjectLiteralNode(context, sourceSection, coreLibrary().getObjectClass()), "ENV");
        TZ = create7BitString("TZ", UTF8Encoding.INSTANCE);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object tz = hashNode.call(frame, envNode.execute(frame), "[]", null, TZ);

        // TODO CS 4-May-15 not sure how TZ ends up being nil

        if (tzNilProfile.profile(tz == nil())) {
            return createString(defaultZone);
        } else if (tzStringProfile.profile(RubyGuards.isRubyString(tz))) {
            return tz;
        } else {
            throw new UnsupportedOperationException();
        }
    }

}
