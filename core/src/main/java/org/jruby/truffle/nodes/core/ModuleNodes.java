/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.*;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.dispatch.Dispatch;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeFactory;
import org.jruby.truffle.nodes.control.SequenceNode;
import org.jruby.truffle.nodes.methods.arguments.CheckArityNode;
import org.jruby.truffle.nodes.methods.arguments.MissingArgumentBehaviour;
import org.jruby.truffle.nodes.methods.arguments.ReadPreArgumentNode;
import org.jruby.truffle.nodes.objects.ReadInstanceVariableNode;
import org.jruby.truffle.nodes.objects.SelfNode;
import org.jruby.truffle.nodes.objects.WriteInstanceVariableNode;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.methods.*;
import org.jruby.truffle.translator.TranslatorDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@CoreClass(name = "Module")
public abstract class ModuleNodes {

    @CoreMethod(names = "===", required = 1)
    public abstract static class ContainsInstanceNode extends CoreMethodNode {

        public ContainsInstanceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ContainsInstanceNode(ContainsInstanceNode prev) {
            super(prev);
        }

        @Specialization
        public boolean containsInstance(RubyModule module, RubyBasicObject instance) {
            notDesignedForCompilation();

            return ModuleOperations.includesModule(instance.getMetaClass(), module);
        }

        @Specialization
        public boolean containsInstance(RubyModule module, Object instance) {
            notDesignedForCompilation();

            return ModuleOperations.includesModule(getContext().getCoreLibrary().getMetaClass(instance), module);
        }
    }

    @CoreMethod(names = "<=", required = 1)
    public abstract static class IsSubclassOfNode extends CoreMethodNode {

        public IsSubclassOfNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IsSubclassOfNode(IsSubclassOfNode prev) {
            super(prev);
        }

        public abstract Object executeIsSubclassOf(VirtualFrame frame, RubyModule self, RubyModule other);

        @Specialization
        public Object isSubclassOf(VirtualFrame frame, RubyModule self, RubyModule other) {
            notDesignedForCompilation();

            if (self == other || ModuleOperations.includesModule(self, other)) {
                return true;
            }

            if (ModuleOperations.includesModule(other, self)) {
                return false;
            }

            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization
        public Object isSubclassOf(VirtualFrame frame, RubyModule self, RubyBasicObject other) {
            notDesignedForCompilation();

            throw new RaiseException(getContext().getCoreLibrary().typeError("compared with non class/module", this));
        }

    }

    @CoreMethod(names = "<=>", required = 1)
    public abstract static class CompareNode extends CoreMethodNode {

        @Child protected IsSubclassOfNode subclassNode;
        @Child protected BooleanCastNode booleanCastNode;

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CompareNode(CompareNode prev) {
            super(prev);
        }

        private Object isSubclass(VirtualFrame frame, RubyModule self, RubyModule other) {
            if (subclassNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                subclassNode = insert(ModuleNodesFactory.IsSubclassOfNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{null, null}));
            }
            return subclassNode.executeIsSubclassOf(frame, self, other);
        }

        private boolean booleanCast(VirtualFrame frame, Object value) {
            if (booleanCastNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                booleanCastNode = insert(BooleanCastNodeFactory.create(getContext(), getSourceSection(), null));
            }
            return booleanCastNode.executeBoolean(frame, value);
        }

        @Specialization
        public Object compare(VirtualFrame frame, RubyModule self, RubyModule other) {
            notDesignedForCompilation();

            if (self == other) {
                return 0;
            }

            final Object isSubclass = isSubclass(frame, self, other);

            if (isSubclass instanceof RubyNilClass) {
                return getContext().getCoreLibrary().getNilObject();
            } else if (booleanCast(frame, isSubclass)) {
                return -1;
            }
            return 1;
        }

        @Specialization
        public Object compare(VirtualFrame frame, RubyModule self, RubyBasicObject other) {
            notDesignedForCompilation();

            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "alias_method", required = 2)
    public abstract static class AliasMethodNode extends CoreMethodNode {

        public AliasMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AliasMethodNode(AliasMethodNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule aliasMethod(RubyModule module, RubySymbol newName, RubySymbol oldName) {
            notDesignedForCompilation();

            module.alias(this, newName.toString(), oldName.toString());
            return module;
        }
    }

    @CoreMethod(names = "ancestors")
    public abstract static class AncestorsNode extends CoreMethodNode {

        public AncestorsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AncestorsNode(AncestorsNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray ancestors(RubyModule self) {
            notDesignedForCompilation();

            final List<RubyModule> ancestors = new ArrayList<>();
            for (RubyModule module : self.ancestors()) {
                ancestors.add(module);
            }

            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), ancestors.toArray(new Object[ancestors.size()]));
        }
    }

    @CoreMethod(names = "append_features", required = 1)
    public abstract static class AppendFeaturesNode extends CoreMethodNode {

        public AppendFeaturesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AppendFeaturesNode(AppendFeaturesNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass appendFeatures(RubyModule module, RubyModule other) {
            notDesignedForCompilation();

            module.appendFeatures(this, other);
            return getContext().getCoreLibrary().getNilObject();
        }
    }

    @CoreMethod(names = "attr_reader", argumentsAsArray = true)
    public abstract static class AttrReaderNode extends CoreMethodNode {

        public AttrReaderNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AttrReaderNode(AttrReaderNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass attrReader(RubyModule module, Object[] args) {
            notDesignedForCompilation();

            final SourceSection sourceSection = Truffle.getRuntime().getCallerFrame().getCallNode().getEncapsulatingSourceSection();

            for (Object arg : args) {
                final String accessorName;

                if (arg instanceof RubySymbol) {
                    accessorName = ((RubySymbol) arg).toString();
                } else {
                    throw new UnsupportedOperationException();
                }

                attrReader(this, getContext(), sourceSection, module, accessorName);
            }

            return getContext().getCoreLibrary().getNilObject();
        }

        public static void attrReader(RubyNode currentNode, RubyContext context, SourceSection sourceSection, RubyModule module, String name) {
            CompilerDirectives.transferToInterpreter();

            final CheckArityNode checkArity = new CheckArityNode(context, sourceSection, new Arity(0, 0, false, false));

            final SelfNode self = new SelfNode(context, sourceSection);
            final ReadInstanceVariableNode readInstanceVariable = new ReadInstanceVariableNode(context, sourceSection, "@" + name, self, false);

            final RubyNode block = SequenceNode.sequence(context, sourceSection, checkArity, readInstanceVariable);

            final String indicativeName = name + "(attr_reader)";

            final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, null, indicativeName, false, null, false);
            final RubyRootNode rootNode = new RubyRootNode(context, sourceSection, null, sharedMethodInfo, block);
            final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
            final RubyMethod method = new RubyMethod(sharedMethodInfo, name, module, Visibility.PUBLIC, false, callTarget, null);
            module.addMethod(currentNode, method);
        }
    }

    @CoreMethod(names = "attr_writer", argumentsAsArray = true)
    public abstract static class AttrWriterNode extends CoreMethodNode {

        public AttrWriterNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AttrWriterNode(AttrWriterNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass attrWriter(RubyModule module, Object[] args) {
            notDesignedForCompilation();

            final SourceSection sourceSection = Truffle.getRuntime().getCallerFrame().getCallNode().getEncapsulatingSourceSection();

            for (Object arg : args) {
                final String accessorName;

                if (arg instanceof RubySymbol) {
                    accessorName = ((RubySymbol) arg).toString();
                } else {
                    throw new UnsupportedOperationException();
                }

                attrWriter(this, getContext(), sourceSection, module, accessorName);
            }

            return getContext().getCoreLibrary().getNilObject();
        }

        public static void attrWriter(RubyNode currentNode, RubyContext context, SourceSection sourceSection, RubyModule module, String name) {
            CompilerDirectives.transferToInterpreter();

            final CheckArityNode checkArity = new CheckArityNode(context, sourceSection, new Arity(1, 0, false, false));

            final SelfNode self = new SelfNode(context, sourceSection);
            final ReadPreArgumentNode readArgument = new ReadPreArgumentNode(context, sourceSection, 0, MissingArgumentBehaviour.RUNTIME_ERROR);
            final WriteInstanceVariableNode writeInstanceVariable = new WriteInstanceVariableNode(context, sourceSection, "@" + name, self, readArgument, false);

            final RubyNode block = SequenceNode.sequence(context, sourceSection, checkArity, writeInstanceVariable);

            final String indicativeName = name + "(attr_writer)";

            final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, null, indicativeName, false, null, false);
            final RubyRootNode rootNode = new RubyRootNode(context, sourceSection, null, sharedMethodInfo, block);
            final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
            final RubyMethod method = new RubyMethod(sharedMethodInfo, name + "=", module, Visibility.PUBLIC, false, callTarget, null);
            module.addMethod(currentNode, method);
        }
    }

    @CoreMethod(names = {"attr_accessor", "attr"}, argumentsAsArray = true)
    public abstract static class AttrAccessorNode extends CoreMethodNode {

        public AttrAccessorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AttrAccessorNode(AttrAccessorNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass attrAccessor(RubyModule module, Object[] args) {
            notDesignedForCompilation();

            final SourceSection sourceSection = Truffle.getRuntime().getCallerFrame().getCallNode().getEncapsulatingSourceSection();

            for (Object arg : args) {
                final String accessorName;

                if (arg instanceof RubySymbol) {
                    accessorName = ((RubySymbol) arg).toString();
                } else {
                    throw new UnsupportedOperationException();
                }

                attrAccessor(this, getContext(), sourceSection, module, accessorName);
            }

            return getContext().getCoreLibrary().getNilObject();
        }

        public static void attrAccessor(RubyNode currentNode, RubyContext context, SourceSection sourceSection, RubyModule module, String name) {
            CompilerDirectives.transferToInterpreter();
            AttrReaderNode.attrReader(currentNode, context, sourceSection, module, name);
            AttrWriterNode.attrWriter(currentNode, context, sourceSection, module, name);
        }

    }

    @CoreMethod(names = {"class_eval","module_eval"}, optional = 3, needsBlock = true)
    public abstract static class ClassEvalNode extends CoreMethodNode {

        @Child protected YieldDispatchHeadNode yield;

        public ClassEvalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            yield = new YieldDispatchHeadNode(context);
        }

        public ClassEvalNode(ClassEvalNode prev) {
            super(prev);
            yield = prev.yield;
        }

        @Specialization
        public Object classEval(VirtualFrame frame, RubyModule module, RubyString code, @SuppressWarnings("unused") UndefinedPlaceholder file, @SuppressWarnings("unused") UndefinedPlaceholder line, @SuppressWarnings("unused") UndefinedPlaceholder block) {
            notDesignedForCompilation();

            final Source source = Source.fromText(code.toString(), "(eval)");
            return getContext().execute(getContext(), source, TranslatorDriver.ParserContext.MODULE, module, frame.materialize(), this);
        }

        @Specialization
        public Object classEval(VirtualFrame frame, RubyModule module, RubyString code, RubyString file, @SuppressWarnings("unused") UndefinedPlaceholder line, @SuppressWarnings("unused") UndefinedPlaceholder block) {
            notDesignedForCompilation();

            final Source source = Source.asPseudoFile(code.toString(), file.toString());
            return getContext().execute(getContext(), source, TranslatorDriver.ParserContext.MODULE, module, frame.materialize(), this);
        }

        @Specialization
        public Object classEval(VirtualFrame frame, RubyModule module, RubyString code, RubyString file, @SuppressWarnings("unused") int line, @SuppressWarnings("unused") UndefinedPlaceholder block) {
            notDesignedForCompilation();

            final Source source = Source.asPseudoFile(code.toString(), file.toString());
            return getContext().execute(getContext(), source, TranslatorDriver.ParserContext.MODULE, module, frame.materialize(), this);
        }

        @Specialization
        public Object classEval(VirtualFrame frame, RubyModule self, @SuppressWarnings("unused") UndefinedPlaceholder code, @SuppressWarnings("unused") UndefinedPlaceholder file, @SuppressWarnings("unused") UndefinedPlaceholder line, RubyProc block) {
            notDesignedForCompilation();

            return yield.dispatchWithModifiedSelf(frame, block, self);
        }

        @Specialization
        public Object classEval(RubyModule self, UndefinedPlaceholder code, UndefinedPlaceholder file, UndefinedPlaceholder line, UndefinedPlaceholder block) {
            notDesignedForCompilation();

            throw new RaiseException(getContext().getCoreLibrary().argumentError(0, 1, 2, this));
        }

    }

    @CoreMethod(names = {"class_exec","module_exec"}, argumentsAsArray = true, needsBlock = true)
    public abstract static class ClassExecNode extends CoreMethodNode {

        @Child protected YieldDispatchHeadNode yield;

        public ClassExecNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            yield = new YieldDispatchHeadNode(context);
        }

        public ClassExecNode(ClassExecNode prev) {
            super(prev);
            yield = prev.yield;
        }

        public abstract Object executeClassEval(VirtualFrame frame, RubyModule self, Object[] args, RubyProc block);

        @Specialization
        public Object classEval(VirtualFrame frame, RubyModule self, Object[] args, RubyProc block) {
            notDesignedForCompilation();

            // TODO: deal with args

            return yield.dispatchWithModifiedSelf(frame, block, self);
        }

    }

    @CoreMethod(names = "class_variable_defined?", required = 1)
    public abstract static class ClassVariableDefinedNode extends CoreMethodNode {

        public ClassVariableDefinedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ClassVariableDefinedNode(ClassVariableDefinedNode prev) {
            super(prev);
        }

        @Specialization
        public boolean isClassVariableDefined(RubyModule module, RubyString name) {
            notDesignedForCompilation();

            return module.getClassVariables().containsKey(name.toString());
        }

        @Specialization
        public boolean isClassVariableDefined(RubyModule module, RubySymbol name) {
            notDesignedForCompilation();

            return module.getClassVariables().containsKey(name.toString());
        }

    }

    @CoreMethod(names = "class_variable_get", required = 1)
    public abstract static class ClassVariableGetNode extends CoreMethodNode {

        public ClassVariableGetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ClassVariableGetNode(ClassVariableGetNode prev) {
            super(prev);
        }

        @Specialization
        public Object getClassVariable(RubyModule module, RubyString name) {
            notDesignedForCompilation();
            return ModuleOperations.lookupClassVariable(module, RubyContext.checkClassVariableName(getContext(), name.toString(), this));
        }

        @Specialization
        public Object getClassVariable(RubyModule module, RubySymbol name) {
            notDesignedForCompilation();
            return ModuleOperations.lookupClassVariable(module, RubyContext.checkClassVariableName(getContext(), name.toString(), this));
        }

    }

    @CoreMethod(names = "class_variables")
    public abstract static class ClassVariablesNode extends CoreMethodNode {

        public ClassVariablesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ClassVariablesNode(ClassVariablesNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray getClassVariables(RubyModule module) {
            notDesignedForCompilation();

            final RubyArray array = new RubyArray(module.getContext().getCoreLibrary().getArrayClass());

            for (String variable : ModuleOperations.getAllClassVariables(module).keySet()) {
                array.slowPush(RubySymbol.newSymbol(module.getContext(), variable));
            }
            return array;
        }
    }

    @CoreMethod(names = "constants", optional = 1)
    public abstract static class ConstantsNode extends CoreMethodNode {

        public ConstantsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ConstantsNode(ConstantsNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray constants(RubyModule module, UndefinedPlaceholder unused) {
            return constants(module, true);
        }

        @Specialization
        public RubyArray constants(RubyModule module, boolean inherit) {
            notDesignedForCompilation();

            final RubyArray array = new RubyArray(getContext().getCoreLibrary().getArrayClass());

            // TODO(cs): handle inherit
            for (String constant : module.getConstants().keySet()) {
                array.slowPush(getContext().newSymbol(constant));
            }

            return array;
        }
    }

    @CoreMethod(names = "const_defined?", required = 1, optional = 1)
    public abstract static class ConstDefinedNode extends CoreMethodNode {

        public ConstDefinedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ConstDefinedNode(ConstDefinedNode prev) {
            super(prev);
        }

        @Specialization
        public boolean isConstDefined(RubyModule module, RubyString name, @SuppressWarnings("unused") UndefinedPlaceholder inherit) {
            notDesignedForCompilation();

            return ModuleOperations.lookupConstant(getContext(), LexicalScope.NONE, module, name.toString()) != null;
        }

        @Specialization
        public boolean isConstDefined(RubyModule module, RubyString name, boolean inherit) {
            notDesignedForCompilation();

            if (inherit) {
                return ModuleOperations.lookupConstant(getContext(), LexicalScope.NONE, module, name.toString()) != null;
            } else {
                return module.getConstants().containsKey(name.toString());
            }
        }

        @Specialization
        public boolean isConstDefined(RubyModule module, RubySymbol name, @SuppressWarnings("unused") UndefinedPlaceholder inherit) {
            notDesignedForCompilation();

            return ModuleOperations.lookupConstant(getContext(), LexicalScope.NONE, module, name.toString()) != null;
        }

    }

    @CoreMethod(names = "const_get", required = 1)
    public abstract static class ConstGetNode extends CoreMethodNode {

        @Child protected DispatchHeadNode dispatch;

        public ConstGetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            dispatch = new DispatchHeadNode(context, Dispatch.MissingBehavior.CALL_CONST_MISSING);
        }

        public ConstGetNode(ConstGetNode prev) {
            super(prev);
            dispatch = prev.dispatch;
        }

        @Specialization
        public Object getConstant(VirtualFrame frame, RubyModule module, RubyString name) {
            notDesignedForCompilation();

            return dispatch.dispatch(
                    frame,
                    getContext().getCoreLibrary().getNilObject(),
                    null,
                    module,
                    name,
                    null,
                    new Object[]{},
                    Dispatch.DispatchAction.READ_CONSTANT);
        }

        @Specialization
        public Object getConstant(VirtualFrame frame, RubyModule module, RubySymbol name) {
            notDesignedForCompilation();

            return dispatch.dispatch(
                    frame,
                    getContext().getCoreLibrary().getNilObject(),
                    null,
                    module,
                    name,
                    null,
                    new Object[]{},
                    Dispatch.DispatchAction.READ_CONSTANT);
        }
    }

    @CoreMethod(names = "const_missing", required = 1)
    public abstract static class ConstMissingNode extends CoreMethodNode {

        public ConstMissingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ConstMissingNode(ConstMissingNode prev) {
            super(prev);
        }

        @Specialization
        public Object methodMissing(RubyModule module, RubySymbol name) {
            throw new RaiseException(getContext().getCoreLibrary().nameErrorUninitializedConstant(module, name.toString(), this));
        }

    }

    @CoreMethod(names = "const_set", required = 2)
    public abstract static class ConstSetNode extends CoreMethodNode {

        public ConstSetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ConstSetNode(ConstSetNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule setConstant(RubyModule module, RubyString name, Object object) {
            notDesignedForCompilation();

            module.setConstant(this, name.toString(), object);
            return module;
        }

        @Specialization
        public RubyModule setConstant(RubyModule module, RubySymbol name, Object object) {
            notDesignedForCompilation();

            module.setConstant(this, name.toString(), object);
            return module;
        }
    }

    @CoreMethod(names = "define_method", needsBlock = true, required = 1, optional = 1)
    public abstract static class DefineMethodNode extends CoreMethodNode {

        public DefineMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DefineMethodNode(DefineMethodNode prev) {
            super(prev);
        }

        @Specialization
        public RubySymbol defineMethod(RubyModule module, RubyString name, @SuppressWarnings("unused") UndefinedPlaceholder proc, RubyProc block) {
            notDesignedForCompilation();

            return defineMethod(module, name, block, UndefinedPlaceholder.INSTANCE);
        }

        @Specialization
        public RubySymbol defineMethod(RubyModule module, RubyString name, RubyProc proc, @SuppressWarnings("unused") UndefinedPlaceholder block) {
            notDesignedForCompilation();

            final RubySymbol symbol = getContext().getSymbolTable().getSymbol(name.getBytes());
            defineMethod(module, symbol, proc);
            return symbol;
        }

        @Specialization
        public RubySymbol defineMethod(RubyModule module, RubySymbol name, @SuppressWarnings("unused") UndefinedPlaceholder proc, RubyProc block) {
            notDesignedForCompilation();

            return defineMethod(module, name, block, UndefinedPlaceholder.INSTANCE);
        }

        @Specialization
        public RubySymbol defineMethod(RubyModule module, RubySymbol name, RubyProc proc, @SuppressWarnings("unused") UndefinedPlaceholder block) {
            notDesignedForCompilation();

            defineMethod(module, name, proc);
            return name;
        }

        private void defineMethod(RubyModule module, RubySymbol name, RubyProc proc) {
            notDesignedForCompilation();

            final CallTarget modifiedCallTarget = proc.getCallTargetForMethods();
            final RubyMethod modifiedMethod = new RubyMethod(proc.getSharedMethodInfo(), name.toString(), module, Visibility.PUBLIC, false, modifiedCallTarget, proc.getDeclarationFrame());
            module.addMethod(this, modifiedMethod);
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true)
    public abstract static class InitializeNode extends CoreMethodNode {

        @Child protected ModuleNodes.ClassExecNode classExecNode;

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeNode(InitializeNode prev) {
            super(prev);
        }

        public abstract RubyModule executeInitialize(VirtualFrame frame, RubyModule module, RubyProc block);

        void classEval(VirtualFrame frame, RubyModule module, RubyProc block) {
            if (classExecNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                classExecNode = insert(ModuleNodesFactory.ClassExecNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{null,null,null}));
            }
            classExecNode.executeClassEval(frame, module, new Object[]{}, block);
        }

        @Specialization
        public RubyModule initialize(RubyModule module, UndefinedPlaceholder block) {
            return module;
        }

        @Specialization
        public RubyModule initialize(VirtualFrame frame, RubyModule module, RubyProc block) {
            classEval(frame, module, block);
            return module;
        }

    }

    @CoreMethod(names = "initialize_copy", visibility = Visibility.PRIVATE, required = 1)
    public abstract static class InitializeCopyNode extends CoreMethodNode {

        public InitializeCopyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeCopyNode(InitializeCopyNode prev) {
            super(prev);
        }

        @Specialization
        public Object initializeCopy(RubyModule self, RubyModule other) {
            notDesignedForCompilation();

            self.initCopy(other);
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "include", argumentsAsArray = true, required = 1)
    public abstract static class IncludeNode extends CoreMethodNode {

        @Child protected DispatchHeadNode appendFeaturesNode;

        public IncludeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            appendFeaturesNode = new DispatchHeadNode(context);
        }

        public IncludeNode(IncludeNode prev) {
            super(prev);
            appendFeaturesNode = prev.appendFeaturesNode;
        }

        public abstract RubyNilClass executeInclude(VirtualFrame frame, RubyModule module, Object[] args);

        @Specialization
        public RubyNilClass include(VirtualFrame frame, RubyModule module, Object[] args) {
            notDesignedForCompilation();

            // Note that we traverse the arguments backwards

            for (int n = args.length - 1; n >= 0; n--) {
                if (args[n] instanceof RubyModule) {
                    final RubyModule included = (RubyModule) args[n];

                    appendFeaturesNode.call(frame, included, "append_features", null, module);

                    // TODO(cs): call included hook
                }
            }

            return getContext().getCoreLibrary().getNilObject();
        }
    }

    @CoreMethod(names = "method_defined?", required = 1, optional = 1)
    public abstract static class MethodDefinedNode extends CoreMethodNode {

        public MethodDefinedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MethodDefinedNode(MethodDefinedNode prev) {
            super(prev);
        }

        @Specialization
        public boolean isMethodDefined(RubyModule module, RubyString name, @SuppressWarnings("unused") UndefinedPlaceholder inherit) {
            notDesignedForCompilation();

            return ModuleOperations.lookupMethod(module, name.toString()) != null;
        }

        @Specialization
        public boolean isMethodDefined(RubyModule module, RubyString name, boolean inherit) {
            notDesignedForCompilation();

            if (inherit) {
                return ModuleOperations.lookupMethod(module, name.toString()) != null;
            } else {
                return module.getMethods().containsKey(name.toString());
            }
        }

        @Specialization
        public boolean isMethodDefined(RubyModule module, RubySymbol name, @SuppressWarnings("unused") UndefinedPlaceholder inherit) {
            notDesignedForCompilation();

            return ModuleOperations.lookupMethod(module, name.toString()) != null;
        }
    }

    @CoreMethod(names = "module_function", argumentsAsArray = true)
    public abstract static class ModuleFunctionNode extends CoreMethodNode {

        public ModuleFunctionNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ModuleFunctionNode(ModuleFunctionNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass moduleFunction(RubyModule module, Object... args) {
            notDesignedForCompilation();

            if (args.length == 0) {
                final Frame unpacked = Truffle.getRuntime().getCallerFrame().getFrame(FrameInstance.FrameAccess.READ_WRITE, false);

                final FrameSlot slot = unpacked.getFrameDescriptor().findFrameSlot(RubyModule.MODULE_FUNCTION_FLAG_FRAME_SLOT_ID);

                /*
                 * setObject, even though it's a boolean, so we can getObject and either get the
                 * default Nil or the boolean value without triggering deoptimization.
                 */

                unpacked.setObject(slot, true);
            } else {
                for (Object argument : args) {
                    final String methodName;

                    if (argument instanceof RubySymbol) {
                        methodName = ((RubySymbol) argument).toString();
                    } else if (argument instanceof RubyString) {
                        methodName = ((RubyString) argument).toString();
                    } else {
                        throw new UnsupportedOperationException();
                    }

                    // TODO(cs): make this instance method private
                    final RubyMethod method = ModuleOperations.lookupMethod(module, methodName);

                    if (method == null) {
                        throw new UnsupportedOperationException();
                    }

                    module.getSingletonClass(this).addMethod(this, method.withVisibility(Visibility.PUBLIC));
                }
            }

            return getContext().getCoreLibrary().getNilObject();
        }
    }

    @CoreMethod(names = "name")
    public abstract static class NameNode extends CoreMethodNode {

        public NameNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NameNode(NameNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString name(RubyModule module) {
            notDesignedForCompilation();

            return getContext().makeString(module.getName());
        }
    }

    @CoreMethod(names = "nesting", onSingleton = true)
    public abstract static class NestingNode extends CoreMethodNode {

        public NestingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NestingNode(NestingNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray nesting() {
            notDesignedForCompilation();

            final List<RubyModule> modules = new ArrayList<>();

            MethodLike method = RubyCallStack.getCallingMethod();
            LexicalScope lexicalScope = method == null ? null : method.getSharedMethodInfo().getLexicalScope();
            RubyClass object = getContext().getCoreLibrary().getObjectClass();

            while (lexicalScope != null) {
                RubyModule enclosing = lexicalScope.getLiveModule();
                if (enclosing == object)
                    break;
                modules.add(enclosing);
                lexicalScope = lexicalScope.getParent();
            }

            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), modules.toArray(new Object[modules.size()]));
        }
    }

    @CoreMethod(names = "public", argumentsAsArray = true)
    public abstract static class PublicNode extends CoreMethodNode {

        public PublicNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PublicNode(PublicNode prev) {
            super(prev);
        }

        public abstract RubyModule executePublic(VirtualFrame frame, RubyModule module, Object[] args);

        @Specialization
        public RubyModule doPublic(RubyModule module, Object[] args) {
            notDesignedForCompilation();

            module.visibilityMethod(this, args, Visibility.PUBLIC);
            return module;
        }
    }

    @CoreMethod(names = "public_class_method", argumentsAsArray = true)
    public abstract static class PublicClassMethodNode extends CoreMethodNode {

        public PublicClassMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PublicClassMethodNode(PublicClassMethodNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule publicClassMethod(RubyModule module, Object... args) {
            notDesignedForCompilation();

            final RubyClass moduleSingleton = module.getSingletonClass(this);

            for (Object arg : args) {
                final String methodName;

                if (arg instanceof RubySymbol) {
                    methodName = arg.toString();
                } else {
                    throw new UnsupportedOperationException();
                }

                final RubyMethod method = ModuleOperations.lookupMethod(moduleSingleton, methodName);

                if (method == null) {
                    throw new RuntimeException("Couldn't find method " + arg.toString());
                }

                moduleSingleton.addMethod(this, method.withVisibility(Visibility.PUBLIC));
            }

            return module;
        }
    }

    @CoreMethod(names = "private", argumentsAsArray = true)
    public abstract static class PrivateNode extends CoreMethodNode {

        public PrivateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PrivateNode(PrivateNode prev) {
            super(prev);
        }

        public abstract RubyModule executePrivate(VirtualFrame frame, RubyModule module, Object[] args);

        @Specialization
        public RubyModule doPrivate(RubyModule module, Object[] args) {
            notDesignedForCompilation();

            module.visibilityMethod(this, args, Visibility.PRIVATE);
            return module;
        }
    }

    @CoreMethod(names = "private_class_method", argumentsAsArray = true)
    public abstract static class PrivateClassMethodNode extends CoreMethodNode {

        public PrivateClassMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PrivateClassMethodNode(PrivateClassMethodNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule privateClassMethod(RubyModule module, Object... args) {
            notDesignedForCompilation();

            final RubyClass moduleSingleton = module.getSingletonClass(this);

            for (Object arg : args) {
                final String methodName;

                if (arg instanceof RubySymbol) {
                    methodName = arg.toString();
                } else {
                    throw new UnsupportedOperationException();
                }

                final RubyMethod method = ModuleOperations.lookupMethod(moduleSingleton, methodName);

                if (method == null) {
                    throw new RuntimeException("Couldn't find method " + arg.toString());
                }

                moduleSingleton.addMethod(this, method.withVisibility(Visibility.PRIVATE));
            }

            return module;
        }
    }

    @CoreMethod(names = "private_instance_methods", optional = 1)
    public abstract static class PrivateInstanceMethodsNode extends CoreMethodNode {

        public PrivateInstanceMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PrivateInstanceMethodsNode(PrivateInstanceMethodsNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray privateInstanceMethods(RubyModule module, UndefinedPlaceholder argument) {
            return privateInstanceMethods(module, false);
        }

        @Specialization
        public RubyArray privateInstanceMethods(RubyModule module, boolean includeAncestors) {
            notDesignedForCompilation();

            final RubyArray array = new RubyArray(getContext().getCoreLibrary().getArrayClass());
            final List<RubyMethod> methods = new ArrayList<>(module.getMethods().values());

            if (includeAncestors) {
                for (RubyModule parent : module.parentAncestors()) {
                    methods.addAll(parent.getMethods().values());
                }
            }
            for (RubyMethod method : methods) {
                if (method.getVisibility() == Visibility.PRIVATE){
                    RubySymbol m = getContext().newSymbol(method.getName());
                    array.slowPush(m);
                }
            }
            return array;
        }
    }

    @CoreMethod(names = "public_instance_methods", optional = 1)
    public abstract static class PublicInstanceMethodsNode extends CoreMethodNode {

        public PublicInstanceMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PublicInstanceMethodsNode(PublicInstanceMethodsNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray publicInstanceMethods(RubyModule module, UndefinedPlaceholder argument) {
            return publicInstanceMethods(module, false);
        }

        @Specialization
        public RubyArray publicInstanceMethods(RubyModule module, boolean includeAncestors) {
            notDesignedForCompilation();

            final RubyArray array = new RubyArray(getContext().getCoreLibrary().getArrayClass());
            final List<RubyMethod> methods = new ArrayList<>(module.getMethods().values());
            if (includeAncestors) {
                for (RubyModule parent : module.parentAncestors()) {
                    methods.addAll(parent.getMethods().values());
                }
            }
            for (RubyMethod method : methods) {
                if (method.getVisibility() == Visibility.PUBLIC){
                    RubySymbol m = getContext().newSymbol(method.getName());
                    array.slowPush(m);
                }
            }
            return array;
        }
    }

    @CoreMethod(names = "instance_methods", optional = 1)
    public abstract static class InstanceMethodsNode extends CoreMethodNode {

        public InstanceMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InstanceMethodsNode(InstanceMethodsNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray instanceMethods(RubyModule module, UndefinedPlaceholder argument) {
            notDesignedForCompilation();

            return instanceMethods(module, true);
        }

        @Specialization
        public RubyArray instanceMethods(RubyModule module, boolean includeAncestors) {
            notDesignedForCompilation();

            Map<String, RubyMethod> methods;

            if (includeAncestors) {
                methods = ModuleOperations.getAllMethods(module);
            } else {
                methods = module.getMethods();
            }

            final RubyArray array = new RubyArray(getContext().getCoreLibrary().getArrayClass());
            for (RubyMethod method : methods.values()) {
                if (method.getVisibility() != Visibility.PRIVATE && !method.isUndefined()) {
                    // TODO(CS): shoudln't be using this
                    array.slowPush(getContext().newSymbol(method.getName()));
                }
            }

            return array;
        }
    }

    @CoreMethod(names = "private_constant", argumentsAsArray = true)
    public abstract static class PrivateConstantNode extends CoreMethodNode {

        public PrivateConstantNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PrivateConstantNode(PrivateConstantNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule privateConstant(RubyModule module, Object[] args) {
            notDesignedForCompilation();

            for (Object ob : args) {
                if (ob instanceof RubySymbol){
                    module.changeConstantVisibility(this, (RubySymbol) ob, true);
                }
            }
            return module;
        }
    }

    @CoreMethod(names = "public_constant", argumentsAsArray = true)
    public abstract static class PublicConstantNode extends CoreMethodNode {

        public PublicConstantNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PublicConstantNode(PublicConstantNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule publicConstant(RubyModule module, Object[] args) {
            notDesignedForCompilation();

            for (Object ob : args) {
                if (ob instanceof RubySymbol){
                    module.changeConstantVisibility(this, (RubySymbol) ob, false);
                }
            }
            return module;
        }
    }

    @CoreMethod(names = "protected", argumentsAsArray = true)
    public abstract static class ProtectedNode extends CoreMethodNode {

        public ProtectedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ProtectedNode(ProtectedNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule doProtected(VirtualFrame frame, RubyModule module, Object... args) {
            notDesignedForCompilation();

            module.visibilityMethod(this, args, Visibility.PROTECTED);
            return module;
        }
    }

    @CoreMethod(names = "remove_class_variable", required = 1)
    public abstract static class RemoveClassVariableNode extends CoreMethodNode {

        public RemoveClassVariableNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RemoveClassVariableNode(RemoveClassVariableNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule removeClassVariable(RubyModule module, RubyString name) {
            notDesignedForCompilation();

            module.removeClassVariable(this, name.toString());
            return module;
        }

        @Specialization
        public RubyModule removeClassVariable(RubyModule module, RubySymbol name) {
            notDesignedForCompilation();

            module.removeClassVariable(this, name.toString());
            return module;
        }

    }

    @CoreMethod(names = "remove_method", required = 1)
    public abstract static class RemoveMethodNode extends CoreMethodNode {

        public RemoveMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RemoveMethodNode(RemoveMethodNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule removeMethod(RubyModule module, RubyString name) {
            notDesignedForCompilation();

            module.removeMethod(this, name.toString());
            return module;
        }

        @Specialization
        public RubyModule removeMethod(RubyModule module, RubySymbol name) {
            notDesignedForCompilation();

            module.removeMethod(this, name.toString());
            return module;
        }

    }

    @CoreMethod(names = {"to_s", "inspect"})
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSNode(ToSNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString toS(RubyModule module) {
            return getContext().makeString(module.getName());
        }
    }

    @CoreMethod(names = "undef_method", required = 1)
    public abstract static class UndefMethodNode extends CoreMethodNode {

        public UndefMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public UndefMethodNode(UndefMethodNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule undefMethod(RubyClass rubyClass, RubyString name) {
            notDesignedForCompilation();

            final RubyMethod method = ModuleOperations.lookupMethod(rubyClass, name.toString());
            if (method == null) {
                throw new RaiseException(getContext().getCoreLibrary().noMethodError(name.toString(), rubyClass.toString(), this));
            }
            rubyClass.undefMethod(this, method);
            return rubyClass;
        }

        @Specialization
        public RubyModule undefMethod(RubyClass rubyClass, RubySymbol name) {
            notDesignedForCompilation();

            final RubyMethod method = ModuleOperations.lookupMethod(rubyClass, name.toString());
            if (method == null) {
                throw new RaiseException(getContext().getCoreLibrary().noMethodError(name.toString(), rubyClass.toString(), this));
            }
            rubyClass.undefMethod(this, method);
            return rubyClass;
        }

        @Specialization
        public RubyModule undefMethod(RubyModule module, RubyString name) {
            notDesignedForCompilation();

            final RubyMethod method = ModuleOperations.lookupMethod(module, name.toString());
            if (method == null) {
                throw new RaiseException(getContext().getCoreLibrary().noMethodError(name.toString(), module.toString(), this));
            }
            module.undefMethod(this, method);
            return module;
        }

        @Specialization
        public RubyModule undefMethod(RubyModule module, RubySymbol name) {
            notDesignedForCompilation();

            final RubyMethod method = ModuleOperations.lookupMethod(module, name.toString());
            if (method == null) {
                throw new RaiseException(getContext().getCoreLibrary().noMethodError(name.toString(), module.toString(), this));
            }
            module.undefMethod(this, method);
            return module;
        }

    }
}
