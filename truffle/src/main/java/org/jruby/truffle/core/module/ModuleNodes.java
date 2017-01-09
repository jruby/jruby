/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.module;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.CoreMethodNode;
import org.jruby.truffle.core.RaiseIfFrozenNode;
import org.jruby.truffle.core.cast.BooleanCastWithDefaultNodeGen;
import org.jruby.truffle.core.cast.NameToJavaStringNode;
import org.jruby.truffle.core.cast.NameToJavaStringNodeGen;
import org.jruby.truffle.core.cast.NameToSymbolOrStringNodeGen;
import org.jruby.truffle.core.cast.TaintResultNode;
import org.jruby.truffle.core.cast.ToPathNodeGen;
import org.jruby.truffle.core.cast.ToStrNode;
import org.jruby.truffle.core.cast.ToStrNodeGen;
import org.jruby.truffle.core.method.MethodFilter;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.core.string.StringUtils;
import org.jruby.truffle.core.symbol.SymbolTable;
import org.jruby.truffle.language.LexicalScope;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyConstant;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.SnippetNode;
import org.jruby.truffle.language.SourceIndexLength;
import org.jruby.truffle.language.Visibility;
import org.jruby.truffle.language.arguments.MissingArgumentBehavior;
import org.jruby.truffle.language.arguments.ProfileArgumentNode;
import org.jruby.truffle.language.arguments.ReadPreArgumentNode;
import org.jruby.truffle.language.arguments.ReadSelfNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.constants.GetConstantNode;
import org.jruby.truffle.language.constants.LookupConstantNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.loader.CodeLoader;
import org.jruby.truffle.language.loader.RequireNode;
import org.jruby.truffle.language.methods.AddMethodNode;
import org.jruby.truffle.language.methods.AddMethodNodeGen;
import org.jruby.truffle.language.methods.Arity;
import org.jruby.truffle.language.methods.CanBindMethodToModuleNode;
import org.jruby.truffle.language.methods.CanBindMethodToModuleNodeGen;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.methods.GetCurrentVisibilityNode;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.methods.SharedMethodInfo;
import org.jruby.truffle.language.objects.IsANode;
import org.jruby.truffle.language.objects.IsANodeGen;
import org.jruby.truffle.language.objects.IsFrozenNode;
import org.jruby.truffle.language.objects.IsFrozenNodeGen;
import org.jruby.truffle.language.objects.ReadInstanceVariableNode;
import org.jruby.truffle.language.objects.SingletonClassNode;
import org.jruby.truffle.language.objects.SingletonClassNodeGen;
import org.jruby.truffle.language.objects.WriteInstanceVariableNode;
import org.jruby.truffle.language.yield.YieldNode;
import org.jruby.truffle.parser.Identifiers;
import org.jruby.truffle.parser.ParserContext;
import org.jruby.truffle.parser.Translator;
import org.jruby.truffle.platform.UnsafeGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@CoreClass("Module")
public abstract class ModuleNodes {

    @TruffleBoundary
    public static DynamicObject createModule(RubyContext context, SourceSection sourceSection, DynamicObject selfClass, DynamicObject lexicalParent, String name, Node currentNode) {
        final ModuleFields model = new ModuleFields(context, sourceSection, lexicalParent, name);
        final DynamicObject module = Layouts.MODULE.createModule(Layouts.CLASS.getInstanceFactory(selfClass), model);
        model.rubyModuleObject = module;

        if (lexicalParent != null) {
            Layouts.MODULE.getFields(module).getAdoptedByLexicalParent(context, lexicalParent, name, currentNode);
        } else if (Layouts.MODULE.getFields(module).givenBaseName != null) { // bootstrap module
            Layouts.MODULE.getFields(module).setFullName(Layouts.MODULE.getFields(module).givenBaseName);
        }
        return module;
    }

    @CoreMethod(names = "===", required = 1)
    public abstract static class ContainsInstanceNode extends CoreMethodArrayArgumentsNode {

        @Child private IsANode isANode = IsANodeGen.create(null, null);

        @Specialization
        public boolean containsInstance(DynamicObject module, Object instance) {
            return isANode.executeIsA(instance, module);
        }

    }

    @CoreMethod(names = "<", required = 1)
    public abstract static class IsSubclassOfNode extends CoreMethodArrayArgumentsNode {

        public abstract Object executeIsSubclassOf(VirtualFrame frame, DynamicObject self, DynamicObject other);

        @TruffleBoundary
        @Specialization(guards = "isRubyModule(other)")
        public Object isSubclassOf(DynamicObject self, DynamicObject other) {
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

        @Specialization(guards = "!isRubyModule(other)")
        public Object isSubclassOfOther(DynamicObject self, DynamicObject other) {
            throw new RaiseException(coreExceptions().typeError("compared with non class/module", this));
        }

    }

    @CoreMethod(names = "<=", required = 1)
    public abstract static class IsSubclassOfOrEqualToNode extends CoreMethodArrayArgumentsNode {

        public abstract Object executeIsSubclassOfOrEqualTo(DynamicObject self, DynamicObject other);

        @TruffleBoundary
        @Specialization(guards = "isRubyModule(other)")
        public Object isSubclassOfOrEqualTo(DynamicObject self, DynamicObject other) {
            if (self == other || ModuleOperations.includesModule(self, other)) {
                return true;
            }

            if (ModuleOperations.includesModule(other, self)) {
                return false;
            }

            return nil();
        }

        @Specialization(guards = "!isRubyModule(other)")
        public Object isSubclassOfOrEqualToOther(DynamicObject self, DynamicObject other) {
            throw new RaiseException(coreExceptions().typeError("compared with non class/module", this));
        }

    }

    @CoreMethod(names = ">", required = 1)
    public abstract static class IsSuperclassOfNode extends CoreMethodArrayArgumentsNode {

        public abstract Object executeIsSuperclassOf(VirtualFrame frame, DynamicObject self, DynamicObject other);

        @TruffleBoundary
        @Specialization(guards = "isRubyModule(other)")
        public Object isSuperclassOf(DynamicObject self, DynamicObject other) {
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

        @Specialization(guards = "!isRubyModule(other)")
        public Object isSuperclassOfOther(DynamicObject self, DynamicObject other) {
            throw new RaiseException(coreExceptions().typeError("compared with non class/module", this));
        }

    }

    @CoreMethod(names = ">=", required = 1)
    public abstract static class IsSuperclassOfOrEqualToNode extends CoreMethodArrayArgumentsNode {

        public abstract Object executeIsSuperclassOfOrEqualTo(VirtualFrame frame, DynamicObject self, DynamicObject other);

        @TruffleBoundary
        @Specialization(guards = "isRubyModule(other)")
        public Object isSuperclassOfOrEqualTo(DynamicObject self, DynamicObject other) {
            if (self == other || ModuleOperations.includesModule(other, self)) {
                return true;
            }

            if (ModuleOperations.includesModule(self, other)) {
                return false;
            }

            return nil();
        }

        @Specialization(guards = "!isRubyModule(other)")
        public Object isSuperclassOfOrEqualToOther(DynamicObject self, DynamicObject other) {
            throw new RaiseException(coreExceptions().typeError("compared with non class/module", this));
        }

    }

    @CoreMethod(names = "<=>", required = 1)
    public abstract static class CompareNode extends CoreMethodArrayArgumentsNode {

        @Child private IsSubclassOfOrEqualToNode subclassNode;

        private Object isSubclass(DynamicObject self, DynamicObject other) {
            if (subclassNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                subclassNode = insert(ModuleNodesFactory.IsSubclassOfOrEqualToNodeFactory.create(null));
            }
            return subclassNode.executeIsSubclassOfOrEqualTo(self, other);
        }

        @Specialization(guards = "isRubyModule(other)")
        public Object compare(DynamicObject self, DynamicObject other) {
            if (self == other) {
                return 0;
            }

            final Object isSubclass = isSubclass(self, other);

            if (isSubclass == nil()) {
                return nil();
            } else {
                return (boolean) isSubclass ? -1 : 1;
            }
        }

        @Specialization(guards = "!isRubyModule(other)")
        public Object compareOther(DynamicObject self, DynamicObject other) {
            return nil();
        }

    }

    @CoreMethod(names = "alias_method", required = 2, raiseIfFrozenSelf = true, visibility = Visibility.PRIVATE)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "newName"),
            @NodeChild(type = RubyNode.class, value = "oldName")
    })
    public abstract static class AliasMethodNode extends CoreMethodNode {

        @CreateCast("newName")
        public RubyNode coercetNewNameToString(RubyNode newName) {
            return NameToJavaStringNodeGen.create(newName);
        }

        @CreateCast("oldName")
        public RubyNode coerceOldNameToString(RubyNode oldName) {
            return NameToJavaStringNodeGen.create(oldName);
        }

        @Specialization
        public DynamicObject aliasMethod(DynamicObject module, String newName, String oldName) {
            Layouts.MODULE.getFields(module).alias(getContext(), this, newName, oldName);
            return module;
        }

    }

    @CoreMethod(names = "ancestors")
    public abstract static class AncestorsNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject ancestors(DynamicObject self) {
            final List<DynamicObject> ancestors = new ArrayList<>();
            for (DynamicObject module : Layouts.MODULE.getFields(self).ancestors()) {
                ancestors.add(module);
            }

            Object[] objects = ancestors.toArray(new Object[ancestors.size()]);
            return createArray(objects, objects.length);
        }
    }

    @CoreMethod(names = "append_features", required = 1, visibility = Visibility.PRIVATE)
    public abstract static class AppendFeaturesNode extends CoreMethodArrayArgumentsNode {

        @Child private TaintResultNode taintResultNode = new TaintResultNode();

        @Specialization(guards = "isRubyModule(target)")
        public DynamicObject appendFeatures(DynamicObject features, DynamicObject target,
                @Cached("create()") BranchProfile errorProfile) {
            if (RubyGuards.isRubyClass(features)) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().typeError("append_features must be called only on modules", this));
            }
            Layouts.MODULE.getFields(target).include(getContext(), this, features);
            taintResultNode.maybeTaint(features, target);
            return nil();
        }
    }

    @NodeChildren({ @NodeChild("module"), @NodeChild("name") })
    public abstract static class GenerateAccessorNode extends RubyNode {

        final boolean isGetter;
        @Child private NameToJavaStringNode nameToJavaStringNode = NameToJavaStringNode.create();

        public GenerateAccessorNode(boolean isGetter) {
            this.isGetter = isGetter;
        }

        public abstract DynamicObject executeGenerateAccessor(VirtualFrame frame, DynamicObject module, Object name);

        @Specialization
        public DynamicObject generateAccessor(VirtualFrame frame, DynamicObject module, Object nameObject) {
            final String name = nameToJavaStringNode.executeToJavaString(frame, nameObject);
            createAccesor(module, name);
            return nil();
        }

        @TruffleBoundary
        private void createAccesor(DynamicObject module, String name) {
            final FrameInstance callerFrame = getContext().getCallStack().getCallerFrameIgnoringSend();
            final SourceSection sourceSection = callerFrame.getCallNode().getEncapsulatingSourceSection();
            final SourceIndexLength sourceIndexLength = new SourceIndexLength(sourceSection.getCharIndex(), sourceSection.getCharLength());
            final Visibility visibility = DeclarationContext.findVisibility(callerFrame.getFrame(FrameAccess.READ_ONLY, true));
            final Arity arity = isGetter ? Arity.NO_ARGUMENTS : Arity.ONE_REQUIRED;
            final String ivar = "@" + name;
            final String accessorName = isGetter ? name : name + "=";

            final RubyNode checkArity = Translator.createCheckArityNode(arity);

            final LexicalScope lexicalScope = new LexicalScope(getContext().getRootLexicalScope(), module);
            final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(
                    sourceSection,
                    lexicalScope,
                    arity,
                    module,
                    accessorName,
                    "attr_" + (isGetter ? "reader" : "writer"),
                    null,
                    false,
                    false,
                    false);

            final RubyNode self = new ProfileArgumentNode(new ReadSelfNode());
            final RubyNode accessInstanceVariable;
            if (isGetter) {
                accessInstanceVariable = new ReadInstanceVariableNode(ivar, self);
            } else {
                RubyNode readArgument = new ProfileArgumentNode(new ReadPreArgumentNode(0, MissingArgumentBehavior.RUNTIME_ERROR));
                accessInstanceVariable = new WriteInstanceVariableNode(ivar, self, readArgument);
            }
            final RubyNode sequence = Translator.sequence(sourceIndexLength, Arrays.asList(checkArity, accessInstanceVariable));
            final RubyRootNode rootNode = new RubyRootNode(getContext(), sourceSection, null, sharedMethodInfo, sequence, false);
            final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
            final InternalMethod method = new InternalMethod(getContext(), sharedMethodInfo, lexicalScope, accessorName, module, visibility, callTarget);

            Layouts.MODULE.getFields(module).addMethod(getContext(), this, method);
        }
    }

    @CoreMethod(names = "attr", rest = true, visibility = Visibility.PRIVATE)
    public abstract static class AttrNode extends CoreMethodArrayArgumentsNode {

        @Child private GenerateAccessorNode generateGetterNode = ModuleNodesFactory.GenerateAccessorNodeGen.create(true, null, null);
        @Child private GenerateAccessorNode generateSetterNode = ModuleNodesFactory.GenerateAccessorNodeGen.create(false, null, null);

        @Specialization
        public DynamicObject attr(VirtualFrame frame, DynamicObject module, Object[] names) {
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

    @CoreMethod(names = "attr_accessor", rest = true, visibility = Visibility.PRIVATE)
    public abstract static class AttrAccessorNode extends CoreMethodArrayArgumentsNode {

        @Child private GenerateAccessorNode generateGetterNode = ModuleNodesFactory.GenerateAccessorNodeGen.create(true, null, null);
        @Child private GenerateAccessorNode generateSetterNode = ModuleNodesFactory.GenerateAccessorNodeGen.create(false, null, null);

        @Specialization
        public DynamicObject attrAccessor(VirtualFrame frame, DynamicObject module, Object[] names) {
            for (Object name : names) {
                generateGetterNode.executeGenerateAccessor(frame, module, name);
                generateSetterNode.executeGenerateAccessor(frame, module, name);
            }
            return nil();
        }

    }

    @CoreMethod(names = "attr_reader", rest = true, visibility = Visibility.PRIVATE)
    public abstract static class AttrReaderNode extends CoreMethodArrayArgumentsNode {

        @Child private GenerateAccessorNode generateGetterNode = ModuleNodesFactory.GenerateAccessorNodeGen.create(true, null, null);

        @Specialization
        public DynamicObject attrReader(VirtualFrame frame, DynamicObject module, Object[] names) {
            for (Object name : names) {
                generateGetterNode.executeGenerateAccessor(frame, module, name);
            }
            return nil();
        }

    }

    @CoreMethod(names = "attr_writer", rest = true, visibility = Visibility.PRIVATE)
    public abstract static class AttrWriterNode extends CoreMethodArrayArgumentsNode {

        @Child private GenerateAccessorNode generateSetterNode = ModuleNodesFactory.GenerateAccessorNodeGen.create(false, null, null);

        @Specialization
        public DynamicObject attrWriter(VirtualFrame frame, DynamicObject module, Object[] names) {
            for (Object name : names) {
                generateSetterNode.executeGenerateAccessor(frame, module, name);
            }
            return nil();
        }

    }

    @CoreMethod(names = "autoload", required = 2, unsafe = UnsafeGroup.LOAD)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name"),
            @NodeChild(type = RubyNode.class, value = "filename")
    })
    public abstract static class AutoloadNode extends CoreMethodNode {

        @CreateCast("name") public RubyNode coerceNameToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(name);
        }

        @CreateCast("filename") public RubyNode coerceFilenameToPath(RubyNode filename) {
            return ToPathNodeGen.create(filename);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(filename)")
        public DynamicObject autoload(DynamicObject module, String name, DynamicObject filename) {
            if (!Identifiers.isValidConstantName19(name)) {
                throw new RaiseException(coreExceptions().nameError(StringUtils.format("autoload must be constant name: %s", name), module, name, this));
            }

            if (StringOperations.rope(filename).isEmpty()) {
                throw new RaiseException(coreExceptions().argumentError("empty file name", this));
            }

            if (Layouts.MODULE.getFields(module).getConstant(name) != null) {
                return nil();
            }

            Layouts.MODULE.getFields(module).setAutoloadConstant(getContext(), this, name, filename);

            return nil();
        }
    }

    @CoreMethod(names = "autoload?", required = 1)
    public abstract static class AutoloadQueryNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubySymbol(name)")
        public Object autoloadQuerySymbol(DynamicObject module, DynamicObject name) {
            return autoloadQuery(module, Layouts.SYMBOL.getString(name));
        }

        @Specialization(guards = "isRubyString(name)")
        public Object autoloadQueryString(DynamicObject module, DynamicObject name) {
            return autoloadQuery(module, name.toString());
        }

        private Object autoloadQuery(DynamicObject module, String name) {
            final RubyConstant constant = ModuleOperations.lookupConstant(getContext(), module, name);

            if ((constant == null) || ! constant.isAutoload()) {
                return nil();
            }

            return constant.getValue();
        }
    }

    @CoreMethod(names = { "class_eval", "module_eval" }, optional = 3, lowerFixnum = 3, needsBlock = true)
    public abstract static class ClassEvalNode extends CoreMethodArrayArgumentsNode {

        @Child private YieldNode yield = new YieldNode(DeclarationContext.CLASS_EVAL);
        @Child private ToStrNode toStrNode;

        protected DynamicObject toStr(VirtualFrame frame, Object object) {
            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStrNode = insert(ToStrNodeGen.create(null));
            }
            return toStrNode.executeToStr(frame, object);
        }

        @Specialization(guards = "isRubyString(code)")
        public Object classEval(VirtualFrame frame, DynamicObject module, DynamicObject code, NotProvided file, NotProvided line, NotProvided block, @Cached("create()") IndirectCallNode callNode) {
            return classEvalSource(frame, module, code, "(eval)", callNode);
        }

        @Specialization(guards = {"isRubyString(code)", "isRubyString(file)"})
        public Object classEval(VirtualFrame frame, DynamicObject module, DynamicObject code, DynamicObject file, NotProvided line, NotProvided block, @Cached("create()") IndirectCallNode callNode) {
            return classEvalSource(frame, module, code, file.toString(), callNode);
        }

        @Specialization(guards = {"isRubyString(code)", "isRubyString(file)"})
        public Object classEval(VirtualFrame frame, DynamicObject module, DynamicObject code, DynamicObject file, int line, NotProvided block, @Cached("create()") IndirectCallNode callNode) {
            final CodeLoader.DeferredCall deferredCall = classEvalSource(module, code, file.toString(), line);
            return deferredCall.call(frame, callNode);
        }

        @Specialization(guards = "wasProvided(code)")
        public Object classEval(VirtualFrame frame, DynamicObject module, Object code, NotProvided file, NotProvided line, NotProvided block, @Cached("create()") IndirectCallNode callNode) {
            return classEvalSource(frame, module, toStr(frame, code), file.toString(), callNode);
        }

        @Specialization(guards = {"isRubyString(code)", "wasProvided(file)"})
        public Object classEval(VirtualFrame frame, DynamicObject module, DynamicObject code, Object file, NotProvided line, NotProvided block, @Cached("create()") IndirectCallNode callNode) {
            return classEvalSource(frame, module, code, toStr(frame, file).toString(), callNode);
        }

        private Object classEvalSource(VirtualFrame frame, DynamicObject module, DynamicObject code, String file, @Cached("create()") IndirectCallNode callNode) {
            final CodeLoader.DeferredCall deferredCall = classEvalSource(module, code, file, 1);
            return deferredCall.call(frame, callNode);
        }

        @TruffleBoundary
        private CodeLoader.DeferredCall classEvalSource(DynamicObject module, DynamicObject rubySource, String file, int line) {
            assert RubyGuards.isRubyString(rubySource);
            final Rope code = StringOperations.rope(rubySource);

            final MaterializedFrame callerFrame = getContext().getCallStack().getCallerFrameIgnoringSend()
                    .getFrame(FrameInstance.FrameAccess.MATERIALIZE, true).materialize();
            Encoding encoding = Layouts.STRING.getRope(rubySource).getEncoding();

            // TODO (pitr 15-Oct-2015): fix this ugly hack, required for AS, copy-paste
            final String s = new String(new char[Math.max(line - 1, 0)]);
            final String space = StringUtils.replace(s, "\0", "\n");
            Source source = getContext().getSourceLoader().loadFragment(space + code, file);

            final RubyRootNode rootNode = getContext().getCodeLoader().parse(source, encoding, ParserContext.MODULE, callerFrame, true, this);
            return getContext().getCodeLoader().prepareExecute(ParserContext.MODULE, DeclarationContext.CLASS_EVAL, rootNode, callerFrame, module);
        }

        @Specialization
        public Object classEval(VirtualFrame frame, DynamicObject self, NotProvided code, NotProvided file, NotProvided line, DynamicObject block) {
            return yield.dispatchWithModifiedSelf(frame, block, self, self);
        }

        @Specialization
        public Object classEval(DynamicObject self, NotProvided code, NotProvided file, NotProvided line, NotProvided block) {
            throw new RaiseException(coreExceptions().argumentError(0, 1, 2, this));
        }

        @Specialization(guards = "wasProvided(code)")
        public Object classEval(DynamicObject self, Object code, NotProvided file, NotProvided line, DynamicObject block) {
            throw new RaiseException(coreExceptions().argumentError(1, 0, this));
        }

    }

    @CoreMethod(names = { "class_exec", "module_exec" }, rest = true, needsBlock = true)
    public abstract static class ClassExecNode extends CoreMethodArrayArgumentsNode {

        @Child private YieldNode yield = new YieldNode(DeclarationContext.CLASS_EVAL);

        public abstract Object executeClassExec(VirtualFrame frame, DynamicObject self, Object[] args, Object block);

        @Specialization
        public Object classExec(VirtualFrame frame, DynamicObject self, Object[] args, DynamicObject block) {
            return yield.dispatchWithModifiedSelf(frame, block, self, args);
        }

        @Specialization
        public Object classExec(DynamicObject self, Object[] args, NotProvided block) {
            throw new RaiseException(coreExceptions().noBlockGiven(this));
        }

    }

    @CoreMethod(names = "class_variable_defined?", required = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name")
    })
    public abstract static class ClassVariableDefinedNode extends CoreMethodNode {

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(name);
        }

        @TruffleBoundary(throwsControlFlowException = true)
        @Specialization
        public boolean isClassVariableDefinedString(DynamicObject module, String name) {
            SymbolTable.checkClassVariableName(getContext(), name,module, this);

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

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(name);
        }

        @Specialization
        @TruffleBoundary(throwsControlFlowException = true)
        public Object getClassVariable(DynamicObject module, String name) {
            SymbolTable.checkClassVariableName(getContext(), name,module, this);

            final Object value = ModuleOperations.lookupClassVariable(module, name);

            if (value == null) {
                throw new RaiseException(coreExceptions().nameErrorUninitializedClassVariable(module, name, this));
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

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(name);
        }

        @Specialization
        @TruffleBoundary(throwsControlFlowException = true)
        public Object setClassVariable(DynamicObject module, String name, Object value) {
            SymbolTable.checkClassVariableName(getContext(), name,module, this);

            ModuleOperations.setClassVariable(getContext(), module, name, value, this);

            return value;
        }

    }

    @CoreMethod(names = "class_variables")
    public abstract static class ClassVariablesNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject getClassVariables(DynamicObject module) {
            final Map<String, Object> allClassVariables = ModuleOperations.getAllClassVariables(module);
            final int size = allClassVariables.size();
            final Object[] store = new Object[size];

            int i = 0;
            for (String variable : allClassVariables.keySet()) {
                store[i++] = getSymbol(variable);
            }
            return createArray(store, size);
        }
    }

    @CoreMethod(names = "constants", optional = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "inherit")
    })
    public abstract static class ConstantsNode extends CoreMethodNode {

        @CreateCast("inherit")
        public RubyNode coerceToBoolean(RubyNode inherit) {
            return BooleanCastWithDefaultNodeGen.create(true, inherit);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject constants(DynamicObject module, boolean inherit) {
            final List<DynamicObject> constantsArray = new ArrayList<>();

            final Iterable<Entry<String, RubyConstant>> constants;
            if (inherit) {
                constants = ModuleOperations.getAllConstants(module);
            } else {
                constants = Layouts.MODULE.getFields(module).getConstants();
            }

            for (Entry<String, RubyConstant> constant : constants) {
                if (!constant.getValue().isPrivate()) {
                    constantsArray.add(getSymbol(constant.getKey()));
                }
            }

            Object[] objects = constantsArray.toArray(new Object[constantsArray.size()]);
            return createArray(objects, objects.length);
        }

    }

    @CoreMethod(names = "const_defined?", required = 1, optional = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name"),
            @NodeChild(type = RubyNode.class, value = "inherit")
    })
    public abstract static class ConstDefinedNode extends CoreMethodNode {

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(name);
        }

        @CreateCast("inherit")
        public RubyNode coerceToBoolean(RubyNode inherit) {
            return BooleanCastWithDefaultNodeGen.create(true, inherit);
        }

        @TruffleBoundary
        @Specialization
        public boolean isConstDefined(DynamicObject module, String fullName, boolean inherit) {
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

        @Child private RequireNode requireNode;
        @Child private LookupConstantNode lookupConstantNode = LookupConstantNode.create(true, true);
        @Child private GetConstantNode getConstantNode = GetConstantNode.create();

        @CreateCast("name")
        public RubyNode coerceToSymbolOrString(RubyNode name) {
            return NameToSymbolOrStringNodeGen.create(name);
        }

        @CreateCast("inherit")
        public RubyNode coerceToBoolean(RubyNode inherit) {
            return BooleanCastWithDefaultNodeGen.create(true, inherit);
        }

        // Symbol
        @Specialization(guards = { "inherit", "isRubySymbol(name)" })
        public Object getConstant(VirtualFrame frame, DynamicObject module, DynamicObject name, boolean inherit) {
            return getConstant(frame, module, Layouts.SYMBOL.getString(name));
        }

        @Specialization(guards = { "!inherit", "isRubySymbol(name)" })
        public Object getConstantNoInherit(VirtualFrame frame, DynamicObject module, DynamicObject name, boolean inherit) {
            return getConstantNoInherit(frame, module, Layouts.SYMBOL.getString(name), this);
        }

        // String
        @Specialization(guards = { "inherit", "isRubyString(name)", "!isScoped(name)" })
        public Object getConstantString(VirtualFrame frame, DynamicObject module, DynamicObject name, boolean inherit) {
            return getConstant(frame, module, name.toString());
        }

        @Specialization(guards = { "!inherit", "isRubyString(name)", "!isScoped(name)" })
        public Object getConstantNoInheritString(VirtualFrame frame, DynamicObject module, DynamicObject name, boolean inherit) {
            return getConstantNoInherit(frame, module, name.toString(), this);
        }

        // Scoped String
        @Specialization(guards = {"isRubyString(fullName)", "isScoped(fullName)"})
        public Object getConstantScoped(DynamicObject module, DynamicObject fullName, boolean inherit) {
            return getConstantScoped(module, fullName.toString(), inherit);
        }

        private Object getConstant(VirtualFrame frame, Object module, String name) {
            final RubyConstant constant = lookupConstantNode.lookupConstant(frame, module, name);
            return getConstantNode.executeGetConstant(frame, module, name, constant, lookupConstantNode);
        }

        private Object getConstantNoInherit(VirtualFrame frame, DynamicObject module, String name, Node currentNode) {
            RubyConstant constant = ModuleOperations.lookupConstantWithInherit(getContext(), module, name, false, currentNode);
            if (constant == null) {
                // Call const_missing
                return getConstantNode.executeGetConstant(frame, module, name, null, lookupConstantNode);
            } else {
                if (constant.isAutoload()) {
                    loadAutoloadedConstant(frame, constant);
                    constant = ModuleOperations.lookupConstantWithInherit(getContext(), module, name, false, currentNode);
                }

                return constant.getValue();
            }
        }

        @TruffleBoundary
        private Object getConstantScoped(DynamicObject module, String fullName, boolean inherit) {
            RubyConstant constant = ModuleOperations.lookupScopedConstant(getContext(), module, fullName, inherit, this);
            if (constant == null) {
                throw new RaiseException(coreExceptions().nameErrorUninitializedConstant(module, fullName, this));
            } else {
                return constant.getValue();
            }
        }

        @TruffleBoundary
        boolean isScoped(DynamicObject name) {
            assert RubyGuards.isRubyString(name);
            // TODO (eregon, 27 May 2015): Any way to make this efficient?
            return name.toString().contains("::");
        }

        private void loadAutoloadedConstant(VirtualFrame frame, RubyConstant constant) {
            if (requireNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                requireNode = insert(RequireNode.create());
            }

            final String feature = StringOperations.getString((DynamicObject) constant.getValue());
            requireNode.executeRequire(frame, feature);
        }

    }

    @CoreMethod(names = "const_missing", required = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name")
    })
    public abstract static class ConstMissingNode extends CoreMethodNode {

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(name);
        }

        @Specialization
        public Object constMissing(DynamicObject module, String name) {
            throw new RaiseException(coreExceptions().nameErrorUninitializedConstant(module, name, this));
        }

    }

    @CoreMethod(names = "const_set", required = 2)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name"),
            @NodeChild(type = RubyNode.class, value = "value")
    })
    public abstract static class ConstSetNode extends CoreMethodNode {

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(name);
        }

        @TruffleBoundary
        @Specialization
        public Object setConstant(DynamicObject module, String name, Object value) {
            if (!Identifiers.isValidConstantName19(name)) {
                throw new RaiseException(coreExceptions().nameError(StringUtils.format("wrong constant name %s", name), module, name, this));
            }

            Layouts.MODULE.getFields(module).setConstant(getContext(), this, name, value);
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

        @Child private AddMethodNode addMethodNode = AddMethodNodeGen.create(false, false, null, null, null);

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(name);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject defineMethod(DynamicObject module, String name, NotProvided proc, NotProvided block) {
            throw new RaiseException(coreExceptions().argumentError("needs either proc or block", this));
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject defineMethodBlock(DynamicObject module, String name, NotProvided proc, DynamicObject block) {
            return defineMethodProc(module, name, block, NotProvided.INSTANCE);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyProc(proc)")
        public DynamicObject defineMethodProc(DynamicObject module, String name, DynamicObject proc, NotProvided block) {
            return defineMethod(module, name, proc);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyMethod(methodObject)")
        public DynamicObject defineMethodMethod(DynamicObject module, String name, DynamicObject methodObject, NotProvided block,
                @Cached("createCanBindMethodToModuleNode()") CanBindMethodToModuleNode canBindMethodToModuleNode) {
            final InternalMethod method = Layouts.METHOD.getMethod(methodObject);

            if (!canBindMethodToModuleNode.executeCanBindMethodToModule(method, module)) {
                final DynamicObject declaringModule = method.getDeclaringModule();
                if (RubyGuards.isSingletonClass(declaringModule)) {
                    throw new RaiseException(coreExceptions().typeError(
                            "can't bind singleton method to a different class", this));
                } else {
                    throw new RaiseException(coreExceptions().typeError(
                            "class must be a subclass of " + Layouts.MODULE.getFields(declaringModule).getName(), this));
                }
            }

            Layouts.MODULE.getFields(module).addMethod(getContext(), this, method.withName(name));
            return getSymbol(name);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyUnboundMethod(method)")
        public DynamicObject defineMethod(DynamicObject module, String name, DynamicObject method, NotProvided block) {
            final DynamicObject origin = Layouts.UNBOUND_METHOD.getOrigin(method);
            if (!ModuleOperations.canBindMethodTo(origin, module)) {
                throw new RaiseException(coreExceptions().typeError("bind argument must be a subclass of " + Layouts.MODULE.getFields(origin).getName(), this));
            }

            // TODO CS 5-Apr-15 TypeError if the method came from a singleton

            return addMethod(module, name, Layouts.UNBOUND_METHOD.getMethod(method));
        }

        @TruffleBoundary
        private DynamicObject defineMethod(DynamicObject module, String name, DynamicObject proc) {
            final RootCallTarget callTarget = (RootCallTarget) Layouts.PROC.getCallTargetForLambdas(proc);
            final RubyRootNode rootNode = (RubyRootNode) callTarget.getRootNode();
            final SharedMethodInfo info = Layouts.PROC.getSharedMethodInfo(proc).withName(name);

            final RubyNode body = NodeUtil.cloneNode(rootNode.getBody());
            final RubyNode newBody = new CallMethodWithProcBody(Layouts.PROC.getDeclarationFrame(proc), body);
            final RubyRootNode newRootNode = new RubyRootNode(getContext(), info.getSourceSection(), rootNode.getFrameDescriptor(), info, newBody, false);
            final CallTarget newCallTarget = Truffle.getRuntime().createCallTarget(newRootNode);

            final InternalMethod method = InternalMethod.fromProc(getContext(), info, name, module, Visibility.PUBLIC, proc, newCallTarget);
            return addMethod(module, name, method);
        }

        private static class CallMethodWithProcBody extends RubyNode {

            private final MaterializedFrame declarationFrame;
            @Child private RubyNode procBody;

            public CallMethodWithProcBody(MaterializedFrame declarationFrame, RubyNode procBody) {
                this.declarationFrame = declarationFrame;
                this.procBody = procBody;
            }

            @Override
            public Object execute(VirtualFrame frame) {
                RubyArguments.setDeclarationFrame(frame, declarationFrame);
                return procBody.execute(frame);
            }

        }

        @TruffleBoundary
        private DynamicObject addMethod(DynamicObject module, String name, InternalMethod method) {
            method = method.withName(name);

            final Frame frame = getContext().getCallStack().getCallerFrameIgnoringSend().getFrame(FrameAccess.READ_ONLY, true);
            final Visibility visibility = GetCurrentVisibilityNode.getVisibilityFromNameAndFrame(name, frame);
            return addMethodNode.executeAddMethod(module, method, visibility);
        }

        protected CanBindMethodToModuleNode createCanBindMethodToModuleNode() {
            return CanBindMethodToModuleNodeGen.create(null, null);
        }

    }

    @CoreMethod(names = "extend_object", required = 1, visibility = Visibility.PRIVATE)
    public abstract static class ExtendObjectNode extends CoreMethodArrayArgumentsNode {

        @Child private SingletonClassNode singletonClassNode = SingletonClassNodeGen.create(null);

        @Specialization
        public DynamicObject extendObject(DynamicObject module, DynamicObject object,
                @Cached("create()") BranchProfile errorProfile) {
            if (RubyGuards.isRubyClass(module)) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().typeErrorWrongArgumentType(module, "Module", this));
            }

            Layouts.MODULE.getFields(singletonClassNode.executeSingletonClass(object)).include(getContext(), this, module);
            return module;
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        @Child private ModuleNodes.ClassExecNode classExecNode;

        public abstract DynamicObject executeInitialize(VirtualFrame frame, DynamicObject module, Object block);

        void classEval(VirtualFrame frame, DynamicObject module, DynamicObject block) {
            if (classExecNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                classExecNode = insert(ModuleNodesFactory.ClassExecNodeFactory.create(null));
            }
            classExecNode.executeClassExec(frame, module, new Object[]{module}, block);
        }

        @Specialization
        public DynamicObject initialize(DynamicObject module, NotProvided block) {
            return module;
        }

        @Specialization
        public DynamicObject initialize(VirtualFrame frame, DynamicObject module, DynamicObject block) {
            classEval(frame, module, block);
            return module;
        }

    }

    @CoreMethod(names = "initialize_copy", required = 1)
    public abstract static class InitializeCopyNode extends CoreMethodArrayArgumentsNode {

        @Child private SingletonClassNode singletonClassNode;

        @Specialization(guards = { "!isRubyClass(self)", "isRubyModule(from)", "!isRubyClass(from)" })
        public Object initializeCopyModule(DynamicObject self, DynamicObject from) {
            Layouts.MODULE.getFields(self).initCopy(from);
            return nil();
        }

        @Specialization(guards = {"isRubyClass(self)", "isRubyClass(from)"})
        public Object initializeCopyClass(DynamicObject self, DynamicObject from,
                @Cached("create()") BranchProfile errorProfile) {
            if (from == coreLibrary().getBasicObjectClass()) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().typeError("can't copy the root class", this));
            } else if (Layouts.CLASS.getIsSingleton(from)) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().typeError("can't copy singleton class", this));
            }

            Layouts.MODULE.getFields(self).initCopy(from);

            final DynamicObject selfMetaClass = getSingletonClass(self);
            final DynamicObject fromMetaClass = Layouts.BASIC_OBJECT.getMetaClass(from);

            assert Layouts.CLASS.getIsSingleton(fromMetaClass);
            assert Layouts.CLASS.getIsSingleton(Layouts.BASIC_OBJECT.getMetaClass(self));

            Layouts.MODULE.getFields(selfMetaClass).initCopy(fromMetaClass); // copy class methods

            return nil();
        }

        protected DynamicObject getSingletonClass(DynamicObject object) {
            if (singletonClassNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                singletonClassNode = insert(SingletonClassNodeGen.create(null));
            }

            return singletonClassNode.executeSingletonClass(object);
        }

    }

    @CoreMethod(names = "included", needsSelf = false, required = 1, visibility = Visibility.PRIVATE)
    public abstract static class IncludedNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject included(Object subclass) {
            return nil();
        }

    }

    @CoreMethod(names = "included_modules")
    public abstract static class IncludedModulesNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject includedModules(DynamicObject module) {
            final List<DynamicObject> modules = new ArrayList<>();

            for (DynamicObject included : Layouts.MODULE.getFields(module).ancestors()) {
                if (!RubyGuards.isRubyClass(included) && included != module) {
                    modules.add(included);
                }
            }

            Object[] objects = modules.toArray(new Object[modules.size()]);
            return createArray(objects, objects.length);
        }
    }

    @CoreMethod(names = "method_defined?", required = 1, optional = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name"),
            @NodeChild(type = RubyNode.class, value = "inherit") })
    public abstract static class MethodDefinedNode extends CoreMethodNode {

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(name);
        }

        @CreateCast("inherit")
        public RubyNode coerceToBoolean(RubyNode inherit) {
            return BooleanCastWithDefaultNodeGen.create(true, inherit);
        }

        @TruffleBoundary
        @Specialization
        public boolean isMethodDefined(DynamicObject module, String name, boolean inherit) {
            final InternalMethod method;
            if (inherit) {
                method = ModuleOperations.lookupMethod(module, name);
            } else {
                method = Layouts.MODULE.getFields(module).getMethod(name);
            }

            return method != null && !method.getVisibility().isPrivate() && !method.isUndefined();
        }

    }

    @CoreMethod(names = "module_function", rest = true, visibility = Visibility.PRIVATE)
    public abstract static class ModuleFunctionNode extends CoreMethodArrayArgumentsNode {

        @Child private SetVisibilityNode setVisibilityNode = ModuleNodesFactory.SetVisibilityNodeGen.create(Visibility.MODULE_FUNCTION, null, null);

        @Specialization
        public DynamicObject moduleFunction(VirtualFrame frame, DynamicObject module, Object[] names,
                @Cached("create()") BranchProfile errorProfile) {
            if (RubyGuards.isRubyClass(module) && !coreLibrary().isLoadingRubyCore()) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().typeError("module_function must be called for modules", this));
            }

            return setVisibilityNode.executeSetVisibility(frame, module, names);
        }

    }

    @CoreMethod(names = "name")
    public abstract static class NameNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public Object name(DynamicObject module,
                @Cached("createIdentityProfile()") ValueProfile fieldsProfile) {
            final ModuleFields fields = fieldsProfile.profile(Layouts.MODULE.getFields(module));

            if (!fields.hasPartialName()) {
                return nil();
            }

            return createString(StringOperations.encodeRope(fields.getName(), UTF8Encoding.INSTANCE));
        }
    }

    @CoreMethod(names = "nesting", onSingleton = true)
    public abstract static class NestingNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject nesting() {
            final List<DynamicObject> modules = new ArrayList<>();

            InternalMethod method = getContext().getCallStack().getCallingMethodIgnoringSend();
            LexicalScope lexicalScope = method == null ? null : method.getSharedMethodInfo().getLexicalScope();
            DynamicObject object = coreLibrary().getObjectClass();

            while (lexicalScope != null) {
                final DynamicObject enclosing = lexicalScope.getLiveModule();
                if (enclosing == object)
                    break;
                modules.add(enclosing);
                lexicalScope = lexicalScope.getParent();
            }

            Object[] objects = modules.toArray(new Object[modules.size()]);
            return createArray(objects, objects.length);
        }
    }

    @CoreMethod(names = "public", rest = true, visibility = Visibility.PRIVATE)
    public abstract static class PublicNode extends CoreMethodArrayArgumentsNode {

        @Child private SetVisibilityNode setVisibilityNode = ModuleNodesFactory.SetVisibilityNodeGen.create(Visibility.PUBLIC, null, null);

        public abstract DynamicObject executePublic(VirtualFrame frame, DynamicObject module, Object[] args);

        @Specialization
        public DynamicObject doPublic(VirtualFrame frame, DynamicObject module, Object[] names) {
            return setVisibilityNode.executeSetVisibility(frame, module, names);
        }

    }

    @CoreMethod(names = "public_class_method", rest = true)
    public abstract static class PublicClassMethodNode extends CoreMethodArrayArgumentsNode {

        @Child private SingletonClassNode singletonClassNode = SingletonClassNodeGen.create(null);
        @Child private SetMethodVisibilityNode setMethodVisibilityNode = ModuleNodesFactory.SetMethodVisibilityNodeGen.create(Visibility.PUBLIC, null, null);

        @Specialization
        public DynamicObject publicClassMethod(VirtualFrame frame, DynamicObject module, Object[] names) {
            final DynamicObject singletonClass = singletonClassNode.executeSingletonClass(module);

            for (Object name : names) {
                setMethodVisibilityNode.executeSetMethodVisibility(frame, singletonClass, name);
            }

            return module;
        }
    }

    @CoreMethod(names = "private", rest = true, visibility = Visibility.PRIVATE)
    public abstract static class PrivateNode extends CoreMethodArrayArgumentsNode {

        @Child private SetVisibilityNode setVisibilityNode = ModuleNodesFactory.SetVisibilityNodeGen.create(Visibility.PRIVATE, null, null);

        public abstract DynamicObject executePrivate(VirtualFrame frame, DynamicObject module, Object[] args);

        @Specialization
        public DynamicObject doPrivate(VirtualFrame frame, DynamicObject module, Object[] names) {
            return setVisibilityNode.executeSetVisibility(frame, module, names);
        }

    }

    @CoreMethod(names = "prepend_features", required = 1, visibility = Visibility.PRIVATE)
    public abstract static class PrependFeaturesNode extends CoreMethodArrayArgumentsNode {

        @Child private TaintResultNode taintResultNode = new TaintResultNode();

        @Specialization(guards = "isRubyModule(target)")
        public DynamicObject prependFeatures(DynamicObject features, DynamicObject target,
                @Cached("create()") BranchProfile errorProfile) {
            if (RubyGuards.isRubyClass(features)) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().typeError("prepend_features must be called only on modules", this));
            }
            Layouts.MODULE.getFields(target).prepend(getContext(), this, features);
            taintResultNode.maybeTaint(features, target);
            return nil();
        }
    }

    @CoreMethod(names = "private_class_method", rest = true)
    public abstract static class PrivateClassMethodNode extends CoreMethodArrayArgumentsNode {

        @Child private SingletonClassNode singletonClassNode = SingletonClassNodeGen.create(null);
        @Child private SetMethodVisibilityNode setMethodVisibilityNode = ModuleNodesFactory.SetMethodVisibilityNodeGen.create(Visibility.PRIVATE, null, null);

        @Specialization
        public DynamicObject privateClassMethod(VirtualFrame frame, DynamicObject module, Object[] names) {
            final DynamicObject singletonClass = singletonClassNode.executeSingletonClass(module);

            for (Object name : names) {
                setMethodVisibilityNode.executeSetMethodVisibility(frame, singletonClass, name);
            }

            return module;
        }
    }

    @CoreMethod(names = "public_instance_method", required = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name")
    })
    public abstract static class PublicInstanceMethodNode extends CoreMethodNode {

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(name);
        }

        @Specialization
        public DynamicObject publicInstanceMethod(DynamicObject module, String name,
                @Cached("create()") BranchProfile errorProfile) {
            // TODO(CS, 11-Jan-15) cache this lookup
            final InternalMethod method = ModuleOperations.lookupMethod(module, name);

            if (method == null || method.isUndefined()) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().nameErrorUndefinedMethod(name, module, this));
            } else if (method.getVisibility() != Visibility.PUBLIC) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().nameErrorPrivateMethod(name, module, this));
            }

            return Layouts.UNBOUND_METHOD.createUnboundMethod(coreLibrary().getUnboundMethodFactory(), module, method);
        }

    }

    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "includeAncestors")
    })
    protected abstract static class AbstractInstanceMethodsNode extends CoreMethodNode {

        final Visibility visibility;

        public AbstractInstanceMethodsNode(Visibility visibility) {
            this.visibility = visibility;
        }

        @CreateCast("includeAncestors")
        public RubyNode coerceToBoolean(RubyNode includeAncestors) {
            return BooleanCastWithDefaultNodeGen.create(true, includeAncestors);
        }

        @Specialization
        @TruffleBoundary
        public DynamicObject getInstanceMethods(DynamicObject module, boolean includeAncestors) {
            Object[] objects = Layouts.MODULE.getFields(module).filterMethods(getContext(), includeAncestors, MethodFilter.by(visibility)).toArray();
            return createArray(objects, objects.length);
        }

    }

    @CoreMethod(names = "public_instance_methods", optional = 1)
    public abstract static class PublicInstanceMethodsNode extends AbstractInstanceMethodsNode {

        public PublicInstanceMethodsNode() {
            super(Visibility.PUBLIC);
        }

    }

    @CoreMethod(names = "protected_instance_methods", optional = 1)
    public abstract static class ProtectedInstanceMethodsNode extends AbstractInstanceMethodsNode {

        public ProtectedInstanceMethodsNode() {
            super(Visibility.PROTECTED);
        }

    }
    @CoreMethod(names = "private_instance_methods", optional = 1)
    public abstract static class PrivateInstanceMethodsNode extends AbstractInstanceMethodsNode {

        public PrivateInstanceMethodsNode() {
            super(Visibility.PRIVATE);
        }

    }


    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name")
    })
    protected abstract static class AbstractMethodDefinedNode extends CoreMethodNode {

        final Visibility visibility;

        public AbstractMethodDefinedNode(Visibility visibility) {
            this.visibility = visibility;
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(name);
        }

        @Specialization
        public boolean isMethodDefined(DynamicObject module, String name) {
            // TODO (pitr-ch 30-Mar-2016): cache lookup
            return ModuleOperations.lookupMethod(module, name, visibility) != null;
        }

    }

    @CoreMethod(names = "public_method_defined?", required = 1)
    public abstract static class PublicMethodDefinedNode extends AbstractMethodDefinedNode {

        public PublicMethodDefinedNode(SourceIndexLength sourceSection) {
            super(Visibility.PUBLIC);
        }

    }

    @CoreMethod(names = "protected_method_defined?", required = 1)
    public abstract static class ProtectedMethodDefinedNode extends AbstractMethodDefinedNode {

        public ProtectedMethodDefinedNode(SourceIndexLength sourceSection) {
            super(Visibility.PROTECTED);
        }

    }

    @CoreMethod(names = "private_method_defined?", required = 1)
    public abstract static class PrivateMethodDefinedNode extends AbstractMethodDefinedNode {

        public PrivateMethodDefinedNode(SourceIndexLength sourceSection) {
            super(Visibility.PRIVATE);
        }

    }

    @CoreMethod(names = "instance_methods", optional = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "includeAncestors")
    })
    public abstract static class InstanceMethodsNode extends CoreMethodNode {

        @CreateCast("includeAncestors")
        public RubyNode coerceToBoolean(RubyNode includeAncestors) {
            return BooleanCastWithDefaultNodeGen.create(true, includeAncestors);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject instanceMethods(DynamicObject module, boolean includeAncestors) {
            Object[] objects = Layouts.MODULE.getFields(module).filterMethods(getContext(), includeAncestors, MethodFilter.PUBLIC_PROTECTED).toArray();
            return createArray(objects, objects.length);
        }
    }

    @CoreMethod(names = "instance_method", required = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name")
    })
    public abstract static class InstanceMethodNode extends CoreMethodNode {

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(name);
        }

        @Specialization
        public DynamicObject instanceMethod(DynamicObject module, String name,
                @Cached("create()") BranchProfile errorProfile) {
            // TODO(CS, 11-Jan-15) cache this lookup
            final InternalMethod method = ModuleOperations.lookupMethod(module, name);

            if (method == null || method.isUndefined()) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().nameErrorUndefinedMethod(name, module, this));
            }

            return Layouts.UNBOUND_METHOD.createUnboundMethod(coreLibrary().getUnboundMethodFactory(), module, method);
        }

    }

    @CoreMethod(names = "private_constant", rest = true)
    public abstract static class PrivateConstantNode extends CoreMethodArrayArgumentsNode {

        @Child private NameToJavaStringNode nameToJavaStringNode = NameToJavaStringNode.create();

        @Specialization
        public DynamicObject privateConstant(VirtualFrame frame, DynamicObject module, Object[] args) {
            for (Object arg : args) {
                String name = nameToJavaStringNode.executeToJavaString(frame, arg);
                Layouts.MODULE.getFields(module).changeConstantVisibility(getContext(), this, name, true);
            }
            return module;
        }
    }

    @CoreMethod(names = "deprecate_constant", rest = true, raiseIfFrozenSelf = true)
    public abstract static class DeprecateConstantNode extends CoreMethodArrayArgumentsNode {

        @Child private NameToJavaStringNode nameToJavaStringNode = NameToJavaStringNode.create();

        @Specialization
        public DynamicObject deprecateConstant(VirtualFrame frame, DynamicObject module, Object[] args) {
            for (Object arg : args) {
                String name = nameToJavaStringNode.executeToJavaString(frame, arg);
                Layouts.MODULE.getFields(module).deprecateConstant(getContext(), this, name);
            }
            return module;
        }
    }

    @CoreMethod(names = "public_constant", rest = true)
    public abstract static class PublicConstantNode extends CoreMethodArrayArgumentsNode {

        @Child private NameToJavaStringNode nameToJavaStringNode = NameToJavaStringNode.create();

        @Specialization
        public DynamicObject publicConstant(VirtualFrame frame, DynamicObject module, Object[] args) {
            for (Object arg : args) {
                String name = nameToJavaStringNode.executeToJavaString(frame, arg);
                Layouts.MODULE.getFields(module).changeConstantVisibility(getContext(), this, name, false);
            }
            return module;
        }
    }

    @CoreMethod(names = "protected", rest = true, visibility = Visibility.PRIVATE)
    public abstract static class ProtectedNode extends CoreMethodArrayArgumentsNode {

        @Child private SetVisibilityNode setVisibilityNode = ModuleNodesFactory.SetVisibilityNodeGen.create(Visibility.PROTECTED, null, null);

        @Specialization
        public DynamicObject doProtected(VirtualFrame frame, DynamicObject module, Object[] names) {
            return setVisibilityNode.executeSetVisibility(frame, module, names);
        }

    }

    @CoreMethod(names = "remove_class_variable", required = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name")
    })
    public abstract static class RemoveClassVariableNode extends CoreMethodNode {

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(name);
        }

        @TruffleBoundary(throwsControlFlowException = true)
        @Specialization
        public Object removeClassVariableString(DynamicObject module, String name) {
            SymbolTable.checkClassVariableName(getContext(), name,module, this);
            return ModuleOperations.removeClassVariable(Layouts.MODULE.getFields(module), getContext(), this, name);
        }

    }

    @CoreMethod(names = "remove_const", required = 1, visibility = Visibility.PRIVATE)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name")
    })
    public abstract static class RemoveConstNode extends CoreMethodNode {

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(name);
        }

        @TruffleBoundary
        @Specialization
        Object removeConstant(DynamicObject module, String name) {
            RubyConstant oldConstant = Layouts.MODULE.getFields(module).removeConstant(getContext(), this, name);
            if (oldConstant == null) {
                throw new RaiseException(coreExceptions().nameErrorConstantNotDefined(module, name, this));
            } else {
                if (oldConstant.isAutoload()) {
                    return nil();
                } else {
                    return oldConstant.getValue();
                }
            }
        }

    }

    @CoreMethod(names = "remove_method", rest = true, visibility = Visibility.PRIVATE)
    public abstract static class RemoveMethodNode extends CoreMethodArrayArgumentsNode {

        private final BranchProfile errorProfile = BranchProfile.create();

        @Child private NameToJavaStringNode nameToJavaStringNode = NameToJavaStringNode.create();
        @Child private IsFrozenNode isFrozenNode = IsFrozenNodeGen.create(null);
        @Child private CallDispatchHeadNode methodRemovedNode = DispatchHeadNodeFactory.createMethodCallOnSelf();

        @Specialization
        public DynamicObject removeMethods(VirtualFrame frame, DynamicObject module, Object[] names) {
            for (Object name : names) {
                removeMethod(frame, module, nameToJavaStringNode.executeToJavaString(frame, name));
            }
            return module;
        }

        private void removeMethod(VirtualFrame frame, DynamicObject module, String name) {
            isFrozenNode.raiseIfFrozen(module);

            if (Layouts.MODULE.getFields(module).getMethod(name) != null) {
                Layouts.MODULE.getFields(module).removeMethod(name);
                if (RubyGuards.isSingletonClass(module)) {
                    final DynamicObject receiver = Layouts.CLASS.getAttached(module);
                    methodRemovedNode.call(frame, receiver, "singleton_method_removed", getSymbol(name));
                } else {
                    methodRemovedNode.call(frame, module, "method_removed", getSymbol(name));
                }
            } else {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().nameErrorMethodNotDefinedIn(module, name, this));
            }
        }

    }

    @CoreMethod(names = { "to_s", "inspect" })
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        @Child private SnippetNode snippetNode;

        @Specialization
        public DynamicObject toS(VirtualFrame frame, DynamicObject module) {

            final String moduleName;
            if (RubyGuards.isSingletonClass(module)) {
                final DynamicObject attached = Layouts.CLASS.getAttached(module);
                final String name;
                if (Layouts.CLASS.isClass(attached) || Layouts.MODULE.isModule(attached)) {
                    if (snippetNode == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        snippetNode = insert(new SnippetNode());
                    }
                    DynamicObject inspectResult = (DynamicObject) snippetNode.execute(frame,
                        "Rubinius::Type.inspect(val)", "val", attached);
                    name = StringOperations.getString(inspectResult);
                } else {
                    name = Layouts.MODULE.getFields(module).getName();
                }
                moduleName = "#<Class:" + name + ">";
            } else {
                moduleName = Layouts.MODULE.getFields(module).getName();
            }

            // TODO BJF Aug 31, 2016 Add refinements to_s

            return createString(StringOperations.encodeRope(moduleName, UTF8Encoding.INSTANCE));
        }

    }

    @CoreMethod(names = "undef_method", rest = true, visibility = Visibility.PRIVATE)
    public abstract static class UndefMethodNode extends CoreMethodArrayArgumentsNode {

        @Child private NameToJavaStringNode nameToJavaStringNode = NameToJavaStringNode.create();
        @Child private RaiseIfFrozenNode raiseIfFrozenNode = new RaiseIfFrozenNode(new ProfileArgumentNode(new ReadSelfNode()));
        @Child private CallDispatchHeadNode methodUndefinedNode = DispatchHeadNodeFactory.createMethodCallOnSelf();

        @Specialization
        public DynamicObject undefMethods(VirtualFrame frame, DynamicObject module, Object[] names) {
            for (Object name : names) {
                undefMethod(frame, module, nameToJavaStringNode.executeToJavaString(frame, name));
            }
            return module;
        }

        private void undefMethod(VirtualFrame frame, DynamicObject module, String name) {
            raiseIfFrozenNode.execute(frame);

            Layouts.MODULE.getFields(module).undefMethod(getContext(), this, name);
            if (RubyGuards.isSingletonClass(module)) {
                final DynamicObject receiver = Layouts.CLASS.getAttached(module);
                methodUndefinedNode.call(frame, receiver, "singleton_method_undefined", getSymbol(name));
            } else {
                methodUndefinedNode.call(frame, module, "method_undefined", getSymbol(name));
            }
        }

    }

    @NodeChildren({ @NodeChild(value = "module"), @NodeChild(value = "names") })
    public abstract static class SetVisibilityNode extends RubyNode {

        private final Visibility visibility;

        @Child private SetMethodVisibilityNode setMethodVisibilityNode;

        public SetVisibilityNode(Visibility visibility) {
            this.visibility = visibility;
            setMethodVisibilityNode = ModuleNodesFactory.SetMethodVisibilityNodeGen.create(visibility, null, null);
        }

        public abstract DynamicObject executeSetVisibility(VirtualFrame frame, DynamicObject module, Object[] arguments);

        @Specialization
        public DynamicObject setVisibility(VirtualFrame frame, DynamicObject module, Object[] names) {
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
            final Frame callerFrame = getContext().getCallStack().getCallerFrameIgnoringSend().getFrame(FrameInstance.FrameAccess.READ_WRITE, true);
            DeclarationContext.changeVisibility(callerFrame, visibility);
        }

    }

    @NodeChildren({ @NodeChild(value = "module"), @NodeChild(value = "name") })
    public abstract static class SetMethodVisibilityNode extends RubyNode {

        private final Visibility visibility;

        @Child private NameToJavaStringNode nameToJavaStringNode = NameToJavaStringNode.create();
        @Child private AddMethodNode addMethodNode = AddMethodNodeGen.create(true, false, null, null, null);;

        public SetMethodVisibilityNode(Visibility visibility) {
            this.visibility = visibility;
        }

        public abstract DynamicObject executeSetMethodVisibility(VirtualFrame frame, DynamicObject module, Object name);

        @Specialization
        public DynamicObject setMethodVisibility(VirtualFrame frame, DynamicObject module, Object name,
                @Cached("create()") BranchProfile errorProfile) {
            final String methodName = nameToJavaStringNode.executeToJavaString(frame, name);

            final InternalMethod method = Layouts.MODULE.getFields(module).deepMethodSearch(getContext(), methodName);

            if (method == null) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().nameErrorUndefinedMethod(methodName, module, this));
            }

            /*
             * If the method was already defined in this class, that's fine
             * {@link addMethod} will overwrite it, otherwise we do actually
             * want to add a copy of the method with a different visibility
             * to this module.
             */
            return addMethodNode.executeAddMethod(module, method, visibility);
        }

    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return createModule(getContext(), getEncapsulatingSourceSection(), rubyClass, null, null, this);
        }

    }

    @CoreMethod(names = "singleton_class?")
    public abstract static class IsSingletonClassNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "!isRubyClass(rubyModule)")
        public Object doModule(DynamicObject rubyModule) {
            return false;
        }

        @Specialization(guards = "isRubyClass(rubyClass)")
        public Object doClass(DynamicObject rubyClass) {
            return Layouts.CLASS.getIsSingleton(rubyClass);
        }
    }
}
