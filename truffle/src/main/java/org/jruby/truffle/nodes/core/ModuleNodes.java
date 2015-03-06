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

        public ContainsInstanceNode(ContainsInstanceNode prev) {
            super(prev);
            metaClassNode = prev.metaClassNode;
        }

        @Specialization
        public boolean containsInstance(RubyModule module, RubyBasicObject instance) {
            return includes(instance.getMetaClass(), module);
        }

        @Specialization(guards = "!isRubyBasicObject(arguments[1])")
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

        public IsSubclassOfNode(IsSubclassOfNode prev) {
            super(prev);
        }

        public abstract Object executeIsSubclassOf(VirtualFrame frame, RubyModule self, RubyModule other);

        @Specialization
        public Object isSubclassOf(VirtualFrame frame, RubyModule self, RubyModule other) {
            notDesignedForCompilation("5198b4d15b08423499fd677f735b7f41");

            if (self == other) {
                return false;
            }

            if (ModuleOperations.includesModule(self, other)) {
                return true;
            }

            if (ModuleOperations.includesModule(other, self)) {
                return false;
            }

            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization
        public Object isSubclassOf(VirtualFrame frame, RubyModule self, RubyBasicObject other) {
            notDesignedForCompilation("d1bb53f5164343619c11d1f69600c428");

            throw new RaiseException(getContext().getCoreLibrary().typeError("compared with non class/module", this));
        }

    }

    @CoreMethod(names = "<=", required = 1)
    public abstract static class IsSubclassOfOrEqualToNode extends CoreMethodNode {

        public IsSubclassOfOrEqualToNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IsSubclassOfOrEqualToNode(IsSubclassOfOrEqualToNode prev) {
            super(prev);
        }

        public abstract Object executeIsSubclassOfOrEqualTo(VirtualFrame frame, RubyModule self, RubyModule other);

        @Specialization
        public Object isSubclassOfOrEqualTo(VirtualFrame frame, RubyModule self, RubyModule other) {
            notDesignedForCompilation("f17ac0072e6140bbb5fb868ce35494c5");

            if (self == other || ModuleOperations.includesModule(self, other)) {
                return true;
            }

            if (ModuleOperations.includesModule(other, self)) {
                return false;
            }

            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization
        public Object isSubclassOfOrEqualTo(VirtualFrame frame, RubyModule self, RubyBasicObject other) {
            notDesignedForCompilation("e2ebc8ef84ac42d59d8afb1d548f2f2c");

            throw new RaiseException(getContext().getCoreLibrary().typeError("compared with non class/module", this));
        }

    }

    @CoreMethod(names = ">", required = 1)
    public abstract static class IsSuperclassOfNode extends CoreMethodNode {

        public IsSuperclassOfNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IsSuperclassOfNode(IsSuperclassOfNode prev) {
            super(prev);
        }

        public abstract Object executeIsSuperclassOf(VirtualFrame frame, RubyModule self, RubyModule other);

        @Specialization
        public Object isSuperclassOf(VirtualFrame frame, RubyModule self, RubyModule other) {
            notDesignedForCompilation("2c4abf17e460443697e4870ecab9ce3e");

            if (self == other) {
                return false;
            }

            if (ModuleOperations.includesModule(other, self)) {
                return true;
            }

            if (ModuleOperations.includesModule(self, other)) {
                return false;
            }

            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization
        public Object isSuperclassOf(VirtualFrame frame, RubyModule self, RubyBasicObject other) {
            notDesignedForCompilation("d8ac468507bd4af08d495f1b1cfdddb8");

            throw new RaiseException(getContext().getCoreLibrary().typeError("compared with non class/module", this));
        }

    }

    @CoreMethod(names = ">=", required = 1)
    public abstract static class IsSuperclassOfOrEqualToNode extends CoreMethodNode {

        public IsSuperclassOfOrEqualToNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IsSuperclassOfOrEqualToNode(IsSuperclassOfOrEqualToNode prev) {
            super(prev);
        }

        public abstract Object executeIsSuperclassOfOrEqualTo(VirtualFrame frame, RubyModule self, RubyModule other);

        @Specialization
        public Object isSuperclassOfOrEqualTo(VirtualFrame frame, RubyModule self, RubyModule other) {
            notDesignedForCompilation("6d9fb2610a884fe98cdfddfa1466bc17");

            if (self == other || ModuleOperations.includesModule(other, self)) {
                return true;
            }

            if (ModuleOperations.includesModule(self, other)) {
                return false;
            }

            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization
        public Object isSuperclassOfOrEqualTo(VirtualFrame frame, RubyModule self, RubyBasicObject other) {
            notDesignedForCompilation("4219c729049d4847a5922974d7dc9d32");

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

        public CompareNode(CompareNode prev) {
            super(prev);
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
            notDesignedForCompilation("02cead72dfa74a8a929fa68c2daf3b9f");

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
            notDesignedForCompilation("e4782cb159d74b3e886327864da757ac");

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
            notDesignedForCompilation("3644aceef0ce45a4849ff2701a539500");

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
            notDesignedForCompilation("31854e8926db474c9a6b070d9618144f");

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
            notDesignedForCompilation("67ff7193ce9944b49882d2b7d9704a47");

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
            notDesignedForCompilation("984a666c79db4aabb531a2b6a95d822e");

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

        public static void attrReader(Node currentNode, RubyContext context, SourceSection sourceSection, RubyModule module, String name) {
            CompilerDirectives.transferToInterpreter();

            final CheckArityNode checkArity = new CheckArityNode(context, sourceSection, new Arity(0, 0, false, false));

            final SelfNode self = new SelfNode(context, sourceSection);
            final ReadInstanceVariableNode readInstanceVariable = new ReadInstanceVariableNode(context, sourceSection, "@" + name, self, false);

            final RubyNode block = SequenceNode.sequence(context, sourceSection, checkArity, readInstanceVariable);

            final String indicativeName = name + "(attr_reader)";

            final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, null, indicativeName, false, null, false);
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

        public AttrWriterNode(AttrWriterNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass attrWriter(RubyModule module, Object[] args) {
            notDesignedForCompilation("7a9e9935f7604621869e7401f797a9c4");

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

        public static void attrWriter(Node currentNode, RubyContext context, SourceSection sourceSection, RubyModule module, String name) {
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
            final InternalMethod method = new InternalMethod(sharedMethodInfo, name + "=", module, Visibility.PUBLIC, false, callTarget, null);
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
            notDesignedForCompilation("56d1b482ed824b72a11439ce34e76050");

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

        public AutoloadNode(AutoloadNode prev) {
            super(prev);
            emptyNode = prev.emptyNode;
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

            return getContext().getCoreLibrary().getNilObject();
        }
    }

    @CoreMethod(names = "autoload?", required = 1)
    public abstract static class AutoloadQueryNode extends CoreMethodNode {

        public AutoloadQueryNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AutoloadQueryNode(AutoloadQueryNode prev) {
            super(prev);
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
                return getContext().getCoreLibrary().getNilObject();
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

        public ClassEvalNode(ClassEvalNode prev) {
            super(prev);
            yield = prev.yield;
        }

        protected RubyBinding getCallerBinding(VirtualFrame frame) {
            if (bindingNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                bindingNode = insert(KernelNodesFactory.BindingNodeFactory.create(getContext(), getSourceSection(), new RubyNode[] {}));
            }
            return bindingNode.executeRubyBinding(frame);
        }

        @Specialization
        public Object classEval(VirtualFrame frame, RubyModule module, RubyString code, @SuppressWarnings("unused") UndefinedPlaceholder file, @SuppressWarnings("unused") UndefinedPlaceholder line, @SuppressWarnings("unused") UndefinedPlaceholder block) {
            notDesignedForCompilation("2e784f78806746d6a7ba85c7a290ce4d");

            final Source source = Source.fromText(code.toString(), "(eval)");
            return classEvalSource(frame, module, source, code.getBytes().getEncoding());
        }

        @Specialization
        public Object classEval(VirtualFrame frame, RubyModule module, RubyString code, RubyString file, @SuppressWarnings("unused") UndefinedPlaceholder line, @SuppressWarnings("unused") UndefinedPlaceholder block) {
            notDesignedForCompilation("1baf86f1618447279db51f5097c4a9cb");

            final Source source = Source.asPseudoFile(code.toString(), file.toString());
            return classEvalSource(frame, module, source, code.getBytes().getEncoding());
        }

        @Specialization
        public Object classEval(VirtualFrame frame, RubyModule module, RubyString code, RubyString file, @SuppressWarnings("unused") int line, @SuppressWarnings("unused") UndefinedPlaceholder block) {
            notDesignedForCompilation("f0c26f50f23c465cbb9ebc996623138f");

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
        public Object classEval(VirtualFrame frame, RubyModule self, @SuppressWarnings("unused") UndefinedPlaceholder code, @SuppressWarnings("unused") UndefinedPlaceholder file, @SuppressWarnings("unused") UndefinedPlaceholder line, RubyProc block) {
            notDesignedForCompilation("1e17e8986aaa458694cb8695f335ba3f");

            return yield.dispatchWithModifiedSelf(frame, block, self);
        }

        @Specialization
        public Object classEval(RubyModule self, UndefinedPlaceholder code, UndefinedPlaceholder file, UndefinedPlaceholder line, UndefinedPlaceholder block) {
            notDesignedForCompilation("7c3ec8ebaaee4b18a3cb4df6da0deb6e");

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

        public ClassExecNode(ClassExecNode prev) {
            super(prev);
            yield = prev.yield;
        }

        public abstract Object executeClassEval(VirtualFrame frame, RubyModule self, Object[] args, RubyProc block);

        @Specialization
        public Object classExec(VirtualFrame frame, RubyModule self, Object[] args, RubyProc block) {
            notDesignedForCompilation("53489176d5654167845f939f621413ad");

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
            notDesignedForCompilation("bf19b1bf42924a149a2d46e4d974ffb8");

            return module.getClassVariables().containsKey(name.toString());
        }

        @Specialization
        public boolean isClassVariableDefined(RubyModule module, RubySymbol name) {
            notDesignedForCompilation("2e8847399ed64f5398f18fdd26d372b0");

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
            notDesignedForCompilation("d089b2e5ea2942beac76618102c77d0d");
            return ModuleOperations.lookupClassVariable(module, RubyContext.checkClassVariableName(getContext(), name.toString(), this));
        }

        @Specialization
        public Object getClassVariable(RubyModule module, RubySymbol name) {
            notDesignedForCompilation("1bbd8c25e290404980dbdbf491cb0039");
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
            notDesignedForCompilation("3721249ac75c4a2d9a9b868db9143e82");

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
            notDesignedForCompilation("64953a749b8f429ca0ae90fd525a8ecd");

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
            notDesignedForCompilation("8f21a99f59814df8bf2cfd04a77c90c6");

            return ModuleOperations.lookupConstant(getContext(), LexicalScope.NONE, module, name.toString()) != null;
        }

        @Specialization
        public boolean isConstDefined(RubyModule module, RubyString name, boolean inherit) {
            notDesignedForCompilation("b84d1f8fcf6e4da9942da26c8e5bd692");

            if (inherit) {
                return ModuleOperations.lookupConstant(getContext(), LexicalScope.NONE, module, name.toString()) != null;
            } else {
                return module.getConstants().containsKey(name.toString());
            }
        }

        @Specialization
        public boolean isConstDefined(RubyModule module, RubySymbol name, @SuppressWarnings("unused") UndefinedPlaceholder inherit) {
            notDesignedForCompilation("b8aaa4f1b7c44cf4bc7a321f734f94e5");

            return ModuleOperations.lookupConstant(getContext(), LexicalScope.NONE, module, name.toString()) != null;
        }

    }

    @CoreMethod(names = "const_get", required = 1)
    public abstract static class ConstGetNode extends CoreMethodNode {

        @Child private DispatchHeadNode dispatch;

        public ConstGetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            dispatch = new DispatchHeadNode(context, false, false, MissingBehavior.CALL_CONST_MISSING, null, DispatchAction.READ_CONSTANT);
        }

        public ConstGetNode(ConstGetNode prev) {
            super(prev);
            dispatch = prev.dispatch;
        }

        @Specialization
        public Object getConstant(VirtualFrame frame, RubyModule module, RubyString name) {
            notDesignedForCompilation("27257bbe647242b09170fa43b3f29fcb");

            return dispatch.dispatch(
                    frame,
                    module,
                    name,
                    null,
                    new Object[]{});
        }

        @Specialization
        public Object getConstant(VirtualFrame frame, RubyModule module, RubySymbol name) {
            notDesignedForCompilation("8b8487df8a004b2683179f51445ec167");

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

        public ConstMissingNode(ConstMissingNode prev) {
            super(prev);
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

        public ConstSetNode(ConstSetNode prev) {
            super(prev);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return SymbolOrToStrNodeFactory.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        public Object setConstant(RubyModule module, String name, Object value) {
            notDesignedForCompilation("5d5e3d320e66445aa664e77dbcc0f980");

            if (!IdUtil.isValidConstantName19(name)) {
                throw new RaiseException(getContext().getCoreLibrary().nameError(String.format("wrong constant name %s", name), this));
            }

            module.setConstant(this, name, value);
            return value;
        }

    }

    @CoreMethod(names = "define_method", needsBlock = true, required = 1, optional = 1)
    @NodeChildren({ @NodeChild("module"), @NodeChild("name"), @NodeChild("proc"), @NodeChild("block") })
    public abstract static class DefineMethodNode extends RubyNode {

        public DefineMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DefineMethodNode(DefineMethodNode prev) {
            super(prev);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return SymbolOrToStrNodeFactory.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        public RubySymbol defineMethod(RubyModule module, String name, UndefinedPlaceholder proc, RubyProc block) {
            notDesignedForCompilation("9c9e0b73b02546c980b4d7a41d026bf4");

            return defineMethod(module, name, block, UndefinedPlaceholder.INSTANCE);
        }

        @Specialization
        public RubySymbol defineMethod(RubyModule module, String name, RubyProc proc, UndefinedPlaceholder block) {
            return defineMethod(module, name, proc);
        }

        @Specialization
        public RubySymbol defineMethod(RubyModule module, String name, RubyMethod method, UndefinedPlaceholder block) {
            notDesignedForCompilation("6ece38c5801d4e3ba78f6c31946d193d");

            module.addMethod(this, method.getMethod().withNewName(name));

            return getContext().getSymbolTable().getSymbol(name);
        }

        private RubySymbol defineMethod(RubyModule module, String name, RubyProc proc) {
            notDesignedForCompilation("1bf0a463a53c4e1d87da91a4a82fb447");

            final CallTarget modifiedCallTarget = proc.getCallTargetForMethods();
            final InternalMethod modifiedMethod = new InternalMethod(proc.getSharedMethodInfo(), name, module, Visibility.PUBLIC, false, modifiedCallTarget, proc.getDeclarationFrame());
            module.addMethod(this, modifiedMethod);

            return getContext().getSymbolTable().getSymbol(name);
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true)
    public abstract static class InitializeNode extends CoreMethodNode {

        @Child private ModuleNodes.ClassExecNode classExecNode;

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
            notDesignedForCompilation("8768834a8ee44e7eabc5dedab9e6f4d0");

            self.initCopy(other);
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "include", argumentsAsArray = true, required = 1)
    public abstract static class IncludeNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode appendFeaturesNode;

        public IncludeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            appendFeaturesNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        public IncludeNode(IncludeNode prev) {
            super(prev);
            appendFeaturesNode = prev.appendFeaturesNode;
        }

        @Specialization
        public RubyNilClass include(VirtualFrame frame, RubyModule module, Object[] args) {
            notDesignedForCompilation("de92a03ba9b847608416013c6c4aa75d");

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

    @CoreMethod(names = "include?", required = 1)
    public abstract static class IncludePNode extends CoreMethodNode {

        @Child private DispatchHeadNode appendFeaturesNode;

        public IncludePNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            appendFeaturesNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        public IncludePNode(IncludePNode prev) {
            super(prev);
            appendFeaturesNode = prev.appendFeaturesNode;
        }

        @Specialization
        public boolean include(RubyModule module, RubyModule included) {
            notDesignedForCompilation("b053f8d8b4584fc1bba07d2addb1571a");

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
            notDesignedForCompilation("e014bea79a6343ffb76a4ae3a2778f44");

            return ModuleOperations.lookupMethod(module, name.toString()) != null;
        }

        @Specialization
        public boolean isMethodDefined(RubyModule module, RubyString name, boolean inherit) {
            notDesignedForCompilation("1b6de8686885484a93a784fd2ac2de15");

            if (inherit) {
                return ModuleOperations.lookupMethod(module, name.toString()) != null;
            } else {
                return module.getMethods().containsKey(name.toString());
            }
        }

        @Specialization
        public boolean isMethodDefined(RubyModule module, RubySymbol name, @SuppressWarnings("unused") UndefinedPlaceholder inherit) {
            notDesignedForCompilation("b91aea21b24744a19fa1d584f6d3a663");

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
        public RubyModule moduleFunction(RubyModule module, Object... args) {
            notDesignedForCompilation("d05688b66333485e818f644a99c1703f");

            module.visibilityMethod(this, args, Visibility.MODULE_FUNCTION);
            return module;
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
        public Object name(RubyModule module) {
            notDesignedForCompilation("f6ac6e8598ee4ac6a9ea2599a5f5bdb6");

            if (!module.hasName()) {
                return getContext().getCoreLibrary().getNilObject();
            }

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
        public RubyArray nesting(VirtualFrame frame) {
            notDesignedForCompilation("2e0c8d8326de4b8b8a50ec6c2c0f0c3e");

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

        public PublicNode(PublicNode prev) {
            super(prev);
        }

        public abstract RubyModule executePublic(VirtualFrame frame, RubyModule module, Object[] args);

        @Specialization
        public RubyModule doPublic(RubyModule module, Object[] args) {
            notDesignedForCompilation("a106cb1100cb427da5c787c8013857d5");

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
            notDesignedForCompilation("b3a39129eec445bf943499193782501f");

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

        public PrivateNode(PrivateNode prev) {
            super(prev);
        }

        public abstract RubyModule executePrivate(VirtualFrame frame, RubyModule module, Object[] args);

        @Specialization
        public RubyModule doPrivate(RubyModule module, Object[] args) {
            notDesignedForCompilation("f38264029ae747d4b0b6e4039b4bd129");

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
            notDesignedForCompilation("1e81428c6d494f7e9cc40d87d841d0c2");

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
            notDesignedForCompilation("c7298aff05534cfcb6296497b4baeeb1");

            final RubyArray array = new RubyArray(getContext().getCoreLibrary().getArrayClass());
            final List<InternalMethod> methods = new ArrayList<>(module.getMethods().values());

            if (includeAncestors) {
                for (RubyModule parent : module.parentAncestors()) {
                    methods.addAll(parent.getMethods().values());
                }
            }
            for (InternalMethod method : methods) {
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
            notDesignedForCompilation("bd477c46284e4a2592ca53d2ba09e078");

            final RubyArray array = new RubyArray(getContext().getCoreLibrary().getArrayClass());
            final List<InternalMethod> methods = new ArrayList<>(module.getMethods().values());
            if (includeAncestors) {
                for (RubyModule parent : module.parentAncestors()) {
                    methods.addAll(parent.getMethods().values());
                }
            }
            for (InternalMethod method : methods) {
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
            notDesignedForCompilation("df85e37b123b488194df0266fb9cb8b7");

            return instanceMethods(module, true);
        }

        @Specialization
        public RubyArray instanceMethods(RubyModule module, boolean includeAncestors) {
            notDesignedForCompilation("e4117b864a944de3a7fd8201867b64b6");

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

        public InstanceMethodNode(InstanceMethodNode prev) {
            super(prev);
        }

        @Specialization
        public RubyUnboundMethod instanceMethod(RubyModule module, RubySymbol name) {
            notDesignedForCompilation("b5e6817224264ad787305283b170d139");

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

        public PrivateConstantNode(PrivateConstantNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule privateConstant(RubyModule module, Object[] args) {
            notDesignedForCompilation("636ca542510d4336a8764f4a47a837e3");

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
            notDesignedForCompilation("6ec37a77cdbb4eb8b1f54e8df6b677c7");

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
            notDesignedForCompilation("5e4bde10a4794873bbebd5b958ca4605");

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
            notDesignedForCompilation("341bb83e587e4cc493e80c1894fd313a");

            module.removeClassVariable(this, name.toString());
            return module;
        }

        @Specialization
        public RubyModule removeClassVariable(RubyModule module, RubySymbol name) {
            notDesignedForCompilation("714c68634661428197f057204411f50a");

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

        public RemoveConstNode(RemoveConstNode prev) {
            super(prev);
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
            notDesignedForCompilation("c5385ad4c9294a2da674652aae95ac12");

            module.removeMethod(this, name.toString());
            return module;
        }

        @Specialization
        public RubyModule removeMethod(RubyModule module, RubySymbol name) {
            notDesignedForCompilation("9d5e4ccb54e74c06ad0aca90412c1cd9");

            module.removeMethod(this, name.toString());
            return module;
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
        public RubyModule undefMethod(RubyModule module, RubyString name) {
            return undefMethod(module, name.toString());
        }

        @Specialization
        public RubyModule undefMethod(RubyModule module, RubySymbol name) {
            return undefMethod(module, name.toString());
        }

        private RubyModule undefMethod(RubyModule module, String name) {
            notDesignedForCompilation("391152635e2a43b9b8e08ec8fb5b8566");

            final InternalMethod method = ModuleOperations.lookupMethod(module, name);
            if (method == null) {
                throw new RaiseException(getContext().getCoreLibrary().noMethodError(name, module, this));
            }
            module.undefMethod(this, method);
            return module;
        }

    }
}
