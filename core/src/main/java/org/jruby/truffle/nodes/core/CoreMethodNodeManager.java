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

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GeneratedBy;
import com.oracle.truffle.api.dsl.NodeFactory;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.CoreSourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.control.SequenceNode;
import org.jruby.truffle.nodes.methods.ExceptionTranslatingNode;
import org.jruby.truffle.nodes.methods.arguments.*;
import org.jruby.truffle.nodes.objects.SelfNode;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.util.ArrayUtils;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.methods.Arity;
import org.jruby.truffle.runtime.methods.RubyMethod;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CoreMethodNodeManager {

    public static void addCoreMethodNodes(RubyClass rubyObjectClass, List<? extends NodeFactory<? extends CoreMethodNode>> nodeFactories) {
        for (NodeFactory<? extends CoreMethodNode> nodeFactory : nodeFactories) {
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

        if (methodDetails.getClassAnnotation().name().equals("main")) {
            module = context.getCoreLibrary().getMainObject().getSingletonClass(null);
        } else {
            module = rubyObjectClass;

            for (String moduleName : methodDetails.getClassAnnotation().name().split("::")) {
                module = (RubyModule) ModuleOperations.lookupConstant(module, moduleName).getValue();
            }
        }

        assert module != null : methodDetails.getClassAnnotation().name();

        final CoreMethod anno = methodDetails.getMethodAnnotation();

        final List<String> names = Arrays.asList(anno.names());
        assert names.size() >= 1;

        final String canonicalName = names.get(0);
        final List<String> aliases = names.subList(1, names.size());

        final Visibility visibility = anno.visibility();

        if (anno.isModuleFunction()) {
            if (visibility != Visibility.PUBLIC) {
                System.err.println("WARNING: visibility ignored when isModuleFunction in " + methodDetails.getIndicativeName());
            }
            if (anno.onSingleton()) {
                System.err.println("WARNING: Either onSingleton or isModuleFunction for " + methodDetails.getIndicativeName());
            }
        }

        // Do not use needsSelf=true in module functions, it is either the module/class or the instance.
        // Usage of needsSelf is quite rare for singleton methods (except constructors).
        final boolean needsSelf = !anno.isModuleFunction() && !anno.onSingleton() && anno.needsSelf();

        final RubyRootNode rootNode = makeGenericMethod(context, methodDetails, needsSelf);

        final RubyMethod method = new RubyMethod(rootNode.getSharedMethodInfo(), canonicalName, module, visibility, false,
                Truffle.getRuntime().createCallTarget(rootNode), null);

        if (anno.isModuleFunction()) {
            addMethod(module, method, aliases, Visibility.PRIVATE);
            addMethod(module.getSingletonClass(null), method, aliases, Visibility.PUBLIC);
        } else if (anno.onSingleton()) {
            addMethod(module.getSingletonClass(null), method, aliases, visibility);
        } else {
            addMethod(module, method, aliases, visibility);
        }
    }

    private static void addMethod(RubyModule module, RubyMethod method, List<String> aliases, Visibility visibility) {
        method = method.withVisibility(visibility);

        module.addMethod(null, method);
        for (String alias : aliases) {
            module.addMethod(null, method.withNewName(alias));
        }
    }

    private static RubyRootNode makeGenericMethod(RubyContext context, MethodDetails methodDetails, boolean needsSelf) {
        final CoreSourceSection sourceSection = new CoreSourceSection(methodDetails.getClassAnnotation().name(), methodDetails.getMethodAnnotation().names()[0]);

        final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, methodDetails.getIndicativeName(), false, null);

        final Arity arity = new Arity(methodDetails.getMethodAnnotation().minArgs(), methodDetails.getMethodAnnotation().maxArgs());

        final List<RubyNode> argumentsNodes = new ArrayList<>();

        if (needsSelf) {
            RubyNode readSelfNode = new SelfNode(context, sourceSection);

            if (methodDetails.getMethodAnnotation().lowerFixnumSelf()) {
                readSelfNode = new FixnumLowerNode(readSelfNode);
            }

            argumentsNodes.add(readSelfNode);
        }

        if (methodDetails.getMethodAnnotation().isSplatted()) {
            argumentsNodes.add(new ReadAllArgumentsNode(context, sourceSection));
        } else {
            if (arity.getMaximum() == Arity.NO_MAXIMUM) {
                throw new UnsupportedOperationException("if a core method isn't splatted, you need to specify a maximum");
            }

            for (int n = 0; n < arity.getMaximum(); n++) {
                RubyNode readArgumentNode = new ReadPreArgumentNode(context, sourceSection, n, MissingArgumentBehaviour.UNDEFINED);

                if (ArrayUtils.contains(methodDetails.getMethodAnnotation().lowerFixnumParameters(), n)) {
                    readArgumentNode = new FixnumLowerNode(readArgumentNode);
                }

                argumentsNodes.add(readArgumentNode);
            }
        }

        if (methodDetails.getMethodAnnotation().needsBlock()) {
            argumentsNodes.add(new ReadBlockNode(context, sourceSection, UndefinedPlaceholder.INSTANCE));
        }

        final RubyNode methodNode = methodDetails.getNodeFactory().createNode(context, sourceSection, argumentsNodes.toArray(new RubyNode[argumentsNodes.size()]));
        final CheckArityNode checkArity = new CheckArityNode(context, sourceSection, arity);
        final RubyNode block = SequenceNode.sequence(context, sourceSection, checkArity, methodNode);
        final ExceptionTranslatingNode exceptionTranslatingNode = new ExceptionTranslatingNode(context, sourceSection, block);

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
