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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jcodings.Encoding;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.arguments.CheckArityNode;
import org.jruby.truffle.nodes.arguments.MissingArgumentBehaviour;
import org.jruby.truffle.nodes.arguments.ReadPreArgumentNode;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeGen;
import org.jruby.truffle.nodes.cast.BooleanCastWithDefaultNodeGen;
import org.jruby.truffle.nodes.cast.TaintResultNode;
import org.jruby.truffle.nodes.coerce.*;
import org.jruby.truffle.nodes.constants.GetConstantNode;
import org.jruby.truffle.nodes.constants.GetConstantNodeGen;
import org.jruby.truffle.nodes.constants.LookupConstantNodeGen;
import org.jruby.truffle.nodes.control.SequenceNode;
import org.jruby.truffle.nodes.core.ModuleNodesFactory.GenerateAccessorNodeGen;
import org.jruby.truffle.nodes.core.ModuleNodesFactory.SetMethodVisibilityNodeGen;
import org.jruby.truffle.nodes.core.ModuleNodesFactory.SetVisibilityNodeGen;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.methods.AddMethodNode;
import org.jruby.truffle.nodes.methods.SetMethodDeclarationContext;
import org.jruby.truffle.nodes.objects.*;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.MethodFilter;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.core.RubyModuleModel;
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

    /**
     * The slot within a module definition method frame where we store the implicit state that is
     * the current visibility for new methods.
     */
    public static final Object VISIBILITY_FRAME_SLOT_ID = new Object();

    public static RubyModuleModel getModel(RubyBasicObject module) {
        assert RubyGuards.isRubyModule(module);
        return ((RubyModule) module).model;
    }

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
        
        @TruffleBoundary
        public boolean includes(RubyBasicObject metaClass, RubyBasicObject module) {
            assert RubyGuards.isRubyModule(metaClass);
            assert RubyGuards.isRubyModule(module);
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

    @CoreMethod(names = "alias_method", required = 2, visibility = Visibility.PRIVATE)
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
            return NameToJavaStringNodeGen.create(getContext(), getSourceSection(), newName);
        }

        @CreateCast("oldName")
        public RubyNode coerceOldNameToString(RubyNode oldName) {
            return NameToJavaStringNodeGen.create(getContext(), getSourceSection(), oldName);
        }

        @Specialization
        public RubyBasicObject aliasMethod(RubyModule module, String newName, String oldName) {
            CompilerDirectives.transferToInterpreter();

            getModel(module).alias(this, newName, oldName);
            return module;
        }

    }

    @CoreMethod(names = "ancestors")
    public abstract static class AncestorsNode extends CoreMethodArrayArgumentsNode {

        public AncestorsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject ancestors(RubyModule self) {
            CompilerDirectives.transferToInterpreter();

            final List<RubyBasicObject> ancestors = new ArrayList<>();
            for (RubyBasicObject module : getModel(self).ancestors()) {
                ancestors.add(module);
            }

            return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(), ancestors.toArray(new Object[ancestors.size()]));
        }
    }

    @CoreMethod(names = "append_features", required = 1, visibility = Visibility.PRIVATE)
    public abstract static class AppendFeaturesNode extends CoreMethodArrayArgumentsNode {

        @Child TaintResultNode taintResultNode;

        public AppendFeaturesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            taintResultNode = new TaintResultNode(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject appendFeatures(RubyModule features, RubyModule target) {
            if (RubyGuards.isRubyClass(features)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().typeError("append_features must be called only on modules", this));
            }
            getModel(target).include(this, features);
            taintResultNode.maybeTaint(features, target);
            return nil();
        }
    }

    @NodeChildren({ @NodeChild("module"), @NodeChild("name") })
    public abstract static class GenerateAccessorNode extends RubyNode {

        final boolean isGetter;
        @Child NameToJavaStringNode nameToJavaStringNode;

        public GenerateAccessorNode(RubyContext context, SourceSection sourceSection, boolean isGetter) {
            super(context, sourceSection);
            this.isGetter = isGetter;
            this.nameToJavaStringNode = NameToJavaStringNodeGen.create(context, sourceSection, null);
        }

        public abstract RubyBasicObject executeGenerateAccessor(VirtualFrame frame, RubyModule module, Object name);

        @Specialization
        public RubyBasicObject generateAccessor(VirtualFrame frame, RubyModule module, Object nameObject) {
            final String name = nameToJavaStringNode.executeToJavaString(frame, nameObject);

            CompilerDirectives.transferToInterpreter();
            final FrameInstance callerFrame = RubyCallStack.getCallerFrame(getContext());
            final SourceSection sourceSection = callerFrame.getCallNode().getEncapsulatingSourceSection();
            final Visibility visibility = AddMethodNode.getVisibility(callerFrame.getFrame(FrameAccess.READ_ONLY, true));
            final Arity arity = isGetter ? Arity.NO_ARGUMENTS : Arity.ONE_REQUIRED;
            final String ivar = "@" + name;
            final String accessorName = isGetter ? name : name + "=";
            final String indicativeName = name + "(attr_" + (isGetter ? "reader" : "writer") + ")";

            final CheckArityNode checkArity = new CheckArityNode(getContext(), sourceSection, arity);
            final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, LexicalScope.NONE, arity, indicativeName, false, null, false);

            final SelfNode self = new SelfNode(getContext(), sourceSection);
            final RubyNode accessInstanceVariable;
            if (isGetter) {
                accessInstanceVariable = new ReadInstanceVariableNode(getContext(), sourceSection, ivar, self, false);
            } else {
                ReadPreArgumentNode readArgument = new ReadPreArgumentNode(getContext(), sourceSection, 0, MissingArgumentBehaviour.RUNTIME_ERROR);
                accessInstanceVariable = new WriteInstanceVariableNode(getContext(), sourceSection, ivar, self, readArgument, false);
            }
            final RubyNode sequence = SequenceNode.sequence(getContext(), sourceSection, checkArity, accessInstanceVariable);
            final RubyRootNode rootNode = new RubyRootNode(getContext(), sourceSection, null, sharedMethodInfo, sequence);
            final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
            final InternalMethod method = new InternalMethod(sharedMethodInfo, accessorName, module, visibility, false, callTarget, null);

            getModel(module).addMethod(this, method);
            return nil();
        }
    }

    @CoreMethod(names = "attr", argumentsAsArray = true, visibility = Visibility.PRIVATE)
    public abstract static class AttrNode extends CoreMethodArrayArgumentsNode {

        @Child GenerateAccessorNode generateGetterNode;
        @Child GenerateAccessorNode generateSetterNode;

        public AttrNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.generateGetterNode = GenerateAccessorNodeGen.create(context, sourceSection, true, null, null);
            this.generateSetterNode = GenerateAccessorNodeGen.create(context, sourceSection, false, null, null);
        }

        @Specialization
        public RubyBasicObject attr(VirtualFrame frame, RubyModule module, Object[] names) {
            CompilerDirectives.transferToInterpreter();
            final boolean setter;
            if (names.length == 2 && names[1] instanceof Boolean) {
                setter = (boolean) names[1];
                names = new Object[] { names[0] };
            } else {
                setter = false;
            }

            for (Object name : names) {
                generateGetterNode.executeGenerateAccessor(frame, module, name);
                if (setter) {
                    generateSetterNode.executeGenerateAccessor(frame, module, name);
                }
            }
            return nil();
        }

    }

    @CoreMethod(names = "attr_accessor", argumentsAsArray = true, visibility = Visibility.PRIVATE)
    public abstract static class AttrAccessorNode extends CoreMethodArrayArgumentsNode {

        @Child GenerateAccessorNode generateGetterNode;
        @Child GenerateAccessorNode generateSetterNode;

        public AttrAccessorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.generateGetterNode = GenerateAccessorNodeGen.create(context, sourceSection, true, null, null);
            this.generateSetterNode = GenerateAccessorNodeGen.create(context, sourceSection, false, null, null);
        }

        @Specialization
        public RubyBasicObject attrAccessor(VirtualFrame frame, RubyModule module, Object[] names) {
            for (Object name : names) {
                generateGetterNode.executeGenerateAccessor(frame, module, name);
                generateSetterNode.executeGenerateAccessor(frame, module, name);
            }
            return nil();
        }

    }

    @CoreMethod(names = "attr_reader", argumentsAsArray = true, visibility = Visibility.PRIVATE)
    public abstract static class AttrReaderNode extends CoreMethodArrayArgumentsNode {

        @Child GenerateAccessorNode generateGetterNode;

        public AttrReaderNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.generateGetterNode = GenerateAccessorNodeGen.create(context, sourceSection, true, null, null);
        }

        @Specialization
        public RubyBasicObject attrReader(VirtualFrame frame, RubyModule module, Object[] names) {
            for (Object name : names) {
                generateGetterNode.executeGenerateAccessor(frame, module, name);
            }
            return nil();
        }

    }

    @CoreMethod(names = "attr_writer", argumentsAsArray = true, visibility = Visibility.PRIVATE)
    public abstract static class AttrWriterNode extends CoreMethodArrayArgumentsNode {

        @Child GenerateAccessorNode generateSetterNode;

        public AttrWriterNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.generateSetterNode = GenerateAccessorNodeGen.create(context, sourceSection, false, null, null);
        }

        @Specialization
        public RubyBasicObject attrWriter(VirtualFrame frame, RubyModule module, Object[] names) {
            for (Object name : names) {
                generateSetterNode.executeGenerateAccessor(frame, module, name);
            }
            return nil();
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
        private final ConditionProfile alreadyLoaded = ConditionProfile.createBinaryProfile();

        public AutoloadNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            emptyNode = StringNodesFactory.EmptyNodeFactory.create(context, sourceSection, new RubyNode[]{});
        }

        @CreateCast("name") public RubyNode coerceNameToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(getContext(), getSourceSection(), name);
        }

        @CreateCast("filename") public RubyNode coerceFilenameToPath(RubyNode filename) {
            return ToPathNodeGen.create(getContext(), getSourceSection(), filename);
        }

        @Specialization(guards = "isRubyString(filename)")
        public RubyBasicObject autoload(RubyModule module, String name, RubyBasicObject filename) {
            if (invalidConstantName.profile(!IdUtil.isValidConstantName19(name))) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().nameError(String.format("autoload must be constant name: %s", name), name, this));
            }

            if (emptyFilename.profile(emptyNode.empty(filename))) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("empty file name", this));
            }

            if (alreadyLoaded.profile(getModel(module).getConstants().get(name) != null)) {
                return nil();
            }

            getModel(module).setAutoloadConstant(this, name, filename);

            return nil();
        }
    }

    @CoreMethod(names = "autoload?", required = 1)
    public abstract static class AutoloadQueryNode extends CoreMethodArrayArgumentsNode {

        public AutoloadQueryNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubySymbol(name)")
        public Object autoloadQuerySymbol(RubyModule module, RubyBasicObject name) {
            return autoloadQuery(module, SymbolNodes.getString(name));
        }

        @Specialization(guards = "isRubyString(name)")
        public Object autoloadQueryString(RubyModule module, RubyBasicObject name) {
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
        @Child private ToStrNode toStrNode;

        public ClassEvalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            yield = new YieldDispatchHeadNode(context);
        }

        protected RubyBasicObject toStr(VirtualFrame frame, Object object) {
            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStrNode = insert(ToStrNodeGen.create(getContext(), getSourceSection(), null));
            }
            return toStrNode.executeToStr(frame, object);
        }

        @Specialization(guards = "isRubyString(code)")
        public Object classEval(RubyModule module, RubyBasicObject code, NotProvided file, NotProvided line, NotProvided block) {
            return classEvalSource(module, code, "(eval)");
        }

        @Specialization(guards = {"isRubyString(code)", "isRubyString(file)"})
        public Object classEval(RubyModule module, RubyBasicObject code, RubyBasicObject file, NotProvided line, NotProvided block) {
            return classEvalSource(module, code, file.toString());
        }

        @Specialization(guards = {"isRubyString(code)", "isRubyString(file)"})
        public Object classEval(RubyModule module, RubyBasicObject code, RubyBasicObject file, int line, NotProvided block) {
            return classEvalSource(module, code, file.toString());
        }

        @Specialization(guards = "wasProvided(code)")
        public Object classEval(VirtualFrame frame, RubyModule module, Object code, NotProvided file, NotProvided line, NotProvided block) {
            return classEvalSource(module, toStr(frame, code), file.toString());
        }

        @Specialization(guards = {"isRubyString(code)", "wasProvided(file)"})
        public Object classEval(VirtualFrame frame, RubyModule module, RubyBasicObject code, Object file, NotProvided line, NotProvided block) {
            return classEvalSource(module, code, toStr(frame, file).toString());
        }

        private Object classEvalSource(RubyModule module, RubyBasicObject code, String file) {
            assert RubyGuards.isRubyString(code);

            final MaterializedFrame callerFrame = RubyCallStack.getCallerFrame(getContext())
                    .getFrame(FrameInstance.FrameAccess.MATERIALIZE, false).materialize();
            Encoding encoding = StringNodes.getByteList(code).getEncoding();

            CompilerDirectives.transferToInterpreter();
            Source source = Source.fromText(code.toString(), file);

            return getContext().execute(source, encoding, TranslatorDriver.ParserContext.MODULE, module, callerFrame, this, new NodeWrapper() {
                @Override
                public RubyNode wrap(RubyNode node) {
                    return new SetMethodDeclarationContext(node.getContext(), node.getSourceSection(), Visibility.PUBLIC, "class_eval", node);
                }
            });
        }

        @Specialization(guards = "isRubyProc(block)")
        public Object classEval(VirtualFrame frame, RubyModule self, NotProvided code, NotProvided file, NotProvided line, RubyBasicObject block) {
            return yield.dispatchWithModifiedSelf(frame, block, self);
        }

        @Specialization
        public Object classEval(RubyModule self, NotProvided code, NotProvided file, NotProvided line, NotProvided block) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError(0, 1, 2, this));
        }

        @Specialization(guards = {"wasProvided(code)", "isRubyProc(block)"})
        public Object classEval(RubyModule self, Object code, NotProvided file, NotProvided line, RubyBasicObject block) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError(1, 0, this));
        }

    }

    @CoreMethod(names = { "class_exec", "module_exec" }, argumentsAsArray = true, needsBlock = true)
    public abstract static class ClassExecNode extends CoreMethodArrayArgumentsNode {

        @Child private YieldDispatchHeadNode yield;

        public ClassExecNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            yield = new YieldDispatchHeadNode(context);
        }

        public abstract Object executeClassExec(VirtualFrame frame, RubyBasicObject self, Object[] args, RubyBasicObject block);

        @Specialization(guards = "isRubyProc(block)")
        public Object classExec(VirtualFrame frame, RubyModule self, Object[] args, RubyBasicObject block) {
            return yield.dispatchWithModifiedSelf(frame, block, self, args);
        }

        @Specialization
        public Object classExec(VirtualFrame frame, RubyModule self, Object[] args, NotProvided block) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().noBlockGiven(this));
        }

    }

    @CoreMethod(names = "class_variable_defined?", required = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name")
    })
    public abstract static class ClassVariableDefinedNode extends CoreMethodNode {

        public ClassVariableDefinedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(getContext(), getSourceSection(), name);
        }

        @TruffleBoundary
        @Specialization
        public boolean isClassVariableDefinedString(RubyModule module, String name) {
            RubyContext.checkClassVariableName(getContext(), name, this);

            final Object value = ModuleOperations.lookupClassVariable(module, name);

            return value != null;
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
            return NameToJavaStringNodeGen.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        @TruffleBoundary
        public Object getClassVariable(RubyModule module, String name) {
            RubyContext.checkClassVariableName(getContext(), name, this);

            final Object value = ModuleOperations.lookupClassVariable(module, name);

            if (value == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().nameErrorUninitializedClassVariable(module, name, this));
            } else {
                return value;
            }
        }

    }

    @CoreMethod(names = "class_variable_set", required = 2, raiseIfFrozenSelf = true)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name"),
            @NodeChild(type = RubyNode.class, value = "value")
    })
    public abstract static class ClassVariableSetNode extends CoreMethodNode {

        public ClassVariableSetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        @TruffleBoundary
        public Object setClassVariable(RubyModule module, String name, Object value) {
            RubyContext.checkClassVariableName(getContext(), name, this);

            ModuleOperations.setClassVariable(module, name, value, this);

            return value;
        }

    }

    @CoreMethod(names = "class_variables")
    public abstract static class ClassVariablesNode extends CoreMethodArrayArgumentsNode {

        public ClassVariablesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject getClassVariables(RubyModule module) {
            CompilerDirectives.transferToInterpreter();

            final RubyBasicObject array = ArrayNodes.createEmptyArray(module.getContext().getCoreLibrary().getArrayClass());

            for (String variable : ModuleOperations.getAllClassVariables(module).keySet()) {
                ArrayNodes.slowPush(array, getSymbol(variable));
            }
            return array;
        }
    }

    @CoreMethod(names = "constants", optional = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "inherit")
    })
    public abstract static class ConstantsNode extends CoreMethodNode {

        public ConstantsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("inherit")
        public RubyNode coerceToBoolean(RubyNode inherit) {
            return BooleanCastWithDefaultNodeGen.create(getContext(), getSourceSection(), true, inherit);
        }

        @Specialization
        public RubyBasicObject constants(RubyModule module, boolean inherit) {
            CompilerDirectives.transferToInterpreter();

            final List<RubyBasicObject> constantsArray = new ArrayList<>();

            final Map<String, RubyConstant> constants;
            if (inherit) {
                constants = ModuleOperations.getAllConstants(module);
            } else {
                constants = getModel(module).getConstants();
            }

            for (Entry<String, RubyConstant> constant : constants.entrySet()) {
                if (!constant.getValue().isPrivate()) {
                    constantsArray.add(getSymbol(constant.getKey()));
                }
            }

            return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(), constantsArray.toArray(new Object[constantsArray.size()]));
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
            return NameToJavaStringNodeGen.create(getContext(), getSourceSection(), name);
        }

        @CreateCast("inherit")
        public RubyNode coerceToBoolean(RubyNode inherit) {
            return BooleanCastWithDefaultNodeGen.create(getContext(), getSourceSection(), true, inherit);
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

        @Child private GetConstantNode getConstantNode;

        public ConstGetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.getConstantNode = GetConstantNodeGen.create(context, sourceSection, null, null,
                    LookupConstantNodeGen.create(context, sourceSection, LexicalScope.NONE, null, null));
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToSymbolOrStringNodeGen.create(getContext(), getSourceSection(), name);
        }

        @CreateCast("inherit")
        public RubyNode coerceToBoolean(RubyNode inherit) {
            return BooleanCastWithDefaultNodeGen.create(getContext(), getSourceSection(), true, inherit);
        }

        // Symbol
        @Specialization(guards = {"inherit", "isRubySymbol(name)"})
        public Object getConstant(VirtualFrame frame, RubyModule module, RubyBasicObject name, boolean inherit) {
            return getConstantNode.executeGetConstant(frame, module, SymbolNodes.getString(name));
        }

        @Specialization(guards = {"!inherit", "isRubySymbol(name)"})
        public Object getConstantNoInherit(RubyModule module, RubyBasicObject name, boolean inherit) {
            return getConstantNoInherit(module, SymbolNodes.getString(name), this);
        }

        // String
        @Specialization(guards = { "inherit", "isRubyString(name)", "!isScoped(name)" })
        public Object getConstantString(VirtualFrame frame, RubyModule module, RubyBasicObject name, boolean inherit) {
            return getConstantNode.executeGetConstant(frame, module, name.toString());
        }

        @Specialization(guards = { "!inherit", "isRubyString(name)", "!isScoped(name)" })
        public Object getConstantNoInheritString(RubyModule module, RubyBasicObject name, boolean inherit) {
            return getConstantNoInherit(module, name.toString(), this);
        }

        // Scoped String
        @Specialization(guards = {"isRubyString(fullName)", "isScoped(fullName)"})
        public Object getConstantScoped(RubyModule module, RubyBasicObject fullName, boolean inherit) {
            return getConstantScoped(module, fullName.toString(), inherit);
        }

        @TruffleBoundary
        private Object getConstantNoInherit(RubyModule module, String name, Node currentNode) {
            final RubyConstant constant = ModuleOperations.lookupConstantWithInherit(getContext(), module, name, false, currentNode);

            if (constant == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().nameErrorUninitializedConstant(module, name, this));
            } else {
                return constant.getValue();
            }
        }

        @TruffleBoundary
        private Object getConstantScoped(RubyModule module, String fullName, boolean inherit) {
            RubyConstant constant = ModuleOperations.lookupScopedConstant(getContext(), module, fullName, inherit, this);
            if (constant == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().nameErrorUninitializedConstant(module, fullName, this));
            } else {
                return constant.getValue();
            }
        }

        @TruffleBoundary
        boolean isScoped(RubyBasicObject name) {
            assert RubyGuards.isRubyString(name);
            // TODO (eregon, 27 May 2015): Any way to make this efficient?
            return name.toString().contains("::");
        }

    }

    @CoreMethod(names = "const_missing", required = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name")
    })
    public abstract static class ConstMissingNode extends CoreMethodNode {

        public ConstMissingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        public Object methodMissing(RubyModule module, String name) {
            throw new RaiseException(getContext().getCoreLibrary().nameErrorUninitializedConstant(module, name, this));
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
            return NameToJavaStringNodeGen.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        public Object setConstant(RubyModule module, String name, Object value) {
            CompilerDirectives.transferToInterpreter();

            if (!IdUtil.isValidConstantName19(name)) {
                throw new RaiseException(getContext().getCoreLibrary().nameError(String.format("wrong constant name %s", name), name, this));
            }

            getModel(module).setConstant(this, name, value);
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
            return NameToJavaStringNodeGen.create(getContext(), getSourceSection(), name);
        }

        @TruffleBoundary
        @Specialization
        public RubyBasicObject defineMethod(RubyModule module, String name, NotProvided proc, NotProvided block) {
            throw new RaiseException(getContext().getCoreLibrary().argumentError("needs either proc or block", this));
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyProc(block)")
        public RubyBasicObject defineMethodBlock(RubyModule module, String name, NotProvided proc, RubyBasicObject block) {
            return defineMethodProc(module, name, block, NotProvided.INSTANCE);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyProc(proc)")
        public RubyBasicObject defineMethodProc(RubyModule module, String name, RubyBasicObject proc, NotProvided block) {
            return defineMethod(module, name, proc);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyMethod(method)")
        public RubyBasicObject defineMethodMethod(RubyModule module, String name, RubyBasicObject method, NotProvided block) {
            getModel(module).addMethod(this, MethodNodes.getMethod(method).withName(name));
            return getSymbol(name);
        }

        @Specialization(guards = "isRubyUnboundMethod(method)")
        public RubyBasicObject defineMethod(VirtualFrame frame, RubyModule module, String name, RubyBasicObject method, NotProvided block) {
            CompilerDirectives.transferToInterpreter();

            final RubyBasicObject origin = UnboundMethodNodes.getOrigin(method);
            if (!ModuleOperations.canBindMethodTo(origin, module)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().typeError("bind argument must be a subclass of " + getModel(origin).getName(), this));
            }

            // TODO CS 5-Apr-15 TypeError if the method came from a singleton

            return addMethod(module, name, UnboundMethodNodes.getMethod(method));
        }

        private RubyBasicObject defineMethod(RubyModule module, String name, RubyBasicObject proc) {
            CompilerDirectives.transferToInterpreter();

            assert RubyGuards.isRubyProc(proc);

            final CallTarget modifiedCallTarget = ProcNodes.getCallTargetForLambdas(proc);
            final SharedMethodInfo info = ProcNodes.getSharedMethodInfo(proc).withName(name);
            final InternalMethod modifiedMethod = new InternalMethod(info, name, module, Visibility.PUBLIC, false, modifiedCallTarget, ProcNodes.getDeclarationFrame(proc));

            return addMethod(module, name, modifiedMethod);
        }

        private RubyBasicObject addMethod(RubyModule module, String name, InternalMethod method) {
            method = method.withName(name);

            if (ModuleOperations.isMethodPrivateFromName(name)) {
                method = method.withVisibility(Visibility.PRIVATE);
            }

            getModel(module).addMethod(this, method);
            return getSymbol(name);
        }

    }

    @CoreMethod(names = "extend_object", required = 1, visibility = Visibility.PRIVATE)
    public abstract static class ExtendObjectNode extends CoreMethodArrayArgumentsNode {

        @Child private SingletonClassNode singletonClassNode;

        public ExtendObjectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.singletonClassNode = SingletonClassNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public RubyBasicObject extendObject(VirtualFrame frame, RubyModule module, RubyBasicObject object) {
            if (RubyGuards.isRubyClass(module)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().typeErrorWrongArgumentType(module, "Module", this));
            }

            getModel(singletonClassNode.executeSingletonClass(frame, object)).include(this, module);
            return module;
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        @Child private ModuleNodes.ClassExecNode classExecNode;

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract RubyBasicObject executeInitialize(VirtualFrame frame, RubyBasicObject module, RubyBasicObject block);

        void classEval(VirtualFrame frame, RubyBasicObject module, RubyBasicObject block) {
            assert RubyGuards.isRubyModule(module);
            assert RubyGuards.isRubyProc(block);

            if (classExecNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                classExecNode = insert(ModuleNodesFactory.ClassExecNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{null,null,null}));
            }
            classExecNode.executeClassExec(frame, module, new Object[]{}, block);
        }

        @Specialization
        public RubyBasicObject initialize(RubyModule module, NotProvided block) {
            return module;
        }

        @Specialization(guards = "isRubyProc(block)")
        public RubyBasicObject initialize(VirtualFrame frame, RubyModule module, RubyBasicObject block) {
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

            getModel(self).initCopy(from);
            return nil();
        }

        @Specialization(guards = {"isRubyClass(self)", "isRubyClass(from)"})
        public Object initializeCopy(RubyBasicObject self, RubyBasicObject from) {
            CompilerDirectives.transferToInterpreter();

            if (from == getContext().getCoreLibrary().getBasicObjectClass()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().typeError("can't copy the root class", this));
            } else if (getModel(from).isSingleton()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().typeError("can't copy singleton class", this));
            }

            getModel(self).initCopy(from);
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
        public RubyBasicObject includedModules(RubyModule module) {
            CompilerDirectives.transferToInterpreter();

            final List<RubyBasicObject> modules = new ArrayList<>();

            for (RubyBasicObject included : getModel(module).ancestors()) {
                if (getModel(included).isOnlyAModule() && included != module) {
                    modules.add(included);
                }
            }

            return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(), modules.toArray(new Object[modules.size()]));
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
            return NameToJavaStringNodeGen.create(getContext(), getSourceSection(), name);
        }

        @CreateCast("inherit")
        public RubyNode coerceToBoolean(RubyNode inherit) {
            return BooleanCastWithDefaultNodeGen.create(getContext(), getSourceSection(), true, inherit);
        }

        @Specialization
        public boolean isMethodDefined(RubyModule module, String name, boolean inherit) {
            CompilerDirectives.transferToInterpreter();

            final InternalMethod method;
            if (inherit) {
                method = ModuleOperations.lookupMethod(module, name);
            } else {
                method = getModel(module).getMethods().get(name);
            }

            return method != null && !method.getVisibility().isPrivate();
        }

    }

    @CoreMethod(names = "module_function", argumentsAsArray = true, visibility = Visibility.PRIVATE)
    public abstract static class ModuleFunctionNode extends CoreMethodArrayArgumentsNode {

        @Child SetVisibilityNode setVisibilityNode;

        public ModuleFunctionNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            setVisibilityNode = SetVisibilityNodeGen.create(context, sourceSection, Visibility.MODULE_FUNCTION, null, null);
        }

        @Specialization
        public RubyBasicObject moduleFunction(VirtualFrame frame, RubyModule module, Object[] names) {
            if (RubyGuards.isRubyClass(module) && !getContext().getCoreLibrary().isLoadingRubyCore()) {
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

            if (!getModel(module).hasPartialName()) {
                return nil();
            }

            return createString(getModel(module).getName());
        }
    }

    @CoreMethod(names = "nesting", onSingleton = true)
    public abstract static class NestingNode extends CoreMethodArrayArgumentsNode {

        public NestingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject nesting() {
            CompilerDirectives.transferToInterpreter();

            final List<RubyBasicObject> modules = new ArrayList<>();

            InternalMethod method = RubyCallStack.getCallingMethod(getContext());
            LexicalScope lexicalScope = method == null ? null : method.getSharedMethodInfo().getLexicalScope();
            RubyBasicObject object = getContext().getCoreLibrary().getObjectClass();

            while (lexicalScope != null) {
                final RubyBasicObject enclosing = lexicalScope.getLiveModule();
                if (enclosing == object)
                    break;
                modules.add(enclosing);
                lexicalScope = lexicalScope.getParent();
            }

            return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(), modules.toArray(new Object[modules.size()]));
        }
    }

    @CoreMethod(names = "public", argumentsAsArray = true, visibility = Visibility.PRIVATE)
    public abstract static class PublicNode extends CoreMethodArrayArgumentsNode {

        @Child SetVisibilityNode setVisibilityNode;

        public PublicNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            setVisibilityNode = SetVisibilityNodeGen.create(context, sourceSection, Visibility.PUBLIC, null, null);
        }

        public abstract RubyBasicObject executePublic(VirtualFrame frame, RubyBasicObject module, Object[] args);

        @Specialization
        public RubyBasicObject doPublic(VirtualFrame frame, RubyModule module, Object[] names) {
            return setVisibilityNode.executeSetVisibility(frame, module, names);
        }

    }

    @CoreMethod(names = "public_class_method", argumentsAsArray = true)
    public abstract static class PublicClassMethodNode extends CoreMethodArrayArgumentsNode {

        @Child SingletonClassNode singletonClassNode;
        @Child SetMethodVisibilityNode setMethodVisibilityNode;

        public PublicClassMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.singletonClassNode = SingletonClassNodeGen.create(context, sourceSection, null);
            this.setMethodVisibilityNode = SetMethodVisibilityNodeGen.create(context, sourceSection, Visibility.PUBLIC, null, null);
        }

        @Specialization
        public RubyBasicObject publicClassMethod(VirtualFrame frame, RubyModule module, Object[] names) {
            final RubyBasicObject singletonClass = singletonClassNode.executeSingletonClass(frame, module);

            for (Object name : names) {
                setMethodVisibilityNode.executeSetMethodVisibility(frame, singletonClass, name);
            }

            return module;
        }
    }

    @CoreMethod(names = "private", argumentsAsArray = true, visibility = Visibility.PRIVATE)
    public abstract static class PrivateNode extends CoreMethodArrayArgumentsNode {

        @Child SetVisibilityNode setVisibilityNode;

        public PrivateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            setVisibilityNode = SetVisibilityNodeGen.create(context, sourceSection, Visibility.PRIVATE, null, null);
        }

        public abstract RubyBasicObject executePrivate(VirtualFrame frame, RubyBasicObject module, Object[] args);

        @Specialization
        public RubyBasicObject doPrivate(VirtualFrame frame, RubyModule module, Object[] names) {
            return setVisibilityNode.executeSetVisibility(frame, module, names);
        }

    }

    @CoreMethod(names = "prepend_features", required = 1, visibility = Visibility.PRIVATE)
    public abstract static class PrependFeaturesNode extends CoreMethodArrayArgumentsNode {

        @Child TaintResultNode taintResultNode;

        public PrependFeaturesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            taintResultNode = new TaintResultNode(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject prependFeatures(RubyModule features, RubyModule target) {
            if (RubyGuards.isRubyClass(features)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().typeError("prepend_features must be called only on modules", this));
            }
            getModel(target).prepend(this, features);
            taintResultNode.maybeTaint(features, target);
            return nil();
        }
    }

    @CoreMethod(names = "private_class_method", argumentsAsArray = true)
    public abstract static class PrivateClassMethodNode extends CoreMethodArrayArgumentsNode {

        @Child SingletonClassNode singletonClassNode;
        @Child SetMethodVisibilityNode setMethodVisibilityNode;

        public PrivateClassMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.singletonClassNode = SingletonClassNodeGen.create(context, sourceSection, null);
            this.setMethodVisibilityNode = SetMethodVisibilityNodeGen.create(context, sourceSection, Visibility.PRIVATE, null, null);
        }

        @Specialization
        public RubyBasicObject privateClassMethod(VirtualFrame frame, RubyModule module, Object[] names) {
            final RubyBasicObject singletonClass = singletonClassNode.executeSingletonClass(frame, module);

            for (Object name : names) {
                setMethodVisibilityNode.executeSetMethodVisibility(frame, singletonClass, name);
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
            return NameToJavaStringNodeGen.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        public boolean isPrivateMethodDefined(RubyModule module, String name) {
            CompilerDirectives.transferToInterpreter();

            InternalMethod method = ModuleOperations.lookupMethod(module, name);
            return method != null && method.getVisibility().isPrivate();
        }

    }

    @CoreMethod(names = "protected_instance_methods", optional = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "includeAncestors")
    })
    public abstract static class ProtectedInstanceMethodsNode extends CoreMethodNode {

        public ProtectedInstanceMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("includeAncestors")
        public RubyNode coerceToBoolean(RubyNode includeAncestors) {
            return BooleanCastWithDefaultNodeGen.create(getContext(), getSourceSection(), true, includeAncestors);
        }

        @Specialization
        public RubyBasicObject protectedInstanceMethods(RubyModule module, boolean includeAncestors) {
            CompilerDirectives.transferToInterpreter();
            return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(),
                    getModel(module).filterMethods(includeAncestors, MethodFilter.PROTECTED).toArray());
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
            return NameToJavaStringNodeGen.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        public boolean isProtectedMethodDefined(RubyModule module, String name) {
            CompilerDirectives.transferToInterpreter();

            InternalMethod method = ModuleOperations.lookupMethod(module, name);
            return method != null && method.getVisibility().isProtected();
        }

    }

    @CoreMethod(names = "private_instance_methods", optional = 1)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "module"),
        @NodeChild(type = RubyNode.class, value = "includeAncestors")
    })
    public abstract static class PrivateInstanceMethodsNode extends CoreMethodNode {

        public PrivateInstanceMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("includeAncestors")
        public RubyNode coerceToBoolean(RubyNode includeAncestors) {
            return BooleanCastWithDefaultNodeGen.create(getContext(), getSourceSection(), true, includeAncestors);
        }

        @Specialization
        public RubyBasicObject privateInstanceMethods(RubyModule module, boolean includeAncestors) {
            CompilerDirectives.transferToInterpreter();

            return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(),
                    getModel(module).filterMethods(includeAncestors, MethodFilter.PRIVATE).toArray());
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
            return NameToJavaStringNodeGen.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        public RubyBasicObject publicInstanceMethod(RubyModule module, String name) {
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

            return UnboundMethodNodes.createUnboundMethod(getContext().getCoreLibrary().getUnboundMethodClass(), module, method);
        }

    }

    @CoreMethod(names = "public_instance_methods", optional = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "includeAncestors")
    })
    public abstract static class PublicInstanceMethodsNode extends CoreMethodNode {

        public PublicInstanceMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("includeAncestors")
        public RubyNode coerceToBoolean(RubyNode includeAncestors) {
            return BooleanCastWithDefaultNodeGen.create(getContext(), getSourceSection(), true, includeAncestors);
        }

        @Specialization
        public RubyBasicObject publicInstanceMethods(RubyModule module, boolean includeAncestors) {
            CompilerDirectives.transferToInterpreter();

            return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(),
                    getModel(module).filterMethods(includeAncestors, MethodFilter.PUBLIC).toArray());
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
            return NameToJavaStringNodeGen.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        public boolean isPublicMethodDefined(RubyModule module, String name) {
            CompilerDirectives.transferToInterpreter();

            InternalMethod method = ModuleOperations.lookupMethod(module, name);
            return method != null && method.getVisibility() == Visibility.PUBLIC;
        }

    }

    @CoreMethod(names = "instance_methods", optional = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "includeAncestors")
    })
    public abstract static class InstanceMethodsNode extends CoreMethodNode {

        public InstanceMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("includeAncestors")
        public RubyNode coerceToBoolean(RubyNode includeAncestors) {
            return BooleanCastWithDefaultNodeGen.create(getContext(), getSourceSection(), true, includeAncestors);
        }

        @Specialization
        public RubyBasicObject instanceMethods(RubyModule module, boolean includeAncestors) {
            CompilerDirectives.transferToInterpreter();

            return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(),
                    getModel(module).filterMethods(includeAncestors, MethodFilter.PUBLIC_PROTECTED).toArray());
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
            return NameToJavaStringNodeGen.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        public RubyBasicObject instanceMethod(RubyModule module, String name) {
            CompilerDirectives.transferToInterpreter();

            // TODO(CS, 11-Jan-15) cache this lookup
            final InternalMethod method = ModuleOperations.lookupMethod(module, name);

            if (method == null || method.isUndefined()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().nameErrorUndefinedMethod(name, module, this));
            }

            return UnboundMethodNodes.createUnboundMethod(getContext().getCoreLibrary().getUnboundMethodClass(), module, method);
        }

    }

    @CoreMethod(names = "private_constant", argumentsAsArray = true)
    public abstract static class PrivateConstantNode extends CoreMethodArrayArgumentsNode {

        public PrivateConstantNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject privateConstant(RubyModule module, Object[] args) {
            CompilerDirectives.transferToInterpreter();

            for (Object name : args) {
                if (RubyGuards.isRubySymbol(name)) {
                    getModel(module).changeConstantVisibility(this, SymbolNodes.getString((RubyBasicObject) name), true);
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
        public RubyBasicObject publicConstant(RubyModule module, Object[] args) {
            CompilerDirectives.transferToInterpreter();

            for (Object name : args) {
                if (RubyGuards.isRubySymbol(name)) {
                    getModel(module).changeConstantVisibility(this, SymbolNodes.getString((RubyBasicObject) name), false);
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
            setVisibilityNode = SetVisibilityNodeGen.create(context, sourceSection, Visibility.PROTECTED, null, null);
        }

        @Specialization
        public RubyBasicObject doProtected(VirtualFrame frame, RubyModule module, Object[] names) {
            return setVisibilityNode.executeSetVisibility(frame, module, names);
        }

    }

    @CoreMethod(names = "remove_class_variable", required = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name")
    })
    public abstract static class RemoveClassVariableNode extends CoreMethodNode {

        public RemoveClassVariableNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(getContext(), getSourceSection(), name);
        }
        
        @TruffleBoundary
        @Specialization
        public Object removeClassVariableString(RubyModule module, String name) {
            RubyContext.checkClassVariableName(getContext(), name, this);
            return getModel(module).removeClassVariable(this, name);
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
            return NameToJavaStringNodeGen.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        Object removeConstant(RubyModule module, String name) {
            RubyConstant oldConstant = getModel(module).removeConstant(this, name);
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

        @Child NameToJavaStringNode nameToJavaStringNode;
        @Child RaiseIfFrozenNode raiseIfFrozenNode;
        @Child CallDispatchHeadNode methodRemovedNode;

        public RemoveMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.nameToJavaStringNode = NameToJavaStringNodeGen.create(context, sourceSection, null);
            this.raiseIfFrozenNode = new RaiseIfFrozenNode(new SelfNode(context, sourceSection));
            this.methodRemovedNode = DispatchHeadNodeFactory.createMethodCallOnSelf(context);
        }

        @Specialization
        public RubyBasicObject removeMethods(VirtualFrame frame, RubyModule module, Object[] names) {
            for (Object name : names) {
                removeMethod(frame, module, nameToJavaStringNode.executeToJavaString(frame, name));
            }
            return module;
        }

        private void removeMethod(VirtualFrame frame, RubyModule module, String name) {
            raiseIfFrozenNode.execute(frame);

            CompilerDirectives.transferToInterpreter();
            if (getModel(module).getMethods().containsKey(name)) {
                getModel(module).removeMethod(name);
                methodRemovedNode.call(frame, module, "method_removed", null, getSymbol(name));
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().nameErrorMethodNotDefinedIn(module, name, this));
            }
        }

    }

    @CoreMethod(names = { "to_s", "inspect" })
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject toS(RubyModule module) {
            CompilerDirectives.transferToInterpreter();

            return createString(getModel(module).getName());
        }

    }

    @CoreMethod(names = "undef_method", argumentsAsArray = true, visibility = Visibility.PRIVATE)
    public abstract static class UndefMethodNode extends CoreMethodArrayArgumentsNode {

        @Child NameToJavaStringNode nameToJavaStringNode;
        @Child RaiseIfFrozenNode raiseIfFrozenNode;
        @Child CallDispatchHeadNode methodUndefinedNode;

        public UndefMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.nameToJavaStringNode = NameToJavaStringNodeGen.create(context, sourceSection, null);
            this.raiseIfFrozenNode = new RaiseIfFrozenNode(new SelfNode(context, sourceSection));
            this.methodUndefinedNode = DispatchHeadNodeFactory.createMethodCallOnSelf(context);
        }

        @Specialization
        public RubyBasicObject undefMethods(VirtualFrame frame, RubyModule module, Object[] names) {
            for (Object name : names) {
                undefMethod(frame, module, nameToJavaStringNode.executeToJavaString(frame, name));
            }
            return module;
        }

        private void undefMethod(VirtualFrame frame, RubyModule module, String name) {
            raiseIfFrozenNode.execute(frame);

            final InternalMethod method = ModuleOperations.lookupMethod(module, name);

            if (method != null) {
                getModel(module).undefMethod(this, method);
                methodUndefinedNode.call(frame, module, "method_undefined", null, getSymbol(name));
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().noMethodErrorOnModule(name, module, this));
            }
        }

    }

    @NodeChildren({ @NodeChild(value = "module"), @NodeChild(value = "names") })
    public abstract static class SetVisibilityNode extends RubyNode {

        private final Visibility visibility;

        @Child SetMethodVisibilityNode setMethodVisibilityNode;

        public SetVisibilityNode(RubyContext context, SourceSection sourceSection, Visibility visibility) {
            super(context, sourceSection);
            this.visibility = visibility;
            this.setMethodVisibilityNode = SetMethodVisibilityNodeGen.create(context, sourceSection, visibility, null, null);
        }

        public abstract RubyBasicObject executeSetVisibility(VirtualFrame frame, RubyModule module, Object[] arguments);

        @Specialization
        public RubyBasicObject setVisibility(VirtualFrame frame, RubyModule module, Object[] names) {
            CompilerDirectives.transferToInterpreter();

            if (names.length == 0) {
                setCurrentVisibility(visibility);
            } else {
                for (Object name : names) {
                    setMethodVisibilityNode.executeSetMethodVisibility(frame, module, name);
                }
            }

            return module;
        }

        private void setCurrentVisibility(Visibility visibility) {
            CompilerDirectives.transferToInterpreter();

            final Frame callerFrame = RubyCallStack.getCallerFrame(getContext()).getFrame(FrameInstance.FrameAccess.READ_WRITE, true);
            assert callerFrame != null;
            assert callerFrame.getFrameDescriptor() != null;

            final FrameSlot visibilitySlot = callerFrame.getFrameDescriptor().findOrAddFrameSlot(
                    VISIBILITY_FRAME_SLOT_ID, "visibility for frame", FrameSlotKind.Object);

            callerFrame.setObject(visibilitySlot, visibility);
        }

    }

    @NodeChildren({ @NodeChild(value = "module"), @NodeChild(value = "name") })
    public abstract static class SetMethodVisibilityNode extends RubyNode {

        private final Visibility visibility;

        @Child SingletonClassNode singletonClassNode;
        @Child NameToJavaStringNode nameToJavaStringNode;

        public SetMethodVisibilityNode(RubyContext context, SourceSection sourceSection, Visibility visibility) {
            super(context, sourceSection);
            this.visibility = visibility;
            this.nameToJavaStringNode = NameToJavaStringNodeGen.create(context, sourceSection, null);
            this.singletonClassNode = SingletonClassNodeGen.create(context, sourceSection, null);
        }

        public abstract RubyBasicObject executeSetMethodVisibility(VirtualFrame frame, RubyBasicObject module, Object name);

        @Specialization
        public RubyBasicObject setMethodVisibility(VirtualFrame frame, RubyModule module, Object name) {
            final String methodName = nameToJavaStringNode.executeToJavaString(frame, name);

            final InternalMethod method = getModel(module).deepMethodSearch(methodName);

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
                getModel(module).addMethod(this, method.withVisibility(Visibility.PRIVATE));
                getModel(singletonClassNode.executeSingletonClass(frame, module)).addMethod(this, method.withVisibility(Visibility.PUBLIC));
            } else {
                getModel(module).addMethod(this, method.withVisibility(visibility));
            }

            return module;
        }

    }

    public static class ModuleAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyBasicObject rubyClass, Node currentNode) {
            return new RubyModule(context, rubyClass, null, null, currentNode);
        }

    }
}
