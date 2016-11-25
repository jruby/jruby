/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.options;

// This file would be automatically generated from the list of options in the text file.

public class OptionsCatalogue {

    public static final OptionDescription ARGUMENTS = new StringArrayOptionDescription("arguments", "Foo bar baz", new String[0]);

    public static final OptionDescription EXCEPTIONS_PRINT_JAVA = new BooleanOptionDescription("exceptions.print_java", "Foo baz bar", false);

    public static OptionDescription fromName(String name) {
        switch (name) {
            case "arguments":
                return ARGUMENTS;
            case "exceptions.print_java":
                return EXCEPTIONS_PRINT_JAVA;
            default:
                return null;
        }
    }

}
