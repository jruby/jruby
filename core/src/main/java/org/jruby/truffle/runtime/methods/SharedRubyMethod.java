/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.methods;

import com.oracle.truffle.api.SourceSection;

/**
 * {@link RubyMethod} objects are copied as properties such as visibility are changed. {@link SharedRubyMethod} stores
 * the state that does not change, such as where the method was defined.
 */
public class SharedRubyMethod {

    private final SourceSection sourceSection;

    public SharedRubyMethod(SourceSection sourceSection) {
        this.sourceSection = sourceSection;
    }

    public SourceSection getSourceSection() {
        return sourceSection;
    }

}
