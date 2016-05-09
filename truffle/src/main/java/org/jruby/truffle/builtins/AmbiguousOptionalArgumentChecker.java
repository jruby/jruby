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

import com.oracle.truffle.api.dsl.Specialization;

import java.lang.reflect.Method;

public class AmbiguousOptionalArgumentChecker {

    private static final Method GET_PARAMETERS = checkParametersNamesAvailable();
    public static boolean SUCCESS = true;

    private static Method checkParametersNamesAvailable() {
        try {
            return Method.class.getMethod("getParameters");
        } catch (NoSuchMethodException | SecurityException e) {
            // Java 7 or could not find how to get names of method parameters
            System.err.println("Could not find method Method.getParameters()");
            System.exit(1);
            return null;
        }
    }

    public static void verifyNoAmbiguousOptionalArguments(CoreMethodNodeManager.MethodDetails methodDetails) {
        try {
            verifyNoAmbiguousOptionalArgumentsWithReflection(methodDetails);
        } catch (Exception e) {
            e.printStackTrace();
            SUCCESS = false;
        }
    }

    private static void verifyNoAmbiguousOptionalArgumentsWithReflection(CoreMethodNodeManager.MethodDetails methodDetails) throws ReflectiveOperationException {
        final CoreMethod methodAnnotation = methodDetails.getMethodAnnotation();
        if (methodAnnotation.optional() > 0 || methodAnnotation.needsBlock()) {
            int opt = methodAnnotation.optional();
            if (methodAnnotation.needsBlock()) {
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
                        if (methodAnnotation.rest()) {
                            n--; // ignore final Object[] argument
                        }
                        Class<?> parameterType = parameterTypes[n];
                        Object[] parameters = (Object[]) GET_PARAMETERS.invoke(method);

                        Object parameter = parameters[n];
                        boolean isNamePresent = (boolean) parameter.getClass().getMethod("isNamePresent").invoke(parameter);
                        if (!isNamePresent) {
                            System.err.println("Method parameters names are not available for " + method);
                            System.exit(1);
                        }
                        String name = (String) parameter.getClass().getMethod("getName").invoke(parameter);

                        if (parameterType == Object.class && !name.startsWith("unused") && !name.equals("maybeBlock")) {
                            String[] guards = method.getAnnotation(Specialization.class).guards();
                            if (!isGuarded(name, guards)) {
                                unguardedObjectArgument = true;
                                errors.append("\"").append(name).append("\" in ").append(methodToString(method, parameterTypes, parameters)).append("\n");
                            }
                        }
                    }
                }

                if (unguardedObjectArgument) {
                    SUCCESS = false;
                    System.err.println("Ambiguous optional argument in " + node.getCanonicalName() + ":");
                    System.err.println(errors);
                }
            }
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
