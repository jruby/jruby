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
import com.oracle.truffle.api.CompilerDirectives;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.methods.RubyMethod;

import java.math.BigInteger;

public abstract class DebugOperations {

    public static String inspect(RubyContext context, Object object) {
        CompilerAsserts.neverPartOfCompilation();

        final Object inspected = send(context, object, "inspect", null);

        if (inspected == null) {
            return String.format("%s@%x", object.getClass().getSimpleName(), object.hashCode());
        }

        return inspected.toString();
    }

    public static Object send(RubyContext context, Object object, String methodName, RubyProc block, Object... arguments) {
        CompilerAsserts.neverPartOfCompilation();

        final RubyBasicObject rubyObject = context.getCoreLibrary().box(object);

        final RubyMethod method = ModuleOperations.lookupMethod(rubyObject.getMetaClass(), methodName);

        if (method == null) {
            return null;
        }

        return method.getCallTarget().call(
                RubyArguments.pack(method, method.getDeclarationFrame(), rubyObject, block, arguments));
    }

    public static void panic(RubyContext context, RubyNode node, String message) {
        CompilerDirectives.transferToInterpreter();

        System.err.println("=========================== JRuby+Truffle Debug Report ========================");

        if (message != null) {
            System.err.println();
            System.err.println("Stopped because: " + message);
        }

        System.err.println();
        System.err.println("=============================== Ruby Bracktrace ===============================");
        System.err.println();

        for (String line : Backtrace.PANIC_FORMATTER.format(context, null, RubyCallStack.getBacktrace(node))) {
            System.err.println(line);
        }

        System.err.println();
        System.err.println("=============================== Java Backtrace ================================");
        System.err.println();

        new Exception().printStackTrace();

        System.err.println();
        System.err.println("===============================================================================");

        System.exit(1);
    }
}
