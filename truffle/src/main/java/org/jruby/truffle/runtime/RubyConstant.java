/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

import com.oracle.truffle.api.CompilerAsserts;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyModule;

public class RubyConstant {

    private final RubyModule declaringModule;
    private final Object value;
    private boolean isPrivate;
    private boolean autoload;

    public RubyConstant(RubyModule declaringModule, Object value, boolean isPrivate, boolean autoload) {
        this.declaringModule = declaringModule;
        this.value = value;
        this.isPrivate = isPrivate;
        this.autoload = autoload;
    }

    public Object getValue() {
        return value;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public boolean isVisibleTo(RubyContext context, LexicalScope lexicalScope, RubyModule module) {
        CompilerAsserts.neverPartOfCompilation();
        assert lexicalScope == null || lexicalScope.getLiveModule() == module;

        if (!isPrivate) {
            return true;
        }

        // Look in lexical scope
        if (lexicalScope != null) {
            while (lexicalScope != context.getRootLexicalScope()) {
                if (lexicalScope.getLiveModule() == declaringModule) {
                    return true;
                }
                lexicalScope = lexicalScope.getParent();
            }
        }

        // Look in ancestors
        if (module instanceof RubyClass) {
            for (RubyModule included : module.parentAncestors()) {
                if (included == declaringModule) {
                    return true;
                }
            }
        }

        // Allow Object constants if looking with lexical scope.
        if (lexicalScope != null && context.getCoreLibrary().getObjectClass() == declaringModule) {
            return true;
        }

        return false;
    }

    public boolean isAutoload() {
        return autoload;
    }

}
