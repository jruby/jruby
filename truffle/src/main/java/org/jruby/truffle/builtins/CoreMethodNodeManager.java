/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.builtins;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GeneratedBy;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.RaiseIfFrozenNode;
import org.jruby.truffle.core.array.ArrayUtils;
import org.jruby.truffle.core.cast.TaintResultNode;
import org.jruby.truffle.core.module.ModuleOperations;
import org.jruby.truffle.core.numeric.FixnumLowerNodeGen;
import org.jruby.truffle.language.LexicalScope;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.Options;
import org.jruby.truffle.language.RubyConstant;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.arguments.MissingArgumentBehavior;
import org.jruby.truffle.language.arguments.ReadBlockNode;
import org.jruby.truffle.language.arguments.ReadCallerFrameNode;
import org.jruby.truffle.language.arguments.ReadPreArgumentNode;
import org.jruby.truffle.language.arguments.ReadRemainingArgumentsNode;
import org.jruby.truffle.language.methods.Arity;
import org.jruby.truffle.language.methods.ExceptionTranslatingNode;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.methods.SharedMethodInfo;
import org.jruby.truffle.language.objects.SelfNode;
import org.jruby.truffle.language.objects.SingletonClassNode;
import org.jruby.truffle.language.parser.jruby.Translator;
import org.jruby.truffle.platform.UnsafeGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CoreMethodNodeManager {

    private final RubyContext context;
    private final SingletonClassNode singletonClassNode;

    public CoreMethodNodeManager(RubyContext context, SingletonClassNode singletonClassNode) {
        this.context = context;
        this.singletonClassNode = singletonClassNode;
    }

    public void addCoreMethodNodes(List<? extends NodeFactory<? extends RubyNode>> nodeFactories) {
        for (NodeFactory<? extends RubyNode> nodeFactory : nodeFactories) {
            final GeneratedBy generatedBy = nodeFactory.getClass().getAnnotation(GeneratedBy.class);
            final Class<?> nodeClass = generatedBy.value();
            final CoreClass classAnnotation = nodeClass.getEnclosingClass().getAnnotation(CoreClass.class);
            final CoreMethod methodAnnotation = nodeClass.getAnnotation(CoreMethod.class);

            if (methodAnnotation != null) {
                addCoreMethod(new MethodDetails(classAnnotation, methodAnnotation, nodeFactory));
            }
        }
    }

    private DynamicObject getSingletonClass(Object object) {
        return singletonClassNode.executeSingletonClass(object);
    }

    private void addCoreMethod(MethodDetails methodDetails) {
        DynamicObject module;
        String fullName = methodDetails.getClassAnnotation().value();

        if (fullName.equals("main")) {
            module = getSingletonClass(context.getCoreLibrary().getMainObject());
        } else {
            module = context.getCoreLibrary().getObjectClass();

            for (String moduleName : fullName.split("::")) {
                final RubyConstant constant = ModuleOperations.lookupConstant(context, module, moduleName);

                if (constant == null) {
                    throw new RuntimeException(String.format("Module %s not found when adding core library", moduleName));
                }

                module = (DynamicObject) constant.getValue();
            }
        }

        assert RubyGuards.isRubyModule(module) : fullName;

        final CoreMethod method = methodDetails.getMethodAnnotation();

        final List<String> names = Arrays.asList(method.names());
        assert names.size() >= 1;

        final Visibility visibility = method.visibility();

        if (method.isModuleFunction()) {
            if (visibility != Visibility.PUBLIC) {
                System.err.println("WARNING: visibility ignored when isModuleFunction in " + methodDetails.getIndicativeName());
            }
            if (method.onSingleton()) {
                System.err.println("WARNING: Either onSingleton or isModuleFunction for " + methodDetails.getIndicativeName());
            }
            if (method.constructor()) {
                System.err.println("WARNING: Either constructor or isModuleFunction for " + methodDetails.getIndicativeName());
            }
            if (RubyGuards.isRubyClass(module)) {
                System.err.println("WARNING: Using isModuleFunction on a Class for " + methodDetails.getIndicativeName());
            }
        }
        if (method.onSingleton() && method.constructor()) {
            System.err.println("WARNING: Either onSingleton or constructor for " + methodDetails.getIndicativeName());
        }

        final RubyRootNode rootNode = makeGenericMethod(context, methodDetails);

        if (method.isModuleFunction()) {
            addMethod(context, module, rootNode, names, Visibility.PRIVATE);
            addMethod(context, getSingletonClass(module), rootNode, names, Visibility.PUBLIC);
        } else if (method.onSingleton() || method.constructor()) {
            addMethod(context, getSingletonClass(module), rootNode, names, visibility);
        } else {
            addMethod(context, module, rootNode, names, visibility);
        }
    }

    private static void addMethod(RubyContext context, DynamicObject module, RubyRootNode rootNode, List<String> names, final Visibility originalVisibility) {
        assert RubyGuards.isRubyModule(module);

        for (String name : names) {
            final RubyRootNode rootNodeCopy = NodeUtil.cloneNode(rootNode);

            Visibility visibility = originalVisibility;
            if (ModuleOperations.isMethodPrivateFromName(name)) {
                visibility = Visibility.PRIVATE;
            }

            final InternalMethod method = new InternalMethod(rootNodeCopy.getSharedMethodInfo(), name, module, visibility, Truffle.getRuntime().createCallTarget(rootNodeCopy));

            Layouts.MODULE.getFields(module).addMethod(context, null, method.withVisibility(visibility).withName(name));
        }
    }

    private static RubyRootNode makeGenericMethod(RubyContext context, MethodDetails methodDetails) {
        final CoreMethod method = methodDetails.getMethodAnnotation();

        final SourceSection sourceSection = SourceSection.createUnavailable(null, String.format("%s#%s", methodDetails.getClassAnnotation().value(), method.names()[0]));

        final int required = method.required();
        final int optional = method.optional();
        final boolean needsCallerFrame = method.needsCallerFrame();
        final boolean alwaysInline = needsCallerFrame && context.getOptions().INLINE_NEEDS_CALLER_FRAME;

        final Arity arity = new Arity(required, optional, method.rest());

        final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, LexicalScope.NONE, arity, method.names()[0], false, null, context.getOptions().CORE_ALWAYS_CLONE, alwaysInline, needsCallerFrame);

        final List<RubyNode> argumentsNodes = new ArrayList<>();

        if (needsCallerFrame) {
            argumentsNodes.add(new ReadCallerFrameNode());
        }

        // Do not use needsSelf=true in module functions, it is either the module/class or the instance.
        // Usage of needsSelf is quite rare for singleton methods (except constructors).
        final boolean needsSelf = method.constructor() || (!method.isModuleFunction() && !method.onSingleton() && method.needsSelf());

        if (needsSelf) {
            RubyNode readSelfNode = new SelfNode(context, sourceSection);

            if (method.lowerFixnumSelf()) {
                readSelfNode = FixnumLowerNodeGen.create(context, sourceSection, readSelfNode);
            }

            if (method.raiseIfFrozenSelf()) {
                readSelfNode = new RaiseIfFrozenNode(readSelfNode);
            }

            argumentsNodes.add(readSelfNode);
        }

        for (int n = 0; n < arity.getPreRequired() + arity.getOptional(); n++) {
            RubyNode readArgumentNode = new ReadPreArgumentNode(n, MissingArgumentBehavior.UNDEFINED);

            if (ArrayUtils.contains(method.lowerFixnumParameters(), n)) {
                readArgumentNode = FixnumLowerNodeGen.create(context, sourceSection, readArgumentNode);
            }

            if (ArrayUtils.contains(method.raiseIfFrozenParameters(), n)) {
                readArgumentNode = new RaiseIfFrozenNode(readArgumentNode);
            }

            argumentsNodes.add(readArgumentNode);
        }
        if (method.rest()) {
            argumentsNodes.add(new ReadRemainingArgumentsNode(arity.getPreRequired() + arity.getOptional()));
        }

        if (method.needsBlock()) {
            argumentsNodes.add(new ReadBlockNode(NotProvided.INSTANCE));
        }

        final RubyNode methodNode;
        final NodeFactory<? extends RubyNode> nodeFactory = methodDetails.getNodeFactory();
        List<List<Class<?>>> signatures = nodeFactory.getNodeSignatures();

        assert signatures.size() == 1;
        List<Class<?>> signature = signatures.get(0);

        if (signature.size() == 0) {
            methodNode = nodeFactory.createNode();
        } else {
            final RubyNode[] argumentsArray = argumentsNodes.toArray(new RubyNode[argumentsNodes.size()]);
            if (signature.size() == 1 && signature.get(0) == RubyNode[].class) {
                methodNode = nodeFactory.createNode(new Object[] { argumentsArray });
            } else if (signature.size() >= 3 && signature.get(2) == RubyNode[].class) {
                methodNode = nodeFactory.createNode(context, sourceSection, argumentsArray);
            } else if (signature.get(0) != RubyContext.class) {
                Object[] args = argumentsArray;
                methodNode = nodeFactory.createNode(args);
            } else {
                Object[] args = new Object[2 + argumentsNodes.size()];
                args[0] = context;
                args[1] = sourceSection;
                System.arraycopy(argumentsArray, 0, args, 2, argumentsNodes.size());
                methodNode = nodeFactory.createNode(args);
            }
        }

        if (System.getenv("TRUFFLE_CHECK_AMBIGUOUS_OPTIONAL_ARGS") != null) {
            AmbiguousOptionalArgumentChecker.verifyNoAmbiguousOptionalArguments(methodDetails);
        }

        final RubyNode checkArity = Translator.createCheckArityNode(context, sourceSection, arity);

        RubyNode sequence;

        if (!isSafe(context, method.unsafe())) {
            sequence = new UnsafeNode(context, sourceSection);
        } else {
            sequence = Translator.sequence(context, sourceSection, Arrays.asList(checkArity, methodNode));

            if (method.returnsEnumeratorIfNoBlock()) {
                // TODO BF 3-18-2015 Handle multiple method names correctly
                sequence = new ReturnEnumeratorIfNoBlockNode(method.names()[0], sequence);
            }

            if (method.taintFromSelf() || method.taintFromParameter() != -1) {
                sequence = new TaintResultNode(method.taintFromSelf(),
                        method.taintFromParameter(),
                        sequence);
            }
        }

        final ExceptionTranslatingNode exceptionTranslatingNode = new ExceptionTranslatingNode(context, sourceSection, sequence, method.unsupportedOperationBehavior());

        return new RubyRootNode(context, sourceSection, null, sharedMethodInfo, exceptionTranslatingNode, false);
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
        if (System.getenv("TRUFFLE_CHECK_AMBIGUOUS_OPTIONAL_ARGS") != null &&
            !AmbiguousOptionalArgumentChecker.SUCCESS) {
            System.exit(1);
        }
    }

    public static class MethodDetails {

        private final CoreClass classAnnotation;
        private final CoreMethod methodAnnotation;
        private final NodeFactory<? extends RubyNode> nodeFactory;

        public MethodDetails(CoreClass classAnnotation, CoreMethod methodAnnotation, NodeFactory<? extends RubyNode> nodeFactory) {
            assert classAnnotation != null;
            assert methodAnnotation != null;
            assert nodeFactory != null;
            this.classAnnotation = classAnnotation;
            this.methodAnnotation = methodAnnotation;
            this.nodeFactory = nodeFactory;
        }

        public CoreClass getClassAnnotation() {
            return classAnnotation;
        }

        public CoreMethod getMethodAnnotation() {
            return methodAnnotation;
        }

        public NodeFactory<? extends RubyNode> getNodeFactory() {
            return nodeFactory;
        }

        public String getIndicativeName() {
            return classAnnotation.value() + "#" + methodAnnotation.names()[0];
        }
    }

}
