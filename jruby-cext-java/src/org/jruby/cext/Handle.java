/*
 * Copyright (C) 2008, 2009 Wayne Meissner
 *
 * This file is part of jruby-cext.
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jruby.cext;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public final class Handle extends WeakReference<Object> {
    private static final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<Object>();
    private static final Thread reaperThread;
    private static volatile Handle allHandles = null;
    
    private final Ruby runtime;
    private final long address;
    private Handle prev = null, next = null;

    
    private List<IRubyObject> linkedObjects = null;

    static Handle newHandle(Ruby runtime, Object rubyObject, long nativeHandle) {
        Handle h = new Handle(runtime, rubyObject, nativeHandle);

        synchronized (Handle.class) {
            if (allHandles != null) {
                h.next = allHandles;
                allHandles.prev = h;
            }
            allHandles = h;
        }

        return h;
    }
    
    private Handle(Ruby runtime, Object rubyObject, long address) {
        super(rubyObject, referenceQueue);
        this.runtime = runtime;
        this.address = address;
    }
    
    public final long getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Handle other = (Handle) obj;
        return this.address == other.address;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + (int) (this.address ^ (this.address >>> 32));
        return hash;
    }

    @Override
    public String toString() {
        return "Native ruby object " + Long.toString(address);
    }


    void link(List<IRubyObject> fields) {
        this.linkedObjects = new ArrayList<IRubyObject>(fields);
    }

    public static final synchronized Handle valueOf(IRubyObject obj) {
        Handle h = GC.lookup(obj);
        if (h != null) {
            return h;
        }

        Ruby runtime = obj.getRuntime();
        long nativeHandle;
        if (obj instanceof RubyFixnum) {
            nativeHandle = Native.getInstance(runtime).newFixnumHandle(obj, ((RubyFixnum) obj).getLongValue());
        } else {
            nativeHandle = Native.getInstance(runtime).newHandle(obj);
        }
        Handle handle = newHandle(runtime, obj, nativeHandle);

        GC.register(obj, handle);

        return handle;
    }

    public static long nativeHandle(IRubyObject obj) {
        return Handle.valueOf(obj).getAddress();
    }

    private static final Runnable reaper = new Runnable() {

        public void run() {
            for ( ; ; ) {
                try {
                    Reference<? extends Object> r = referenceQueue.remove();
                    try {
                        if (r instanceof Handle) {
                            final Handle h = (Handle) r;
                            synchronized (Handle.class) {
                                if (h.prev != null) {
                                    h.prev.next = h.next;
                                }
                                if (h.next != null) {
                                    h.next.prev = h.prev;
                                }

                                if (h == allHandles) {
                                    if (h.next != null) {
                                        allHandles = h.next;
                                    } else {
                                        allHandles = h.prev;
                                    }
                                }
                            }
                            h.prev = h.next = null;
                            
                            ThreadContext context = h.runtime.getCurrentContext();
                            ExecutionLock.lock(context);
                            try {
                                Native.getInstance(h.runtime).freeHandle(h.address);
                            } finally {
                                ExecutionLock.unlockNoCleanup(context);
                            }
                            
                            
                        }
                    } finally {
                        r.clear();
                    }
                } catch (InterruptedException ex) {
                    break;
                } catch (Throwable t) {
                    continue;
                }
            }
        }
    };
    static {
        reaperThread = new Thread(reaper, "Native Handle Reaper");
        reaperThread.setDaemon(true);
        reaperThread.setPriority(Thread.NORM_PRIORITY + 1);
        reaperThread.start();
    }

}
