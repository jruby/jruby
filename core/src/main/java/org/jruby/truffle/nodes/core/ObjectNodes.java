/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.common.IRubyWarnings;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.methods.RubyMethod;
import org.jruby.truffle.runtime.core.RubyBasicObject;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@CoreClass(name = "Object")
public abstract class ObjectNodes {

    @CoreMethod(names = "===", minArgs = 1, maxArgs = 1)
    public abstract static class ThreeEqualNode extends CoreMethodNode {

        public ThreeEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ThreeEqualNode(ThreeEqualNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public boolean equal(@SuppressWarnings("unused") NilPlaceholder a, @SuppressWarnings("unused") NilPlaceholder b) {
            return true;
        }

        @Specialization(order = 2)
        public boolean equal(boolean a, boolean b) {
            return a == b;
        }

        @Specialization(order = 3)
        public boolean equal(int a, int b) {
            return a == b;
        }

        @Specialization(order = 4)
        public boolean equal(long a, long b) {
            return a == b;
        }

        @Specialization(order = 5)
        public boolean equal(double a, double b) {
            return a == b;
        }

        @Specialization(order = 6)
        public boolean equal(BigInteger a, BigInteger b) {
            return a.compareTo(b) == 0;
        }

        @Specialization(order = 7)
        public boolean equal(RubyBasicObject a, RubyBasicObject b) {
            return a == b;
        }

    }

    @CoreMethod(names = "=~", minArgs = 1, maxArgs = 1)
    public abstract static class MatchNode extends CoreMethodNode {

        public MatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MatchNode(MatchNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public boolean equal(@SuppressWarnings("unused") NilPlaceholder a, @SuppressWarnings("unused") NilPlaceholder b) {
            return true;
        }

        @Specialization(order = 2)
        public boolean equal(boolean a, boolean b) {
            return a == b;
        }

        @Specialization(order = 3)
        public boolean equal(int a, int b) {
            return a == b;
        }

        @Specialization(order = 4)
        public boolean equal(long a, long b) {
            return a == b;
        }

        @Specialization(order = 5)
        public boolean equal(double a, double b) {
            return a == b;
        }

        @Specialization(order = 6)
        public boolean equal(BigInteger a, BigInteger b) {
            return a.compareTo(b) == 0;
        }

        @Specialization(order = 7)
        public boolean equal(RubyBasicObject a, RubyBasicObject b) {
            return a == b;
        }

    }

    @CoreMethod(names = "!~", minArgs = 1, maxArgs = 1)
    public abstract static class NotMatchNode extends CoreMethodNode {

        public NotMatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NotMatchNode(NotMatchNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public boolean equal(@SuppressWarnings("unused") NilPlaceholder a, @SuppressWarnings("unused") NilPlaceholder b) {
            return true;
        }

        @Specialization(order = 2)
        public boolean equal(boolean a, boolean b) {
            return a != b;
        }

        @Specialization(order = 3)
        public boolean equal(int a, int b) {
            return a != b;
        }

        @Specialization(order = 4)
        public boolean equal(long a, long b) {
            return a != b;
        }

        @Specialization(order = 5)
        public boolean equal(double a, double b) {
            return a != b;
        }

        @Specialization(order = 6)
        public boolean equal(BigInteger a, BigInteger b) {
            return a.compareTo(b) != 0;
        }

        @Specialization(order = 7)
        public boolean equal(RubyBasicObject a, RubyBasicObject b) {
            return a != b;
        }

    }

    @CoreMethod(names = "class", maxArgs = 0)
    public abstract static class ClassNode extends CoreMethodNode {

        public ClassNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ClassNode(ClassNode prev) {
            super(prev);
        }

        @Specialization
        public RubyClass getClass(boolean value) {
            notDesignedForCompilation();

            if (value) {
                return getContext().getCoreLibrary().getTrueClass();
            } else {
                return getContext().getCoreLibrary().getFalseClass();
            }
        }

        @Specialization
        public RubyClass getClass(@SuppressWarnings("unused") int value) {
            return getContext().getCoreLibrary().getFixnumClass();
        }

        @Specialization
        public RubyClass getClass(@SuppressWarnings("unused") BigInteger value) {
            return getContext().getCoreLibrary().getBignumClass();
        }

        @Specialization
        public RubyClass getClass(@SuppressWarnings("unused") double value) {
            return getContext().getCoreLibrary().getFloatClass();
        }

        @Specialization
        public RubyClass getClass(RubyBasicObject self) {
            return self.getRubyClass();
        }

    }

    @CoreMethod(names = {"dup", "clone"}, maxArgs = 0)
    public abstract static class DupNode extends CoreMethodNode {

        public DupNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DupNode(DupNode prev) {
            super(prev);
        }

        @Specialization
        public Object dup(RubyObject self) {
            notDesignedForCompilation();

            final RubyObject newObject = new RubyObject(self.getRubyClass());
            newObject.setInstanceVariables(self.getFields());
            return newObject;
        }

    }

    @CoreMethod(names = "extend", isSplatted = true, minArgs = 1)
    public abstract static class ExtendNode extends CoreMethodNode {

        public ExtendNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ExtendNode(ExtendNode prev) {
            super(prev);
        }

        @Specialization
        public RubyBasicObject extend(RubyBasicObject self, Object[] args) {
            notDesignedForCompilation();

            for (int n = 0; n < args.length; n++) {
                self.extend((RubyModule) args[n]);
            }

            return self;
        }

    }

    @CoreMethod(names = "freeze", maxArgs = 0)
    public abstract static class FreezeNode extends CoreMethodNode {

        public FreezeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FreezeNode(FreezeNode prev) {
            super(prev);
        }

        @Specialization
        public RubyObject freeze(RubyObject self) {
            notDesignedForCompilation();

            self.frozen = true;
            return self;
        }

    }

    @CoreMethod(names = "frozen?", maxArgs = 0)
    public abstract static class FrozenNode extends CoreMethodNode {

        public FrozenNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FrozenNode(FrozenNode prev) {
            super(prev);
        }

        @Specialization
        public boolean isFrozen(RubyObject self) {
            notDesignedForCompilation();

            return self.frozen;
        }

    }

    @CoreMethod(names = "instance_eval", needsBlock = true, maxArgs = 0)
    public abstract static class InstanceEvalNode extends CoreMethodNode {

        public InstanceEvalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InstanceEvalNode(InstanceEvalNode prev) {
            super(prev);
        }

        @Specialization
        public Object instanceEval(VirtualFrame frame, RubyBasicObject receiver, RubyProc block) {
            notDesignedForCompilation();

            if (receiver instanceof RubyFixnum || receiver instanceof RubySymbol) {
                throw new RaiseException(getContext().getCoreLibrary().typeError("no class to make alias"));
            }

            return block.callWithModifiedSelf(receiver);
        }

        @Specialization
        public Object instanceEval(VirtualFrame frame, Object self, RubyProc block) {
            notDesignedForCompilation();

            return instanceEval(frame, getContext().getCoreLibrary().box(self), block);
        }

    }

    @CoreMethod(names = "instance_variable_defined?", minArgs = 1, maxArgs = 1)
    public abstract static class InstanceVariableDefinedNode extends CoreMethodNode {

        public InstanceVariableDefinedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InstanceVariableDefinedNode(InstanceVariableDefinedNode prev) {
            super(prev);
        }

        @Specialization
        public boolean isInstanceVariableDefined(RubyBasicObject object, RubyString name) {
            notDesignedForCompilation();

            return object.isFieldDefined(RubyObject.checkInstanceVariableName(getContext(), name.toString()));
        }

        @Specialization
        public boolean isInstanceVariableDefined(RubyBasicObject object, RubySymbol name) {
            notDesignedForCompilation();

            return object.isFieldDefined(RubyObject.checkInstanceVariableName(getContext(), name.toString()));
        }

    }

    @CoreMethod(names = "instance_variable_get", minArgs = 1, maxArgs = 1)
    public abstract static class InstanceVariableGetNode extends CoreMethodNode {

        public InstanceVariableGetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InstanceVariableGetNode(InstanceVariableGetNode prev) {
            super(prev);
        }

        @Specialization
        public Object isInstanceVariableGet(RubyBasicObject object, RubyString name) {
            notDesignedForCompilation();

            return object.getInstanceVariable(RubyObject.checkInstanceVariableName(getContext(), name.toString()));
        }

        @Specialization
        public Object isInstanceVariableGet(RubyBasicObject object, RubySymbol name) {
            notDesignedForCompilation();

            return object.getInstanceVariable(RubyObject.checkInstanceVariableName(getContext(), name.toString()));
        }

    }

    @CoreMethod(names = "instance_variable_set", minArgs = 2, maxArgs = 2)
    public abstract static class InstanceVariableSetNode extends CoreMethodNode {

        public InstanceVariableSetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InstanceVariableSetNode(InstanceVariableSetNode prev) {
            super(prev);
        }

        @Specialization
        public Object isInstanceVariableSet(RubyBasicObject object, RubyString name, Object value) {
            notDesignedForCompilation();

            object.setInstanceVariable(RubyObject.checkInstanceVariableName(getContext(), name.toString()), value);
            return value;
        }

        @Specialization
        public Object isInstanceVariableSet(RubyBasicObject object, RubySymbol name, Object value) {
            notDesignedForCompilation();

            object.setInstanceVariable(RubyObject.checkInstanceVariableName(getContext(), name.toString()), value);
            return value;
        }

    }

    @CoreMethod(names = "instance_variables", maxArgs = 0)
    public abstract static class InstanceVariablesNode extends CoreMethodNode {

        public InstanceVariablesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InstanceVariablesNode(InstanceVariablesNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray instanceVariables(RubyObject self) {
            notDesignedForCompilation();

            final String[] instanceVariableNames = self.getFieldNames();

            Arrays.sort(instanceVariableNames);

            final RubyArray array = new RubyArray(getContext().getCoreLibrary().getArrayClass());

            for (String name : instanceVariableNames) {
                array.slowPush(RubyString.fromJavaString(getContext().getCoreLibrary().getStringClass(), name));
            }

            return array;
        }

    }

    @CoreMethod(names = {"is_a?", "instance_of?", "kind_of?"}, minArgs = 1, maxArgs = 1)
    public abstract static class IsANode extends CoreMethodNode {

        public IsANode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IsANode(IsANode prev) {
            super(prev);
        }

        @Specialization
        public boolean isA(@SuppressWarnings("unused") RubyBasicObject self, @SuppressWarnings("unused") NilPlaceholder nil) {
            return false;
        }

        @Specialization
        public boolean isA(Object self, RubyClass rubyClass) {
            notDesignedForCompilation();

            // TODO(CS): fast path
            return getContext().getCoreLibrary().box(self).getRubyClass().assignableTo(rubyClass);
        }

    }

    @CoreMethod(names = "methods", maxArgs = 1)
    public abstract static class MethodsNode extends CoreMethodNode {

        public MethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MethodsNode(MethodsNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public RubyArray methods(RubyObject self, boolean includeInherited) {
            notDesignedForCompilation();

            if (!includeInherited) {
                getContext().getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, RubyCallStack.getCallerFrame().getCallNode().getEncapsulatingSourceSection().getSource().getName(), RubyCallStack.getCallerFrame().getCallNode().getEncapsulatingSourceSection().getStartLine(), "Object#methods always returns inherited methods at the moment");
            }

            return methods(self, UndefinedPlaceholder.INSTANCE);
        }

        @Specialization(order = 2)
        public RubyArray methods(RubyObject self, @SuppressWarnings("unused") UndefinedPlaceholder includeInherited) {
            notDesignedForCompilation();

            final RubyArray array = new RubyArray(self.getRubyClass().getContext().getCoreLibrary().getArrayClass());

            final Map<String, RubyMethod> methods = new HashMap<>();

            self.getLookupNode().getMethods(methods);

            for (RubyMethod method : methods.values()) {
                if (method.getVisibility() == Visibility.PUBLIC || method.getVisibility() == Visibility.PROTECTED) {
                    array.slowPush(self.getRubyClass().getContext().newSymbol(method.getName()));
                }
            }

            return array;
        }

    }

    @CoreMethod(names = "nil?", needsSelf = false, maxArgs = 0)
    public abstract static class NilNode extends CoreMethodNode {

        public NilNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NilNode(NilNode prev) {
            super(prev);
        }

        @Specialization
        public boolean nil() {
            return false;
        }
    }

    @CoreMethod(names = "object_id", needsSelf = true, maxArgs = 0)
    public abstract static class ObjectIDNode extends CoreMethodNode {

        public ObjectIDNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ObjectIDNode(ObjectIDNode prev) {
            super(prev);
        }

        @Specialization
        public long objectID(RubyBasicObject object) {
            notDesignedForCompilation();

            return object.getObjectID();
        }

    }

    @CoreMethod(names = "public_methods", maxArgs = 1)
    public abstract static class PublicMethodsNode extends CoreMethodNode {

        public PublicMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PublicMethodsNode(PublicMethodsNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public RubyArray methods(RubyObject self, boolean includeInherited) {
            notDesignedForCompilation();

            if (!includeInherited) {
                getContext().getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, RubyCallStack.getCallerFrame().getCallNode().getEncapsulatingSourceSection().getSource().getName(), RubyCallStack.getCallerFrame().getCallNode().getEncapsulatingSourceSection().getStartLine(), "Object#methods always returns inherited methods at the moment");
            }

            return methods(self, UndefinedPlaceholder.INSTANCE);
        }

        @Specialization(order = 2)
        public RubyArray methods(RubyObject self, @SuppressWarnings("unused") UndefinedPlaceholder includeInherited) {
            notDesignedForCompilation();

            final RubyArray array = new RubyArray(self.getRubyClass().getContext().getCoreLibrary().getArrayClass());

            final Map<String, RubyMethod> methods = new HashMap<>();

            self.getLookupNode().getMethods(methods);

            for (RubyMethod method : methods.values()) {
                if (method.getVisibility() == Visibility.PUBLIC) {
                    array.slowPush(self.getRubyClass().getContext().newSymbol(method.getName()));
                }
            }

            return array;
        }

    }

    @CoreMethod(names = "respond_to?", minArgs = 1, maxArgs = 2)
    public abstract static class RespondToNode extends CoreMethodNode {

        public RespondToNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RespondToNode(RespondToNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public boolean doesRespondTo(Object object, RubyString name, @SuppressWarnings("unused") UndefinedPlaceholder checkVisibility) {
            notDesignedForCompilation();

            return doesRespondTo(getContext().getCoreLibrary().box(object), name.toString(), false);
        }

        @Specialization(order = 2)
        public boolean doesRespondTo(Object object, RubyString name, boolean dontCheckVisibility) {
            notDesignedForCompilation();

            return doesRespondTo(getContext().getCoreLibrary().box(object), name.toString(), dontCheckVisibility);
        }

        @Specialization(order = 3)
        public boolean doesRespondTo(Object object, RubySymbol name, @SuppressWarnings("unused") UndefinedPlaceholder checkVisibility) {
            notDesignedForCompilation();

            return doesRespondTo(getContext().getCoreLibrary().box(object), name.toString(), false);
        }

        @Specialization(order = 4)
        public boolean doesRespondTo(Object object, RubySymbol name, boolean dontCheckVisibility) {
            notDesignedForCompilation();

            return doesRespondTo(getContext().getCoreLibrary().box(object), name.toString(), dontCheckVisibility);
        }

        private static boolean doesRespondTo(RubyBasicObject object, String name, boolean dontCheckVisibility) {
            final RubyMethod method = object.getLookupNode().lookupMethod(name);

            if (method == null || method.isUndefined()) {
                return false;
            }

            if (dontCheckVisibility) {
                return true;
            } else {
                return method.getVisibility() == Visibility.PUBLIC;
            }
        }

    }

    @CoreMethod(names = "respond_to_missing?", minArgs = 1, maxArgs = 2)
    public abstract static class RespondToMissingNode extends CoreMethodNode {

        public RespondToMissingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RespondToMissingNode(RespondToMissingNode prev) {
            super(prev);
        }

        @Specialization
        public boolean doesRespondToMissing(Object object, RubySymbol name, boolean includeAll) {
            return false;
        }

        @Specialization
        public boolean doesRespondToMissing(Object object, RubyString name, boolean includeAll) {
            return false;
        }

    }

    @CoreMethod(names = "singleton_class", maxArgs = 0)
    public abstract static class SingletonClassMethodNode extends CoreMethodNode {

        public SingletonClassMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SingletonClassMethodNode(SingletonClassMethodNode prev) {
            super(prev);
        }

        @Specialization
        public RubyClass singletonClass(Object self) {
            notDesignedForCompilation();

            return getContext().getCoreLibrary().box(self).getSingletonClass();
        }

    }

    @CoreMethod(names = "singleton_methods", maxArgs = 1)
    public abstract static class SingletonMethodsNode extends CoreMethodNode {

        public SingletonMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SingletonMethodsNode(SingletonMethodsNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public RubyArray singletonMethods(RubyObject self, boolean includeInherited) {
            notDesignedForCompilation();

            if (!includeInherited) {
                getContext().getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, RubyCallStack.getCallerFrame().getCallNode().getEncapsulatingSourceSection().getSource().getName(), RubyCallStack.getCallerFrame().getCallNode().getEncapsulatingSourceSection().getStartLine(), "Object#singleton_methods always returns inherited methods at the moment");
            }

            return singletonMethods(self, UndefinedPlaceholder.INSTANCE);
        }

        @Specialization(order = 2)
        public RubyArray singletonMethods(RubyObject self, @SuppressWarnings("unused") UndefinedPlaceholder includeInherited) {
            notDesignedForCompilation();

            final RubyArray array = new RubyArray(self.getRubyClass().getContext().getCoreLibrary().getArrayClass());

            for (RubyMethod method : self.getSingletonClass().getDeclaredMethods()) {
                array.slowPush(RubySymbol.newSymbol(self.getRubyClass().getContext(), method.getName()));
            }

            return array;
        }

    }

    @CoreMethod(names = {"to_s", "inspect"}, maxArgs = 0)
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSNode(ToSNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString toS(RubyBasicObject self) {
            notDesignedForCompilation();

            return getContext().makeString("#<" + self.getRubyClass().getName() + ":0x" + Long.toHexString(self.getObjectID()) + ">");
        }

    }
}
