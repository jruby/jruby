/*
 * Copyright (c) 2016, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
/*
 * Copyright (c) 2013, 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.literal;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.language.RubyNode;

public class StringLiteralNode extends RubyNode {

    private final Rope rope;

    public StringLiteralNode(Rope rope) {
        this.rope = rope;
    }

    @Override
    public DynamicObject execute(VirtualFrame frame) {
        return Layouts.STRING.createString(coreLibrary().getStringFactory(), rope);
    }

}
