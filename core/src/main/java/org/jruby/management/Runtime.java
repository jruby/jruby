/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2007-2011 Nick Sieger <nicksieger@gmail.com>
 * Copyright (C) 2009 Joseph LaFata <joe@quibb.org>
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
 ***** END LICENSE BLOCK *****/
package org.jruby.management;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.SoftReference;

import org.jruby.Ruby;
import org.jruby.RubyException;
import org.jruby.RubyThread;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.backtrace.TraceType.Format;
import org.jruby.runtime.backtrace.TraceType.Gather;

public class Runtime implements RuntimeMBean {
    private final SoftReference<Ruby> ruby;

    public Runtime(Ruby ruby) {
        this.ruby = new SoftReference<Ruby>(ruby);
    }

    public int getExceptionCount() {
        return ruby.get().getExceptionCount();
    }

    public int getBacktraceCount() {
        return ruby.get().getBacktraceCount();
    }

    public int getCallerCount() {
        return ruby.get().getCallerCount();
    }

    public String threadDump() {
        return dumpThreads(Gather.NORMAL);
    }
    
    public String rawThreadDump() {
        return dumpThreads(Gather.RAW);
    }
    
    public String fullThreadDump() {
        return dumpThreads(Gather.FULL);
    }


    /**
     *
     * Dump all the threads that are known to ruby. We first discover any running
     * threads and then raise an exception in each thread adding the current thread
     * and it's context to the backtrace.
     *
     * @param gather The level of backtrace that get's raised in each thread
     * @return [String] A string represnetation of the threds that have been dumped with included backtrace.
     */
    public String dumpThreads(Gather gather) {
        Ruby ruby = this.ruby.get();
        RubyThread[] thrs = ruby.getThreadService().getActiveRubyThreads();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        pw.println("All threads known to Ruby instance " + ruby.hashCode());
        pw.println();
        
        for (RubyThread th : thrs) {
            dumpThread(ruby, th, gather, pw);
        }
        
        return sw.toString();
    }
    
    private static void dumpThread(Ruby ruby, RubyThread th, Gather gather, PrintWriter pw) {
        pw.println("Thread: " + th.getNativeThread().getName());
        pw.println("Stack:");
        ThreadContext tc = th.getContext();
        if (tc != null) {
            RubyException exc = new RubyException(ruby, ruby.getRuntimeError(), "thread dump");
            exc.setBacktraceData(gather.getBacktraceData(tc, th.getNativeThread().getStackTrace(), true));
            pw.println(Format.MRI.printBacktrace(exc, false));
        } else {
            pw.println("    [no longer alive]");
        }
        pw.println();
    }

    public String executeRuby(final String code) {
        final String[] result = new String[1];

        Thread t = new Thread() {

            @Override
            public void run() {
                // IRubyObject oldExc = ruby.get().getGlobalVariables().get("$!"); // Save $!
                try {
                    result[0] = ruby.get().evalScriptlet(code).toString();
                } catch (RaiseException re) {
                    result[0] = ruby.get().getInstanceConfig().getTraceType().printBacktrace(re.getException(), false);
                    // ruby.get().getGlobalVariables().set("$!", oldExc); // Restore $!
                } catch (Throwable t) {
                    StringWriter sw = new StringWriter();
                    t.printStackTrace(new PrintWriter(sw));
                    result[0] = sw.toString();
                }
            }
        };
        t.start();
        try {
            t.join();
        } catch (InterruptedException ie) {
            // ignore
        }

        return result[0];
    }
}
