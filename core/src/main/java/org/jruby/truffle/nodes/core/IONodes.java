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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyFile;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@CoreClass(name = "IO")
public abstract class IONodes {

    @CoreMethod(names = "open", onSingleton = true, needsBlock = true, required = 2)
    public abstract static class OpenNode extends YieldingCoreMethodNode {

        public OpenNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public OpenNode(OpenNode prev) {
            super(prev);
        }

        @Specialization
        public Object open(VirtualFrame frame, RubyString fileName, RubyString mode, RubyProc block) {
            notDesignedForCompilation();

            final RubyFile file = RubyFile.open(getContext(), fileName.toString(), mode.toString());

            if (block != null) {
                try {
                    yield(frame, block, file);
                } finally {
                    file.close();
                }
            }

            return file;
        }

    }

    @CoreMethod(names = "readlines", onSingleton = true, required = 1)
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

            try(final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file.toString())))) {
                
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
