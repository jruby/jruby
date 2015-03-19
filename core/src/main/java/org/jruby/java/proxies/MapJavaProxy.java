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
 * Copyright (C) 2011 Yoko Harada <yokolet@gmail.com>
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
package org.jruby.java.proxies;


import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;
import java.util.Set;

/**
 *
 * @author Yoko Harada
 */

public class MapJavaProxy extends ConcreteJavaProxy {
    private RubyHashMap wrappedMap;

    public MapJavaProxy(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }

    public MapJavaProxy(Ruby runtime, RubyClass klazz, Map map) {
        super(runtime, klazz, map);
    }

    private static final ObjectAllocator ALLOCATOR = new ObjectAllocator() {
        public MapJavaProxy allocate(Ruby runtime, RubyClass klazz) {
            return new MapJavaProxy(runtime, klazz);
        }
    };

    public static RubyClass createMapJavaProxy(final Ruby runtime) {
        RubyClass MapJavaProxy = runtime.defineClass(
            "MapJavaProxy", runtime.getJavaSupport().getConcreteProxyClass(), ALLOCATOR
        );
        // this is done while proxy class is created.
        // See org.jruby.javasuppoer.java.createProxyClass()
        // MapJavaProxy.defineAnnotatedMethods(MapJavaProxy.class);
        ConcreteJavaProxy.initialize(MapJavaProxy);
        return MapJavaProxy;
    }

    private RubyHashMap getOrCreateRubyHashMap() {
        if (wrappedMap == null) {
            wrappedMap = new RubyHashMap(getRuntime(), this);
        }
        // (JavaProxy)recv).getObject() might raise exception when
        // wrong number of args are given to the constructor.
        IRubyObject oldExc = getRuntime().getGlobalVariables().get("$!"); // Save $!
        try {
            wrappedMap.setSize(((Map)((JavaProxy)this).getObject()).size());
        } catch (RaiseException e) {
            wrappedMap.setSize(0);
            getRuntime().getGlobalVariables().set("$!", oldExc); // Restore $!
        }
        return wrappedMap;
    }

    private static class RubyHashMap extends RubyHash {
        private IRubyObject receiver;

        public RubyHashMap(Ruby runtime, IRubyObject receiver) {
            super(runtime);
            this.receiver = receiver;
        }

        private void setSize(int size) {
            this.size = size;
        }

        private Map getMap() {
            return (Map) ((JavaProxy)receiver).getObject();
        }

        @Override
        public void internalPut(final IRubyObject key, final IRubyObject value, final boolean checkForExisting) {
            Map map = getMap();
            map.put(key.toJava(Object.class), value.toJava(Object.class));
            size = map.size();
        }

        @Override
        public IRubyObject internalGet(IRubyObject key) {
            Object result = getMap().get(key.toJava(Object.class));
            if (result == null) return null;
            return JavaUtil.convertJavaToUsableRubyObject(getRuntime(), result);
        }

        @Override
        public RubyHashEntry internalGetEntry(IRubyObject key) {
            Map map = getMap();
            Object convertedKey = key.toJava(Object.class);
            Object value = map.get(convertedKey);

            if (value != null) {
                RubyHashEntry rubyEntry = new RubyHashEntry(key.hashCode(), key, JavaUtil.convertJavaToUsableRubyObject(getRuntime(), value), null, null);
                return rubyEntry;
            }

            return NO_ENTRY;
        }

        @Override
        public RubyHashEntry internalDelete(final IRubyObject key) {
            Map map = getMap();
            Object convertedKey = key.toJava(Object.class);
            Object value = map.get(convertedKey);

            if (value != null) {
                RubyHashEntry rubyEntry = new RubyHashEntry(key.hashCode(), key, JavaUtil.convertJavaToUsableRubyObject(getRuntime(), value), null, null);
                map.remove(convertedKey);
                size = map.size();
                return rubyEntry;
            }

            return NO_ENTRY;
        }

        @Override
        public RubyHashEntry internalDeleteEntry(final RubyHashEntry entry) {
            Map map = getMap();
            Object convertedKey = ((IRubyObject)entry.getKey()).toJava(Object.class);

            if (map.containsKey(convertedKey)) {
                map.remove(convertedKey);
                size = map.size();
                return entry;
            }

            return NO_ENTRY;
        }

        @Override
        public void visitAll(Visitor visitor) {
            Map.Entry[] entries = (Entry[]) getMap().entrySet().toArray(new Map.Entry[0]);
            for (Map.Entry entry : entries) {
                IRubyObject key = JavaUtil.convertJavaToUsableRubyObject(getRuntime(), entry.getKey());
                IRubyObject value = JavaUtil.convertJavaToUsableRubyObject(getRuntime(), entry.getValue());
                visitor.visit(key, value);
            }
        }

        @Override
        public void op_asetForString(Ruby runtime, RubyString key, IRubyObject value) {
            getMap().put(key.toJava(String.class), value.toJava(Object.class));
            size = getMap().size();
        }

        @Override
        public RubyHash rehash() {
            throw getRuntime().newNotImplementedError("rehash method is not implemented in a Java Map backed object");
        }

        @Override
        public RubyHash rb_clear() {
            getMap().clear();
            size = 0;

            return this;
        }

        /** rb_hash_shift
         *
         */
        @Override
        public IRubyObject shift(ThreadContext context) {
            throw getRuntime().newNotImplementedError("shift method is not implemented in a Java Map backed object");
        }

        @Override
        public RubyHash to_hash() {
            RubyHash hash = new RubyHash(getRuntime());
            Set<Map.Entry> entries = ((Map) ((JavaProxy)receiver).getObject()).entrySet();
            for (Map.Entry entry : entries) {
                IRubyObject key = JavaUtil.convertJavaToUsableRubyObject(getRuntime(), entry.getKey());
                IRubyObject value = JavaUtil.convertJavaToUsableRubyObject(getRuntime(), entry.getValue());
                hash.op_aset(getRuntime().getCurrentContext(), key, value);
            }
            return hash;
        }
    }

    @JRubyMethod(name = "default")
    public IRubyObject default_value_get(ThreadContext context) {
        return getOrCreateRubyHashMap().default_value_get(context);
    }

    @JRubyMethod(name = "default")
    public IRubyObject default_value_get(ThreadContext context, IRubyObject arg) {
        return getOrCreateRubyHashMap().default_value_get(context, arg);
    }

    /** rb_hash_set_default
     *
     */
    @JRubyMethod(name = "default=", required = 1)
    public IRubyObject default_value_set(final IRubyObject defaultValue) {
        return getOrCreateRubyHashMap().default_value_set(defaultValue);
    }

    /** rb_hash_default_proc
     *
     */
    @JRubyMethod(name = "default_proc")
    public IRubyObject default_proc() {
        return getOrCreateRubyHashMap().default_proc();
    }

    /** rb_hash_set_default_proc
     *
     */
    @JRubyMethod(name = "default_proc=")
    public IRubyObject set_default_proc(IRubyObject proc) {
        return getOrCreateRubyHashMap().set_default_proc(proc);
    }

    /** rb_hash_inspect
     *
     */
    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        return getOrCreateRubyHashMap().inspect19(context);
    }

    /** rb_hash_size
     *
     */
    @JRubyMethod(name = {"size", "length"})
    public RubyFixnum rb_size() {
        return getOrCreateRubyHashMap().rb_size();
    }

    /** rb_hash_empty_p
     *
     */
    @JRubyMethod(name = "empty?")
    public RubyBoolean empty_p() {
        return getOrCreateRubyHashMap().empty_p();
    }

    /** rb_hash_to_a
     *
     */
    @JRubyMethod(name = "to_a")
    public RubyArray to_a() {
        return getOrCreateRubyHashMap().to_a();
    }

    /** rb_hash_to_s
     *
     */
    @JRubyMethod(name = "to_s")
    public IRubyObject to_s(ThreadContext context) {
        return getOrCreateRubyHashMap().to_s19(context);
    }

    /** rb_hash_rehash
     *
     */
    @JRubyMethod(name = "rehash")
    public RubyHash rehash() {
        return getOrCreateRubyHashMap().rehash();
    }

    /** rb_hash_to_hash
     *
     */
    @JRubyMethod(name = "to_hash")
    public RubyHash to_hash() {
        return getOrCreateRubyHashMap().to_hash();
    }

    /** rb_hash_aset
     *
     */
    @JRubyMethod(name = {"[]=", "store"}, required = 2)
    public IRubyObject op_aset(ThreadContext context, IRubyObject key, IRubyObject value) {
        return getOrCreateRubyHashMap().op_aset(context, key, value);
    }

    /** rb_hash_equal
     *
     */
    @JRubyMethod(name = "==")
    public IRubyObject op_equal(final ThreadContext context, IRubyObject other) {
        return getOrCreateRubyHashMap().op_equal(context, other);
    }

    /** rb_hash_eql19
     *
     */
    @JRubyMethod(name = "eql?")
    public IRubyObject op_eql19(final ThreadContext context, IRubyObject other) {
        return getOrCreateRubyHashMap().op_eql19(context, other);
    }

    /** rb_hash_aref
     *
     */
    @JRubyMethod(name = "[]", required = 1)
    public IRubyObject op_aref(ThreadContext context, IRubyObject key) {
        return getOrCreateRubyHashMap().op_aref(context, key);
    }

    /** rb_hash_hash
     *
     */
    @JRubyMethod(name = "hash")
    public RubyFixnum hash() {
        return getOrCreateRubyHashMap().hash19();
    }

    /** rb_hash_fetch
     *
     */
    @JRubyMethod
    public IRubyObject fetch(ThreadContext context, IRubyObject key, Block block) {
        return getOrCreateRubyHashMap().fetch(context, key, block);
    }

    @JRubyMethod
    public IRubyObject fetch(ThreadContext context, IRubyObject key, IRubyObject _default, Block block) {
        return getOrCreateRubyHashMap().fetch(context, key, _default, block);
    }

    /** rb_hash_has_key_p
     *
     */
    @JRubyMethod(name = {"has_key?", "key?", "include?", "member?"}, required = 1)
    public RubyBoolean has_key_p(IRubyObject key) {
        return getOrCreateRubyHashMap().has_key_p(key);
    }

    /** rb_hash_has_value
     *
     */
    @JRubyMethod(name = {"has_value?", "value?"}, required = 1)
    public RubyBoolean has_value_p(ThreadContext context, IRubyObject expected) {
        return getOrCreateRubyHashMap().has_value_p(context, expected);
    }

    /** rb_hash_each
     *
     */
    @JRubyMethod
    public IRubyObject each(final ThreadContext context, final Block block) {
        return getOrCreateRubyHashMap().each19(context, block);
    }


    /** rb_hash_each_pair
     *
     */
    @JRubyMethod(name = "each_pair")
    public IRubyObject each_pair(final ThreadContext context, final Block block) {
        return getOrCreateRubyHashMap().each_pair(context, block);
    }

    /** rb_hash_each_value
     *
     */
    @JRubyMethod(name = "each_value")
    public IRubyObject each_value(final ThreadContext context, final Block block) {
        return getOrCreateRubyHashMap().each_value(context, block);
    }

    /** rb_hash_each_key
     *
     */
    @JRubyMethod(name = "each_key")
    public IRubyObject each_key(final ThreadContext context, final Block block) {
        return getOrCreateRubyHashMap().each_key(context, block);
    }

    /** rb_hash_select_bang
     *
     */
    @JRubyMethod(name = "select!")
    public IRubyObject select_bang(final ThreadContext context, final Block block) {
        return getOrCreateRubyHashMap().select_bang(context, block);
    }

    /** rb_hash_keep_if
     *
     */
    @JRubyMethod(name = "keep_if")
    public IRubyObject keep_if(final ThreadContext context, final Block block) {
        return getOrCreateRubyHashMap().keep_if(context, block);
    }

    /** rb_hash_sort
     *
     */
    @JRubyMethod(name = "sort")
    public IRubyObject sort(ThreadContext context, Block block) {
        return getOrCreateRubyHashMap().sort(context, block);
    }

    /** rb_hash_index
     *
     */
    @JRubyMethod(name = "index")
    public IRubyObject index(ThreadContext context, IRubyObject expected) {
        return getOrCreateRubyHashMap().index19(context, expected);
    }

    /** rb_hash_key
     *
     */
    @JRubyMethod(name = "key")
    public IRubyObject key(ThreadContext context, IRubyObject expected) {
        return getOrCreateRubyHashMap().key(context, expected);
    }

    /** rb_hash_keys
     *
     */
    @JRubyMethod(name = "keys")
    public RubyArray keys() {
        return getOrCreateRubyHashMap().keys();
    }

    /** rb_hash_values
     *
     */
    @JRubyMethod(name = "values")
    public RubyArray rb_values() {
        return getOrCreateRubyHashMap().rb_values();
    }

    /** rb_hash_shift
     *
     */
    @JRubyMethod(name = "shift")
    public IRubyObject shift(ThreadContext context) {
        return getOrCreateRubyHashMap().shift(context);
    }

    /** rb_hash_delete
     *
     */
    @JRubyMethod(name = "delete")
    public IRubyObject delete(ThreadContext context, IRubyObject key, Block block) {
        return getOrCreateRubyHashMap().delete(context, key, block);
    }

    /** rb_hash_select
     *
     */
    @JRubyMethod(name = "select")
    public IRubyObject select(final ThreadContext context, final Block block) {
        return getOrCreateRubyHashMap().select19(context, block);
    }

    /** rb_hash_delete_if
     *
     */
    @JRubyMethod(name = "delete_if")
    public IRubyObject delete_if(final ThreadContext context, final Block block) {
        return getOrCreateRubyHashMap().delete_if(context, block);
    }

    /** rb_hash_reject
     *
     */
    @JRubyMethod(name = "reject")
    public IRubyObject reject(final ThreadContext context, final Block block) {
        return getOrCreateRubyHashMap().reject(context, block);
    }

    /** rb_hash_reject_bang
     *
     */
    @JRubyMethod(name = "reject!")
    public IRubyObject reject_bang(final ThreadContext context, final Block block) {
        return getOrCreateRubyHashMap().reject_bang(context, block);
    }

    /** rb_hash_clear
     *
     */
    @JRubyMethod(name = "clear")
    public RubyHash rb_clear() {
        return getOrCreateRubyHashMap().rb_clear();
    }

    /** rb_hash_invert
     *
     */
    @JRubyMethod(name = "invert")
    public RubyHash invert(final ThreadContext context) {
        return getOrCreateRubyHashMap().invert(context);
    }

    /** rb_hash_merge_bang
     *
     */
    @JRubyMethod(name = {"merge!", "update"}, required = 1)
    public RubyHash merge_bang(final ThreadContext context, final IRubyObject other, final Block block) {
        return getOrCreateRubyHashMap().merge_bang19(context, other, block);
    }

    /** rb_hash_merge
     *
     */
    @JRubyMethod(name = "merge")
    public RubyHash merge(ThreadContext context, IRubyObject other, Block block) {
        return getOrCreateRubyHashMap().merge(context, other, block);
    }

    /** rb_hash_initialize_copy
     *
     */
    @JRubyMethod(name = "initialize_copy", visibility = Visibility.PRIVATE)
    public RubyHash initialize_copy(ThreadContext context, IRubyObject other) {
        return getOrCreateRubyHashMap().initialize_copy19(context, other);
    }

    /** rb_hash_replace
     *
     */
    @JRubyMethod(name = "replace", required = 1)
    public RubyHash replace(final ThreadContext context, IRubyObject other) {
        return getOrCreateRubyHashMap().replace19(context, other);
    }

    /** rb_hash_values_at
     *
     */
    @JRubyMethod(name = "values_at", rest = true)
    public RubyArray values_at(ThreadContext context, IRubyObject[] args) {
        return getOrCreateRubyHashMap().values_at(context, args);
    }

    @JRubyMethod(name = "assoc")
    public IRubyObject assoc(final ThreadContext context, final IRubyObject obj) {
        return getOrCreateRubyHashMap().assoc(context, obj);
    }

    @JRubyMethod(name = "rassoc")
    public IRubyObject rassoc(final ThreadContext context, final IRubyObject obj) {
        return getOrCreateRubyHashMap().rassoc(context, obj);
    }

    @JRubyMethod(name = "flatten")
    public IRubyObject flatten(ThreadContext context) {
        return getOrCreateRubyHashMap().flatten(context);
    }

    @JRubyMethod(name = "flatten")
    public IRubyObject flatten(ThreadContext context, IRubyObject level) {
        return getOrCreateRubyHashMap().flatten(context, level);
    }

    @JRubyMethod(name = "compare_by_identity")
    public IRubyObject getCompareByIdentity(ThreadContext context) {
        return getOrCreateRubyHashMap().getCompareByIdentity(context);
    }

    @JRubyMethod(name = "compare_by_identity?")
    public IRubyObject getCompareByIdentity_p(ThreadContext context) {
        return getOrCreateRubyHashMap().getCompareByIdentity_p(context);
    }

    @Override
    public IRubyObject dup() {
        try {
            MapJavaProxy proxy = new MapJavaProxy(getRuntime(), metaClass);
            Map newMap = (Map)getObject().getClass().newInstance();
            newMap.putAll((Map)getObject());
            proxy.setObject(newMap);
            return proxy;
        } catch (InstantiationException ex) {
            throw getRuntime().newNotImplementedError("can't dup Map of type " + getObject().getClass().getName());
        } catch (IllegalAccessException ex) {
            throw getRuntime().newNotImplementedError("can't dup Map of type " + getObject().getClass().getName());
        }
    }

    @Override
    public IRubyObject rbClone() {
        try {
            MapJavaProxy proxy = new MapJavaProxy(getRuntime(), metaClass);
            Map newMap = (Map)getObject().getClass().newInstance();
            newMap.putAll((Map)getObject());
            proxy.setObject(newMap);
            return proxy;
        } catch (InstantiationException ex) {
            throw getRuntime().newNotImplementedError("can't clone Map of type " + getObject().getClass().getName());
        } catch (IllegalAccessException ex) {
            throw getRuntime().newNotImplementedError("can't clone Map of type " + getObject().getClass().getName());
        }
    }

    @Deprecated
    public IRubyObject op_aset19(ThreadContext context, IRubyObject key, IRubyObject value) {
        return getOrCreateRubyHashMap().op_aset19(context, key, value);
    }
}
