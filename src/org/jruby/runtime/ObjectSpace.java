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
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2006 Charles O Nutter <headius@headius.com>
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
package org.jruby.runtime;

import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.WeakIdentityHashMap;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.jruby.RubyNumeric;

/**
 * FIXME: This version is faster than the previous, but both suffer from a
 * crucial flaw: It is impossible to create an ObjectSpace with an iterator
 * that doesn't either: a. hold on to objects that might otherwise be collected
 * or b. have no way to guarantee that a call to hasNext() will be correct or
 * that a subsequent call to next() will produce an object. For our purposes,
 * for now, this may be acceptable.
 */
public class ObjectSpace {
    private ReferenceQueue deadReferences = new ReferenceQueue();
    private WeakReferenceListNode top;

    private ReferenceQueue deadIdentityReferences = new ReferenceQueue();
    private final Map identities = new HashMap();
    private final Map identitiesByObject = new WeakIdentityHashMap();

    private long maxId = 4; // Highest reserved id

    public long idOf(IRubyObject rubyObject) {
        synchronized (identities) {
            Long longId = (Long) identitiesByObject.get(rubyObject);
            if (longId == null) {
                longId = createId(rubyObject);
            }
            return longId.longValue();
        }
    }

    private Long createId(IRubyObject object) {
        cleanIdentities();
        maxId += 2; // id must always be even
        Long longMaxId = new Long(maxId);
        identities.put(longMaxId, new IdReference(object, maxId, deadIdentityReferences));
        identitiesByObject.put(object, longMaxId);
        return longMaxId;
    }

    public IRubyObject id2ref(long id) {
        synchronized (identities) {
            cleanIdentities();
            IdReference reference = (IdReference) identities.get(new Long(id));
            if (reference == null)
                return null;
            return (IRubyObject) reference.get();
        }
    }

    private void cleanIdentities() {
        IdReference ref;
        while ((ref = (IdReference) deadIdentityReferences.poll()) != null)
            identities.remove(new Long(ref.id()));
    }

    private Map finalizableWeakRefs          = new ConcurrentHashMap();
    private Map weakRefs            = new ConcurrentHashMap();
    private LinkedBlockingQueue finalizersToRun   = new LinkedBlockingQueue();
    private FinalizerThread finalizerThread = new FinalizerThread();

    private class FinalizerThread extends Thread {
        private boolean running;
        public FinalizerThread() {
            super("Ruby Finalizer Thread");
            setDaemon(true);
        }

        public void run() {
            while (true) {
                Runnable finalizer;
                try {
                    while ((finalizer = (Runnable) finalizersToRun.poll(100, TimeUnit.MILLISECONDS)) != null) {
                        try {
                            finalizer.run();
                        } catch (Exception e) {
                        }
                    }
                } catch (InterruptedException ie) {
                    break;
                }
                synchronized (this) {
                    if (finalizersToRun.peek() == null && !running) {
                        break;
                    }
                }
            }
        }

        public synchronized void startRunning() {
            if(!running) {
                running = true;
                start();
            }
        }

        public synchronized void stopRunning() {
            running = false;
        }
    }

    private class FinalizerEntry implements Runnable {
        private long id;
        private RubyProc proc;
        private IRubyObject fid;
        public FinalizerEntry(long id, RubyProc proc) {
            this.id = id;
            this.proc = proc;
            this.fid = proc.getRuntime().newFixnum(id);
        }

        public void _finalize() {
            finalizersToRun.offer(this);
        }

        public void run() {
            proc.call(new IRubyObject[]{fid});
        }
    }

    public void finishFinalizers() {
        for(Iterator iter = finalizableWeakRefs.keySet().iterator();iter.hasNext();) {
            Object key = iter.next();
            WeakReferenceListNode weakRef = (WeakReferenceListNode)finalizableWeakRefs.get(key);
            weakRef.finishFinalizers();
        }
        finalizerThread.stopRunning();
        try { finalizerThread.join(); } catch (InterruptedException ie) {}
    }

    public void addFinalizer(IRubyObject obj, long id, RubyProc proc) {
        WeakReferenceListNode finalizableWeakRef = (WeakReferenceListNode)finalizableWeakRefs.get(new Long(id));
        if(finalizableWeakRef == null) {
            finalizableWeakRef = new WeakReferenceListNode(obj, deadReferences, top);
            finalizableWeakRefs.put(new Long(id),finalizableWeakRef);
        }
        synchronized(finalizableWeakRef) {
            finalizableWeakRef.addFinalizer(new FinalizerEntry(id,proc));
        }
        finalizerThread.startRunning();
    }

    public void removeFinalizers(long id) {
        WeakReferenceListNode fins = (WeakReferenceListNode)finalizableWeakRefs.get(new Long(id));
        if(null != fins) {
            fins.removeFinalizers();
        }
        finalizableWeakRefs.remove(new Long(id));
    }

    public synchronized void add(IRubyObject object) {
        cleanup();
        top = new WeakReferenceListNode(object, deadReferences, top);
    }

    public synchronized Iterator iterator(RubyModule rubyClass) {
        final List objList = new ArrayList();
        WeakReferenceListNode current = top;
        while (current != null) {
            IRubyObject obj = (IRubyObject)current.get();
            if (obj != null && obj.isKindOf(rubyClass)) {
                objList.add(current);
            }

            current = current.nextNode;
        }

        return new Iterator() {
            private Iterator iter = objList.iterator();

            public boolean hasNext() {
                throw new UnsupportedOperationException();
            }

            public Object next() {
                Object obj = null;
                while (iter.hasNext()) {
                    WeakReferenceListNode node = (WeakReferenceListNode)iter.next();

                    obj = node.get();

                    if (obj != null) break;
                }
                return obj;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private synchronized void cleanup() {
        WeakReferenceListNode reference;
        while ((reference = (WeakReferenceListNode)deadReferences.poll()) != null) {
            reference.remove();
        }
    }

    private class WeakReferenceListNode extends WeakReference {
        private WeakReferenceListNode prevNode;
        private WeakReferenceListNode nextNode;
        private List finalizers;
        private long finalizerID;

        public WeakReferenceListNode(Object ref, ReferenceQueue queue, WeakReferenceListNode next) {
            super(ref, queue);

            this.nextNode = next;
            if (next != null) {
                next.prevNode = this;
            }
        }

        public void remove() {
            synchronized (ObjectSpace.this) {
                if (prevNode != null) {
                    prevNode.nextNode = nextNode;
                } else {
                    top = nextNode;
                }
                if (nextNode != null) {
                    nextNode.prevNode = prevNode;
                }
            }
            
            if (finalizerID != 0) {
                finalizersToRun.addAll(finalizers);
                finalizableWeakRefs.remove(new Long(finalizerID));
            }
        }
        
        public void addFinalizer(FinalizerEntry finalizer) {
            if (finalizers == null) {
                finalizers = new ArrayList();
            }
            finalizerID = finalizer.id;
            finalizers.add(finalizer);
        }
        
        public void removeFinalizers() {
            finalizers = null;
        }
        
        public void finishFinalizers() {
            finalizersToRun.addAll(finalizers);
        }
    }

    private static class IdReference extends WeakReference {
        private final long id;

        public IdReference(IRubyObject object, long id, ReferenceQueue queue) {
            super(object, queue);
            this.id = id;
        }

        public long id() {
            return id;
        }
    }
}
