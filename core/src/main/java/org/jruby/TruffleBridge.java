/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby;

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

public interface TruffleBridge {

    enum BacktraceFormatter {
        MRI,
        DEBUG,
        IMPL_DEBUG
    }

    void init();

    Object execute(Object self, org.jruby.ast.RootNode rootNode);

    Object toTruffle(IRubyObject object);

    void shutdown();
    
}
