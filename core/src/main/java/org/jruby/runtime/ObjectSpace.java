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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.runtime;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.jruby.RubyBasicObject;
import org.jruby.RubyModule;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.javasupport.JavaPackage;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.WeakIdentityHashMap;

/**
 * FIXME: This version is faster than the previous, but both suffer from a
 * crucial flaw: It is impossible to create an ObjectSpace with an iterator
 * that doesn't either: a. hold on to objects that might otherwise be collected
 * or b. have no way to guarantee that a call to hasNext() will be correct or
 * that a subsequent call to next() will produce an object. For our purposes,
 * for now, this may be acceptable.
 */
public class ObjectSpace {
    private final ReferenceQueue<Object> deadReferences = new ReferenceQueue<>();
    private final ReferenceQueue<ObjectGroup> objectGroupReferenceQueue = new ReferenceQueue<>();
    private WeakReferenceListNode top;

    private final ReferenceQueue deadIdentityReferences = new ReferenceQueue();
    private final Map<Long, IdReference> identities = new HashMap<>(64);
    private final Map<IRubyObject, Long> identitiesByObject = new WeakIdentityHashMap(64);
    private static final AtomicLong maxId = new AtomicLong(1000);
    private final ThreadLocal<Reference<ObjectGroup>> currentObjectGroup = new ThreadLocal<>();
    private Reference<GroupSweeper> groupSweeperReference;

    public void registerObjectId(long id, IRubyObject object) {
        synchronized (identities) {
            cleanIdentities();
            identities.put(id, new IdReference(object, id, deadIdentityReferences));
            identitiesByObject.put(object, id);
        }
    }

    public static long calculateObjectId(Object object) {
        // Fixnums get all the 0b01 id's, flonums get the 0b10 id's, so we use next ID * 4
        return maxId.getAndIncrement() * 4;
    }

    public long createAndRegisterObjectId(IRubyObject rubyObject) {
        synchronized (identities) {
            Long longId = identitiesByObject.get(rubyObject);
            if (longId == null) {
                longId = createId(rubyObject);
            }
            return longId.longValue();
        }
    }

    private long createId(IRubyObject object) {
        long id = calculateObjectId(object);
        registerObjectId(id, object);
        return id;
    }

    public IRubyObject id2ref(long id) {
        synchronized (identities) {
            cleanIdentities();
            IdReference reference = identities.get(id);
            if (reference == null) {
                return null;
            }
            return reference.get();
        }
    }

    private void cleanIdentities() {
        IdReference ref;
        while ((ref = (IdReference) deadIdentityReferences.poll()) != null) {
            identities.remove(ref.id);
        }
    }

    @Deprecated(since = "1.6.0")
    public long idOf(IRubyObject rubyObject) {
        return createAndRegisterObjectId(rubyObject);
    }

    @Deprecated(since = "9.4.10.0")
    public void addFinalizer(IRubyObject object, IRubyObject proc) {
        addFinalizer(((RubyBasicObject) object).getCurrentContext(), object, proc);
    }

    public IRubyObject addFinalizer(ThreadContext context, IRubyObject object, IRubyObject proc) {
        return object.addFinalizer(context, proc);
    }

    public void removeFinalizers(long id) {
        IRubyObject object = id2ref(id);
        if (object != null) {
            object.removeFinalizers();
        }
    }

    public void add(IRubyObject object) {
        if (object instanceof JavaPackage)
            return;
        if (true && object.getMetaClass() != null && !(object instanceof JavaProxy)) {
            // If the object is already frozen when we encounter it, it's pre-frozen.
            // Since this only (currently) applies to objects created outside the
            // normal routes of construction, we don't show it in ObjectSpace.
            if (object.isFrozen()) return;

            getObjectGroup().add(object);
        } else {
            addIndividualWeakReference(object);
        }
    }

    private synchronized void addIndividualWeakReference(IRubyObject object) {
        cleanup(deadReferences);
        top = new WeakReferenceListNode<Object>(object, deadReferences, top);
    }

    private synchronized void registerGroupSweeper() {
        if (groupSweeperReference == null || groupSweeperReference.get() == null) {
            groupSweeperReference = new WeakReference<GroupSweeper>(new GroupSweeper());
        }
    }

    private synchronized void splitObjectGroups() {
        cleanup(objectGroupReferenceQueue);

        // Split apart each group into individual weak references
        WeakReferenceListNode node = top;
        while (node != null) {
            Object obj = node.get();
            if (obj instanceof ObjectGroup) {
                ObjectGroup objectGroup = (ObjectGroup) obj;
                for (int i = 0; i < objectGroup.size(); i++) {
                    IRubyObject rubyObject = objectGroup.set(i, null);
                    if (rubyObject != null) {
                        top = new WeakReferenceListNode<Object>(rubyObject, deadReferences, top);
                    }
                }
            }
            node = node.nextNode;
        }
    }

    public synchronized Iterator iterator(RubyModule rubyClass) {
        final ArrayList<Reference<Object>> objList = new ArrayList<>(64);
        WeakReferenceListNode current = top;
        while (current != null) {
            Object obj = current.get();
            if (obj instanceof IRubyObject) {
                IRubyObject rubyObject = (IRubyObject) obj;
                if (rubyClass.isInstance(rubyObject)) {
                    objList.add(current);
                }

            } else if (obj instanceof ObjectGroup) {
                for (IRubyObject rubyObject : (ObjectGroup) obj) {
                    if (rubyObject != null && rubyClass.isInstance(rubyObject)) {
                        objList.add(new WeakReference<Object>(rubyObject));
                    }
                }
            }

            current = current.nextNode;
        }

        return new Iterator() {
            final Iterator<Reference<Object>> iter = objList.iterator();

            public boolean hasNext() {
                throw new UnsupportedOperationException();
            }

            public Object next() {
                Object obj = null;
                while (iter.hasNext()) {
                    obj = iter.next().get();

                    if (obj != null) break;
                }
                return obj;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private void cleanup(ReferenceQueue<?> referenceQueue) {
        WeakReferenceListNode reference;
        while ((reference = (WeakReferenceListNode) referenceQueue.poll()) != null) {
            reference.remove();
        }
    }

    private class WeakReferenceListNode<T> extends WeakReference<T> {
        private WeakReferenceListNode prevNode;
        private WeakReferenceListNode nextNode;

        public WeakReferenceListNode(T referent, ReferenceQueue<T> queue, WeakReferenceListNode<?> next) {
            super(referent, queue);

            this.nextNode = next;
            if (next != null) {
                next.prevNode = this;
            }
        }

        private void remove() {
            if (prevNode != null) {
                prevNode.nextNode = nextNode;
            } else {
                top = nextNode;
            }
            if (nextNode != null) {
                nextNode.prevNode = prevNode;
            }
        }
    }

    private static class IdReference extends WeakReference<IRubyObject> {
        final long id;

        IdReference(IRubyObject object, long id, ReferenceQueue queue) {
            super(object, queue);
            this.id = id;
        }
    }

    private ObjectGroup getObjectGroup() {
        Reference<ObjectGroup> ref = currentObjectGroup.get();
        ObjectGroup objectGroup = ref != null ? ref.get() : null;
        return objectGroup != null && !objectGroup.isFull() ? objectGroup : addObjectGroup();
    }


    private synchronized ObjectGroup addObjectGroup() {
        cleanup(objectGroupReferenceQueue);
        ObjectGroup objectGroup;
        WeakReferenceListNode<ObjectGroup> ref = new WeakReferenceListNode<ObjectGroup>(objectGroup = new ObjectGroup(),
                objectGroupReferenceQueue, top);
        currentObjectGroup.set(ref);
        top = ref;
        if (groupSweeperReference == null) registerGroupSweeper();

        return objectGroup;
    }

    private static final class ObjectGroup extends AbstractList<IRubyObject> {
        private static final int MAX_OBJECTS_PER_GROUP = 64;
        private final AtomicReferenceArray<IRubyObject> objects = new AtomicReferenceArray<IRubyObject>(MAX_OBJECTS_PER_GROUP);
        private int nextIndex = 0;

        public boolean add(IRubyObject obj) {
            obj.getMetaClass().getObjectGroupAccessorForWrite().set(obj, this);
            objects.set(nextIndex, obj);
            ++nextIndex;
            return true;
        }

        @Override
        public IRubyObject get(int index) {
            return objects.get(index);
        }

        @Override
        public IRubyObject set(int index, IRubyObject element) {
            return objects.getAndSet(index, element);
        }

        private boolean isFull() {
            return nextIndex >= objects.length();
        }

        public int size() {
            return objects.length();
        }
    }

    // This class is used as a low-memory cleaner.  When it is finalized, it will cause all groups to be split into
    // individual weak refs, so each object can be collected.
    private final class GroupSweeper {

        @Override
        protected void finalize() throws Throwable {
            try {
                splitObjectGroups();
            } finally {
                registerGroupSweeper();
                super.finalize();
            }
        }
    }
}
