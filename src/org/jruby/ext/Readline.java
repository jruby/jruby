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
 * Copyright (C) 2008 Joseph LaFata <joe@quibb.org>
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import jline.CandidateListCompletionHandler;
import jline.Completor;
import jline.ConsoleReader;
import jline.CursorBuffer;
import jline.FileNameCompletor;
import jline.History;

import static org.jruby.CompatVersion.*;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyIO;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 * @author <a href="mailto:pldms@mac.com">Damian Steer</a>
 * @author <a href="mailto:koichiro@meadowy.org">Koichiro Ohba</a>
 */
@JRubyModule(name = "Readline")
public class Readline {
    public static final char ESC_KEY_CODE = (char)27;
    private final static boolean DEBUG = false;
    private static IRubyObject COMPLETION_CASE_FOLD = null;

    public static class ReadlineHistory extends History {
        ArrayList historyList = null;
        Field index = null;
        private boolean securityRestricted = false;

        public ReadlineHistory() {
            try {
                Field list = History.class.getDeclaredField("history");
                list.setAccessible(true);
                historyList = (ArrayList) list.get(this);
                index = History.class.getDeclaredField("currentIndex");
                index.setAccessible(true);
            } catch (NoSuchFieldException ex) {
                ex.printStackTrace();
            } catch (SecurityException ex) {
                securityRestricted = true;
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }

        public void setCurrentIndex(int i) {
            if (securityRestricted) {
                return; // do nothing
            }
            try {
                index.setInt(this, i);
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }

        public void set(int i, String s) {
            if (securityRestricted) {
                return; // do nothing
            }
            historyList.set(i, s);
        }

        @SuppressWarnings("unchecked")
        public String pop() {
            if (securityRestricted) {
                // Not fully implemented in security restricted environment.
                // We just return the last value, without really popping it.
                List histList = getHistoryList();
                return (String)histList.get(histList.size() - 1);
            }
            return remove(historyList.size() - 1);
        }

        public String remove(int i) {
            if (securityRestricted) {
                // do nothing, we can't modify the history without
                // accessing private members of History.
                return "";
            }
            setCurrentIndex(historyList.size() - 2);
            return (String)historyList.remove(i);
        }
    }

    public static class ConsoleHolder {
        public ConsoleReader readline;
        public Completor currentCompletor;
        public ReadlineHistory history;
    }

    public static void createReadline(Ruby runtime) throws IOException {
        ConsoleHolder holder = new ConsoleHolder();
        holder.history = new ReadlineHistory();
        holder.currentCompletor = null;
        COMPLETION_CASE_FOLD = runtime.getNil();

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
    protected static void initReadline(Ruby runtime, final ConsoleHolder holder) {
        try {
            holder.readline = new ConsoleReader();
        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        }
        holder.readline.setUseHistory(false);
        holder.readline.setUsePagination(true);
        holder.readline.setBellEnabled(true);
        ((CandidateListCompletionHandler) holder.readline.getCompletionHandler()).setAlwaysIncludeNewline(false);
        if (holder.currentCompletor == null) {
            holder.currentCompletor = new RubyFileNameCompletor();
        }
        holder.readline.addCompletor(holder.currentCompletor);
        holder.readline.setHistory(holder.history);

        // JRUBY-852, ignore escape key (it causes IRB to quit if we pass it out through readline)
        holder.readline.addTriggeredAction(ESC_KEY_CODE, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    holder.readline.beep();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        });

        if (DEBUG) holder.readline.setDebug(new PrintWriter(System.err));
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

    public static IRubyObject s_readline(IRubyObject recv, IRubyObject prompt, IRubyObject add_to_hist) {
        return s_readline(recv.getRuntime().getCurrentContext(), recv, prompt, add_to_hist);
    }

    @JRubyMethod(name = "readline", module = true, visibility = PRIVATE)
    public static IRubyObject s_readline(ThreadContext context, IRubyObject recv, IRubyObject prompt, IRubyObject add_to_hist) {
        Ruby runtime = context.getRuntime();
        ConsoleHolder holder = getHolder(runtime);
        if (holder.readline == null) {
            initReadline(runtime, holder); // not overridden, let's go
        }
        
        IRubyObject line = runtime.getNil();
        String v = null;
        while (true) {
            try {
                holder.readline.getTerminal().disableEcho();
                v = holder.readline.readLine(prompt.toString());
                break;
            } catch (IOException ioe) {
                if (RubyIO.restartSystemCall(ioe)) {
                    // This is for JRUBY-2988, since after a suspend the terminal seems
                    // to need to be reinitialized. Since we can't easily detect suspension,
                    // initialize after every readline. Probably not fast, but this is for
                    // interactive terminals anyway...so who cares?
                    try {holder.readline.getTerminal().initializeTerminal();} catch (Exception e) {}
                    continue;
                }
                throw runtime.newIOErrorFromException(ioe);
            } finally {
                holder.readline.getTerminal().enableEcho();
            }
        }
        
        if (null != v) {
            if (add_to_hist.isTrue()) {
                holder.readline.getHistory().addToHistory(v);
            }

            // Enebo: This is a little weird and a little broken.  We just ask
            // for the bytes and hope they match default_external.  This will 
            // work for common cases, but not ones in which the user explicitly
            // sets the default_external to something else.  The second problem
            // is that no al M17n encodings are valid encodings in java.lang.String.
            // We clearly need a byte[]-version of JLine since we cannot totally
            // behave properly using Java Strings.
            if (runtime.is1_9()) {
                ByteList list = new ByteList(v.getBytes(), runtime.getDefaultExternalEncoding());
                line = RubyString.newString(runtime, list);
            } else {
                /* Explicitly use UTF-8 here. c.f. history.addToHistory using line.asUTF8() */
                line = RubyString.newUnicodeString(recv.getRuntime(), v);
            }
        }
        return line;
    }

    @JRubyMethod(name = "input=", module = true, visibility = PRIVATE, compat = RUBY1_9)
    public static IRubyObject setInput(ThreadContext context, IRubyObject recv, IRubyObject input) {
        // FIXME: JRUBY-3604
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = "output=", module = true, visibility = PRIVATE, compat = RUBY1_9)
    public static IRubyObject setOutput(ThreadContext context, IRubyObject recv, IRubyObject output) {
        // FIXME: JRUBY-3604
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = "readline", module = true, visibility = PRIVATE)
    public static IRubyObject s_readline(IRubyObject recv, IRubyObject prompt) {
        return s_readline(recv, prompt, recv.getRuntime().getFalse());
    }

    @JRubyMethod(name = "readline", module = true, visibility = PRIVATE)
    public static IRubyObject s_readline(IRubyObject recv) {
        return s_readline(recv, RubyString.newEmptyString(recv.getRuntime()), recv.getRuntime().getFalse());
    }

    @JRubyMethod(name = "basic_word_break_characters=", module = true, visibility = PRIVATE)
    public static IRubyObject s_set_basic_word_break_character(IRubyObject recv, IRubyObject achar) {
        Ruby runtime = recv.getRuntime();
        if (!achar.respondsTo("to_str")) {
            throw runtime.newTypeError("can't convert " + achar.getMetaClass() + " into String");
        }
        ProcCompletor.setDelimiter(achar.convertToString().toString());
        return achar;
    }

    @JRubyMethod(name = "basic_word_break_characters", module = true, visibility = PRIVATE)
    public static IRubyObject s_get_basic_word_break_character(IRubyObject recv) {
        return recv.getRuntime().newString(ProcCompletor.getDelimiter());
    }

    @JRubyMethod(name = "completion_append_character=", module = true, visibility = PRIVATE)
    public static IRubyObject s_set_completion_append_character(IRubyObject recv, IRubyObject achar) {
        return recv.getRuntime().getNil();
    }

    @JRubyMethod(name = "completion_proc=", module = true, visibility = PRIVATE)
    public static IRubyObject s_set_completion_proc(IRubyObject recv, IRubyObject proc) {
        if (!proc.respondsTo("call")) {
            throw recv.getRuntime().newArgumentError("argument must respond to call");
        }
        setCompletor(getHolder(recv.getRuntime()), new ProcCompletor(proc));
        return recv.getRuntime().getNil();
    }

    @JRubyMethod(name = {
        "basic_quote_characters", "basic_quote_characters=",
        "completer_quote_characters", "completer_quote_characters=",
        "completer_word_break_characters", "completer_word_break_characters=",
        "completion_append_character",
        "completion_proc",
        "emacs_editing_mode",
        "filename_quote_characters", "filename_quote_characters=",
        "vi_editing_mode"}, frame = true, module = true, visibility = PRIVATE)
    public static IRubyObject unimplemented(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.getRuntime();
        runtime.secure(4);
        String err = context.getFrameName() + "() function is unimplemented on this machine";
        throw runtime.newNotImplementedError(err);
    }

    @JRubyMethod(name = {
        "basic_quote_characters", "basic_quote_characters=",
        "completer_quote_characters", "completer_quote_characters=",
        "completer_word_break_characters", "completer_word_break_characters=",
        "completion_append_character",
        "completion_proc",
        "emacs_editing_mode", "emacs_editing_mode?",
        "filename_quote_characters", "filename_quote_characters=",
        "vi_editing_mode", "vi_editing_mode?",
        "set_screen_size"}, frame = true, module = true, visibility = PRIVATE, compat = RUBY1_9)
    public static IRubyObject unimplemented19(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.getRuntime();
        runtime.secure(4);
        String err = context.getFrameName() + "() function is unimplemented on this machine";
        throw runtime.newNotImplementedError(err);
    }

    @JRubyMethod(name = "completion_case_fold", module = true, visibility = PRIVATE)
    public static IRubyObject s_get_completion_case_fold(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.getRuntime();
        runtime.secure(4);
        return COMPLETION_CASE_FOLD;
    }

    @JRubyMethod(name = "completion_case_fold=", required = 1, module = true, visibility = PRIVATE)
    // FIXME: this is really a noop
    public static IRubyObject s_set_completion_case_fold(ThreadContext context, IRubyObject recv,
            IRubyObject other) {
        Ruby runtime = context.getRuntime();
        runtime.secure(4);
        return COMPLETION_CASE_FOLD = other;
    }

    @JRubyMethod(name = "get_screen_size", module = true, visibility = PRIVATE, compat = RUBY1_9)
    public static IRubyObject s_get_screen_size(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.getRuntime();
        runtime.secure(4);
        ConsoleHolder holder = getHolder(runtime);
        IRubyObject[] ary = new IRubyObject[2];
        ary[0] = runtime.newFixnum(holder.readline.getTermheight());
        ary[1] = runtime.newFixnum(holder.readline.getTermwidth());
        return RubyArray.newArray(runtime, ary);

    }

    @JRubyMethod(name = "line_buffer", module = true, visibility = PRIVATE, compat = RUBY1_9)
    public static IRubyObject s_get_line_buffer(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.getRuntime();
        runtime.secure(4);
        ConsoleHolder holder = getHolder(runtime);
        if (holder.readline == null) {
            initReadline(runtime, holder);
        }
        CursorBuffer cb = holder.readline.getCursorBuffer();
        return runtime.newString(cb.toString()).taint(context);
    }

    @JRubyMethod(name = "point", module = true, visibility = PRIVATE, compat = RUBY1_9)
    public static IRubyObject s_get_point(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.getRuntime();
        runtime.secure(4);
        ConsoleHolder holder = getHolder(runtime);
        if (holder.readline == null) {
            initReadline(runtime, holder);
        }
        CursorBuffer cb = holder.readline.getCursorBuffer();
        return runtime.newFixnum(cb.cursor);
    }

    @JRubyMethod(name = "refresh_line", module = true, visibility = PRIVATE, compat = RUBY1_9)
    public static IRubyObject s_refresh_line(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.getRuntime();
        runtime.secure(4);
        ConsoleHolder holder = getHolder(runtime);
        try {
            holder.readline.redrawLine(); // not quite the same as rl_refresh_line()
        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        }
        return runtime.getNil();
    }

    public static class HistoryMethods {
        @JRubyMethod(name = {"push", "<<"}, rest = true)
        public static IRubyObject s_push(IRubyObject recv, IRubyObject[] lines) {
            ConsoleHolder holder = getHolder(recv.getRuntime());
            for (int i = 0; i < lines.length; i++) {
                RubyString line = lines[i].convertToString();
                holder.history.addToHistory(line.getUnicodeValue());
            }
            return recv.getRuntime().getNil();
        }

        @JRubyMethod(name = "pop")
        @SuppressWarnings("unchecked")
        public static IRubyObject s_pop(IRubyObject recv) {
            Ruby runtime = recv.getRuntime();
            ConsoleHolder holder = getHolder(runtime);

            if (holder.history.size() == 0) return runtime.getNil();

            return runtime.newString((String)holder.history.pop()).taint(runtime.getCurrentContext());
        }

        @JRubyMethod(name = "to_a")
        public static IRubyObject s_hist_to_a(IRubyObject recv) {
            Ruby runtime = recv.getRuntime();
            ConsoleHolder holder = getHolder(runtime);
            RubyArray histList = runtime.newArray();

            for (Iterator i = holder.history.getHistoryList().iterator(); i.hasNext();) {
                histList.append(runtime.newString((String) i.next()));
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

            if (i < 0) i += holder.history.size();

            try {
                ThreadContext context = runtime.getCurrentContext();

                return runtime.newString((String) holder.history.getHistoryList().get(i)).taint(context);
            } catch (IndexOutOfBoundsException ioobe) {
                throw runtime.newIndexError("invalid history index: " + i);
            }
        }

        @JRubyMethod(name = "[]=")
        public static IRubyObject s_hist_set(IRubyObject recv, IRubyObject index, IRubyObject val) {
            Ruby runtime = recv.getRuntime();
            ConsoleHolder holder = getHolder(runtime);
            int i = (int) index.convertToInteger().getLongValue();

            if (i < 0) i += holder.history.size();

            try {
                holder.history.set(i, val.asJavaString());
            } catch (IndexOutOfBoundsException ioobe) {
                throw runtime.newIndexError("invalid history index: " + i);
        }
            return runtime.getNil();
        }

        @JRubyMethod(name = "shift")
        public static IRubyObject s_hist_shift(IRubyObject recv) {
            Ruby runtime = recv.getRuntime();
            ConsoleHolder holder = getHolder(runtime);

            if (holder.history.size() == 0) return runtime.getNil();

            try {
                return runtime.newString(holder.history.remove(0)).taint(runtime.getCurrentContext());
            } catch (IndexOutOfBoundsException ioobe) {
                throw runtime.newIndexError("history shift error");
            }
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
            Ruby runtime = recv.getRuntime();
            ThreadContext context = runtime.getCurrentContext();

            ConsoleHolder holder = getHolder(runtime);
            int i = RubyNumeric.num2int(index);

            if (i < 0) i += holder.history.size();
            
            try {
                return runtime.newString(holder.history.remove(i)).taint(context);
            } catch (IndexOutOfBoundsException ioobe) {
                throw runtime.newIndexError("invalid history index: " + i);
            }
        }

        @JRubyMethod(name = "each")
        public static IRubyObject s_hist_each(IRubyObject recv, Block block) {
            Ruby runtime = recv.getRuntime();
            ThreadContext context = runtime.getCurrentContext();
            ConsoleHolder holder = getHolder(runtime);

            for (Iterator i = holder.history.getHistoryList().iterator(); i.hasNext();) {
                block.yield(context, runtime.newString((String) i.next()).taint(context));
            }
            return recv;
        }
    }

    // Complete using a Proc object
    public static class ProcCompletor implements Completor {

        IRubyObject procCompletor;
        //\t\n\"\\'`@$><=;|&{(
        static private String[] delimiters = {" ", "\t", "\n", "\"", "\\", "'", "`", "@", "$", ">", "<", "=", ";", "|", "&", "{", "("};

        public ProcCompletor(IRubyObject procCompletor) {
            this.procCompletor = procCompletor;
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

            Ruby runtime = procCompletor.getRuntime();
            ThreadContext context = runtime.getCurrentContext();
            IRubyObject result = procCompletor.callMethod(context, "call", runtime.newString(buffer));
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

    // Fix FileNameCompletor to work mid-line
    public static class RubyFileNameCompletor extends FileNameCompletor {
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
