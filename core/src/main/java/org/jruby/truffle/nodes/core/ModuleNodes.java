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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import org.jruby.common.IRubyWarnings;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.control.SequenceNode;
import org.jruby.truffle.nodes.methods.CatchReturnNode;
import org.jruby.truffle.nodes.methods.arguments.CheckArityNode;
import org.jruby.truffle.nodes.methods.arguments.MissingArgumentBehaviour;
import org.jruby.truffle.nodes.methods.arguments.ReadPreArgumentNode;
import org.jruby.truffle.nodes.objects.ReadInstanceVariableNode;
import org.jruby.truffle.nodes.objects.SelfNode;
import org.jruby.truffle.nodes.objects.WriteInstanceVariableNode;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.array.RubyArray;
import org.jruby.truffle.runtime.methods.*;
import org.jruby.truffle.translator.TranslatorDriver;

import java.util.List;

@CoreClass(name = "Module")
public abstract class ModuleNodes {

    @CoreMethod(names = "alias_method", minArgs = 2, maxArgs = 2)
    public abstract static class AliasMethodNode extends CoreMethodNode {

        public AliasMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AliasMethodNode(AliasMethodNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule aliasMethod(RubyModule module, RubySymbol newName, RubySymbol oldName) {
            module.alias(newName.toString(), oldName.toString());
            return module;
        }
    }

    @CoreMethod(names = "append_features", minArgs = 1, maxArgs = 1)
    public abstract static class AppendFeaturesNode extends CoreMethodNode {

        public AppendFeaturesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AppendFeaturesNode(AppendFeaturesNode prev) {
            super(prev);
        }

        @Specialization
        public NilPlaceholder appendFeatures(RubyModule module, RubyModule other) {
            module.appendFeatures(other);
            return NilPlaceholder.INSTANCE;
        }
    }

    @CoreMethod(names = "attr_reader", isSplatted = true)
    public abstract static class AttrReaderNode extends CoreMethodNode {

        public AttrReaderNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AttrReaderNode(AttrReaderNode prev) {
            super(prev);
        }

        @Specialization
        public NilPlaceholder attrReader(RubyModule module, Object[] args) {
            final SourceSection sourceSection = RubyArguments.getCallerFrame().getCallNode().getEncapsulatingSourceSection();

            for (int n = 0; n < args.length; n++) {
                attrReader(getContext(), sourceSection, module, args[n].toString());
            }

            return NilPlaceholder.INSTANCE;
        }

        public static void attrReader(RubyContext context, SourceSection sourceSection, RubyModule module, String name) {
            CompilerDirectives.transferToInterpreter();

            final CheckArityNode checkArity = new CheckArityNode(context, sourceSection, Arity.NO_ARGS);

            final SelfNode self = new SelfNode(context, sourceSection);
            final ReadInstanceVariableNode readInstanceVariable = new ReadInstanceVariableNode(context, sourceSection, "@" + name, self, false);

            final RubyNode block = SequenceNode.sequence(context, sourceSection, checkArity, readInstanceVariable);

            final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, name + "(attr_reader)", null);
            final RubyRootNode rootNode = new RubyRootNode(sourceSection, null, sharedMethodInfo, block);
            final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
            final RubyMethod method = new RubyMethod(sharedMethodInfo, name, module, Visibility.PUBLIC, false, callTarget, null);
            module.addMethod(method);
        }
    }

    @CoreMethod(names = "attr_writer", isSplatted = true)
    public abstract static class AttrWriterNode extends CoreMethodNode {

        public AttrWriterNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AttrWriterNode(AttrWriterNode prev) {
            super(prev);
        }

        @Specialization
        public NilPlaceholder attrWriter(RubyModule module, Object[] args) {
            final SourceSection sourceSection = RubyArguments.getCallerFrame().getCallNode().getEncapsulatingSourceSection();

            for (int n = 0; n < args.length; n++) {
                attrWriter(getContext(), sourceSection, module, args[n].toString());
            }

            return NilPlaceholder.INSTANCE;
        }

        public static void attrWriter(RubyContext context, SourceSection sourceSection, RubyModule module, String name) {
            CompilerDirectives.transferToInterpreter();

            final CheckArityNode checkArity = new CheckArityNode(context, sourceSection, Arity.ONE_ARG);

            final SelfNode self = new SelfNode(context, sourceSection);
            final ReadPreArgumentNode readArgument = new ReadPreArgumentNode(context, sourceSection, 0, MissingArgumentBehaviour.RUNTIME_ERROR);
            final WriteInstanceVariableNode writeInstanceVariable = new WriteInstanceVariableNode(context, sourceSection, "@" + name, self, readArgument, false);

            final RubyNode block = SequenceNode.sequence(context, sourceSection, checkArity, writeInstanceVariable);

            final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, name + "(attr_writer)", null);
            final RubyRootNode rootNode = new RubyRootNode(sourceSection, null, sharedMethodInfo, block);
            final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
            final RubyMethod method = new RubyMethod(sharedMethodInfo, name + "=", module, Visibility.PUBLIC, false, callTarget, null);

            module.addMethod(method);
        }
    }

    @CoreMethod(names = {"attr_accessor", "attr"}, isSplatted = true)
    public abstract static class AttrAccessorNode extends CoreMethodNode {

        public AttrAccessorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AttrAccessorNode(AttrAccessorNode prev) {
            super(prev);
        }

        @Specialization
        public NilPlaceholder attrAccessor(RubyModule module, Object[] args) {
            final SourceSection sourceSection = RubyArguments.getCallerFrame().getCallNode().getEncapsulatingSourceSection();

            for (int n = 0; n < args.length; n++) {
                attrAccessor(getContext(), sourceSection, module, args[n].toString());
            }

            return NilPlaceholder.INSTANCE;
        }

        public static void attrAccessor(RubyContext context, SourceSection sourceSection, RubyModule module, String name) {
            CompilerDirectives.transferToInterpreter();
            AttrReaderNode.attrReader(context, sourceSection, module, name);
            AttrWriterNode.attrWriter(context, sourceSection, module, name);
        }

    }

    @CoreMethod(names = "class_eval", maxArgs = 3, minArgs = 0, needsBlock = true)
    public abstract static class ClassEvalNode extends CoreMethodNode {

        public ClassEvalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ClassEvalNode(ClassEvalNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public Object classEval(VirtualFrame frame, RubyModule module, RubyString code, @SuppressWarnings("unused") UndefinedPlaceholder file, @SuppressWarnings("unused") UndefinedPlaceholder line, @SuppressWarnings("unused") UndefinedPlaceholder block) {
            final Source source = getContext().getSourceManager().get("(eval)", code.toString());
            return getContext().execute(getContext(), source, TranslatorDriver.ParserContext.MODULE, module, frame.materialize());
        }

        @Specialization(order = 2)
        public Object classEval(VirtualFrame frame, RubyModule module, RubyString code, RubyString file, @SuppressWarnings("unused") UndefinedPlaceholder line, @SuppressWarnings("unused") UndefinedPlaceholder block) {
            final Source source = getContext().getSourceManager().get(file.toString(), code.toString());
            return getContext().execute(getContext(), source, TranslatorDriver.ParserContext.MODULE, module, frame.materialize());
        }

        @Specialization(order = 3)
        public Object classEval(VirtualFrame frame, RubyModule module, RubyString code, RubyString file, @SuppressWarnings("unused") int line, @SuppressWarnings("unused") UndefinedPlaceholder block) {
            final Source source = getContext().getSourceManager().get(file.toString(), code.toString());
            return getContext().execute(getContext(), source, TranslatorDriver.ParserContext.MODULE, module, frame.materialize());
        }

        @Specialization(order = 4)
        public Object classEval(RubyModule self, @SuppressWarnings("unused") UndefinedPlaceholder code, @SuppressWarnings("unused") UndefinedPlaceholder file, @SuppressWarnings("unused") UndefinedPlaceholder line, RubyProc block) {
            return block.callWithModifiedSelf(self);
        }

    }

    @CoreMethod(names = "class_variable_defined?", maxArgs = 0)
    public abstract static class ClassVariableDefinedNode extends CoreMethodNode {

        public ClassVariableDefinedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ClassVariableDefinedNode(ClassVariableDefinedNode prev) {
            super(prev);
        }

        @Specialization
        public boolean isClassVariableDefined(RubyModule module, RubyString name) {
            return module.lookupClassVariable(name.toString()) != null;
        }

        @Specialization
        public boolean isClassVariableDefined(RubyModule module, RubySymbol name) {
            return module.lookupClassVariable(name.toString()) != null;
        }

    }

    @CoreMethod(names = "constants", maxArgs = 0)
    public abstract static class ConstantsNode extends CoreMethodNode {

        public ConstantsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ConstantsNode(ConstantsNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray constants(@SuppressWarnings("unused") RubyModule module) {
            getContext().getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, RubyArguments.getCallerFrame().getCallNode().getEncapsulatingSourceSection().getSource().getName(), RubyArguments.getCallerFrame().getCallNode().getEncapsulatingSourceSection().getStartLine(), "Module#constants returns an empty array");
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        }
    }

    @CoreMethod(names = "const_defined?", minArgs = 1, maxArgs = 2)
    public abstract static class ConstDefinedNode extends CoreMethodNode {

        public ConstDefinedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ConstDefinedNode(ConstDefinedNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public boolean isConstDefined(RubyModule module, RubyString name, @SuppressWarnings("unused") UndefinedPlaceholder inherit) {
            return module.lookupConstant(name.toString()) != null;
        }

        @Specialization(order = 2)
        public boolean isConstDefined(RubyModule module, RubyString name, boolean inherit) {
            if (inherit) {
                return module.lookupConstant(name.toString()) != null;
            } else {
                return module.getConstants().containsKey(name.toString());
            }
        }

        @Specialization(order = 3)
        public boolean isConstDefined(RubyModule module, RubySymbol name, @SuppressWarnings("unused") UndefinedPlaceholder inherit) {
            return module.lookupConstant(name.toString()) != null;
        }

        public boolean isConstDefined(RubyModule module, RubySymbol name, boolean inherit) {
            if (inherit) {
                return module.lookupConstant(name.toString()) != null;
            } else {
                return module.getConstants().containsKey(name.toString());
            }
        }

    }

    @CoreMethod(names = "define_method", needsBlock = true, minArgs = 1, maxArgs = 2)
    public abstract static class DefineMethodNode extends CoreMethodNode {

        public DefineMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DefineMethodNode(DefineMethodNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public RubySymbol defineMethod(RubyModule module, RubyString name, @SuppressWarnings("unused") UndefinedPlaceholder proc, RubyProc block) {
            return defineMethod(module, name, block, UndefinedPlaceholder.INSTANCE);
        }

        @Specialization(order = 2)
        public RubySymbol defineMethod(RubyModule module, RubyString name, RubyProc proc, @SuppressWarnings("unused") UndefinedPlaceholder block) {
            final RubySymbol symbol = getContext().getSymbolTable().getSymbol(name.getBytes());
            defineMethod(module, symbol, proc);
            return symbol;
        }

        @Specialization(order = 3)
        public RubySymbol defineMethod(RubyModule module, RubySymbol name, @SuppressWarnings("unused") UndefinedPlaceholder proc, RubyProc block) {
            return defineMethod(module, name, block, UndefinedPlaceholder.INSTANCE);
        }

        @Specialization(order = 4)
        public RubySymbol defineMethod(RubyModule module, RubySymbol name, RubyProc proc, @SuppressWarnings("unused") UndefinedPlaceholder block) {
            defineMethod(module, name, proc);
            return name;
        }

        private static void defineMethod(RubyModule module, RubySymbol name, RubyProc proc) {
            final RubyMethod method = proc.getMethod();

            if (!(method.getCallTarget() instanceof RootCallTarget)) {
                throw new UnsupportedOperationException("Can only use define_method with methods where we have the original AST, as we need to clone and modify it");
            }

            final RubyRootNode modifiedRootNode = (RubyRootNode) ((RootCallTarget) method.getCallTarget()).getRootNode();
            final CatchReturnNode modifiedCatchReturn = NodeUtil.findFirstNodeInstance(modifiedRootNode, CatchReturnNode.class);

            if (modifiedCatchReturn == null) {
                throw new UnsupportedOperationException("Doesn't seem to have a " + CatchReturnNode.class.getName());
            }

            modifiedCatchReturn.setIsProc(false);

            final CallTarget modifiedCallTarget = Truffle.getRuntime().createCallTarget(modifiedRootNode);
            final RubyMethod modifiedMethod = new RubyMethod(method.getSharedMethodInfo(), name.toString(), method.getDeclaringModule(), method.getVisibility(), method.isUndefined(), modifiedCallTarget, method.getDeclarationFrame());
            module.addMethod(modifiedMethod);
        }

    }

    @CoreMethod(names = "include", isSplatted = true, minArgs = 1)
    public abstract static class IncludeNode extends CoreMethodNode {

        public IncludeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IncludeNode(IncludeNode prev) {
            super(prev);
        }

        @Specialization
        public NilPlaceholder include(RubyModule module, Object[] args) {
            // Note that we traverse the arguments backwards

            for (int n = args.length - 1; n >= 0; n--) {
                if (args[n] instanceof RubyModule) {
                    final RubyModule included = (RubyModule) args[n];

                    // Note that we do appear to do full method lookup here
                    included.getLookupNode().lookupMethod("append_features").call(included, null, module);

                    // TODO(cs): call included hook
                }
            }

            return NilPlaceholder.INSTANCE;
        }
    }

    @CoreMethod(names = "method_defined?", minArgs = 1, maxArgs = 2)
    public abstract static class MethodDefinedNode extends CoreMethodNode {

        public MethodDefinedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MethodDefinedNode(MethodDefinedNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public boolean isMethodDefined(RubyModule module, RubyString name, @SuppressWarnings("unused") UndefinedPlaceholder inherit) {
            return module.lookupMethod(name.toString()) != null;
        }

        @Specialization(order = 2)
        public boolean isMethodDefined(RubyModule module, RubyString name, boolean inherit) {
            if (inherit) {
                return module.lookupMethod(name.toString()) != null;
            } else {
                return module.getMethods().containsKey(name.toString());
            }
        }

        @Specialization(order = 3)
        public boolean isMethodDefined(RubyModule module, RubySymbol name, @SuppressWarnings("unused") UndefinedPlaceholder inherit) {
            return module.lookupMethod(name.toString()) != null;
        }

        public boolean isMethodDefined(RubyModule module, RubySymbol name, boolean inherit) {
            if (inherit) {
                return module.lookupMethod(name.toString()) != null;
            } else {
                return module.getMethods().containsKey(name.toString());
            }
        }
    }

    @CoreMethod(names = "module_eval", minArgs = 1, maxArgs = 3)
    public abstract static class ModuleEvalNode extends CoreMethodNode {

        public ModuleEvalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ModuleEvalNode(ModuleEvalNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule moduleEval(RubyModule module, RubyString code, @SuppressWarnings("unused") Object file, @SuppressWarnings("unused") Object line) {
            module.moduleEval(code.toString());
            return module;
        }
    }

    @CoreMethod(names = "module_function", isSplatted = true)
    public abstract static class ModuleFunctionNode extends CoreMethodNode {

        public ModuleFunctionNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ModuleFunctionNode(ModuleFunctionNode prev) {
            super(prev);
        }

        @Specialization
        public NilPlaceholder moduleFunction(RubyModule module, Object... args) {
            if (args.length == 0) {
                final Frame unpacked = RubyArguments.getCallerFrame(FrameInstance.FrameAccess.READ_WRITE, false);

                final FrameSlot slot = unpacked.getFrameDescriptor().findFrameSlot(RubyModule.MODULE_FUNCTION_FLAG_FRAME_SLOT_ID);

                /*
                 * setObject, even though it's a boolean, so we can getObject and either get the
                 * default Nil or the boolean value without triggering deoptimization.
                 */

                unpacked.setObject(slot, true);
            } else {
                for (Object argument : args) {
                    final String methodName = argument.toString();
                    module.getSingletonClass().addMethod(module.lookupMethod(methodName));
                }
            }

            return NilPlaceholder.INSTANCE;
        }
    }

    @CoreMethod(names = "public", isSplatted = true)
    public abstract static class PublicNode extends CoreMethodNode {

        public PublicNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PublicNode(PublicNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule doPublic(RubyModule module, Object... args) {
            module.visibilityMethod(args, Visibility.PUBLIC);
            return module;
        }
    }

    @CoreMethod(names = "private", isSplatted = true)
    public abstract static class PrivateNode extends CoreMethodNode {

        public PrivateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PrivateNode(PrivateNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule doPrivate(RubyModule module, Object... args) {
            module.visibilityMethod(args, Visibility.PRIVATE);
            return module;
        }
    }

    @CoreMethod(names = "private_class_method", isSplatted = true)
    public abstract static class PrivateClassMethodNode extends CoreMethodNode {

        public PrivateClassMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PrivateClassMethodNode(PrivateClassMethodNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule privateClassMethod(RubyModule module, Object... args) {
            final RubyClass moduleSingleton = module.getSingletonClass();

            for (Object arg : args) {
                final RubyMethod method = moduleSingleton.lookupMethod(arg.toString());

                if (method == null) {
                    throw new RuntimeException("Couldn't find method " + arg.toString());
                }

                moduleSingleton.addMethod(method.withNewVisibility(Visibility.PRIVATE));
            }

            return module;
        }
    }
    @CoreMethod(names = "private_instance_methods", minArgs = 0, maxArgs = 1)
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
            final RubyArray array = new RubyArray(getContext().getCoreLibrary().getArrayClass());
            final List<RubyMethod> methods = module.getDeclaredMethods();
            if (includeAncestors) {
                RubyModule parent = module.getParentModule();
                while(parent != null){
                    methods.addAll(parent.getDeclaredMethods());
                    parent = parent.getParentModule();
                }
            }
            for (RubyMethod method : methods) {
                if (method.getVisibility() == Visibility.PRIVATE){
                    RubySymbol m = getContext().newSymbol(method.getName());
                    array.push(m);
                }
            }
            return array;
        }
    }

    @CoreMethod(names = "instance_methods", minArgs = 0, maxArgs = 1)
    public abstract static class InstanceMethodsNode extends CoreMethodNode {

        public InstanceMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InstanceMethodsNode(InstanceMethodsNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray instanceMethods(RubyModule module, UndefinedPlaceholder argument) {
            return instanceMethods(module, false);
        }

        @Specialization
        public RubyArray instanceMethods(RubyModule module, boolean includeAncestors) {
            final RubyArray array = new RubyArray(getContext().getCoreLibrary().getArrayClass());
            final List<RubyMethod> methods = module.getDeclaredMethods();
            if (includeAncestors) {
                RubyModule parent = module.getParentModule();
                while(parent != null){
                    methods.addAll(parent.getDeclaredMethods());
                    parent = parent.getParentModule();
                }
            }
            for (RubyMethod method : methods) {
                if (method.getVisibility() != Visibility.PRIVATE){
                    RubySymbol m = getContext().newSymbol(method.getName());
                    array.push(m);
                }
            }
            return array;
        }
    }

    @CoreMethod(names = "private_constant", isSplatted = true)
    public abstract static class PrivateConstantNode extends CoreMethodNode {

        public PrivateConstantNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PrivateConstantNode(PrivateConstantNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule privateConstant(RubyModule module, Object[] args) {
            for (Object ob : args) {
                if (ob instanceof RubySymbol){
                    module.changeConstantVisibility((RubySymbol) ob, true);
                }
            }
            return module;
        }
    }

    @CoreMethod(names = "public_constant", isSplatted = true)
    public abstract static class PublicConstantNode extends CoreMethodNode {

        public PublicConstantNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PublicConstantNode(PublicConstantNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule publicConstant(RubyModule module, Object[] args) {
            for (Object ob : args) {
                if (ob instanceof RubySymbol){
                    module.changeConstantVisibility((RubySymbol) ob, false);
                }
            }
            return module;
        }
    }

    @CoreMethod(names = "protected", isSplatted = true)
    public abstract static class ProtectedNode extends CoreMethodNode {

        public ProtectedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ProtectedNode(ProtectedNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule doProtected(VirtualFrame frame, RubyModule module, Object... args) {
            module.visibilityMethod(args, Visibility.PROTECTED);
            return module;
        }
    }

    @CoreMethod(names = "remove_class_variable", minArgs = 1, maxArgs = 1)
    public abstract static class RemoveClassVariableNode extends CoreMethodNode {

        public RemoveClassVariableNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RemoveClassVariableNode(RemoveClassVariableNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule removeClassVariable(RubyModule module, RubyString name) {
            module.removeClassVariable(name.toString());
            return module;
        }

        @Specialization
        public RubyModule removeClassVariable(RubyModule module, RubySymbol name) {
            module.removeClassVariable(name.toString());
            return module;
        }

    }

    @CoreMethod(names = "remove_method", minArgs = 1, maxArgs = 1)
    public abstract static class RemoveMethodNode extends CoreMethodNode {

        public RemoveMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RemoveMethodNode(RemoveMethodNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule removeMethod(RubyModule module, RubyString name) {
            module.removeMethod(name.toString());
            return module;
        }

        @Specialization
        public RubyModule removeMethod(RubyModule module, RubySymbol name) {
            module.removeMethod(name.toString());
            return module;
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
        public RubyString toS(RubyModule module) {
            return getContext().makeString(module.getName());
        }
    }

    @CoreMethod(names = "undef_method", minArgs = 1, maxArgs = 1)
    public abstract static class UndefMethodNode extends CoreMethodNode {

        public UndefMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public UndefMethodNode(UndefMethodNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule undefMethod(RubyModule module, RubyString name) {
            final RubyMethod method = module.lookupMethod(name.toString());
            module.undefMethod(method);
            return module;
        }

        @Specialization
        public RubyModule undefMethod(RubyModule module, RubySymbol name) {
            final RubyMethod method = module.lookupMethod(name.toString());
            module.undefMethod(method);
            return module;
        }

    }

    @CoreMethod(names = "const_set", minArgs = 2, maxArgs = 2)
    public abstract static class ConstSetNode extends CoreMethodNode {

        public ConstSetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ConstSetNode(ConstSetNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule setConstant(RubyModule module, RubyString name, Object object) {
            module.setConstant(name.toString(), object);
            return module;
        }

    }
}
