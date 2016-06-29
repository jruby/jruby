/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.adapaters;

import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.RubyContext;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamAdapter extends InputStream {

    private final RubyContext context;
    private final DynamicObject object;

    public InputStreamAdapter(RubyContext context, DynamicObject object) {
        this.context = context;
        this.object = object;
    }

    @Override
    public int read() throws IOException {
        final Object result = context.send(object, "getbyte", null);

        if (result == context.getCoreLibrary().getNilObject()) {
            return -1;
        }

        return (int) result;
    }
}
