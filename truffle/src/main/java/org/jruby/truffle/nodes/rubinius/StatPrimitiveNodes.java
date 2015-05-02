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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.source.SourceSection;
import jnr.posix.FileStat;
import org.jruby.RubyEncoding;
import org.jruby.truffle.nodes.objectstorage.ReadHeadObjectFieldNode;
import org.jruby.truffle.nodes.objectstorage.WriteHeadObjectFieldNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyString;

public abstract class StatPrimitiveNodes {

    public static final HiddenKey STAT_IDENTIFIER = new HiddenKey("stat");

    @RubiniusPrimitive(name = "stat_atime")
    public static abstract class StatAtimePrimitiveNode extends StatReadPrimitiveNode {

        public StatAtimePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object atime(VirtualFrame frame, RubyBasicObject rubyStat) {
            final long time = getStat(rubyStat).atime();
            return ruby(frame, "Time.at(time)", "time", time);
        }

    }

    @RubiniusPrimitive(name = "stat_ctime")
    public static abstract class StatCtimePrimitiveNode extends StatReadPrimitiveNode {

        public StatCtimePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object ctime(VirtualFrame frame, RubyBasicObject rubyStat) {
            final long time = getStat(rubyStat).ctime();
            return ruby(frame, "Time.at(time)", "time", time);
        }

    }

    @RubiniusPrimitive(name = "stat_mtime")
    public static abstract class StatMtimePrimitiveNode extends StatReadPrimitiveNode {

        public StatMtimePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object mtime(VirtualFrame frame, RubyBasicObject rubyStat) {
            final long time = getStat(rubyStat).mtime();
            return ruby(frame, "Time.at(time)", "time", time);
        }

    }

    @RubiniusPrimitive(name = "stat_nlink")
    public static abstract class NlinkPrimitiveNode extends StatReadPrimitiveNode {

        public NlinkPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int nlink(RubyBasicObject rubyStat) {
            return getStat(rubyStat).nlink();
        }

    }

    @RubiniusPrimitive(name = "stat_rdev")
    public static abstract class RdevPrimitiveNode extends StatReadPrimitiveNode {

        public RdevPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long rdev(RubyBasicObject rubyStat) {
            return getStat(rubyStat).rdev();
        }

    }

    @RubiniusPrimitive(name = "stat_blksize")
    public static abstract class StatBlksizePrimitiveNode extends StatReadPrimitiveNode {

        public StatBlksizePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long blksize(RubyBasicObject rubyStat) {
            return getStat(rubyStat).blockSize();
        }

    }

    @RubiniusPrimitive(name = "stat_blocks")
    public static abstract class StatBlocksPrimitiveNode extends StatReadPrimitiveNode {

        public StatBlocksPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long blocks(RubyBasicObject rubyStat) {
            return getStat(rubyStat).blocks();
        }

    }

    @RubiniusPrimitive(name = "stat_dev")
    public static abstract class StatDevPrimitiveNode extends StatReadPrimitiveNode {

        public StatDevPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long dev(RubyBasicObject rubyStat) {
            return getStat(rubyStat).dev();
        }

    }

    @RubiniusPrimitive(name = "stat_ino")
    public static abstract class StatInoPrimitiveNode extends StatReadPrimitiveNode {

        public StatInoPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long ino(RubyBasicObject rubyStat) {
            return getStat(rubyStat).ino();
        }

    }

    @RubiniusPrimitive(name = "stat_stat")
    public static abstract class StatStatPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private WriteHeadObjectFieldNode writeStatNode;

        public StatStatPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            writeStatNode = new WriteHeadObjectFieldNode(STAT_IDENTIFIER);
        }

        @Specialization
        public int stat(RubyBasicObject rubyStat, RubyString path) {
            final FileStat stat = posix().allocateStat();
            final String pathString = RubyEncoding.decodeUTF8(path.getByteList().getUnsafeBytes(), path.getByteList().getBegin(), path.getByteList().getRealSize());
            final int code = posix().stat(pathString, stat);

            if (code == 0) {
                writeStatNode.execute(rubyStat, stat);
            }
            
            return code;
        }

        @Specialization(guards = "!isRubyString(path)")
        public Object stat(RubyBasicObject rubyStat, Object path) {
            return null;
        }

    }

    @RubiniusPrimitive(name = "stat_fstat")
    public static abstract class StatFStatPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private WriteHeadObjectFieldNode writeStatNode;

        public StatFStatPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            writeStatNode = new WriteHeadObjectFieldNode(STAT_IDENTIFIER);
        }

        @Specialization
        public int fstat(RubyBasicObject rubyStat, int fd) {
            final FileStat stat = posix().allocateStat();
            final int code = posix().fstat(fd, stat);

            if (code == 0) {
                writeStatNode.execute(rubyStat, stat);
            }

            return code;
        }

    }

    @RubiniusPrimitive(name = "stat_lstat")
    public static abstract class StatLStatPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private WriteHeadObjectFieldNode writeStatNode;

        public StatLStatPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            writeStatNode = new WriteHeadObjectFieldNode(STAT_IDENTIFIER);
        }

        @Specialization
        public int lstat(RubyBasicObject rubyStat, RubyString path) {
            final FileStat stat = posix().allocateStat();
            final int code = posix().lstat(path.toString(), stat);

            if (code == 0) {
                writeStatNode.execute(rubyStat, stat);
            }

            return code;
        }

        @Specialization(guards = "!isRubyString(path)")
        public Object stat(RubyBasicObject rubyStat, Object path) {
            return null;
        }

    }

    public static abstract class StatReadPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private ReadHeadObjectFieldNode readStatNode;

        public StatReadPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readStatNode = new ReadHeadObjectFieldNode(STAT_IDENTIFIER);
        }

        public FileStat getStat(RubyBasicObject rubyStat) {
            return (FileStat) readStatNode.execute(rubyStat);
        }

    }

    @RubiniusPrimitive(name = "stat_size")
    public static abstract class StatSizePrimitiveNode extends StatReadPrimitiveNode {

        public StatSizePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long size(RubyBasicObject rubyStat) {
            return getStat(rubyStat).st_size();
        }

    }

    @RubiniusPrimitive(name = "stat_mode")
    public static abstract class StatModePrimitiveNode extends StatReadPrimitiveNode {

        public StatModePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int mode(RubyBasicObject rubyStat) {
            return getStat(rubyStat).mode();
        }

    }

    @RubiniusPrimitive(name = "stat_gid")
    public static abstract class StatGIDPrimitiveNode extends StatReadPrimitiveNode {

        public StatGIDPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int gid(RubyBasicObject rubyStat) {
            return getStat(rubyStat).gid();
        }

    }

    @RubiniusPrimitive(name = "stat_uid")
    public static abstract class StatUIDPrimitiveNode extends StatReadPrimitiveNode {

        public StatUIDPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int uid(RubyBasicObject rubyStat) {
            return getStat(rubyStat).uid();
        }

    }

}
