/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyString;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@CoreClass(name = "IO")
public abstract class IONodes {

    @CoreMethod(names = "readlines", isModuleMethod = true, needsSelf = false, minArgs = 1, maxArgs = 1)
    public abstract static class ReadLinesNode extends CoreMethodNode {

        public ReadLinesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ReadLinesNode(ReadLinesNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray readLines(RubyString file) {
            notDesignedForCompilation();

            final List<Object> lines = new ArrayList<>();

            try {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file.toString())));

                while (true) {
                    final String line = reader.readLine();

                    if (line == null) {
                        break;
                    }

                    lines.add(getContext().makeString(line));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), lines.toArray(new Object[lines.size()]));
        }

    }

}
