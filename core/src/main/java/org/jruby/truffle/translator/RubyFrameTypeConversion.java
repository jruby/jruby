/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
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

public final class RubyFrameTypeConversion extends DefaultFrameTypeConversion {

    private static final RubyFrameTypeConversion INSTANCE = new RubyFrameTypeConversion();

    private RubyFrameTypeConversion() {
    }

    @Override
    public Object getDefaultValue() {
        return NilPlaceholder.INSTANCE;
    }

    public static RubyFrameTypeConversion getInstance() {
        return INSTANCE;
    }
}
