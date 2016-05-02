/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.extra;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.Layouts;

@CoreClass(name = "Truffle::Attachments::Internal")
public abstract class AttachmentsInternalNodes {

    @CoreMethod(names = "attach", onSingleton = true, required = 2, needsBlock = true)
    public abstract static class AttachNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubyString(file)")
        public DynamicObject attach(DynamicObject file, int line, DynamicObject block) {
            return handle(getContext().getAttachmentsManager().attach(file.toString(), line, block));
        }

    }

    @CoreMethod(names = "detach", onSingleton = true, required = 1)
    public abstract static class DetachNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isHandle(handle)")
        public DynamicObject detach(DynamicObject handle) {
            ((EventBinding<?>) Layouts.HANDLE.getObject(handle)).dispose();
            return getContext().getCoreLibrary().getNilObject();
        }

    }

}
