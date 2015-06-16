/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;

public abstract class ModulePrimitiveNodes {

    @RubiniusPrimitive(name = "module_mirror")
    public abstract static class ModuleMirrorPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private CallDispatchHeadNode moduleMirrorNode;

        public ModuleMirrorPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object moduleMirrorCached(VirtualFrame frame, Object reflectee) {
            if (moduleMirrorNode == null) {
                CompilerDirectives.transferToInterpreter();
                moduleMirrorNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), null));
            }

            return moduleMirrorNode.call(frame, getContext().getCoreLibrary().getRubiniusMirror(), "module_mirror", null, reflectee);
        }

    }

}
