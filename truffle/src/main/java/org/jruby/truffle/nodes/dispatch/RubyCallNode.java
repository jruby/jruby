/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.dispatch;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.arguments.OptionalKeywordArgMissingNode;
import org.jruby.truffle.nodes.arguments.UnknownArgumentErrorNode;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeGen;
import org.jruby.truffle.nodes.cast.ProcOrNullNode;
import org.jruby.truffle.nodes.cast.ProcOrNullNodeGen;
import org.jruby.truffle.nodes.core.hash.HashLiteralNode;
import org.jruby.truffle.nodes.literal.ObjectLiteralNode;
import org.jruby.truffle.nodes.methods.MarkerNode;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.array.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

public class RubyCallNode extends RubyNode {

    private final String methodName;

    @Child private RubyNode receiver;
    @Child private ProcOrNullNode block;
    @Children private final RubyNode[] arguments;
    @Children private final RubyNode[] keywordOptimizedArguments;
    @CompilerDirectives.CompilationFinal private int keywordOptimizedArgumentsLength;

    private final boolean isSplatted;
    private final boolean isVCall;

    @Child private CallDispatchHeadNode dispatchHead;

    @CompilerDirectives.CompilationFinal private boolean seenNullInUnsplat = false;
    @CompilerDirectives.CompilationFinal private boolean seenIntegerFixnumInUnsplat = false;
    @CompilerDirectives.CompilationFinal private boolean seenLongFixnumInUnsplat = false;
    @CompilerDirectives.CompilationFinal private boolean seenFloatInUnsplat = false;
    @CompilerDirectives.CompilationFinal private boolean seenObjectInUnsplat = false;

    @Child private CallDispatchHeadNode respondToMissing;
    @Child private BooleanCastNode respondToMissingCast;

    private final boolean ignoreVisibility;

    @CompilerDirectives.CompilationFinal private boolean cannotOptimize;

    public RubyCallNode(RubyContext context, SourceSection section, String methodName, RubyNode receiver, RubyNode block, boolean isSplatted, RubyNode... arguments) {
        this(context, section, methodName, receiver, block, isSplatted, false, arguments);
    }

    public RubyCallNode(RubyContext context, SourceSection section, String methodName, RubyNode receiver, RubyNode block, boolean isSplatted, boolean ignoreVisibility, RubyNode... arguments) {
        this(context, section, methodName, receiver, block, isSplatted, ignoreVisibility, false, arguments);
    }

    public RubyCallNode(RubyContext context, SourceSection section, String methodName, RubyNode receiver, RubyNode block, boolean isSplatted, boolean ignoreVisibility, boolean isVCall, RubyNode... arguments) {
        super(context, section);

        this.methodName = methodName;

        this.receiver = receiver;

        if (block == null) {
            this.block = null;
        } else {
            this.block = ProcOrNullNodeGen.create(context, section, block);
        }

        this.arguments = arguments;
        this.isSplatted = isSplatted;
        this.isVCall = isVCall;

        dispatchHead = DispatchHeadNodeFactory.createMethodCall(context, ignoreVisibility, false, MissingBehavior.CALL_METHOD_MISSING);
        respondToMissing = DispatchHeadNodeFactory.createMethodCall(context, true, MissingBehavior.RETURN_MISSING);
        respondToMissingCast = BooleanCastNodeGen.create(context, section, null);

        this.ignoreVisibility = ignoreVisibility;

        /*
         * TODO CS 19-Mar-15 we currently can't swap an @Children array out
         * so we just allocate a lot up-front. In a future version of Truffle
         * @Children might not need to be final, which would fix this.
         */
        keywordOptimizedArguments = new RubyNode[arguments.length + 32];
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object receiverObject = receiver.execute(frame);

        final Object[] argumentsObjects;

        if (dispatchHead.getFirstDispatchNode().couldOptimizeKeywordArguments() && !cannotOptimize) {
            final CachedBoxedDispatchNode dispatchNode = (CachedBoxedDispatchNode) dispatchHead.getFirstDispatchNode();

            if (keywordOptimizedArguments[0] == null) {
                CompilerDirectives.transferToInterpreter();

                System.err.println("optimizing for keyword arguments!");

                final RubyNode[] optimized = expandedArgumentNodes(dispatchNode.getMethod(), arguments, isSplatted);

                if (optimized == null || optimized.length > keywordOptimizedArguments.length) {
                    System.err.println("couldn't optimize :(");
                    cannotOptimize = true;
                } else {
                    keywordOptimizedArgumentsLength = optimized.length;

                    for (int n = 0; n < keywordOptimizedArgumentsLength; n++) {
                        keywordOptimizedArguments[n] = insert(NodeUtil.cloneNode(optimized[n]));
                    }
                }
            }

            if (dispatchNode.guard(methodName, receiverObject) && dispatchNode.getUnmodifiedAssumption().isValid()) {
                argumentsObjects = executeKeywordOptimizedArguments(frame);
            } else {
                argumentsObjects = executeArguments(frame);
            }

        } else {
            argumentsObjects = executeArguments(frame);
        }

        final RubyProc blockObject = executeBlock(frame);

        return dispatchHead.call(frame, receiverObject, methodName, blockObject, argumentsObjects);
    }

    private RubyProc executeBlock(VirtualFrame frame) {
        if (block != null) {
            return block.executeRubyProc(frame);
        } else {
            return null;
        }
    }

    @ExplodeLoop
    private Object[] executeArguments(VirtualFrame frame) {
        final Object[] argumentsObjects = new Object[arguments.length];

        for (int i = 0; i < arguments.length; i++) {
            argumentsObjects[i] = arguments[i].execute(frame);
        }

        if (isSplatted) {
            return splat(argumentsObjects[0]);
        } else {
            return argumentsObjects;
        }
    }

    @ExplodeLoop
    private Object[] executeKeywordOptimizedArguments(VirtualFrame frame) {
        final Object[] argumentsObjects = new Object[keywordOptimizedArgumentsLength];

        for (int i = 0; i < keywordOptimizedArgumentsLength; i++) {
            argumentsObjects[i] = keywordOptimizedArguments[i].execute(frame);
        }

        if (isSplatted) {
            return splat(argumentsObjects[0]);
        } else {
            return argumentsObjects;
        }
    }

    private Object[] splat(Object argument) {
        // TODO(CS): what happens if isn't just one argument, or it isn't an Array?

        if (!(argument instanceof RubyArray)) {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException(argument.toString());
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
        } else if (seenFloatInUnsplat && store instanceof double[]) {
            return ArrayUtils.boxUntil((double[]) store, size);
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
        } else if (store instanceof double[]) {
            seenFloatInUnsplat = true;
            return ArrayUtils.boxUntil((double[]) store, size);
        } else if (store instanceof Object[]) {
            seenObjectInUnsplat = true;
            return ArrayUtils.extractRange((Object[]) store, 0, size);
        }

        throw new UnsupportedOperationException();
    }

    public RubyNode[] expandedArgumentNodes(InternalMethod method, RubyNode[] argumentNodes, boolean isSplatted) {
        final RubyNode[] result;

        boolean shouldExpand = true;
        if (method == null
                || method.getSharedMethodInfo().getArity().getKeywordArguments() == null) {
            // no keyword arguments in method definition
            shouldExpand = false;
        } else if (argumentNodes.length != 0
                && !(argumentNodes[argumentNodes.length - 1] instanceof HashLiteralNode)) {
            // last argument is not a Hash that could be expanded
            shouldExpand = false;
        } else if (method.getSharedMethodInfo().getArity() == null
                || method.getSharedMethodInfo().getArity().getRequired() >= argumentNodes.length) {
            shouldExpand = false;
        } else if (isSplatted
                || method.getSharedMethodInfo().getArity().allowsMore()) {
            // TODO: make optimization work if splat arguments are involed
            // the problem is that Markers and keyword args are used when
            // reading splatted args
            shouldExpand = false;
        }

        if (shouldExpand) {
            List<String> kwargs = method.getSharedMethodInfo().getArity().getKeywordArguments();

            int countArgNodes = argumentNodes.length + kwargs.size() + 1;
            if (argumentNodes.length == 0) {
                countArgNodes++;
            }

            result = new RubyNode[countArgNodes];
            int i;

            for (i = 0; i < argumentNodes.length - 1; ++i) {
                result[i] = argumentNodes[i];
            }

            int firstMarker = i++;
            result[firstMarker] = new MarkerNode(getContext(), null);

            HashLiteralNode hashNode;
            if (argumentNodes.length > 0) {
                hashNode = (HashLiteralNode) argumentNodes[argumentNodes.length - 1];
            } else {
                hashNode = HashLiteralNode.create(getContext(), null,
                        new RubyNode[0]);
            }

            List<String> restKeywordLabels = new ArrayList<>();
            for (int j = 0; j < hashNode.size(); j++) {
                Object key = hashNode.getKey(j);
                boolean keyIsSymbol = key instanceof ObjectLiteralNode &&
                        ((ObjectLiteralNode) key).getObject() instanceof RubySymbol;

                if (!keyIsSymbol) {
                    // cannot optimize case where keyword label is dynamic (not a fixed RubySymbol)
                    cannotOptimize = true;
                    return null;
                }

                final String label = ((ObjectLiteralNode) hashNode.getKey(j)).getObject().toString();
                restKeywordLabels.add(label);
            }

            for (String kwarg : kwargs) {
                result[i] = new OptionalKeywordArgMissingNode(getContext(), null);
                for (int j = 0; j < hashNode.size(); j++) {
                    final String label = ((ObjectLiteralNode) hashNode.getKey(j)).getObject().toString();

                    if (label.equals(kwarg)) {
                        result[i] = hashNode.getValue(j);
                        restKeywordLabels.remove(label);
                        break;
                    }
                }
                i++;
            }
            result[i++] = new MarkerNode(getContext(), null);

            if (restKeywordLabels.size() > 0
                    && !method.getSharedMethodInfo().getArity().hasKeyRest()) {
                result[firstMarker] = new UnknownArgumentErrorNode(getContext(), null, restKeywordLabels.get(0));
            } else if (restKeywordLabels.size() > 0) {
                i = 0;
                RubyNode[] keyValues = new RubyNode[2 * restKeywordLabels
                        .size()];

                for (String label : restKeywordLabels) {
                    for (int j = 0; j < hashNode.size(); j++) {
                        final String argLabel = ((ObjectLiteralNode) hashNode.getKey(j)).getObject().toString();

                        if (argLabel.equals(label)) {
                            keyValues[i++] = hashNode.getKey(j);
                            keyValues[i++] = hashNode.getValue(j);
                        }
                    }
                }

                HashLiteralNode restHash = HashLiteralNode.create(getContext(), null, keyValues);
                result[firstMarker] = restHash;
            }

        }
        else {
            cannotOptimize = true;
            result = null;
        }

        return result;
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        if (receiver.isDefined(frame) == nil()) {
            return nil();
        }

        for (RubyNode argument : arguments) {
            if (argument.isDefined(frame) == nil()) {
                return nil();
            }
        }

        final RubyContext context = getContext();

        Object receiverObject;

        try {
            /*
             * TODO(CS): Getting a node via an accessor like this doesn't work with Truffle at the
             * moment and will cause frame escape errors, so we don't use it in compilation mode.
             */

            CompilerAsserts.neverPartOfCompilation();

            receiverObject = receiver.execute(frame);
        } catch (Exception e) {
            return nil();
        }

        // TODO(CS): this lookup should be cached

        final InternalMethod method = ModuleOperations.lookupMethod(context.getCoreLibrary().getMetaClass(receiverObject), methodName);

        final Object self = RubyArguments.getSelf(frame.getArguments());

        if (method == null) {
            final Object r = respondToMissing.call(frame, receiverObject, "respond_to_missing?", null, context.makeString(methodName));

            if (r != DispatchNode.MISSING && !respondToMissingCast.executeBoolean(frame, r)) {
                return nil();
            }
        } else if (method.isUndefined()) {
            return nil();
        } else if (!ignoreVisibility && !method.isVisibleTo(this, context.getCoreLibrary().getMetaClass(self))) {
            return nil();
        }

        return context.makeString("method");
    }

    public String getName() {
        return methodName;
    }

    public boolean isVCall() {
        return isVCall;
    }

}
