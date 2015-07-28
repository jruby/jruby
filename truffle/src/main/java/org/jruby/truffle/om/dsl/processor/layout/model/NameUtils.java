/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.om.dsl.processor.layout.model;

public abstract class NameUtils {

    public static String identifierToConstant(String name) {
        final StringBuilder result = new StringBuilder();

        for (int n = 0; n < name.length(); n++) {
            if (Character.isUpperCase(name.charAt(n))) {
                result.append('_');
            }

            result.append(Character.toUpperCase(name.charAt(n)));
        }

        return result.toString();
    }

    public static String constantToIdentifier(String name) {
        final StringBuilder result = new StringBuilder();

        for (int n = 0; n < name.length(); n++) {
            if (name.charAt(n) == '_') {
                n++;
                result.append(Character.toUpperCase(name.charAt(n)));
            } else {
                result.append(Character.toLowerCase(name.charAt(n)));
            }
        }

        return result.toString();
    }

}
