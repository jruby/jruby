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

public class NewOptions {

    public final String[] ARGUMENTS;

    public final boolean EXCEPTIONS_PRINT_JAVA;

    NewOptions(OptionsBuilder builder) {
        ARGUMENTS = builder.getOrDefault(OptionsCatalogue.ARGUMENTS);
        EXCEPTIONS_PRINT_JAVA = builder.getOrDefault(OptionsCatalogue.EXCEPTIONS_PRINT_JAVA);
    }

}
