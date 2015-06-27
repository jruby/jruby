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
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.methods.CallMethodNode;
import org.jruby.truffle.nodes.methods.CallMethodNodeGen;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.methods.InternalMethod;

/**
 * Represents a super call with implicit arguments (using the ones of the surrounding methods).
 */
public class GeneralSuperReCallNode extends RubyNode {

    private final boolean inBlock;
    private final boolean isSplatted;
    @Children private final RubyNode[] reloadNodes;
    @Child private RubyNode block;

    @Child LookupSuperMethodNode lookupSuperMethodNode;
    @Child CallMethodNode callMethodNode;

    public GeneralSuperReCallNode(RubyContext context, SourceSection sourceSection, boolean inBlock, boolean isSplatted, RubyNode[] reloadNodes, RubyNode block) {
        super(context, sourceSection);
        this.inBlock = inBlock;
        this.isSplatted = isSplatted;
        this.reloadNodes = reloadNodes;
        this.block = block;

        lookupSuperMethodNode = LookupSuperMethodNodeGen.create(context, sourceSection, null);
        callMethodNode = CallMethodNodeGen.create(context, sourceSection, null, new RubyNode[] {});
    }

    @ExplodeLoop
    @Override
    public final Object execute(VirtualFrame frame) {
        CompilerAsserts.compilationConstant(reloadNodes.length);

        final Object self = RubyArguments.getSelf(frame.getArguments());

        final Object[] originalArguments;
        if (inBlock) {
            originalArguments = RubyArguments.getDeclarationFrame(frame.getArguments()).getArguments();
        } else {
            originalArguments = frame.getArguments();
        }

        // Reload the arguments
        Object[] superArguments = new Object[reloadNodes.length];
        for (int n = 0; n < superArguments.length; n++) {
            superArguments[n] = reloadNodes[n].execute(frame);
        }

        if (isSplatted) {
            CompilerDirectives.transferToInterpreter();
            assert superArguments.length == 1;
            assert superArguments[0] instanceof RubyArray;
            superArguments = ArrayNodes.slowToArray(((RubyArray) superArguments[0]));
        }

        // Execute or inherit the block
        final RubyProc blockObject;
        if (block != null) {
            final Object blockTempObject = block.execute(frame);
            if (blockTempObject == nil()) {
                blockObject = null;
            } else {
                blockObject = (RubyProc) blockTempObject;
            }
        } else {
            blockObject = RubyArguments.getBlock(originalArguments);
        }

        final InternalMethod superMethod = lookupSuperMethodNode.executeLookupSuperMethod(frame, self);

        if (superMethod == null) {
            CompilerDirectives.transferToInterpreter();
            final String name = RubyArguments.getMethod(frame.getArguments()).getSharedMethodInfo().getName(); // use the original name
            throw new RaiseException(getContext().getCoreLibrary().noMethodError(String.format("super: no superclass method `%s'", name), name, this));
        }

        final Object[] frameArguments = RubyArguments.pack(
                superMethod,
                RubyArguments.getDeclarationFrame(originalArguments),
                RubyArguments.getSelf(originalArguments),
                blockObject,
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
            return createString("super");
        }
    }

}
