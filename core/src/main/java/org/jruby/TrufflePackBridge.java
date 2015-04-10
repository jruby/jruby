/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby;

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.pack.runtime.PackResult;

/**
 * Interface to the Truffle implementation of pack. The implementation is in
 * the {@code truffle} package, so is only available if the {@code truffle.jar}
 * is available on the classpath.
 * <p>
 * To instantiate the implementation, use reflection to look for
 * {@code org.jruby.truffle.pack.TrufflePackBridgeImpl}.
 * <p>
 * Using the Truffle implementation only makes sense on a JVM that has the
 * Graal compiler.
 */
public interface TrufflePackBridge {

    /**
     * Compile a format expression into code that can apply that expression
     * to an array.
     */
    Packer compileFormat(String format);

    /**
     * Represents a Truffle implementation of a particular pack format
     * expression. Both interpretation and dynamic compilation implementations
     * of the format are contained behind this interface.
     */
    interface Packer {

        PackResult pack(IRubyObject[] objects, int size);

    }
    
}
