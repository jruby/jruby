/**
 * **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2009-2013 Yoko Harada <yokolet@gmail.com>
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
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jruby.RubyObject;
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
    private final LocalContextProvider provider;
    private List<String> varNames = null;
    private List<BiVariable> variables = null;
    private boolean lazy;

    /**
     * Constructs an empty map. Users do not instantiate this map. The map is created
     * internally.
     * 
     * @param runtime is environment where variables are used to execute Ruby scripts.
     * @param behavior is one of variable behaviors defined in VariableBehavior.
     */
    public BiVariableMap(LocalContextProvider provider, boolean lazy) {
        this.provider = provider;
        this.lazy = lazy;
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
     * Returns a local variable behavior
     *
     * @return a local variable behavior
     */
    public LocalVariableBehavior getLocalVariableBehavior() {
        return provider.getLocalVariableBehavior();
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
        if (varNames == null) return 0;
        return varNames.size();
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     * 
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    public boolean isEmpty() {
        return varNames == null || varNames.isEmpty();
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
     * @param key is a key to be tested its presence
     * @return <tt>true</tt> if this map contains a mapping for the specified key
     */
    public boolean containsKey(Object key) {
        if (varNames == null) return false;
        checkKey(key);
        return varNames.contains((String)key);
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     * 
     * @param value is a Java object to be tested it presence
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
        return get(null, key);
    }

    /**
     * Returns the value in simple Java object to which the specified receiver
     * and key is mapped, or {@code null} if this map contains no mapping
     * for the key in a given receiver.
     *
     * @param receiver is a receiver object to get the value from
     * @param key is the key whose associated value is to be returned
     * @return the value in simple Java object to which the specified key is mapped, or
     *         {@code null} if this map contains no mapping for the key
     */
    public V get(Object receiver, Object key) {
        checkKey(key);
        RubyObject robj = getReceiverObject(receiver);
        // attemps to retrieve global variables
        if (lazy) VariableInterceptor.tryLazyRetrieval(provider.getLocalVariableBehavior(), this, robj, key);
        BiVariable var = getVariable(robj, (String)key);
        if (var == null) return null;
        else return (V) var.getJavaObject();
    }

    private RubyObject getReceiverObject(Object receiver) {
        if (receiver == null || !(receiver instanceof IRubyObject)) return (RubyObject)provider.getRuntime().getTopSelf();
        else if (receiver instanceof RubyObject) return (RubyObject)receiver;
        else return (RubyObject)((IRubyObject)receiver).getRuntime().getTopSelf();
    }

    /**
     * Returns the value in BiVariable type to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     * 
     * @param key is the key whose associated BiVariable object is to be returned
     * @return the BiVariable type object to which the specified key is mapped, or
     *         {@code null} if this map contains no mapping for the key
     */
    @Deprecated
    public BiVariable getVariable(String key) {
        return getVariable((RubyObject)provider.getRuntime().getTopSelf(), key);
    }

    /**
     * Returns the value in BiVariable type to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * @param receiver is a receiver object to get key-value pair from
     * @param key is the key whose associated BiVariable object is to be returned
     * @return the BiVariable type object to which the specified key is mapped, or
     *         {@code null} if this map contains no mapping for the key
     */
    public BiVariable getVariable(RubyObject receiver, String key) {
        if (varNames == null) return null;
        for (int i=0; i<varNames.size(); i++) {
            if (key.equals(varNames.get(i))) {
                BiVariable var = null;
                while (var == null) {
                    try {
                        var = variables.get(i);
                    } catch (Exception e) {
                        var = null;
                    }
                }
                if (var.isReceiverIdentical(receiver)) {
                    return var;
                }
            }
        }
        return null;
    }

    @Deprecated
    public void setVariable(BiVariable var) {
        setVariable((RubyObject)provider.getRuntime().getTopSelf(), var);
    }

    public void setVariable(RubyObject receiver, BiVariable var) {
        if (var == null) {
            return;
        }
        String key = var.getName();
        BiVariable old = getVariable(receiver, key);
        if (old != null) {
            // updates the value of an existing key-value pair
            old.setJavaObject(receiver.getRuntime(), var.getJavaObject());
        } else {
            update(key, var);
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
        return put(null, key, value);
    }

    /**
     * Associates the specified value with the specified key in this map.
     * The values is a simple Java object. If the map previously contained a mapping for
     * the key, the old value is replaced by the specified value.
     *
     * @param receiver a receiver object to associate a given key-value pair with
     * @param key the key with which the specified value is to be associated
     * @param value a simple Java object to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     */
    public V put (Object receiver, K key, V value) {
        checkKey(key);
        RubyObject robj = getReceiverObject(receiver);
        String name = ((String)key).intern();
        BiVariable v = getVariable(robj, name);
        Object oldValue = null;
        if (v != null) {
            // updates
            oldValue = v.getJavaObject();
            v.setJavaObject(robj.getRuntime(), value);
        } else {
            // creates new value
            v = VariableInterceptor.getVariableInstance(provider.getLocalVariableBehavior(), robj, name, value);
            if (v != null) {
                update(name, v);
            }
        }
        return (V)oldValue;
    }

    /**
     * Returns Ruby's local variable names this map has. The returned array is mainly
     * used to inject local variables to Ruby scripts while parsing.
     *
     * @return String array of Ruby's local variable names
     */
    public String[] getLocalVarNames() {
        if (variables == null) return null;
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
        if (variables == null) return null;
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
        VariableInterceptor.inject(this, provider.getRuntime(), scope, depth, receiver);
    }

    void retrieve(IRubyObject receiver) {
        RubyObject robj = getReceiverObject(receiver);
        VariableInterceptor.retrieve(provider.getLocalVariableBehavior(), this, robj);
    }

    void terminate() {
        VariableInterceptor.terminateGlobalVariables(provider.getLocalVariableBehavior(), variables, provider.getRuntime());
        VariableInterceptor.terminateLocalVariables(provider.getLocalVariableBehavior(), varNames, variables);
    }

    /**
     * Removes the mapping for a key from this map if it is present in a top level.
     *
     * <p>Returns the value to which this map previously associated the key,
     * or <tt>null</tt> if the map contained no mapping for the key.

     * @param key the key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     */
    public V remove(Object key) {
        return removeFrom(provider.getRuntime().getTopSelf(), key);
    }

    /**
     * Removes the mapping for a key from this map if it is present in a given
     * receiver.
     *
     * <p>Returns the value to which this map previously associated the key,
     * or <tt>null</tt> if the map contained no mapping for the key.

     * @param key the key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     */
    public V removeFrom(Object receiver, Object key) {
        if (varNames == null) return null;
        checkKey(key);
        RubyObject robj = getReceiverObject(receiver);
        String name = ((String)key).intern();
        for (int i=0; i<varNames.size(); i++) {
            if (name.equals(varNames.get(i))) {
                BiVariable var = variables.get(i);
                if (var.getReceiver() == robj) {
                    varNames.remove(i);
                    BiVariable v = variables.remove(i);
                    v.remove();
                    return (V)v.getJavaObject();
                }
            }
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
        if (varNames == null) return;
        boolean argv_presence = false;
        if (varNames.contains("ARGV")) argv_presence = true;
        varNames.clear();
        if (argv_presence) varNames.add("ARGV");
        BiVariable argv_object = null;
        for (BiVariable v : variables) {
            if (v != null) {
                if ("ARGV".equals(v.getName())) {
                    argv_object = v;
                } else {
                    v.remove();
                }
            }
        }
        variables.clear();
        if (argv_object != null) variables.add(argv_object);
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
        if (isEmpty()) return null;
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
        if (isEmpty()) return null;
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
        if (isEmpty()) return null;
        return getMap().entrySet();
    }

    /**
     * Adds a key-value pair of Ruby local variable to double array.
     * 
     * @param name is a Ruby's local variable name
     * @param value is BiVariable type object corresponding to the name
     */
    public void update(String name, BiVariable value) {
        if (varNames == null) {
            varNames = new ArrayList<String>();
            variables = new ArrayList<BiVariable>();
        }
        varNames.add(name);
        variables.add(value);
    }

    /**
     * Returns true when eager retrieval is requird or false when eager retrieval is
     * unnecessary.
     *
     * @return true for eager retrieve, false for on-demand retrieval
     */
    public boolean isLazy() {
        return lazy;
    }
}
