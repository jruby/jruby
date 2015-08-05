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
    private final RubyBasicObject liveModule;

    public LexicalScope(LexicalScope parent, RubyBasicObject liveModule) {
        assert liveModule == null || RubyGuards.isRubyModule(liveModule);
        this.parent = parent;
        this.liveModule = liveModule;
    }

    public LexicalScope getParent() {
        return parent;
    }

    public RubyBasicObject getLiveModule() {
        return liveModule;
    }

    @Override
    public String toString() {
        return " :: " + liveModule + (parent == null ? "" : parent.toString());
    }
}
