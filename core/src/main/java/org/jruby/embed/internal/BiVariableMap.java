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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jruby.Ruby;
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
public class BiVariableMap implements Map<String, Object> {

    private final LocalContextProvider provider;
    private final boolean lazy;

    private List<String> varNames;
    private List<BiVariable> variables;

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
        return varNames == null ? varNames = new ArrayList<String>() : varNames;
    }

    /**
     * Returns a list of all values in this map.
     *
     * @return a List of all values.
     */
    public List<BiVariable> getVariables() {
        return variables == null ? variables = new ArrayList<BiVariable>() : variables;
    }

    public Ruby getRuntime() { return provider.getRuntime(); }

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
    public Map<String, Object> getMap() {
        HashMap<String, Object> map = new HashMap<String, Object>();
        if ( variables != null ) {
            for ( final BiVariable var : getVariables() ) {
                map.put( var.getName(), var.getJavaObject() );
            }
        }
        return map;
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    @Override
    public int size() {
        return variables == null ? 0 : variables.size();
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    @Override
    public boolean isEmpty() {
        return variables == null || variables.isEmpty();
    }

    private static String checkKey(final Object key) {
        if ( key == null ) {
            throw new NullPointerException("key is null");
        }
        if ( ! (key instanceof String) ) {
            throw new ClassCastException("key is NOT String");
        }
        if ( ( (String) key ).isEmpty() ) {
            throw new IllegalArgumentException("key is empty");
        }
        return (String) key;
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified
     * key.
     *
     * @param key is a key to be tested its presence
     * @return <tt>true</tt> if this map contains a mapping for the specified key
     */
    @Override
    public boolean containsKey(final Object key) {
        if ( varNames == null || key == null ) return false;
        return varNames.contains( checkKey(key) );
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     *
     * @param value is a Java object to be tested it presence
     * @return Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     */
    @Override
    public boolean containsValue(final Object value) {
        if ( variables == null || value == null ) return false;
        for ( final BiVariable var : getVariables() ) {
            if ( value.equals( var.getJavaObject() ) ) return true;
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
    @Override
    public Object get(Object key) {
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
    public Object get(Object receiver, Object key) {
        checkKey(key);
        final RubyObject robj = getReceiverObject(receiver);
        // attemps to retrieve global variables
        if ( isLazy() ) {
            VariableInterceptor.tryLazyRetrieval(provider.getLocalVariableBehavior(), this, robj, key);
        }
        BiVariable var = getVariable(robj, (String) key);
        return var == null ? null : var.getJavaObject();
    }

    private RubyObject getReceiverObject(final Object receiver) {
        if ( receiver instanceof RubyObject ) return (RubyObject) receiver;
        //if ( receiver instanceof IRubyObject ) {
        //    return (RubyObject) ( (IRubyObject) receiver ).getRuntime().getTopSelf();
        //}
        return getTopSelf();
    }

    private RubyObject getTopSelf() {
        return (RubyObject) getRuntime().getTopSelf();
    }

    /**
     * Returns the value in BiVariable type to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * @param name is the key whose associated BiVariable object is to be returned
     * @return the BiVariable type object to which the specified key is mapped, or
     *         {@code null} if this map contains no mapping for the key
     */
    //@Deprecated
    public BiVariable getVariable(final String name) {
        return getVariable(getTopSelf(), name);
    }

    /**
     * Returns the value in BiVariable type to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * @param receiver is a receiver object to get key-value pair from
     * @param name is the key whose associated BiVariable object is to be returned
     * @return the BiVariable type object to which the specified key is mapped, or
     *         {@code null} if this map contains no mapping for the key
     */
    public BiVariable getVariable(final RubyObject receiver, final String name) {
        if ( variables == null ) return null;

        for ( int i = 0; i < size(); i++ ) {
            if ( name.equals( getNames().get(i) ) ) {
                BiVariable var;
                //try {
                    var = getVariables().get(i);
                //}
                //catch (RuntimeException e) {
                //    var = null;
                //}
                if ( var != null && var.isReceiverIdentical(receiver) ) {
                    return var;
                }
            }
        }

        return null;
    }

    public void setVariable(BiVariable var) {
        setVariable(getTopSelf(), var);
    }

    public void setVariable(final RubyObject receiver, final BiVariable var) {
        if ( var == null ) return;

        final String key = var.getName();
        final BiVariable old = getVariable(receiver, key);
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
    @Override
    public Object put(String key, Object value) {
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
    public Object put(Object receiver, String key, Object value) {
        checkKey(key);
        final RubyObject robj = getReceiverObject(receiver);
        final String name = key.intern();
        BiVariable var = getVariable(robj, name);
        Object oldValue = null;
        if ( var != null ) { // updates
            oldValue = var.getJavaObject();
            var.setJavaObject(robj.getRuntime(), value);
        }
        else { // creates new value
            var = VariableInterceptor.getVariableInstance(provider.getLocalVariableBehavior(), robj, name, value);
            if ( var != null ) update(name, var);
        }
        return oldValue;
    }

    /**
     * Returns Ruby's local variable names this map has. The returned array is mainly
     * used to inject local variables to Ruby scripts while parsing.
     *
     * @return String array of Ruby's local variable names
     */
    public String[] getLocalVarNames() {
        if ( variables == null ) return new String[0];

        List<String> localVarNames = new ArrayList<String>();
        for ( final BiVariable var : variables ) {
            if ( var.getType() == BiVariable.Type.LocalVariable ) {
                localVarNames.add( var.getName() );
            }
        }
        return localVarNames.toArray(new String[localVarNames.size()]);
    }

    /**
     * Returns Ruby's local variable values this map has. The returned array is
     * mainly used to inject local variables to Ruby scripts while evaluating.
     *
     * @return IRubyObject array of Ruby's local variable names.
     */
    public IRubyObject[] getLocalVarValues() {
        if ( variables == null ) return IRubyObject.NULL_ARRAY;

        List<IRubyObject> localVarValues = new ArrayList<IRubyObject>();
        for ( final BiVariable var : variables ) {
            if ( var.getType() == BiVariable.Type.LocalVariable ) {
                localVarValues.add( var.getRubyObject() );
            }
        }
        return localVarValues.toArray( new IRubyObject[ localVarValues.size() ] );
    }

    void inject(final ManyVarsDynamicScope scope, final int depth, final IRubyObject receiver) {
        VariableInterceptor.inject(this, provider.getRuntime(), scope, depth, receiver);
    }

    void retrieve(final IRubyObject receiver) {
        final RubyObject robj = getReceiverObject(receiver);
        VariableInterceptor.retrieve(getLocalVariableBehavior(), this, robj);
    }

    void terminate() {
        VariableInterceptor.terminateGlobalVariables(getLocalVariableBehavior(), getVariables(), getRuntime());
        VariableInterceptor.terminateLocalVariables(getLocalVariableBehavior(), getNames(), getVariables());
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
    @Override
    public Object remove(final Object key) {
        return removeFrom(getTopSelf(), key);
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
    public Object removeFrom(final Object receiver, final Object key) {
        if ( variables == null ) return null;
        checkKey(key);
        final RubyObject robj = getReceiverObject(receiver);
        for ( int i = 0; i < size(); i++ ) {
            if ( ((String) key).equals( varNames.get(i) ) ) {
                final BiVariable var = variables.get(i);
                if ( var.isReceiverIdentical(robj) ) {
                    varNames.remove(i);
                    variables.remove(i);
                    return var.getJavaObject();
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

    @Override
    public void putAll(final Map<? extends String, ? extends Object> map) {
        if (map == null) {
            throw new NullPointerException("map is null");
        }
        if (map.isEmpty()) {
            throw new IllegalArgumentException("map is empty");
        }
        for ( final Entry entry : map.entrySet() ) {
            final Object key = entry.getKey();
            if (key instanceof String) {
                put( (String) key, entry.getValue());
            } else {
                throw new ClassCastException("key is not String");
            }
        }
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns. Ruby variables are also
     * removed from Ruby instance. However, Ruby instance keep having global variable
     * names with null value.
     */
    @Override
    public void clear() {
        if ( variables == null ) return;
        BiVariable argv = null;
        for ( BiVariable var : getVariables() ) {
            if ( var != null ) {
                if ( "ARGV".equals(var.getName()) ) {
                    argv = var;
                }
                else {
                    var.remove();
                }
            }
        }
        getNames().clear(); getVariables().clear();
        if ( argv != null ) update("ARGV", argv);
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * The set is backed by the map, so changes to the map should be
     * reflected in the set, and vice-versa. However, the implementation
     * does not reflect changes currently.
     *
     * @return a set view of the keys contained in this map
     */
    @Override
    public Set<String> keySet() {
        return new LinkedHashSet<String>( getNames() );
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map should be
     * reflected in the collection, and vice-versa. However, the implementation
     * does not reflect changes currently.
     *
     * @return a collection view of the values contained in this map
     */
    @Override
    public Collection<Object> values() {
        return getMap().values();
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map should be
     * reflected in the set, and vice-versa. However, the implementation
     * does not reflect changes currently.
     *
     * @return an entry set of a map
     */
    @Override
    public Set<Entry<String, Object>> entrySet() {
        return getMap().entrySet();
    }

    /**
     * Adds a key-value pair of Ruby local variable to double array.
     *
     * @param name is a Ruby's local variable name
     * @param value is BiVariable type object corresponding to the name
     */
    public void update(final String name, final BiVariable value) {
        getNames().add(name);
        getVariables().add(value);
    }

    public void updateVariable(final RubyObject receiver, final String name,
        final IRubyObject value, final Class<? extends BiVariable> type) {
        final BiVariable var = getVariable(receiver, name);
        if (var != null) {
            var.setRubyObject(value);
        }
        else {
            update(name, newVariable(receiver, name, value, type));
        }
    }

    private static BiVariable newVariable(final RubyObject receiver,
        final String name, final IRubyObject value,
        final Class<? extends BiVariable> type) {
        try {
            // (RubyObject receiver, String name, IRubyObject irubyObject)
            final Constructor<? extends BiVariable> constructor =
                type.getDeclaredConstructor(RubyObject.class, String.class, IRubyObject.class);
            constructor.setAccessible(true);
            return constructor.newInstance(receiver, name, value);
        }
        catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
        catch (InvocationTargetException ex) {
            final Throwable cause = ex.getTargetException();
            if ( cause instanceof RuntimeException ) throw (RuntimeException) cause;
            throw new RuntimeException(cause);
        }
        catch (InstantiationException ex) { throw new RuntimeException(ex); }
        catch (IllegalAccessException ex) { throw new RuntimeException(ex); }
    }

    /**
     * Returns true when eager retrieval is required or false when eager
     * retrieval is unnecessary.
     *
     * @return true for eager retrieve, false for on-demand retrieval
     */
    public boolean isLazy() {
        return lazy;
    }

    @Override
    public String toString() {
        final StringBuilder str = new StringBuilder();
        str.append( getClass().getName() );

        str.append('{');
        for ( int i = 0; i < size(); i++ ) {
            final String name = getNames().get(i);
            final BiVariable variable = getVariables().get(i);
            str.append(name).append('=').append(variable.getJavaObject());
            if ( i == size() - 1 ) break;
            str.append(',').append(' ');
        }
        return str.append('}').toString();
    }

}
