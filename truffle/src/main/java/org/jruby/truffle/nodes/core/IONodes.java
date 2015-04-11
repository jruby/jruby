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

import org.jcodings.specific.ASCIIEncoding;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.ToSNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyFile;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.subsystems.ThreadManager;
import org.jruby.util.ByteList;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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

    @CoreMethod(names = "binread", onSingleton = true, required = 1, optional = 2)
    public abstract static class BinReadNode extends CoreMethodNode {

        public BinReadNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object binaryRead(RubyString file, UndefinedPlaceholder size, UndefinedPlaceholder offset) {
            notDesignedForCompilation();
            return readFile(getContext(), file, null, null, this);
        }

        @Specialization
        public Object binaryRead(RubyString file, int size, UndefinedPlaceholder offset) {
            notDesignedForCompilation();
            return readFile(getContext(), file, size, null, this);
        }

        @Specialization
        public Object binaryRead(RubyString file, int size, int offset) {
            notDesignedForCompilation();
            return readFile(getContext(), file, size, offset, this);
        }

        private static Object readFile(RubyContext context, RubyString rubyString, Integer requestedLength, Integer offset, BinReadNode node) {
            final File file = new File(RubyFile.expandPath(context, rubyString.toString()));
            final int length;
            if (offset != null) {
                if (offset < 0) {
                    throw new RaiseException(context.getCoreLibrary().invalidArgumentError(Integer.toString(offset), node));
                }
            } else {
                offset = 0;
            }
            final int fileLength = longToInt(file.length());
            if (requestedLength != null) {
                if (requestedLength < 0) {
                    throw new RaiseException(context.getCoreLibrary().argumentError("length cannot be negative", node));
                } else if (requestedLength == 0) {
                    return context.makeString("", ASCIIEncoding.INSTANCE);
                }
                if (offset > fileLength) {
                    return context.getCoreLibrary().getNilObject();
                } else if ((offset + requestedLength) > fileLength) {
                    length = fileLength - offset;
                } else {
                    length = requestedLength;
                }
            } else {
                length = fileLength;
            }
            try (InputStream in = new BufferedInputStream(new FileInputStream(file), length)) {
                final ByteList byteList = new ByteList(readWithOffset(in, length, offset), ASCIIEncoding.INSTANCE);
                return context.makeString(byteList);
            } catch (EOFException e) {
                return context.getCoreLibrary().getNilObject();
            } catch (FileNotFoundException e) {
                throw new RaiseException(context.getCoreLibrary().fileNotFoundError(rubyString.toString(), node));
            } catch (IOException e) {
                throw new RaiseException(context.getCoreLibrary().ioError(rubyString.toString(), node));
            }
        }

        private static byte[] readWithOffset(InputStream input, int length, int offset) throws IOException {
            int read = 0;
            int n;
            final byte[] bytes = new byte[length];
            while (offset > 0) {
                offset -= input.skip(offset);
            }
            for (int start = 0; read < length; read += n) {
                n = input.read(bytes, start + read, length - read);
                if (n == -1) {
                    if (read == 0) {
                        throw new EOFException();
                    }
                    break;
                }
            }
            return bytes;
        }

        private static int longToInt(long lon) {
            int integer = (int) lon;
            if ((long) integer != lon) {
                throw new IllegalArgumentException("size too large to fit in int");
            }
            return integer;
        }

    }

    @CoreMethod(names = "write", needsSelf = false, required = 1)
    @NodeChild(value = "string")
    public abstract static class WriteNode extends RubyNode {

        public WriteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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

    @CoreMethod(names = { "tty?", "isatty" })
    public abstract static class IsATTYNode extends CoreMethodNode {

        public IsATTYNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean isatty(Object self) {
            notDesignedForCompilation();

            Object stdout = getContext().getCoreLibrary().getObjectClass().getConstants().get("STDOUT").getValue();
            if (self != stdout) {
                throw new UnsupportedOperationException("Only STDOUT.tty? supported");
            }

            // TODO (eregon 8-Apr-15) get the actual fd
            final FileDescriptor fd = FileDescriptor.out;

            return getContext().getRuntime().getPosix().isatty(fd);
        }

    }

}
