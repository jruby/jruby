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
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.internal.runtime.methods.UndefinedMethod;

public class TruffelizeModule {

    @JRubyMethod(rest = true)
    public static RubyModule truffelize(IRubyObject self, IRubyObject[] args) {
        final RubyModule module = (RubyModule) self;

        for (IRubyObject arg : args) {
            final String methodName = arg.asJavaString();

            TruffelizeLibrary.truffelize(module, methodName);

            if (!(module.getSingletonClass().searchMethod(methodName) instanceof UndefinedMethod)) {
                TruffelizeLibrary.truffelize(module.getSingletonClass(), methodName);
            }
        }

        return module;
    }

}
