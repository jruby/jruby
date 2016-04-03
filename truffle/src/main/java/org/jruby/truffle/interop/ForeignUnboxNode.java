/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.AcceptMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyObjectType;

@AcceptMessage(value = "UNBOX", receiverType = RubyObjectType.class, language = RubyLanguage.class)
public final class ForeignUnboxNode extends ForeignUnboxBaseNode {

    private final ConditionProfile stringProfile = ConditionProfile.createBinaryProfile();

    @Override
    public Object access(VirtualFrame frame, DynamicObject object) {
        if (stringProfile.profile(RubyGuards.isRubyString(object))) {
            return StringOperations.getByteListReadOnly(object).get(0);
        } else {
            return object;
        }
    }

}
