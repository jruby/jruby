/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.supercall;

import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.ProcOrNullNode;
import org.jruby.truffle.nodes.cast.ProcOrNullNodeGen;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.methods.CallMethodNode;
import org.jruby.truffle.nodes.methods.CallMethodNodeGen;
import org.jruby.truffle.nodes.methods.DeclarationContext;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.array.ArrayUtils;
import org.jruby.truffle.runtime.core.StringOperations;
import org.jruby.truffle.runtime.methods.InternalMethod;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

public class SuperCallNode extends RubyNode {

    @Child private RubyNode arguments;
    @Child private RubyNode block;
    @Child ProcOrNullNode procOrNullNode;

    @Child LookupSuperMethodNode lookupSuperMethodNode;
    @Child CallMethodNode callMethodNode;
    @Child CallDispatchHeadNode callMethodMissingNode;

    public SuperCallNode(RubyContext context, SourceSection sourceSection, RubyNode arguments, RubyNode block) {
        super(context, sourceSection);
        this.arguments = arguments;
        this.block = block;
        this.procOrNullNode = ProcOrNullNodeGen.create(context, sourceSection, null);
        this.lookupSuperMethodNode = LookupSuperMethodNodeGen.create(context, sourceSection, null);
        this.callMethodNode = CallMethodNodeGen.create(context, sourceSection, null, new RubyNode[] {});
    }

    @ExplodeLoop
    @Override
    public final Object execute(VirtualFrame frame) {
        final Object self = RubyArguments.getSelf(frame.getArguments());

        // Execute the arguments
        final Object[] superArguments = (Object[]) arguments.execute(frame);

        // Execute the block
        final DynamicObject blockObject = procOrNullNode.executeProcOrNull(block.execute(frame));

        final InternalMethod superMethod = lookupSuperMethodNode.executeLookupSuperMethod(frame, self);

        if (superMethod == null) {
            CompilerDirectives.transferToInterpreter();
            final String name = RubyArguments.getMethod(frame.getArguments()).getSharedMethodInfo().getName(); // use the original name
            final Object[] methodMissingArguments = ArrayUtils.unshift(superArguments, getContext().getSymbol(name));
            return callMethodMissing(frame, self, blockObject, methodMissingArguments);
        }

        final Object[] frameArguments = RubyArguments.pack(superMethod, null, null, self, blockObject, DeclarationContext.METHOD, null, superArguments);

        return callMethodNode.executeCallMethod(frame, superMethod, frameArguments);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        final Object self = RubyArguments.getSelf(frame.getArguments());
        final InternalMethod superMethod = lookupSuperMethodNode.executeLookupSuperMethod(frame, self);

        if (superMethod == null) {
            return nil();
        } else {
            return create7BitString(StringOperations.encodeByteList("super", UTF8Encoding.INSTANCE));
        }
    }

    private Object callMethodMissing(VirtualFrame frame, Object receiver, DynamicObject block, Object[] arguments) {
        if (callMethodMissingNode == null) {
            CompilerDirectives.transferToInterpreter();
            callMethodMissingNode = insert(DispatchHeadNodeFactory.createMethodCallOnSelf(getContext()));
        }
        return callMethodMissingNode.call(frame, receiver, "method_missing", block, arguments);
    }

}
