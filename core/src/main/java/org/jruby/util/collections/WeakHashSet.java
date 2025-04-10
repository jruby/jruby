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

package org.jruby.util.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A simple set that uses weak references to ensure that its elements can be garbage collected.
 * @see java.util.WeakHashMap
 * @see java.util.HashSet
 * @param <T> type
 */
public class WeakHashSet<T> implements Set<T>, Cloneable {

    // Dummy value to associate with an Object in the backing Map
    private static final Object PRESENT = Boolean.TRUE;

    private final WeakHashMap<T, Object> map;

    public WeakHashSet() {
        map = new WeakHashMap<T, Object>();
    }

    public WeakHashSet(int size) {
        map = new WeakHashMap<T, Object>(size);
    }

    public boolean add(T o) {
        return map.put(o, PRESENT) == null;
    }

    public boolean remove(Object o) {
        return map.remove(o) == PRESENT;
    }

    public Iterator<T> iterator() {
        return map.keySet().iterator();
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public void clear() {
        map.clear();
    }

    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    public boolean containsAll(Collection<?> coll) {
        return map.keySet().containsAll(coll);
    }

    public boolean removeAll(Collection<?> coll) {
        return map.keySet().removeAll(coll);
    }

    public boolean retainAll(Collection<?> coll) {
        return map.keySet().retainAll(coll);
    }

    public boolean addAll(Collection<? extends T> coll) {
        boolean added = false;
        for (T i: coll) {
            add(i);
            added = true;
        }
        return added;
    }

    public Object[] toArray() {
        return map.keySet().toArray();
    }

    public <T> T[] toArray(final T[] arr) {
        return map.keySet().toArray(arr);
    }

    @Override
    public boolean equals(final Object o) {
        if ( o == this ) return true;
        if ( o instanceof Set ) {
            final Set that = (Set) o;
            if ( that.size() != this.size() ) return false;
            try {
                return containsAll(that);
            }
            catch (ClassCastException ignore)   { /* return false; */ }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 11 * map.hashCode();
    }

    @Override
    public WeakHashSet<T> clone() {
        //WeakHashSet<T> newSet = (WeakHashSet<T>) super.clone();
        //newSet.map = (WeakHashMap<T, Object>) map.clone();
        WeakHashSet<T> newSet = new WeakHashSet<T>(map.size());
        newSet.map.putAll(this.map);
        return newSet;
    }

    @Override
    public String toString() {
        Iterator<T> it = iterator();
        if ( ! it.hasNext() ) return "[]";

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        while (true) {
            final T e = it.next();
            sb.append(e == this ? "(this Collection)" : e);
            if ( ! it.hasNext() ) return sb.append(']').toString();
            sb.append(',').append(' ');
        }
    }

}
