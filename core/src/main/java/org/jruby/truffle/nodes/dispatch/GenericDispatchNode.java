/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.dispatch;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import org.jruby.common.IRubyWarnings;
import org.jruby.truffle.nodes.cast.BoxingNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyConstant;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.RubyMethod;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.util.cli.Options;

import java.util.HashMap;
import java.util.Map;

public abstract class GenericDispatchNode extends DispatchNode {

    private final boolean ignoreVisibility;

    private final Map<MethodCacheKey, ConstantCacheEntry> constantCache;
    @CompilerDirectives.CompilationFinal private boolean hasAnyConstantsMissing = false;

    private final Map<MethodCacheKey, MethodCacheEntry> methodCache;
    @CompilerDirectives.CompilationFinal private boolean hasAnyMethodsMissing = false;

    @Child protected IndirectCallNode callNode;
    @Child protected BoxingNode box;

    @CompilerDirectives.CompilationFinal private boolean hasSeenSymbolAsMethodName = false;
    @CompilerDirectives.CompilationFinal private boolean hasSeenRubyStringAsMethodName = false;
    @CompilerDirectives.CompilationFinal private boolean hasSeenJavaStringAsMethodName = false;

    public GenericDispatchNode(RubyContext context, boolean ignoreVisibility) {
        super(context);
        this.ignoreVisibility = ignoreVisibility;
        constantCache = new HashMap<>();
        methodCache = new HashMap<>();
        callNode = Truffle.getRuntime().createIndirectCallNode();
        box = new BoxingNode(context, null, null);
    }

    public GenericDispatchNode(GenericDispatchNode prev) {
        super(prev);
        ignoreVisibility = prev.ignoreVisibility;
        constantCache = prev.constantCache;
        hasAnyConstantsMissing = prev.hasAnyConstantsMissing;
        methodCache = prev.methodCache;
        hasAnyMethodsMissing = prev.hasAnyMethodsMissing;
        callNode = prev.callNode;
        box = prev.box;
    }

    @Specialization
    public Object dispatch(
            VirtualFrame frame,
            Object methodReceiverObject,
            LexicalScope lexicalScope,
            RubyBasicObject receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects,
            Dispatch.DispatchAction dispatchAction) {
        CompilerAsserts.compilationConstant(dispatchAction);

        if (dispatchAction == Dispatch.DispatchAction.CALL_METHOD || dispatchAction == Dispatch.DispatchAction.RESPOND_TO_METHOD) {
            MethodCacheEntry entry = lookupInCache(receiverObject.getMetaClass(), methodName);

            if (entry == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();

                final RubyClass callerClass = box.box(RubyArguments.getSelf(frame.getArguments())).getMetaClass();

                final RubyMethod method = lookup(callerClass, receiverObject, methodName.toString(),
                        ignoreVisibility, dispatchAction);

                if (method == null) {
                    final RubyMethod missingMethod = lookup(callerClass, receiverObject, "method_missing", true,
                            dispatchAction);

                    if (missingMethod == null) {
                        if (dispatchAction == Dispatch.DispatchAction.RESPOND_TO_METHOD) {
                            // TODO(CS): we should methodCache the fact that we would throw an exception - this will miss each time
                            getContext().getRuntime().getWarnings().warn(
                                    IRubyWarnings.ID.TRUFFLE,
                                    getEncapsulatingSourceSection().getSource().getName(),
                                    getEncapsulatingSourceSection().getStartLine(),
                                    "Lack of method_missing isn't cached and is being used for respond");
                            return false;
                        } else {
                            throw new RaiseException(getContext().getCoreLibrary().runtimeError(
                                    receiverObject.toString() + " didn't have a #method_missing", this));
                        }
                    }

                    entry = new MethodCacheEntry(missingMethod, true);
                } else {
                    entry = new MethodCacheEntry(method, false);
                }

                if (entry.isMethodMissing()) {
                    hasAnyMethodsMissing = true;
                }

                if (methodCache.size() <= Options.TRUFFLE_DISPATCH_MEGAMORPHIC_MAX.load()) {
                    methodCache.put(new MethodCacheKey(receiverObject.getMetaClass(), methodName), entry);
                }
            }

            if (dispatchAction == Dispatch.DispatchAction.CALL_METHOD) {
                final Object[] argumentsObjectsArray = CompilerDirectives.unsafeCast(argumentsObjects, Object[].class, true);

                final Object[] argumentsToUse;

                // TODO(CS): where is the assumption check?!

                if (hasAnyMethodsMissing && entry.isMethodMissing()) {
                    final Object[] modifiedArgumentsObjects = new Object[1 + argumentsObjectsArray.length];

                    final RubySymbol methodNameAsSymbol;

                    if (hasSeenSymbolAsMethodName && methodName instanceof RubySymbol) {
                        methodNameAsSymbol = (RubySymbol) methodName;
                    } else if (hasSeenRubyStringAsMethodName && methodName instanceof RubyString) {
                        methodNameAsSymbol = getContext().newSymbol(((RubyString) methodName).getBytes());
                    } else if (hasSeenJavaStringAsMethodName && methodName instanceof String) {
                        methodNameAsSymbol = getContext().newSymbol((String) methodName);
                    } else {
                        CompilerDirectives.transferToInterpreterAndInvalidate();

                        if (methodName instanceof RubySymbol) {
                            hasSeenSymbolAsMethodName = true;
                            methodNameAsSymbol = (RubySymbol) methodName;
                        } else if (methodName instanceof RubyString) {
                            hasSeenRubyStringAsMethodName = true;
                            methodNameAsSymbol = getContext().newSymbol(((RubyString) methodName).getBytes());
                        } else if (methodName instanceof String) {
                            hasSeenJavaStringAsMethodName = true;
                            methodNameAsSymbol = getContext().newSymbol((String) methodName);
                        } else {
                            throw new UnsupportedOperationException();
                        }
                    }

                    modifiedArgumentsObjects[0] = methodNameAsSymbol;

                    System.arraycopy(argumentsObjectsArray, 0, modifiedArgumentsObjects, 1, argumentsObjectsArray.length);
                    argumentsToUse = modifiedArgumentsObjects;
                } else {
                    argumentsToUse = argumentsObjectsArray;
                }

                return callNode.call(
                        frame,
                        entry.getMethod().getCallTarget(),
                        RubyArguments.pack(
                                entry.getMethod(),
                                entry.getMethod().getDeclarationFrame(),
                                receiverObject,
                                CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                                argumentsToUse));
            } else if (dispatchAction == Dispatch.DispatchAction.RESPOND_TO_METHOD) {
                if (hasAnyMethodsMissing) {
                    return !entry.isMethodMissing();
                } else {
                    return true;
                }
            } else {
                throw new UnsupportedOperationException();
            }
        } else if (dispatchAction == Dispatch.DispatchAction.READ_CONSTANT) {
            final RubyModule module = (RubyModule) receiverObject;

            ConstantCacheEntry entry = lookupInConstantCache(module, methodName);

            if (entry == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();

                final RubyConstant constant = lookupConstant(lexicalScope, module,
                        methodName.toString(), ignoreVisibility, dispatchAction);

                if (constant == null) {
                    final RubyClass callerClass = box.box(RubyArguments.getSelf(frame.getArguments())).getMetaClass();

                    final RubyMethod missingMethod = lookup(callerClass, module, "const_missing", ignoreVisibility,
                            dispatchAction);

                    if (missingMethod == null) {
                        throw new RaiseException(getContext().getCoreLibrary().runtimeError(
                                module.toString() + " didn't have a #const_missing", this));
                    }

                    entry = new ConstantCacheEntry(null, missingMethod, true);
                } else {
                    entry = new ConstantCacheEntry(constant.getValue(), null, false);
                }

                if (entry.isConstantMissing()) {
                    hasAnyConstantsMissing = true;
                }

                if (constantCache.size() <= Options.TRUFFLE_DISPATCH_MEGAMORPHIC_MAX.load()) {
                    //constantCache.put(new MethodCacheKey(receiverObject.getMetaClass(), methodName), entry);
                }
            }
            
            // TODO(CS): where is the assumption check?!

            if (dispatchAction == Dispatch.DispatchAction.READ_CONSTANT) {
                if (hasAnyConstantsMissing && entry.isConstantMissing()) {
                    final RubySymbol methodNameAsSymbol;

                    if (hasSeenSymbolAsMethodName && methodName instanceof RubySymbol) {
                        methodNameAsSymbol = (RubySymbol) methodName;
                    } else if (hasSeenRubyStringAsMethodName && methodName instanceof RubyString) {
                        methodNameAsSymbol = getContext().newSymbol(((RubyString) methodName).getBytes());
                    } else if (hasSeenJavaStringAsMethodName && methodName instanceof String) {
                        methodNameAsSymbol = getContext().newSymbol((String) methodName);
                    } else {
                        CompilerDirectives.transferToInterpreterAndInvalidate();

                        if (methodName instanceof RubySymbol) {
                            hasSeenSymbolAsMethodName = true;
                            methodNameAsSymbol = (RubySymbol) methodName;
                        } else if (methodName instanceof RubyString) {
                            hasSeenRubyStringAsMethodName = true;
                            methodNameAsSymbol = getContext().newSymbol(((RubyString) methodName).getBytes());
                        } else if (methodName instanceof String) {
                            hasSeenJavaStringAsMethodName = true;
                            methodNameAsSymbol = getContext().newSymbol((String) methodName);
                        } else {
                            throw new UnsupportedOperationException();
                        }
                    }

                    return callNode.call(
                            frame,
                            entry.getMethod().getCallTarget(),
                            RubyArguments.pack(
                                    entry.getMethod(),
                                    entry.getMethod().getDeclarationFrame(),
                                    receiverObject,
                                    null,
                                    new Object[]{methodNameAsSymbol}));
                } else {
                    return entry.getValue();
                }
            } else {
                throw new UnsupportedOperationException();
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Specialization
    public Object dispatch(
            VirtualFrame frame,
            Object methodReceiverObject,
            LexicalScope lexicalScope,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects,
            Dispatch.DispatchAction dispatchAction) {
        return dispatch(
                frame,
                methodReceiverObject,
                lexicalScope,
                box.box(receiverObject),
                methodName,
                CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                CompilerDirectives.unsafeCast(argumentsObjects, Object[].class, true, true),
                dispatchAction);
    }

    @CompilerDirectives.TruffleBoundary
    public ConstantCacheEntry lookupInConstantCache(RubyModule module, Object methodName) {
        return constantCache.get(new MethodCacheKey(module.getSingletonClass(null), methodName));
    }

    @CompilerDirectives.TruffleBoundary
    public MethodCacheEntry lookupInCache(RubyClass metaClass, Object methodName) {
        return methodCache.get(new MethodCacheKey(metaClass, methodName));
    }

    private static class MethodCacheKey {

        private final RubyClass metaClass;
        private final Object methodName;

        private MethodCacheKey(RubyClass metaClass, Object methodName) {
            this.metaClass = metaClass;
            this.methodName = methodName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MethodCacheKey that = (MethodCacheKey) o;

            if (!metaClass.equals(that.metaClass)) return false;
            if (!methodName.equals(that.methodName)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = metaClass.hashCode();
            result = 31 * result + methodName.hashCode();
            return result;
        }

    }

    private static class ConstantCacheEntry {

        private final Object value;
        private final RubyMethod method;
        private final boolean constantMissing;

        private ConstantCacheEntry(Object value, RubyMethod method, boolean constantMissing) {
            this.value = value;
            this.method = method;
            this.constantMissing = constantMissing;
        }

        public Object getValue() {
            return value;
        }

        public RubyMethod getMethod() {
            return method;
        }

        public boolean isConstantMissing() {
            return constantMissing;
        }

    }

    private static class MethodCacheEntry {

        private final RubyMethod method;
        private final boolean methodMissing;

        private MethodCacheEntry(RubyMethod method, boolean methodMissing) {
            assert method != null;
            this.method = method;
            this.methodMissing = methodMissing;
        }

        public RubyMethod getMethod() {
            return method;
        }

        public boolean isMethodMissing() {
            return methodMissing;
        }
    }

}