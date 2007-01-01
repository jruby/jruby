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
import java.util.List;
import java.util.Iterator;
import java.util.Collections;

import org.jruby.IRuby;
import org.jruby.RubyModule;
import org.jruby.RubyArray;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.load.Library;
import org.jruby.runtime.builtin.IRubyObject;

import jline.ConsoleReader;
import jline.Completor;
import jline.FileNameCompletor;
import jline.CandidateListCompletionHandler;
import jline.History;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 * @author <a href="mailto:pldms@mac.com">Damian Steer</a>
 */
public class Readline {
    public static class Service implements Library {
        public void load(final IRuby runtime) throws IOException {
            createReadline(runtime);
        }
    }

    private static ConsoleReader readline;
    private static Completor currentCompletor;
    private static History history;

    public static void createReadline(IRuby runtime) throws IOException {
        history = new History();
        currentCompletor = null;
        
        RubyModule mReadline = runtime.defineModule("Readline");
        CallbackFactory readlinecb = runtime.callbackFactory(Readline.class);
        mReadline.defineMethod("readline",readlinecb.getSingletonMethod("s_readline",IRubyObject.class,IRubyObject.class));
        mReadline.module_function(new IRubyObject[]{runtime.newSymbol("readline")});
        mReadline.defineMethod("completion_append_character=",readlinecb.getSingletonMethod("s_set_completion_append_character",IRubyObject.class));
        mReadline.module_function(new IRubyObject[]{runtime.newSymbol("completion_append_character=")});
        mReadline.defineMethod("completion_proc=",readlinecb.getSingletonMethod("s_set_completion_proc",IRubyObject.class));
        mReadline.module_function(new IRubyObject[]{runtime.newSymbol("completion_proc=")});
        IRubyObject hist = runtime.getObject().callMethod(runtime.getCurrentContext(), "new");
        mReadline.setConstant("HISTORY",hist);
        hist.defineSingletonMethod("push",readlinecb.getSingletonMethod("s_push",IRubyObject.class));
        hist.defineSingletonMethod("pop",readlinecb.getSingletonMethod("s_pop"));
        hist.defineSingletonMethod("to_a",readlinecb.getSingletonMethod("s_hist_to_a"));
    }
    
    // We lazily initialise this in case Readline.readline has been overriden in ruby (s_readline)
    protected static void initReadline() throws IOException {
        readline = new ConsoleReader();
        readline.setUseHistory(false);
        readline.setUsePagination(true);
        ((CandidateListCompletionHandler) readline.getCompletionHandler()).setAlwaysIncludeNewline(false);
        if (currentCompletor == null)
            currentCompletor = new RubyFileNameCompletor();
        readline.addCompletor(currentCompletor);
        history = readline.getHistory();
        readline.setHistory(history);
    }
    
    public static History getHistory() {
        return history;
    }
    
    public static void setCompletor(Completor completor) {
        if (readline != null) readline.removeCompletor(currentCompletor);
        currentCompletor = completor;
        if (readline != null) readline.addCompletor(currentCompletor);
    }
    
    public static Completor getCompletor() {
        return currentCompletor;
    }
    
    public static IRubyObject s_readline(IRubyObject recv, IRubyObject prompt, IRubyObject add_to_hist) throws IOException {
        if (readline == null) initReadline(); // not overridden, let's go
        IRubyObject line = recv.getRuntime().getNil();
        String v = readline.readLine(prompt.toString());
        if(null != v) {
            if (add_to_hist.isTrue())
                readline.getHistory().addToHistory(v);
            line = recv.getRuntime().newString(v);
        }
        return line;
    }

    public static IRubyObject s_push(IRubyObject recv, IRubyObject line) throws Exception {
        history.addToHistory(line.toString());
        return recv.getRuntime().getNil();
    }

    public static IRubyObject s_pop(IRubyObject recv) throws Exception {
        return recv.getRuntime().getNil();
    }
	
	public static IRubyObject s_hist_to_a(IRubyObject recv) throws Exception {
		RubyArray histList = recv.getRuntime().newArray();
		for (Iterator i = history.getHistoryList().iterator(); i.hasNext();) {
			histList.append(recv.getRuntime().newString((String) i.next()));
		}
		return histList;
	}
	
    public static IRubyObject s_set_completion_append_character(IRubyObject recv, IRubyObject achar) throws Exception {
        return recv.getRuntime().getNil();
    }

    public static IRubyObject s_set_completion_proc(IRubyObject recv, IRubyObject proc) throws Exception {
    	if (!proc.respondsTo("call"))
    		throw recv.getRuntime().newArgumentError("argument must respond to call");
		setCompletor(new ProcCompletor(proc));
        return recv.getRuntime().getNil();
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
            if (index != -1) buffer = buffer.substring(index + 1);
            ThreadContext context = procCompletor.getRuntime().getCurrentContext();
            
            IRubyObject comps = procCompletor.callMethod(context, "call", new IRubyObject[] { procCompletor.getRuntime().newString(buffer) }).callMethod(context, "to_a");
            if (comps instanceof List) {
                for (Iterator i = ((List) comps).iterator(); i.hasNext();) {
                    Object obj = i.next();
                    if (obj != null) candidates.add(obj.toString());
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
            if (index != -1) buffer = buffer.substring(index + 1);
            return index + 1 + super.complete(buffer, cursor, candidates);
        }
   	}
   	
}// Readline
