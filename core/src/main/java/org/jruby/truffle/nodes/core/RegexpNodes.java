/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import java.util.regex.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.util.ByteList;

@CoreClass(name = "Regexp")
public abstract class RegexpNodes {

    @CoreMethod(names = {"=~", "==="}, minArgs = 1, maxArgs = 1)
    public abstract static class MatchOperatorNode extends CoreMethodNode {

        public MatchOperatorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MatchOperatorNode(MatchOperatorNode prev) {
            super(prev);
        }

        @Specialization
        public Object match(VirtualFrame frame, RubyRegexp regexp, RubyString string) {
            return regexp.matchOperator(frame.getCaller().unpack(), string.toString());
        }

    }

    @CoreMethod(names = "!~", minArgs = 1, maxArgs = 1)
    public abstract static class NotMatchOperatorNode extends CoreMethodNode {

        public NotMatchOperatorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NotMatchOperatorNode(NotMatchOperatorNode prev) {
            super(prev);
        }

        @Specialization
        public Object match(VirtualFrame frame, RubyRegexp regexp, RubyString string) {
            return regexp.matchOperator(frame.getCaller().unpack(), string.toString()) == NilPlaceholder.INSTANCE;
        }

    }

    @CoreMethod(names = "escape", isModuleMethod = true, needsSelf = false, minArgs = 1, maxArgs = 1)
    public abstract static class EscapeNode extends CoreMethodNode {

        public EscapeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EscapeNode(EscapeNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString sqrt(RubyString pattern) {
            return getContext().makeString(org.jruby.RubyRegexp.quote19(new ByteList(pattern.getBytes()), true).toString());
        }

    }

    @CoreMethod(names = "initialize", minArgs = 1, maxArgs = 1)
    public abstract static class InitializeNode extends CoreMethodNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeNode(InitializeNode prev) {
            super(prev);
        }

        @Specialization
        public NilPlaceholder initialize(RubyRegexp regexp, RubyString string) {
            regexp.initialize(string.toString());
            return NilPlaceholder.INSTANCE;
        }

    }

    @CoreMethod(names = "match", appendCallNode = true, minArgs = 2, maxArgs = 2)
    public abstract static class MatchNode extends CoreMethodNode {

        public MatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MatchNode(MatchNode prev) {
            super(prev);
        }

        @Specialization
        public Object match(RubyRegexp regexp, RubyString string, Node callNode) {
            return regexp.match(string.toString());
        }

    }

}
