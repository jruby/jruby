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
import java.util.concurrent.atomic.AtomicLong;
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
    private final Ruby runtime;
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

    private final AtomicLong threadCount = new AtomicLong(0);

    public ThreadService(final Ruby runtime) {
        this.runtime = runtime;
        this.localContext = new ThreadLocal<>();

        try {
            this.rubyThreadGroup = new ThreadGroup("Ruby Threads#" + runtime.hashCode());
        } catch(SecurityException e) {
            this.rubyThreadGroup = Thread.currentThread().getThreadGroup();
        }

        this.rubyThreadMap = Collections.synchronizedMap(new WeakHashMap<Object, RubyThread>());
    }

    @Deprecated
    public void teardown() {
        tearDown();
    }

    /**
     * @see Ruby#tearDown(boolean)
     */
    public void tearDown() {
        // clear main context reference
        mainContext = null;

        // clear all thread-local context references
        localContext = new ThreadLocal<>();

        // clear thread map
        rubyThreadMap.clear();
    }

    public void exceptionRaised(RubyThread thread, Throwable ex) {
        if (mainContext != null) thread.exceptionRaised(ex); // only propagate if runtime hasn't shutdown
    }

    public void initMainThread() {
        this.mainContext = ThreadContext.newContext(runtime);

        // Must be called from main thread (it is currently, but this bothers me)
        localContext.set(new SoftReference<>(mainContext));
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
    public final ThreadContext getCurrentContext() {
        SoftReference<ThreadContext> ref = localContext.get();
        if (ref == null) {
            adoptCurrentThread(); // registerNewThread will localContext.set(...)
            ref = localContext.get();
        }
        ThreadContext context;
        if ((context = ref.get()) != null) {
            return context;
        }

        // context is null, wipe out the SoftReference (this could be done with a reference queue)
        localContext.set(null);

        // go until a context is available, to clean up soft-refs that might have been collected
        return getCurrentContext();
    }

    private RubyThread adoptCurrentThread() {
        return RubyThread.adopt(runtime, this, Thread.currentThread());
    }

    public ThreadContext registerNewThread(RubyThread thread) {
        assert thread.getContext() == null;
        ThreadContext context = ThreadContext.newContext(runtime);
        context.setThread(thread);
        ThreadFiber.initRootFiber(context, thread);
        localContext.set(new SoftReference<>(context));
        return context;
    }

    public RubyThread getMainThread() {
        return mainContext.getThread();
    }

    public void setMainThread(Thread thread, RubyThread rubyThread) {
        mainContext.setThread(rubyThread);
        rubyThreadMap.put(thread, rubyThread);
    }

    public RubyThread[] getActiveRubyThreads() {
    	// all threads in ruby thread group plus main thread
        ArrayList<RubyThread> rtList;
        synchronized(rubyThreadMap) {
            rtList = new ArrayList<>(rubyThreadMap.size());

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
        }
        return rtList.toArray(new RubyThread[rtList.size()]);
    }

    public void associateThread(Thread thread, RubyThread rubyThread) {
        rubyThreadMap.put(thread, rubyThread); // synchronized
    }

    public void unregisterThread(RubyThread thread) {
        // NOTE: previously assumed thread.getNativeThread() == Thread.currentThread()
        unregisterThreadImpl(thread.getContext(), thread.getNativeThread());
    }

    public void unregisterCurrentThread(ThreadContext context) {
        unregisterThreadImpl(context, Thread.currentThread());
    }

    private void unregisterThreadImpl(ThreadContext context, Thread nativeThread) {
        rubyThreadMap.remove(nativeThread); // synchronized

        if (context != null) {
            RubyThread thread = context.getThread();
            context.setThread(null);
            if (thread != null) thread.clearContext(); // help GC - clear context-ref
        }

        SoftReference<ThreadContext> ref = localContext.get();
        if (ref != null) ref.clear(); // help GC
        localContext.remove();
    }

    @Deprecated // use unregisterCurrentThread
    public void disposeCurrentThread() {
        unregisterCurrentThread(getCurrentContext());
    }

    public long incrementAndGetThreadCount() {
        return threadCount.incrementAndGet();
    }

    @Deprecated
    public Map<Object, RubyThread> getRubyThreadMap() {
        return rubyThreadMap;
    }

    @Deprecated
    public void deliverEvent(RubyThread sender, RubyThread target, Event event) {
    }

    @Deprecated
    public ThreadGroup getRubyThreadGroup() {
        return rubyThreadGroup;
    }

    @Deprecated
    public ThreadContext getThreadContextForThread(RubyThread thread) {
        return thread.getContext();
    }

    @Deprecated
    public synchronized void dissociateThread(Object threadOrFuture) {
        rubyThreadMap.remove(threadOrFuture);
    }

    @Deprecated
    public final void setCurrentContext(ThreadContext context) {
        localContext.set(new SoftReference<ThreadContext>(context));
    }

    @Deprecated
    public boolean getPolling() {
        return rubyThreadMap.size() > 1;
    }

    @Deprecated
    public static class Event {
        public enum Type { KILL, RAISE, WAKEUP }
        public final String description;
        public final Type type;
        public final IRubyObject exception;

        public Event(String description, Type type) {
            this(description, type, null);
        }

        public Event(String description, Type type, IRubyObject exception) {
            this.description = description;
            this.type = type;
            this.exception = exception;
        }

        public String toString() {
            switch (type) {
                case KILL: return description;
                case RAISE: return description + ": " + exception.getMetaClass().getRealClass();
                case WAKEUP: return description;
            }
            return ""; // not reached
        }

        @Deprecated
        public static Event kill(RubyThread sender, RubyThread target, Type type) {
            return new Event(sender.toString() + " sent KILL to " + target, type);
        }

        @Deprecated
        public static Event raise(RubyThread sender, RubyThread target, Type type, IRubyObject exception) {
            return new Event(sender.toString() + " sent KILL to " + target, type, exception);
        }

        @Deprecated
        public static Event wakeup(RubyThread sender, RubyThread target, Type type) {
            return new Event(sender.toString() + " sent KILL to " + target, type);
        }
    }

    @Deprecated
    public void setCritical(boolean critical) {
        if (critical && !criticalLock.isHeldByCurrentThread()) {
            acquireCritical();
        } else if (!critical && criticalLock.isHeldByCurrentThread()) {
            releaseCritical();
        }
    }

    @Deprecated
    private void acquireCritical() {
        criticalLock.lock();
    }

    @Deprecated
    private void releaseCritical() {
        criticalLock.unlock();
    }

    @Deprecated
    public boolean getCritical() {
        return criticalLock.isHeldByCurrentThread();
    }
}
