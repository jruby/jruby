/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.rubinius;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;
import org.jruby.truffle.language.objects.IsTaintedNode;
import org.jruby.truffle.language.objects.IsTaintedNodeGen;
import org.jruby.truffle.language.objects.ObjectIDOperations;
import org.jruby.truffle.language.objects.ReadObjectFieldNode;
import org.jruby.truffle.language.objects.ReadObjectFieldNodeGen;
import org.jruby.truffle.language.objects.TaintNode;
import org.jruby.truffle.language.objects.TaintNodeGen;
import org.jruby.truffle.language.objects.WriteObjectFieldNode;
import org.jruby.truffle.language.objects.WriteObjectFieldNodeGen;

/**
 * Rubinius primitives associated with the Ruby {@code Object} class.
 */
public abstract class ObjectPrimitiveNodes {

    @Primitive(name = "object_id")
    public abstract static class ObjectIDPrimitiveNode extends PrimitiveArrayArgumentsNode {

        public abstract Object executeObjectID(VirtualFrame frame, Object value);

        @Specialization(guards = "isNil(nil)")
        public long objectID(Object nil) {
            return ObjectIDOperations.NIL;
        }

        @Specialization(guards = "value")
        public long objectIDTrue(boolean value) {
            return ObjectIDOperations.TRUE;
        }

        @Specialization(guards = "!value")
        public long objectIDFalse(boolean value) {
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

        @Specialization
        public Object objectID(long value,
                               @Cached("createCountingProfile()") ConditionProfile smallProfile) {
            if (smallProfile.profile(ObjectIDOperations.isSmallFixnum(value))) {
                return ObjectIDOperations.smallFixnumToID(value);
            } else {
                return ObjectIDOperations.largeFixnumToID(getContext(), value);
            }
        }

        @Specialization
        public Object objectID(double value) {
            return ObjectIDOperations.floatToID(getContext(), value);
        }

        @Specialization(guards = "!isNil(object)")
        public long objectID(DynamicObject object,
                @Cached("createReadObjectIDNode()") ReadObjectFieldNode readObjectIdNode,
                @Cached("createWriteObjectIDNode()") WriteObjectFieldNode writeObjectIdNode) {
            final long id = (long) readObjectIdNode.execute(object);

            if (id == 0) {
                final long newId = getContext().getObjectSpaceManager().getNextObjectID();
                writeObjectIdNode.execute(object, newId);
                return newId;
            }

            return id;
        }

        protected ReadObjectFieldNode createReadObjectIDNode() {
            return ReadObjectFieldNodeGen.create(Layouts.OBJECT_ID_IDENTIFIER, 0L);
        }

        protected WriteObjectFieldNode createWriteObjectIDNode() {
            return WriteObjectFieldNodeGen.create(Layouts.OBJECT_ID_IDENTIFIER);
        }

    }

    @Primitive(name = "object_infect", needsSelf = false)
    public static abstract class ObjectInfectPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private IsTaintedNode isTaintedNode;
        @Child private TaintNode taintNode;
        
        public ObjectInfectPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object objectInfect(Object host, Object source) {
            if (isTaintedNode == null) {
                CompilerDirectives.transferToInterpreter();
                isTaintedNode = insert(IsTaintedNodeGen.create(getContext(), getSourceSection(), null));
            }
            
            if (isTaintedNode.executeIsTainted(source)) {
                // This lazy node allocation effectively gives us a branch profile
                
                if (taintNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    taintNode = insert(TaintNodeGen.create(getContext(), getSourceSection(), null));
                }
                
                taintNode.executeTaint(host);
            }
            
            return host;
        }

    }

}
