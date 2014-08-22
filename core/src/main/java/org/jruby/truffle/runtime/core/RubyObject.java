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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.Ruby;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;

/**
 * Represents the Ruby {@code Object} class.
 */
public class RubyObject extends RubyBasicObject {

    public boolean frozen = false;

    public RubyObject(RubyClass rubyClass) {
        super(rubyClass);
    }

    public void checkFrozen(Node currentNode) {
        if (frozen) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getRubyClass().getContext().getCoreLibrary().frozenError(getRubyClass().getName().toLowerCase(), currentNode));
        }
    }

    public static String checkInstanceVariableName(RubyContext context, String name, RubyNode currentNode) {
        RubyNode.notDesignedForCompilation();

        if (!name.startsWith("@")) {
            throw new RaiseException(context.getCoreLibrary().nameErrorInstanceNameNotAllowable(name, currentNode));
        }

        return name;
    }

    public static String checkClassVariableName(RubyContext context, String name, RubyNode currentNode) {
        RubyNode.notDesignedForCompilation();

        if (!name.startsWith("@@")) {
            throw new RaiseException(context.getCoreLibrary().nameErrorInstanceNameNotAllowable(name, currentNode));
        }

        return name;
    }

    public Ruby getJRubyRuntime(){
        return getRubyClass().getContext().getRuntime();
    }

}
