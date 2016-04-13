/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.module.ModuleNodes;
import org.jruby.truffle.core.module.ModuleNodesFactory;
import org.jruby.truffle.language.RubyNode;

@CoreClass(name = "main")
public abstract class MainNodes {

    @CoreMethod(names = "public", rest = true, needsSelf = false, visibility = Visibility.PRIVATE)
    public abstract static class PublicNode extends CoreMethodArrayArgumentsNode {

        @Child private ModuleNodes.PublicNode publicNode;

        public PublicNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            publicNode = ModuleNodesFactory.PublicNodeFactory.create(context, sourceSection, new RubyNode[]{null, null});
        }

        @Specialization
        public DynamicObject doPublic(VirtualFrame frame, Object[] args) {
            final DynamicObject object = coreLibrary().getObjectClass();
            return publicNode.executePublic(frame, object, args);
        }
    }

    @CoreMethod(names = "private", rest = true, needsSelf = false, visibility = Visibility.PRIVATE)
    public abstract static class PrivateNode extends CoreMethodArrayArgumentsNode {

        @Child private ModuleNodes.PrivateNode privateNode;

        public PrivateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            privateNode = ModuleNodesFactory.PrivateNodeFactory.create(context, sourceSection, new RubyNode[]{null, null});
        }

        @Specialization
        public DynamicObject doPrivate(VirtualFrame frame, Object[] args) {
            final DynamicObject object = coreLibrary().getObjectClass();
            return privateNode.executePrivate(frame, object, args);
        }
    }

}
