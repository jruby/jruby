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

        public StatStatPrimitiveNode(StatStatPrimitiveNode prev) {
            super(prev);
            writeStatNode = prev.writeStatNode;
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

        @Specialization(guards = "!isRubyString(arguments[1])")
        public Object stat(RubyBasicObject rubyStat, Object path) {
            return null;
        }

    }

    @RubiniusPrimitive(name = "stat_size")
    public static abstract class StatSizePrimitiveNode extends RubiniusPrimitiveNode {

        @Child private ReadHeadObjectFieldNode readStatNode;

        public StatSizePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readStatNode = new ReadHeadObjectFieldNode(STAT_IDENTIFIER);
        }

        public StatSizePrimitiveNode(StatSizePrimitiveNode prev) {
            super(prev);
            readStatNode = prev.readStatNode;
        }

        @Specialization
        public long stat(RubyBasicObject rubyStat) {
            final FileStat stat = (FileStat) readStatNode.execute(rubyStat);
            return stat.st_size();
        }

    }

}
