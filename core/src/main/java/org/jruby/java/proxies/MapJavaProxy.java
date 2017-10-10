/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
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
import org.jruby.RubyProc;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.jruby.util.TypeConverter;

/**
 * A proxy for wrapping <code>java.util.Map</code> instances.
 *
 * @author Yoko Harada
 */
public final class MapJavaProxy extends ConcreteJavaProxy {

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
            wrappedMap.setSize( getMapObject().size() );
        }
        catch (RaiseException e) {
            wrappedMap.setSize(0);
            getRuntime().getGlobalVariables().set("$!", oldExc); // Restore $!
        }
        return wrappedMap;
    }

    private static final class RubyHashMap extends RubyHash {

        static final RubyHashEntry[] EMPTY_TABLE = new RubyHashEntry[0];

        private final MapJavaProxy receiver;

        RubyHashMap(Ruby runtime, MapJavaProxy receiver) {
            super(runtime, runtime.getHash(), runtime.getNil(), EMPTY_TABLE, 0);
            this.receiver = receiver;
        }

        private void syncSize() { this.size = mapDelegate().size(); }

        private void setSize(int size) { this.size = size; }

        // the underlying Map object operations should be delegated to
        private Map mapDelegate() { return receiver.getMapObject(); }

        @Override
        public RubyFixnum rb_size() {
            return getRuntime().newFixnum( mapDelegate().size() );
        }

        @Override
        public RubyBoolean empty_p() {
            return mapDelegate().isEmpty() ? getRuntime().getTrue() : getRuntime().getFalse();
        }

        @Override
        public IRubyObject inspect(ThreadContext context) {
            syncSize();
            return super.inspect(context);
        }

        @Override
        public RubyArray to_a() {
            syncSize();
            return super.to_a();
        }

        @Override
        public RubyFixnum hash() {
            return getRuntime().newFixnum( mapDelegate().hashCode() );
        }

        @Override
        public RubyArray keys() {
            syncSize();
            return super.keys();
        }

        @Override
        public RubyArray rb_values() {
            syncSize();
            return super.rb_values();
        }

        @Override
        public IRubyObject delete(ThreadContext context, IRubyObject key, Block block) {
            modify();

            Object value = mapDelegate().remove(key.toJava(Object.class));
            if ( value != null ) return JavaUtil.convertJavaToUsableRubyObject(getRuntime(), value);

            if ( block.isGiven() ) return block.yield(context, key);
            return context.nil;
        }

        @Override
        public RubyHash delete_ifInternal(final ThreadContext context, final Block block) {
            RubyHash self = super.delete_ifInternal(context, block);
            setSize( mapDelegate().size() );
            return self;
        }

        @Override
        public void internalPut(final IRubyObject key, final IRubyObject value, final boolean checkForExisting) {
            internalPutSmall(key, value, checkForExisting);
        }

        @Override
        protected final void internalPutSmall(IRubyObject key, IRubyObject value, boolean checkForExisting) {
            @SuppressWarnings("unchecked")
            final Map<Object, Object> map = mapDelegate();
            map.put(key.toJava(Object.class), value.toJava(Object.class));
            setSize( map.size() );
        }

        @Override
        protected final void op_asetForString(Ruby runtime, RubyString key, IRubyObject value) {
            @SuppressWarnings("unchecked")
            final Map<Object, Object> map = mapDelegate();
            map.put(key.decodeString(), value.toJava(Object.class));
            setSize( map.size() );
        }

        @Override
        protected final void op_asetSmallForString(Ruby runtime, RubyString key, IRubyObject value) {
            op_asetForString(runtime, key, value);
        }

        @Override
        public IRubyObject internalGet(IRubyObject key) {
            Object result = mapDelegate().get(key.toJava(Object.class));
            if (result == null) return null;
            return JavaUtil.convertJavaToUsableRubyObject(getRuntime(), result);
        }

        @Override // NOTE: likely won't be called
        public RubyHashEntry internalGetEntry(IRubyObject key) {
            Map map = mapDelegate();
            Object convertedKey = key.toJava(Object.class);
            Object value = map.get(convertedKey);

            if (value != null) {
                return new RubyHashEntry(key.hashCode(), key, JavaUtil.convertJavaToUsableRubyObject(getRuntime(), value), null, null);
            }

            return NO_ENTRY;
        }

        @Override
        public RubyHashEntry internalDelete(final IRubyObject key) {
            final Map map = mapDelegate();
            Object convertedKey = key.toJava(Object.class);
            Object value = map.get(convertedKey);

            if (value != null) {
                map.remove(convertedKey);
                setSize( map.size() );
                return new RubyHashEntry(key.hashCode(), key, JavaUtil.convertJavaToUsableRubyObject(getRuntime(), value), null, null);
            }
            return NO_ENTRY;
        }

        @Override // NOTE: likely won't be called
        public RubyHashEntry internalDeleteEntry(final RubyHashEntry entry) {
            final Map map = mapDelegate();
            Object convertedKey = ((IRubyObject) entry.getKey()).toJava(Object.class);

            if (map.containsKey(convertedKey)) {
                map.remove(convertedKey);
                setSize( map.size() );
                return entry;
            }

            return NO_ENTRY;
        }

        @Override
        public <T> void visitAll(ThreadContext context, VisitorWithState visitor, T state) {
            final Ruby runtime = getRuntime();
            // NOTE: this is here to make maps act similar to Hash-es which allow modifications while
            // iterating (meant from the same thread) ... thus we avoid iterating entrySet() directly
            final Map<Object, Object> map = mapDelegate();
            final Map.Entry[] entries = map.entrySet().toArray( new Map.Entry[map.size() ] );
            int index = 0;
            for ( Map.Entry entry : entries ) {
                IRubyObject key = JavaUtil.convertJavaToUsableRubyObject(runtime, entry.getKey());
                IRubyObject value = JavaUtil.convertJavaToUsableRubyObject(runtime, entry.getValue());
                visitor.visit(context, this, key, value, index++, state);
            }
        }

        @Override
        public RubyBoolean compare(final ThreadContext context, final VisitorWithState<RubyHash> method, IRubyObject other) {
            syncSize();
            if ( other instanceof RubyHashMap ) {
                ((RubyHashMap) other).syncSize();
            }
            return super.compare(context, method, other);
        }

        @Override
        public RubyBoolean has_key_p(IRubyObject key) {
            final Object convertedKey = key.toJava(Object.class);
            return getRuntime().newBoolean( mapDelegate().containsKey(convertedKey) );
        }

        @Override
        public RubyBoolean has_value_p(ThreadContext context, IRubyObject val) {
            final Object convertedVal = val.toJava(Object.class);
            return getRuntime().newBoolean( mapDelegate().containsValue(convertedVal) );
        }

        @Override
        public RubyHash rehash() {
            // java.util.Map does not expose rehash, and many maps don't use hashing, so we do nothing. #3142
            return this;
        }

        @Override
        public RubyBoolean compare_by_identity_p(ThreadContext context) {
            // NOTE: obviously little we can do to detect - but at least report Java built-in one :
            return context.runtime.newBoolean( mapDelegate() instanceof java.util.IdentityHashMap );
        }

        @Override // re-invent @JRubyMethod(name = "any?")
        public IRubyObject any_p(ThreadContext context, Block block) {
            if (isEmpty()) return context.runtime.getFalse();

            if (!block.isGiven()) return context.runtime.getTrue();

            if (block.getSignature().arityValue() > 1) {
                return any_p_i_fast(context, block);
            }
            return any_p_i(context, block);
        }

        private RubyBoolean any_p_i(ThreadContext context, Block block) {
            final Ruby runtime = context.runtime;
            for ( Map.Entry entry : entrySet() ) {
                final IRubyObject key = JavaUtil.convertJavaToUsableRubyObject(runtime, entry.getKey());
                final IRubyObject val = JavaUtil.convertJavaToUsableRubyObject(runtime, entry.getValue());
                if ( block.yield(context, RubyArray.newArray(runtime, key, val)).isTrue() ) {
                    return runtime.getTrue();
                }
            }
            return runtime.getFalse();
        }

        private RubyBoolean any_p_i_fast(ThreadContext context, Block block) {
            final Ruby runtime = context.runtime;
            for ( Map.Entry entry : entrySet() ) {
                final IRubyObject key = JavaUtil.convertJavaToUsableRubyObject(runtime, entry.getKey());
                final IRubyObject val = JavaUtil.convertJavaToUsableRubyObject(runtime, entry.getValue());
                if ( block.yieldArray(context, runtime.newArray(key, val), null).isTrue() ) {
                    return runtime.getTrue();
                }
            }
            return runtime.getFalse();
        }

        @Override
        public RubyHash rb_clear() {
            mapDelegate().clear();
            setSize( 0 );
            return this;
        }

        @Override
        public IRubyObject shift(ThreadContext context) {
            throw getRuntime().newNotImplementedError("Java Maps do not preserve insertion order and do not support shift");
        }

        @Override
        public RubyHash to_hash() {
            final Ruby runtime = getRuntime();
            final RubyHash hash = new RubyHash(runtime);
            @SuppressWarnings("unchecked")
            Set<Map.Entry> entries = mapDelegate().entrySet();
            for ( Map.Entry entry : entries ) {
                IRubyObject key = JavaUtil.convertJavaToUsableRubyObject(runtime, entry.getKey());
                IRubyObject value = JavaUtil.convertJavaToUsableRubyObject(runtime, entry.getValue());
                hash.fastASetCheckString(runtime, key, value);
            }
            return hash;
        }

        @Override
        public final Set keySet() { return mapDelegate().keySet(); }

        @Override
        public final Set directKeySet() { return keySet(); }

        @Override
        public final Collection values() { return mapDelegate().values(); }

        @Override
        public final Collection directValues() { return values(); }

        @Override
        public final Set<Map.Entry> entrySet() { return mapDelegate().entrySet(); }

        @Override
        public final Set directEntrySet() { return entrySet(); }

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
        return getOrCreateRubyHashMap().inspect(context);
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

    @JRubyMethod(name = "to_proc")
    public RubyProc to_proc(ThreadContext context) {
        IRubyObject newProc = getOrCreateRubyHashMap().callMethod("to_proc");

        TypeConverter.checkType(context, newProc, context.runtime.getProc());

        return (RubyProc) newProc;
    }

    /** rb_hash_to_s
     *
     */
    @JRubyMethod(name = "to_s")
    public IRubyObject to_s(ThreadContext context) {
        return getOrCreateRubyHashMap().to_s(context);
    }

    /** rb_hash_rehash
     *
     */
    @JRubyMethod(name = "rehash", notImplemented = true)
    public RubyHash rehash() {
        return getOrCreateRubyHashMap().rehash();
    }

    /** rb_hash_to_hash
     *
     */
    @JRubyMethod(name = { "to_hash", "to_h" })
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

    /** rb_hash_eql
     *
     */
    @JRubyMethod(name = "eql?")
    public IRubyObject op_eql(final ThreadContext context, IRubyObject other) {
        return getOrCreateRubyHashMap().op_eql(context, other);
    }

    /** rb_hash_aref
     *
     */
    @JRubyMethod(name = "[]", required = 1)
    public IRubyObject op_aref(ThreadContext context, IRubyObject key) {
        return getOrCreateRubyHashMap().op_aref(context, key);
    }

    @JRubyMethod(name = "<", required = 1)
    public IRubyObject op_lt(ThreadContext context, IRubyObject other) {
        return getOrCreateRubyHashMap().op_lt(context, other);
    }

    @JRubyMethod(name = "<=", required = 1)
    public IRubyObject op_le(ThreadContext context, IRubyObject other) {
        return getOrCreateRubyHashMap().op_le(context, other);
    }

    @JRubyMethod(name = ">", required = 1)
    public IRubyObject op_gt(ThreadContext context, IRubyObject other) {
        return getOrCreateRubyHashMap().op_gt(context, other);
    }

    @JRubyMethod(name = ">=", required = 1)
    public IRubyObject op_ge(ThreadContext context, IRubyObject other) {
        return getOrCreateRubyHashMap().op_ge(context, other);
    }

    /** rb_hash_hash
     *
     */
    @JRubyMethod(name = "hash")
    public RubyFixnum hash() {
        return getOrCreateRubyHashMap().hash();
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
    public RubyBoolean has_key_p(ThreadContext context, IRubyObject key) {
        return getOrCreateRubyHashMap().has_key_p(context, key);
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
    @JRubyMethod(name = {"each", "each_pair"})
    public IRubyObject each(final ThreadContext context, final Block block) {
        return getOrCreateRubyHashMap().each(context, block);
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

    /** rb_hash_index
     *
     */
    @JRubyMethod(name = "index")
    public IRubyObject index(ThreadContext context, IRubyObject expected) {
        return getOrCreateRubyHashMap().index(context, expected);
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
    @JRubyMethod(name = { "values", "ruby_values" }) // collision with java.util.Map#values
    public RubyArray rb_values() {
        return getOrCreateRubyHashMap().rb_values();
    }

    /** rb_hash_shift
     *
     */
    @JRubyMethod(name = "shift", notImplemented = true)
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
        return getOrCreateRubyHashMap().select(context, block);
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
    @JRubyMethod(name = { "clear", "ruby_clear" }) // collision with java.util.Map#clear (return type)
    public IRubyObject rb_clear() {
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
    @JRubyMethod(name = { "merge!", "update" }, required = 1)
    public RubyHash merge_bang(final ThreadContext context, final IRubyObject other, final Block block) {
        return getOrCreateRubyHashMap().merge_bang(context, other, block);
    }

    /** rb_hash_merge
     *
     */
    @JRubyMethod(name = { "merge", "ruby_merge" }) // collision with java.util.Map#merge on Java 8+
    public RubyHash merge(ThreadContext context, IRubyObject other, Block block) {
        return getOrCreateRubyHashMap().merge(context, other, block);
    }

    /** rb_hash_initialize_copy
     *
     */
    @JRubyMethod(name = "initialize_copy", visibility = Visibility.PRIVATE)
    public RubyHash initialize_copy(ThreadContext context, IRubyObject other) {
        return getOrCreateRubyHashMap().initialize_copy(context, other);
    }

    /** rb_hash_replace
     *
     */
    @JRubyMethod(name = { "replace", "ruby_replace" }, required = 1) // collision with java.util.Map#replace on Java 8+
    public RubyHash replace(final ThreadContext context, IRubyObject other) {
        return getOrCreateRubyHashMap().replace(context, other);
    }

    /** rb_hash_values_at
     *
     */
    @JRubyMethod(name = "values_at", rest = true)
    public RubyArray values_at(ThreadContext context, IRubyObject[] args) {
        return getOrCreateRubyHashMap().values_at(context, args);
    }

    @JRubyMethod(name = "fetch_values", rest = true)
    public RubyArray fetch_values(ThreadContext context, IRubyObject[] args, Block block) {
        return getOrCreateRubyHashMap().fetch_values(context, args, block);
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
    public IRubyObject compare_by_identity(ThreadContext context) {
        return this; // has no effect - mostly for compatibility
    }

    @JRubyMethod(name = "compare_by_identity?")
    public IRubyObject compare_by_identity_p(ThreadContext context) {
        return getOrCreateRubyHashMap().compare_by_identity_p(context);
    }

    @Override
    public IRubyObject dup() {
        return dupImpl("dup");
    }

    @Override
    public IRubyObject rbClone() {
        return dupImpl("clone");
    }

    @JRubyMethod(name = "any?")
    public IRubyObject any_p(ThreadContext context, Block block) {
        return getOrCreateRubyHashMap().any_p(context, block);
    }

    @JRubyMethod(name = "dig", required = 1, rest = true)
    public IRubyObject dig(ThreadContext context, IRubyObject[] args) {
        return getOrCreateRubyHashMap().dig(context, args);
    }

    @SuppressWarnings("unchecked")
    private MapJavaProxy dupImpl(final String method) {
        final Map map = getMapObject();
        try {
            Map newMap = map.getClass().newInstance();
            newMap.putAll(map);
            MapJavaProxy proxy = new MapJavaProxy(getRuntime(), metaClass);
            proxy.setObject(newMap);
            return proxy;
        }
        catch (InstantiationException|IllegalAccessException ex) {
            final RaiseException e = getRuntime().newNotImplementedError("can't "+ method +" Map of type " + getObject().getClass().getName());
            e.initCause(ex); throw e;
        }
    }

    final Map getMapObject() {
        return (Map) getObject();
    }

    @Override
    public final RubyHash convertToHash() {
        return getOrCreateRubyHashMap();
    }

    @Deprecated
    public IRubyObject op_aset19(ThreadContext context, IRubyObject key, IRubyObject value) {
        return getOrCreateRubyHashMap().op_aset19(context, key, value);
    }

    @Deprecated
    public IRubyObject sort(ThreadContext context, Block block) {
        return getOrCreateRubyHashMap().sort(context, block);
    }

}
