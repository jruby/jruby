/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.internal.runtime;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.RubyThread;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.MainExitException;
import org.jruby.exceptions.ThreadKill;
import org.jruby.ir.runtime.IRBreakJump;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import static org.jruby.api.Access.instanceConfig;

public class RubyRunnable implements ThreadedRunnable {

    private static final Logger LOG = LoggerFactory.getLogger(RubyRunnable.class);

    private final Ruby runtime;
    private final RubyProc proc;
    private final IRubyObject[] arguments;
    private final int callInfo;
    private final RubyThread rubyThread;

    // nulled out when run
    private volatile ThreadContext creatorContext;

    private Thread javaThread;
    private static boolean warnedAboutTC = false;

    public RubyRunnable(RubyThread rubyThread, ThreadContext creatorContext, IRubyObject[] args, Block currentBlock, int callInfo) {
        this.rubyThread = rubyThread;
        this.runtime = rubyThread.getRuntime();

        proc = runtime.newProc(Block.Type.THREAD, currentBlock);
        this.arguments = args;
        this.callInfo = callInfo;
        this.creatorContext = creatorContext;
    }

    @Deprecated(since = "1.7.5")
    public RubyThread getRubyThread() {
        return rubyThread;
    }

    public Thread getJavaThread() {
        return javaThread;
    }

    @Override
    public void run() {
        javaThread = Thread.currentThread();
        ThreadContext context = runtime.getThreadService().registerNewThread(rubyThread);
        context.callInfo = callInfo;

        if (creatorContext != null && creatorContext.getFiber() != null) {
            context.getFiber().inheritFiberStorage(creatorContext);
        }
        creatorContext = null;

        // set thread context JRuby classloader here, for Ruby-owned thread
        ClassLoader oldContextClassLoader = null;
        try {
            oldContextClassLoader = javaThread.getContextClassLoader();
            javaThread.setContextClassLoader(runtime.getJRubyClassLoader());
        } catch (SecurityException se) {
            // can't set TC classloader
            if (!warnedAboutTC && instanceConfig(context).isVerbose()) {
                warnedAboutTC = true;
                LOG.info("WARNING: Security restrictions disallowed setting context classloader for Ruby threads.");
            }
        }

        rubyThread.beforeStart();

        // uber-ThreadKill catcher, since it should always just mean "be dead"
        try {
            // Push a frame for the toplevel of the thread

            // Call the thread's code
            Block threadBlock = proc.getBlock();
            RubyModule frameClass = threadBlock.getFrame().getKlazz();
            String file = threadBlock.getBinding().getFile();
            int line = threadBlock.getBinding().getLine();
            try {
                IRRuntimeHelpers.callTrace(context, frameClass, RubyEvent.THREAD_BEGIN, null, file, line);
                IRubyObject result = proc.call(context, arguments);
                IRRuntimeHelpers.callTrace(context, frameClass, RubyEvent.THREAD_END, null, file, line);
                rubyThread.cleanTerminate(result);
            } catch (MainExitException mee) {
                // Someone called exit!, so we need to kill the main thread
                runtime.getThreadService().getMainThread().kill();
            } catch (IRBreakJump irbj) {
                rubyThread.exceptionRaised(Helpers.newLocalJumpErrorForBreak(runtime, irbj.breakValue));
            } catch (Throwable t) {
                rubyThread.exceptionRaised(t);
            } finally {
                rubyThread.dispose();

                // restore context classloader, in case we're using a thread pool
                try {
                    javaThread.setContextClassLoader(oldContextClassLoader);
                } catch (SecurityException se) {
                    // can't set TC classloader
                    if (!warnedAboutTC && instanceConfig(context).isVerbose()) {
                        warnedAboutTC = true;
                        LOG.info("WARNING: Security restrictions disallowed setting context classloader for Ruby threads.");
                    }
                }

                // dump profile, if any
                if (instanceConfig(context).isProfilingEntireRun()) {
                    runtime.printProfileData(context.getProfileCollection());
                }
            }
        } catch (ThreadKill tk) {
            // be dead
        }
    }
}
