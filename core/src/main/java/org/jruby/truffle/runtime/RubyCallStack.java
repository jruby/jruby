/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameInstance;

public abstract class RubyCallStack {

    public static void dump() {
        CompilerAsserts.neverPartOfCompilation();

        System.err.println("call stack ----------");

        System.err.println("    in " + Truffle.getRuntime().getCurrentFrame().getCallTarget());

        for (FrameInstance frame : Truffle.getRuntime().getStackTrace()) {
            System.err.println("  from " + frame.getCallNode().getEncapsulatingSourceSection());
        }

        System.err.println("---------------------");
    }

}
