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

public class RubyUnboundMethod extends RubyBasicObject {

    private InternalMethod method;

    public RubyUnboundMethod(RubyClass rubyClass, InternalMethod method) {
        super(rubyClass);
        this.method = method;
    }

    public InternalMethod getMethod() {
        return method;
    }
}
