/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
        t.setDaemon(true);
        t.start();
    }

}
