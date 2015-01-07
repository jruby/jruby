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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.KernelNodes;
import org.jruby.truffle.nodes.core.KernelNodesFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.*;

/**
 * Rubinius primitives associated with types and objects.
 */
public abstract class TypePrimitiveNodes {

    @RubiniusPrimitive(name = "vm_object_kind_of", needsSelf = false)
    public static abstract class VMObjectKindOfPrimitiveNode extends RubiniusPrimitiveNode {

        @Child protected KernelNodes.IsANode isANode;

        public VMObjectKindOfPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            isANode = KernelNodesFactory.IsANodeFactory.create(context, sourceSection, new RubyNode[]{null, null});
        }

        public VMObjectKindOfPrimitiveNode(VMObjectKindOfPrimitiveNode prev) {
            super(prev);
            isANode = prev.isANode;
        }

        @Specialization
        public boolean vmObjectKindOf(VirtualFrame frame, Object object, RubyClass rubyClass) {
            return isANode.executeIsA(frame, object, rubyClass);
        }

    }

    @RubiniusPrimitive(name = "vm_object_class", needsSelf = false)
    public static abstract class VMObjectClassPrimitiveNode extends RubiniusPrimitiveNode {

        @Child protected KernelNodes.ClassNode classNode;

        public VMObjectClassPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            classNode = KernelNodesFactory.ClassNodeFactory.create(context, sourceSection, new RubyNode[]{null});
        }

        public VMObjectClassPrimitiveNode(VMObjectClassPrimitiveNode prev) {
            super(prev);
            classNode = prev.classNode;
        }

        @Specialization
        public RubyClass vmObjectClass(VirtualFrame frame, Object object) {
            return classNode.executeGetClass(frame, object);
        }

    }

    @RubiniusPrimitive(name = "vm_object_singleton_class", needsSelf = false)
    public static abstract class VMObjectSingletonClassPrimitiveNode extends RubiniusPrimitiveNode {

        @Child protected KernelNodes.SingletonClassMethodNode singletonClassNode;

        public VMObjectSingletonClassPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            singletonClassNode = KernelNodesFactory.SingletonClassMethodNodeFactory.create(context, sourceSection, new RubyNode[]{null});
        }

        public VMObjectSingletonClassPrimitiveNode(VMObjectSingletonClassPrimitiveNode prev) {
            super(prev);
            singletonClassNode = prev.singletonClassNode;
        }

        @Specialization
        public Object vmObjectClass(VirtualFrame frame, Object object) {
            return singletonClassNode.singletonClass(frame, object);
        }

    }

    @RubiniusPrimitive(name = "vm_singleton_class_object", needsSelf = false)
    public static abstract class VMObjectSingletonClassObjectPrimitiveNode extends RubiniusPrimitiveNode {

        public VMObjectSingletonClassObjectPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public VMObjectSingletonClassObjectPrimitiveNode(VMObjectSingletonClassObjectPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public Object vmSingletonClassObject(Object object) {
            throw new UnsupportedOperationException("vm_singleton_class_object");
        }

    }

    @RubiniusPrimitive(name = "vm_object_respond_to", needsSelf = false)
    public static abstract class VMObjectRespondToPrimitiveNode extends RubiniusPrimitiveNode {

        @Child protected KernelNodes.RespondToNode respondToNode;

        public VMObjectRespondToPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            respondToNode = KernelNodesFactory.RespondToNodeFactory.create(context, sourceSection, new RubyNode[]{null, null, null});
        }

        public VMObjectRespondToPrimitiveNode(VMObjectRespondToPrimitiveNode prev) {
            super(prev);
            respondToNode = prev.respondToNode;
        }

        @Specialization
        public boolean vmObjectRespondTo(VirtualFrame frame, Object object, Object name, boolean includePrivate) {
            return respondToNode.executeDoesRespondTo(frame, object, name, includePrivate);
        }

    }

    @RubiniusPrimitive(name = "vm_object_equal", needsSelf = false)
    public static abstract class VMObjectEqualPrimitiveNode extends RubiniusPrimitiveNode {

        public VMObjectEqualPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public VMObjectEqualPrimitiveNode(VMObjectEqualPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public Object vmObjectEqual(Object a, Object b) {
            throw new UnsupportedOperationException("vm_object_equal");
        }

    }

    @RubiniusPrimitive(name = "vm_get_module_name", needsSelf = false)
    public static abstract class VMGetModuleNamePrimitiveNode extends RubiniusPrimitiveNode {

        public VMGetModuleNamePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public VMGetModuleNamePrimitiveNode(VMGetModuleNamePrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public Object vmGetModuleName(Object object) {
            throw new UnsupportedOperationException("vm_get_module_name");
        }

    }

    @RubiniusPrimitive(name = "vm_set_module_name", needsSelf = false)
    public static abstract class VMSetModuleNamePrimitiveNode extends RubiniusPrimitiveNode {

        public VMSetModuleNamePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public VMSetModuleNamePrimitiveNode(VMSetModuleNamePrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public Object vmSetModuleName(Object object) {
            throw new UnsupportedOperationException("vm_set_module_name");
        }

    }

    @RubiniusPrimitive(name = "encoding_get_object_encoding", needsSelf = false)
    public static abstract class EncodingGetObjectEncodingPrimitiveNode extends RubiniusPrimitiveNode {

        public EncodingGetObjectEncodingPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EncodingGetObjectEncodingPrimitiveNode(EncodingGetObjectEncodingPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public Object encodingGetObjectEncoding(Object object) {
            throw new UnsupportedOperationException("encoding_get_object_encoding");
        }

    }

    @RubiniusPrimitive(name = "object_infect", needsSelf = false)
    public static abstract class ObjectInfectPrimitiveNode extends RubiniusPrimitiveNode {

        public ObjectInfectPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ObjectInfectPrimitiveNode(ObjectInfectPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public Object objectInfect(Object object) {
            throw new UnsupportedOperationException("object_infect");
        }

    }

}
