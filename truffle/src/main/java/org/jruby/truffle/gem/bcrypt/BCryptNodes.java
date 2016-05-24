/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.gem.bcrypt;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.string.StringOperations;

@CoreClass("Truffle::Gem::BCrypt")
public abstract class BCryptNodes {

    @CoreMethod(names = "hashpw", required = 2, onSingleton = true)
    public abstract static class HashPassword extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = { "isRubyString(secret)", "isRubyString(salt)" })
        public Object hashpw(DynamicObject secret, DynamicObject salt) {
            final String result = BCrypt.hashpw(
                    StringOperations.getString(getContext(), secret),
                    StringOperations.getString(getContext(), salt));
            return StringOperations.createString(
                    getContext(),
                    StringOperations.createRope(result, USASCIIEncoding.INSTANCE));
        }
    }

    @CoreMethod(names = "gensalt", required = 1, onSingleton = true)
    public abstract static class GenerateSalt extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public Object gensalt(int cost) {
            return StringOperations.createString(
                    getContext(),
                    StringOperations.createRope(BCrypt.gensalt(cost), USASCIIEncoding.INSTANCE));
        }
    }
}
