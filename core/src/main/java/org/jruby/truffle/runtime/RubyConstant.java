/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyModule;

public class RubyConstant {

    private final RubyModule declaringModule;
    private final Object value;
    private boolean isPrivate;

    public RubyConstant(RubyModule declaringModule, Object value, boolean isPrivate) {
        this.declaringModule = declaringModule;
        this.value = value;
        this.isPrivate = isPrivate;
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
        if (!isPrivate) {
            return true;
        }

        final LexicalScope topLexicalScope = lexicalScope;

        // Look in lexical scope
        if (lexicalScope != null) {
            while (lexicalScope != context.getRootLexicalScope()) {
                if (lexicalScope.getLiveModule() == declaringModule) {
                    return true;
                }
                lexicalScope = lexicalScope.getParent();
            }
        }

        // Look in included modules
        if (module instanceof RubyClass) {
            for (RubyModule included : module.includedModules()) {
                if (included == declaringModule) {
                    return true;
                }
            }
        }

        // Look in Object if there is no qualifier (just CONST, neither Mod::CONST nor ::CONST).
        if (topLexicalScope != null && topLexicalScope.getLiveModule() == module && // This is a guess, we should have that info from AST
            context.getCoreLibrary().getObjectClass() == declaringModule) {
            return true;
        }

        return false;
    }

}
