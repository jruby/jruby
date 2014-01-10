/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.ext.truffelize;

import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class TruffelizeKernel {

    @JRubyMethod(module = true, rest = true)
    public static IRubyObject truffelize(IRubyObject self, IRubyObject[] args) {
        for (IRubyObject arg : args) {
            if (!(arg instanceof RubyString || arg instanceof RubySymbol)) {
                throw new UnsupportedOperationException(arg.getClass().toString());
            }

            TruffelizeLibrary.truffelize(self.getSingletonClass(), arg.asJavaString());
        }

        return self;
    }

}
