/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.methods;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.core.module.ModuleOperations;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.Visibility;

public class GetCurrentVisibilityNode extends RubyNode {

    @Override
    public Visibility execute(VirtualFrame frame) {
        return DeclarationContext.findVisibility(frame);
    }

    @TruffleBoundary
    public static Visibility getVisibilityFromNameAndFrame(String name, Frame frame) {
        if (ModuleOperations.isMethodPrivateFromName(name)) {
            return Visibility.PRIVATE;
        } else {
            return DeclarationContext.findVisibility(frame);
        }
    }

}
