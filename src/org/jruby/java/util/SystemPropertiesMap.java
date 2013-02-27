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

package org.jruby.java.util;

import org.jruby.runtime.builtin.IRubyObject;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A java.lang.Map that defers all methods to System properties.
 */
public class SystemPropertiesMap  implements Map {
    protected String stringFromObject(Object o) {
        if (o instanceof String) {
            return (String)o;
        } else if (o instanceof IRubyObject) {
            return (String)((IRubyObject)o).toJava(String.class);
        }
        return null;
    }

    public int size() {
        return System.getProperties().size();
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean containsKey(Object o) {
        String key = stringFromObject(o);
        if (key != null) {
            return System.getProperty(key) != null;
        }
        return false;
    }

    public boolean containsValue(Object o) {
        String value = stringFromObject(o);
        if (value != null) {
            return System.getProperties().containsValue(value);
        }
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object get(Object o) {
        String key = stringFromObject(o);
        if (key != null) {
            return System.getProperty(key);
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object put(Object s, Object s1) {
        String key = stringFromObject(s);
        String value = stringFromObject(s1);
        if (key != null) {
            if (value == null) {
                return System.clearProperty(key);
            }
            return System.setProperty(key, value);
        }
        return null;
    }

    public Object remove(Object o) {
        String key = stringFromObject(o);
        if (key != null) {
            return System.clearProperty(key);
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void putAll(Map map) {
        for (Map.Entry entry : (Set<Entry>)map.entrySet()) {
            String key = stringFromObject(entry.getKey());
            String value = stringFromObject(entry.getValue());
            if (key != null) {
                if (value == null) {
                    System.clearProperty(key);
                } else {
                    System.setProperty(key, value);
                }
            }
        }
    }

    public void clear() {
        // ignored
    }

    public Set<Object> keySet() {
        return System.getProperties().keySet();
    }

    public Collection<Object> values() {
        return System.getProperties().values();
    }

    public Set<Entry<Object, Object>> entrySet() {
        return System.getProperties().entrySet();
    }
}