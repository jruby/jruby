/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import jnr.posix.Passwd;
import org.jcodings.Encoding;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.arguments.CheckArityNode;
import org.jruby.truffle.nodes.arguments.MissingArgumentBehaviour;
import org.jruby.truffle.nodes.arguments.ReadPreArgumentNode;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeGen;
import org.jruby.truffle.nodes.coerce.SymbolOrToStrNode;
import org.jruby.truffle.nodes.coerce.SymbolOrToStrNodeGen;
import org.jruby.truffle.nodes.coerce.ToStrNodeGen;
import org.jruby.truffle.nodes.control.SequenceNode;
import org.jruby.truffle.nodes.core.KernelNodes.BindingNode;
import org.jruby.truffle.nodes.core.ModuleNodesFactory.SetVisibilityNodeFactory;
import org.jruby.truffle.nodes.dispatch.DispatchAction;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.MissingBehavior;
import org.jruby.truffle.nodes.methods.SetMethodDeclarationContext;
import org.jruby.truffle.nodes.objects.*;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.Arity;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;
import org.jruby.truffle.translator.NodeWrapper;
import org.jruby.truffle.translator.TranslatorDriver;
import org.jruby.util.IdUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@CoreClass(name = "Module")
public abstract class ModuleNodes {

    @CoreMethod(names = "===", required = 1)
    public abstract static class ContainsInstanceNode extends CoreMethodArrayArgumentsNode {

        @Child private MetaClassNode metaClassNode;
        
        public ContainsInstanceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            metaClassNode = MetaClassNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public boolean containsInstance(RubyModule module, RubyBasicObject instance) {
            return includes(instance.getMetaClass(), module);
        }

        @Specialization(guards = "!isRubyBasicObject(instance)")
        public boolean containsInstance(VirtualFrame frame, RubyModule module, Object instance) {
            return includes(metaClassNode.executeMetaClass(frame, instance), module);
        }
        
        @CompilerDirectives.TruffleBoundary
        public boolean includes(RubyModule metaClass, RubyModule module) {
            return ModuleOperations.includesModule(metaClass, module);
        }
    }

    @CoreMethod(names = "<", required = 1)
    public abstract static class IsSubclassOfNode extends CoreMethodArrayArgumentsNode {

        public IsSubclassOfNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract Object executeIsSubclassOf(VirtualFrame frame, RubyModule self, RubyModule other);

        @Specialization
        public Object isSubclassOf(VirtualFrame frame, RubyModule self, RubyModule other) {
            CompilerDirectives.transferToInterpreter();

            if (self == other) {
                return false;
            }

            if (ModuleOperations.includesModule(self, other)) {
                return true;
            }

            if (ModuleOperations.includesModule(other, self)) {
                return false;
            }

            return nil();
        }

        @Specialization
        public Object isSubclassOf(VirtualFrame frame, RubyModule self, RubyBasicObject other) {
            CompilerDirectives.transferToInterpreter();

            throw new RaiseException(getContext().getCoreLibrary().typeError("compared with non class/module", this));
        }

    }

    @CoreMethod(names = "<=", required = 1)
    public abstract static class IsSubclassOfOrEqualToNode extends CoreMethodArrayArgumentsNode {

        public IsSubclassOfOrEqualToNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract Object executeIsSubclassOfOrEqualTo(VirtualFrame frame, RubyModule self, RubyModule other);

        @Specialization
        public Object isSubclassOfOrEqualTo(VirtualFrame frame, RubyModule self, RubyModule other) {
            CompilerDirectives.transferToInterpreter();

            if (self == other || ModuleOperations.includesModule(self, other)) {
                return true;
            }

            if (ModuleOperations.includesModule(other, self)) {
                return false;
            }

            return nil();
        }

        @Specialization
        public Object isSubclassOfOrEqualTo(VirtualFrame frame, RubyModule self, RubyBasicObject other) {
            CompilerDirectives.transferToInterpreter();

            throw new RaiseException(getContext().getCoreLibrary().typeError("compared with non class/module", this));
        }

    }

    @CoreMethod(names = ">", required = 1)
    public abstract static class IsSuperclassOfNode extends CoreMethodArrayArgumentsNode {

        public IsSuperclassOfNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract Object executeIsSuperclassOf(VirtualFrame frame, RubyModule self, RubyModule other);

        @Specialization
        public Object isSuperclassOf(VirtualFrame frame, RubyModule self, RubyModule other) {
            CompilerDirectives.transferToInterpreter();

            if (self == other) {
                return false;
            }

            if (ModuleOperations.includesModule(other, self)) {
                return true;
            }

            if (ModuleOperations.includesModule(self, other)) {
                return false;
            }

            return nil();
        }

        @Specialization
        public Object isSuperclassOf(VirtualFrame frame, RubyModule self, RubyBasicObject other) {
            CompilerDirectives.transferToInterpreter();

            throw new RaiseException(getContext().getCoreLibrary().typeError("compared with non class/module", this));
        }

    }

    @CoreMethod(names = ">=", required = 1)
    public abstract static class IsSuperclassOfOrEqualToNode extends CoreMethodArrayArgumentsNode {

        public IsSuperclassOfOrEqualToNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract Object executeIsSuperclassOfOrEqualTo(VirtualFrame frame, RubyModule self, RubyModule other);

        @Specialization
        public Object isSuperclassOfOrEqualTo(VirtualFrame frame, RubyModule self, RubyModule other) {
            CompilerDirectives.transferToInterpreter();

            if (self == other || ModuleOperations.includesModule(other, self)) {
                return true;
            }

            if (ModuleOperations.includesModule(self, other)) {
                return false;
            }

            return nil();
        }

        @Specialization
        public Object isSuperclassOfOrEqualTo(VirtualFrame frame, RubyModule self, RubyBasicObject other) {
            CompilerDirectives.transferToInterpreter();

            throw new RaiseException(getContext().getCoreLibrary().typeError("compared with non class/module", this));
        }

    }

    @CoreMethod(names = "<=>", required = 1)
    public abstract static class CompareNode extends CoreMethodArrayArgumentsNode {

        @Child private IsSubclassOfOrEqualToNode subclassNode;
        @Child private BooleanCastNode booleanCastNode;

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        private Object isSubclass(VirtualFrame frame, RubyModule self, RubyModule other) {
            if (subclassNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                subclassNode = insert(ModuleNodesFactory.IsSubclassOfOrEqualToNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{null, null}));
            }
            return subclassNode.executeIsSubclassOfOrEqualTo(frame, self, other);
        }

        private boolean booleanCast(VirtualFrame frame, Object value) {
            if (booleanCastNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                booleanCastNode = insert(BooleanCastNodeGen.create(getContext(), getSourceSection(), null));
            }
            return booleanCastNode.executeBoolean(frame, value);
        }

        @Specialization
        public Object compare(VirtualFrame frame, RubyModule self, RubyModule other) {
            CompilerDirectives.transferToInterpreter();

            if (self == other) {
                return 0;
            }

            final Object isSubclass = isSubclass(frame, self, other);

            if (isSubclass == nil()) {
                return nil();
            } else if (booleanCast(frame, isSubclass)) {
                return -1;
            }
            return 1;
        }

        @Specialization
        public Object compare(VirtualFrame frame, RubyModule self, RubyBasicObject other) {
            CompilerDirectives.transferToInterpreter();

            return nil();
        }

    }

    @CoreMethod(names = "alias_method", required = 2)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "newName"),
            @NodeChild(type = RubyNode.class, value = "oldName")
    })
    public abstract static class AliasMethodNode extends CoreMethodNode {

        public AliasMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("newName")
        public RubyNode coercetNewNameToString(RubyNode newName) {
            return SymbolOrToStrNodeGen.create(getContext(), getSourceSection(), newName);
        }

        @CreateCast("oldName")
        public RubyNode coerceOldNameToString(RubyNode oldName) {
            return SymbolOrToStrNodeGen.create(getContext(), getSourceSection(), oldName);
        }

        @Specialization
        public RubyModule aliasMethod(RubyModule module, String newName, String oldName) {
            CompilerDirectives.transferToInterpreter();

            module.alias(this, newName, oldName);
            return module;
        }

    }

    @CoreMethod(names = "ancestors")
    public abstract static class AncestorsNode extends CoreMethodArrayArgumentsNode {

        public AncestorsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyArray ancestors(RubyModule self) {
            CompilerDirectives.transferToInterpreter();

            final List<RubyModule> ancestors = new ArrayList<>();
            for (RubyModule module : self.ancestors()) {
                ancestors.add(module);
            }

            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), ancestors.toArray(new Object[ancestors.size()]));
        }
    }

    @CoreMethod(names = "append_features", required = 1, visibility = Visibility.PRIVATE)
    public abstract static class AppendFeaturesNode extends CoreMethodArrayArgumentsNode {

        public AppendFeaturesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject appendFeatures(RubyModule module, RubyModule other) {
            CompilerDirectives.transferToInterpreter();

            module.appendFeatures(this, other);
            return nil();
        }
    }

    @CoreMethod(names = "attr_reader", argumentsAsArray = true)
    public abstract static class AttrReaderNode extends CoreMethodArrayArgumentsNode {

        public AttrReaderNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject attrReader(RubyModule module, Object[] args) {
            CompilerDirectives.transferToInterpreter();

            final SourceSection sourceSection = Truffle.getRuntime().getCallerFrame().getCallNode().getEncapsulatingSourceSection();

            for (Object arg : args) {
                final String accessorName;

                if (arg instanceof RubySymbol) {
                    accessorName = ((RubySymbol) arg).toString();
                } else if (arg instanceof RubyString) {
                    accessorName = ((RubyString) arg).toString();
                } else {
                    throw new UnsupportedOperationException();
                }

                attrReader(this, getContext(), sourceSection, module, accessorName);
            }

            return nil();
        }

        public static void attrReader(Node currentNode, RubyContext context, SourceSection sourceSection, RubyModule module, String name) {
            CompilerDirectives.transferToInterpreter();

            final CheckArityNode checkArity = new CheckArityNode(context, sourceSection, new Arity(0, 0, false, false, false, 0));

            final SelfNode self = new SelfNode(context, sourceSection);
            final ReadInstanceVariableNode readInstanceVariable = new ReadInstanceVariableNode(context, sourceSection, "@" + name, self, false);

            final RubyNode block = SequenceNode.sequence(context, sourceSection, checkArity, readInstanceVariable);

            final String indicativeName = name + "(attr_reader)";

            final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, null, Arity.NO_ARGUMENTS, indicativeName, false, null, false);
            final RubyRootNode rootNode = new RubyRootNode(context, sourceSection, null, sharedMethodInfo, block);
            final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
            final InternalMethod method = new InternalMethod(sharedMethodInfo, name, module, Visibility.PUBLIC, false, callTarget, null);
            module.addMethod(currentNode, method);
        }
    }

    @CoreMethod(names = "attr_writer", argumentsAsArray = true)
    public abstract static class AttrWriterNode extends CoreMethodArrayArgumentsNode {

        public AttrWriterNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject attrWriter(RubyModule module, Object[] args) {
            CompilerDirectives.transferToInterpreter();

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

            return nil();
        }

        public static void attrWriter(Node currentNode, RubyContext context, SourceSection sourceSection, RubyModule module, String name) {
            CompilerDirectives.transferToInterpreter();

            final CheckArityNode checkArity = new CheckArityNode(context, sourceSection, new Arity(1, 0, false, false, false, 0));

            final SelfNode self = new SelfNode(context, sourceSection);
            final ReadPreArgumentNode readArgument = new ReadPreArgumentNode(context, sourceSection, 0, MissingArgumentBehaviour.RUNTIME_ERROR);
            final WriteInstanceVariableNode writeInstanceVariable = new WriteInstanceVariableNode(context, sourceSection, "@" + name, self, readArgument, false);

            final RubyNode block = SequenceNode.sequence(context, sourceSection, checkArity, writeInstanceVariable);

            final String indicativeName = name + "(attr_writer)";

            final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, null, Arity.ONE_REQUIRED, indicativeName, false, null, false);
            final RubyRootNode rootNode = new RubyRootNode(context, sourceSection, null, sharedMethodInfo, block);
            final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
            final InternalMethod method = new InternalMethod(sharedMethodInfo, name + "=", module, Visibility.PUBLIC, false, callTarget, null);
            module.addMethod(currentNode, method);
        }
    }

    @CoreMethod(names = {"attr_accessor", "attr"}, argumentsAsArray = true)
    public abstract static class AttrAccessorNode extends CoreMethodArrayArgumentsNode {

        public AttrAccessorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject attrAccessor(RubyModule module, Object[] args) {
            CompilerDirectives.transferToInterpreter();

            final SourceSection sourceSection = Truffle.getRuntime().getCallerFrame().getCallNode().getEncapsulatingSourceSection();

            for (Object arg : args) {
                final String accessorName;

                if (arg instanceof RubySymbol) {
                    accessorName = ((RubySymbol) arg).toString();
                } else if (arg instanceof RubyString) {
                    accessorName = ((RubyString) arg).toString();
                } else {
                    throw new RaiseException(getContext().getCoreLibrary().typeError(" is not a symbol or string", this));
                }

                attrAccessor(this, getContext(), sourceSection, module, accessorName);
            }

            return nil();
        }

        public static void attrAccessor(Node currentNode, RubyContext context, SourceSection sourceSection, RubyModule module, String name) {
            CompilerDirectives.transferToInterpreter();
            AttrReaderNode.attrReader(currentNode, context, sourceSection, module, name);
            AttrWriterNode.attrWriter(currentNode, context, sourceSection, module, name);
        }

    }

    @CoreMethod(names = "autoload", required = 2)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name"),
            @NodeChild(type = RubyNode.class, value = "filename")
    })
    public abstract static class AutoloadNode extends CoreMethodNode {

        @Child private StringNodes.EmptyNode emptyNode;
        private final ConditionProfile invalidConstantName = ConditionProfile.createBinaryProfile();
        private final ConditionProfile emptyFilename = ConditionProfile.createBinaryProfile();

        public AutoloadNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            emptyNode = StringNodesFactory.EmptyNodeFactory.create(context, sourceSection, new RubyNode[]{});
        }

        @CreateCast("filename") public RubyNode coerceFilenameToString(RubyNode filename) {
            return ToStrNodeGen.create(getContext(), getSourceSection(), filename);
        }

        @Specialization
        public RubyBasicObject autoload(RubyModule module, RubySymbol name, RubyString filename) {
            return autoload(module, name.toString(), filename);
        }

        @Specialization
        public RubyBasicObject autoload(RubyModule module, RubyString name, RubyString filename) {
            return autoload(module, name.toString(), filename);
        }

        private RubyBasicObject autoload(RubyModule module, String name, RubyString filename) {
            if (invalidConstantName.profile(!IdUtil.isValidConstantName19(name))) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().nameError(String.format("autoload must be constant name: %s", name), name, this));
            }

            if (emptyFilename.profile(emptyNode.empty(filename))) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("empty file name", this));
            }

            module.setAutoloadConstant(this, name, filename);

            return nil();
        }
    }

    @CoreMethod(names = "autoload?", required = 1)
    public abstract static class AutoloadQueryNode extends CoreMethodArrayArgumentsNode {

        public AutoloadQueryNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object autoloadQuery(RubyModule module, RubySymbol name) {
            return autoloadQuery(module, name.toString());
        }

        @Specialization
        public Object autoloadQuery(RubyModule module, RubyString name) {
            return autoloadQuery(module, name.toString());
        }

        private Object autoloadQuery(RubyModule module, String name) {
            final RubyConstant constant = ModuleOperations.lookupConstant(getContext(), LexicalScope.NONE, module, name);

            if ((constant == null) || ! constant.isAutoload()) {
                return nil();
            }

            return constant.getValue();
        }
    }

    @CoreMethod(names = {"class_eval","module_eval"}, optional = 3, needsBlock = true)
    public abstract static class ClassEvalNode extends CoreMethodArrayArgumentsNode {

        @Child private YieldDispatchHeadNode yield;
        @Child private BindingNode bindingNode;

        public ClassEvalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            yield = new YieldDispatchHeadNode(context);
        }

        protected RubyBinding getCallerBinding(VirtualFrame frame) {
            if (bindingNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                bindingNode = insert(KernelNodesFactory.BindingNodeFactory.create(getContext(), getSourceSection(), new RubyNode[] {}));
            }
            return bindingNode.executeRubyBinding(frame);
        }

        @Specialization
        public Object classEval(VirtualFrame frame, RubyModule module, RubyString code, UndefinedPlaceholder file, UndefinedPlaceholder line, UndefinedPlaceholder block) {
            CompilerDirectives.transferToInterpreter();

            final Source source = Source.fromText(code.toString(), "(eval)");
            return classEvalSource(frame, module, source, code.getByteList().getEncoding());
        }

        @Specialization
        public Object classEval(VirtualFrame frame, RubyModule module, RubyString code, RubyString file, UndefinedPlaceholder line, UndefinedPlaceholder block) {
            CompilerDirectives.transferToInterpreter();

            final Source source = Source.fromNamedText(code.toString(), file.toString());
            return classEvalSource(frame, module, source, code.getByteList().getEncoding());
        }

        @Specialization
        public Object classEval(VirtualFrame frame, RubyModule module, RubyString code, RubyString file, int line, UndefinedPlaceholder block) {
            CompilerDirectives.transferToInterpreter();

            final Source source = Source.fromNamedText(code.toString(), file.toString());
            return classEvalSource(frame, module, source, code.getByteList().getEncoding());
        }

        private Object classEvalSource(VirtualFrame frame, RubyModule module, Source source, Encoding encoding) {
            RubyBinding binding = getCallerBinding(frame);

            return getContext().execute(source, encoding, TranslatorDriver.ParserContext.MODULE, module, binding.getFrame(), this, new NodeWrapper() {
                @Override
                public RubyNode wrap(RubyNode node) {
                    return new SetMethodDeclarationContext(node.getContext(), node.getSourceSection(), Visibility.PUBLIC, "class_eval", node);
                }
            });
        }

        @Specialization
        public Object classEval(VirtualFrame frame, RubyModule self, UndefinedPlaceholder code, UndefinedPlaceholder file, UndefinedPlaceholder line, RubyProc block) {
            return yield.dispatchWithModifiedSelf(frame, block, self);
        }

        @Specialization
        public Object classEval(RubyModule self, UndefinedPlaceholder code, UndefinedPlaceholder file, UndefinedPlaceholder line, UndefinedPlaceholder block) {
            CompilerDirectives.transferToInterpreter();

            throw new RaiseException(getContext().getCoreLibrary().argumentError(0, 1, 2, this));
        }

    }

    @CoreMethod(names = {"class_exec","module_exec"}, argumentsAsArray = true, needsBlock = true)
    public abstract static class ClassExecNode extends CoreMethodArrayArgumentsNode {

        @Child private YieldDispatchHeadNode yield;

        public ClassExecNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            yield = new YieldDispatchHeadNode(context);
        }

        public abstract Object executeClassExec(VirtualFrame frame, RubyModule self, Object[] args, RubyProc block);

        @Specialization
        public Object classExec(VirtualFrame frame, RubyModule self, Object[] args, RubyProc block) {
            CompilerDirectives.transferToInterpreter();

            // TODO: deal with args

            return yield.dispatchWithModifiedSelf(frame, block, self);
        }

    }

    @CoreMethod(names = "class_variable_defined?", required = 1)
    public abstract static class ClassVariableDefinedNode extends CoreMethodArrayArgumentsNode {

        public ClassVariableDefinedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean isClassVariableDefined(RubyModule module, RubyString name) {
            CompilerDirectives.transferToInterpreter();

            return module.getClassVariables().containsKey(name.toString());
        }

        @Specialization
        public boolean isClassVariableDefined(RubyModule module, RubySymbol name) {
            CompilerDirectives.transferToInterpreter();

            return module.getClassVariables().containsKey(name.toString());
        }

    }

    @CoreMethod(names = "class_variable_get", required = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name")
    })
    public abstract static class ClassVariableGetNode extends CoreMethodNode {

        public ClassVariableGetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return SymbolOrToStrNodeGen.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        public Object getClassVariable(RubyModule module, String name) {
            CompilerDirectives.transferToInterpreter();

            RubyContext.checkClassVariableName(getContext(), name, this);
            Object value = ModuleOperations.lookupClassVariable(module, name);

            if (value == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().nameErrorUninitializedClassVariable(module, name, this));
            } else {
                return value;
            }
        }

    }

    @CoreMethod(names = "class_variables")
    public abstract static class ClassVariablesNode extends CoreMethodArrayArgumentsNode {

        public ClassVariablesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyArray getClassVariables(RubyModule module) {
            CompilerDirectives.transferToInterpreter();

            final RubyArray array = new RubyArray(module.getContext().getCoreLibrary().getArrayClass());

            for (String variable : ModuleOperations.getAllClassVariables(module).keySet()) {
                array.slowPush(RubySymbol.newSymbol(module.getContext(), variable));
            }
            return array;
        }
    }

    @CoreMethod(names = "constants", optional = 1)
    public abstract static class ConstantsNode extends CoreMethodArrayArgumentsNode {

        @Child BooleanCastNode booleanCastNode;

        public ConstantsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        private boolean booleanCast(VirtualFrame frame, Object value) {
            if (booleanCastNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                booleanCastNode = insert(BooleanCastNodeGen.create(getContext(), getSourceSection(), null));
            }
            return booleanCastNode.executeBoolean(frame, value);
        }

        @Specialization
        public RubyArray constants(RubyModule module, UndefinedPlaceholder inherit) {
            return constants(module, true);
        }

        @Specialization
        public RubyArray constants(RubyModule module, boolean inherit) {
            CompilerDirectives.transferToInterpreter();

            final List<RubySymbol> constantsArray = new ArrayList<>();

            final Map<String, RubyConstant> constants;
            if (inherit) {
                constants = ModuleOperations.getAllConstants(module);
            } else {
                constants = module.getConstants();
            }

            for (Entry<String, RubyConstant> constant : constants.entrySet()) {
                if (!constant.getValue().isPrivate()) {
                    constantsArray.add(getContext().getSymbol(constant.getKey()));
                }
            }

            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), constantsArray.toArray(new Object[constantsArray.size()]));
        }

        @Specialization(guards = "!isUndefinedPlaceholder(inherit)")
        public RubyArray constants(VirtualFrame frame, RubyModule module, Object inherit) {
            return constants(module, booleanCast(frame, inherit));
        }

    }

    @CoreMethod(names = "const_defined?", required = 1, optional = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name"),
            @NodeChild(type = RubyNode.class, value = "inherit")
    })
    public abstract static class ConstDefinedNode extends CoreMethodNode {

        public ConstDefinedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return SymbolOrToStrNodeGen.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        public boolean isConstDefined(RubyModule module, String name, UndefinedPlaceholder inherit) {
            return isConstDefined(module, name, true);
        }

        @Specialization
        public boolean isConstDefined(RubyModule module, String fullName, boolean inherit) {
            CompilerDirectives.transferToInterpreter();
            return ModuleOperations.lookupScopedConstant(getContext(), module, fullName, inherit, this) != null;
        }

    }

    @CoreMethod(names = "const_get", required = 1, optional = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name"),
            @NodeChild(type = RubyNode.class, value = "inherit")
    })
    public abstract static class ConstGetNode extends CoreMethodNode {

        @Child private DispatchHeadNode dispatch;

        public ConstGetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            dispatch = new DispatchHeadNode(context, false, false, MissingBehavior.CALL_CONST_MISSING, null, DispatchAction.READ_CONSTANT);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return SymbolOrToStrNodeGen.create(getContext(), getSourceSection(), name);
        }

        @Specialization(guards = "!isScoped(name)")
        public Object getConstant(VirtualFrame frame, RubyModule module, String name, UndefinedPlaceholder inherit) {
            return getConstant(frame, module, name, true);
        }

        @Specialization(guards = "isScoped(name)")
        public Object getConstantScoped(VirtualFrame frame, RubyModule module, String name, UndefinedPlaceholder inherit) {
            return getConstantScoped(frame, module, name, true);
        }

        @Specialization(guards = { "isTrue(inherit)", "!isScoped(name)" })
        public Object getConstant(VirtualFrame frame, RubyModule module, String name, boolean inherit) {
            return dispatch.dispatch(frame, module, name, null, new Object[] {});
        }

        @Specialization(guards = { "!isTrue(inherit)", "!isScoped(name)" })
        public Object getConstantNoInherit(VirtualFrame frame, RubyModule module, String name, boolean inherit) {
            CompilerDirectives.transferToInterpreter();

            RubyConstant constant = module.getConstants().get(name);
            if (constant == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().nameErrorUninitializedConstant(module, name, this));
            } else {
                return constant.getValue();
            }
        }

        @Specialization(guards = "isScoped(fullName)")
        public Object getConstantScoped(VirtualFrame frame, RubyModule module, String fullName, boolean inherit) {
            CompilerDirectives.transferToInterpreter();

            Object fullNameObject = RubyArguments.getUserArgument(frame.getArguments(), 0);
            if (fullNameObject instanceof RubySymbol && !IdUtil.isValidConstantName19(fullName)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().nameError(String.format("wrong constant name %s", fullName), fullName, this));
            }

            RubyConstant constant = ModuleOperations.lookupScopedConstant(getContext(), module, fullName, inherit, this);
            if (constant == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().nameErrorUninitializedConstant(module, fullName, this));
            } else {
                return constant.getValue();
            }
        }

        boolean isScoped(String name) {
            return name.contains("::");
        }

    }

    @CoreMethod(names = "const_missing", required = 1)
    public abstract static class ConstMissingNode extends CoreMethodArrayArgumentsNode {

        public ConstMissingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object methodMissing(RubyModule module, RubySymbol name) {
            throw new RaiseException(getContext().getCoreLibrary().nameErrorUninitializedConstant(module, name.toString(), this));
        }

    }

    @CoreMethod(names = "const_set", required = 2)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name"),
            @NodeChild(type = RubyNode.class, value = "value")
    })
    public abstract static class ConstSetNode extends CoreMethodNode {

        public ConstSetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return SymbolOrToStrNodeGen.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        public Object setConstant(RubyModule module, String name, Object value) {
            CompilerDirectives.transferToInterpreter();

            if (!IdUtil.isValidConstantName19(name)) {
                throw new RaiseException(getContext().getCoreLibrary().nameError(String.format("wrong constant name %s", name), name, this));
            }

            module.setConstant(this, name, value);
            return value;
        }

    }

    @CoreMethod(names = "define_method", needsBlock = true, required = 1, optional = 1, visibility = Visibility.PRIVATE)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name"),
            @NodeChild(type = RubyNode.class, value = "proc"),
            @NodeChild(type = RubyNode.class, value = "block")
    })
    public abstract static class DefineMethodNode extends CoreMethodNode {

        public DefineMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return SymbolOrToStrNodeGen.create(getContext(), getSourceSection(), name);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubySymbol defineMethod(RubyModule module, String name, UndefinedPlaceholder proc, UndefinedPlaceholder block) {
            throw new RaiseException(getContext().getCoreLibrary().argumentError("needs either proc or block", this));
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubySymbol defineMethod(RubyModule module, String name, UndefinedPlaceholder proc, RubyProc block) {
            return defineMethod(module, name, block, UndefinedPlaceholder.INSTANCE);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubySymbol defineMethod(RubyModule module, String name, RubyProc proc, UndefinedPlaceholder block) {
            return defineMethod(module, name, proc);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubySymbol defineMethod(RubyModule module, String name, RubyMethod method, UndefinedPlaceholder block) {
            module.addMethod(this, method.getMethod().withName(name));
            return getContext().getSymbolTable().getSymbol(name);
        }

        @Specialization
        public RubySymbol defineMethod(VirtualFrame frame, RubyModule module, String name, RubyUnboundMethod method, UndefinedPlaceholder block) {
            CompilerDirectives.transferToInterpreter();

            RubyModule origin = method.getOrigin();
            if (!ModuleOperations.canBindMethodTo(origin, module)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().typeError("bind argument must be a subclass of " + origin.getName(), this));
            }

            // TODO CS 5-Apr-15 TypeError if the method came from a singleton

            return addMethod(module, name, method.getMethod());
        }

        private RubySymbol defineMethod(RubyModule module, String name, RubyProc proc) {
            CompilerDirectives.transferToInterpreter();

            final CallTarget modifiedCallTarget = proc.getCallTargetForMethods();
            final SharedMethodInfo info = proc.getSharedMethodInfo().withName(name);
            final InternalMethod modifiedMethod = new InternalMethod(info, name, module, Visibility.PUBLIC, false, modifiedCallTarget, proc.getDeclarationFrame());

            return addMethod(module, name, modifiedMethod);
        }

        private RubySymbol addMethod(RubyModule module, String name, InternalMethod method) {
            method = method.withName(name);

            if (ModuleOperations.isMethodPrivateFromName(name)) {
                method = method.withVisibility(Visibility.PRIVATE);
            }

            module.addMethod(this, method);
            return getContext().getSymbolTable().getSymbol(name);
        }

    }

    @CoreMethod(names = "extend_object", required = 1, visibility = Visibility.PRIVATE)
    public abstract static class ExtendObjectNode extends CoreMethodArrayArgumentsNode {

        public ExtendObjectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject extendObject(RubyModule module, RubyBasicObject object) {
            CompilerDirectives.transferToInterpreter();

            if (module instanceof RubyClass) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().typeErrorWrongArgumentType(module, "Module", this));
            }

            object.getSingletonClass(this).include(this, module);
            return module;
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        @Child private ModuleNodes.ClassExecNode classExecNode;

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract RubyModule executeInitialize(VirtualFrame frame, RubyModule module, RubyProc block);

        void classEval(VirtualFrame frame, RubyModule module, RubyProc block) {
            if (classExecNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                classExecNode = insert(ModuleNodesFactory.ClassExecNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{null,null,null}));
            }
            classExecNode.executeClassExec(frame, module, new Object[]{}, block);
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

    @CoreMethod(names = "initialize_copy", required = 1)
    public abstract static class InitializeCopyNode extends CoreMethodArrayArgumentsNode {

        public InitializeCopyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = { "!isRubyClass(self)", "!isRubyClass(from)" })
        public Object initializeCopy(RubyModule self, RubyModule from) {
            CompilerDirectives.transferToInterpreter();

            self.initCopy(from);
            return nil();
        }

        @Specialization
        public Object initializeCopy(RubyClass self, RubyClass from) {
            CompilerDirectives.transferToInterpreter();

            if (from == getContext().getCoreLibrary().getBasicObjectClass()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().typeError("can't copy the root class", this));
            } else if (from.isSingleton()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().typeError("can't copy singleton class", this));
            }

            self.initCopy(from);
            return nil();
        }

    }

    @CoreMethod(names = "included", required = 1, visibility = Visibility.PRIVATE)
    public abstract static class IncludedNode extends CoreMethodArrayArgumentsNode {

        public IncludedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject included(Object subclass) {
            return nil();
        }

    }

    @CoreMethod(names = "included_modules")
    public abstract static class IncludedModulesNode extends CoreMethodArrayArgumentsNode {

        public IncludedModulesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        RubyArray includedModules(RubyModule module) {
            CompilerDirectives.transferToInterpreter();

            final List<RubyModule> modules = new ArrayList<>();

            for (RubyModule included : module.parentAncestors()) {
                if (included.isOnlyAModule()) {
                    modules.add(included);
                }
            }

            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), modules.toArray(new Object[modules.size()]));
        }
    }

    @CoreMethod(names = "method_defined?", required = 1, optional = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name"),
            @NodeChild(type = RubyNode.class, value = "inherit") })
    public abstract static class MethodDefinedNode extends CoreMethodNode {

        public MethodDefinedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return SymbolOrToStrNodeGen.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        public boolean isMethodDefined(RubyModule module, String name, UndefinedPlaceholder inherit) {
            return isMethodDefined(module, name, true);
        }

        @Specialization
        public boolean isMethodDefined(RubyModule module, String name, boolean inherit) {
            CompilerDirectives.transferToInterpreter();

            final InternalMethod method;
            if (inherit) {
                method = ModuleOperations.lookupMethod(module, name);
            } else {
                method = module.getMethods().get(name);
            }

            return method != null && !method.getVisibility().isPrivate();
        }

    }

    @CoreMethod(names = "module_function", argumentsAsArray = true, visibility = Visibility.PRIVATE)
    public abstract static class ModuleFunctionNode extends CoreMethodArrayArgumentsNode {

        @Child SetVisibilityNode setVisibilityNode;

        public ModuleFunctionNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            setVisibilityNode = SetVisibilityNodeFactory.create(context, sourceSection, Visibility.MODULE_FUNCTION, null, null);
        }

        @Specialization
        public RubyModule moduleFunction(VirtualFrame frame, RubyModule module, Object[] names) {
            if (module instanceof RubyClass && !getContext().getCoreLibrary().isLoadingRubyCore()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().typeError("module_function must be called for modules", this));
            }

            return setVisibilityNode.executeSetVisibility(frame, module, names);
        }

    }

    @CoreMethod(names = "name")
    public abstract static class NameNode extends CoreMethodArrayArgumentsNode {

        public NameNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object name(RubyModule module) {
            CompilerDirectives.transferToInterpreter();

            if (!module.hasName()) {
                return nil();
            }

            return getContext().makeString(module.getName());
        }
    }

    @CoreMethod(names = "nesting", onSingleton = true)
    public abstract static class NestingNode extends CoreMethodArrayArgumentsNode {

        public NestingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyArray nesting(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreter();

            final List<RubyModule> modules = new ArrayList<>();

            InternalMethod method = RubyCallStack.getCallingMethod(frame);
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

    @CoreMethod(names = "public", argumentsAsArray = true, visibility = Visibility.PRIVATE)
    public abstract static class PublicNode extends CoreMethodArrayArgumentsNode {

        @Child SetVisibilityNode setVisibilityNode;

        public PublicNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            setVisibilityNode = SetVisibilityNodeFactory.create(context, sourceSection, Visibility.PUBLIC, null, null);
        }

        public abstract RubyModule executePublic(VirtualFrame frame, RubyModule module, Object[] args);

        @Specialization
        public RubyModule doPublic(VirtualFrame frame, RubyModule module, Object[] names) {
            return setVisibilityNode.executeSetVisibility(frame, module, names);
        }

    }

    @CoreMethod(names = "public_class_method", argumentsAsArray = true)
    public abstract static class PublicClassMethodNode extends CoreMethodArrayArgumentsNode {

        public PublicClassMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyModule publicClassMethod(RubyModule module, Object... args) {
            CompilerDirectives.transferToInterpreter();

            final RubyClass moduleSingleton = module.getSingletonClass(this);

            for (Object arg : args) {
                final String methodName;

                if (arg instanceof RubySymbol) {
                    methodName = arg.toString();
                } else {
                    throw new UnsupportedOperationException();
                }

                final InternalMethod method = ModuleOperations.lookupMethod(moduleSingleton, methodName);

                if (method == null) {
                    throw new RuntimeException("Couldn't find method " + arg.toString());
                }

                moduleSingleton.addMethod(this, method.withVisibility(Visibility.PUBLIC));
            }

            return module;
        }
    }

    @CoreMethod(names = "private", argumentsAsArray = true, visibility = Visibility.PRIVATE)
    public abstract static class PrivateNode extends CoreMethodArrayArgumentsNode {

        @Child SetVisibilityNode setVisibilityNode;

        public PrivateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            setVisibilityNode = SetVisibilityNodeFactory.create(context, sourceSection, Visibility.PRIVATE, null, null);
        }

        public abstract RubyModule executePrivate(VirtualFrame frame, RubyModule module, Object[] args);

        @Specialization
        public RubyModule doPrivate(VirtualFrame frame, RubyModule module, Object[] names) {
            return setVisibilityNode.executeSetVisibility(frame, module, names);
        }

    }

    @CoreMethod(names = "private_class_method", argumentsAsArray = true)
    public abstract static class PrivateClassMethodNode extends CoreMethodArrayArgumentsNode {

        public PrivateClassMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyModule privateClassMethod(RubyModule module, Object... args) {
            CompilerDirectives.transferToInterpreter();

            final RubyClass moduleSingleton = module.getSingletonClass(this);

            for (Object arg : args) {
                final String methodName;

                if (arg instanceof RubySymbol) {
                    methodName = arg.toString();
                } else if (arg instanceof RubyString) {
                    methodName = ((RubyString) arg).toString();
                } else {
                    // TODO BF 26-APR-2015 the message could be improved to match MRI
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().typeError(" is not a symbol", this));
                }

                final InternalMethod method = ModuleOperations.lookupMethod(moduleSingleton, methodName);

                if (method == null) {
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().nameErrorUndefinedMethod(methodName, module, this));
                }

                moduleSingleton.addMethod(this, method.withVisibility(Visibility.PRIVATE));
            }

            return module;
        }
    }

    @CoreMethod(names = "private_method_defined?", required = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name")
    })
    public abstract static class PrivateMethodDefinedNode extends CoreMethodNode {

        public PrivateMethodDefinedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return SymbolOrToStrNodeGen.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        public boolean isPrivateMethodDefined(RubyModule module, String name) {
            CompilerDirectives.transferToInterpreter();

            InternalMethod method = ModuleOperations.lookupMethod(module, name);
            return method != null && method.getVisibility().isPrivate();
        }

    }

    @CoreMethod(names = "protected_instance_methods", optional = 1)
    public abstract static class ProtectedInstanceMethodsNode extends CoreMethodArrayArgumentsNode {

        public ProtectedInstanceMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyArray protectedInstanceMethods(RubyModule module, UndefinedPlaceholder argument) {
            return protectedInstanceMethods(module, true);
        }

        @Specialization
        public RubyArray protectedInstanceMethods(RubyModule module, boolean includeAncestors) {
            CompilerDirectives.transferToInterpreter();


            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(),
                    module.filterMethods(includeAncestors, new RubyModule.MethodFilter() {
                        @Override
                        public boolean filter(InternalMethod method) {
                            return method.getVisibility() == Visibility.PROTECTED;
                        }
                    }).toArray());
        }
    }

    @CoreMethod(names = "protected_method_defined?", required = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name")
    })
    public abstract static class ProtectedMethodDefinedNode extends CoreMethodNode {

        public ProtectedMethodDefinedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return SymbolOrToStrNodeGen.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        public boolean isProtectedMethodDefined(RubyModule module, String name) {
            CompilerDirectives.transferToInterpreter();

            InternalMethod method = ModuleOperations.lookupMethod(module, name);
            return method != null && method.getVisibility().isProtected();
        }

    }

    @CoreMethod(names = "private_instance_methods", optional = 1)
    public abstract static class PrivateInstanceMethodsNode extends CoreMethodArrayArgumentsNode {

        public PrivateInstanceMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyArray privateInstanceMethods(RubyModule module, UndefinedPlaceholder argument) {
            return privateInstanceMethods(module, true);
        }

        @Specialization
        public RubyArray privateInstanceMethods(RubyModule module, boolean includeAncestors) {
            CompilerDirectives.transferToInterpreter();

            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(),
                    module.filterMethods(includeAncestors, new RubyModule.MethodFilter() {
                        @Override
                        public boolean filter(InternalMethod method) {
                            return method.getVisibility() == Visibility.PRIVATE;
                        }
                    }).toArray());
        }
    }

    @CoreMethod(names = "public_instance_method", required = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name")
    })
    public abstract static class PublicInstanceMethodNode extends CoreMethodNode {

        public PublicInstanceMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return SymbolOrToStrNodeGen.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        public RubyUnboundMethod publicInstanceMethod(RubyModule module, String name) {
            CompilerDirectives.transferToInterpreter();

            // TODO(CS, 11-Jan-15) cache this lookup
            final InternalMethod method = ModuleOperations.lookupMethod(module, name);

            if (method == null || method.isUndefined()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().nameErrorUndefinedMethod(name, module, this));
            } else if (method.getVisibility() != Visibility.PUBLIC) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().nameErrorPrivateMethod(name, module, this));
            }

            return new RubyUnboundMethod(getContext().getCoreLibrary().getUnboundMethodClass(), module, method);
        }

    }

    @CoreMethod(names = "public_instance_methods", optional = 1)
    public abstract static class PublicInstanceMethodsNode extends CoreMethodArrayArgumentsNode {

        public PublicInstanceMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyArray publicInstanceMethods(RubyModule module, UndefinedPlaceholder argument) {
            return publicInstanceMethods(module, true);
        }

        @Specialization
        public RubyArray publicInstanceMethods(RubyModule module, boolean includeAncestors) {
            CompilerDirectives.transferToInterpreter();

            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(),
                    module.filterMethods(includeAncestors, new RubyModule.MethodFilter() {
                        @Override
                        public boolean filter(InternalMethod method) {
                            return method.getVisibility() == Visibility.PUBLIC;
                        }
                    }).toArray());
        }
    }

    @CoreMethod(names = "public_method_defined?", required = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name")
    })
    public abstract static class PublicMethodDefinedNode extends CoreMethodNode {

        public PublicMethodDefinedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return SymbolOrToStrNodeGen.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        public boolean isPublicMethodDefined(RubyModule module, String name) {
            CompilerDirectives.transferToInterpreter();

            InternalMethod method = ModuleOperations.lookupMethod(module, name);
            return method != null && method.getVisibility() == Visibility.PUBLIC;
        }

    }

    @CoreMethod(names = "instance_methods", optional = 1)
    public abstract static class InstanceMethodsNode extends CoreMethodArrayArgumentsNode {

        public InstanceMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyArray instanceMethods(RubyModule module, UndefinedPlaceholder argument) {
            CompilerDirectives.transferToInterpreter();

            return instanceMethods(module, true);
        }

        @Specialization
        public RubyArray instanceMethods(RubyModule module, boolean includeAncestors) {
            CompilerDirectives.transferToInterpreter();

            Map<String, InternalMethod> methods;

            if (includeAncestors) {
                methods = ModuleOperations.getAllMethods(module);
            } else {
                methods = module.getMethods();
            }

            final RubyArray array = new RubyArray(getContext().getCoreLibrary().getArrayClass());
            for (InternalMethod method : methods.values()) {
                if (method.getVisibility() != Visibility.PRIVATE && !method.isUndefined()) {
                    // TODO(CS): shoudln't be using this
                    array.slowPush(getContext().getSymbol(method.getName()));
                }
            }

            return array;
        }
    }

    @CoreMethod(names = "instance_method", required = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name")
    })
    public abstract static class InstanceMethodNode extends CoreMethodNode {

        public InstanceMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return SymbolOrToStrNodeGen.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        public RubyUnboundMethod instanceMethod(RubyModule module, String name) {
            CompilerDirectives.transferToInterpreter();

            // TODO(CS, 11-Jan-15) cache this lookup
            final InternalMethod method = ModuleOperations.lookupMethod(module, name);

            if (method == null || method.isUndefined()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().nameErrorUndefinedMethod(name, module, this));
            }

            return new RubyUnboundMethod(getContext().getCoreLibrary().getUnboundMethodClass(), module, method);
        }

    }

    @CoreMethod(names = "private_constant", argumentsAsArray = true)
    public abstract static class PrivateConstantNode extends CoreMethodArrayArgumentsNode {

        public PrivateConstantNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyModule privateConstant(RubyModule module, Object[] args) {
            CompilerDirectives.transferToInterpreter();

            for (Object name : args) {
                if (name instanceof RubySymbol) {
                    module.changeConstantVisibility(this, name.toString(), true);
                } else {
                    throw new UnsupportedOperationException();
                }
            }
            return module;
        }
    }

    @CoreMethod(names = "public_constant", argumentsAsArray = true)
    public abstract static class PublicConstantNode extends CoreMethodArrayArgumentsNode {

        public PublicConstantNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyModule publicConstant(RubyModule module, Object[] args) {
            CompilerDirectives.transferToInterpreter();

            for (Object name : args) {
                if (name instanceof RubySymbol) {
                    module.changeConstantVisibility(this, name.toString(), false);
                } else {
                    throw new UnsupportedOperationException();
                }
            }
            return module;
        }
    }

    @CoreMethod(names = "protected", argumentsAsArray = true, visibility = Visibility.PRIVATE)
    public abstract static class ProtectedNode extends CoreMethodArrayArgumentsNode {

        @Child SetVisibilityNode setVisibilityNode;

        public ProtectedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            setVisibilityNode = SetVisibilityNodeFactory.create(context, sourceSection, Visibility.PROTECTED, null, null);
        }

        @Specialization
        public RubyModule doProtected(VirtualFrame frame, RubyModule module, Object[] names) {
            return setVisibilityNode.executeSetVisibility(frame, module, names);
        }

    }

    @CoreMethod(names = "remove_class_variable", required = 1)
    public abstract static class RemoveClassVariableNode extends CoreMethodArrayArgumentsNode {

        public RemoveClassVariableNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyModule removeClassVariable(RubyModule module, RubyString name) {
            CompilerDirectives.transferToInterpreter();

            module.removeClassVariable(this, name.toString());
            return module;
        }

        @Specialization
        public RubyModule removeClassVariable(RubyModule module, RubySymbol name) {
            CompilerDirectives.transferToInterpreter();

            module.removeClassVariable(this, name.toString());
            return module;
        }

    }

    @CoreMethod(names = "remove_const", required = 1, visibility = Visibility.PRIVATE)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name")
    })
    public abstract static class RemoveConstNode extends CoreMethodNode {

        public RemoveConstNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return SymbolOrToStrNodeGen.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        Object removeConstant(RubyModule module, String name) {
            RubyConstant oldConstant = module.removeConstant(this, name);
            if (oldConstant == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().nameErrorConstantNotDefined(module, name, this));
            } else {
                return oldConstant.getValue();
            }
        }

    }

    @CoreMethod(names = "remove_method", argumentsAsArray = true, visibility = Visibility.PRIVATE)
    public abstract static class RemoveMethodNode extends CoreMethodArrayArgumentsNode {

        public RemoveMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyModule removeMethod(RubyModule module, Object[] args) {
            for (Object arg : args) {
                final String name;

                if (arg instanceof RubySymbol) {
                    name = ((RubySymbol) arg).toString();
                } else if (arg instanceof RubyString) {
                    name = ((RubyString) arg).toString();
                } else {
                    // TODO BF 9-APR-2015 the MRI message calls inspect for error message i think
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().typeError(" is not a symbol", this));
                }
                module.removeMethod(this, name);

            }

            return module;
        }

    }

    @CoreMethod(names = { "to_s", "inspect" })
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString toS(RubyModule module) {
            CompilerDirectives.transferToInterpreter();

            return getContext().makeString(module.getName());
        }

    }

    @CoreMethod(names = "undef_method", required = 1)
    public abstract static class UndefMethodNode extends CoreMethodArrayArgumentsNode {

        public UndefMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyModule undefMethod(RubyModule module, RubyString name) {
            return undefMethod(module, name.toString());
        }

        @Specialization
        public RubyModule undefMethod(RubyModule module, RubySymbol name) {
            return undefMethod(module, name.toString());
        }

        private RubyModule undefMethod(RubyModule module, String name) {
            CompilerDirectives.transferToInterpreter();

            final InternalMethod method = ModuleOperations.lookupMethod(module, name);
            if (method == null) {
                throw new RaiseException(getContext().getCoreLibrary().noMethodErrorOnModule(name, module, this));
            }
            module.undefMethod(this, method);
            return module;
        }

    }

    @CoreMethod(names = "get_user_home", needsSelf = false, required = 1)
    public abstract static class GetUserHomeNode extends CoreMethodArrayArgumentsNode {

        public GetUserHomeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString userHome(RubyString uname) {
            CompilerDirectives.transferToInterpreter();
            // TODO BJF 30-APR-2015 Review the more robust getHomeDirectoryPath implementation
            final Passwd passwd = getContext().getPosix().getpwnam(uname.toString());
            if (passwd == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("user " + uname.toString() + " does not exist", this));
            }
            return getContext().makeString(passwd.getHome());
        }

    }

    @NodeChildren({ @NodeChild(value = "module"), @NodeChild(value = "names") })
    public abstract static class SetVisibilityNode extends CoreMethodNode {

        @Child SymbolOrToStrNode symbolOrToStrNode;

        private final Visibility visibility;

        public SetVisibilityNode(RubyContext context, SourceSection sourceSection, Visibility visibility) {
            super(context, sourceSection);
            this.visibility = visibility;
            this.symbolOrToStrNode = SymbolOrToStrNodeGen.create(context, sourceSection, null);
        }

        public abstract RubyModule executeSetVisibility(VirtualFrame frame, RubyModule module, Object[] arguments);

        @Specialization
        RubyModule setVisibility(VirtualFrame frame, RubyModule module, Object[] names) {
            CompilerDirectives.transferToInterpreter();

            if (names.length == 0) {
                setCurrentVisibility(visibility);
            } else {
                for (Object name : names) {
                    final String methodName = symbolOrToStrNode.executeToJavaString(frame, name);
                    setMethodVisibility(module, methodName);
                }
            }

            return module;
        }

        private void setMethodVisibility(RubyModule module, final String methodName) {
            final InternalMethod method = module.deepMethodSearch(methodName);

            if (method == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().nameErrorUndefinedMethod(methodName, module, this));
            }

            /*
             * If the method was already defined in this class, that's fine
             * {@link addMethod} will overwrite it, otherwise we do actually
             * want to add a copy of the method with a different visibility
             * to this module.
             */
            if (visibility == Visibility.MODULE_FUNCTION) {
                module.addMethod(this, method.withVisibility(Visibility.PRIVATE));
                module.getSingletonClass(this).addMethod(this, method.withVisibility(Visibility.PUBLIC));
            } else {
                module.addMethod(this, method.withVisibility(visibility));
            }
        }

        private void setCurrentVisibility(Visibility visibility) {
            CompilerDirectives.transferToInterpreter();

            final Frame callerFrame = Truffle.getRuntime().getCallerFrame().getFrame(FrameInstance.FrameAccess.READ_WRITE, false);

            assert callerFrame != null;
            assert callerFrame.getFrameDescriptor() != null;

            final FrameSlot visibilitySlot = callerFrame.getFrameDescriptor().findOrAddFrameSlot(
                    RubyModule.VISIBILITY_FRAME_SLOT_ID, "visibility for frame", FrameSlotKind.Object);

            callerFrame.setObject(visibilitySlot, visibility);
        }

    }

}
