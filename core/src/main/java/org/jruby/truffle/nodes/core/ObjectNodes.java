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
import com.oracle.truffle.api.dsl.Generic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.common.IRubyWarnings;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.array.RubyArray;
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

        @Specialization
        public boolean equal(Object a, Object b) {
            // TODO(CS): placeholder
            return a.equals(b);
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

        @Specialization
        public boolean equal(Object a, Object b) {
            // TODO(CS): placeholder
            return a.equals(b);
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

        @Specialization
        public boolean equal(Object a, Object b) {
            // TODO(CS): placeholder
            return !a.equals(b);
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
            return self.dup();
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
            return self.frozen;
        }

    }

    @CoreMethod(names = "inspect", maxArgs = 0)
    public abstract static class InspectNode extends CoreMethodNode {

        public InspectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InspectNode(InspectNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString inspect(boolean value) {
            return getContext().makeString(Boolean.toString(value));
        }

        @Specialization
        public RubyString inspect(int value) {
            return getContext().makeString(Integer.toString(value));
        }

        @Specialization
        public RubyString inspect(BigInteger value) {
            return getContext().makeString(value.toString());
        }

        @Specialization
        public RubyString inspect(double value) {
            return getContext().makeString(Double.toString(value));
        }

        @Specialization
        public RubyString inspect(RubyObject self) {
            return getContext().makeString(self.inspect());
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
            if (receiver instanceof RubyFixnum || receiver instanceof RubySymbol) {
                throw new RaiseException(getContext().getCoreLibrary().typeError("no class to make alias"));
            }

            return block.callWithModifiedSelf(receiver);
        }

        @Specialization
        public Object instanceEval(VirtualFrame frame, Object self, RubyProc block) {
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
            return object.isFieldDefined(RubyObject.checkInstanceVariableName(getContext(), name.toString()));
        }

        @Specialization
        public boolean isInstanceVariableDefined(RubyBasicObject object, RubySymbol name) {
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
            return object.getInstanceVariable(RubyObject.checkInstanceVariableName(getContext(), name.toString()));
        }

        @Specialization
        public Object isInstanceVariableGet(RubyBasicObject object, RubySymbol name) {
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
            object.setInstanceVariable(RubyObject.checkInstanceVariableName(getContext(), name.toString()), value);
            return value;
        }

        @Specialization
        public Object isInstanceVariableSet(RubyBasicObject object, RubySymbol name, Object value) {
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
            final String[] instanceVariableNames = self.getFieldNames();

            Arrays.sort(instanceVariableNames);

            final RubyArray array = new RubyArray(getContext().getCoreLibrary().getArrayClass());

            for (String name : instanceVariableNames) {
                array.push(new RubyString(getContext().getCoreLibrary().getStringClass(), name));
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
            // TODO(CS): fast path
            return getContext().getCoreLibrary().box(self).getRubyClass().assignableTo(rubyClass);
        }

    }

    @CoreMethod(names = "methods", appendCallNode = true, minArgs = 1, maxArgs = 2)
    public abstract static class MethodsNode extends CoreMethodNode {

        public MethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MethodsNode(MethodsNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public RubyArray methods(RubyObject self, boolean includeInherited, Node callNode) {
            if (!includeInherited) {
                getContext().getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, callNode.getSourceSection().getSource().getName(), callNode.getSourceSection().getStartLine(), "Object#methods always returns inherited methods at the moment");
            }

            return methods(self, callNode, UndefinedPlaceholder.INSTANCE);
        }

        @Specialization(order = 2)
        public RubyArray methods(RubyObject self, @SuppressWarnings("unused") Node callNode, @SuppressWarnings("unused") UndefinedPlaceholder includeInherited) {
            final RubyArray array = new RubyArray(self.getRubyClass().getContext().getCoreLibrary().getArrayClass());

            final Map<String, RubyMethod> methods = new HashMap<>();

            self.getLookupNode().getMethods(methods);

            for (RubyMethod method : methods.values()) {
                if (method.getVisibility() == Visibility.PUBLIC || method.getVisibility() == Visibility.PROTECTED) {
                    array.push(self.getRubyClass().getContext().newSymbol(method.getName()));
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
            return object.getObjectID();
        }

    }

    @CoreMethod(names = "public_methods", appendCallNode = true, minArgs = 1, maxArgs = 2)
    public abstract static class PublicMethodsNode extends CoreMethodNode {

        public PublicMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PublicMethodsNode(PublicMethodsNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public RubyArray methods(RubyObject self, boolean includeInherited, Node callNode) {
            if (!includeInherited) {
                getContext().getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, callNode.getSourceSection().getSource().getName(), callNode.getSourceSection().getStartLine(), "Object#methods always returns inherited methods at the moment");
            }

            return methods(self, callNode, UndefinedPlaceholder.INSTANCE);
        }

        @Specialization(order = 2)
        public RubyArray methods(RubyObject self, @SuppressWarnings("unused") Node callNode, @SuppressWarnings("unused") UndefinedPlaceholder includeInherited) {
            final RubyArray array = new RubyArray(self.getRubyClass().getContext().getCoreLibrary().getArrayClass());

            final Map<String, RubyMethod> methods = new HashMap<>();

            self.getLookupNode().getMethods(methods);

            for (RubyMethod method : methods.values()) {
                if (method.getVisibility() == Visibility.PUBLIC) {
                    array.push(self.getRubyClass().getContext().newSymbol(method.getName()));
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
            return doesRespondTo(getContext().getCoreLibrary().box(object), name.toString(), false);
        }

        @Specialization(order = 2)
        public boolean doesRespondTo(Object object, RubyString name, boolean dontCheckVisibility) {
            return doesRespondTo(getContext().getCoreLibrary().box(object), name.toString(), dontCheckVisibility);
        }

        @Specialization(order = 3)
        public boolean doesRespondTo(Object object, RubySymbol name, @SuppressWarnings("unused") UndefinedPlaceholder checkVisibility) {
            return doesRespondTo(getContext().getCoreLibrary().box(object), name.toString(), false);
        }

        @Specialization(order = 4)
        public boolean doesRespondTo(Object object, RubySymbol name, boolean dontCheckVisibility) {
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
            return getContext().getCoreLibrary().box(self).getSingletonClass();
        }

    }

    @CoreMethod(names = "singleton_methods", appendCallNode = true, minArgs = 1, maxArgs = 2)
    public abstract static class SingletonMethodsNode extends CoreMethodNode {

        public SingletonMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SingletonMethodsNode(SingletonMethodsNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public RubyArray singletonMethods(RubyObject self, boolean includeInherited, Node callNode) {
            if (!includeInherited) {
                getContext().getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, callNode.getSourceSection().getSource().getName(), callNode.getSourceSection().getStartLine(), "Object#singleton_methods always returns inherited methods at the moment");
            }

            return singletonMethods(self, callNode, UndefinedPlaceholder.INSTANCE);
        }

        @Specialization(order = 2)
        public RubyArray singletonMethods(RubyObject self, Node callNode, @SuppressWarnings("unused") UndefinedPlaceholder includeInherited) {
            final RubyArray array = new RubyArray(self.getRubyClass().getContext().getCoreLibrary().getArrayClass());

            for (RubyMethod method : self.getSingletonClass().getDeclaredMethods()) {
                array.push(RubySymbol.newSymbol(self.getRubyClass().getContext(), method.getName()));
            }

            return array;
        }

    }

    @CoreMethod(names = "to_s", maxArgs = 0)
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSNode(ToSNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString toS(Object self) {
            return getContext().makeString(self.toString());
        }

    }
}
