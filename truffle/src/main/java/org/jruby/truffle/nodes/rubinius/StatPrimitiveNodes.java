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
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.source.SourceSection;
import jnr.posix.FileStat;
import org.jruby.truffle.nodes.objectstorage.ReadHeadObjectFieldNode;
import org.jruby.truffle.nodes.objectstorage.WriteHeadObjectFieldNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyString;

public abstract class StatPrimitiveNodes {

    public static final HiddenKey STAT_IDENTIFIER = new HiddenKey("stat");

    @RubiniusPrimitive(name = "stat_stat")
    public static abstract class StatStatPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private WriteHeadObjectFieldNode writeStatNode;

        public StatStatPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            writeStatNode = new WriteHeadObjectFieldNode(STAT_IDENTIFIER);
        }

        @Specialization
        public int stat(RubyBasicObject rubyStat, RubyString path) {
            final FileStat stat = getContext().getRuntime().getPosix().allocateStat();
            final int code = getContext().getRuntime().getPosix().stat(path.toString(), stat);

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
