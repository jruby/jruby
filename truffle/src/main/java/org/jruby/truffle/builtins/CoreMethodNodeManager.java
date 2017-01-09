/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.builtins;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GeneratedBy;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.Log;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.RaiseIfFrozenNode;
import org.jruby.truffle.core.array.ArrayUtils;
import org.jruby.truffle.core.cast.TaintResultNode;
import org.jruby.truffle.core.module.ModuleOperations;
import org.jruby.truffle.core.numeric.FixnumLowerNodeGen;
import org.jruby.truffle.core.string.StringUtils;
import org.jruby.truffle.language.LexicalScope;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyConstant;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.SourceIndexLength;
import org.jruby.truffle.language.Visibility;
import org.jruby.truffle.language.arguments.MissingArgumentBehavior;
import org.jruby.truffle.language.arguments.ProfileArgumentNode;
import org.jruby.truffle.language.arguments.ReadBlockNode;
import org.jruby.truffle.language.arguments.ReadCallerFrameNode;
import org.jruby.truffle.language.arguments.ReadPreArgumentNode;
import org.jruby.truffle.language.arguments.ReadRemainingArgumentsNode;
import org.jruby.truffle.language.arguments.ReadSelfNode;
import org.jruby.truffle.language.methods.Arity;
import org.jruby.truffle.language.methods.ExceptionTranslatingNode;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.methods.SharedMethodInfo;
import org.jruby.truffle.language.objects.SingletonClassNode;
import org.jruby.truffle.options.Options;
import org.jruby.truffle.parser.Translator;
import org.jruby.truffle.platform.UnsafeGroup;
import org.jruby.truffle.tools.ChaosNodeGen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CoreMethodNodeManager {

    private static final boolean CHECK_DSL_USAGE = System.getenv("TRUFFLE_CHECK_DSL_USAGE") != null;
    private final RubyContext context;
    private final SingletonClassNode singletonClassNode;
    private final PrimitiveManager primitiveManager;

    public CoreMethodNodeManager(RubyContext context, SingletonClassNode singletonClassNode, PrimitiveManager primitiveManager) {
        this.context = context;
        this.singletonClassNode = singletonClassNode;
        this.primitiveManager = primitiveManager;
    }

    public void addCoreMethodNodes(List<? extends NodeFactory<? extends RubyNode>> nodeFactories) {
        String moduleName = null;
        DynamicObject module = null;

        for (NodeFactory<? extends RubyNode> nodeFactory : nodeFactories) {
            final Class<?> nodeClass = nodeFactory.getClass().getAnnotation(GeneratedBy.class).value();
            final CoreMethod methodAnnotation = nodeClass.getAnnotation(CoreMethod.class);
            Primitive primitiveAnnotation;

            if (methodAnnotation != null) {
                if (module == null) {
                    moduleName = nodeClass.getEnclosingClass().getAnnotation(CoreClass.class).value();
                    module = getModule(moduleName);
                }
                addCoreMethod(module, new MethodDetails(moduleName, methodAnnotation, nodeFactory));
            } else if ((primitiveAnnotation = nodeClass.getAnnotation(Primitive.class)) != null) {
                primitiveManager.addPrimitive(nodeFactory, primitiveAnnotation);
            }
        }
    }

    private DynamicObject getModule(String fullName) {
        DynamicObject module;

        if (fullName.equals("main")) {
            module = getSingletonClass(context.getCoreLibrary().getMainObject());
        } else {
            module = context.getCoreLibrary().getObjectClass();

            for (String moduleName : fullName.split("::")) {
                final RubyConstant constant = ModuleOperations.lookupConstant(context, module, moduleName);

                if (constant == null) {
                    throw new RuntimeException(StringUtils.format("Module %s not found when adding core library", moduleName));
                }

                module = (DynamicObject) constant.getValue();
            }
        }

        assert RubyGuards.isRubyModule(module) : fullName;
        return module;
    }

    private DynamicObject getSingletonClass(Object object) {
        return singletonClassNode.executeSingletonClass(object);
    }

    private void addCoreMethod(DynamicObject module, MethodDetails methodDetails) {
        final CoreMethod method = methodDetails.getMethodAnnotation();

        final String[] names = method.names();
        assert names.length >= 1;

        final Visibility visibility = method.visibility();

        if (method.isModuleFunction()) {
            if (visibility != Visibility.PUBLIC) {
                Log.LOGGER.warning("visibility ignored when isModuleFunction in " + methodDetails.getIndicativeName());
            }
            if (method.onSingleton()) {
                Log.LOGGER.warning("either onSingleton or isModuleFunction for " + methodDetails.getIndicativeName());
            }
            if (method.constructor()) {
                Log.LOGGER.warning("either constructor or isModuleFunction for " + methodDetails.getIndicativeName());
            }
            if (RubyGuards.isRubyClass(module)) {
                Log.LOGGER.warning("using isModuleFunction on a Class for " + methodDetails.getIndicativeName());
            }
        }
        if (method.onSingleton() && method.constructor()) {
            Log.LOGGER.warning("either onSingleton or constructor for " + methodDetails.getIndicativeName());
        }

        final SharedMethodInfo sharedMethodInfo = makeSharedMethodInfo(context, module, methodDetails);
        final CallTarget callTarget = makeGenericMethod(context, methodDetails, sharedMethodInfo);

        if (method.isModuleFunction()) {
            addMethod(context, module, sharedMethodInfo, callTarget, names, Visibility.PRIVATE);
            addMethod(context, getSingletonClass(module), sharedMethodInfo, callTarget, names, Visibility.PUBLIC);
        } else if (method.onSingleton() || method.constructor()) {
            addMethod(context, getSingletonClass(module), sharedMethodInfo, callTarget, names, visibility);
        } else {
            addMethod(context, module, sharedMethodInfo, callTarget, names, visibility);
        }
    }

    private static void addMethod(RubyContext context, DynamicObject module, SharedMethodInfo sharedMethodInfo, CallTarget callTarget, String[] names, Visibility originalVisibility) {
        assert RubyGuards.isRubyModule(module);

        for (String name : names) {
            Visibility visibility = originalVisibility;
            if (ModuleOperations.isMethodPrivateFromName(name)) {
                visibility = Visibility.PRIVATE;
            }
            final InternalMethod method = new InternalMethod(context, sharedMethodInfo, sharedMethodInfo.getLexicalScope(), name, module, visibility, callTarget);

            Layouts.MODULE.getFields(module).addMethod(context, null, method);
        }
    }

    private static SharedMethodInfo makeSharedMethodInfo(RubyContext context, DynamicObject module, MethodDetails methodDetails) {
        final CoreMethod method = methodDetails.getMethodAnnotation();
        final LexicalScope lexicalScope = new LexicalScope(context.getRootLexicalScope(), module);

        return new SharedMethodInfo(
                context.getCoreLibrary().getSourceSection(),
                lexicalScope,
                new Arity(method.required(), method.optional(), method.rest()),
                module,
                methodDetails.getPrimaryName(),
                "builtin",
                null,
                context.getOptions().CORE_ALWAYS_CLONE,
                method.needsCallerFrame() && context.getOptions().INLINE_NEEDS_CALLER_FRAME,
                method.needsCallerFrame());
    }

    private static CallTarget makeGenericMethod(RubyContext context, MethodDetails methodDetails, SharedMethodInfo sharedMethodInfo) {
        final CoreMethod method = methodDetails.getMethodAnnotation();

        final SourceSection sourceSection = sharedMethodInfo.getSourceSection();
        final SourceIndexLength sourceIndexLength = new SourceIndexLength(sourceSection.getCharIndex(), sourceSection.getCharLength());

        final RubyNode methodNode = createCoreMethodNode(context, sourceSection.getSource(), sourceIndexLength, methodDetails.getNodeFactory(), method);

        if (CHECK_DSL_USAGE) {
            AmbiguousOptionalArgumentChecker.verifyNoAmbiguousOptionalArguments(methodDetails);
        }

        final RubyNode checkArity = Translator.createCheckArityNode(sharedMethodInfo.getArity());

        RubyNode node;
        if (!isSafe(context, method.unsafe())) {
            node = new UnsafeNode();
        } else {
            node = Translator.sequence(sourceIndexLength, Arrays.asList(checkArity, methodNode));
            node = transformResult(method, node);
        }

        RubyNode bodyNode = new ExceptionTranslatingNode(node, method.unsupportedOperationBehavior());

        if (context.getOptions().CHAOS) {
            bodyNode = ChaosNodeGen.create(bodyNode);
        }

        final RubyRootNode rootNode = new RubyRootNode(context, sourceSection, null, sharedMethodInfo, bodyNode, false);

        return Truffle.getRuntime().createCallTarget(rootNode);
    }

    public static RubyNode createCoreMethodNode(RubyContext context, Source source, SourceIndexLength sourceSection, NodeFactory<? extends RubyNode> nodeFactory, CoreMethod method) {
        final List<RubyNode> argumentsNodes = new ArrayList<>();

        if (method.needsCallerFrame()) {
            argumentsNodes.add(new ReadCallerFrameNode());
        }

        final boolean needsSelf = needsSelf(method);

        if (needsSelf) {
            RubyNode readSelfNode = new ProfileArgumentNode(new ReadSelfNode());
            argumentsNodes.add(transformArgument(method, readSelfNode, 0));
        }

        final int required = method.required();
        final int optional = method.optional();
        final int nArgs = required + optional;

        if (CHECK_DSL_USAGE) {
            LowerFixnumChecker.checkLowerFixnumArguments(nodeFactory, needsSelf ? 1 : 0, method);
        }

        for (int n = 0; n < nArgs; n++) {
            RubyNode readArgumentNode = new ProfileArgumentNode(new ReadPreArgumentNode(n, MissingArgumentBehavior.UNDEFINED));
            argumentsNodes.add(transformArgument(method, readArgumentNode, n + 1));
        }

        if (method.rest()) {
            argumentsNodes.add(new ReadRemainingArgumentsNode(nArgs));
        }

        if (method.needsBlock()) {
            argumentsNodes.add(new ReadBlockNode(NotProvided.INSTANCE));
        }

        return createNodeFromFactory(context, source, sourceSection, nodeFactory, argumentsNodes);
    }

    public static <T> T createNodeFromFactory(RubyContext context, Source source, SourceIndexLength sourceSection, NodeFactory<? extends T> nodeFactory, List<RubyNode> argumentsNodes) {
        final T methodNode;
        List<List<Class<?>>> signatures = nodeFactory.getNodeSignatures();

        assert signatures.size() == 1;
        List<Class<?>> signature = signatures.get(0);

        if (signature.size() == 0) {
            methodNode = nodeFactory.createNode();
        } else {
            final RubyNode[] argumentsArray = argumentsNodes.toArray(new RubyNode[argumentsNodes.size()]);
            if (signature.size() == 1 && signature.get(0) == RubyNode[].class) {
                methodNode = nodeFactory.createNode(new Object[] { argumentsArray });
            } else if (signature.size() >= 2 && signature.get(1) == RubyNode[].class) {
                if (signature.get(0) == SourceIndexLength.class) {
                    methodNode = nodeFactory.createNode(sourceSection, argumentsArray);
                } else {
                    throw new UnsupportedOperationException();
                }
            } else if (signature.get(0) != SourceIndexLength.class) {
                Object[] args = argumentsArray;
                methodNode = nodeFactory.createNode(args);
            } else {
                Object[] args = new Object[1 + argumentsNodes.size()];
                args[0] = context;

                if (signature.get(0) == SourceIndexLength.class) {
                    args[0] = sourceSection;
                } else {
                    throw new UnsupportedOperationException();
                }

                System.arraycopy(argumentsArray, 0, args, 1, argumentsNodes.size());
                methodNode = nodeFactory.createNode(args);
            }
        }

        return methodNode;
    }

    public static boolean needsSelf(CoreMethod method) {
        // Do not use needsSelf=true in module functions, it is either the module/class or the instance.
        // Usage of needsSelf is quite rare for singleton methods (except constructors).
        return method.constructor() || (!method.isModuleFunction() && !method.onSingleton() && method.needsSelf());
    }

    private static RubyNode transformArgument(CoreMethod method, RubyNode argument, int n) {
        if (ArrayUtils.contains(method.lowerFixnum(), n)) {
            argument = FixnumLowerNodeGen.create(argument);
        }

        if (n == 0 && method.raiseIfFrozenSelf()) {
            argument = new RaiseIfFrozenNode(argument);
        }

        return argument;
    }

    private static RubyNode transformResult(CoreMethod method, RubyNode node) {
        if (!method.enumeratorSize().isEmpty()) {
            assert !method.returnsEnumeratorIfNoBlock(): "Only one of enumeratorSize or returnsEnumeratorIfNoBlock can be specified";
            // TODO BF 6-27-2015 Handle multiple method names correctly
            node = new EnumeratorSizeNode(method.enumeratorSize(), method.names()[0], node);
        } else if (method.returnsEnumeratorIfNoBlock()) {
            // TODO BF 3-18-2015 Handle multiple method names correctly
            node = new ReturnEnumeratorIfNoBlockNode(method.names()[0], node);
        }

        if (method.taintFrom() != -1) {
            final boolean taintFromSelf = method.taintFrom() == 0;
            final int taintFromArg = taintFromSelf ? -1 : method.taintFrom() - 1;
            node = new TaintResultNode(taintFromSelf, taintFromArg, node);
        }

        return node;
    }

    public static boolean isSafe(RubyContext context, UnsafeGroup[] groups) {
        final Options options = context.getOptions();

        for (UnsafeGroup group : groups) {
            final boolean option;

            switch (group) {
                case LOAD:
                    option = options.PLATFORM_SAFE_LOAD;
                    break;
                case IO:
                    option = options.PLATFORM_SAFE_IO;
                    break;
                case MEMORY:
                    option = options.PLATFORM_SAFE_MEMORY;
                    break;
                case THREADS:
                    option = options.PLATFORM_SAFE_THREADS;
                    break;
                case PROCESSES:
                    option = options.PLATFORM_SAFE_PROCESSES;
                    break;
                case SIGNALS:
                    option = options.PLATFORM_SAFE_SIGNALS;
                    break;
                case EXIT:
                    option = options.PLATFORM_SAFE_EXIT;
                    break;
                case AT_EXIT:
                    option = options.PLATFORM_SAFE_AT_EXIT;
                    break;
                case SAFE_PUTS:
                    option = options.PLATFORM_SAFE_PUTS;
                    break;
                default:
                    throw new IllegalStateException();
            }

            if (!option) {
                return false;
            }
        }

        return true;
    }

    public void allMethodInstalled() {
        if (CHECK_DSL_USAGE) {
            if (!(AmbiguousOptionalArgumentChecker.SUCCESS && LowerFixnumChecker.SUCCESS)) {
                System.exit(1);
            }
        }
    }

    public static class MethodDetails {

        private final String moduleName;
        private final CoreMethod methodAnnotation;
        private final NodeFactory<? extends RubyNode> nodeFactory;

        public MethodDetails(String moduleName, CoreMethod methodAnnotation, NodeFactory<? extends RubyNode> nodeFactory) {
            this.moduleName = moduleName;
            this.methodAnnotation = methodAnnotation;
            this.nodeFactory = nodeFactory;
        }

        public CoreMethod getMethodAnnotation() {
            return methodAnnotation;
        }

        public NodeFactory<? extends RubyNode> getNodeFactory() {
            return nodeFactory;
        }

        public String getModuleName() {
            return moduleName;
        }

        public String getPrimaryName() {
            return methodAnnotation.names()[0];
        }

        public String getIndicativeName() {
            return moduleName + "#" + getPrimaryName();
        }
    }

}
