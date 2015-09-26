/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.Encoding;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.util.ByteList;

public abstract class EncodingOperations {

    public static Encoding getEncoding(RubyContext context, DynamicObject rubyEncoding) {
        Encoding encoding = Layouts.ENCODING.getEncoding(rubyEncoding);

        if (encoding == null) {
            final ByteList name = Layouts.ENCODING.getName(rubyEncoding);
            encoding = context.getRuntime().getEncodingService().loadEncoding(name);
            Layouts.ENCODING.setEncoding(rubyEncoding, encoding);
        }

        return encoding;
    }

}
