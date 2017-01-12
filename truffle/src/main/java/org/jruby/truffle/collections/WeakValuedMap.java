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
package org.jruby.truffle.collections;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Map-like that holds its values weakly (backed by a concurrent hash map).
 */
public class WeakValuedMap<Key, Value> {

    private final Map<Key, KeyedReference<Key, Value>> map = new ConcurrentHashMap<>();
    private final ReferenceQueue<Value> deadRefs = new ReferenceQueue<>();

    public final void put(Key key, Value value) {
        cleanReferences();
        map.put(key, new KeyedReference<>(value, key, deadRefs));
    }

    public final Value get(Key key) {
        cleanReferences();
        KeyedReference<Key, Value> reference = map.get(key);
        if (reference == null) return null;
        return reference.get();
    }

    public void clear() {
        cleanReferences();
        map.clear();
    }

    public int size() {
        cleanReferences();
        return map.size();
    }

    protected static class KeyedReference<Key, Value> extends WeakReference<Value> {

        protected final Key key;

        public KeyedReference(Value object, Key key, ReferenceQueue<? super Value> queue) {
            super(object, queue);
            this.key = key;
        }

    }

    @SuppressWarnings("unchecked")
    private void cleanReferences() {
        KeyedReference<Key, Value> ref;
        while ((ref = (KeyedReference<Key, Value>) deadRefs.poll()) != null) {
            map.remove(ref.key);
        }
    }
}
