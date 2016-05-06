/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.extra;

import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.platform.graal.Graal;

@CoreClass("Truffle::Graal")
public abstract class TruffleGraalNodes {

    @CoreMethod(names = "assert_constant", onSingleton = true, required = 1)
    public abstract static class AssertConstantNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject assertConstant(Object value) {
            throw new RaiseException(coreExceptions().runtimeErrorNotConstant(this));
        }

    }

    @CoreMethod(names = "assert_not_compiled", onSingleton = true)
    public abstract static class AssertNotCompiledNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject assertNotCompiled() {
            throw new RaiseException(coreExceptions().runtimeErrorCompiled(this));
        }

    }

    @CoreMethod(names = "graal?", onSingleton = true)
    public abstract static class GraalNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public boolean graal() {
            return Graal.isGraal();
        }

    }

    @CoreMethod(names = "substrate?", onSingleton = true)
    public abstract static class SubstrateNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public boolean substrate() {
            return TruffleOptions.AOT;
        }

    }

    @CoreMethod(names = "version", onSingleton = true)
    public abstract static class GraalVersionNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject graalVersion() {
            return createString(StringOperations.encodeRope(Graal.getVersion(), UTF8Encoding.INSTANCE));
        }

    }

}
