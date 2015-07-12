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
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.methods.InternalMethod;

/**
 * Represents a super call with explicit arguments.
 */
public class GeneralSuperCallNode extends RubyNode {

    private final boolean isSplatted;

    @Child private RubyNode block;
    @Children private final RubyNode[] arguments;

    @Child LookupSuperMethodNode lookupSuperMethodNode;
    @Child CallMethodNode callMethodNode;

    public GeneralSuperCallNode(RubyContext context, SourceSection sourceSection, RubyNode block, RubyNode[] arguments, boolean isSplatted) {
        super(context, sourceSection);
        assert arguments != null;
        assert !isSplatted || arguments.length == 1;
        this.block = block;
        this.arguments = arguments;
        this.isSplatted = isSplatted;

        lookupSuperMethodNode = LookupSuperMethodNodeGen.create(context, sourceSection, null);
        callMethodNode = CallMethodNodeGen.create(context, sourceSection, null, new RubyNode[] {});
    }

    @ExplodeLoop
    @Override
    public final Object execute(VirtualFrame frame) {
        CompilerAsserts.compilationConstant(arguments.length);

        final Object self = RubyArguments.getSelf(frame.getArguments());

        // Execute the arguments
        final Object[] argumentsObjects = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            argumentsObjects[i] = arguments[i].execute(frame);
        }

        // Execute the block
        final RubyBasicObject blockObject;
        if (block != null) {
            final Object blockTempObject = block.execute(frame);
            if (blockTempObject == nil()) {
                blockObject = null;
            } else {
                blockObject = (RubyBasicObject) blockTempObject;
            }
        } else {
            blockObject = null;
        }

        final Object[] argumentsArray;
        if (isSplatted) {
            // TODO(CS): need something better to splat the arguments array
            argumentsArray = ArrayNodes.slowToArray((RubyBasicObject) argumentsObjects[0]);
        } else {
            argumentsArray = argumentsObjects;
        }

        final InternalMethod superMethod = lookupSuperMethodNode.executeLookupSuperMethod(frame, self);

        if (superMethod == null) {
            CompilerDirectives.transferToInterpreter();
            final String name = RubyArguments.getMethod(frame.getArguments()).getSharedMethodInfo().getName(); // use the original name
            throw new RaiseException(getContext().getCoreLibrary().noMethodError(String.format("super: no superclass method `%s'", name), name, this));
        }

        final Object[] frameArguments = RubyArguments.pack(superMethod, superMethod.getDeclarationFrame(), self, blockObject, argumentsArray);

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
