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
package org.jruby.embed.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jruby.Ruby;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.variable.BiVariable;
import org.jruby.embed.variable.VariableInterceptor;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.ManyVarsDynamicScope;

/**
 * Ruby-Java bi-directional variable map implementation. Keys of this map
 * should be String, and values should be {@link BiVariable} type object.
 * This map does not permit null or empty key. Each operation of this class is not
 * synchronized; however, concurrent access to values are synchronized. When
 * {@link ScriptingContainer} is shared by multiple threads, specify thread safe
 * for a local context scope, which makes a map thread local.
 *
 * Usage example:
 * <pre>
 *         ScriptingContainer container = new ScriptingContainer();
 *         Map map = container.getVarMap();
 *         map.put("@coefficient", new Float(3.14));</pre>
 * or, using a shortcut method:
 * * <pre>
 *         ScriptingContainer container = new ScriptingContainer();
 *         container.put("@coefficient", new Float(3.14));</pre>
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class BiVariableMap<K, V> implements Map<K, V> {
    private Ruby runtime;
    private List<String> varNames;
    private List<BiVariable> variables;
    private VariableInterceptor interceptor;

    /**
     * Constructs an empy map. Users do not instantiate this map. The map is created
     * internally.
     *
     * @param runtime is environment where variables are used to execute Ruby scripts.
     */
    public BiVariableMap(Ruby runtime) {
        this(runtime, LocalVariableBehavior.TRANSIENT);
    }


    /**
     * Constructs an empy map. Users do not instantiate this map. The map is created
     * internally.
     * 
     * @param runtime is environment where variables are used to execute Ruby scripts.
     * @param behavior is one of variable behaviors defined in VariableBehavior.
     */
    public BiVariableMap(Ruby runtime, LocalVariableBehavior behavior) {
        this.runtime = runtime;
        varNames = Collections.synchronizedList(new ArrayList<String>());
        variables = Collections.synchronizedList(new ArrayList<BiVariable>());
        interceptor = new VariableInterceptor(behavior);
    }

    /**
     * Returns a list of all names in this map.
     *
     * @return a List of all names.
     */
    public List<String> getNames() {
        return varNames;
    }

    /**
     * Returns a list of all values in this map.
     *
     * @return a List of all values.
     */
    public List<BiVariable> getVariables() {
        return variables;
    }

    /**
     * Returns a list of all values in this map.
     *
     * @return a List of all values.
     */
    public VariableInterceptor getVariableInterceptor() {
        return interceptor;
    }

    /**
     * Returns a map whose value is a Java object not a BiVariable type object.
     *
     * @return a Map of key and value pair, in which values are simple Java objects.
     */
    public Map getMap() {
        Map m = new HashMap();
        for (BiVariable v : variables) {
            m.put(v.getName(), v.getJavaObject());
        }
        return m;
    }

    /**
     * Returns the number of key-value mappings in this map.
     * 
     * @return the number of key-value mappings in this map
     */
    public int size() {
        return varNames.size();
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     * 
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    public boolean isEmpty() {
        return varNames.isEmpty();
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

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified
     * key.
     * 
     * @param key is a key to be tested its presense
     * @return <tt>true</tt> if this map contains a mapping for the specified key
     */
    public boolean containsKey(Object key) {
        checkKey(key);
        return varNames.contains((String)key);
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     * 
     * @param value is a Java object to be tested it presense
     * @return Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     */
    public boolean containsValue(Object value) {
        Iterator itr = variables.iterator();
        while (itr.hasNext()) {
            BiVariable v = (BiVariable)itr.next();
            if (value == v.getJavaObject()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the value in simple Java object to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     * 
     * @param key is the key whose associated value is to be returned
     * @return the value in simple Java object to which the specified key is mapped, or
     *         {@code null} if this map contains no mapping for the key
     */
    public V get(Object key) {
        interceptor.tryLazyRetrieval(this, runtime, null, key);
        BiVariable bv = getVariable((String)key);
        if (bv == null) {
            return null;
        }
        return (V)bv.getJavaObject();
    }

    /**
     * Returns the value in BiVariable type to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     * 
     * @param key is the key whose associated BiVariable object is to be returned
     * @return the BiVariable type object to which the specified key is mapped, or
     *         {@code null} if this map contains no mapping for the key
     */
    public BiVariable getVariable(String key) {
        if (containsKey(key)) {
            int index = varNames.indexOf(key);
            return variables.get(index);
        } else {
            return null;
        }
    }

    public void setVariable(BiVariable var) {
        if (var == null) {
            return;
        }
        String key = var.getName();
        BiVariable old = getVariable(key);
        if (old != null) {
            old.setJavaObject(runtime, var.getJavaObject());
        } else {
            varNames.add(key);
            variables.add(var);
        }
    }

    /**
     * Associates the specified value with the specified key in this map.
     * The values is a simple Java object. If the map previously contained a mapping for
     * the key, the old value is replaced by the specified value.
     * 
     * @param key the key with which the specified value is to be associated
     * @param value a simple Java object to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     */
    public V put (K key, V value) {
        String name = (String)key;
        BiVariable v = getVariable(name);
        Object oldValue = null;
        if (v != null) {
            oldValue = v.getJavaObject();
            v.setJavaObject(runtime, value);
        } else {
            v = interceptor.getVariableInstance(runtime, name, value);
            if (v != null) {
                varNames.add(name);
                variables.add(v);
            }
        }
        return (V)oldValue;
    }

    /**
     * Returns Ruby's local variable names this map has. The returned array is mainly
     * used to inject local variables to Ruby scripts while parseing.
     *
     * @return String array of Ruby's local variable names
     */
    public String[] getLocalVarNames() {
        List<String> localVarNames = new ArrayList<String>();
        for (BiVariable v : variables) {
            if (v.getType() == BiVariable.Type.LocalVariable) {
                localVarNames.add(v.getName());
            }
        }
        if (localVarNames.size() > 0) {
            return localVarNames.toArray(new String[localVarNames.size()]);
        }
        return null;
    }

    /**
     * Returns Ruby's local variable values this map has. The returned array is
     * mainly used to inject local variables to Ruby scripts while evaluating.
     * 
     * @return IRubyObject array of Ruby's local variable names.
     */
    public IRubyObject[] getLocalVarValues() {
        List<IRubyObject> localVarValues = new ArrayList<IRubyObject>();
        for (BiVariable v : variables) {
            if (v.getType() == BiVariable.Type.LocalVariable) {
                localVarValues.add(v.getRubyObject());
            }
        }
        if (localVarValues.size() > 0) {
            return localVarValues.toArray(new IRubyObject[localVarValues.size()]);
        }
        return null;
    }

    void inject(ManyVarsDynamicScope scope, int depth, IRubyObject receiver) {
        interceptor.inject(this, runtime, scope, depth, receiver);
    }

    void retrieve(IRubyObject receiver) {
        interceptor.retrieve(this, runtime, receiver);
    }

    void terminate() {
        interceptor.terminateGlobalVariables(variables, runtime);
        interceptor.terminateLocalVariables(varNames, variables);
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * <p>Returns the value to which this map previously associated the key,
     * or <tt>null</tt> if the map contained no mapping for the key.

     * @param key the key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     */
    public V remove(Object key) {
        if (containsKey(key)) {
            int index = varNames.indexOf(key);
            varNames.remove(index);
            BiVariable v = variables.remove(index);
            v.remove(runtime);
            return (V)v.getJavaObject();
        }
        return null;
    }

    /**
     * Copies all of the mappings from the specified map to this map.
     *
     * @param t mappings to be stored in this map
     */
    public void putAll(Map<? extends K, ? extends V> t) {
        if (t == null) {
            throw new NullPointerException("map is null");
        }
        if (t.isEmpty()) {
            throw new IllegalArgumentException("map is empty");
        }
        Set set = t.entrySet();
        Iterator itr = set.iterator();
        while (itr.hasNext()) {
            Map.Entry entry = (Map.Entry)itr.next();
            if (entry.getKey() instanceof String) {
                K key = (K)entry.getKey();
                V value = (V)entry.getValue();
                put(key, value);
            } else {
                throw new ClassCastException("key is NOT String");
            }
        }
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns. Ruby variables are also
     * removed from Ruby instance. However, Ruby instance keep having global variable
     * names with null value.
     */
    public void clear() {
        varNames.clear();
        for (BiVariable v : variables) {
            if (v != null) {
                v.remove(runtime);
            }
        }
        variables.clear();
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * The set is backed by the map, so changes to the map should be
     * reflected in the set, and vice-versa. However, the implementation
     * does not reflect changes currently.
     * 
     * @return a set view of the keys contained in this map
     */
    public Set keySet() {
        if (varNames.isEmpty()) {
            return null;
        }
        Set s = new HashSet();
        for (String name : varNames) {
            s.add(name);
        }
        return s;
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map should be
     * reflected in the collection, and vice-versa. However, the implementation
     * does not reflect changes currently.
     * 
     * @return a collection view of the values contained in this map
     */
    public Collection values() {
        // should be vice-versa, but currently NOT
        if (varNames.isEmpty()) {
            return null;
        }
        List l = new ArrayList();
        for (BiVariable v : variables) {
            l.add(v.getJavaObject());
        }
        return l;
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map should be
     * reflected in the set, and vice-versa. However, the implementation
     * does not reflect changes currently.
     * 
     * @return an entry set of a map
     */
    public Set entrySet() {
        if (varNames.isEmpty()) {
            return null;
        }
        return getMap().entrySet();
    }

    /**
     * Adds a key-value pair of Ruby local variable to double array.
     * 
     * @param name is a Ruby's local variable name
     * @param value is BiVariable type object corresponding to the name
     */
    public void update(String name, BiVariable value) {
        this.varNames.add(name);
        this.variables.add(value);
    }
}
