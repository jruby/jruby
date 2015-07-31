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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeUtil;

import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.arguments.*;
import org.jruby.truffle.nodes.cast.TaintResultNode;
import org.jruby.truffle.nodes.control.SequenceNode;
import org.jruby.truffle.nodes.core.fixnum.FixnumLowerNodeGen;
import org.jruby.truffle.nodes.methods.ExceptionTranslatingNode;
import org.jruby.truffle.nodes.objects.SelfNode;
import org.jruby.truffle.nodes.objects.SingletonClassNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.array.ArrayUtils;
import org.jruby.truffle.runtime.core.CoreSourceSection;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.methods.Arity;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CoreMethodNodeManager {

    private final RubyBasicObject objectClass;
    private final SingletonClassNode singletonClassNode;

    public CoreMethodNodeManager(RubyBasicObject objectClass, SingletonClassNode singletonClassNode) {
        assert RubyGuards.isRubyClass(objectClass);
        this.objectClass = objectClass;
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

    private RubyBasicObject getSingletonClass(Object object) {
        return singletonClassNode.executeSingletonClass(null, object);
    }

    private void addCoreMethod(MethodDetails methodDetails) {
        final RubyContext context = objectClass.getContext();

        RubyBasicObject module;
        String fullName = methodDetails.getClassAnnotation().name();

        if (fullName.equals("main")) {
            module = getSingletonClass(context.getCoreLibrary().getMainObject());
        } else {
            module = objectClass;

            for (String moduleName : fullName.split("::")) {
                final RubyConstant constant = ModuleOperations.lookupConstant(context, LexicalScope.NONE, module, moduleName);

                if (constant == null) {
                    throw new RuntimeException(String.format("Module %s not found when adding core library", moduleName));
                }

                module = (RubyBasicObject) constant.getValue();
            }
        }

        assert module != null : fullName;

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
            if (!ModuleNodes.getModel(module).isOnlyAModule()) {
                System.err.println("WARNING: Using isModuleFunction on a Class for " + methodDetails.getIndicativeName());
            }
        }
        if (method.onSingleton() && method.constructor()) {
            System.err.println("WARNING: Either onSingleton or constructor for " + methodDetails.getIndicativeName());
        }

        final RubyRootNode rootNode = makeGenericMethod(context, methodDetails);

        if (method.isModuleFunction()) {
            addMethod(module, rootNode, names, Visibility.PRIVATE);
            addMethod(getSingletonClass(module), rootNode, names, Visibility.PUBLIC);
        } else if (method.onSingleton() || method.constructor()) {
            addMethod(getSingletonClass(module), rootNode, names, visibility);
        } else {
            addMethod(module, rootNode, names, visibility);
        }
    }

    private static void addMethod(RubyBasicObject module, RubyRootNode rootNode, List<String> names, final Visibility originalVisibility) {
        assert RubyGuards.isRubyModule(module);

        for (String name : names) {
            final RubyRootNode rootNodeCopy = NodeUtil.cloneNode(rootNode);

            Visibility visibility = originalVisibility;
            if (ModuleOperations.isMethodPrivateFromName(name)) {
                visibility = Visibility.PRIVATE;
            }

            final InternalMethod method = new InternalMethod(rootNodeCopy.getSharedMethodInfo(), name, module, visibility, false,
                    Truffle.getRuntime().createCallTarget(rootNodeCopy), null);

            ModuleNodes.getModel(module).addMethod(null, method.withVisibility(visibility).withName(name));
        }
    }

    private static RubyRootNode makeGenericMethod(RubyContext context, MethodDetails methodDetails) {
        final CoreMethod method = methodDetails.getMethodAnnotation();

        final CoreSourceSection sourceSection = new CoreSourceSection(methodDetails.getClassAnnotation().name(), method.names()[0]);

        final int required = method.required();
        final int optional = method.optional();

        final Arity arity = new Arity(required, optional, method.argumentsAsArray());

        final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, LexicalScope.NONE, arity, methodDetails.getIndicativeName(), false, null, true);

        final List<RubyNode> argumentsNodes = new ArrayList<>();

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
            RubyNode readArgumentNode = new ReadPreArgumentNode(context, sourceSection, n, MissingArgumentBehaviour.UNDEFINED);

            if (ArrayUtils.contains(method.lowerFixnumParameters(), n)) {
                readArgumentNode = FixnumLowerNodeGen.create(context, sourceSection, readArgumentNode);
            }

            if (ArrayUtils.contains(method.raiseIfFrozenParameters(), n)) {
                readArgumentNode = new RaiseIfFrozenNode(readArgumentNode);
            }

            argumentsNodes.add(readArgumentNode);
        }
        if (method.argumentsAsArray()) {
            argumentsNodes.add(new ReadRemainingArgumentsNode(context, sourceSection, arity.getPreRequired() + arity.getOptional()));
        }

        if (method.needsBlock()) {
            argumentsNodes.add(new ReadBlockNode(context, sourceSection, NotProvided.INSTANCE));
        }

        final RubyNode methodNode;
        final NodeFactory<? extends RubyNode> nodeFactory = methodDetails.getNodeFactory();
        List<List<Class<?>>> signatures = nodeFactory.getNodeSignatures();

        assert signatures.size() == 1;
        List<Class<?>> signature = signatures.get(0);

        if (signature.size() >= 3 && signature.get(2) == RubyNode[].class) {
            Object[] args = argumentsNodes.toArray(new RubyNode[argumentsNodes.size()]);
            methodNode = nodeFactory.createNode(context, sourceSection, args);
        } else {
            Object[] args = new Object[2 + argumentsNodes.size()];
            args[0] = context;
            args[1] = sourceSection;
            System.arraycopy(argumentsNodes.toArray(new RubyNode[argumentsNodes.size()]), 0, args, 2, argumentsNodes.size());
            methodNode = nodeFactory.createNode(args);
        }

        if (System.getenv("TRUFFLE_CHECK_AMBIGUOUS_OPTIONAL_ARGS") != null) {
            AmbiguousOptionalArgumentChecker.verifyNoAmbiguousOptionalArguments(methodDetails);
        }

        final CheckArityNode checkArity = new CheckArityNode(context, sourceSection, arity);
        RubyNode sequence = SequenceNode.sequence(context, sourceSection, checkArity, methodNode);

        if (method.returnsEnumeratorIfNoBlock()) {
            // TODO BF 3-18-2015 Handle multiple method names correctly
            sequence = new ReturnEnumeratorIfNoBlockNode(method.names()[0], sequence);
        }

        if (method.taintFromSelf() || method.taintFromParameter() != -1) {
            sequence = new TaintResultNode(method.taintFromSelf(),
                                           method.taintFromParameter(),
                                           sequence);
        }

        final ExceptionTranslatingNode exceptionTranslatingNode = new ExceptionTranslatingNode(context, sourceSection, sequence, method.unsupportedOperationBehavior());

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

    private static class AmbiguousOptionalArgumentChecker {

        private static final Method GET_PARAMETERS = checkParametersNamesAvailable();

        private static Method checkParametersNamesAvailable() {
            try {
                return Method.class.getMethod("getParameters");
            } catch (NoSuchMethodException | SecurityException e) {
                // Java 7 or could not find how to get names of method parameters
                System.err.println("Could not find method Method.getParameters()");
                return null;
            }
        }

        private static void verifyNoAmbiguousOptionalArguments(MethodDetails methodDetails) {
            if (GET_PARAMETERS == null) {
                System.exit(1);
            }

            try {
                verifyNoAmbiguousOptionalArgumentsWithReflection(methodDetails);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        private static void verifyNoAmbiguousOptionalArgumentsWithReflection(MethodDetails methodDetails) throws ReflectiveOperationException {
            boolean success = true;

            if (methodDetails.getMethodAnnotation().optional() > 0) {
                int opt = methodDetails.getMethodAnnotation().optional();
                if (methodDetails.getMethodAnnotation().needsBlock()) {
                    opt++;
                }

                Class<?> node = methodDetails.getNodeFactory().getNodeClass();

                for (int i = 1; i <= opt; i++) {
                    boolean unguardedObjectArgument = false;
                    StringBuilder errors = new StringBuilder();
                    for (Method method : node.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(Specialization.class)) {
                            // count from the end to ignore optional VirtualFrame in front.
                            Class<?>[] parameterTypes = method.getParameterTypes();
                            int n = parameterTypes.length - i;
                            Class<?> parameterType = parameterTypes[n];
                            Object[] parameters = (Object[]) GET_PARAMETERS.invoke(method);

                            Object parameter = parameters[n];
                            boolean isNamePresent = (boolean) parameter.getClass().getMethod("isNamePresent").invoke(parameter);
                            if (!isNamePresent) {
                                System.err.println("Method parameters names are not available for " + method);
                                System.exit(1);
                            }
                            String name = (String) parameter.getClass().getMethod("getName").invoke(parameter);

                            if (parameterType == Object.class && !name.startsWith("unused")) {
                                String[] guards = method.getAnnotation(Specialization.class).guards();
                                if (!isGuarded(name, guards)) {
                                    unguardedObjectArgument = true;
                                    errors.append("\"").append(name).append("\" in ").append(methodToString(method, parameterTypes, parameters)).append("\n");
                                }
                            }
                        }
                    }

                    if (unguardedObjectArgument) {
                        success = false;
                        System.err.println("Ambiguous optional argument in " + node.getCanonicalName() + ":");
                        System.err.println(errors);
                    }
                }
            }

            if (!success) {
                System.exit(1);
            }
        }

        private static boolean isGuarded(String name, String[] guards) {
            for (String guard : guards) {
                if (guard.equals("wasProvided(" + name + ")") ||
                        guard.equals("wasNotProvided(" + name + ")") ||
                        guard.equals("wasNotProvided(" + name + ") || isRubiniusUndefined(" + name + ")") ||
                        guard.equals("isNil(" + name + ")")) {
                    return true;
                }
            }
            return false;
        }

        private static String methodToString(Method method, Class<?>[] parameterTypes, Object[] parameters) throws ReflectiveOperationException {
            StringBuilder str = new StringBuilder();
            str.append(method.getName()).append("(");
            for (int i = 0; i < parameters.length; i++) {
                Object parameter = parameters[i];
                String name = (String) parameter.getClass().getMethod("getName").invoke(parameter);
                str.append(parameterTypes[i].getSimpleName()).append(" ").append(name);
                if (i < parameters.length - 1) {
                    str.append(", ");
                }
            }
            str.append(")");
            return str.toString();
        }
    }

}
