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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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
import org.jruby.RubyThread;
import org.jruby.runtime.ThreadContext;

public class ThreadService {
    private Ruby runtime;
    private ThreadContext mainContext = new ThreadContext(runtime);
    private ThreadContextLocal localContext = new ThreadContextLocal(mainContext);
    private ThreadGroup rubyThreadGroup;
    private volatile boolean critical;

    public ThreadService(Ruby runtime) {
        this.runtime = runtime;
        this.mainContext = new ThreadContext(runtime);
        this.localContext = new ThreadContextLocal(mainContext);
        this.rubyThreadGroup = new ThreadGroup("Ruby Threads#" + runtime.hashCode());
    }

    public void disposeCurrentThread() {
        localContext.dispose();
    }

    public ThreadContext getCurrentContext() {
        return (ThreadContext) localContext.get();
    }

    public RubyThread getMainThread() {
        return mainContext.getThread();
    }

    public void setMainThread(RubyThread thread) {
        mainContext.setThread(thread);
    }
    
    public RubyThread[] getActiveRubyThreads() {
    	// all threads in ruby thread group plus main thread
    	Thread[] threads = new Thread[rubyThreadGroup.activeCount() + 1];
    	RubyThread[] rubyThreads = new RubyThread[threads.length];
    	
    	rubyThreadGroup.enumerate(threads);
    	for (int i = 0; i < threads.length; i++) {
    		rubyThreads[i] = getRubyThreadFromThread(threads[i]);
    	}
    	
    	return rubyThreads;
    }
    
    public ThreadGroup getRubyThreadGroup() {
    	return rubyThreadGroup;
    }

    public void registerNewThread(RubyThread thread) {
        localContext.set(new ThreadContext(runtime));
        getCurrentContext().setThread(thread);
    }
    
    public synchronized void setCritical(boolean critical) {
    	// TODO: this implementation is obviously dependent on native threads
    	if (this.critical) {
    		if (critical) {
				return;
			}
    		this.critical = false;
    		
    		Thread[] activeThreads = new Thread[rubyThreadGroup.activeCount()];
    		rubyThreadGroup.enumerate(activeThreads);
    		
    		// unsuspend all threads, starting with main if necessary
    		if (getCurrentContext() != mainContext) {
    			mainContext.getThread().decriticalize();
    		}
    		
    		for (int i = 0; i < activeThreads.length; i++) {
    			RubyThread rubyThread = getRubyThreadFromThread(activeThreads[i]);
    			
    			rubyThread.decriticalize();
    		}
    	} else {
    		if (!critical) {
				return;
			}
    		this.critical = true;
    		
    		Thread[] activeThreads = new Thread[rubyThreadGroup.activeCount()];
    		rubyThreadGroup.enumerate(activeThreads);
    		AtomicSpinlock spinlock = new AtomicSpinlock();

    		// suspend all threads, starting with main if necessary
    		if (getCurrentContext() != mainContext) {
    			mainContext.getThread().criticalize(spinlock);
    		}
    		
    		for (int i = 0; i < activeThreads.length; i++) {
    			RubyThread rubyThread = null;
    			rubyThread = getRubyThreadFromThread(activeThreads[i]);

    			if (rubyThread != getCurrentContext().getThread()) {
    				rubyThread.criticalize(spinlock);
    			}
    		}
    		
    		try {
    			spinlock.waitForZero(1000);
    		} catch (InterruptedException ie) {
    			// TODO: throw something more appropriate
    			throw new RuntimeException(ie);
    		}
    	}
    }
    
	private RubyThread getRubyThreadFromThread(Thread activeThread) {
		RubyThread rubyThread;
		if (activeThread instanceof RubyNativeThread) {
			RubyNativeThread rubyNativeThread = (RubyNativeThread)activeThread;
			rubyThread = rubyNativeThread.getRubyThread();
		} else {
			// main thread
			rubyThread = mainContext.getThread();
		}
		return rubyThread;
	}

	public boolean getCritical() {
    	return critical;
    }

    private static class ThreadContextLocal extends ThreadLocal {
        private ThreadContext mainContext;

        public ThreadContextLocal(ThreadContext mainContext) {
            this.mainContext = mainContext;
        }

        /**
         * @see java.lang.ThreadLocal#initialValue()
         */
        protected Object initialValue() {
            return this.mainContext;
        }

        public void dispose() {
            this.mainContext = null;
            set(null);
        }
    }

}
