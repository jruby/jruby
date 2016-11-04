/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;

import java.util.Collection;

/** Load libraries required from the command line (-r LIBRARY) */
public class LoadRequiredLibrariesNode extends RubyNode {

    @Child CallDispatchHeadNode requireNode;

    public LoadRequiredLibrariesNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        requireNode = DispatchHeadNodeFactory.createMethodCallOnSelf(context);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object self = RubyArguments.getSelf(frame);

        for (String requiredLibrary : getRequiredLibraries()) {
            requireNode.call(frame, self, "require", createString(StringOperations.encodeRope(requiredLibrary, UTF8Encoding.INSTANCE)));
        }

        return nil();
    }

    @TruffleBoundary
    private Collection<String> getRequiredLibraries() {
        return getContext().getInstanceConfig().getRequiredLibraries();
    }

}
