/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language;

/**
 * Represents a value that was not provided by the user, such as optional arguments to a core library node.
 */
public final class NotProvided {

    public static final NotProvided INSTANCE = new NotProvided();

    private NotProvided() {
    }

}
