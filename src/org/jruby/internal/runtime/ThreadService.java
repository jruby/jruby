/*
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License or
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License and GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public
 * License and GNU Lesser General Public License along with JRuby;
 * if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 *
 */
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
    		if (critical) return;
    		
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
    		if (!critical) return;
    		
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
    
    /**
	 * @param activeThreads
	 * @param i
	 * @return
	 */
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
