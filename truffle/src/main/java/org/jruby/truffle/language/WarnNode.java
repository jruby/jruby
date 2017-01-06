/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;

public class WarnNode extends RubyBaseNode {

    @Child private CallDispatchHeadNode warnMethod = CallDispatchHeadNode.createMethodCall();

    public Object execute(VirtualFrame frame, String... arguments) {
        final String warningMessage = concatArgumentsToString(arguments);
        final DynamicObject warningString = createString(warningMessage.getBytes(), UTF8Encoding.INSTANCE);
        return warnMethod.call(frame, getContext().getCoreLibrary().getKernelModule(), "warn", warningString);
    }

    @TruffleBoundary
    private String concatArgumentsToString(String... arguments) {
        return String.join("", arguments);
    }
}
