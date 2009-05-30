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
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
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
package org.jruby.internal.runtime;

import org.jruby.Ruby;
import org.jruby.RubyProc;
import org.jruby.RubyThread;
import org.jruby.RubyThreadGroup;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.MainExitException;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.ThreadKill;
import org.jruby.runtime.Block;
import org.jruby.runtime.Frame;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyRunnable implements Runnable {
    private Ruby runtime;
    private RubyProc proc;
    private IRubyObject[] arguments;
    private RubyThread rubyThread;

    /** Frames at thread construction time, to produce a good contextual backtrace **/
    private Frame[] currentFrames;
    
    private Thread javaThread;
    private static boolean warnedAboutTC = false;
    
    public RubyRunnable(RubyThread rubyThread, IRubyObject[] args, Frame[] frames, Block currentBlock) {
        this.rubyThread = rubyThread;
        this.runtime = rubyThread.getRuntime();
        
        proc = runtime.newProc(Block.Type.THREAD, currentBlock);
        this.currentFrames = frames;
        this.arguments = args;
    }
    
    public RubyThread getRubyThread() {
        return rubyThread;
    }
    
    public Thread getJavaThread() {
        return javaThread;
    }
    
    public void run() {
        javaThread = Thread.currentThread();
        ThreadContext context = runtime.getThreadService().registerNewThread(rubyThread);
        
        // set thread context JRuby classloader here, for Ruby-owned thread
        ClassLoader oldContextClassLoader = null;
        try {
            oldContextClassLoader = javaThread.getContextClassLoader();
            javaThread.setContextClassLoader(runtime.getJRubyClassLoader());
        } catch (SecurityException se) {
            // can't set TC classloader
            if (!warnedAboutTC && runtime.getInstanceConfig().isVerbose()) {
                System.err.println("WARNING: Security restrictions disallowed setting context classloader for Ruby threads.");
            }
        }
        
        context.preRunThread(currentFrames);

        // uber-ThreadKill catcher, since it should always just mean "be dead"
        try {
            // Call the thread's code
            try {
                IRubyObject result = proc.call(context, arguments);
                rubyThread.cleanTerminate(result);
            } catch (JumpException.ReturnJump rj) {
                rubyThread.exceptionRaised(runtime.newThreadError("return can't jump across threads"));
            } catch (RaiseException e) {
                rubyThread.exceptionRaised(e);
            } catch (MainExitException mee) {
                // Someone called exit!, so we need to kill the main thread
                runtime.getThreadService().getMainThread().kill();
            } finally {
                rubyThread.beDead();
                runtime.getThreadService().setCritical(false);
                runtime.getThreadService().unregisterThread(rubyThread);

                ((RubyThreadGroup)rubyThread.group()).remove(rubyThread);

                // restore context classloader, in case we're using a thread pool
                try {
                    javaThread.setContextClassLoader(oldContextClassLoader);
                } catch (SecurityException se) {
                    // can't set TC classloader
                    if (!warnedAboutTC && runtime.getInstanceConfig().isVerbose()) {
                        System.err.println("WARNING: Security restrictions disallowed setting context classloader for Ruby threads.");
                    }
                }
            }
        } catch (ThreadKill tk) {
            // be dead
        }
    }
}
