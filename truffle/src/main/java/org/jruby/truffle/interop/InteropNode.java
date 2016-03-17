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

import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;

public abstract class InteropNode extends RubyNode {

    public InteropNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public static RubyNode createRead(RubyContext context, SourceSection sourceSection) {
        return new UnresolvedInteropReadNode(context, sourceSection);
    }

    public static RubyNode createWrite(RubyContext context, SourceSection sourceSection) {
        return new UnresolvedInteropWriteNode(context, sourceSection);
    }

    public static RubyNode createExecuteAfterRead(RubyContext context, SourceSection sourceSection, int arity) {
        return new UnresolvedInteropExecuteAfterReadNode(context, sourceSection, arity);
    }

    public static RubyNode createIsExecutable(final RubyContext context, final SourceSection sourceSection) {
        return new InteropIsExecutable(context, sourceSection);
    }
    
    public static RubyNode createExecute(final RubyContext context, final SourceSection sourceSection) {
        return new InteropExecute(context, sourceSection);
    }

    public static RubyNode createIsBoxedPrimitive(final RubyContext context, final SourceSection sourceSection) {
        return new InteropIsBoxedPrimitive(context, sourceSection);
    }

    public static RubyNode createIsNull(final RubyContext context, final SourceSection sourceSection) {
        return new InteropIsNull(context, sourceSection);
    }

    public static RubyNode createHasSizePropertyFalse(final RubyContext context, final SourceSection sourceSection) {
        return new InteropHasSizePropertyFalse(context, sourceSection);
    }

    public static RubyNode createHasSizePropertyTrue(final RubyContext context, final SourceSection sourceSection) {
        return new InteropHasSizePropertyTrue(context, sourceSection);
    }

    public static RubyNode createGetSize(RubyContext context, final SourceSection sourceSection) {
        return new InteropGetSizeProperty(context, sourceSection);
    }

    public static RubyNode createStringIsBoxed(RubyContext context, final SourceSection sourceSection) {
        return new InteropStringIsBoxed(context, sourceSection);
    }

    public static RubyNode createStringRead(RubyContext context, final SourceSection sourceSection) {
        return new UnresolvedInteropStringReadNode(context, sourceSection);
    }

    public static RubyNode createStringUnbox(RubyContext context, final SourceSection sourceSection) {
        return new InteropStringUnboxNode(context, sourceSection);
    }


}
