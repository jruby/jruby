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

    @CoreMethod(names = "absolute_path", onSingleton = true, required = 1)
    public abstract static class AbsolutePathNode extends CoreMethodNode {

        public AbsolutePathNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AbsolutePathNode(AbsolutePathNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString absolutePath(RubyString path) {
            notDesignedForCompilation();

            String absolute = new File(path.toString()).getAbsolutePath();

            if (getContext().isRunningOnWindows()) {
                absolute = absolute.replace('\\', '/');
            }

            return getContext().makeString(absolute);
        }

    }

    @CoreMethod(names = "basename", onSingleton = true, required = 1, optional = 1)
    public abstract static class BasenameNode extends CoreMethodNode {

        public BasenameNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BasenameNode(BasenameNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString basename(RubyString path, @SuppressWarnings("unused") UndefinedPlaceholder extension) {
            notDesignedForCompilation();

            return getContext().makeString(new File(path.toString()).getName());
        }

        @Specialization
        public RubyString basename(RubyString path, RubyString extension) {
            notDesignedForCompilation();

            final String extensionAsString = extension.toString();
            final String name = new File(path.toString()).getName();
            final String basename;

            if (extensionAsString.equals(".*") && name.indexOf('.') != -1) {
                basename = name.substring(0, name.lastIndexOf('.'));
            } else if (name.endsWith(extensionAsString)) {
                basename = name.substring(0, name.lastIndexOf(extensionAsString));
            } else {
                basename = name;
            }

            return getContext().makeString(basename);
        }
    }

    @CoreMethod(names = "close")
    public abstract static class CloseNode extends CoreMethodNode {

        public CloseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CloseNode(CloseNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass close(RubyFile file) {
            notDesignedForCompilation();

            file.close();
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = { "delete", "unlink" }, onSingleton = true, required = 1)
    public abstract static class DeleteNode extends CoreMethodNode {

        public DeleteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DeleteNode(DeleteNode prev) {
            super(prev);
        }

        @Specialization
        public int delete(RubyString file) {
            notDesignedForCompilation();

            if (!new File(file.toString()).delete()) {
                // TODO(CS, 12-Jan-15) handle failure
                throw new UnsupportedOperationException();
            }

            return 1;
        }

    }

    @CoreMethod(names = "directory?", onSingleton = true, required = 1)
    public abstract static class DirectoryNode extends CoreMethodNode {

        public DirectoryNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DirectoryNode(DirectoryNode prev) {
            super(prev);
        }

        @Specialization
        public boolean directory(RubyString path) {
            notDesignedForCompilation();

            return new File(path.toString()).isDirectory();
        }

    }

    @CoreMethod(names = "dirname", onSingleton = true, required = 1)
    public abstract static class DirnameNode extends CoreMethodNode {

        public DirnameNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DirnameNode(DirnameNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString dirname(RubyString path) {
            notDesignedForCompilation();

            final String parent = new File(path.toString()).getParent();

            if (parent == null) {
                return getContext().makeString(".");
            } else {
                return getContext().makeString(parent);
            }
        }

    }

    @CoreMethod(names = "each_line", needsBlock = true)
    public abstract static class EachLineNode extends YieldingCoreMethodNode {

        public EachLineNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EachLineNode(EachLineNode prev) {
            super(prev);
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

            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "executable?", onSingleton = true, required = 1)
    public abstract static class ExecutableNode extends CoreMethodNode {

        public ExecutableNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ExecutableNode(ExecutableNode prev) {
            super(prev);
        }

        @Specialization
        public boolean executable(RubyString path) {
            notDesignedForCompilation();

            return new File(path.toString()).canExecute();
        }

    }

    @CoreMethod(names = {"exist?", "exists?"}, onSingleton = true, required = 1)
    @NodeChild(value = "path")
    public abstract static class ExistsNode extends RubyNode {

        public ExistsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ExistsNode(ExistsNode prev) {
            super(prev);
        }

        @CreateCast("path") public RubyNode coercePathToString(RubyNode path) {
            return ToStrNodeFactory.create(getContext(), getSourceSection(), path);
        }

        @Specialization
        public boolean exists(RubyString path) {
            notDesignedForCompilation();

            return new File(path.toString()).exists();
        }

    }

    @CoreMethod(names = "expand_path", onSingleton = true, required = 1, optional = 1)
    public abstract static class ExpandPathNode extends CoreMethodNode {

        public ExpandPathNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ExpandPathNode(ExpandPathNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString expandPath(RubyString path, @SuppressWarnings("unused") UndefinedPlaceholder dir) {
            return getContext().makeString(RubyFile.expandPath(getContext(), path.toString()));
        }

        @Specialization
        public RubyString expandPath(RubyString path, RubyString dir) {
            notDesignedForCompilation();

            return getContext().makeString(RubyFile.expandPath(path.toString(), dir.toString()));
        }

    }

    @CoreMethod(names = "file?", onSingleton = true, required = 1)
    public abstract static class FileNode extends CoreMethodNode {

        public FileNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FileNode(FileNode prev) {
            super(prev);
        }

        @Specialization
        public boolean file(RubyString path) {
            notDesignedForCompilation();

            return new File(path.toString()).isFile();
        }

    }

    @CoreMethod(names = "join", onSingleton = true, argumentsAsArray = true)
    public abstract static class JoinNode extends CoreMethodNode {

        public JoinNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public JoinNode(JoinNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString join(Object[] parts) {
            notDesignedForCompilation();

            final StringBuilder builder = new StringBuilder();
            join(builder, parts);
            return getContext().makeString(builder.toString());
        }

        @TruffleBoundary
        public static void join(StringBuilder builder, Object[] parts) {
            notDesignedForCompilation();

            for (int n = 0; n < parts.length; n++) {
                if (n > 0) {
                    builder.append("/");
                }

                if (parts[n] instanceof RubyArray) {
                    join(builder, ((RubyArray) parts[n]).slowToArray());
                } else {
                    builder.append(parts[n].toString());
                }
            }
        }
    }

    @CoreMethod(names = "open", onSingleton = true, needsBlock = true, required = 2)
    public abstract static class OpenNode extends YieldingCoreMethodNode {

        public OpenNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public OpenNode(OpenNode prev) {
            super(prev);
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

    @CoreMethod(names = "path", onSingleton = true, required = 1)
    public abstract static class PathNode extends CoreMethodNode {

        public PathNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PathNode(PathNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString path(RubyString path) {
            notDesignedForCompilation();

            return getContext().makeString(path.toString());
        }

    }

    @CoreMethod(names = "puts", required = 1)
    public abstract static class PutsNode extends CoreMethodNode {

        public PutsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PutsNode(PutsNode prev) {
            super(prev);
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

            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "read", onSingleton = true, needsSelf = false, required = 1)
    public abstract static class ReadFunctionNode extends CoreMethodNode {

        public ReadFunctionNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ReadFunctionNode(ReadFunctionNode prev) {
            super(prev);
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

        public ReadNode(ReadNode prev) {
            super(prev);
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

    @CoreMethod(names = "readable?", onSingleton = true, needsSelf = false, required = 1)
    public abstract static class ReadableQueryNode extends CoreMethodNode {

        public ReadableQueryNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ReadableQueryNode(ReadableQueryNode prev) {
            super(prev);
        }

        @Specialization
        public boolean isReadable(RubyString file) {
            notDesignedForCompilation();

            return new File(file.toString()).canRead();
        }

    }

    @CoreMethod(names = "size?", onSingleton = true, required = 1)
    public abstract static class SizeNode extends CoreMethodNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SizeNode(SizeNode prev) {
            super(prev);
        }

        @Specialization
        public Object read(RubyString file) {
            notDesignedForCompilation();

            final File f = new File(file.toString());

            if (!f.exists()) {
                return getContext().getCoreLibrary().getNilObject();
            }

            final long size = f.length();

            if (size == 0) {
                return getContext().getCoreLibrary().getNilObject();
            }

            return size;
        }

    }

    @CoreMethod(names = "symlink?", onSingleton = true, required = 1)
    public abstract static class SymlinkQueryNode extends CoreMethodNode {

        public SymlinkQueryNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SymlinkQueryNode(SymlinkQueryNode prev) {
            super(prev);
        }

        @Specialization
        public boolean symlinkQuery(RubyString fileName) {
            notDesignedForCompilation();

            try {
                // Note: We can't use file.exists() to check whether the symlink
                // exists or not, because that method returns false for existing
                // but broken symlink. So, we try without the existence check,
                // but in the try-catch block.
                // MRI behavior: symlink? on broken symlink should return true.
                FileStat stat = getContext().getRuntime().getPosix().allocateStat();

                if (getContext().getRuntime().getPosix().lstat(fileName.toString(), stat) < 0) {
                    stat = null;
                }

                return (stat != null && stat.isSymlink());
            } catch (SecurityException re) {
                return false;
            }
        }
    }

    @CoreMethod(names = "write", required = 1)
    public abstract static class WriteNode extends CoreMethodNode {

        public WriteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public WriteNode(WriteNode prev) {
            super(prev);
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

            return getContext().getCoreLibrary().getNilObject();
        }

    }

}
