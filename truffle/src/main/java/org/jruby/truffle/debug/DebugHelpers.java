/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.debug;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import org.jruby.truffle.RubyContext;

public abstract class DebugHelpers {

    public static Object eval(String code, Object... arguments) {
        return eval(RubyContext.getLatestInstance(), code, arguments);
    }

    public static Object eval(RubyContext context, String code, Object... arguments) {
        CompilerAsserts.neverPartOfCompilation();
        final FrameInstance currentFrameInstance = Truffle.getRuntime().getCurrentFrame();
        final Frame currentFrame = currentFrameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE, true);
        return context.getCodeLoader().inline(null, currentFrame, code, arguments);
    }

}
