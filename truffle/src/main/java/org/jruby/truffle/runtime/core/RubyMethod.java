/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import org.jruby.truffle.runtime.methods.InternalMethod;

import com.oracle.truffle.api.interop.ForeignAccessFactory;

public class RubyMethod extends RubyBasicObject {

    private final Object receiver;
    private final InternalMethod method;

    public RubyMethod(RubyClass rubyClass, Object object, InternalMethod method) {
        super(rubyClass);
        this.receiver = object;
        this.method = method;
    }

    public InternalMethod getMethod() {
        return method;
    }

    public Object getReceiver() {
        return receiver;
    }
    
    @Override
    public ForeignAccessFactory getForeignAccessFactory() {
        return new RubyMethodForeignAccessFactory(getContext());
    }
}
