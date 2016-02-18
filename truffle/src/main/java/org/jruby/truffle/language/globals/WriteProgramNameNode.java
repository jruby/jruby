/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.globals;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;

@NodeChild(value = "value", type = RubyNode.class)
public abstract class WriteProgramNameNode extends RubyNode {

    public WriteProgramNameNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @TruffleBoundary
    @Specialization(guards = "isRubyString(name)")
    protected Object writeProgramName(DynamicObject name) {
        if (getContext().getNativePlatform().getProcessName().canSet()) {
            getContext().getNativePlatform().getProcessName().set(name.toString());
        }

        return name;
    }

}
