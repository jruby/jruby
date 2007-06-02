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
package org.jruby.util.collections;

import java.util.*;

/**
 * A simple set that uses weak references to ensure that its elements can be garbage collected.
 * See WeakHashMap.
 *
 * @author <a href="http://www.cs.auckland.ac.nz/~robert/">Robert Egglestone</a>
 */
public class WeakHashSet extends AbstractSet {
    private static final Object MAP_VALUE = new Object();
    private WeakHashMap map;

    public WeakHashSet() {
        map = new WeakHashMap();
    }

    public boolean add(Object o) {
        Object previousValue = map.put(o, MAP_VALUE);
        return previousValue == null;
    }

    public Iterator iterator() {
        return map.keySet().iterator();
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    public boolean remove(Object o) {
        return map.remove(o) == MAP_VALUE;
    }

    public boolean removeAll(Collection collection) {
        return map.keySet().removeAll(collection);
    }

    public boolean retainAll(Collection collection) {
        return map.keySet().retainAll(collection);
    }

    public void clear() {
        map.clear();
    }

}
