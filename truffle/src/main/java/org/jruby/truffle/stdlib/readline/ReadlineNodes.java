/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.object.DynamicObject;
import jline.console.completer.Completer;
import jline.console.completer.FileNameCompleter;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.builtins.*;
import org.jruby.truffle.core.array.ArrayHelpers;
import org.jruby.truffle.core.cast.BooleanCastWithDefaultNodeGen;
import org.jruby.truffle.core.cast.ToStrNodeGen;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@CoreClass("Truffle::Readline")
public abstract class ReadlineNodes {

    @CoreMethod(names = "basic_word_break_characters", isModuleFunction = true)
    public abstract static class BasicWordBreakCharactersNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject basicWordBreakCharacters() {
            return StringOperations.createString(getContext(), StringOperations.createRope(ProcCompleter.getDelimiter(), UTF8Encoding.INSTANCE));
        }

    }

    @CoreMethod(names = "basic_word_break_characters=", isModuleFunction = true, required = 1)
    @NodeChild(type = RubyNode.class, value = "characters")
    public abstract static class SetBasicWordBreakCharactersNode extends CoreMethodNode {

        @CreateCast("characters") public RubyNode coerceCharactersToString(RubyNode characters) {
            return ToStrNodeGen.create(null, null, characters);
        }

        @Specialization
        public DynamicObject setBasicWordBreakCharacters(DynamicObject characters) {
            ProcCompleter.setDelimiter(RopeOperations.decodeUTF8(StringOperations.rope(characters)));

            return characters;
        }

    }

    @CoreMethod(names = "get_screen_size", isModuleFunction = true)
    public abstract static class GetScreenSizeNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject getScreenSize() {
            final int[] store = { getContext().getConsoleHolder().getReadline().getTerminal().getHeight(), getContext().getConsoleHolder().getReadline().getTerminal().getWidth() };

            return ArrayHelpers.createArray(getContext(), store, 2);
        }

    }

    @CoreMethod(names = "readline", isModuleFunction = true, optional = 2)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "prompt"),
            @NodeChild(type = RubyNode.class, value = "addToHistory")
    })
    public abstract static class ReadlineNode extends CoreMethodNode {

        @CreateCast("addToHistory") public RubyNode coerceToBoolean(RubyNode addToHistory) {
            return BooleanCastWithDefaultNodeGen.create(null, null, false, addToHistory);
        }

        @Specialization(guards = "wasNotProvided(prompt)")
        public DynamicObject readlineNoPrompt(Object prompt, boolean addToHistory) {
            return readline(coreStrings().EMPTY_STRING.createInstance(), addToHistory);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(prompt)")
        public DynamicObject readline(DynamicObject prompt, boolean addToHistory) {
            getContext().getConsoleHolder().getReadline().setExpandEvents(false);

            DynamicObject line = nil();
            String value;
            while (true) {
                try {
                    getContext().getConsoleHolder().getReadline().getTerminal().setEchoEnabled(false);
                    value = getContext().getConsoleHolder().getReadline().readLine(RopeOperations.decodeUTF8(StringOperations.rope(prompt)));
                    break;
                } catch (IOException e) {
                    throw new RaiseException(coreExceptions().ioError("readline", this));
                } finally {
                    getContext().getConsoleHolder().getReadline().getTerminal().setEchoEnabled(true);
                }
            }

            if (value != null) {
                if (addToHistory) {
                    getContext().getConsoleHolder().getReadline().getHistory().add(value);
                }

                // Enebo: This is a little weird and a little broken.  We just ask
                // for the bytes and hope they match default_external.  This will
                // work for common cases, but not ones in which the user explicitly
                // sets the default_external to something else.  The second problem
                // is that no al M17n encodings are valid encodings in java.lang.String.
                // We clearly need a byte[]-version of JLine since we cannot totally
                // behave properly using Java Strings.
                line = StringOperations.createString(getContext(), StringOperations.createRope(value, getContext().getJRubyRuntime().getDefaultExternalEncoding()));
            }

            return line;
        }

    }

    // Taken from org.jruby.ext.readline.Readline.ProcCompleter.
    // Complete using a Proc object
    public static class ProcCompleter implements Completer {

        IRubyObject procCompleter;
        //\t\n\"\\'`@$><=;|&{(
        static private String[] delimiters = {" ", "\t", "\n", "\"", "\\", "'", "`", "@", "$", ">", "<", "=", ";", "|", "&", "{", "("};

        public ProcCompleter(IRubyObject procCompleter) {
            this.procCompleter = procCompleter;
        }

        public static String getDelimiter() {
            StringBuilder result = new StringBuilder(delimiters.length);
            for (String delimiter : delimiters) {
                result.append(delimiter);
            }
            return result.toString();
        }

        public static void setDelimiter(String delimiter) {
            List<String> l = new ArrayList<String>();
            CharBuffer buf = CharBuffer.wrap(delimiter);
            while (buf.hasRemaining()) {
                l.add(String.valueOf(buf.get()));
            }
            delimiters = l.toArray(new String[l.size()]);
        }

        private int wordIndexOf(String buffer) {
            int index = 0;
            for (String c : delimiters) {
                index = buffer.lastIndexOf(c);
                if (index != -1) return index;
            }
            return index;
        }

        public int complete(String buffer, int cursor, List candidates) {
            buffer = buffer.substring(0, cursor);
            int index = wordIndexOf(buffer);
            if (index != -1) buffer = buffer.substring(index + 1);

            Ruby runtime = procCompleter.getRuntime();
            ThreadContext context = runtime.getCurrentContext();
            IRubyObject result = procCompleter.callMethod(context, "call", runtime.newString(buffer));
            IRubyObject comps = result.callMethod(context, "to_a");

            if (comps instanceof List) {
                for (Iterator i = ((List) comps).iterator(); i.hasNext();) {
                    Object obj = i.next();
                    if (obj != null) {
                        candidates.add(obj.toString());
                    }
                }
                Collections.sort(candidates);
            }
            return cursor - buffer.length();
        }
    }

    // Taken from org.jruby.ext.readline.Readline.RubyFileNameCompleter.
    // Fix FileNameCompletor to work mid-line
    public static class RubyFileNameCompleter extends FileNameCompleter {
        @Override
        public int complete(String buffer, int cursor, List candidates) {
            buffer = buffer.substring(0, cursor);
            int index = buffer.lastIndexOf(" ");
            if (index != -1) {
                buffer = buffer.substring(index + 1);
            }
            return index + 1 + super.complete(buffer, cursor, candidates);
        }
    }
}
