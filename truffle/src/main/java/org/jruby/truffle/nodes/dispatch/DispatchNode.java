/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.dispatch;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.ProcOrNullNode;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyConstant;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.util.ArrayUtils;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.utilities.BranchProfile;

public abstract class DispatchNode extends RubyNode {

    private final DispatchAction dispatchAction;
    
    protected final boolean isSplatted;
    private final BranchProfile splatNotArrayProfile = BranchProfile.create();
    
    @CompilerDirectives.CompilationFinal private boolean seenNullInUnsplat = false;
    @CompilerDirectives.CompilationFinal private boolean seenIntegerFixnumInUnsplat = false;
    @CompilerDirectives.CompilationFinal private boolean seenLongFixnumInUnsplat = false;
    @CompilerDirectives.CompilationFinal private boolean seenFloatInUnsplat = false;
    @CompilerDirectives.CompilationFinal private boolean seenObjectInUnsplat = false;

    @Children protected final RubyNode[] argumentNodes;
    @Child protected ProcOrNullNode block;

    private static final class Missing {
    }

    public static final Object MISSING = new Missing();

    public DispatchNode(RubyContext context, DispatchAction dispatchAction, RubyNode[] argumentNodes, ProcOrNullNode block, boolean isSplatted) {
        super(context, null);
        this.dispatchAction = dispatchAction;
        this.argumentNodes = argumentNodes;
        this.block = block;
        this.isSplatted = isSplatted;
        assert dispatchAction != null;
    }

    public DispatchNode(DispatchNode prev) {
        super(prev);
        argumentNodes = prev.getHeadNode().getArgumentNodes();
        block = prev.getHeadNode().getBlock();
        isSplatted = prev.isSplatted;
        dispatchAction = prev.dispatchAction;
    }

    public abstract Object executeDispatch(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects);

    @CompilerDirectives.TruffleBoundary
    protected RubyConstant lookupConstant(
            RubyModule module,
            String name,
            boolean ignoreVisibility) {
        final LexicalScope lexicalScope = getHeadNode().getLexicalScope();

        RubyConstant constant = ModuleOperations.lookupConstant(getContext(), lexicalScope, module, name);

        // If no constant was found, use #const_missing
        if (constant == null) {
            return null;
        }

        if (!ignoreVisibility && !constant.isVisibleTo(getContext(), lexicalScope, module)) {
            throw new RaiseException(getContext().getCoreLibrary().nameErrorPrivateConstant(module, name, this));
        }

        return constant;
    }

    @CompilerDirectives.TruffleBoundary
    protected InternalMethod lookup(
            RubyClass callerClass,
            Object receiver,
            String name,
            boolean ignoreVisibility) {
        InternalMethod method = ModuleOperations.lookupMethod(getContext().getCoreLibrary().getMetaClass(receiver), name);

        // If no method was found, use #method_missing

        if (method == null) {
            return null;
        }

        // Check for methods that are explicitly undefined

        if (method.isUndefined()) {
            throw new RaiseException(getContext().getCoreLibrary().noMethodError(name, getContext().getCoreLibrary().getLogicalClass(receiver), this));
        }

        // Check visibility

        if (!ignoreVisibility && !method.isVisibleTo(this, callerClass)) {
            final DispatchAction dispatchAction = getHeadNode().getDispatchAction();

            if (dispatchAction == DispatchAction.CALL_METHOD) {
                throw new RaiseException(getContext().getCoreLibrary().privateMethodError(name, getContext().getCoreLibrary().getLogicalClass(receiver), this));
            } else if (dispatchAction == DispatchAction.RESPOND_TO_METHOD) {
                return null;
            } else {
                throw new UnsupportedOperationException();
            }
        }

        return method;
    }

    protected Object resetAndDispatch(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            RubyProc blockObject,
            Object argumentsObjects,
            String reason) {
        final DispatchHeadNode head = getHeadNode();
        head.reset(reason);
        return head.dispatch(
                frame,
                receiverObject,
                methodName,
                blockObject,
                argumentsObjects);
    }

    protected DispatchHeadNode getHeadNode() {
        return NodeUtil.findParent(this, DispatchHeadNode.class);
    }

    public final Object execute(VirtualFrame frame) {
        throw new IllegalStateException("do not call execute on dispatch nodes");
    }

    public DispatchAction getDispatchAction() {
        return dispatchAction;
    }
    
    private Object[] splat(Object argument) {
        // TODO(CS): what happens if isn't just one argument, or it isn't an Array?

        if (!(argument instanceof RubyArray)) {
            splatNotArrayProfile.enter();
            notDesignedForCompilation();
            throw new UnsupportedOperationException();
        }

        final RubyArray array = (RubyArray) argument;
        final int size = array.getSize();
        final Object store = array.getStore();

        if (seenNullInUnsplat && store == null) {
            return new Object[]{};
        } else if (seenIntegerFixnumInUnsplat && store instanceof int[]) {
            return ArrayUtils.boxUntil((int[]) store, size);
        } else if (seenLongFixnumInUnsplat && store instanceof long[]) {
            return ArrayUtils.boxUntil((long[]) store, size);
        } else if (seenObjectInUnsplat && store instanceof Object[]) {
            return ArrayUtils.extractRange((Object[]) store, 0, size);
        }

        CompilerDirectives.transferToInterpreterAndInvalidate();

        if (store == null) {
            seenNullInUnsplat = true;
            return new Object[]{};
        } else if (store instanceof int[]) {
            seenIntegerFixnumInUnsplat = true;
            return ArrayUtils.boxUntil((int[]) store, size);
        } else if (store instanceof long[]) {
            seenLongFixnumInUnsplat = true;
            return ArrayUtils.boxUntil((long[]) store, size);
        } else if (store instanceof Object[]) {
            seenObjectInUnsplat = true;
            return ArrayUtils.extractRange((Object[]) store, 0, size);
        }

        throw new UnsupportedOperationException();
    }
    
    @ExplodeLoop
    protected Object executeArguments(VirtualFrame frame, Object argumentOverride) {
       if (argumentOverride != null) {
           return argumentOverride;
       }
       
        final Object[] argumentsObjects = new Object[argumentNodes.length];

        for (int i = 0; i < argumentNodes.length; i++) {
            argumentsObjects[i] = argumentNodes[i].execute(frame);
        }

        if (isSplatted) {
            return splat(argumentsObjects[0]);
        } else {
            return argumentsObjects;
        }
    }
    
    protected Object executeBlock(VirtualFrame frame, Object blockOverride) {
        if (blockOverride != null) {
            return blockOverride;
        }
        
        if (block != null) {
            return block.executeRubyProc(frame);
        } else {
            return null;
        }
    }
    
    public boolean isSplatted() {
       return isSplatted();
    }

}
