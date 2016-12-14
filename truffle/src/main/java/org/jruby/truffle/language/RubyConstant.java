/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;

public class RubyConstant {

    private final DynamicObject declaringModule;
    private final Object value;
    private final boolean isPrivate;
    private final boolean autoload;
    private final boolean isDeprecated;

    public RubyConstant(DynamicObject declaringModule, Object value, boolean isPrivate, boolean autoload, boolean isDeprecated) {
        assert RubyGuards.isRubyModule(declaringModule);
        this.declaringModule = declaringModule;
        this.value = value;
        this.isPrivate = isPrivate;
        this.autoload = autoload;
        this.isDeprecated = isDeprecated;
    }

    public DynamicObject getDeclaringModule() {
        return declaringModule;
    }

    public Object getValue() {
        return value;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public boolean isDeprecated() {
        return isDeprecated;
    }

    public RubyConstant withPrivate(boolean isPrivate) {
        if (isPrivate == this.isPrivate) {
            return this;
        } else {
            return new RubyConstant(declaringModule, value, isPrivate, autoload, isDeprecated);
        }
    }

    public RubyConstant withDeprecated() {
        if (this.isDeprecated()) {
            return this;
        } else {
            return new RubyConstant(declaringModule, value, isPrivate, autoload, true);
        }
    }

    public boolean isVisibleTo(RubyContext context, LexicalScope lexicalScope, DynamicObject module) {
        CompilerAsserts.neverPartOfCompilation();

        assert RubyGuards.isRubyModule(module);
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
        if (RubyGuards.isRubyClass(module)) {
            for (DynamicObject included : Layouts.MODULE.getFields(module).parentAncestors()) {
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
