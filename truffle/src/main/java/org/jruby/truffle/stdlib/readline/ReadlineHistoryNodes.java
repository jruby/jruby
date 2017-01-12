/*
 * Copyright (c) 2016, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * This code is modified from the Readline JRuby extension module
 * implementation with the following header:
 *
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * Copyright (C) 2006 Damian Steer <pldms@mac.com>
 * Copyright (C) 2008 Joseph LaFata <joe@quibb.org>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 */
package org.jruby.truffle.stdlib.readline;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import jline.console.history.History;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.CoreMethodNode;
import org.jruby.truffle.builtins.YieldingCoreMethodNode;
import org.jruby.truffle.core.cast.NameToJavaStringNode;
import org.jruby.truffle.core.cast.NameToJavaStringNodeGen;
import org.jruby.truffle.core.cast.ToIntNodeGen;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.objects.TaintNode;

@CoreClass("Truffle::ReadlineHistory")
public abstract class ReadlineHistoryNodes {

    @CoreMethod(names = { "push", "<<" }, rest = true)
    public abstract static class PushNode extends CoreMethodArrayArgumentsNode {

        @Child private NameToJavaStringNode toJavaStringNode = NameToJavaStringNodeGen.create();

        @Specialization
        public DynamicObject push(VirtualFrame frame, DynamicObject history, Object... lines) {
            for (Object line : lines) {
                final String asString = toJavaStringNode.executeToJavaString(frame, line);
                addToHistory(asString);
            }

            return history;
        }

        @TruffleBoundary
        private void addToHistory(String item) {
            getContext().getConsoleHolder().getHistory().add(item);
        }

    }

    @CoreMethod(names = "pop", needsSelf = false)
    public abstract static class PopNode extends CoreMethodArrayArgumentsNode {

        @Child private TaintNode taintNode = TaintNode.create();

        @TruffleBoundary
        @Specialization
        public Object pop() {
            final ConsoleHolder consoleHolder = getContext().getConsoleHolder();

            if (consoleHolder.getHistory().isEmpty()) {
                return nil();
            }

            final String lastLine = consoleHolder.getHistory().removeLast().toString();
            final DynamicObject ret = createString(StringOperations.encodeRope(lastLine, getDefaultInternalEncoding()));

            return taintNode.executeTaint(ret);
        }

    }

    @CoreMethod(names = "shift", needsSelf = false)
    public abstract static class ShiftNode extends CoreMethodArrayArgumentsNode {

        @Child private TaintNode taintNode = TaintNode.create();

        @TruffleBoundary
        @Specialization
        public Object shift() {
            final ConsoleHolder consoleHolder = getContext().getConsoleHolder();

            if (consoleHolder.getHistory().isEmpty()) {
                return nil();
            }

            final String lastLine = consoleHolder.getHistory().removeFirst().toString();
            final DynamicObject ret = createString(StringOperations.encodeRope(lastLine, getDefaultInternalEncoding()));

            return taintNode.executeTaint(ret);
        }

    }

    @CoreMethod(names = { "length", "size" }, needsSelf = false)
    public abstract static class LengthNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public int length() {
            return getContext().getConsoleHolder().getHistory().size();
        }

    }

    @CoreMethod(names = "clear", needsSelf = false)
    public abstract static class ClearNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject clear() {
            getContext().getConsoleHolder().getHistory().clear();

            return nil();
        }

    }

    @CoreMethod(names = "each", needsBlock = true)
    public abstract static class EachNode extends YieldingCoreMethodNode {

        @Child private TaintNode taintNode = TaintNode.create();

        @Specialization
        public DynamicObject each(VirtualFrame frame, DynamicObject history, DynamicObject block) {
            final ConsoleHolder consoleHolder = getContext().getConsoleHolder();

            for (final History.Entry e : consoleHolder.getHistory()) {
                final DynamicObject line = createString(StringOperations.encodeRope(historyEntryToString(e), getDefaultInternalEncoding()));

                yield(frame, block, taintNode.executeTaint(line));
            }

            return history;
        }

        @TruffleBoundary
        private String historyEntryToString(History.Entry entry) {
            return entry.value().toString();
        }

    }

    @CoreMethod(names = "[]", needsSelf = false, required = 1, lowerFixnum = 1)
    public abstract static class GetIndexNode extends CoreMethodArrayArgumentsNode {

        @Child private TaintNode taintNode = TaintNode.create();

        @TruffleBoundary
        @Specialization
        public Object getIndex(int index) {
            final ConsoleHolder consoleHolder = getContext().getConsoleHolder();

            final int normalizedIndex = index < 0 ? index + consoleHolder.getHistory().size() : index;

            try {
                final String line = consoleHolder.getHistory().get(normalizedIndex).toString();
                final DynamicObject ret = createString(StringOperations.encodeRope(line, getDefaultInternalEncoding()));

                return taintNode.executeTaint(ret);
            } catch (IndexOutOfBoundsException e) {
                throw new RaiseException(coreExceptions().indexErrorInvalidIndex(this));
            }
        }

    }

    @CoreMethod(names = "[]=", needsSelf = false, lowerFixnum = 1, required = 2)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "index"),
            @NodeChild(type = RubyNode.class, value = "line")
    })
    public abstract static class SetIndexNode extends CoreMethodNode {

        @CreateCast("index") public RubyNode coerceIndexToInt(RubyNode index) {
            return ToIntNodeGen.create(index);
        }

        @CreateCast("line") public RubyNode coerceLineToJavaString(RubyNode line) {
            return NameToJavaStringNodeGen.create(line);
        }

        @TruffleBoundary
        @Specialization
        public Object setIndex(int index, String line) {
            final ConsoleHolder consoleHolder = getContext().getConsoleHolder();

            final int normalizedIndex = index < 0 ? index + consoleHolder.getHistory().size() : index;

            try {
                consoleHolder.getHistory().set(normalizedIndex, line);

                return nil();
            } catch (IndexOutOfBoundsException e) {
                throw new RaiseException(coreExceptions().indexErrorInvalidIndex(this));
            }
        }

    }

    @CoreMethod(names = "delete_at", needsSelf = false, required = 1, lowerFixnum = 1)
    public abstract static class DeleteAtNode extends CoreMethodArrayArgumentsNode {

        @Child private TaintNode taintNode = TaintNode.create();

        @TruffleBoundary
        @Specialization
        public Object deleteAt(int index) {
            final ConsoleHolder consoleHolder = getContext().getConsoleHolder();

            final int normalizedIndex = index < 0 ? index + consoleHolder.getHistory().size() : index;

            try {
                final String line = consoleHolder.getHistory().remove(normalizedIndex).toString();
                final DynamicObject ret = createString(StringOperations.encodeRope(line, getDefaultInternalEncoding()));

                return taintNode.executeTaint(ret);
            } catch (IndexOutOfBoundsException e) {
                throw new RaiseException(coreExceptions().indexErrorInvalidIndex(this));
            }
        }

    }

}
