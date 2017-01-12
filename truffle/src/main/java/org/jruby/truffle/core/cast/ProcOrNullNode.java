/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyNode;

@NodeChild(value = "child", type = RubyNode.class)
public abstract class ProcOrNullNode extends RubyNode {

    public abstract RubyNode getChild();

    public abstract DynamicObject executeProcOrNull(Object proc);

    @Specialization
    public DynamicObject doNotProvided(NotProvided proc) {
        return null;
    }

    @Specialization(guards = "isNil(nil)")
    public DynamicObject doNil(Object nil) {
        return null;
    }

    @Specialization(guards = "isRubyProc(proc)")
    public DynamicObject doProc(DynamicObject proc) {
        return proc;
    }

}
