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
 * Copyright (C) 2010 Wayne Meissner
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

package org.jruby.cext;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
abstract class Cleaner extends WeakReference<Object> {
    private static ReferenceQueue<Object> queue = new ReferenceQueue<Object>();
    private static Cleaner list = null;
    
    private Cleaner prev, next;


    Cleaner(Object obj) {
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
                Reference ref; // routing around generics here: IBM J9 doesn't like it
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
                            r.clear();
                        }
                    } while((ref = queue.poll()) != null);
                } finally {
                    GIL.releaseNoCleanup();
                }
            }
        }
    };

    static {
        Thread t = new Thread(reaper, "JRuby C extension cleanup thread");
        t.setPriority(Thread.MAX_PRIORITY);
        t.setDaemon(true);
        t.start();
    }

}
