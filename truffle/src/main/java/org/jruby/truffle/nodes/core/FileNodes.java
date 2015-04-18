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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import jnr.posix.FileStat;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.coerce.ToStrNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.subsystems.ThreadManager.BlockingActionWithoutGlobalLock;
import org.jruby.truffle.runtime.util.FileUtils;
import org.jruby.util.ByteList;

import java.io.*;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@CoreClass(name = "File")
public abstract class FileNodes {

    @CoreMethod(names = "close")
    public abstract static class CloseNode extends CoreMethodNode {

        public CloseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyNilClass close(RubyFile file) {
            notDesignedForCompilation();

            file.close();
            return nil();
        }

    }

    @CoreMethod(names = "each_line", needsBlock = true)
    public abstract static class EachLineNode extends YieldingCoreMethodNode {

        public EachLineNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyNilClass eachLine(VirtualFrame frame, RubyFile file, RubyProc block) {
            notDesignedForCompilation();

            final RubyContext context = getContext();

            // TODO(cs): this buffered reader may consume too much

            final BufferedReader lineReader = new BufferedReader(file.getReader());

            while (true) {
                String line;

                try {
                    line = lineReader.readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if (line == null) {
                    break;
                }

                yield(frame, block, context.makeString(line));
            }

            return nil();
        }

    }

    @CoreMethod(names = "open", onSingleton = true, needsBlock = true, required = 1, optional = 1)
    public abstract static class OpenNode extends YieldingCoreMethodNode {

        public OpenNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object open(RubyString fileName, UndefinedPlaceholder mode, UndefinedPlaceholder block) {
            return open(fileName, getContext().makeString("r"), block);
        }

        @Specialization
        public Object open(VirtualFrame frame, RubyString fileName, UndefinedPlaceholder mode, RubyProc block) {
            return open(frame, fileName, getContext().makeString("r"), block);
        }

        @Specialization
        public Object open(RubyString fileName, RubyString mode, UndefinedPlaceholder block) {
            notDesignedForCompilation();

            return RubyFile.open(getContext(), fileName.toString(), mode.toString());
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

    @CoreMethod(names = "puts", required = 1)
    public abstract static class PutsNode extends CoreMethodNode {

        public PutsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyNilClass puts(RubyFile file, RubyString string) {
            notDesignedForCompilation();

            try {
                final Writer writer = file.getWriter();
                writer.write(string.toString());
                writer.write("\n");
                writer.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return nil();
        }

    }

    @CoreMethod(names = "read", onSingleton = true, needsSelf = false, required = 1)
    public abstract static class ReadFunctionNode extends CoreMethodNode {

        public ReadFunctionNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString read(RubyString file) {
            notDesignedForCompilation();

            return new RubyString(getContext().getCoreLibrary().getStringClass(),
                    new ByteList(FileUtils.readAllBytesInterruptedly(getContext(), file.toString())));
        }

    }

    @CoreMethod(names = "read")
    public abstract static class ReadNode extends CoreMethodNode {

        public ReadNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString read(RubyFile file) {
            notDesignedForCompilation();

            try {
                final Reader reader = file.getReader();

                final StringBuilder builder = new StringBuilder();

                while (true) {
                    final int c = reader.read();

                    if (c == -1) {
                        break;
                    }

                    builder.append((char) c);
                }

                return getContext().makeString(builder.toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    @CoreMethod(names = "write", required = 1)
    public abstract static class WriteNode extends CoreMethodNode {

        public WriteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyNilClass write(RubyFile file, RubyString string) {
            notDesignedForCompilation();

            try {
                final Writer writer = file.getWriter();
                writer.write(string.toString());
                writer.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return nil();
        }

    }

}
