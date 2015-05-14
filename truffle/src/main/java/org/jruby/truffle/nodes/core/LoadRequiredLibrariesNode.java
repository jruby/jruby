/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import java.util.Collection;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;

/** Load libraries required from the command line (-r LIBRARY) */
public class LoadRequiredLibrariesNode extends RubyNode {

    @Child CallDispatchHeadNode requireNode;

    public LoadRequiredLibrariesNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        requireNode = DispatchHeadNodeFactory.createMethodCallOnSelf(context);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object self = RubyArguments.getSelf(frame.getArguments());
        Collection<String> requiredLibraries = getContext().getRuntime().getInstanceConfig().getRequiredLibraries();

        for (String requiredLibrary : requiredLibraries) {
            requireNode.call(frame, self, "require", null, getContext().makeString(requiredLibrary));
        }

        return nil();
    }

}
