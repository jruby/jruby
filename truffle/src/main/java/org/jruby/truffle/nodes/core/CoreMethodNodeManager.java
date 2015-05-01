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

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GeneratedBy;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.nodes.NodeUtil;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.CoreSourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.cast.TaintResultNode;
import org.jruby.truffle.nodes.control.SequenceNode;
import org.jruby.truffle.nodes.methods.ExceptionTranslatingNode;
import org.jruby.truffle.nodes.methods.arguments.*;
import org.jruby.truffle.nodes.objects.SelfNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.TruffleFatalException;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.methods.Arity;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;
import org.jruby.truffle.runtime.util.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CoreMethodNodeManager {

    public static void addCoreMethodNodes(RubyClass rubyObjectClass, List<? extends NodeFactory<? extends RubyNode>> nodeFactories) {
        for (NodeFactory<? extends RubyNode> nodeFactory : nodeFactories) {
            final GeneratedBy generatedBy = nodeFactory.getClass().getAnnotation(GeneratedBy.class);
            final Class<?> nodeClass = generatedBy.value();
            final CoreClass classAnnotation = nodeClass.getEnclosingClass().getAnnotation(CoreClass.class);
            final CoreMethod methodAnnotation = nodeClass.getAnnotation(CoreMethod.class);

            if (methodAnnotation != null) {
                final MethodDetails details = new MethodDetails(classAnnotation, methodAnnotation, nodeFactory);
                addMethod(rubyObjectClass, details);
            }
        }
    }

    private static void addMethod(RubyClass rubyObjectClass, MethodDetails methodDetails) {
        assert rubyObjectClass != null;
        assert methodDetails != null;

        final RubyContext context = rubyObjectClass.getContext();

        RubyModule module;
        String fullName = methodDetails.getClassAnnotation().name();

        if (fullName.equals("main")) {
            module = context.getCoreLibrary().getMainObject().getSingletonClass(null);
        } else {
            module = rubyObjectClass;

            for (String moduleName : fullName.split("::")) {
                final RubyConstant constant = ModuleOperations.lookupConstant(context, LexicalScope.NONE, module, moduleName);

                if (constant == null) {
                    throw new RuntimeException(String.format("Module %s not found when adding core library", moduleName));
                }

                module = (RubyModule) constant.getValue();
            }
        }

        assert module != null : fullName;

        final CoreMethod anno = methodDetails.getMethodAnnotation();

        final List<String> names = Arrays.asList(anno.names());
        assert names.size() >= 1;

        final Visibility visibility = anno.visibility();

        if (anno.isModuleFunction()) {
            if (visibility != Visibility.PUBLIC) {
                System.err.println("WARNING: visibility ignored when isModuleFunction in " + methodDetails.getIndicativeName());
            }
            if (anno.onSingleton()) {
                System.err.println("WARNING: Either onSingleton or isModuleFunction for " + methodDetails.getIndicativeName());
            }
            if (!module.isOnlyAModule()) {
                System.err.println("WARNING: Using isModuleFunction on a Class for " + methodDetails.getIndicativeName());
            }
        }

        // Do not use needsSelf=true in module functions, it is either the module/class or the instance.
        // Usage of needsSelf is quite rare for singleton methods (except constructors).
        final boolean needsSelf = !anno.isModuleFunction() && !anno.onSingleton() && anno.needsSelf() || anno.reallyDoesNeedSelf();

        final RubyRootNode rootNode = makeGenericMethod(context, methodDetails, needsSelf);

        if (anno.isModuleFunction()) {
            addMethod(module, rootNode, names, Visibility.PRIVATE);
            addMethod(module.getSingletonClass(null), rootNode, names, Visibility.PUBLIC);
        } else if (anno.onSingleton()) {
            addMethod(module.getSingletonClass(null), rootNode, names, visibility);
        } else {
            addMethod(module, rootNode, names, visibility);
        }
    }

    private static void addMethod(RubyModule module, RubyRootNode rootNode, List<String> names, final Visibility originalVisibility) {
        for (String name : names) {
            final RubyRootNode rootNodeCopy = NodeUtil.cloneNode(rootNode);

            Visibility visibility = originalVisibility;
            if (ModuleOperations.isMethodPrivateFromName(name)) {
                visibility = Visibility.PRIVATE;
            }

            final InternalMethod method = new InternalMethod(rootNodeCopy.getSharedMethodInfo(), name, module, visibility, false,
                    Truffle.getRuntime().createCallTarget(rootNodeCopy), null);

            module.addMethod(null, method.withVisibility(visibility).withName(name));
        }
    }

    private static RubyRootNode makeGenericMethod(RubyContext context, MethodDetails methodDetails, boolean needsSelf) {
        final CoreSourceSection sourceSection = new CoreSourceSection(methodDetails.getClassAnnotation().name(), methodDetails.getMethodAnnotation().names()[0]);

        final int required = methodDetails.getMethodAnnotation().required();
        final int optional;

        if (methodDetails.getMethodAnnotation().argumentsAsArray()) {
            optional = 0;
        } else {
            optional = methodDetails.getMethodAnnotation().optional();
        }

        final Arity arity = new Arity(required,  optional, methodDetails.getMethodAnnotation().argumentsAsArray(), false, false, 0);

        final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, null, arity, methodDetails.getIndicativeName(), false, null, true);

        final List<RubyNode> argumentsNodes = new ArrayList<>();

        if (needsSelf) {
            RubyNode readSelfNode = new SelfNode(context, sourceSection);

            if (methodDetails.getMethodAnnotation().lowerFixnumSelf()) {
                readSelfNode = new FixnumLowerNode(readSelfNode);
            }

            if (methodDetails.getMethodAnnotation().raiseIfFrozenSelf()) {
                readSelfNode = new RaiseIfFrozenNode(readSelfNode);
            }

            argumentsNodes.add(readSelfNode);
        }

        if (methodDetails.getMethodAnnotation().argumentsAsArray()) {
            argumentsNodes.add(new ReadAllArgumentsNode(context, sourceSection));
        } else {
            for (int n = 0; n < arity.getRequired() + arity.getOptional(); n++) {
                RubyNode readArgumentNode = new ReadPreArgumentNode(context, sourceSection, n, MissingArgumentBehaviour.UNDEFINED);

                if (ArrayUtils.contains(methodDetails.getMethodAnnotation().lowerFixnumParameters(), n)) {
                    readArgumentNode = new FixnumLowerNode(readArgumentNode);
                }

                if (ArrayUtils.contains(methodDetails.getMethodAnnotation().raiseIfFrozenParameters(), n)) {
                    readArgumentNode = new RaiseIfFrozenNode(readArgumentNode);
                }

                argumentsNodes.add(readArgumentNode);
            }
        }

        if (methodDetails.getMethodAnnotation().needsBlock()) {
            argumentsNodes.add(new ReadBlockNode(context, sourceSection, UndefinedPlaceholder.INSTANCE));
        }

        RubyNode methodNode = null;
        final NodeFactory<?> nodeFactory = methodDetails.getNodeFactory();
        List<List<Class<?>>> signatures = nodeFactory.getNodeSignatures();
        assert !signatures.isEmpty();

        for (List<Class<?>> signature : signatures) {
            if (signature.size() >= 1 && signature.get(0) != RubyContext.class && signature.get(0) != nodeFactory.getNodeClass()) {
                throw new TruffleFatalException("Copy constructor with wrong type for previous in "+nodeFactory.getNodeClass()+" : "+signature.get(0), null);
            } else if (signature.size() >= 3 && signature.get(2) == RubyNode[].class) {
                methodNode = methodDetails.getNodeFactory().createNode(context, sourceSection, argumentsNodes.toArray(new RubyNode[argumentsNodes.size()]));
            } else {
                Object[] args = new Object[2 + argumentsNodes.size()];
                args[0] = context;
                args[1] = sourceSection;
                System.arraycopy(argumentsNodes.toArray(new RubyNode[argumentsNodes.size()]), 0, args, 2, argumentsNodes.size());
                methodNode = methodDetails.getNodeFactory().createNode(args);
            }
        }

        final CheckArityNode checkArity = new CheckArityNode(context, sourceSection, arity);
        RubyNode sequence = SequenceNode.sequence(context, sourceSection, checkArity, methodNode);

        if (methodDetails.getMethodAnnotation().returnsEnumeratorIfNoBlock()) {
            // TODO BF 3-18-2015 Handle multiple method names correctly
            sequence = new ReturnEnumeratorIfNoBlockNode(methodDetails.getMethodAnnotation().names()[0], sequence);
        }

        if (methodDetails.getMethodAnnotation().taintFromSelf() || methodDetails.getMethodAnnotation().taintFromParameter() != -1) {
            sequence = new TaintResultNode(methodDetails.getMethodAnnotation().taintFromSelf(),
                                           methodDetails.getMethodAnnotation().taintFromParameter(),
                                           sequence);
        }

        final ExceptionTranslatingNode exceptionTranslatingNode = new ExceptionTranslatingNode(context, sourceSection, sequence, methodDetails.getMethodAnnotation().unsupportedOperationBehavior());

        return new RubyRootNode(context, sourceSection, null, sharedMethodInfo, exceptionTranslatingNode);
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
            return classAnnotation.name() + "#" + methodAnnotation.names()[0] + "(core)";
        }
    }

}
