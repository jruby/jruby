/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.runtime.core.RubyBasicObject;

public class LexicalScope {
    public static final LexicalScope NONE = null;

    private final LexicalScope parent;
    private final RubyBasicObject module;

    public LexicalScope(LexicalScope parent, RubyBasicObject module) {
        assert RubyGuards.isRubyModule(module);
        this.parent = parent;
        this.module = module;
    }

    public LexicalScope getParent() {
        return parent;
    }

    public RubyBasicObject getModule() {
        return module;
    }

    @Override
    public String toString() {
        return " :: " + module + (parent == null ? "" : parent.toString());
    }
}
