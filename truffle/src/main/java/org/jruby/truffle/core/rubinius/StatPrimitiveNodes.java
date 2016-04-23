/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.rubinius;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.source.SourceSection;
import jnr.posix.FileStat;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.SnippetNode;
import org.jruby.truffle.language.objects.ReadObjectFieldNode;
import org.jruby.truffle.language.objects.ReadObjectFieldNodeGen;
import org.jruby.truffle.language.objects.WriteObjectFieldNode;
import org.jruby.truffle.language.objects.WriteObjectFieldNodeGen;
import org.jruby.truffle.platform.UnsafeGroup;

public abstract class StatPrimitiveNodes {

    public static final HiddenKey STAT_IDENTIFIER = new HiddenKey("stat");

    @RubiniusPrimitive(name = "stat_atime", unsafe = UnsafeGroup.IO)
    public static abstract class StatAtimePrimitiveNode extends StatReadPrimitiveNode {

        @Specialization
        public Object atime(
                VirtualFrame frame,
                DynamicObject rubyStat,
                @Cached("new()")SnippetNode snippetNode) {
            final long time = getStat(rubyStat).atime();
            return snippetNode.execute(frame, "Time.at(time)", "time", time);
        }

    }

    @RubiniusPrimitive(name = "stat_ctime", unsafe = UnsafeGroup.IO)
    public static abstract class StatCtimePrimitiveNode extends StatReadPrimitiveNode {

        @Specialization
        public Object ctime(
                VirtualFrame frame,
                DynamicObject rubyStat,
                @Cached("new()")SnippetNode snippetNode) {
            final long time = getStat(rubyStat).ctime();
            return snippetNode.execute(frame, "Time.at(time)", "time", time);
        }

    }

    @RubiniusPrimitive(name = "stat_mtime", unsafe = UnsafeGroup.IO)
    public static abstract class StatMtimePrimitiveNode extends StatReadPrimitiveNode {

        @Specialization
        public Object mtime(
                VirtualFrame frame,
                DynamicObject rubyStat,
                @Cached("new()")SnippetNode snippetNode) {
            final long time = getStat(rubyStat).mtime();
            return snippetNode.execute(frame, "Time.at(time)", "time", time);
        }

    }

    @RubiniusPrimitive(name = "stat_nlink", unsafe = UnsafeGroup.IO)
    public static abstract class NlinkPrimitiveNode extends StatReadPrimitiveNode {

        @Specialization
        public int nlink(DynamicObject rubyStat) {
            return getStat(rubyStat).nlink();
        }

    }

    @RubiniusPrimitive(name = "stat_rdev", unsafe = UnsafeGroup.IO)
    public static abstract class RdevPrimitiveNode extends StatReadPrimitiveNode {

        @Specialization
        public long rdev(DynamicObject rubyStat) {
            return getStat(rubyStat).rdev();
        }

    }

    @RubiniusPrimitive(name = "stat_blksize", unsafe = UnsafeGroup.IO)
    public static abstract class StatBlksizePrimitiveNode extends StatReadPrimitiveNode {

        @Specialization
        public long blksize(DynamicObject rubyStat) {
            return getStat(rubyStat).blockSize();
        }

    }

    @RubiniusPrimitive(name = "stat_blocks", unsafe = UnsafeGroup.IO)
    public static abstract class StatBlocksPrimitiveNode extends StatReadPrimitiveNode {

        @Specialization
        public long blocks(DynamicObject rubyStat) {
            return getStat(rubyStat).blocks();
        }

    }

    @RubiniusPrimitive(name = "stat_dev", unsafe = UnsafeGroup.IO)
    public static abstract class StatDevPrimitiveNode extends StatReadPrimitiveNode {

        @Specialization
        public long dev(DynamicObject rubyStat) {
            return getStat(rubyStat).dev();
        }

    }

    @RubiniusPrimitive(name = "stat_ino", unsafe = UnsafeGroup.IO)
    public static abstract class StatInoPrimitiveNode extends StatReadPrimitiveNode {

        @Specialization
        public long ino(DynamicObject rubyStat) {
            return getStat(rubyStat).ino();
        }

    }

    @RubiniusPrimitive(name = "stat_stat", unsafe = UnsafeGroup.IO)
    public static abstract class StatStatPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Child private WriteObjectFieldNode writeStatNode;

        public StatStatPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            writeStatNode = WriteObjectFieldNodeGen.create(STAT_IDENTIFIER);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(path)")
        public int stat(DynamicObject rubyStat, DynamicObject path) {
            final FileStat stat = posix().allocateStat();
            final int code = posix().stat(StringOperations.decodeUTF8(path), stat);

            if (code == 0) {
                writeStatNode.execute(rubyStat, stat);
            }
            
            return code;
        }

        @Specialization(guards = "!isRubyString(path)")
        public Object stat(DynamicObject rubyStat, Object path) {
            return null;
        }

    }

    @RubiniusPrimitive(name = "stat_fstat", unsafe = UnsafeGroup.IO)
    public static abstract class StatFStatPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Child private WriteObjectFieldNode writeStatNode;

        public StatFStatPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            writeStatNode = WriteObjectFieldNodeGen.create(STAT_IDENTIFIER);
        }

        @TruffleBoundary
        @Specialization
        public int fstat(DynamicObject rubyStat, int fd) {
            final FileStat stat = posix().allocateStat();
            final int code = posix().fstat(fd, stat);

            if (code == 0) {
                writeStatNode.execute(rubyStat, stat);
            }

            return code;
        }

    }

    @RubiniusPrimitive(name = "stat_lstat", unsafe = UnsafeGroup.IO)
    public static abstract class StatLStatPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Child private WriteObjectFieldNode writeStatNode;

        public StatLStatPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            writeStatNode = WriteObjectFieldNodeGen.create(STAT_IDENTIFIER);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(path)")
        public int lstat(DynamicObject rubyStat, DynamicObject path) {
            final FileStat stat = posix().allocateStat();
            final int code = posix().lstat(path.toString(), stat);

            if (code == 0) {
                writeStatNode.execute(rubyStat, stat);
            }

            return code;
        }

        @Specialization(guards = "!isRubyString(path)")
        public Object stat(DynamicObject rubyStat, Object path) {
            return null;
        }

    }

    public static abstract class StatReadPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Child private ReadObjectFieldNode readStatNode;

        public StatReadPrimitiveNode() {
            this(null, null);
        }

        public StatReadPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readStatNode = ReadObjectFieldNodeGen.create(STAT_IDENTIFIER, null);
        }

        public FileStat getStat(DynamicObject rubyStat) {
            return (FileStat) readStatNode.execute(rubyStat);
        }

    }

    @RubiniusPrimitive(name = "stat_size", unsafe = UnsafeGroup.IO)
    public static abstract class StatSizePrimitiveNode extends StatReadPrimitiveNode {

        @Specialization
        public long size(DynamicObject rubyStat) {
            return getStat(rubyStat).st_size();
        }

    }

    @RubiniusPrimitive(name = "stat_mode", unsafe = UnsafeGroup.IO)
    public static abstract class StatModePrimitiveNode extends StatReadPrimitiveNode {

        @Specialization
        public int mode(DynamicObject rubyStat) {
            return getStat(rubyStat).mode();
        }

    }

    @RubiniusPrimitive(name = "stat_gid", unsafe = UnsafeGroup.IO)
    public static abstract class StatGIDPrimitiveNode extends StatReadPrimitiveNode {

        @Specialization
        public int gid(DynamicObject rubyStat) {
            return getStat(rubyStat).gid();
        }

    }

    @RubiniusPrimitive(name = "stat_uid", unsafe = UnsafeGroup.IO)
    public static abstract class StatUIDPrimitiveNode extends StatReadPrimitiveNode {

        @Specialization
        public int uid(DynamicObject rubyStat) {
            return getStat(rubyStat).uid();
        }

    }

}
