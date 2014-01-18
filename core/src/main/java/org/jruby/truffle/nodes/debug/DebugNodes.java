/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.debug;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import org.jruby.truffle.nodes.call.*;
import org.jruby.truffle.nodes.core.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.*;
import org.jruby.util.cli.Options;

@CoreClass(name = "Debug")
public abstract class DebugNodes {

    @CoreMethod(names = "break", isModuleMethod = true, needsSelf = false, needsBlock = true, appendCallNode = true, minArgs = 0, maxArgs = 3)
    public abstract static class BreakNode extends CoreMethodNode {

        public BreakNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BreakNode(BreakNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public NilPlaceholder debugBreak(VirtualFrame frame, Node callNode, @SuppressWarnings("unused") UndefinedPlaceholder undefined0, @SuppressWarnings("unused") UndefinedPlaceholder undefined1,
                        @SuppressWarnings("unused") UndefinedPlaceholder block) {
            final RubyContext context = getContext();
            if (Options.TRUFFLE_DEBUG_NODES.load()) {
                Node realCallNode = callNode;
                while (realCallNode != null && !(realCallNode instanceof CallNode)) {
                    realCallNode = realCallNode.getParent();
                }
                context.getDebugManager().haltedAt(realCallNode, frame.materialize());
            }
            return NilPlaceholder.INSTANCE;
        }

        @Specialization(order = 2)
        public NilPlaceholder debugBreak(RubyString fileName, int line, @SuppressWarnings("unused") Node callNode, @SuppressWarnings("unused") UndefinedPlaceholder block) {
            final RubyContext context = getContext();
            if (Options.TRUFFLE_DEBUG_NODES.load()) {
                final Source source = context.getSourceManager().get(fileName.toString());
                final SourceLineLocation lineLocation = new SourceLineLocation(source, line);
                context.getDebugManager().setBreakpoint(lineLocation);
            }
            return NilPlaceholder.INSTANCE;
        }

        @Specialization(order = 3)
        public NilPlaceholder debugBreak(RubyString fileName, int line, @SuppressWarnings("unused") Node callNode, RubyProc block) {
            final RubyContext context = getContext();
            if (Options.TRUFFLE_DEBUG_NODES.load()) {
                final Source source = context.getSourceManager().get(fileName.toString());
                final SourceLineLocation lineLocation = new SourceLineLocation(source, line);
                context.getDebugManager().setLineProc(lineLocation, block);
            }
            return NilPlaceholder.INSTANCE;
        }

        @Specialization(order = 4)
        public NilPlaceholder debugBreak(RubySymbol methodName, RubySymbol localName, @SuppressWarnings("unused") Node callNode, @SuppressWarnings("unused") UndefinedPlaceholder block) {
            final RubyContext context = getContext();
            if (Options.TRUFFLE_DEBUG_NODES.load()) {
                final RubyMethod method = context.getCoreLibrary().getMainObject().getLookupNode().lookupMethod(methodName.toString());
                context.getDebugManager().setLocalBreak(method.getUniqueIdentifier(), localName.toString());
            }
            return NilPlaceholder.INSTANCE;
        }

        @Specialization(order = 5)
        public NilPlaceholder debugBreak(RubySymbol methodName, RubySymbol localName, @SuppressWarnings("unused") Node callNode, RubyProc block) {
            final RubyContext context = getContext();
            if (Options.TRUFFLE_DEBUG_NODES.load()) {
                final RubyMethod method = context.getCoreLibrary().getMainObject().getLookupNode().lookupMethod(methodName.toString());
                context.getDebugManager().setLocalProc(method.getUniqueIdentifier(), localName.toString(), block);
            }
            return NilPlaceholder.INSTANCE;
        }

    }

    @CoreMethod(names = "continue", isModuleMethod = true, needsSelf = false, maxArgs = 0)
    public abstract static class ContinueNode extends CoreMethodNode {

        public ContinueNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ContinueNode(ContinueNode prev) {
            super(prev);
        }

        @Specialization
        public Object debugContinue() {
            if (Options.TRUFFLE_DEBUG_NODES.load()) {
                throw new BreakShellException();
            }
            return NilPlaceholder.INSTANCE;
        }

    }

    @CoreMethod(names = "enabled?", isModuleMethod = true, needsSelf = false, maxArgs = 0)
    public abstract static class EnabledNode extends CoreMethodNode {

        public EnabledNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EnabledNode(ContinueNode prev) {
            super(prev);
        }

        @Specialization
        public boolean enabled() {
            return Options.TRUFFLE_DEBUG_NODES.load();
        }

    }

    @CoreMethod(names = "where", isModuleMethod = true, needsSelf = false, appendCallNode = true, minArgs = 1, maxArgs = 1)
    public abstract static class WhereNode extends CoreMethodNode {

        public WhereNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public WhereNode(WhereNode prev) {
            super(prev);
        }

        @Specialization
        public NilPlaceholder where(Node callNode) {
            final RubyContext context = getContext();
            if (Options.TRUFFLE_DEBUG_NODES.load()) {
                context.getRuntime().getOutputStream().println(callNode.getSourceSection());
            }
            return NilPlaceholder.INSTANCE;
        }

    }

    @CoreMethod(names = "remove", isModuleMethod = true, needsSelf = false, needsBlock = true, minArgs = 2, maxArgs = 2)
    public abstract static class RemoveNode extends CoreMethodNode {

        public RemoveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RemoveNode(RemoveNode prev) {
            super(prev);
        }

        @Specialization
        public NilPlaceholder debugRemove(RubyString fileName, int line) {
            final RubyContext context = getContext();
            if (Options.TRUFFLE_DEBUG_NODES.load()) {
                final Source source = context.getSourceManager().get(fileName.toString());
                final SourceLineLocation lineLocation = new SourceLineLocation(source, line);
                context.getDebugManager().removeBreakpoint(lineLocation);
            }
            return NilPlaceholder.INSTANCE;
        }

        @Specialization
        public NilPlaceholder debugRemove(RubySymbol methodName, RubySymbol localName) {
            final RubyContext context = getContext();
            if (Options.TRUFFLE_DEBUG_NODES.load()) {
                final RubyMethod method = context.getCoreLibrary().getMainObject().getLookupNode().lookupMethod(methodName.toString());
                context.getDebugManager().removeLocalProbe(method.getUniqueIdentifier(), localName.toString());
            }
            return NilPlaceholder.INSTANCE;
        }

    }

}
