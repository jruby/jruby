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
import org.jcodings.Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamAdapter extends OutputStream {

    private final RubyContext context;
    private final DynamicObject object;
    private final Encoding encoding;

    public OutputStreamAdapter(RubyContext context, DynamicObject object, Encoding encoding) {
        this.context = context;
        this.object = object;
        this.encoding = encoding;
    }

    @Override
    public void write(int bite) throws IOException {
        context.send(object, "write", null, Layouts.STRING.createString(context.getCoreLibrary().getStringFactory(),
                StringOperations.ropeFromByteList(new ByteList(new byte[]{(byte) bite}, encoding), StringSupport.CR_VALID)));
    }

}
