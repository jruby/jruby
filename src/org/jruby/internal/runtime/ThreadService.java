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
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
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

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;
import java.util.concurrent.locks.ReentrantLock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import java.util.WeakHashMap;
import java.util.concurrent.Future;
import org.jruby.Ruby;
import org.jruby.RubyIO;
import org.jruby.RubyThread;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ThreadContext;

public class ThreadService {
    private Ruby runtime;
    private ThreadContext mainContext;
    private ThreadLocal<SoftReference<ThreadContext>> localContext;
    private ThreadGroup rubyThreadGroup;
    private Map<Object, RubyThread> rubyThreadMap;
    
    private ReentrantLock criticalLock = new ReentrantLock();
    private Map<RubyThread,ThreadContext> threadContextMap;

    public ThreadService(Ruby runtime) {
        this.runtime = runtime;
        this.mainContext = ThreadContext.newContext(runtime);
        this.localContext = new ThreadLocal<SoftReference<ThreadContext>>();

        try {
            this.rubyThreadGroup = new ThreadGroup("Ruby Threads#" + runtime.hashCode());
        } catch(SecurityException e) {
            this.rubyThreadGroup = Thread.currentThread().getThreadGroup();
        }

        this.rubyThreadMap = Collections.synchronizedMap(new WeakHashMap<Object, RubyThread>());
        this.threadContextMap = Collections.synchronizedMap(new WeakHashMap<RubyThread,ThreadContext>());
        
        // Must be called from main thread (it is currently, but this bothers me)
        localContext.set(new SoftReference<ThreadContext>(mainContext));
    }

    public void disposeCurrentThread() {
        localContext.set(null);
        rubyThreadMap.remove(Thread.currentThread());
    }

    /**
     * In order to provide an appropriate execution context for a given thread,
     * we store ThreadContext instances in a threadlocal. This method is a utility
     * to get at that threadlocal context from anywhere in the program it may
     * not be immediately available. This method should be used sparingly, and
     * if it is possible to pass ThreadContext on the argument list, it is
     * preferable.
     * 
     * <b>Description of behavior</b>
     * 
     * The threadlocal does not actually contain the ThreadContext directly;
     * instead, it contains a SoftReference that holds the ThreadContext. This
     * is to allow new threads to enter the system and execute Ruby code with
     * a valid context, but still allow that context to garbage collect if the
     * thread stays alive much longer. We use SoftReference here because
     * WeakReference is collected too quickly, resulting in very expensive
     * ThreadContext churn (and this originally lead to JRUBY-2261's leak of
     * adopted RubyThread instances).
     * 
     * @return The ThreadContext instance for the current thread, or a new one
     * if none has previously been created or the old ThreadContext has been
     * collected.
     */
    public ThreadContext getCurrentContext() {
        SoftReference sr = null;
        ThreadContext context = null;
        
        while (context == null) {
            // loop until a context is available, to clean up softrefs that might have been collected
            if ((sr = (SoftReference)localContext.get()) == null) {
                sr = adoptCurrentThread();
                context = (ThreadContext)sr.get();
            } else {
                context = (ThreadContext)sr.get();
            }
            
            // context is null, wipe out the SoftReference (this could be done with a reference queue)
            if (context == null) {
                localContext.set(null);
            }
        }

        return context;
    }
    
    private SoftReference adoptCurrentThread() {
        Thread current = Thread.currentThread();
        
        RubyThread.adopt(runtime.getThread(), current);
        
        return (SoftReference) localContext.get();
    }

    public RubyThread getMainThread() {
        return mainContext.getThread();
    }

    public void setMainThread(Thread thread, RubyThread rubyThread) {
        mainContext.setThread(rubyThread);
        threadContextMap.put(rubyThread, mainContext);
        rubyThreadMap.put(thread, rubyThread);
    }
    
    public synchronized RubyThread[] getActiveRubyThreads() {
    	// all threads in ruby thread group plus main thread

        synchronized(rubyThreadMap) {
            List<RubyThread> rtList = new ArrayList<RubyThread>(rubyThreadMap.size());
        
            for (Map.Entry<Object, RubyThread> entry : rubyThreadMap.entrySet()) {
                Object key = entry.getKey();
                if (key instanceof Thread) {
                    Thread t = (Thread)key;

                    // thread is not alive, skip it
                    if (!t.isAlive()) continue;
                } else if (key instanceof Future) {
                    Future f = (Future)key;

                    // future is done or cancelled, skip it
                    if (f.isDone() || f.isCancelled()) continue;
                }
            
                rtList.add(entry.getValue());
            }

            RubyThread[] rubyThreads = new RubyThread[rtList.size()];
            rtList.toArray(rubyThreads);
    	
            return rubyThreads;
        }
    }

    public Map getRubyThreadMap() {
        return rubyThreadMap;
    }
    
    public ThreadGroup getRubyThreadGroup() {
    	return rubyThreadGroup;
    }

    public ThreadContext getThreadContextForThread(RubyThread thread) {
        return threadContextMap.get(thread);
    }

    public synchronized ThreadContext registerNewThread(RubyThread thread) {
        ThreadContext context = ThreadContext.newContext(runtime);
        localContext.set(new SoftReference(context));
        threadContextMap.put(thread, context);
        context.setThread(thread);
        return context;
    }

    public synchronized void associateThread(Object threadOrFuture, RubyThread rubyThread) {
        rubyThreadMap.put(threadOrFuture, rubyThread);
    }

    public synchronized void dissociateThread(Object threadOrFuture) {
        rubyThreadMap.remove(threadOrFuture);
    }
    
    public synchronized void unregisterThread(RubyThread thread) {
        rubyThreadMap.remove(Thread.currentThread());
        threadContextMap.remove(thread);
        getCurrentContext().setThread(null);
        localContext.set(null);
    }
    
    public void setCritical(boolean critical) {
        if (critical && !criticalLock.isHeldByCurrentThread()) {
            criticalLock.lock();
        } else if (!critical && criticalLock.isHeldByCurrentThread()) {
            criticalLock.unlock();
        }
    }
    
    public boolean getCritical() {
        return criticalLock.isHeldByCurrentThread();
    }
    
    public static class Event {
        public enum Type { KILL, RAISE, WAKEUP }
        public final RubyThread sender;
        public final RubyThread target;
        public final Type type;
        public final IRubyObject exception;

        public Event(RubyThread sender, RubyThread target, Type type) {
            this(sender, target, type, null);
        }

        public Event(RubyThread sender, RubyThread target, Type type, IRubyObject exception) {
            this.sender = sender;
            this.target = target;
            this.type = type;
            this.exception = exception;
        }
    }

    public synchronized void deliverEvent(Event event) {
        // first, check if the sender has unreceived mail
        event.sender.checkMail(getCurrentContext());

        // then deliver mail to the target
        event.target.receiveMail(event);
    }
}
