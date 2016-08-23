/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.arguments;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ValueProfile;
import org.jruby.truffle.language.RubyNode;

public class ProfileArgumentNode extends RubyNode {

    @Child private RubyNode readArgumentNode;

    private final ValueProfile valueProfile = ValueProfile.createEqualityProfile();

    public ProfileArgumentNode(RubyNode readArgumentNode) {
        this.readArgumentNode = readArgumentNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return valueProfile.profile(readArgumentNode.execute(frame));
    }

}
