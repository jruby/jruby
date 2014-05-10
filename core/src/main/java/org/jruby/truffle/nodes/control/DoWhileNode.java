/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.control;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.BreakException;
import org.jruby.truffle.runtime.control.NextException;
import org.jruby.truffle.runtime.control.RedoException;

/**
 * Represents a Ruby {@code while} statement where the body is executed before the condition for the first time.
 */
@NodeInfo(shortName = "do-while")
public class DoWhileNode extends RubyNode {

    @Child protected BooleanCastNode condition;
    @Child protected RubyNode body;

    @CompilerDirectives.CompilationFinal private int thenCount;
    @CompilerDirectives.CompilationFinal private int elseCount;

    private final BranchProfile thenProfile = new BranchProfile();
    private final BranchProfile elseProfile = new BranchProfile();

    private final BranchProfile breakProfile = new BranchProfile();
    private final BranchProfile nextProfile = new BranchProfile();
    private final BranchProfile redoProfile = new BranchProfile();

    public DoWhileNode(RubyContext context, SourceSection sourceSection, BooleanCastNode condition, RubyNode body) {
        super(context, sourceSection);
        this.condition = condition;
        this.body = body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        int count = 0;

        try {
            outer: while (true) {
                while (true) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    try {
                        body.execute(frame);
                        break;
                    } catch (BreakException e) {
                        breakProfile.enter();
                        return e.getResult();
                    } catch (NextException e) {
                        nextProfile.enter();
                        break;
                    } catch (RedoException e) {
                        redoProfile.enter();
                    }
                }
                if (CompilerDirectives.injectBranchProbability((double) thenCount / (double) (thenCount + elseCount), condition.executeBoolean(frame))) {
                    if (CompilerDirectives.inInterpreter()) {
                        thenCount++;
                    }
                    thenProfile.enter();
                    continue outer;
                } else {
                    if (CompilerDirectives.inInterpreter()) {
                        elseCount++;
                    }
                    elseProfile.enter();
                    break outer;
                }
            }
        } finally {
            if (CompilerDirectives.inInterpreter()) {
                ((RubyRootNode) getRootNode()).reportLoopCountThroughBlocks(count);
            }
        }

        return NilPlaceholder.INSTANCE;
    }

}
