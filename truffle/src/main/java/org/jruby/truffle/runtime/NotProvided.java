/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

/**
 * The {@link NotProvided} instance represents an argument which was <i>not provided</i>. 
 * This is necessary as we need to differentiate based on the number of passed arguments
 * and there is not a single default value that fits for omitted arguments.
 */
public final class NotProvided {

    public static final NotProvided INSTANCE = new NotProvided();

    private NotProvided() {
    }

}
