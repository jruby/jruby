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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

@CoreClass(name = "Dir")
public abstract class DirNodes {

    @CoreMethod(names = "chdir", onSingleton = true, needsBlock = true, required = 1)
    public abstract static class ChdirNode extends YieldingCoreMethodNode {

        public ChdirNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ChdirNode(ChdirNode prev) {
            super(prev);
        }

        @Specialization
        public Object chdir(VirtualFrame frame, RubyString path, RubyProc block) {
            notDesignedForCompilation();

            final RubyContext context = getContext();

            final String previous = context.getRuntime().getCurrentDirectory();
            context.getRuntime().setCurrentDirectory(path.toString());

            if (block != null) {
                try {
                    return yield(frame, block, path);
                } finally {
                    context.getRuntime().setCurrentDirectory(previous);
                }
            } else {
                return 0;
            }
        }

    }

    @CoreMethod(names = { "delete", "rmdir", "unlink" }, onSingleton = true, optional = 1)
    public abstract static class DeleteNode extends CoreMethodNode {

        public DeleteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DeleteNode(DeleteNode prev) {
            super(prev);
        }

        @Specialization
        public int delete(RubyString path) {
            notDesignedForCompilation();

            File dir = new File(path.toString());
            if (!dir.isDirectory()) {
                throw new UnsupportedOperationException(path.toString());
            }

            if (!dir.delete()) {
                // TODO(CS, 12-Jan-15) handle failure
                throw new UnsupportedOperationException();
            }

            return 0;
        }

    }

    @CoreMethod(names = {"exist?", "exists?"}, onSingleton = true, optional = 1)
    public abstract static class ExistsNode extends CoreMethodNode {

        public ExistsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ExistsNode(ExistsNode prev) {
            super(prev);
        }

        @Specialization
        public boolean exists(RubyString path) {
            notDesignedForCompilation();

            return new File(path.toString()).isDirectory();
        }

    }

    @CoreMethod(names = {"glob", "[]"}, onSingleton = true, required = 1)
    public abstract static class GlobNode extends CoreMethodNode {

        public GlobNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GlobNode(GlobNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray glob(RubyString glob) {
            return glob(getContext(), glob.toString());
        }

        @TruffleBoundary
        private static RubyArray glob(final RubyContext context, String glob) {
            /*
             * Globbing is quite complicated. We've implemented a subset of the functionality that
             * satisfies MSpec, but it will likely break for anyone else.
             */

            String absoluteGlob;

            if (!glob.startsWith("/")) {
                absoluteGlob = new File(".", glob).getAbsolutePath();
            } else {
                absoluteGlob = glob;
            }

            // Get the first star

            final int firstStar = absoluteGlob.indexOf('*');
            assert firstStar >= 0;

            // Walk back from that to the first / before that star

            int prefixLength = firstStar;

            while (prefixLength > 0 && absoluteGlob.charAt(prefixLength) == File.separatorChar) {
                prefixLength--;
            }

            final String prefix = absoluteGlob.substring(0, prefixLength - 1);

            // Glob patterns must always use '/', even on Windows.
            final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + absoluteGlob.substring(prefixLength).replace('\\', '/'));

            final RubyArray array = new RubyArray(context.getCoreLibrary().getArrayClass());

            try {
                Files.walkFileTree(FileSystems.getDefault().getPath(prefix), new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (matcher.matches(file)) {
                            array.slowPush(context.makeString(file.toString()));
                        }

                        return FileVisitResult.CONTINUE;
                    }

                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return array;
        }

    }

    @CoreMethod(names = "mkdir", needsSelf = false, onSingleton = true, required = 1)
    public abstract static class MkdirNode extends CoreMethodNode {

        public MkdirNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MkdirNode(MkdirNode prev) {
            super(prev);
        }

        @Specialization
        public int mkdir(RubyString path) {
            notDesignedForCompilation();

            if (!new File(path.toString()).mkdir()) {
                // TODO(CS, 12-Jan-15) handle failure
                throw new UnsupportedOperationException();
            }

            return 0;
        }

    }

    @CoreMethod(names = {"pwd", "getwd"}, onSingleton = true)
    public abstract static class PwdNode extends CoreMethodNode {

        public PwdNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PwdNode(PwdNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString pwd() {
            notDesignedForCompilation();

            return getContext().makeString(getContext().getRuntime().getCurrentDirectory());
        }

    }

}
