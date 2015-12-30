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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.nodes.RubyGuards;
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
import org.jruby.truffle.runtime.core.ArrayOperations;
import org.jruby.truffle.runtime.core.StringOperations;
import org.jruby.truffle.runtime.methods.InternalMethod;

import java.util.Arrays;

/**
 * Represents a super call with implicit arguments (using the ones of the surrounding methods).
 */
public class GeneralSuperReCallNode extends RubyNode {

    private final boolean hasRestParameter;
    @Children private final RubyNode[] reloadNodes;
    @Child private RubyNode block;

    @Child ProcOrNullNode procOrNullNode;
    @Child LookupSuperMethodNode lookupSuperMethodNode;
    @Child CallMethodNode callMethodNode;

    @Child CallDispatchHeadNode callMethodMissingNode;

    public GeneralSuperReCallNode(RubyContext context, SourceSection sourceSection, boolean hasRestParameter, RubyNode[] reloadNodes, RubyNode block) {
        super(context, sourceSection);
        this.hasRestParameter = hasRestParameter;
        this.reloadNodes = reloadNodes;
        this.block = block;

        procOrNullNode = ProcOrNullNodeGen.create(context, sourceSection, null);
        lookupSuperMethodNode = LookupSuperMethodNodeGen.create(context, sourceSection, null);
        callMethodNode = CallMethodNodeGen.create(context, sourceSection, null, new RubyNode[] {});
    }

    @ExplodeLoop
    @Override
    public final Object execute(VirtualFrame frame) {
        CompilerAsserts.compilationConstant(reloadNodes.length);

        final Object self = RubyArguments.getSelf(frame.getArguments());

        final Object[] originalArguments = frame.getArguments();

        // Reload the arguments
        Object[] superArguments = new Object[reloadNodes.length];
        for (int n = 0; n < superArguments.length; n++) {
            superArguments[n] = reloadNodes[n].execute(frame);
        }

        if (hasRestParameter) {
            CompilerDirectives.transferToInterpreter();
            // TODO (eregon, 22 July 2015): Assumes rest arg is last, not true if post or keyword args.
            final Object restArg = superArguments[superArguments.length - 1];
            assert RubyGuards.isRubyArray(restArg);
            final Object[] restArgs = ArrayOperations.toObjectArray((DynamicObject) restArg);
            final int restArgIndex = reloadNodes.length - 1;
            superArguments = Arrays.copyOf(superArguments, restArgIndex + restArgs.length);
            ArrayUtils.arraycopy(restArgs, 0, superArguments, restArgIndex, restArgs.length);
        }

        // Execute or inherit the block
        final DynamicObject blockObject;
        if (block != null) {
            blockObject = procOrNullNode.executeProcOrNull(block.execute(frame));
        } else {
            blockObject = RubyArguments.getBlock(originalArguments);
        }

        final InternalMethod superMethod = lookupSuperMethodNode.executeLookupSuperMethod(frame, self);

        if (superMethod == null) {
            CompilerDirectives.transferToInterpreter();
            final String name = RubyArguments.getMethod(frame.getArguments()).getSharedMethodInfo().getName(); // use the original name
            final Object[] methodMissingArguments = new Object[1 + superArguments.length];
            methodMissingArguments[0] = getContext().getSymbol(name);
            ArrayUtils.arraycopy(superArguments, 0, methodMissingArguments, 1, superArguments.length);
            return callMethodMissing(frame, self, blockObject, methodMissingArguments);
        }

        final Object[] frameArguments = RubyArguments.pack(
                superMethod,
                RubyArguments.getDeclarationFrame(originalArguments),
                null,
                self,
                blockObject,
                DeclarationContext.METHOD,
                superArguments);

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
