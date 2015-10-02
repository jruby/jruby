/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods;

import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.objects.SingletonClassNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;

/**
 * Declaration context for methods:
 * <ul>
 * <li>visibility</li>
 * <li>default definee (which module to define on)</li>
 * </ul>
 */
public class DeclarationContext {

    /** @see <a href="http://yugui.jp/articles/846">http://yugui.jp/articles/846</a> */
    private enum DefaultDefinee {
        LEXICAL_SCOPE,
        SINGLETON_CLASS,
        SELF
    }

    public final Visibility visibility;
    public final DefaultDefinee defaultDefinee;

    public DeclarationContext(Visibility visibility, DefaultDefinee defaultDefinee) {
        this.visibility = visibility;
        this.defaultDefinee = defaultDefinee;
    }

    public static Visibility findVisibility(Frame frame) {
        while (frame != null) {
            Visibility visibility = RubyArguments.getDeclarationContext(frame.getArguments()).visibility;
            if (visibility != null) {
                return visibility;
            }
            frame = RubyArguments.getDeclarationFrame(frame.getArguments());
        }

        throw new UnsupportedOperationException("No declaration frame with visibility found");
    }

    public DeclarationContext withVisibility(Visibility visibility) {
        if (visibility == this.visibility) {
            return this;
        } else {
            return new DeclarationContext(visibility, this.defaultDefinee);
        }
    }

    public DynamicObject getModuleToDefineMethods(VirtualFrame frame, RubyContext context, SingletonClassNode singletonClassNode) {
        switch (defaultDefinee) {
        case LEXICAL_SCOPE:
            return RubyArguments.getMethod(frame.getArguments()).getSharedMethodInfo().getLexicalScope().getLiveModule();
        case SINGLETON_CLASS:
            final Object self = RubyArguments.getSelf(frame.getArguments());
            return singletonClassNode.executeSingletonClass(self);
        case SELF:
            return (DynamicObject) RubyArguments.getSelf(frame.getArguments());
        default:
            throw new UnsupportedOperationException();
        }
    }

    public static final DeclarationContext MODULE = new DeclarationContext(Visibility.PUBLIC, DefaultDefinee.LEXICAL_SCOPE);
    public static final DeclarationContext METHOD = new DeclarationContext(null, DefaultDefinee.LEXICAL_SCOPE);
    public static final DeclarationContext BLOCK = new DeclarationContext(null, DefaultDefinee.LEXICAL_SCOPE);
    public static final DeclarationContext TOP_LEVEL = new DeclarationContext(Visibility.PRIVATE, DefaultDefinee.LEXICAL_SCOPE);
    public static final DeclarationContext INSTANCE_EVAL = new DeclarationContext(Visibility.PUBLIC, DefaultDefinee.SINGLETON_CLASS);
    public static final DeclarationContext CLASS_EVAL = new DeclarationContext(Visibility.PUBLIC, DefaultDefinee.SELF);

}
