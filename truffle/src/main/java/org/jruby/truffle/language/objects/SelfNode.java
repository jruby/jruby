/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ValueProfile;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;

public class SelfNode extends RubyNode {

    private final ValueProfile valueProfile = ValueProfile.createEqualityProfile();

    public SelfNode(RubyContext context) {
        super(context, null);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return valueProfile.profile(RubyArguments.getSelf(frame));
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return coreStrings().SELF.createInstance();
    }

}
