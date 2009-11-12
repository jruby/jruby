/**
 * **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2009 Yoko Harada <yokolet@gmail.com>
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
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed.jsr223;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.AttributeName;

/**
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class JRubyBindings implements Bindings {
    private ScriptingContainer container;
    public final static String BACKED_BINDING = "org.jruby.embed.jsr223.backed_bindings";

    JRubyBindings(ScriptingContainer container) {
        this.container = container;
        container.getVarMap().clear();
    }

    public int size() {
        Set set = entrySet();
        return set.size();
    }

    public boolean isEmpty() {
        if (size() == 0) {
            return true;
        }
        return false;
    }

    private void checkKey(Object key) {
        if (key == null) {
            throw new NullPointerException("key is null");
        }
        if (!(key instanceof String)) {
            throw new ClassCastException("key is NOT String");
        }
        if (((String)key).length() == 0) {
            throw new IllegalArgumentException("key is empty");
        }
    }
    
    public boolean containsKey(Object key) {
        boolean isExist = container.getVarMap().containsKey(key);
        if (!isExist) {
            isExist = container.getAttributeMap().containsKey(key);
        }
        return isExist;
    }

    public boolean containsValue(Object value) {
        boolean isExist = container.getVarMap().containsValue(value);
        if (!isExist) {
            isExist = container.getAttributeMap().containsValue(value);
        }
        return isExist;
    }

    public Object get(Object key) {
        checkKey(key);
        Object obj = container.get((String)key);
        if (obj == null) {
            obj = container.getAttribute(key);
        }
        return obj;
    }

    public Object put(String key, Object value) {
        checkKey(key);
        Object oldValue = null;
        String adjustedKey = adjustKey(key);
        if (isRubyVariable(adjustedKey)) {
            oldValue = container.put(adjustedKey, value);
        } else {
            oldValue = container.setAttribute(adjustedKey, value);
            if (container.getAttributeMap().containsKey(BACKED_BINDING)) {
                Bindings b = (Bindings) container.getAttribute(BACKED_BINDING);
                b.put(key, value);
            }
        }
        return oldValue;
    }

    public Object remove(Object key) {
        checkKey(key);
        Object removedObj = null;
        if (container.getVarMap().containsKey(key)) {
            removedObj = container.getVarMap().remove(key);
        } else if (container.getAttributeMap().containsKey(key)) {
            removedObj = container.getAttributeMap().remove(key);
            if (container.getAttributeMap().containsKey(BACKED_BINDING)) {
                Bindings b = (Bindings) container.getAttribute(BACKED_BINDING);
                b.remove(key);
            }
        }
        return removedObj;
    }

    public void putAll(Map t) {
        if (t == null) {
            throw new NullPointerException("map is null");
        }
        Set set = t.keySet();
        for (Object key : set) {
            Object value = t.get(key);
            put((String)key, value);
        }
    }

    public void clear() {
        container.getVarMap().clear();
        Map map = container.getAttributeMap();
        if (map == null) {
            return;
        }
        List eliminateList = new ArrayList();
        for (Object key : map.keySet()) {
            if (isEligibleKey(key)) {
                eliminateList.add(key);
            }
        }
        for (Object key : eliminateList) {
            map.remove(key);
        }
    }

    public Set keySet() {
        Set keys = new HashSet();
        Set<Map.Entry> entries = entrySet();
        if (entries != null) {
            for (Map.Entry entry : entries) {
                keys.add(entry.getKey());
            }
        }
        keys.addAll(container.getVarMap().keySet());
        return keys;
    }

    public Collection values() {
        Collection values = new HashSet();
        Set<Map.Entry> entries = entrySet();
        if (entries != null) {
            for (Map.Entry entry : entries) {
                values.add(entry.getValue());
            }
        }
        values.addAll(container.getVarMap().values());
        return values;
    }

    public Set entrySet() {
        Set set = new HashSet();
        Set<Map.Entry> s = container.getVarMap().entrySet();
        if (s != null) {
            for (Map.Entry entry : s) {
                set.add(entry);
            }
        }
        s = container.getAttributeMap().entrySet();
        if (s != null) {
            for (Map.Entry entry : s) {
                if (isEligibleKey(entry.getKey())) {
                    set.add(entry);
                }
            }
        }
        return set;
    }

    private boolean isEligibleKey(Object k) {
        Object[] keys = {
            BACKED_BINDING,
            AttributeName.READER,
            AttributeName.WRITER,
            AttributeName.ERROR_WRITER
        };
        for (Object key : keys) {
            if (k == key) {
                return false;
            }
        }
        return true;
    }

    private boolean isRubyVariable(String name) {
        return container.getVarMap().getVariableInterceptor().isKindOfRubyVariable(name);
    }

    private String adjustKey(String key) {
        if (key.equals(ScriptEngine.ARGV)) {
            return "ARGV";
        } else {
            return key;
        }
    }
}