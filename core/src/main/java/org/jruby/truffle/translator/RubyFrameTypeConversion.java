/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.translator;

import com.oracle.truffle.api.impl.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.RubyNilClass;

public final class RubyFrameTypeConversion extends DefaultFrameTypeConversion {

    private final RubyNilClass nil;

    public RubyFrameTypeConversion(RubyNilClass nil) {
        this.nil = nil;
    }

    @Override
    public Object getDefaultValue() {
        return nil;
    }

}
