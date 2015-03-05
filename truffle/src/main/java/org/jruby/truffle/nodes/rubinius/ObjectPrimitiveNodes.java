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

import org.jruby.truffle.nodes.core.FixnumOrBignumNode;
import org.jruby.truffle.nodes.objects.IsTaintedNode;
import org.jruby.truffle.nodes.objects.IsTaintedNodeFactory;
import org.jruby.truffle.nodes.objects.TaintNode;
import org.jruby.truffle.nodes.objects.TaintNodeFactory;
import org.jruby.truffle.runtime.ObjectIDOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyNilClass;

/**
 * Rubinius primitives associated with the Ruby {@code Object} class.
 */
public abstract class ObjectPrimitiveNodes {

    @RubiniusPrimitive(name = "object_id")
    public abstract static class ObjectIDPrimitiveNode extends RubiniusPrimitiveNode {

        public ObjectIDPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ObjectIDPrimitiveNode(ObjectIDPrimitiveNode prev) {
            super(prev);
        }

        public abstract Object executeObjectID(VirtualFrame frame, Object value);

        @Specialization
        public int objectID(RubyNilClass nil) {
            return ObjectIDOperations.NIL;
        }

        @Specialization(guards = "isTrue(value)")
        public int objectIDTrue(boolean value) {
            return ObjectIDOperations.TRUE;
        }

        @Specialization(guards = "!isTrue(value)")
        public int objectIDFalse(boolean value) {
            return ObjectIDOperations.FALSE;
        }

        @Specialization
        public long objectID(int value) {
            return ObjectIDOperations.smallFixnumToID(value);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long objectIDSmallFixnumOverflow(long value) throws ArithmeticException {
            return ObjectIDOperations.smallFixnumToIDOverflow(value);
        }

        /* TODO: Ideally we would have this instead of the code below to speculate better. [GRAAL-903]
        @Specialization(guards = "isSmallFixnum")
        public long objectIDSmallFixnum(long value) {
            return ObjectIDOperations.smallFixnumToID(value);
        }

        @Specialization(guards = "!isSmallFixnum")
        public Object objectIDLargeFixnum(long value) {
            return ObjectIDOperations.largeFixnumToID(getContext(), value);
        } */

        @Specialization
        public Object objectID(long value) {
            if (isSmallFixnum(value)) {
                return ObjectIDOperations.smallFixnumToID(value);
            } else {
                return ObjectIDOperations.largeFixnumToID(getContext(), value);
            }
        }

        @Specialization
        public Object objectID(double value) {
            return ObjectIDOperations.floatToID(getContext(), value);
        }

        @Specialization
        public long objectID(RubyBasicObject object) {
            return object.getObjectID();
        }

        protected boolean isSmallFixnum(long fixnum) {
            return ObjectIDOperations.isSmallFixnum(fixnum);
        }

    }

    @RubiniusPrimitive(name = "object_infect", needsSelf = false)
    public static abstract class ObjectInfectPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private IsTaintedNode isTaintedNode;
        @Child private TaintNode taintNode;
        
        public ObjectInfectPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ObjectInfectPrimitiveNode(ObjectInfectPrimitiveNode prev) {
            super(prev);
            isTaintedNode = prev.isTaintedNode;
            taintNode = prev.taintNode;
        }

        @Specialization
        public Object objectInfect(Object host, Object source) {
            if (isTaintedNode == null) {
                CompilerDirectives.transferToInterpreter();
                isTaintedNode = insert(IsTaintedNodeFactory.create(getContext(), getSourceSection(), null));
            }
            
            if (isTaintedNode.executeIsTainted(source)) {
                // This lazy node allocation effectively gives us a branch profile
                
                if (taintNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    taintNode = insert(TaintNodeFactory.create(getContext(), getSourceSection(), null));
                }
                
                taintNode.executeTaint(host);
            }
            
            return host;
        }

    }

}
