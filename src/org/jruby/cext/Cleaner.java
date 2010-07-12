/*
 * Copyright (C) 2010 Wayne Meissner
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
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
abstract class Cleaner extends WeakReference<IRubyObject> {
    private static ReferenceQueue<IRubyObject> queue = new ReferenceQueue<IRubyObject>();
    private static Cleaner list = null;
    
    private Cleaner prev, next;


    Cleaner(IRubyObject obj) {
        super(obj, queue);
    }

    static void register(Cleaner cleaner) {
        if (list != null) {
            cleaner.next = list;
            list.prev = cleaner;
        }
        list = cleaner;
    }
    
    abstract void dispose();

    private static final Runnable reaper = new Runnable() {

        public void run() {
            while (true) {
                Reference<? extends IRubyObject> ref;
                try {
                     ref = queue.remove();
                } catch (InterruptedException ex) {
                    break;
                }
                GIL.acquire();
                try {
                    do {
                        if (ref instanceof Cleaner) {
                            Cleaner r = (Cleaner) ref;

                            if (r.next != null) {
                                r.next.prev = r.prev;
                            }
                            if (r.prev != null) {
                                r.prev.next = r.next;
                            }
                            if (r == list) {
                                if (list.next != null) {
                                    list = list.next;
                                } else {
                                    list = list.prev;
                                }
                            }
                            r.prev = r.next = null;
                            r.dispose();
                        }
                    } while((ref = queue.poll()) != null);
                } finally {
                    GIL.releaseNoCleanup();
                }
            }
        }
    };

    static {
        Thread t = new Thread(reaper);
        t.setPriority(Thread.MAX_PRIORITY);
        t.setDaemon(true);
        t.start();
    }

}
