/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
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
import jnr.posix.FileStat;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.SnippetNode;
import org.jruby.truffle.platform.UnsafeGroup;

public abstract class StatPrimitiveNodes {

    static FileStat getStat(DynamicObject rubyStat) {
        return Layouts.STAT.getStat(rubyStat);
    }

    @Primitive(name = "stat_allocate", unsafe = UnsafeGroup.IO)
    public static abstract class StatAllocatePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public DynamicObject allocate(DynamicObject classToAllocate) {
            return Layouts.STAT.createStat(coreLibrary().getStatFactory(), null);
        }

    }

    @Primitive(name = "stat_atime", unsafe = UnsafeGroup.IO)
    public static abstract class StatAtimePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public Object atime(
                VirtualFrame frame,
                DynamicObject rubyStat,
                @Cached("new()") SnippetNode snippetNode) {
            final long time = getStat(rubyStat).atime();
            return snippetNode.execute(frame, "Time.at(time)", "time", time);
        }

    }

    @Primitive(name = "stat_ctime", unsafe = UnsafeGroup.IO)
    public static abstract class StatCtimePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public Object ctime(
                VirtualFrame frame,
                DynamicObject rubyStat,
                @Cached("new()") SnippetNode snippetNode) {
            final long time = getStat(rubyStat).ctime();
            return snippetNode.execute(frame, "Time.at(time)", "time", time);
        }

    }

    @Primitive(name = "stat_mtime", unsafe = UnsafeGroup.IO)
    public static abstract class StatMtimePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public Object mtime(
                VirtualFrame frame,
                DynamicObject rubyStat,
                @Cached("new()") SnippetNode snippetNode) {
            final long time = getStat(rubyStat).mtime();
            return snippetNode.execute(frame, "Time.at(time)", "time", time);
        }

    }

    @Primitive(name = "stat_nlink", unsafe = UnsafeGroup.IO)
    public static abstract class NlinkPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public int nlink(DynamicObject rubyStat) {
            return getStat(rubyStat).nlink();
        }

    }

    @Primitive(name = "stat_rdev", unsafe = UnsafeGroup.IO)
    public static abstract class RdevPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public long rdev(DynamicObject rubyStat) {
            return getStat(rubyStat).rdev();
        }

    }

    @Primitive(name = "stat_blksize", unsafe = UnsafeGroup.IO)
    public static abstract class StatBlksizePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public long blksize(DynamicObject rubyStat) {
            return getStat(rubyStat).blockSize();
        }

    }

    @Primitive(name = "stat_blocks", unsafe = UnsafeGroup.IO)
    public static abstract class StatBlocksPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public long blocks(DynamicObject rubyStat) {
            return getStat(rubyStat).blocks();
        }

    }

    @Primitive(name = "stat_dev", unsafe = UnsafeGroup.IO)
    public static abstract class StatDevPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public long dev(DynamicObject rubyStat) {
            return getStat(rubyStat).dev();
        }

    }

    @Primitive(name = "stat_ino", unsafe = UnsafeGroup.IO)
    public static abstract class StatInoPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public long ino(DynamicObject rubyStat) {
            return getStat(rubyStat).ino();
        }

    }

    @Primitive(name = "stat_stat", unsafe = UnsafeGroup.IO)
    public static abstract class StatStatPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubyString(path)")
        public int stat(DynamicObject rubyStat, DynamicObject path) {
            final FileStat stat = posix().allocateStat();
            final int code = posix().stat(StringOperations.decodeUTF8(path), stat);

            if (code == 0) {
                Layouts.STAT.setStat(rubyStat, stat);
            }
            
            return code;
        }

        @Specialization(guards = "!isRubyString(path)")
        public Object stat(DynamicObject rubyStat, Object path) {
            return null;
        }

    }

    @Primitive(name = "stat_fstat", unsafe = UnsafeGroup.IO)
    public static abstract class StatFStatPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public int fstat(DynamicObject rubyStat, int fd) {
            final FileStat stat = posix().allocateStat();
            final int code = posix().fstat(fd, stat);

            if (code == 0) {
                Layouts.STAT.setStat(rubyStat, stat);
            }

            return code;
        }

    }

    @Primitive(name = "stat_lstat", unsafe = UnsafeGroup.IO)
    public static abstract class StatLStatPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubyString(path)")
        public int lstat(DynamicObject rubyStat, DynamicObject path) {
            final FileStat stat = posix().allocateStat();
            final int code = posix().lstat(RopeOperations.decodeRope(StringOperations.rope(path)), stat);

            if (code == 0) {
                Layouts.STAT.setStat(rubyStat, stat);
            }

            return code;
        }

        @Specialization(guards = "!isRubyString(path)")
        public Object stat(DynamicObject rubyStat, Object path) {
            return null;
        }

    }

    @Primitive(name = "stat_size", unsafe = UnsafeGroup.IO)
    public static abstract class StatSizePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public long size(DynamicObject rubyStat) {
            return getStat(rubyStat).st_size();
        }

    }

    @Primitive(name = "stat_mode", unsafe = UnsafeGroup.IO)
    public static abstract class StatModePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public int mode(DynamicObject rubyStat) {
            return getStat(rubyStat).mode();
        }

    }

    @Primitive(name = "stat_gid", unsafe = UnsafeGroup.IO)
    public static abstract class StatGIDPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public int gid(DynamicObject rubyStat) {
            return getStat(rubyStat).gid();
        }

    }

    @Primitive(name = "stat_uid", unsafe = UnsafeGroup.IO)
    public static abstract class StatUIDPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public int uid(DynamicObject rubyStat) {
            return getStat(rubyStat).uid();
        }

    }

}
