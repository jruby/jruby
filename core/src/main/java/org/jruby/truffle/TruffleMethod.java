/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle;

import com.oracle.truffle.api.*;

import org.jruby.RubyModule;
import org.jruby.TruffleBridge;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.runtime.RubyArguments;

public class TruffleMethod extends DynamicMethod {

    private final CallTarget callTarget;

    public TruffleMethod(DynamicMethod originalMethod, CallTarget callTarget) {
        super(originalMethod.getImplementationClass(), originalMethod.getVisibility(), originalMethod.getCallConfig(), originalMethod.getName());

        assert callTarget != null;

        this.callTarget = callTarget;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        final TruffleBridge bridge = context.getRuntime().getTruffleBridge();

        final Object[] truffleArgumentsObjects = new Object[args.length];

        for (int n = 0; n < args.length; n++) {
            truffleArgumentsObjects[n] = bridge.toTruffle(args[n]);
        }

        final Arguments truffleArguments = new RubyArguments(null, bridge.toTruffle(self), null, truffleArgumentsObjects);

        return bridge.toJRuby(callTarget.call(truffleArguments));
    }

    @Override
    public DynamicMethod dup() {
        throw new UnsupportedOperationException();
    }
}
