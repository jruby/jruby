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
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2006 Ola Bini <Ola.Bini@ki.se>
 * Copyright (C) 2006 Tim Azzopardi <tim@tigerfive.com>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2007 MenTaLguY <mental@rydia.net>
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

package org.jruby;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.Create;
import org.jruby.api.Error;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalDumper;
import org.jruby.runtime.marshal.MarshalLoader;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.RubyInputStream;
import org.jruby.util.io.RubyOutputStream;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.jruby.api.Access.arrayClass;
import static org.jruby.api.Access.hashClass;
import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.*;
import static org.jruby.runtime.ThreadContext.hasKeywords;
import static org.jruby.runtime.Visibility.PRIVATE;

// Design overview:
//
// RubyHash is implemented as hash table with a singly-linked list of
// RubyHash.RubyHashEntry objects for each bucket.  RubyHashEntry objects
// are also kept in a doubly-linked list which reflects their insertion
// order and is used for iteration.  For simplicity, this latter list is
// circular; a dummy RubyHashEntry, RubyHash.head, is used to mark the
// ends of the list.
//
// When an entry is removed from the table, it is also removed from the
// doubly-linked list.  However, while the reference to the previous
// RubyHashEntry is cleared (to mark the entry as dead), the reference
// to the next RubyHashEntry is preserved so that iterators are not
// invalidated: any iterator with a reference to a dead entry can climb
// back up into the list of live entries by chasing next references until
// it finds a live entry (or head).
//
// Ordinarily, this scheme would require O(N) time to clear a hash (since
// each RubyHashEntry would need to be visited and unlinked from the
// iteration list), but RubyHash also maintains a generation count.  Every
// time the hash is cleared, the doubly-linked list is simply discarded and
// the generation count incremented.  Iterators check to see whether the
// generation count has changed; if it has, they reset themselves back to
// the new start of the list.
//
// This design means that iterators are never invalidated by changes to the
// hashtable, and they do not need to modify the structure during their
// lifecycle.
//

/** Implementation of the Hash class.
 *
 *  Concurrency: no synchronization is required among readers, but
 *  all users must synchronize externally with writers.
 *
 */
@JRubyClass(name = "Hash", include="Enumerable",
        overrides = {RubyHashLinkedBuckets.class})
public class RubyHash extends RubyObject implements Map {
    public static final int DEFAULT_INSPECT_STR_SIZE = 20;

    /**
     * This field is used as part of the state of RubyHashLinkedBuckets, but when supporting legacy constructors it will
     * point at a delegated instance. Once all consumers of the deprecated constructor have been migrated, we move this
     * field back into the subclasses and eliminate all delegated methods.
     */
    protected Object state;

    protected static final byte COMPARE_BY_IDENTITY = 0b00000001;
    protected static final byte RUBY2_KEYWORD = 0b00000010;
    protected static final byte PROCDEFAULT_HASH = 0b00000100;

    protected static final RubyHashEntry NULL_ENTRY = new RubyHashEntry();

    @Deprecated(since = "10.0.3.0")
    public static final RubyHashEntry NO_ENTRY = NULL_ENTRY;

    public static RubyClass createHashClass(ThreadContext context, RubyClass Object, RubyModule Enumerable) {
        return defineClass(context, "Hash", Object, (runtime, klass) -> new RubyHashLinkedBuckets(runtime, klass)).
                reifiedClass(RubyHash.class).
                kindOf(new RubyModule.JavaClassKindOf(RubyHash.class)).
                classIndex(ClassIndex.HASH).
                include(context, Enumerable).
                defineMethods(context, RubyHash.class);
    }

    @Override
    public ClassIndex getNativeClassIndex() {
        return ClassIndex.HASH;
    }

    /** rb_hash_s_create
     *
     */
    @JRubyMethod(name = "[]", rest = true, meta = true)
    public static IRubyObject create(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        if (args.length == 1) {
            IRubyObject tmp = TypeConverter.convertToTypeWithCheck(args[0], hashClass(context), "to_hash");
            if (!tmp.isNil()) return new RubyHashLinkedBuckets(context.runtime, (RubyClass) recv, (RubyHash) tmp);

            var Array = arrayClass(context);
            final IRubyObject nil = context.nil;
            tmp = TypeConverter.convertToTypeWithCheck(args[0], Array, "to_ary");
            if (tmp != nil) {
                RubyHash hash = (RubyHash) ((RubyClass) recv).allocate(context);
                var arr = (RubyArray<?>) tmp;
                for (int i = 0, j = arr.getLength(); i<j; i++) {
                    IRubyObject e = arr.entry(i);
                    IRubyObject v = TypeConverter.convertToTypeWithCheck(e, Array, "to_ary");
                    IRubyObject key;
                    IRubyObject val = nil;
                    if (v == nil) {
                        throw argumentError(context, "wrong element type " + e.getMetaClass() + " at " + i + " (expected array)");
                    }
                    switch (((RubyArray<?>) v).getLength()) {
                    default:
                        throw argumentError(context, "invalid number of elements (" + ((RubyArray<?>) v).getLength() + " for 1..2)");
                    case 2:
                        val = ((RubyArray<?>) v).entry(1);
                    case 1:
                        key = ((RubyArray<?>) v).entry(0);
                        hash.fastASetCheckString(context.runtime, key, val);
                    }
                }
                return hash;
            }
        }

        if ((args.length & 1) != 0) throw argumentError(context, "odd number of arguments for Hash");

        RubyHash hash = (RubyHash) ((RubyClass) recv).allocate(context);
        for (int i=0; i < args.length; i+=2) hash.fastASetCheckString(context.runtime, args[i], args[i+1]);

        return hash;
    }

    @JRubyMethod(name = "try_convert", meta = true)
    public static IRubyObject try_convert(ThreadContext context, IRubyObject recv, IRubyObject args) {
        return TypeConverter.convertToTypeWithCheck(args, hashClass(context), "to_hash");
    }

    /** rb_hash_new
     *
     */
    public static final RubyHash newHash(Ruby runtime) {
        return RubyHashLinkedBuckets.newLBHash(runtime);
    }

    /** rb_hash_new
     *
     */
    public static final RubyHash newSmallHash(Ruby runtime) {
        return RubyHashLinkedBuckets.newLBHash(runtime, 1);
    }

    public static RubyHash newKwargs(Ruby runtime, String key, IRubyObject value) {
        return newHash(runtime, runtime.newSymbol(key), value);
    }

    public static RubyHash newHash(Ruby runtime, IRubyObject key, IRubyObject value) {
        RubyHash kwargs = newSmallHash(runtime);
        kwargs.fastASetSmall(key, value);
        return kwargs;
    }

    /** rb_hash_new
     *
     */
    public static final RubyHash newHash(Ruby runtime, Map valueMap, IRubyObject defaultValue) {
        assert defaultValue != null;

        return new RubyHashLinkedBuckets(runtime, valueMap, defaultValue);
    }

    // Only constructor that does not delegate
    protected RubyHash(Ruby runtime, RubyClass klass, boolean objectSpace, int unused) {
        super(runtime, klass, objectSpace);

        // ensure only subclasses call this constructor
        assert this.getClass() != RubyHash.class;
    }

    // Delegated constructor, to be hidden and returned to normal super constructor once no longer in use
    @Deprecated(since = "10.0.3.0")
    public RubyHash(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
        // ensure no subclasses call this constructor
        assert getClass() == RubyHash.class;
        setDelegate(this);
    }

    // Delegated constructor, to be hidden and returned to normal super constructor once no longer in use
    @Deprecated(since = "10.0.3.0")
    public RubyHash(Ruby runtime, RubyClass klass, boolean objectSpace) {
        super(runtime, klass, objectSpace);
        // ensure no subclasses call this constructor
        assert getClass() == RubyHash.class;
        this.setDelegate(new RubyHashLinkedBuckets(runtime, klass, objectSpace));
    }

    // Delegated constructor, to be hidden and returned to normal super constructor once no longer in use
    @Deprecated(since = "10.0.3.0")
    public RubyHash(Ruby runtime) {
        super(runtime, runtime.getHash());
        // ensure no subclasses call this constructor
        assert getClass() == RubyHash.class;
        this.setDelegate(RubyHashLinkedBuckets.newLBHash(runtime));
    }

    // Delegated constructor, to be hidden and returned to normal super constructor once no longer in use
    @Deprecated(since = "10.0.3.0")
    public RubyHash(Ruby runtime, IRubyObject defaultValue) {
        super(runtime, runtime.getHash());
        // ensure no subclasses call this constructor
        assert getClass() == RubyHash.class;
        this.setDelegate(RubyHashLinkedBuckets.newLBHash(runtime, defaultValue));
    }

    /* ============================
     * Here are hash internals
     * (This could be extracted to a separate class but it's not too large though)
     * ============================
     */

    @Deprecated(since = "10.0.3.0")
    public static final int MRI_PRIMES[] = {
        8 + 3, 16 + 3, 32 + 5, 64 + 3, 128 + 3, 256 + 27, 512 + 9, 1024 + 9, 2048 + 5, 4096 + 3,
        8192 + 27, 16384 + 43, 32768 + 3, 65536 + 45, 131072 + 29, 262144 + 3, 524288 + 21, 1048576 + 7,
        2097152 + 17, 4194304 + 15, 8388608 + 9, 16777216 + 43, 33554432 + 35, 67108864 + 15,
        134217728 + 29, 268435456 + 3, 536870912 + 11, 1073741824 + 85, 0
    };

    private RubyHash getDelegate() {
        return (RubyHash) state;
    }

    private void setDelegate(RubyHash state) {
        this.state = state;
    }

    public static final class RubyHashEntry implements Map.Entry {
        final IRubyObject key;
        IRubyObject value;
        RubyHashEntry next;
        RubyHashEntry prevAdded;
        RubyHashEntry nextAdded;
        final int hash;

        RubyHashEntry() {
            key = NEVER;
            hash = -1;
        }

        public RubyHashEntry(int h, IRubyObject k, IRubyObject v, RubyHashEntry e, RubyHashEntry head) {
            key = k;
            value = v;
            next = e;
            hash = h;

            if (head != null) {
                RubyHashEntry prevAdded = head.prevAdded;
                RubyHashEntry nextAdded = head;

                this.prevAdded = prevAdded;
                prevAdded.nextAdded = this;

                this.nextAdded = nextAdded;
                nextAdded.prevAdded = this;
            }
        }

        public RubyHashEntry(RubyHashEntry oldEntry, int newHash) {
            this.hash = newHash;
            this.key = oldEntry.key;
            this.value = oldEntry.value;
            this.next = oldEntry.next;

            // prevAdded is never null
            RubyHashEntry prevAdded = oldEntry.prevAdded;
            this.prevAdded = prevAdded;
            prevAdded.nextAdded = this;

            RubyHashEntry nextAdded = oldEntry.nextAdded;
            if (nextAdded != null) {
                this.nextAdded = nextAdded;
                nextAdded.prevAdded = this;
            }
        }

        public void detach() {
            if (prevAdded != null) {
                prevAdded.nextAdded = nextAdded;
                nextAdded.prevAdded = prevAdded;
                prevAdded = null;
            }
        }

        public boolean isLive() {
            return prevAdded != null;
        }

        @Override
        public Object getKey() {
            return key;
        }
        public Object getJavaifiedKey(){
            return key.toJava(Object.class);
        }

        @Override
        public Object getValue() {
            return value;
        }
        public Object getJavaifiedValue() {
            return value.toJava(Object.class);
        }

        @Override
        public Object setValue(Object value) {
            IRubyObject oldValue = this.value;
            if (value instanceof IRubyObject) {
                this.value = (IRubyObject)value;
            } else {
                throw new UnsupportedOperationException("directEntrySet() doesn't support setValue for non IRubyObject instance entries, convert them manually or use entrySet() instead");
            }
            return oldValue;
        }

        @Override
        public boolean equals(Object other){
            if(!(other instanceof RubyHashEntry)) return false;
            RubyHashEntry otherEntry = (RubyHashEntry)other;

            return (key == otherEntry.key || key.eql(otherEntry.key)) &&
                    (value == otherEntry.value || value.equals(otherEntry.value));
        }

        @Override
        public int hashCode(){
            return key.hashCode() ^ value.hashCode();
        }
    }

    // put implementation

    public IRubyObject internalPut(final IRubyObject key, final IRubyObject value) {
        return getDelegate().internalPut(key, value);
    }

    boolean internalPutIfNoKey(final IRubyObject key, final IRubyObject value) {
        return getDelegate().internalPutIfNoKey(key, value);
    }

    // get implementation
    protected IRubyObject internalGet(IRubyObject key) {
        return getDelegate().internalGet(key);
    }

    @Deprecated(since = "10.0.3.0")
    RubyHashEntry getEntry(IRubyObject key) {
        return getDelegate().getEntry(key);
    }

    public interface VisitorWithStateI {
        void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index);
    }

    public static abstract class VisitorWithState<T> {
        public abstract void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, T state);
    }

    public static abstract class Visitor extends VisitorWithState {
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, Object state) {
            visit(key, value);
        }

        public abstract void visit(IRubyObject key, IRubyObject value);
    }

    public <T> void visitAll(ThreadContext context, VisitorWithStateI visitor) {
        getDelegate().visitAll(context, visitor);
    }

    public <T> void visitAll(ThreadContext context, VisitorWithState visitor, T state) {
        this.getDelegate().visitAll(context, visitor, state);
    }

    protected <T> void visitLimited(ThreadContext context, RubyHash.VisitorWithState visitor, long size, T state) {
        this.getDelegate().visitLimited(context, visitor, size, state);
    }

    public <T> boolean allSymbols() {
        return getDelegate().allSymbols();
    }

    /* ============================
     * End of hash internals
     * ============================
     */

    /*  ================
     *  Instance Methods
     *  ================
     */

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, final Block block) {
        return getDelegate().initialize(context, block);
    }

    @JRubyMethod(visibility = PRIVATE, keywords = true)
    public IRubyObject initialize(ThreadContext context, IRubyObject _default, final Block block) {
        return getDelegate().initialize(context, _default, block);
    }

    @JRubyMethod(visibility = PRIVATE, keywords = true)
    public IRubyObject initialize(ThreadContext context, IRubyObject _default, IRubyObject hash, final Block block) {
        return getDelegate().initialize(context, _default, hash, block);
    }

    @JRubyMethod(name = "default")
    public IRubyObject default_value_get(ThreadContext context) {
        return getDelegate().default_value_get(context);
    }

    @JRubyMethod(name = "default")
    public IRubyObject default_value_get(ThreadContext context, IRubyObject arg) {
        return getDelegate().default_value_get(context, arg);
    }

    @JRubyMethod(name = "default=")
    public IRubyObject default_value_set(ThreadContext context, final IRubyObject defaultValue) {
        return getDelegate().default_value_set(context, defaultValue);
    }

    @JRubyMethod
    public IRubyObject default_proc(ThreadContext context) {
        return getDelegate().default_proc(context);
    }

    @JRubyMethod(name = "default_proc=")
    public IRubyObject set_default_proc(ThreadContext context, IRubyObject proc) {
        return getDelegate().set_default_proc(context, proc);
    }

    public void modify() {
        getDelegate().modify();
    }

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        return getDelegate().inspect(context);
    }

    @JRubyMethod(name = {"size", "length"})
    public RubyFixnum rb_size(ThreadContext context) {
        return getDelegate().rb_size(context);
    }

    @JRubyMethod(name = "empty?")
    public RubyBoolean empty_p(ThreadContext context) {
        return getDelegate().empty_p(context);
    }

    @JRubyMethod(name = "to_a")
    @Override
    public RubyArray to_a(ThreadContext context) {
        return getDelegate().to_a(context);
    }

    @JRubyMethod(name = "to_s")
    public IRubyObject to_s(ThreadContext context) {
        return getDelegate().to_s(context);
    }

    @JRubyMethod(name = "rehash")
    public RubyHash rehash(ThreadContext context) {
        return getDelegate().rehash(context);
    }

    @JRubyMethod(name = "to_hash")
    public RubyHash to_hash(ThreadContext context) {
        return getDelegate().to_hash(context);
    }

    @JRubyMethod
    public RubyHash to_h(ThreadContext context, Block block) {
        return getDelegate().to_h(context, block);
    }

    protected RubyHash to_h_block(ThreadContext context, Block block) {
        return getDelegate().to_h_block(context, block);
    }

    @Override
    public RubyHash convertToHash() {
        return getDelegate().convertToHash();
    }

    public void fastASet(IRubyObject key, IRubyObject value) {
        getDelegate().fastASet(key, value);
    }

    public void fastASetSmall(IRubyObject key, IRubyObject value) {
        getDelegate().fastASetSmall(key, value);
    }

    // MRI: rb_hash_set_pair, fast/small version
    public void fastASetSmallPair(ThreadContext context, IRubyObject _pair) {
        getDelegate().fastASetSmallPair(context, _pair);
    }

    public void fastASetCheckString(Ruby runtime, IRubyObject key, IRubyObject value) {
        getDelegate().fastASetCheckString(runtime, key, value);
    }

    public void fastASetSmallCheckString(Ruby runtime, IRubyObject key, IRubyObject value) {
        getDelegate().fastASetSmallCheckString(runtime, key, value);
    }

    public void fastASet(Ruby runtime, IRubyObject key, IRubyObject value, boolean prepareString) {
        getDelegate().fastASet(runtime, key, value, prepareString);
    }

    public void fastASetSmall(Ruby runtime, IRubyObject key, IRubyObject value, boolean prepareString) {
        getDelegate().fastASetSmall(runtime, key, value, prepareString);
    }

    /**
     * Set a key/value pair into this hash.
     *
     * @param context the current thread context
     * @param key     the key
     * @param value   the value
     * @return the value set
     */
    @JRubyMethod(name = {"[]=", "store"})
    public IRubyObject op_aset(ThreadContext context, IRubyObject key, IRubyObject value) {
        return getDelegate().op_aset(context, key, value);
    }

    // returns null when not found to avoid unnecessary getRuntime().getNil() call
    public IRubyObject fastARef(IRubyObject key) {
        return getDelegate().fastARef(key);
    }

    public RubyBoolean compare(final ThreadContext context, VisitorWithState<RubyHash> visitor, IRubyObject other, boolean eql) {
        return getDelegate().compare(context, visitor, other, eql);
    }

    @Override
    @JRubyMethod(name = "==")
    public IRubyObject op_equal(final ThreadContext context, IRubyObject other) {
        return getDelegate().op_equal(context, other);
    }

    @JRubyMethod(name = "eql?")
    public IRubyObject op_eql(final ThreadContext context, IRubyObject other) {
        return getDelegate().op_eql(context, other);
    }

    @JRubyMethod(name = "[]")
    public IRubyObject op_aref(ThreadContext context, IRubyObject key) {
        return getDelegate().op_aref(context, key);
    }

    @JRubyMethod(name = "<")
    public IRubyObject op_lt(ThreadContext context, IRubyObject other) {
        return getDelegate().op_lt(context, other);
    }

    @JRubyMethod(name = "<=")
    public IRubyObject op_le(ThreadContext context, IRubyObject other) {
        return getDelegate().op_le(context, other);
    }

    @JRubyMethod(name = ">")
    public IRubyObject op_gt(ThreadContext context, IRubyObject other) {
        return getDelegate().op_gt(context, other);
    }

    @JRubyMethod(name = ">=")
    public IRubyObject op_ge(ThreadContext context, IRubyObject other) {
        return getDelegate().op_ge(context, other);
    }

    // MRI: rb_hash_hash
    @JRubyMethod(name = "hash")
    public RubyFixnum hash(ThreadContext context) {
        return getDelegate().hash(context);
    }

    public IRubyObject fetch(ThreadContext context, IRubyObject[] args, Block block) {
        return getDelegate().fetch(context, args, block);
    }

    @JRubyMethod(rest = true)
    public IRubyObject except(ThreadContext context, IRubyObject[] keys) {
        return getDelegate().except(context, keys);
    }

    @JRubyMethod
    public IRubyObject fetch(ThreadContext context, IRubyObject key, Block block) {
        return getDelegate().fetch(context, key, block);
    }

    @JRubyMethod
    public IRubyObject fetch(ThreadContext context, IRubyObject key, IRubyObject _default, Block block) {
        return getDelegate().fetch(context, key, _default, block);
    }

    @JRubyMethod(name = {"has_key?", "key?", "include?", "member?"})
    public RubyBoolean has_key_p(ThreadContext context, IRubyObject key) {
        return getDelegate().has_key_p(context, key);
    }

    /**
     * A Java API to test the presence of a (Ruby) key in the Hash
     *
     * @param key the native (Ruby) key
     * @return true if the hash contains the provided key
     */
    public boolean hasKey(IRubyObject key) {
        return getDelegate().hasKey(key);
    }

    @JRubyMethod(name = {"has_value?", "value?"})
    public RubyBoolean has_value_p(ThreadContext context, IRubyObject expected) {
        return getDelegate().has_value_p(context, expected);
    }

    public RubyHash eachCommon(final ThreadContext context, final Block block) {
        return getDelegate().eachCommon(context, block);
    }

    @JRubyMethod(name = {"each", "each_pair"})
    public IRubyObject each(final ThreadContext context, final Block block) {
        return getDelegate().each(context, block);
    }

    public RubyHash each_pairCommon(final ThreadContext context, final Block block) {
        return getDelegate().each_pairCommon(context, block);
    }

    public RubyHash each_valueCommon(final ThreadContext context, final Block block) {
        return getDelegate().each_valueCommon(context, block);
    }

    @JRubyMethod
    public IRubyObject each_value(final ThreadContext context, final Block block) {
        return getDelegate().each_value(context, block);
    }

    public RubyHash each_keyCommon(final ThreadContext context, final Block block) {
        return getDelegate().each_keyCommon(context, block);
    }

    @JRubyMethod
    public IRubyObject each_key(final ThreadContext context, final Block block) {
        return getDelegate().each_key(context, block);
    }

    @JRubyMethod(name = "transform_keys", rest = true)
    public IRubyObject transform_keys(final ThreadContext context, IRubyObject[] args, final Block block) {
        return getDelegate().transform_keys(context, args, block);
    }

    @JRubyMethod(name = "transform_values")
    public IRubyObject transform_values(final ThreadContext context, final Block block) {
        return getDelegate().transform_values(context, block);
    }

    @JRubyMethod(name = "transform_keys!", rest = true)
    public IRubyObject transform_keys_bang(final ThreadContext context, IRubyObject[] args, final Block block) {
        return getDelegate().transform_keys_bang(context, args, block);
    }

    @JRubyMethod(name = "transform_values!")
    public IRubyObject transform_values_bang(final ThreadContext context, final Block block) {
        return getDelegate().transform_values_bang(context, block);
    }

    @JRubyMethod(name = "select!", alias = "filter!")
    public IRubyObject select_bang(final ThreadContext context, final Block block) {
        return getDelegate().select_bang(context, block);
    }

    @JRubyMethod
    public IRubyObject keep_if(final ThreadContext context, final Block block) {
        return getDelegate().keep_if(context, block);
    }

    public boolean keep_ifCommon(final ThreadContext context, final Block block) {
        return getDelegate().keep_ifCommon(context, block);
    }

    @JRubyMethod
    public IRubyObject key(ThreadContext context, IRubyObject expected) {
        return getDelegate().key(context, expected);
    }

    @JRubyMethod(name = "keys")
    public RubyArray keys(final ThreadContext context) {
        return getDelegate().keys(context);
    }

    public RubyArray keys() {
        return getDelegate().keys();
    }

    @JRubyMethod(name = "values")
    public RubyArray values(final ThreadContext context) {
        return getDelegate().values(context);
    }

    public RubyArray rb_values(ThreadContext context) {
        return getDelegate().rb_values(context);
    }

    public static final VisitorWithState<RubyArray> StoreValueVisitor = new VisitorWithState<RubyArray>() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, RubyArray values) {
            values.storeInternal(context, index, value);
        }
    };

    // like RubyHash.StoreValueVisitor but 'unsafe' - user needs to assure array capacity and adjust length
    static final VisitorWithState SetValueVisitor = new VisitorWithState<RubyArray>() {
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject val, int index, RubyArray target) {
            target.eltInternalSet(index, val);
        }
    };

    @JRubyMethod(name = "shift")
    public IRubyObject shift(ThreadContext context) {
        return getDelegate().shift(context);
    }

    public boolean fastDelete(IRubyObject key) {
        return getDelegate().fastDelete(key);
    }

    @JRubyMethod
    public IRubyObject delete(ThreadContext context, IRubyObject key, Block block) {
        return getDelegate().delete(context, key, block);
    }

    public IRubyObject delete(IRubyObject key) {
        return getDelegate().delete(key);
    }

    public IRubyObject delete(ThreadContext context, IRubyObject key) {
        return getDelegate().delete(context, key);
    }

    @JRubyMethod(name = "select", alias = "filter")
    public IRubyObject select(final ThreadContext context, final Block block) {
        return getDelegate().select(context, block);
    }

    @JRubyMethod(name = "slice", rest = true)
    public RubyHash slice(final ThreadContext context, final IRubyObject[] args) {
        return getDelegate().slice(context, args);
    }

    public RubyHash delete_ifInternal(ThreadContext context, Block block) {
        return getDelegate().delete_ifInternal(context, block);
    }

    @JRubyMethod
    public IRubyObject delete_if(final ThreadContext context, final Block block) {
        return getDelegate().delete_if(context, block);
    }

    public RubyHash rejectInternal(ThreadContext context, Block block) {
        return getDelegate().rejectInternal(context, block);
    }

    @JRubyMethod
    public IRubyObject reject(final ThreadContext context, final Block block) {
        return getDelegate().reject(context, block);
    }

    public IRubyObject reject_bangInternal(ThreadContext context, Block block) {
        return getDelegate().reject_bangInternal(context, block);
    }

    @JRubyMethod(name = "reject!")
    public IRubyObject reject_bang(final ThreadContext context, final Block block) {
        return getDelegate().reject_bang(context, block);
    }

    @JRubyMethod(name = "clear")
    public RubyHash rb_clear(ThreadContext context) {
        return getDelegate().rb_clear(context);
    }

    @JRubyMethod(name = "invert")
    public RubyHash invert(final ThreadContext context) {
        return getDelegate().invert(context);
    }

    @JRubyMethod(name = {"merge!", "update"}, rest = true)
    public RubyHash merge_bang(ThreadContext context, IRubyObject[] others, Block block) {
        return getDelegate().merge_bang(context, others, block);
    }

    public void addAll(ThreadContext context, RubyHash otherHash) {
        getDelegate().addAll(context, otherHash);
    }

    @JRubyMethod(rest = true)
    public RubyHash merge(ThreadContext context, IRubyObject[] others, Block block) {
        return getDelegate().merge(context, others, block);
    }

    @JRubyMethod(name = "initialize_copy", visibility = PRIVATE)
    public RubyHash initialize_copy(ThreadContext context, IRubyObject other) {
        return getDelegate().initialize_copy(context, other);
    }

    @JRubyMethod(name = "replace")
    public RubyHash replace(final ThreadContext context, IRubyObject other) {
        return getDelegate().replace(context, other);
    }

    @JRubyMethod(name = "values_at", rest = true)
    public RubyArray values_at(ThreadContext context, IRubyObject[] args) {
        return getDelegate().values_at(context, args);
    }

    @JRubyMethod(name = "fetch_values", rest = true)
    public RubyArray fetch_values(ThreadContext context, IRubyObject[] args, Block block) {
        return getDelegate().fetch_values(context, args, block);
    }

    @JRubyMethod(name = "assoc")
    public IRubyObject assoc(final ThreadContext context, final IRubyObject obj) {
        return getDelegate().assoc(context, obj);
    }

    @JRubyMethod(name = "rassoc")
    public IRubyObject rassoc(final ThreadContext context, final IRubyObject obj) {
        return getDelegate().rassoc(context, obj);
    }

    @JRubyMethod
    public IRubyObject flatten(ThreadContext context) {
        return getDelegate().flatten(context);
    }

    @JRubyMethod
    public IRubyObject flatten(ThreadContext context, IRubyObject level) {
        return getDelegate().flatten(context, level);
    }

    @JRubyMethod(name = "compact")
    public IRubyObject compact(ThreadContext context) {
        return getDelegate().compact(context);
    }

    @JRubyMethod(name = "compact!")
    public IRubyObject compact_bang(ThreadContext context) {
        return getDelegate().compact_bang(context);
    }

    @JRubyMethod(name = "compare_by_identity")
    public IRubyObject compare_by_identity(ThreadContext context) {
        return getDelegate().compare_by_identity(context);
    }

    @JRubyMethod(name = "compare_by_identity?")
    public IRubyObject compare_by_identity_p(ThreadContext context) {
        return getDelegate().compare_by_identity_p(context);
    }

    @JRubyMethod
    public IRubyObject dup(ThreadContext context) {
        return getDelegate().dup(context);
    }

    public IRubyObject rbClone(ThreadContext context, IRubyObject opts) {
        return super.rbClone(context, opts);
    }

    public IRubyObject rbClone(ThreadContext context) {
        return super.rbClone(context, context.nil);
    }

    @JRubyMethod(name = "any?")
    public IRubyObject any_p(ThreadContext context, Block block) {
        return getDelegate().any_p(context, block);
    }

    @JRubyMethod(name = "any?")
    public IRubyObject any_p(ThreadContext context, IRubyObject pattern, Block block) {
        return getDelegate().any_p(context, pattern, block);
    }

    /**
     * A lightweight dup for internal use that does not dispatch to initialize_copy nor rehash the keys. Intended for
     * use in dup'ing keyword args for processing.
     *
     * @param context
     * @return
     */
    public RubyHash dupFast(final ThreadContext context) {
        return getDelegate().dupFast(context);
    }

    public RubyHash withRuby2Keywords(boolean ruby2Keywords) {
        return getDelegate().withRuby2Keywords(ruby2Keywords);
    }

    public boolean hasDefaultProc() {
        return getDelegate().hasDefaultProc();
    }

    public IRubyObject getIfNone() {
        return getDelegate().getIfNone();
    }

    @JRubyMethod
    public IRubyObject deconstruct_keys(ThreadContext context, IRubyObject _arg1) {
        return getDelegate().deconstruct_keys(context, _arg1);
    }

    @JRubyMethod(name = "dig")
    public IRubyObject dig(ThreadContext context, IRubyObject arg0) {
        return getDelegate().dig(context, arg0);
    }

    @JRubyMethod(name = "dig")
    public IRubyObject dig(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return getDelegate().dig(context, arg0, arg1);
    }

    @JRubyMethod(name = "dig")
    public IRubyObject dig(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return getDelegate().dig(context, arg0, arg1, arg2);
    }

    @JRubyMethod(name = "dig", required = 1, rest = true, checkArity = false)
    public IRubyObject dig(ThreadContext context, IRubyObject[] args) {
        return getDelegate().dig(context, args);
    }

    @JRubyMethod
    public IRubyObject to_proc(ThreadContext context) {
        return getDelegate().to_proc(context);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject ruby2_keywords_hash(ThreadContext context, IRubyObject _self, IRubyObject arg) {
        TypeConverter.checkType(context, arg, hashClass(context));

        RubyHash hash = (RubyHash) arg.dup();
        hash.setRuby2KeywordHash(true);

        return hash;
    }

    @JRubyMethod(meta = true, name = "ruby2_keywords_hash?")
    public static IRubyObject ruby2_keywords_hash_p(ThreadContext context, IRubyObject _self, IRubyObject arg) {
        TypeConverter.checkType(context, arg, hashClass(context));

        return asBoolean(context, ((RubyHash) arg).isRuby2KeywordHash());
    }

    protected static class VisitorIOException extends RuntimeException {
        VisitorIOException(Throwable cause) {
            super(cause);
        }
    }

    // FIXME:  Total hack to get flash in Rails marshalling/unmarshalling in session ok...We need
    // to totally change marshalling to work with overridden core classes.
    @Deprecated(since = "10.0.0.0", forRemoval = true)
    @SuppressWarnings("removal")
    public static void marshalTo(final RubyHash hash, final org.jruby.runtime.marshal.MarshalStream output) throws IOException {
        var context = hash.getRuntime().getCurrentContext();
        output.registerLinkTarget(context, hash);
       int hashSize = hash.size();
       output.writeInt(hashSize);
        try {
            hash.visitLimited(context, MarshalDumpVisitor, hashSize, output);
        } catch (VisitorIOException e) {
            throw (IOException)e.getCause();
        }

        if (hash.getIfNone() != UNDEF) output.dumpObject(hash.getIfNone());
    }

    public static void marshalTo(ThreadContext context, RubyOutputStream out, final RubyHash hash, final MarshalDumper output) {
        output.registerObject(hash);
        int hashSize = hash.size();
        output.writeInt(out, hashSize);
        try {
            hash.visitLimited(context, new VisitorWithState<MarshalDumper>() {
                @Override
                public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, MarshalDumper state) {
                    state.dumpObject(context, out, key);
                    state.dumpObject(context, out, value);
                }
            }, hashSize, output);
        } catch (VisitorIOException e) {
            throw Error.toRubyException(context, (IOException) e.getCause());
        }

        if (hash.getIfNone() != UNDEF) output.dumpObject(context, out, hash.getIfNone());
    }

    @Deprecated(since = "10.0.0.0", forRemoval = true)
    @SuppressWarnings("removal")
    private static final VisitorWithState<org.jruby.runtime.marshal.MarshalStream> MarshalDumpVisitor = new VisitorWithState<>() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, org.jruby.runtime.marshal.MarshalStream output) {
            try {
                output.dumpObject(key);
                output.dumpObject(value);
            } catch (IOException e) {
                throw new VisitorIOException(e);
            }
        }
    };

    @Deprecated(since = "10.0.0.0", forRemoval = true)
    @SuppressWarnings("removal")
    public static RubyHash unmarshalFrom(org.jruby.runtime.marshal.UnmarshalStream input, boolean defaultValue) throws IOException {
        Ruby runtime = input.getRuntime();
        RubyHash result = (RubyHash) input.entry(newHash(runtime));
        int size = input.unmarshalInt();

        for (int i = 0; i < size; i++) {
            result.fastASetCheckString(runtime, input.unmarshalObject(), input.unmarshalObject());
        }

        if (defaultValue) result.default_value_set(runtime.getCurrentContext(), input.unmarshalObject());
        return result;
    }

    public static RubyHash unmarshalFrom(ThreadContext context, RubyInputStream in, MarshalLoader input, boolean defaultValue, boolean identity) {
        RubyHash result = Create.newHash(context);
        if (identity) result.setComparedByIdentity(true);
        result = (RubyHash) input.entry(result);
        int size = input.unmarshalInt(context, in);

        for (int i = 0; i < size; i++) {
            result.fastASetCheckString(context.runtime, input.unmarshalObject(context, in), input.unmarshalObject(context, in));
        }

        if (defaultValue) result.default_value_set(context, input.unmarshalObject(context, in));
        return result;
    }

    @Override
    public Class getJavaClass() {
        return Map.class;
    }


    // Satisfy java.util.Map interface (for Java integration)

    @Override
    public int size() {
        return getDelegate().size();
    }

    @Override
    public boolean isEmpty() {
        return getDelegate().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return getDelegate().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return getDelegate().containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return getDelegate().get(key);
    }

    @Override
    public Object put(Object key, Object value) {
        return getDelegate().put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return getDelegate().remove(key);
    }

    @Override
    public void putAll(Map map) {
        getDelegate().putAll(map);
    }

    @Override
    public void clear() {
        getDelegate().clear();
    }

    @Override
    public boolean equals(Object other) {
        return getDelegate().equals(other);
    }

    @Override
    public Set keySet() {
        return getDelegate().keySet();
    }

    @Override
    public Collection values() {
        return getDelegate().values();
    }

    @Override
    public Set entrySet() {
        return getDelegate().entrySet();
    }

    public Set directKeySet() {
        return getDelegate().directKeySet();
    }

    public Collection directValues() {
        return getDelegate().directValues();
    }

    public Set directEntrySet() {
        return getDelegate().directEntrySet();
    }

    public boolean isComparedByIdentity() {
        return getDelegate().isComparedByIdentity();
    }

    public void setComparedByIdentity(boolean comparedByIdentity) {
        getDelegate().setComparedByIdentity(comparedByIdentity);
    }

    public boolean isRuby2KeywordHash() {
        return getDelegate().isRuby2KeywordHash();
    }

    public void setRuby2KeywordHash(boolean value) {
        getDelegate().setRuby2KeywordHash(value);
    }

    protected void set(int flag, boolean set) {
        getDelegate().set(flag, set);
    }

    protected boolean get(int flag) {
        return getDelegate().get(flag);
    }

    // Still used by jruby-openssl
    @Deprecated(since = "9.1.3.0")
    public final void visitAll(Visitor visitor) {
        // use -1 to disable concurrency checks
        visitLimited(getRuntime().getCurrentContext(), visitor, -1, null);
    }
}
