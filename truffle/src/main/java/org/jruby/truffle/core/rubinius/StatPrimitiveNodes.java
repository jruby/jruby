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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.source.SourceSection;
import jnr.posix.FileStat;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.objects.ReadObjectFieldNode;
import org.jruby.truffle.language.objects.ReadObjectFieldNodeGen;
import org.jruby.truffle.language.objects.WriteObjectFieldNode;
import org.jruby.truffle.language.objects.WriteObjectFieldNodeGen;

public abstract class StatPrimitiveNodes {

    public static final HiddenKey STAT_IDENTIFIER = new HiddenKey("stat");

    @RubiniusPrimitive(unsafeNeedsAudit = true, name = "stat_atime")
    public static abstract class StatAtimePrimitiveNode extends StatReadPrimitiveNode {

        public StatAtimePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object atime(VirtualFrame frame, DynamicObject rubyStat) {
            final long time = getStat(rubyStat).atime();
            return ruby("Time.at(time)", "time", time);
        }

    }

    @RubiniusPrimitive(unsafeNeedsAudit = true, name = "stat_ctime")
    public static abstract class StatCtimePrimitiveNode extends StatReadPrimitiveNode {

        public StatCtimePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object ctime(VirtualFrame frame, DynamicObject rubyStat) {
            final long time = getStat(rubyStat).ctime();
            return ruby("Time.at(time)", "time", time);
        }

    }

    @RubiniusPrimitive(unsafeNeedsAudit = true, name = "stat_mtime")
    public static abstract class StatMtimePrimitiveNode extends StatReadPrimitiveNode {

        public StatMtimePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object mtime(VirtualFrame frame, DynamicObject rubyStat) {
            final long time = getStat(rubyStat).mtime();
            return ruby("Time.at(time)", "time", time);
        }

    }

    @RubiniusPrimitive(unsafeNeedsAudit = true, name = "stat_nlink")
    public static abstract class NlinkPrimitiveNode extends StatReadPrimitiveNode {

        public NlinkPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int nlink(DynamicObject rubyStat) {
            return getStat(rubyStat).nlink();
        }

    }

    @RubiniusPrimitive(unsafeNeedsAudit = true, name = "stat_rdev")
    public static abstract class RdevPrimitiveNode extends StatReadPrimitiveNode {

        public RdevPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long rdev(DynamicObject rubyStat) {
            return getStat(rubyStat).rdev();
        }

    }

    @RubiniusPrimitive(unsafeNeedsAudit = true, name = "stat_blksize")
    public static abstract class StatBlksizePrimitiveNode extends StatReadPrimitiveNode {

        public StatBlksizePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long blksize(DynamicObject rubyStat) {
            return getStat(rubyStat).blockSize();
        }

    }

    @RubiniusPrimitive(unsafeNeedsAudit = true, name = "stat_blocks")
    public static abstract class StatBlocksPrimitiveNode extends StatReadPrimitiveNode {

        public StatBlocksPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long blocks(DynamicObject rubyStat) {
            return getStat(rubyStat).blocks();
        }

    }

    @RubiniusPrimitive(unsafeNeedsAudit = true, name = "stat_dev")
    public static abstract class StatDevPrimitiveNode extends StatReadPrimitiveNode {

        public StatDevPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long dev(DynamicObject rubyStat) {
            return getStat(rubyStat).dev();
        }

    }

    @RubiniusPrimitive(unsafeNeedsAudit = true, name = "stat_ino")
    public static abstract class StatInoPrimitiveNode extends StatReadPrimitiveNode {

        public StatInoPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long ino(DynamicObject rubyStat) {
            return getStat(rubyStat).ino();
        }

    }

    @RubiniusPrimitive(unsafeNeedsAudit = true, name = "stat_stat")
    public static abstract class StatStatPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Child private WriteObjectFieldNode writeStatNode;

        public StatStatPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            writeStatNode = WriteObjectFieldNodeGen.create(getContext(), STAT_IDENTIFIER);
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

    @RubiniusPrimitive(unsafeNeedsAudit = true, name = "stat_fstat")
    public static abstract class StatFStatPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Child private WriteObjectFieldNode writeStatNode;

        public StatFStatPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            writeStatNode = WriteObjectFieldNodeGen.create(getContext(), STAT_IDENTIFIER);
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

    @RubiniusPrimitive(unsafeNeedsAudit = true, name = "stat_lstat")
    public static abstract class StatLStatPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Child private WriteObjectFieldNode writeStatNode;

        public StatLStatPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            writeStatNode = WriteObjectFieldNodeGen.create(getContext(), STAT_IDENTIFIER);
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

        public StatReadPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readStatNode = ReadObjectFieldNodeGen.create(getContext(), STAT_IDENTIFIER, null);
        }

        public FileStat getStat(DynamicObject rubyStat) {
            return (FileStat) readStatNode.execute(rubyStat);
        }

    }

    @RubiniusPrimitive(unsafeNeedsAudit = true, name = "stat_size")
    public static abstract class StatSizePrimitiveNode extends StatReadPrimitiveNode {

        public StatSizePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long size(DynamicObject rubyStat) {
            return getStat(rubyStat).st_size();
        }

    }

    @RubiniusPrimitive(unsafeNeedsAudit = true, name = "stat_mode")
    public static abstract class StatModePrimitiveNode extends StatReadPrimitiveNode {

        public StatModePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int mode(DynamicObject rubyStat) {
            return getStat(rubyStat).mode();
        }

    }

    @RubiniusPrimitive(unsafeNeedsAudit = true, name = "stat_gid")
    public static abstract class StatGIDPrimitiveNode extends StatReadPrimitiveNode {

        public StatGIDPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int gid(DynamicObject rubyStat) {
            return getStat(rubyStat).gid();
        }

    }

    @RubiniusPrimitive(unsafeNeedsAudit = true, name = "stat_uid")
    public static abstract class StatUIDPrimitiveNode extends StatReadPrimitiveNode {

        public StatUIDPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int uid(DynamicObject rubyStat) {
            return getStat(rubyStat).uid();
        }

    }

}
