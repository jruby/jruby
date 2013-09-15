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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.internal.runtime;

import java.lang.ref.SoftReference;
import java.util.concurrent.locks.ReentrantLock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import java.util.WeakHashMap;
import java.util.concurrent.Future;
import org.jruby.Ruby;
import org.jruby.RubyThread;
import org.jruby.ext.fiber.ThreadFiber;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ThreadContext;

/**
 * ThreadService maintains lists ofall the JRuby-specific thread data structures
 * needed for Ruby's threading API and for JRuby's execution. The main
 * structures are:
 *
 * <ul>
 * <li>ThreadContext, which contains frames, scopes, etc needed for Ruby execution</li>
 * <li>RubyThread, the Ruby object representation of a thread's state</li>
 * <li>RubyThreadGroup, which represents a group of Ruby threads</li>
 * <li>NativeThread, used to wrap native Java threads</li>
 * <li>FutureThread, used to wrap java.util.concurrent.Future</li>
 * </ul>
 *
 * In order to ensure these structures do not linger after the thread has terminated,
 * most of them are either weakly or softly referenced. The references associated
 * with these structures are:
 *
 * <ul>
 * <li>ThreadService has a hard reference to a ThreadLocal, which holds a soft reference
 * to a ThreadContext. So the thread's locals softly reference ThreadContext.
 * We use a soft reference to keep ThreadContext instances from going away too
 * quickly when a Java thread leaves Ruby space completely, which would otherwise
 * result in a lot of ThreadContext object churn.</li>
 * <li>ThreadService maintains a weak map from the actual java.lang.Thread (or
 * java.util.concurrent.Future) instance to the associated RubyThread. The map
 * is weak-keyyed, so it will not prevent the collection of the associated
 * Thread or Future. The associated RubyThread will remain alive as long as the
 * Thread/Future and this ThreadService instance are both alive, maintaining
 * the external thread's identity in Ruby-land.</li>
 * <li>RubyThread has a weak reference to its to ThreadContext.</li>
 * <li>ThreadContext has a hard reference to its associated RubyThread. Ignoring other
 * references, this will usually mean RubyThread is softly reachable via the
 * soft threadlocal reference to ThreadContext in ThreadService.</li>
 * <li>RubyThreadGroup has hard references to threads it owns. The thread removes
 * itself on termination (if it's a Ruby thread) or when the ThreadContext is
 * collected (as in the case of "adopted" Java threads.</li>
 * </ul>
 *
 * These data structures can come to life in one of two ways:
 *
 * <ul>
 * <li>A Ruby thread is started. This constructs a new RubyThread object, which
 * calls to ThreadService to initialize a ThreadContext and appropriate mappings
 * in all ThreadService's structures. The body of the thread is wrapped with a
 * finally block that will forcibly unregister the thread and all related
 * structures from ThreadService.</li>
 * <li>A Java thread enters Ruby by doing a call. The thread is "adopted", and
 * gains a RubyThread instance, a ThreadContext instance, and all associated
 * mappings in ThreadService. Since we don't know when the thread has "left"
 * Ruby permanently, no forcible unregistration is attempted for the various
 * structures and maps. However, they should not be hard-rooted; the
 * ThreadContext is only softly reachable at best if no calls are in-flight,
 * so it will collect. Its collection will release the reference to RubyThread,
 * and its finalizer will unregister that RubyThread from its RubyThreadGroup.
 * With the RubyThread gone, the Thread-to-RubyThread map will eventually clear,
 * releasing the hard reference to the Thread itself.</li>
 * <ul>
 */
public class ThreadService {
    private Ruby runtime;
    /**
     * A hard reference to the "main" context, so we always have one waiting for
     * "main" thread execution.
     */
    private ThreadContext mainContext;

    /**
     * A thread-local soft reference to the current thread's ThreadContext. We
     * use a soft reference so that the ThreadContext is still collectible but
     * will not immediately disappear once dereferenced, to avoid churning
     * through ThreadContext instances every time a Java thread enters and exits
     * Ruby space.
     */
    private ThreadLocal<SoftReference<ThreadContext>> localContext;

    /**
     * The Java thread group into which we register all Ruby threads. This is
     * distinct from the RubyThreadGroup, which is simply a mutable collection
     * of threads.
     */
    private ThreadGroup rubyThreadGroup;

    /**
     * A map from a Java Thread or Future to its RubyThread instance. This is
     * a synchronized WeakHashMap, so it weakly references its keys; this means
     * that when the Thread/Future goes away, eventually its entry in this map
     * will follow.
     */
    private final Map<Object, RubyThread> rubyThreadMap;
    
    private final ReentrantLock criticalLock = new ReentrantLock();

    public ThreadService(Ruby runtime) {
        this.runtime = runtime;
        this.localContext = new ThreadLocal<SoftReference<ThreadContext>>();

        try {
            this.rubyThreadGroup = new ThreadGroup("Ruby Threads#" + runtime.hashCode());
        } catch(SecurityException e) {
            this.rubyThreadGroup = Thread.currentThread().getThreadGroup();
        }

        this.rubyThreadMap = Collections.synchronizedMap(new WeakHashMap<Object, RubyThread>());
    }

    public void disposeCurrentThread() {
        localContext.set(null);
        rubyThreadMap.remove(Thread.currentThread());
    }

    public void initMainThread() {
        this.mainContext = ThreadContext.newContext(runtime);

        // Must be called from main thread (it is currently, but this bothers me)
        localContext.set(new SoftReference<ThreadContext>(mainContext));
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

    /*
     * Used only for Fiber context management
     */
    public void setCurrentContext(ThreadContext context) {
        localContext.set(new SoftReference<ThreadContext>(context));
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
        rubyThreadMap.put(thread, rubyThread);
    }
    
    public synchronized RubyThread[] getActiveRubyThreads() {
    	// all threads in ruby thread group plus main thread

        synchronized(rubyThreadMap) {
            List<RubyThread> rtList = new ArrayList<RubyThread>(rubyThreadMap.size());
        
            for (Map.Entry<Object, RubyThread> entry : rubyThreadMap.entrySet()) {
                Object key = entry.getKey();
                if (key == null) continue;
                
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

    public ThreadGroup getRubyThreadGroup() {
    	return rubyThreadGroup;
    }

    public ThreadContext getThreadContextForThread(RubyThread thread) {
        return thread.getContext();
    }

    public synchronized ThreadContext registerNewThread(RubyThread thread) {
        ThreadContext context = ThreadContext.newContext(runtime);
        localContext.set(new SoftReference(context));
        context.setThread(thread);
        if (runtime.is1_9()) ThreadFiber.initRootFiber(context); // may be overwritten by fiber
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
        getCurrentContext().setThread(null);
        localContext.set(null);
    }
    
    public void setCritical(boolean critical) {
        if (critical && !criticalLock.isHeldByCurrentThread()) {
            acquireCritical();
        } else if (!critical && criticalLock.isHeldByCurrentThread()) {
            releaseCritical();
        }
    }

    private void acquireCritical() {
        criticalLock.lock();
    }

    private void releaseCritical() {
        criticalLock.unlock();
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
        
        public String toString() {
            switch (type) {
                case KILL: return sender.toString() + " sent KILL to " + target;
                case RAISE: return sender.toString() + " sent RAISE to " + target + ": " + exception.getMetaClass().getRealClass();
                case WAKEUP: return sender.toString() + " sent WAKEUP to " + target;
            }
            return ""; // not reached
        }
    }

    public void deliverEvent(Event event) {
        // first, check if the sender has unreceived mail
        event.sender.checkMail(getCurrentContext());

        // then deliver mail to the target
        event.target.receiveMail(event);
    }

    /**
     * Get the map from threadlike objects to RubyThread instances. Used mainly
     * for testing purposes.
     *
     * @return The ruby thread map
     */
    public Map<Object, RubyThread> getRubyThreadMap() {
        return rubyThreadMap;
    }
}
