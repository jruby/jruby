/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyNode;

public class ThreadLocalObjectNode extends RubyNode {

    @CompilationFinal private DynamicObject firstThreadSeen;
    @CompilationFinal private DynamicObject firstThreadSeenLocals;
    private final ConditionProfile firstThreadProfile = ConditionProfile.createCountingProfile();

    public ThreadLocalObjectNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Override
    public DynamicObject executeDynamicObject(VirtualFrame frame) {
        if (firstThreadSeen == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            firstThreadSeen = currentThread();
            firstThreadSeenLocals = Layouts.THREAD.getThreadLocals(firstThreadSeen);
        }

        if (firstThreadProfile.profile(currentThread() == firstThreadSeen)) {
            return firstThreadSeenLocals;
        } else {
            return Layouts.THREAD.getThreadLocals(currentThread());
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeDynamicObject(frame);
    }

    private DynamicObject currentThread() {
        return getContext().getThreadManager().getCurrentThread();
    }

}
