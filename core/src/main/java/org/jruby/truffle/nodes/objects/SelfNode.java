/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objects;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.ValueProfile;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;

public class SelfNode extends RubyNode {

    private final ValueProfile valueProfile = ValueProfile.createPrimitiveProfile();

    public SelfNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return valueProfile.profile(RubyArguments.getSelf(frame.getArguments()));
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return getContext().makeString("self");
    }

}
