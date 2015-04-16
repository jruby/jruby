/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.util.Dir;

public abstract class IOPrimitiveNodes {

    @RubiniusPrimitive(name = "io_fnmatch", needsSelf = false)
    public static abstract class IOFNMatchPrimitiveNode extends RubiniusPrimitiveNode {

        public IOFNMatchPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public boolean fnmatch(RubyString pattern, RubyString path, int flags) {
            return Dir.fnmatch(pattern.getByteList().getUnsafeBytes(),
                    pattern.getByteList().getBegin(),
                    pattern.getByteList().getBegin() + pattern.getByteList().getRealSize(),
                    path.getByteList().getUnsafeBytes(),
                    path.getByteList().getBegin(),
                    path.getByteList().getBegin() + path.getByteList().getRealSize(),
                    flags) != Dir.FNM_NOMATCH;
        }

    }

}
