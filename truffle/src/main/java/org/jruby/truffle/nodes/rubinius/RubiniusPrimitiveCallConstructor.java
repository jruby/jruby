/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import org.jruby.truffle.runtime.core.RubyModule;

public class RubiniusPrimitiveCallConstructor implements RubiniusPrimitiveConstructor {

    private final RubyModule module;
    private final String method;

    public RubiniusPrimitiveCallConstructor(RubyModule module, String method) {
        this.module = module;
        this.method = method;
    }

    public RubyModule getModule() {
        return module;
    }

    public String getMethod() {
        return method;
    }
}
