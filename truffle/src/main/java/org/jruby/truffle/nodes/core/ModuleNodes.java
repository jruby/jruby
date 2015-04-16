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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;

import org.jcodings.Encoding;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeFactory;
import org.jruby.truffle.nodes.coerce.SymbolOrToStrNodeFactory;
import org.jruby.truffle.nodes.coerce.ToStrNodeFactory;
import org.jruby.truffle.nodes.control.SequenceNode;
import org.jruby.truffle.nodes.core.KernelNodes.BindingNode;
import org.jruby.truffle.nodes.dispatch.*;
import org.jruby.truffle.nodes.methods.SetMethodDeclarationContext;
import org.jruby.truffle.nodes.methods.arguments.CheckArityNode;
import org.jruby.truffle.nodes.methods.arguments.MissingArgumentBehaviour;
import org.jruby.truffle.nodes.methods.arguments.ReadPreArgumentNode;
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

@CoreClass(name = "Module")
public abstract class ModuleNodes {

    @CoreMethod(names = "===", required = 1)
    public abstract static class ContainsInstanceNode extends CoreMethodNode {

        @Child private MetaClassNode metaClassNode;
        
        public ContainsInstanceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            metaClassNode = MetaClassNodeFactory.create(context, sourceSection, null);
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
    public abstract static class IsSubclassOfNode extends CoreMethodNode {

        public IsSubclassOfNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract Object executeIsSubclassOf(VirtualFrame frame, RubyModule self, RubyModule other);

        @Specialization
        public Object isSubclassOf(VirtualFrame frame, RubyModule self, RubyModule other) {
            notDesignedForCompilation();

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
            notDesignedForCompilation();

            throw new RaiseException(getContext().getCoreLibrary().typeError("compared with non class/module", this));
        }

    }

    @CoreMethod(names = "<=", required = 1)
    public abstract static class IsSubclassOfOrEqualToNode extends CoreMethodNode {

        public IsSubclassOfOrEqualToNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract Object executeIsSubclassOfOrEqualTo(VirtualFrame frame, RubyModule self, RubyModule other);

        @Specialization
        public Object isSubclassOfOrEqualTo(VirtualFrame frame, RubyModule self, RubyModule other) {
            notDesignedForCompilation();

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
            notDesignedForCompilation();

            throw new RaiseException(getContext().getCoreLibrary().typeError("compared with non class/module", this));
        }

    }

    @CoreMethod(names = ">", required = 1)
    public abstract static class IsSuperclassOfNode extends CoreMethodNode {

        public IsSuperclassOfNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract Object executeIsSuperclassOf(VirtualFrame frame, RubyModule self, RubyModule other);

        @Specialization
        public Object isSuperclassOf(VirtualFrame frame, RubyModule self, RubyModule other) {
            notDesignedForCompilation();

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
            notDesignedForCompilation();

            throw new RaiseException(getContext().getCoreLibrary().typeError("compared with non class/module", this));
        }

    }

    @CoreMethod(names = ">=", required = 1)
    public abstract static class IsSuperclassOfOrEqualToNode extends CoreMethodNode {

        public IsSuperclassOfOrEqualToNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract Object executeIsSuperclassOfOrEqualTo(VirtualFrame frame, RubyModule self, RubyModule other);

        @Specialization
        public Object isSuperclassOfOrEqualTo(VirtualFrame frame, RubyModule self, RubyModule other) {
            notDesignedForCompilation();

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
            notDesignedForCompilation();

            throw new RaiseException(getContext().getCoreLibrary().typeError("compared with non class/module", this));
        }

    }

    @CoreMethod(names = "<=>", required = 1)
    public abstract static class CompareNode extends CoreMethodNode {

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
                return nil();
            } else if (booleanCast(frame, isSubclass)) {
                return -1;
            }
            return 1;
        }

        @Specialization
        public Object compare(VirtualFrame frame, RubyModule self, RubyBasicObject other) {
            notDesignedForCompilation();

            return nil();
        }

    }

    @CoreMethod(names = "alias_method", required = 2)
    public abstract static class AliasMethodNode extends CoreMethodNode {

        public AliasMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyModule aliasMethod(RubyModule module, RubySymbol newName, RubySymbol oldName) {
            notDesignedForCompilation();

            module.alias(this, newName.toString(), oldName.toString());
            return module;
        }

        @Specialization
        public RubyModule aliasMethod(RubyModule module, RubyString newName, RubyString oldName) {
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

    @CoreMethod(names = "append_features", required = 1, visibility = Visibility.PRIVATE)
    public abstract static class AppendFeaturesNode extends CoreMethodNode {

        public AppendFeaturesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyNilClass appendFeatures(RubyModule module, RubyModule other) {
            notDesignedForCompilation();

            module.appendFeatures(this, other);
            return nil();
        }
    }

    @CoreMethod(names = "attr_reader", argumentsAsArray = true)
    public abstract static class AttrReaderNode extends CoreMethodNode {

        public AttrReaderNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyNilClass attrReader(RubyModule module, Object[] args) {
            notDesignedForCompilation();

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
    public abstract static class AttrWriterNode extends CoreMethodNode {

        public AttrWriterNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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
    public abstract static class AttrAccessorNode extends CoreMethodNode {

        public AttrAccessorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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
            @NodeChild(value = "module"),
            @NodeChild(value = "name"),
            @NodeChild(value = "filename")
    })
    public abstract static class AutoloadNode extends RubyNode {

        @Child private StringNodes.EmptyNode emptyNode;
        private final ConditionProfile invalidConstantName = ConditionProfile.createBinaryProfile();
        private final ConditionProfile emptyFilename = ConditionProfile.createBinaryProfile();

        public AutoloadNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            emptyNode = StringNodesFactory.EmptyNodeFactory.create(context, sourceSection, new RubyNode[]{});
        }

        @CreateCast("filename") public RubyNode coerceFilenameToString(RubyNode filename) {
            return ToStrNodeFactory.create(getContext(), getSourceSection(), filename);
        }

        @Specialization
        public RubyNilClass autoload(RubyModule module, RubySymbol name, RubyString filename) {
            return autoload(module, name.toString(), filename);
        }

        @Specialization
        public RubyNilClass autoload(RubyModule module, RubyString name, RubyString filename) {
            return autoload(module, name.toString(), filename);
        }

        private RubyNilClass autoload(RubyModule module, String name, RubyString filename) {
            if (invalidConstantName.profile(!IdUtil.isValidConstantName19(name))) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().nameError(String.format("autoload must be constant name: %s", name), this));
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
    public abstract static class AutoloadQueryNode extends CoreMethodNode {

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
    public abstract static class ClassEvalNode extends CoreMethodNode {

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
            notDesignedForCompilation();

            final Source source = Source.fromText(code.toString(), "(eval)");
            return classEvalSource(frame, module, source, code.getBytes().getEncoding());
        }

        @Specialization
        public Object classEval(VirtualFrame frame, RubyModule module, RubyString code, RubyString file, UndefinedPlaceholder line, UndefinedPlaceholder block) {
            notDesignedForCompilation();

            final Source source = Source.asPseudoFile(code.toString(), file.toString());
            return classEvalSource(frame, module, source, code.getBytes().getEncoding());
        }

        @Specialization
        public Object classEval(VirtualFrame frame, RubyModule module, RubyString code, RubyString file, int line, UndefinedPlaceholder block) {
            notDesignedForCompilation();

            final Source source = Source.asPseudoFile(code.toString(), file.toString());
            return classEvalSource(frame, module, source, code.getBytes().getEncoding());
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

        @Child private YieldDispatchHeadNode yield;

        public ClassExecNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            yield = new YieldDispatchHeadNode(context);
        }

        public abstract Object executeClassExec(VirtualFrame frame, RubyModule self, Object[] args, RubyProc block);

        @Specialization
        public Object classExec(VirtualFrame frame, RubyModule self, Object[] args, RubyProc block) {
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
    @NodeChildren({ @NodeChild("module"), @NodeChild("name"), @NodeChild("inherit") })
    public abstract static class ConstDefinedNode extends RubyNode {

        public ConstDefinedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return SymbolOrToStrNodeFactory.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        public boolean isConstDefined(RubyModule module, String name, UndefinedPlaceholder inherit) {
            return isConstDefined(module, name, true);
        }

        @Specialization
        public boolean isConstDefined(RubyModule module, String fullName, boolean inherit) {
            notDesignedForCompilation();

            int start = 0, next;
            if (fullName.startsWith("::")) {
                module = getContext().getCoreLibrary().getObjectClass();
                start += 2;
            }

            while ((next = fullName.indexOf("::", start)) != -1) {
                String segment = fullName.substring(start, next);
                RubyConstant constant = lookup(module, segment, inherit);
                if (constant == null) {
                    return false;
                } else if (constant.getValue() instanceof RubyModule) {
                    module = (RubyModule) constant.getValue();
                } else {
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().typeError(fullName.substring(0, next) + " does not refer to class/module", this));
                }
                start = next + 2;
            }

            String lastSegment = fullName.substring(start);
            return lookup(module, lastSegment, inherit) != null;
        }

        private RubyConstant lookup(RubyModule module, String name, boolean inherit) {
            if (!IdUtil.isValidConstantName19(name)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().nameError(String.format("wrong constant name %s", name), this));
            }

            if (inherit) {
                return ModuleOperations.lookupConstant(getContext(), LexicalScope.NONE, module, name);
            } else {
                return module.getConstants().get(name);
            }
        }

    }

    @CoreMethod(names = "const_get", required = 1)
    public abstract static class ConstGetNode extends CoreMethodNode {

        @Child private DispatchHeadNode dispatch;

        public ConstGetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            dispatch = new DispatchHeadNode(context, false, false, MissingBehavior.CALL_CONST_MISSING, null, DispatchAction.READ_CONSTANT);
        }

        @Specialization
        public Object getConstant(VirtualFrame frame, RubyModule module, RubyString name) {
            notDesignedForCompilation();

            return dispatch.dispatch(
                    frame,
                    module,
                    name,
                    null,
                    new Object[]{});
        }

        @Specialization
        public Object getConstant(VirtualFrame frame, RubyModule module, RubySymbol name) {
            notDesignedForCompilation();

            return dispatch.dispatch(
                    frame,
                    module,
                    name,
                    null,
                    new Object[]{});
        }
    }

    @CoreMethod(names = "const_missing", required = 1)
    public abstract static class ConstMissingNode extends CoreMethodNode {

        public ConstMissingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object methodMissing(RubyModule module, RubySymbol name) {
            throw new RaiseException(getContext().getCoreLibrary().nameErrorUninitializedConstant(module, name.toString(), this));
        }

    }

    @CoreMethod(names = "const_set", required = 2)
    @NodeChildren({ @NodeChild("module"), @NodeChild("name"), @NodeChild("value") })
    public abstract static class ConstSetNode extends RubyNode {

        public ConstSetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return SymbolOrToStrNodeFactory.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        public Object setConstant(RubyModule module, String name, Object value) {
            notDesignedForCompilation();

            if (!IdUtil.isValidConstantName19(name)) {
                throw new RaiseException(getContext().getCoreLibrary().nameError(String.format("wrong constant name %s", name), this));
            }

            module.setConstant(this, name, value);
            return value;
        }

    }

    @CoreMethod(names = "define_method", needsBlock = true, required = 1, optional = 1, visibility = Visibility.PRIVATE)
    @NodeChildren({ @NodeChild("module"), @NodeChild("name"), @NodeChild("proc"), @NodeChild("block") })
    public abstract static class DefineMethodNode extends RubyNode {

        public DefineMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return SymbolOrToStrNodeFactory.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        public RubySymbol defineMethod(RubyModule module, String name, UndefinedPlaceholder proc, UndefinedPlaceholder block) {
            notDesignedForCompilation();
            throw new RaiseException(getContext().getCoreLibrary().argumentError("needs either proc or block", this));
        }

        @Specialization
        public RubySymbol defineMethod(RubyModule module, String name, UndefinedPlaceholder proc, RubyProc block) {
            notDesignedForCompilation();

            return defineMethod(module, name, block, UndefinedPlaceholder.INSTANCE);
        }

        @Specialization
        public RubySymbol defineMethod(RubyModule module, String name, RubyProc proc, UndefinedPlaceholder block) {
            return defineMethod(module, name, proc);
        }

        @Specialization
        public RubySymbol defineMethod(RubyModule module, String name, RubyMethod method, UndefinedPlaceholder block) {
            notDesignedForCompilation();

            module.addMethod(this, method.getMethod().withNewName(name));

            return getContext().getSymbolTable().getSymbol(name);
        }

        @Specialization
        public RubySymbol defineMethod(VirtualFrame frame, RubyModule module, String name, RubyUnboundMethod method, UndefinedPlaceholder block) {
            notDesignedForCompilation();

            RubyModule origin = method.getOrigin();
            if (!ModuleOperations.canBindMethodTo(origin, module)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().typeError("bind argument must be a subclass of " + origin.getName(), this));
            }

            // TODO CS 5-Apr-15 TypeError if the method came from a singleton

            module.addMethod(this, method.getMethod().withNewName(name));

            return getContext().getSymbolTable().getSymbol(name);
        }

        private RubySymbol defineMethod(RubyModule module, String name, RubyProc proc) {
            notDesignedForCompilation();

            final CallTarget modifiedCallTarget = proc.getCallTargetForMethods();
            final SharedMethodInfo info = proc.getSharedMethodInfo().withName(name);
            final InternalMethod modifiedMethod = new InternalMethod(info, name, module, Visibility.PUBLIC, false, modifiedCallTarget, proc.getDeclarationFrame());
            module.addMethod(this, modifiedMethod);

            return getContext().getSymbolTable().getSymbol(name);
        }

    }

    @CoreMethod(names = "extend_object", required = 1)
    public abstract static class ExtendObjectNode extends CoreMethodNode {

        public ExtendObjectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject extendObject(RubyModule module, RubyBasicObject object) {
            notDesignedForCompilation();

            object.getSingletonClass(this).include(this, module);
            return module;
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true)
    public abstract static class InitializeNode extends CoreMethodNode {

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

    @CoreMethod(names = "initialize_copy", visibility = Visibility.PRIVATE, required = 1)
    public abstract static class InitializeCopyNode extends CoreMethodNode {

        public InitializeCopyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = { "!isRubyClass(self)", "!isRubyClass(from)" })
        public Object initializeCopy(RubyModule self, RubyModule from) {
            notDesignedForCompilation();

            self.initCopy(from);
            return nil();
        }

        @Specialization
        public Object initializeCopy(RubyClass self, RubyClass from) {
            notDesignedForCompilation();

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

    @CoreMethod(names = "include", argumentsAsArray = true, required = 1)
    public abstract static class IncludeNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode appendFeaturesNode;
        @Child private CallDispatchHeadNode includedNode;

        public IncludeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            appendFeaturesNode = DispatchHeadNodeFactory.createMethodCall(context, true);
            includedNode = DispatchHeadNodeFactory.createMethodCall(context, true);
        }

        @Specialization
        public RubyNilClass include(VirtualFrame frame, RubyModule module, Object[] args) {
            notDesignedForCompilation();

            // Note that we traverse the arguments backwards

            for (int n = args.length - 1; n >= 0; n--) {
                if (args[n] instanceof RubyModule) {
                    final RubyModule included = (RubyModule) args[n];

                    appendFeaturesNode.call(frame, included, "append_features", null, module);
                    includedNode.call(frame, included, "included", null, module);
                }
            }

            return nil();
        }
    }

    @CoreMethod(names = "include?", required = 1)
    public abstract static class IncludePNode extends CoreMethodNode {

        @Child private DispatchHeadNode appendFeaturesNode;

        public IncludePNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            appendFeaturesNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization
        public boolean include(RubyModule module, RubyModule included) {
            notDesignedForCompilation();

            ModuleChain ancestor = module.getParentModule();

            while (ancestor != null) {
                if (ancestor.getActualModule() == included) {
                    return true;
                }

                ancestor = ancestor.getParentModule();
            }

            return false;
        }
    }

    @CoreMethod(names = "included", required = 1, visibility = Visibility.PRIVATE)
    public abstract static class IncludedNode extends CoreMethodNode {

        public IncludedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyNilClass included(Object subclass) {
            return nil();
        }

    }

    @CoreMethod(names = "included_modules")
    public abstract static class IncludedModulesNode extends CoreMethodNode {

        public IncludedModulesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        RubyArray includedModules(RubyModule module) {
            notDesignedForCompilation();

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
    public abstract static class MethodDefinedNode extends CoreMethodNode {

        public MethodDefinedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean isMethodDefined(RubyModule module, RubyString name, UndefinedPlaceholder inherit) {
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
        public boolean isMethodDefined(RubyModule module, RubySymbol name, UndefinedPlaceholder inherit) {
            notDesignedForCompilation();

            return ModuleOperations.lookupMethod(module, name.toString()) != null;
        }
    }

    @CoreMethod(names = "module_function", argumentsAsArray = true)
    public abstract static class ModuleFunctionNode extends CoreMethodNode {

        public ModuleFunctionNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyModule moduleFunction(RubyModule module, Object... args) {
            notDesignedForCompilation();

            module.visibilityMethod(this, args, Visibility.MODULE_FUNCTION);
            return module;
        }
    }

    @CoreMethod(names = "name")
    public abstract static class NameNode extends CoreMethodNode {

        public NameNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object name(RubyModule module) {
            notDesignedForCompilation();

            if (!module.hasName()) {
                return nil();
            }

            return getContext().makeString(module.getName());
        }
    }

    @CoreMethod(names = "nesting", onSingleton = true)
    public abstract static class NestingNode extends CoreMethodNode {

        public NestingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyArray nesting(VirtualFrame frame) {
            notDesignedForCompilation();

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

    @CoreMethod(names = "public", argumentsAsArray = true)
    public abstract static class PublicNode extends CoreMethodNode {

        public PublicNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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

                final InternalMethod method = ModuleOperations.lookupMethod(moduleSingleton, methodName);

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

                final InternalMethod method = ModuleOperations.lookupMethod(moduleSingleton, methodName);

                if (method == null) {
                    throw new RuntimeException("Couldn't find method " + arg.toString());
                }

                moduleSingleton.addMethod(this, method.withVisibility(Visibility.PRIVATE));
            }

            return module;
        }
    }

    @CoreMethod(names = "protected_instance_methods", optional = 1)
    public abstract static class ProtectedInstanceMethodsNode extends CoreMethodNode {

        public ProtectedInstanceMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyArray protectedInstanceMethods(RubyModule module, UndefinedPlaceholder argument) {
            return protectedInstanceMethods(module, false);
        }

        @Specialization
        public RubyArray protectedInstanceMethods(RubyModule module, boolean includeAncestors) {
            notDesignedForCompilation();


            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(),
                    module.filterMethods(includeAncestors, new RubyModule.MethodFilter() {

                        @Override
                        public boolean filter(InternalMethod method) {
                            return method.getVisibility() == Visibility.PROTECTED;
                        }

                    }).toArray());
        }
    }

    @CoreMethod(names = "private_instance_methods", optional = 1)
    public abstract static class PrivateInstanceMethodsNode extends CoreMethodNode {

        public PrivateInstanceMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyArray privateInstanceMethods(RubyModule module, UndefinedPlaceholder argument) {
            return privateInstanceMethods(module, false);
        }

        @Specialization
        public RubyArray privateInstanceMethods(RubyModule module, boolean includeAncestors) {
            notDesignedForCompilation();

            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(),
                    module.filterMethods(includeAncestors, new RubyModule.MethodFilter() {

                        @Override
                        public boolean filter(InternalMethod method) {
                            return method.getVisibility() == Visibility.PRIVATE;
                        }

                    }).toArray());
        }
    }

    @CoreMethod(names = "public_instance_methods", optional = 1)
    public abstract static class PublicInstanceMethodsNode extends CoreMethodNode {

        public PublicInstanceMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyArray publicInstanceMethods(RubyModule module, UndefinedPlaceholder argument) {
            return publicInstanceMethods(module, false);
        }

        @Specialization
        public RubyArray publicInstanceMethods(RubyModule module, boolean includeAncestors) {
            notDesignedForCompilation();

            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(),
                    module.filterMethods(includeAncestors, new RubyModule.MethodFilter() {

                        @Override
                        public boolean filter(InternalMethod method) {
                            return method.getVisibility() == Visibility.PUBLIC;
                        }

                    }).toArray());
        }
    }

    @CoreMethod(names = "instance_methods", optional = 1)
    public abstract static class InstanceMethodsNode extends CoreMethodNode {

        public InstanceMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyArray instanceMethods(RubyModule module, UndefinedPlaceholder argument) {
            notDesignedForCompilation();

            return instanceMethods(module, true);
        }

        @Specialization
        public RubyArray instanceMethods(RubyModule module, boolean includeAncestors) {
            notDesignedForCompilation();

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
                    array.slowPush(getContext().newSymbol(method.getName()));
                }
            }

            return array;
        }
    }

    @CoreMethod(names = "instance_method", required = 1)
    public abstract static class InstanceMethodNode extends CoreMethodNode {

        public InstanceMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyUnboundMethod instanceMethod(RubyModule module, RubySymbol name) {
            notDesignedForCompilation();

            // TODO(CS, 11-Jan-15) cache this lookup

            final InternalMethod method = ModuleOperations.lookupMethod(module, name.toString());

            if (method == null) {
                throw new UnsupportedOperationException();
            }

            return new RubyUnboundMethod(getContext().getCoreLibrary().getUnboundMethodClass(), module, method);
        }

    }

    @CoreMethod(names = "private_constant", argumentsAsArray = true)
    public abstract static class PrivateConstantNode extends CoreMethodNode {

        public PrivateConstantNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyModule privateConstant(RubyModule module, Object[] args) {
            notDesignedForCompilation();

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
    public abstract static class PublicConstantNode extends CoreMethodNode {

        public PublicConstantNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyModule publicConstant(RubyModule module, Object[] args) {
            notDesignedForCompilation();

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

    @CoreMethod(names = "protected", argumentsAsArray = true)
    public abstract static class ProtectedNode extends CoreMethodNode {

        public ProtectedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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

    @CoreMethod(names = "remove_const", required = 1, visibility = Visibility.PRIVATE)
    @NodeChildren({ @NodeChild("module"), @NodeChild("name") })
    public abstract static class RemoveConstNode extends RubyNode {

        public RemoveConstNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return SymbolOrToStrNodeFactory.create(getContext(), getSourceSection(), name);
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
    public abstract static class RemoveMethodNode extends CoreMethodNode {

        public RemoveMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyModule removeMethod(RubyModule module, Object[] args) {
            notDesignedForCompilation();

            for (Object arg : args) {
                final String name;

                if (arg instanceof RubySymbol) {
                    name = ((RubySymbol) arg).toString();
                } else if (arg instanceof RubyString) {
                    name = ((RubyString) arg).toString();
                } else {
                    // TODO BF 9-APR-2015 the MRI message calls inspect for error message i think
                    throw new RaiseException(getContext().getCoreLibrary().typeError(" is not a symbol", this));
                }
                module.removeMethod(this, name);

            }

            return module;
        }

    }

    @CoreMethod(names = { "to_s", "inspect" })
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString toS(RubyModule module) {
            notDesignedForCompilation();

            return getContext().makeString(module.getName());
        }

    }

    @CoreMethod(names = "undef_method", required = 1)
    public abstract static class UndefMethodNode extends CoreMethodNode {

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
            notDesignedForCompilation();

            final InternalMethod method = ModuleOperations.lookupMethod(module, name);
            if (method == null) {
                throw new RaiseException(getContext().getCoreLibrary().noMethodErrorOnModule(name, module, this));
            }
            module.undefMethod(this, method);
            return module;
        }

    }
}
