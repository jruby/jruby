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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.core.ThreadNodes;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyException;
import org.jruby.truffle.runtime.core.RubyThread;
import org.jruby.truffle.runtime.subsystems.SafepointAction;

import java.util.Locale;

/**
 * Rubinius primitives associated with the Ruby {@code Thread} class.
 */
public class ThreadPrimitiveNodes {

    @RubiniusPrimitive(name = "thread_raise")
    public static abstract class ThreadRaisePrimitiveNode extends RubiniusPrimitiveNode {

        public ThreadRaisePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = { "isRubyThread(thread)", "isRubyException(exception)" })
        public RubyBasicObject raise(RubyBasicObject thread, final RubyBasicObject exception) {
            getContext().getSafepointManager().pauseThreadAndExecuteLater(
                    ThreadNodes.getCurrentFiberJavaThread(thread),
                    this,
                    new SafepointAction() {
                        @Override
                        public void run(RubyBasicObject currentThread, Node currentNode) {
                            throw new RaiseException(exception);
                        }
                    });

            return nil();
        }

    }

}
