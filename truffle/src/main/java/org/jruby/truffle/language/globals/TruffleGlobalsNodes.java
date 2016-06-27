/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.globals;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;

@CoreClass("Truffle::Globals")
public abstract class TruffleGlobalsNodes {

    @CoreMethod(names = "permanently_invalidate", onSingleton = true, required = 1)
    public abstract static class PermanentlyInvalidateNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubySymbol(name)")
        public DynamicObject permanentlyInvalidate(DynamicObject name) {
            getContext().getCoreLibrary().getGlobalVariables().getStorage(name.toString()).permanentlyInvalidate();
            return nil();
        }

    }

}
