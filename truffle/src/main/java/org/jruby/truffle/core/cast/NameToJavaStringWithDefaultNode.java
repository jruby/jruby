/*
 * Copyright (c) 2016, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.cast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyNode;

/**
 * Take a Symbol or some object accepting #to_str
 * and convert it to a Java String and defaults to
 * the given value if not provided.
 */
@NodeChild(value = "value", type = RubyNode.class)
public abstract class NameToJavaStringWithDefaultNode extends RubyNode {

    private final String defaultValue;
    @Child private NameToJavaStringNode toJavaStringNode = NameToJavaStringNodeGen.create(null);

    public NameToJavaStringWithDefaultNode(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public abstract String executeString(VirtualFrame frame, Object value);

    @Specialization
    public String doDefault(VirtualFrame frame, NotProvided value) {
        return toJavaStringNode.executeToJavaString(frame, defaultValue);
    }

    @Specialization(guards = "wasProvided(value)")
    public String doProvided(VirtualFrame frame, Object value) {
        return toJavaStringNode.executeToJavaString(frame, value);
    }


}
