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
import org.jruby.truffle.Log;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class AmbiguousOptionalArgumentChecker {

    public static boolean SUCCESS = true;
    private static boolean AVAILABLE = true;

    public static void verifyNoAmbiguousOptionalArguments(CoreMethodNodeManager.MethodDetails methodDetails) {
        if (!AVAILABLE) {
            return;
        }
        try {
            verifyNoAmbiguousOptionalArgumentsWithReflection(methodDetails);
        } catch (Exception e) {
            e.printStackTrace();
            SUCCESS = false;
        }
    }

    private static void verifyNoAmbiguousOptionalArgumentsWithReflection(CoreMethodNodeManager.MethodDetails methodDetails) {
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
                        Parameter[] parameters = method.getParameters();

                        Parameter parameter = parameters[n];
                        boolean isNamePresent = parameter.isNamePresent();
                        if (!isNamePresent) {
                            AVAILABLE = SUCCESS = false;
                            Log.LOGGER.warning("Method parameters names are not available for " + method);
                            return;
                        }
                        String name = parameter.getName();

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
                    Log.LOGGER.warning("Ambiguous optional argument in " + node.getCanonicalName() + ":\n" + errors);
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

    private static String methodToString(Method method, Class<?>[] parameterTypes, Parameter[] parameters) {
        StringBuilder str = new StringBuilder();
        str.append(method.getName()).append("(");
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String name = parameter.getName();
            str.append(parameterTypes[i].getSimpleName()).append(" ").append(name);
            if (i < parameters.length - 1) {
                str.append(", ");
            }
        }
        str.append(")");
        return str.toString();
    }
}
