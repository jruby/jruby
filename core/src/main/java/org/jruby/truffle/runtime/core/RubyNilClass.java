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

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.*;

/**
 * Represents the Ruby {@code NilClass} class.
 */
public class RubyNilClass extends RubyObject {

    public RubyNilClass(RubyClass rubyClass) {
        super(rubyClass);
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean isTrue() {
        return false;
    }

    @Override
    public boolean hasClassAsSingleton() {
        return true;
    }

}
