/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.ToSNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.subsystems.ThreadManager;
import org.jruby.util.ByteList;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@CoreClass(name = "IO")
public abstract class IONodes {

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

            try(final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file.toString()), StandardCharsets.UTF_8))) {
                
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

    @CoreMethod(names = "write", needsSelf = false, required = 1)
    @NodeChild(value = "string")
    public abstract static class WriteNode extends RubyNode {

        public WriteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public WriteNode(WriteNode prev) {
            super(prev);
        }

        @CreateCast("string") public RubyNode callToS(RubyNode other) {
            return ToSNodeFactory.create(getContext(), getSourceSection(), other);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public int write(RubyString string) {
            final ByteList byteList = string.getByteList();

            final int offset = byteList.getBegin();
            final int length = byteList.getRealSize();
            final byte[] bytes = byteList.getUnsafeBytes();

            // TODO (nirvdrum 17-Feb-15) This shouldn't always just write to STDOUT, but that's the only use case we're supporting currently.
            final PrintStream stream = getContext().getRuntime().getInstanceConfig().getOutput();

            getContext().getThreadManager().runUntilResult(new ThreadManager.BlockingActionWithoutGlobalLock<Boolean>() {
                @Override
                public Boolean block() throws InterruptedException {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    write(stream, bytes, offset, length);
                    return SUCCESS;
                }
            });

            return length;
        }

        @CompilerDirectives.TruffleBoundary
        private void write(PrintStream stream, byte[] bytes, int offset, int length) throws InterruptedException {
            stream.write(bytes, offset, length);
        }

    }

}
