/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.*;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.*;
import org.jruby.truffle.runtime.methods.*;

/**
 * Represents the Ruby {@code Proc} class.
 */
public class RubyProc extends RubyObject {

    /**
     * The class from which we create the object that is {@code Proc}. A subclass of
     * {@link RubyClass} so that we can override {@link #newInstance} and allocate a
     * {@link RubyProc} rather than a normal {@link RubyBasicObject}.
     */
    public static class RubyProcClass extends RubyClass {

        public RubyProcClass(RubyClass objectClass) {
            super(null, objectClass, "Proc");
        }

        @Override
        public RubyBasicObject newInstance() {
            return new RubyProc(this);
        }

    }

    public static enum Type {
        PROC, LAMBDA
    }

    @CompilationFinal private Type type;
    @CompilationFinal private Object selfCapturedInScope;
    @CompilationFinal private RubyProc blockCapturedInScope;
    @CompilationFinal private RubyMethod method;

    public RubyProc(RubyClass procClass) {
        super(procClass);
    }

    public RubyProc(RubyClass procClass, Type type, Object selfCapturedInScope, RubyProc blockCapturedInScope, RubyMethod method) {
        super(procClass);
        initialize(type, selfCapturedInScope, blockCapturedInScope, method);
    }

    public void initialize(Type setType, Object selfCapturedInScope, RubyProc blockCapturedInScope, RubyMethod setMethod) {
        assert RubyContext.shouldObjectBeVisible(selfCapturedInScope);

        type = setType;
        this.selfCapturedInScope = selfCapturedInScope;
        this.blockCapturedInScope = blockCapturedInScope;
        method = setMethod;
    }

    public Object call(Object... args) {
        return callWithModifiedSelf(selfCapturedInScope, args);
    }

    public Object callWithModifiedSelf(Object modifiedSelf, Object... args) {
        RubyNode.notDesignedForCompilation();

        assert modifiedSelf != null;
        assert args != null;

        try {
            return method.call(modifiedSelf, blockCapturedInScope, args);
        } catch (ReturnException e) {
            switch (type) {
                case PROC:
                    throw e;
                case LAMBDA:
                    return e.getValue();
                default:
                    throw new IllegalStateException();
            }
        }
    }

    public Type getType() {
        return type;
    }

    public Object getSelfCapturedInScope() {
        return selfCapturedInScope;
    }

    public RubyProc getBlockCapturedInScope() {
        return blockCapturedInScope;
    }

    public RubyMethod getMethod() {
        return method;
    }

}
