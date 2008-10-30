/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * Copyright (C) 2006 Damian Steer <pldms@mac.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Iterator;
import java.util.Collections;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyArray;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.load.Library;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.anno.JRubyModule;

import jline.ConsoleReader;
import jline.Completor;
import jline.FileNameCompletor;
import jline.CandidateListCompletionHandler;
import jline.History;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.Visibility;
import org.jruby.util.ByteList;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 * @author <a href="mailto:pldms@mac.com">Damian Steer</a>
 */
@JRubyModule(name = "Readline", include = "Enumerable")
public class Readline {

    public static class Service implements Library {

        public void load(final Ruby runtime, boolean wrap) throws IOException {
            createReadline(runtime);
        }
    }

    public static class ConsoleHolder {

        public ConsoleReader readline;
        public Completor currentCompletor;
        public History history;
    }

    public static void createReadline(Ruby runtime) throws IOException {
        ConsoleHolder holder = new ConsoleHolder();
        holder.history = new History();
        holder.currentCompletor = null;

        RubyModule mReadline = runtime.defineModule("Readline");

        mReadline.dataWrapStruct(holder);

        mReadline.defineAnnotatedMethods(Readline.class);
        IRubyObject hist = runtime.getObject().callMethod(runtime.getCurrentContext(), "new");
        mReadline.fastSetConstant("HISTORY", hist);
        hist.getSingletonClass().includeModule(runtime.getEnumerable());
        hist.getSingletonClass().defineAnnotatedMethods(HistoryMethods.class);

        // MRI does similar thing on MacOS X with 'EditLine wrapper'.
        mReadline.fastSetConstant("VERSION", runtime.newString("JLine wrapper"));
    }

    // We lazily initialize this in case Readline.readline has been overridden in ruby (s_readline)
    protected static void initReadline(Ruby runtime, ConsoleHolder holder) throws IOException {
        holder.readline = new ConsoleReader();
        holder.readline.setUseHistory(false);
        holder.readline.setUsePagination(true);
        holder.readline.setBellEnabled(false);
        ((CandidateListCompletionHandler) holder.readline.getCompletionHandler()).setAlwaysIncludeNewline(false);
        if (holder.currentCompletor == null) {
            holder.currentCompletor = new RubyFileNameCompletor();
        }
        holder.readline.addCompletor(holder.currentCompletor);
        holder.readline.setHistory(holder.history);
    }

    public static History getHistory(ConsoleHolder holder) {
        return holder.history;
    }

    public static ConsoleHolder getHolder(Ruby runtime) {
        return (ConsoleHolder) (runtime.fastGetModule("Readline").dataGetStruct());
    }

    public static void setCompletor(ConsoleHolder holder, Completor completor) {
        if (holder.readline != null) {
            holder.readline.removeCompletor(holder.currentCompletor);
        }
        holder.currentCompletor = completor;
        if (holder.readline != null) {
            holder.readline.addCompletor(holder.currentCompletor);
        }
    }

    public static Completor getCompletor(ConsoleHolder holder) {
        return holder.currentCompletor;
    }

    @JRubyMethod(name = "readline", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject s_readline(IRubyObject recv, IRubyObject prompt, IRubyObject add_to_hist) throws IOException {
        ConsoleHolder holder = getHolder(recv.getRuntime());
        if (holder.readline == null) {
            initReadline(recv.getRuntime(), holder); // not overridden, let's go

        }
        IRubyObject line = recv.getRuntime().getNil();
        holder.readline.getTerminal().disableEcho();
        String v = holder.readline.readLine(prompt.toString());
        holder.readline.getTerminal().enableEcho();
        if (null != v) {
            if (add_to_hist.isTrue()) {
                holder.readline.getHistory().addToHistory(v);
            }

            /* Explicitly use UTF-8 here. c.f. history.addToHistory using line.asUTF8() */
            line = RubyString.newUnicodeString(recv.getRuntime(), v);
        }
        return line;
    }

    @JRubyMethod(name = "completion_append_character=", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject s_set_completion_append_character(IRubyObject recv, IRubyObject achar) throws Exception {
        return recv.getRuntime().getNil();
    }

    @JRubyMethod(name = "completion_proc=", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject s_set_completion_proc(IRubyObject recv, IRubyObject proc) throws Exception {
        if (!proc.respondsTo("call")) {
            throw recv.getRuntime().newArgumentError("argument must respond to call");
        }
        setCompletor(getHolder(recv.getRuntime()), new ProcCompletor(proc));
        return recv.getRuntime().getNil();
    }

    public static class HistoryMethods {
        @JRubyMethod(name = {"push", "<<"}, rest = true)
        public static IRubyObject s_push(IRubyObject recv, IRubyObject[] lines) throws Exception {
            ConsoleHolder holder = getHolder(recv.getRuntime());
            for (int i = 0; i < lines.length; i++) {
                RubyString line = lines[i].convertToString();
                holder.history.addToHistory(line.getUnicodeValue());
            }
            return recv.getRuntime().getNil();
        }

        @JRubyMethod(name = "pop")
        @SuppressWarnings("unchecked")
        public static IRubyObject s_pop(IRubyObject recv) throws Exception {
            Ruby runtime = recv.getRuntime();
            ConsoleHolder holder = getHolder(runtime);
            List histList = holder.history.getHistoryList();

            // TODO: Not fully implemented. We just return the last value,
            // without really popping it.
            String current = (String)histList.get(histList.size() - 1);
            return runtime.newString(current);
        }

        @JRubyMethod(name = "to_a")
        public static IRubyObject s_hist_to_a(IRubyObject recv) throws Exception {
            ConsoleHolder holder = getHolder(recv.getRuntime());
            RubyArray histList = recv.getRuntime().newArray();
            for (Iterator i = holder.history.getHistoryList().iterator(); i.hasNext();) {
                histList.append(recv.getRuntime().newString((String) i.next()));
            }
            return histList;
        }

        @JRubyMethod(name = "to_s")
        public static IRubyObject s_hist_to_s(IRubyObject recv) {
            return recv.getRuntime().newString("HISTORY");
        }

        @JRubyMethod(name = "[]")
        public static IRubyObject s_hist_get(IRubyObject recv, IRubyObject index) {
            Ruby runtime = recv.getRuntime();
            ConsoleHolder holder = getHolder(runtime);
            int i = (int) index.convertToInteger().getLongValue();
            try {
                // TODO: MRI behavior is more complicated than that,
                // there is some magic when dealing with negative indexes.
                return runtime.newString((String) holder.history.getHistoryList().get(i));
            } catch (IndexOutOfBoundsException ioobe) {
                throw runtime.newIndexError("invalid history index: " + i);
            }
        }

        @JRubyMethod(name = "[]=")
        public static IRubyObject s_hist_set(IRubyObject recv, IRubyObject index, IRubyObject val) {
            throw recv.getRuntime().newNotImplementedError("the []=() function is unimplemented on this machine");
        }

        @JRubyMethod(name = "shift")
        public static IRubyObject s_hist_shift(IRubyObject recv) {
            throw recv.getRuntime().newNotImplementedError("the shift function is unimplemented on this machine");
        }

        @JRubyMethod(name = {"length", "size"})
        public static IRubyObject s_hist_length(IRubyObject recv) {
            ConsoleHolder holder = getHolder(recv.getRuntime());
            return recv.getRuntime().newFixnum(holder.history.size());
        }

        @JRubyMethod(name = "empty?")
        public static IRubyObject s_hist_empty_p(IRubyObject recv) {
            ConsoleHolder holder = getHolder(recv.getRuntime());
            return recv.getRuntime().newBoolean(holder.history.size() == 0);
        }

        @JRubyMethod(name = "delete_at")
        public static IRubyObject s_hist_delete_at(IRubyObject recv, IRubyObject index) {
            throw recv.getRuntime().newNotImplementedError("the delete_at function is unimplemented on this machine");
        }

        @JRubyMethod(name = "each")
        public static IRubyObject s_hist_each(IRubyObject recv, Block block) {
            ConsoleHolder holder = getHolder(recv.getRuntime());
            for (Iterator i = holder.history.getHistoryList().iterator(); i.hasNext();) {
                block.yield(recv.getRuntime().getCurrentContext(), recv.getRuntime().newString((String) i.next()));
            }
            return recv;
        }
    }

    // Complete using a Proc object
    public static class ProcCompletor implements Completor {

        IRubyObject procCompletor;

        public ProcCompletor(IRubyObject procCompletor) {
            this.procCompletor = procCompletor;
        }

        public int complete(String buffer, int cursor, List candidates) {
            buffer = buffer.substring(0, cursor);
            int index = buffer.lastIndexOf(" ");
            if (index != -1) {
                buffer = buffer.substring(index + 1);
            }
            ThreadContext context = procCompletor.getRuntime().getCurrentContext();

            IRubyObject comps = RuntimeHelpers
                    .invoke(context, procCompletor, "call", procCompletor.getRuntime().newString(buffer))
                    .callMethod(context, "to_a");
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

    // Fix FileNameCompletor to work mid-line
    public static class RubyFileNameCompletor extends FileNameCompletor {

        public int complete(String buffer, int cursor, List candidates) {
            buffer = buffer.substring(0, cursor);
            int index = buffer.lastIndexOf(" ");
            if (index != -1) {
                buffer = buffer.substring(index + 1);
            }
            return index + 1 + super.complete(buffer, cursor, candidates);
        }
    }
}// Readline
