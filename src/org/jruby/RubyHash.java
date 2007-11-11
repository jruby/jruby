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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;

/** Implementation of the Hash class.
 *
 * @author  jpetersen
 */
public class RubyHash extends RubyObject implements Map {

    public static RubyClass createHashClass(Ruby runtime) {
        RubyClass hashc = runtime.defineClass("Hash", runtime.getObject(), HASH_ALLOCATOR);
        runtime.setHash(hashc);
        hashc.index = ClassIndex.HASH;
        hashc.kindOf = new RubyModule.KindOf() {
            public boolean isKindOf(IRubyObject obj, RubyModule type) {
                return obj instanceof RubyHash;
            }
        };
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyHash.class);

        hashc.includeModule(runtime.getEnumerable());

        hashc.defineAnnotatedMethods(RubyHash.class);
        hashc.dispatcher = callbackFactory.createDispatcher(hashc);

        return hashc;
    }

    private final static ObjectAllocator HASH_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyHash(runtime, klass);
        }
    };

    public int getNativeTypeIndex() {
        return ClassIndex.HASH;
    }

    /** rb_hash_s_create
     *
     */
    @JRubyMethod(name = "[]", rest = true, frame = true, meta = true)
    public static IRubyObject create(IRubyObject recv, IRubyObject[] args, Block block) {
        RubyClass klass = (RubyClass) recv;
        RubyHash hash;

        if (args.length == 1 && args[0] instanceof RubyHash) {
            RubyHash otherHash = (RubyHash)args[0];
            return new RubyHash(recv.getRuntime(), klass, otherHash.internalCopyTable(), otherHash.size); // hash_alloc0
        }

        if ((args.length & 1) != 0) throw recv.getRuntime().newArgumentError("odd number of args for Hash");

        hash = (RubyHash)klass.allocate();
        for (int i=0; i < args.length; i+=2) hash.op_aset(args[i], args[i+1]);

        return hash;
    }

    /** rb_hash_new
     *
     */
    public static final RubyHash newHash(Ruby runtime) {
        return new RubyHash(runtime);
    }

    /** rb_hash_new
     *
     */
    public static final RubyHash newHash(Ruby runtime, Map valueMap, IRubyObject defaultValue) {
        assert defaultValue != null;

        return new RubyHash(runtime, valueMap, defaultValue);
    }

    private RubyHashEntry[] table;
    private int size = 0;
    private int threshold;

    private int iterLevel = 0;

    private static final int DELETED_HASH_F = 1 << 9;
    private static final int PROCDEFAULT_HASH_F = 1 << 10;

    private IRubyObject ifNone;

    private RubyHash(Ruby runtime, RubyClass klass, RubyHashEntry[]newTable, int newSize) {
        super(runtime, klass);
        this.ifNone = runtime.getNil();
        threshold = INITIAL_THRESHOLD;
        table = newTable;
        size = newSize;
    }

    public RubyHash(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
        this.ifNone = runtime.getNil();
        alloc();
    }

    public RubyHash(Ruby runtime) {
        this(runtime, runtime.getNil());
    }

    public RubyHash(Ruby runtime, IRubyObject defaultValue) {
        super(runtime, runtime.getHash());
        this.ifNone = defaultValue;
        alloc();
    }

    /*
     *  Constructor for internal usage (mainly for Array#|, Array#&, Array#- and Array#uniq)
     *  it doesn't initialize ifNone field
     */
    RubyHash(Ruby runtime, boolean objectSpace) {
        super(runtime, runtime.getHash(), objectSpace);
        alloc();
    }

    // TODO should this be deprecated ? (to be efficient, internals should deal with RubyHash directly)
    public RubyHash(Ruby runtime, Map valueMap, IRubyObject defaultValue) {
        super(runtime, runtime.getHash());
        this.ifNone = runtime.getNil();
        alloc();

        for (Iterator iter = valueMap.entrySet().iterator();iter.hasNext();) {
            Map.Entry e = (Map.Entry)iter.next();
            internalPut((IRubyObject)e.getKey(), (IRubyObject)e.getValue());
        }
    }

    private final void alloc() {
        threshold = INITIAL_THRESHOLD;
        table = new RubyHashEntry[MRI_HASH_RESIZE ? MRI_INITIAL_CAPACITY : JAVASOFT_INITIAL_CAPACITY];
    }

    /* ============================
     * Here are hash internals
     * (This could be extracted to a separate class but it's not too large though)
     * ============================
     */

    private static final int MRI_PRIMES[] = {
        8 + 3, 16 + 3, 32 + 5, 64 + 3, 128 + 3, 256 + 27, 512 + 9, 1024 + 9, 2048 + 5, 4096 + 3,
        8192 + 27, 16384 + 43, 32768 + 3, 65536 + 45, 131072 + 29, 262144 + 3, 524288 + 21, 1048576 + 7,
        2097152 + 17, 4194304 + 15, 8388608 + 9, 16777216 + 43, 33554432 + 35, 67108864 + 15,
        134217728 + 29, 268435456 + 3, 536870912 + 11, 1073741824 + 85, 0
    };

    private static final int JAVASOFT_INITIAL_CAPACITY = 8; // 16 ?
    private static final int MRI_INITIAL_CAPACITY = MRI_PRIMES[0];

    private static final int INITIAL_THRESHOLD = JAVASOFT_INITIAL_CAPACITY - (JAVASOFT_INITIAL_CAPACITY >> 2);
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    private static final RubyHashEntry NO_ENTRY = new RubyHashEntry(0, NEVER, null, null);

    static final class RubyHashEntry implements Map.Entry {
        private IRubyObject key;
        private IRubyObject value;
        private RubyHashEntry next;
        private int hash;

        RubyHashEntry(int h, IRubyObject k, IRubyObject v, RubyHashEntry e) {
            key = k; value = v; next = e; hash = h;
        }
        public Object getKey() {
            return key;
        }
        public Object getJavaifiedKey(){
            return JavaUtil.convertRubyToJava(key);
        }
        public Object getValue() {
            return value;
        }
        public Object getJavaifiedValue() {
            return JavaUtil.convertRubyToJava(value);
        }
        public Object setValue(Object value) {
            IRubyObject oldValue = this.value;
            if (value instanceof IRubyObject) {
                this.value = (IRubyObject)value;
        } else {
                throw new UnsupportedOperationException("directEntrySet() doesn't support setValue for non IRubyObject instance entries, convert them manually or use entrySet() instead");
                }
            return oldValue;
        }
        public boolean equals(Object other){
            if(!(other instanceof RubyHashEntry)) return false;
            RubyHashEntry otherEntry = (RubyHashEntry)other;
            if(key == otherEntry.key && key != NEVER && key.eql(otherEntry.key)){
                if(value == otherEntry.value || value.equals(otherEntry.value)) return true;
            }
            return false;
        }
        public int hashCode(){
            return key.hashCode() ^ value.hashCode();
        }
    }

    private static int JavaSoftHashValue(int h) {
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }

    private static int JavaSoftBucketIndex(final int h, final int length) {
        return h & (length - 1);
    }

    private static int MRIHashValue(int h) {
        return h & HASH_SIGN_BIT_MASK;
    }

    private static final int HASH_SIGN_BIT_MASK = ~(1 << 31);
    private static int MRIBucketIndex(final int h, final int length) {
        return (h % length);
    }

    private final void resize(int newCapacity) {
        final RubyHashEntry[] oldTable = table;
        final RubyHashEntry[] newTable = new RubyHashEntry[newCapacity];
        for (int j = 0; j < oldTable.length; j++) {
            RubyHashEntry entry = oldTable[j];
            oldTable[j] = null;
            while (entry != null) {
                RubyHashEntry next = entry.next;
                int i = bucketIndex(entry.hash, newCapacity);
                entry.next = newTable[i];
                newTable[i] = entry;
                entry = next;
            }
        }
        table = newTable;
    }

    private final void JavaSoftCheckResize() {
        if (size > threshold) {
            int oldCapacity = table.length;
            if (oldCapacity == MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return;
            }
            int newCapacity = table.length << 1;
            resize(newCapacity);
            threshold = newCapacity - (newCapacity >> 2);
        }
    }

    private static final int MIN_CAPA = 8;
    private static final int ST_DEFAULT_MAX_DENSITY = 5;
    private final void MRICheckResize() {
        if (size / table.length > ST_DEFAULT_MAX_DENSITY) {
            int forSize = table.length + 1; // size + 1;
            for (int i=0, newCapacity = MIN_CAPA; i < MRI_PRIMES.length; i++, newCapacity <<= 1) {
                if (newCapacity > forSize) {
                    resize(MRI_PRIMES[i]);
                    return;
                }
            }
            return; // suboptimal for large hashes (> 1073741824 + 85 entries) not very likely to happen
        }
    }
    // ------------------------------
    private static boolean MRI_HASH = true;
    private static boolean MRI_HASH_RESIZE = true;

    private static int hashValue(final int h) {
        return MRI_HASH ? MRIHashValue(h) : JavaSoftHashValue(h);
    }

    private static int bucketIndex(final int h, final int length) {
        return MRI_HASH ? MRIBucketIndex(h, length) : JavaSoftBucketIndex(h, length);
    }

    private void checkResize() {
        if (MRI_HASH_RESIZE) MRICheckResize(); else JavaSoftCheckResize();
    }
    // ------------------------------
    public static long collisions = 0;

    // put implementation

    private final void internalPut(final IRubyObject key, final IRubyObject value) {
        internalPut(key, value, true);
    }

    private final void internalPut(final IRubyObject key, final IRubyObject value, final boolean checkForExisting) {
        checkResize();
        final int hash = hashValue(key.hashCode());
        final int i = bucketIndex(hash, table.length);

        // if (table[i] != null) collisions++;

        if (checkForExisting) {
            for (RubyHashEntry entry = table[i]; entry != null; entry = entry.next) {
                IRubyObject k;
                if (entry.hash == hash && ((k = entry.key) == key || key.eql(k))) {
                    entry.value = value;
                    return;
                }
            }
        }

        table[i] = new RubyHashEntry(hash, key, value, table[i]);
        size++;
    }

    // get implementation

    private final IRubyObject internalGet(IRubyObject key) { // specialized for value
        return internalGetEntry(key).value;
    }

    private final RubyHashEntry internalGetEntry(IRubyObject key) {
        final int hash = hashValue(key.hashCode());
        for (RubyHashEntry entry = table[bucketIndex(hash, table.length)]; entry != null; entry = entry.next) {
            IRubyObject k;
            if (entry.hash == hash && ((k = entry.key) == key || key.eql(k))) return entry;
        }
        return NO_ENTRY;
    }

    // delete implementation

    private final RubyHashEntry internalDelete(final IRubyObject key) {
        return internalDelete(hashValue(key.hashCode()), MATCH_KEY, key);
    }

    private final RubyHashEntry internalDeleteEntry(final RubyHashEntry entry) {
        // n.b. we need to recompute the hash in case the key object was modified
        return internalDelete(hashValue(entry.key.hashCode()), MATCH_ENTRY, entry);
    }

    private final RubyHashEntry internalDelete(final int hash, final EntryMatchType matchType, final Object obj) {
        final int i = bucketIndex(hash, table.length);

        RubyHashEntry entry = table[i];
        if (entry != null) {
            RubyHashEntry prior = null;
            for (; entry != null; prior = entry, entry = entry.next) {
                if (entry.hash == hash && matchType.matches(entry, obj)) {
                    final RubyHashEntry next = entry.next;
                    if (iterLevel > 0) {
                        entry.key = NEVER;
                        flags |= DELETED_HASH_F;
                    } else {
                        if (prior != null) {
                            prior.next = entry.next;
                        } else {
                            table[i] = entry.next;
                        }
                    }
                    size--;
                    return entry;
                }
            }
        }

        return NO_ENTRY;
    }

    private static abstract class EntryMatchType {
        public abstract boolean matches(final RubyHashEntry entry, final Object obj);
    }

    private static final EntryMatchType MATCH_KEY = new EntryMatchType() {
        public boolean matches(final RubyHashEntry entry, final Object obj) {
            final IRubyObject key = entry.key;
            return obj == key || ((IRubyObject)obj).eql(key);
        }
    };

    private static final EntryMatchType MATCH_ENTRY = new EntryMatchType() {
        public boolean matches(final RubyHashEntry entry, final Object obj) {
            return entry.equals(obj);
        }
    };

    private final void internalCleanupSafe() { // synchronized ?
        for (int i=0; i < table.length; i++) {
            RubyHashEntry entry = table[i];
            while (entry != null && entry.key == NEVER) table[i] = entry = entry.next;
            if (entry != null) {
                RubyHashEntry prev = entry;
                entry = entry.next;
                while (entry != null) {
                    if (entry.key == NEVER) {
                        prev.next = entry.next;
                    } else {
                        prev = prev.next;
                    }
                    entry = prev.next;
                }
            }
        }
    }

    private final RubyHashEntry[] internalCopyTable() {
         RubyHashEntry[]newTable = new RubyHashEntry[table.length];

         for (int i=0; i < table.length; i++) {
             for (RubyHashEntry entry = table[i]; entry != null; entry = entry.next) {
                 if (entry.key != NEVER) newTable[i] = new RubyHashEntry(entry.hash, entry.key, entry.value, newTable[i]);
             }
         }
         return newTable;
    }

    // flags for callback based interation
    public static final int ST_CONTINUE = 0;
    public static final int ST_STOP = 1;
    public static final int ST_DELETE = 2;
    public static final int ST_CHECK = 3;

    private void rehashOccured() {
        throw getRuntime().newRuntimeError("rehash occurred during iteration");
    }

    private final void preIter() {
        iterLevel++;
    }

    private final void postIter() {
        iterLevel--;
        if ((flags & DELETED_HASH_F) != 0) {
            internalCleanupSafe();
            flags &= ~DELETED_HASH_F;
        }
    }

    private final RubyHashEntry checkIter(RubyHashEntry[]ltable, RubyHashEntry node) {
        while (node != null && node.key == NEVER) node = node.next;
        if (ltable != table) rehashOccured();
        return node;
    }

    /* ============================
     * End of hash internals
     * ============================
     */

    /*  ================
     *  Instance Methods
     *  ================
     */

    /** rb_hash_initialize
     *
     */
    @JRubyMethod(name = "initialize", optional = 1, frame = true)
    public IRubyObject initialize(IRubyObject[] args, final Block block) {
            modify();

        if (block.isGiven()) {
            if (args.length > 0) throw getRuntime().newArgumentError("wrong number of arguments");
            ifNone = getRuntime().newProc(Block.Type.PROC, block);
            flags |= PROCDEFAULT_HASH_F;
        } else {
            Arity.checkArgumentCount(getRuntime(), args, 0, 1);
            if (args.length == 1) ifNone = args[0];
        }
        return this;
    }

    /** rb_hash_default
     *
     */
    @JRubyMethod(name = "default", optional = 1, frame = true)
    public IRubyObject default_value_get(IRubyObject[] args) {
        Arity.checkArgumentCount(getRuntime(), args, 0, 1);

        if ((flags & PROCDEFAULT_HASH_F) != 0) {
            if (args.length == 0) return getRuntime().getNil();
            return ifNone.callMethod(getRuntime().getCurrentContext(), "call", new IRubyObject[]{this, args[0]});
        }
        return ifNone;
    }

    /** rb_hash_set_default
     *
     */
    @JRubyMethod(name = "default=", required = 1)
    public IRubyObject default_value_set(final IRubyObject defaultValue) {
        modify();

        ifNone = defaultValue;
        flags &= ~PROCDEFAULT_HASH_F;

        return ifNone;
    }

    /** rb_hash_default_proc
     *
     */
    @JRubyMethod(name = "default_proc", frame = true)
    public IRubyObject default_proc() {
        return (flags & PROCDEFAULT_HASH_F) != 0 ? ifNone : getRuntime().getNil();
    }

    /** rb_hash_modify
     *
     */
    public void modify() {
    	testFrozen("hash");
        if (isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw getRuntime().newSecurityError("Insecure: can't modify hash");
        }
    }

    /** inspect_hash
     *
     */
    public IRubyObject inspectHash() {
        try {
            Ruby runtime = getRuntime();
            final String sep = ", ";
            final String arrow = "=>";
            final StringBuffer sb = new StringBuffer("{");
            boolean firstEntry = true;

            ThreadContext context = runtime.getCurrentContext();
            preIter();
            RubyHashEntry[]ltable = table;
            for (int i = 0; i < ltable.length; i++) {
                for (RubyHashEntry entry = ltable[i]; (entry = checkIter(ltable, entry)) != null; entry = entry.next) {
                    if (!firstEntry) sb.append(sep);
                    sb.append(entry.key.callMethod(context, "inspect")).append(arrow);
                    sb.append(entry.value.callMethod(context, "inspect"));
                    firstEntry = false;
                }
            }
            sb.append("}");
            return runtime.newString(sb.toString());
        } finally {
            postIter();
        }
    }

    /** rb_hash_inspect
     *
     */
    @JRubyMethod(name = "inspect")
    public IRubyObject inspect() {
        if (size == 0) return getRuntime().newString("{}");
        if (getRuntime().isInspecting(this)) return getRuntime().newString("{...}");

        try {
            getRuntime().registerInspecting(this);
            return inspectHash();
        } finally {
            getRuntime().unregisterInspecting(this);
        }
    }

    /** rb_hash_size
     *
     */
    @JRubyMethod(name = {"size", "length"})
    public RubyFixnum rb_size() {
        return getRuntime().newFixnum(size);
    }

    /** rb_hash_empty_p
     *
     */
    @JRubyMethod(name = "empty?")
    public RubyBoolean empty_p() {
        return size == 0 ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    /** rb_hash_to_a
     *
     */
    @JRubyMethod(name = "to_a")
    public RubyArray to_a() {
        Ruby runtime = getRuntime();
        RubyArray result = RubyArray.newArray(runtime, size);

        try {
            preIter();
            RubyHashEntry[]ltable = table;
            for (int i = 0; i < ltable.length; i++) {
                for (RubyHashEntry entry = ltable[i]; (entry = checkIter(ltable, entry)) != null; entry = entry.next) {
                    result.append(RubyArray.newArray(runtime, entry.key, entry.value));
                }
            }
        } finally {postIter();}

        result.setTaint(isTaint());
        return result;
    }

    /** rb_hash_to_s & to_s_hash
     *
     */
    @JRubyMethod(name = "to_s")
    public IRubyObject to_s() {
        if (getRuntime().isInspecting(this)) return getRuntime().newString("{...}");
        try {
            getRuntime().registerInspecting(this);
            return to_a().to_s();
        } finally {
            getRuntime().unregisterInspecting(this);
        }
    }

    /** rb_hash_rehash
     *
     */
    @JRubyMethod(name = "rehash")
    public RubyHash rehash() {
        modify();
        final RubyHashEntry[] oldTable = table;
        final RubyHashEntry[] newTable = new RubyHashEntry[oldTable.length];
        for (int j = 0; j < oldTable.length; j++) {
            RubyHashEntry entry = oldTable[j];
            oldTable[j] = null;
            while (entry != null) {
                RubyHashEntry next = entry.next;
                if (entry.key != NEVER) {
                    entry.hash = entry.key.hashCode(); // update the hash value
                    int i = bucketIndex(entry.hash, newTable.length);
                    entry.next = newTable[i];
                    newTable[i] = entry;
                }
                entry = next;
            }
        }
        table = newTable;
        return this;
    }

    /** rb_hash_to_hash
     *
     */
    @JRubyMethod(name = "to_hash")
    public RubyHash to_hash() {
        return this;
    }

    public RubyHash convertToHash() {
        return this;
    }

    public final void fastASet(IRubyObject key, IRubyObject value) {
        internalPut(key, value);
    }

    /** rb_hash_aset
     *
     */
    @JRubyMethod(name = {"[]=", "store"}, required = 2)
    public IRubyObject op_aset(IRubyObject key, IRubyObject value) {
        modify();

        if (!(key instanceof RubyString)) {
            internalPut(key, value);
        } else {
            final RubyHashEntry entry = internalGetEntry(key);
            if (entry != NO_ENTRY) {
                entry.value = value;
            } else {
                IRubyObject realKey = ((RubyString)key).strDup();
                realKey.setFrozen(true);
                internalPut(realKey, value, false);
            }
        }

        return value;
    }

    /**
     * Note: this is included as a compatibility measure for AR-JDBC
     * @deprecated use RubyHash.op_aset instead
     */
    public IRubyObject aset(IRubyObject key, IRubyObject value) {
        return op_aset(key, value);
    }

    /**
     * Note: this is included as a compatibility measure for Mongrel+JRuby
     * @deprecated use RubyHash.op_aref instead
     */
    public IRubyObject aref(IRubyObject key) {
        return op_aref(key);
    }

    public final IRubyObject fastARef(IRubyObject key) { // retuns null when not found to avoid unnecessary getRuntime().getNil() call
        return internalGet(key);
    }

    /** rb_hash_aref
     *
     */
    @JRubyMethod(name = "[]", required = 1)
    public IRubyObject op_aref(IRubyObject key) {
        IRubyObject value;
        return ((value = internalGet(key)) == null) ? callMethod(getRuntime().getCurrentContext(), MethodIndex.DEFAULT, "default", key) : value;
    }

    /** rb_hash_fetch
     *
     */
    @JRubyMethod(name = "fetch", required = 1, optional = 1, frame = true)
    public IRubyObject fetch(IRubyObject[] args, Block block) {
        if (Arity.checkArgumentCount(getRuntime(), args, 1, 2) == 2 && block.isGiven()) {
            getRuntime().getWarnings().warn("block supersedes default value argument");
        }

        IRubyObject value;
        if ((value = internalGet(args[0])) == null) {
            if (block.isGiven()) return block.yield(getRuntime().getCurrentContext(), args[0]);
            if (args.length == 1) throw getRuntime().newIndexError("key not found");
            return args[1];
        }
        return value;
    }

    /** rb_hash_has_key
     *
     */
    @JRubyMethod(name = {"has_key?", "key?", "include?", "member?"}, required = 1)
    public RubyBoolean has_key_p(IRubyObject key) {
        return internalGetEntry(key) == NO_ENTRY ? getRuntime().getFalse() : getRuntime().getTrue();
    }

    /** rb_hash_has_value
     *
     */
    @JRubyMethod(name = {"has_value?", "value?"}, required = 1)
    public RubyBoolean has_value_p(IRubyObject value) {
        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();

        try {
            preIter();
            RubyHashEntry[]ltable = table;
            for (int i = 0; i < ltable.length; i++) {
                for (RubyHashEntry entry = ltable[i]; (entry = checkIter(ltable, entry)) != null; entry = entry.next) {
                    if (equalInternal(context, entry.value, value).isTrue()) return runtime.getTrue();
                }
            }
        } finally {postIter();}
        return runtime.getFalse();
    }

    /** rb_hash_each
     *
     */
    @JRubyMethod(name = "each", frame = true)
    public RubyHash each(Block block) {
        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();

        try {
            preIter();
            RubyHashEntry[]ltable = table;
            for (int i = 0; i < ltable.length; i++) {
                for (RubyHashEntry entry = ltable[i]; (entry = checkIter(ltable, entry)) != null; entry = entry.next) {
                    // rb_assoc_new equivalent
                    block.yield(context, RubyArray.newArray(runtime, entry.key, entry.value), null, null, false);
                }
            }
        } finally {postIter();}

        return this;
    }

    /** rb_hash_each_pair
     *
     */
    @JRubyMethod(name = "each_pair", frame = true)
    public RubyHash each_pair(Block block) {
        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();

        try {
            preIter();
            RubyHashEntry[]ltable = table;
            for (int i = 0; i < ltable.length; i++) {
                for (RubyHashEntry entry = ltable[i]; (entry = checkIter(ltable, entry)) != null; entry = entry.next) {
                    // rb_yield_values(2,...) equivalent
                    block.yield(context, RubyArray.newArray(runtime, entry.key, entry.value), null, null, true);
                }
            }
        } finally {postIter();}

        return this;	
    }

    /** rb_hash_each_value
     *
     */
    @JRubyMethod(name = "each_value", frame = true)
    public RubyHash each_value(Block block) {
        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();

        try {
            preIter();
            RubyHashEntry[]ltable = table;
            for (int i = 0; i < ltable.length; i++) {
                for (RubyHashEntry entry = ltable[i]; (entry = checkIter(ltable, entry)) != null; entry = entry.next) {
                    block.yield(context, entry.value);
                }
            }
        } finally {postIter();}

        return this;
    }

    /** rb_hash_each_key
     *
     */
    @JRubyMethod(name = "each_key", frame = true)
    public RubyHash each_key(Block block) {
        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();

        try {
            preIter();
            RubyHashEntry[]ltable = table;
            for (int i = 0; i < ltable.length; i++) {
                for (RubyHashEntry entry = ltable[i]; (entry = checkIter(ltable, entry)) != null; entry = entry.next) {
                    block.yield(context, entry.key);
                }
            }
        } finally {postIter();}

        return this;
    }

    /** rb_hash_sort
     *
     */
    @JRubyMethod(name = "sort", frame = true)
    public RubyArray sort(Block block) {
        return to_a().sort_bang(block);
    }

    /** rb_hash_index
     *
     */
    @JRubyMethod(name = "index", required = 1)
    public IRubyObject index(IRubyObject value) {
        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();

        try {
            preIter();
            RubyHashEntry[]ltable = table;
            for (int i = 0; i < ltable.length; i++) {
                for (RubyHashEntry entry = ltable[i]; (entry = checkIter(ltable, entry)) != null; entry = entry.next) {
                    if (equalInternal(context, entry.value, value).isTrue()) return entry.key;
                }
            }
        } finally {postIter();}

        return getRuntime().getNil();
    }

    /** rb_hash_indexes
     *
     */
    @JRubyMethod(name = {"indexes", "indices"}, rest = true)
    public RubyArray indices(IRubyObject[] indices) {
        RubyArray values = RubyArray.newArray(getRuntime(), indices.length);

        for (int i = 0; i < indices.length; i++) {
            values.append(op_aref(indices[i]));
        }

        return values;
    }

    /** rb_hash_keys
     *
     */
    @JRubyMethod(name = "keys")
    public RubyArray keys() {
        Ruby runtime = getRuntime();
        RubyArray keys = RubyArray.newArray(runtime, size);

        try {
            preIter();
            RubyHashEntry[]ltable = table;
            for (int i = 0; i < ltable.length; i++) {
                for (RubyHashEntry entry = ltable[i]; (entry = checkIter(ltable, entry)) != null; entry = entry.next) {
                    keys.append(entry.key);
                }
            }
        } finally {postIter();}

        return keys;
    }

    /** rb_hash_values
     *
     */
    @JRubyMethod(name = "values")
    public RubyArray rb_values() {
        RubyArray values = RubyArray.newArray(getRuntime(), size);

        try {
            preIter();
            RubyHashEntry[]ltable = table;
            for (int i = 0; i < ltable.length; i++) {
                for (RubyHashEntry entry = ltable[i]; (entry = checkIter(ltable, entry)) != null; entry = entry.next) {
                    values.append(entry.value);
                }
            }
        } finally {postIter();}

        return values;
    }

    /** rb_hash_equal
     *
     */

    private static final boolean EQUAL_CHECK_DEFAULT_VALUE = false;

    @JRubyMethod(name = "==", required = 1)
    public IRubyObject op_equal(IRubyObject other) {
        if (this == other ) return getRuntime().getTrue();
        if (!(other instanceof RubyHash)) {
            if (!other.respondsTo("to_hash")) return getRuntime().getFalse();
            return equalInternal(getRuntime().getCurrentContext(), other, this);
        }

        RubyHash otherHash = (RubyHash)other;
        if (size != otherHash.size) return getRuntime().getFalse();

        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();

        if (EQUAL_CHECK_DEFAULT_VALUE) {
            if (!equalInternal(context, ifNone, otherHash.ifNone).isTrue() &&
               (flags & PROCDEFAULT_HASH_F) != (otherHash.flags & PROCDEFAULT_HASH_F)) return runtime.getFalse();
        }

        try {
            preIter();
            RubyHashEntry[]ltable = table;
            for (int i = 0; i < ltable.length; i++) {
                for (RubyHashEntry entry = ltable[i]; (entry = checkIter(ltable, entry)) != null; entry = entry.next) {
                    IRubyObject value = otherHash.internalGet(entry.key);
                    if (value == null) return runtime.getFalse();
                    if (!equalInternal(context, entry.value, value).isTrue()) return runtime.getFalse();
                }
            }
        } finally {postIter();}

        return runtime.getTrue();
    }

    /** rb_hash_shift
     *
     */
    @JRubyMethod(name = "shift")
    public IRubyObject shift() {
        modify();

        try {
            preIter();
            RubyHashEntry[]ltable = table;
            for (int i = 0; i < ltable.length; i++) {
                for (RubyHashEntry entry = ltable[i]; (entry = checkIter(ltable, entry)) != null; entry = entry.next) {
                    RubyArray result = RubyArray.newArray(getRuntime(), entry.key, entry.value);
                    internalDelete(entry.key);
                    return result;
                }
            }
        } finally {postIter();}

        if ((flags & PROCDEFAULT_HASH_F) != 0) return ifNone.callMethod(getRuntime().getCurrentContext(), "call", new IRubyObject[]{this, getRuntime().getNil()});
        return ifNone;
    }

    public final boolean fastDelete(IRubyObject key) {
        return internalDelete(key) != NO_ENTRY;
    }

    /** rb_hash_delete
     *
     */
    @JRubyMethod(name = "delete", required = 1, frame = true)
    public IRubyObject delete(IRubyObject key, Block block) {
        modify();

        final RubyHashEntry entry = internalDelete(key);
        if (entry != NO_ENTRY) return entry.value;
 
        if (block.isGiven()) return block.yield(getRuntime().getCurrentContext(), key);
        return getRuntime().getNil();
    }

    /** rb_hash_select
     *
     */
    @JRubyMethod(name = "select", frame = true)
    public IRubyObject select(Block block) {
        RubyArray result = getRuntime().newArray();

        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();

        try {
            preIter();
            RubyHashEntry[]ltable = table;
            for (int i = 0; i < ltable.length; i++) {
                for (RubyHashEntry entry = ltable[i]; (entry = checkIter(ltable, entry)) != null; entry = entry.next) {
                    IRubyObject key = entry.key;
                    if (block.yield(context, runtime.newArray(key, entry.value), null, null, true).isTrue()) {
                        result.append(runtime.newArray(key, entry.value));
                    }
                }
            }
        } finally {postIter();}
        return result;
    }

    /** rb_hash_delete_if
     *
     */
    @JRubyMethod(name = "delete_if", frame = true)
    public RubyHash delete_if(Block block) {
        modify();

        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();

        try {
            preIter();
            RubyHashEntry[]ltable = table;
            for (int i = 0; i < ltable.length; i++) {
                for (RubyHashEntry entry = ltable[i]; (entry = checkIter(ltable, entry)) != null; entry = entry.next) {
                    IRubyObject key = entry.key;
                    if (block.yield(context, RubyArray.newArray(runtime, key, entry.value), null, null, true).isTrue())
                        delete(key, block);
                }
            }
        } finally {postIter();}

        return this;
    }

    /** rb_hash_reject
     *
     */
    @JRubyMethod(name = "reject", frame = true)
    public RubyHash reject(Block block) {
        return ((RubyHash)dup()).delete_if(block);
    }

    /** rb_hash_reject_bang
     *
     */
    @JRubyMethod(name = "reject!", frame = true)
    public IRubyObject reject_bang(Block block) {
        int n = size;
        delete_if(block);
        if (n == size) return getRuntime().getNil();
        return this;
    }

    /** rb_hash_clear
     *
     */
    @JRubyMethod(name = "clear")
    public RubyHash rb_clear() {
        modify();

        if (size > 0) {
            alloc();
            size = 0;
            flags &= ~DELETED_HASH_F;
        }

        return this;
    }

    /** rb_hash_invert
     *
     */
    @JRubyMethod(name = "invert")
    public RubyHash invert() {
        RubyHash result = newHash(getRuntime());

        try {
            preIter();
            RubyHashEntry[]ltable = table;
            for (int i = 0; i < ltable.length; i++) {
                for (RubyHashEntry entry = ltable[i]; (entry = checkIter(ltable, entry)) != null; entry = entry.next) {
                    result.op_aset(entry.value, entry.key);
                }
            }
        } finally {postIter();}

        return result;
    }

    /** rb_hash_update
     *
     */
    @JRubyMethod(name = {"merge!", "update"}, required = 1, frame = true)
    public RubyHash merge_bang(IRubyObject other, Block block) {
        modify();

        RubyHash otherHash = other.convertToHash();

        try {
             otherHash.preIter();
             RubyHashEntry[]ltable = otherHash.table;
             if (block.isGiven()) {
                 Ruby runtime = getRuntime();
                 ThreadContext context = runtime.getCurrentContext();

                 for (int i = 0; i < ltable.length; i++) {
                     for (RubyHashEntry entry = ltable[i]; (entry = otherHash.checkIter(ltable, entry)) != null; entry = entry.next) {
                         IRubyObject value;
                         IRubyObject key = entry.key;
                         if (internalGet(key) != null)
                             value = block.yield(context, RubyArray.newArrayNoCopy(runtime, new IRubyObject[]{key, op_aref(key), entry.value}));
                         else
                             value = entry.value;
                         op_aset(key, value);
                     }
                 }
            } else {
                for (int i = 0; i < ltable.length; i++) {
                    for (RubyHashEntry entry = ltable[i]; (entry = otherHash.checkIter(ltable, entry)) != null; entry = entry.next) {
                        op_aset(entry.key, entry.value);
                    }
                }
            }
        } finally {otherHash.postIter();}

        return this;
    }

    /** rb_hash_merge
     *
     */
    @JRubyMethod(name = "merge", required = 1, frame = true)
    public RubyHash merge(IRubyObject other, Block block) {
        return ((RubyHash)dup()).merge_bang(other, block);
    }

    /** rb_hash_replace
     *
     */
    @JRubyMethod(name = {"replace", "initialize_copy"}, required = 1)
    public RubyHash replace(IRubyObject other) {
        RubyHash otherHash = other.convertToHash();

        if (this == otherHash) return this;

        rb_clear();

        try {
            otherHash.preIter();
            RubyHashEntry[]ltable = otherHash.table;
            for (int i = 0; i < ltable.length; i++) {
                for (RubyHashEntry entry = ltable[i]; (entry = otherHash.checkIter(ltable, entry)) != null; entry = entry.next) {
                    op_aset(entry.key, entry.value);
                }
            }
        } finally {otherHash.postIter();}

        ifNone = otherHash.ifNone;

        if ((otherHash.flags & PROCDEFAULT_HASH_F) != 0) {
            flags |= PROCDEFAULT_HASH_F;
        } else {
            flags &= ~PROCDEFAULT_HASH_F;
        }

        return this;
    }

    /** rb_hash_values_at
     *
     */
    @JRubyMethod(name = "values_at", rest = true)
    public RubyArray values_at(IRubyObject[] args) {
        RubyArray result = RubyArray.newArray(getRuntime(), args.length);
        for (int i = 0; i < args.length; i++) {
            result.append(op_aref(args[i]));
        }
        return result;
    }

    public boolean hasDefaultProc() {
        return (flags & PROCDEFAULT_HASH_F) != 0;
    }

    public IRubyObject getIfNone(){
        return ifNone;
    }

    // FIXME:  Total hack to get flash in Rails marshalling/unmarshalling in session ok...We need
    // to totally change marshalling to work with overridden core classes.
    public static void marshalTo(RubyHash hash, MarshalStream output) throws IOException {
        output.writeInt(hash.size);
        try {
            hash.preIter();
            RubyHashEntry[]ltable = hash.table;
            for (int i = 0; i < ltable.length; i++) {
                for (RubyHashEntry entry = ltable[i]; (entry = hash.checkIter(ltable, entry)) != null; entry = entry.next) {
                    output.dumpObject(entry.key);
                    output.dumpObject(entry.value);
                }
            }
        } finally {hash.postIter();}

        if (!hash.ifNone.isNil()) output.dumpObject(hash.ifNone);
    }

    public static RubyHash unmarshalFrom(UnmarshalStream input, boolean defaultValue) throws IOException {
        RubyHash result = newHash(input.getRuntime());
        input.registerLinkTarget(result);
        int size = input.unmarshalInt();
        for (int i = 0; i < size; i++) {
            result.op_aset(input.unmarshalObject(), input.unmarshalObject());
        }
        if (defaultValue) result.default_value_set(input.unmarshalObject());
        return result;
    }

    public Class getJavaClass() {
        return Map.class;
    }

    // Satisfy java.util.Set interface (for Java integration)

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean containsKey(Object key) {
        return internalGet(JavaUtil.convertJavaToRuby(getRuntime(), key)) != null;
    }

    public boolean containsValue(Object value) {
        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();
        IRubyObject element = JavaUtil.convertJavaToRuby(runtime, value);

        try {
            preIter();
            RubyHashEntry[]ltable = table;
            for (int i = 0; i < ltable.length; i++) {
                for (RubyHashEntry entry = ltable[i]; (entry = checkIter(ltable, entry)) != null; entry = entry.next) {
                    if (equalInternal(context, entry.value, element).isTrue()) return true;
                }
            }
        } finally {postIter();}

        return false;
    }

    public Object get(Object key) {
        return JavaUtil.convertRubyToJava(internalGet(JavaUtil.convertJavaToRuby(getRuntime(), key)));
    }

    public Object put(Object key, Object value) {
        internalPut(JavaUtil.convertJavaToRuby(getRuntime(), key), JavaUtil.convertJavaToRuby(getRuntime(), value));
        return value;
    }

    public Object remove(Object key) {
        IRubyObject rubyKey = JavaUtil.convertJavaToRuby(getRuntime(), key);
        return internalDelete(rubyKey).value;
    }

    public void putAll(Map map) {
        Ruby runtime = getRuntime();
        for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
            Object key = iter.next();
            internalPut(JavaUtil.convertJavaToRuby(runtime, key), JavaUtil.convertJavaToRuby(runtime, map.get(key)));
        }
    }

    public void clear() {
        rb_clear();
    }

    private abstract class RubyHashIterator implements Iterator {
        RubyHashEntry entry, current;
        int index;
        RubyHashEntry[]iterTable;
        Ruby runtime = getRuntime();

        public RubyHashIterator(){
            iterTable = table;
            if(size > 0) seekNextValidEntry();
        }

        private final void seekNextValidEntry(){
            do {
                while (index < iterTable.length && (entry = iterTable[index++]) == null);
                while (entry != null && entry.key == NEVER) entry = entry.next;
            } while (entry == null && index < iterTable.length);
        }

        public boolean hasNext() {
            return entry != null;
        }

        public final RubyHashEntry nextEntry() {
            if (entry == null) throw new NoSuchElementException();
            RubyHashEntry e = current = entry;
            if ((entry = checkIter(iterTable, entry.next)) == null) seekNextValidEntry();
            return e;
        }

        public void remove() {
            if (current == null) throw new IllegalStateException();
            internalDelete(current.key);
        }
    }

    private final class KeyIterator extends RubyHashIterator {
        public Object next() {
            return JavaUtil.convertRubyToJava(nextEntry().key);
        }
    }

    private class KeySet extends AbstractSet {
        public Iterator iterator() {
            return new KeyIterator();
        }
        public int size() {
            return size;
        }
        public boolean contains(Object o) {
            return containsKey(o);
        }
        public boolean remove(Object o) {
            return RubyHash.this.remove(o) != null;
        }
        public void clear() {
            RubyHash.this.clear();
        }
    }

    public Set keySet() {
        return new KeySet();
    }

    private final class DirectKeyIterator extends RubyHashIterator {
        public Object next() {
            return nextEntry().key;
        }
    }	

    private final class DirectKeySet extends KeySet {
        public Iterator iterator() {
            return new DirectKeyIterator();
        }
    }

    public Set directKeySet() {
        return new DirectKeySet();
    }

    private final class ValueIterator extends RubyHashIterator {
        public Object next() {
            return JavaUtil.convertRubyToJava(nextEntry().value);
        }
    }		

    private class Values extends AbstractCollection {
        public Iterator iterator() {
            return new ValueIterator();
        }
        public int size() {
            return size;
        }
        public boolean contains(Object o) {
            return containsValue(o);
        }
        public void clear() {
            RubyHash.this.clear();
        }
    }

    public Collection values() {
        return new Values();
    }

    private final class DirectValueIterator extends RubyHashIterator {
        public Object next() {
            return nextEntry().value;
        }
    }

    private final class DirectValues extends Values {
        public Iterator iterator() {
            return new DirectValueIterator();
        }
    }

    public Collection directValues() {
        return new DirectValues();
    }

    static final class ConversionMapEntry implements Map.Entry {
        private final RubyHashEntry entry;
        private final Ruby runtime;

        public ConversionMapEntry(Ruby runtime, RubyHashEntry entry) {
            this.entry = entry;
            this.runtime = runtime;
        }

        public Object getKey() {
            return JavaUtil.convertRubyToJava(entry.key, Object.class);
        }

        public Object getValue() {
            return JavaUtil.convertRubyToJava(entry.value, Object.class);
        }

        public Object setValue(Object value) {
            return entry.value = JavaUtil.convertJavaToRuby(runtime, value);
        }

        public boolean equals(Object other){
            if(!(other instanceof RubyHashEntry)) return false;
            RubyHashEntry otherEntry = (RubyHashEntry)other;
            if(entry.key != NEVER && entry.key == otherEntry.key && entry.key.eql(otherEntry.key)){
                if(entry.value == otherEntry.value || entry.value.equals(otherEntry.value)) return true;
            }
            return false;
        }
        public int hashCode(){
            return entry.hashCode();
        }
    }

    private final class EntryIterator extends RubyHashIterator {
        public Object next() {
            return new ConversionMapEntry(runtime, nextEntry());
        }
    }

    private final class EntrySet extends AbstractSet {
        public Iterator iterator() {
            return new EntryIterator();
        }
        public boolean contains(Object o) {
            if (!(o instanceof ConversionMapEntry))
                return false;
            ConversionMapEntry entry = (ConversionMapEntry)o;
            if (entry.entry.key == NEVER) return false;
            RubyHashEntry candidate = internalGetEntry(entry.entry.key);
            return candidate != NO_ENTRY && candidate.equals(entry.entry);
        }
        public boolean remove(Object o) {
            if (!(o instanceof ConversionMapEntry)) return false;
            return internalDeleteEntry(((ConversionMapEntry)o).entry) != NO_ENTRY;
        }
        public int size() {
            return size;
        }
        public void clear() {
            RubyHash.this.clear();
        }
    }

    public Set entrySet() {
        return new EntrySet();
    }

    private final class DirectEntryIterator extends RubyHashIterator {
        public Object next() {
            return nextEntry();
        }
    }

    private final class DirectEntrySet extends AbstractSet {
        public Iterator iterator() {
            return new DirectEntryIterator();
        }
        public boolean contains(Object o) {
            if (!(o instanceof RubyHashEntry))
                return false;
            RubyHashEntry entry = (RubyHashEntry)o;
            if (entry.key == NEVER) return false;
            RubyHashEntry candidate = internalGetEntry(entry.key);
            return candidate != NO_ENTRY && candidate.equals(entry);
        }
        public boolean remove(Object o) {
            if (!(o instanceof RubyHashEntry)) return false;
            return internalDeleteEntry((RubyHashEntry)o) != NO_ENTRY;
        }
        public int size() {
            return size;
        }
        public void clear() {
            RubyHash.this.clear();
        }
    }

    /** return an entry set who's entries do not convert their values, faster
     *
     */
    public Set directEntrySet() {
        return new DirectEntrySet();
    }

    public boolean equals(Object other) {
        if (!(other instanceof RubyHash)) return false;
        if (this == other) return true;
        return op_equal((RubyHash)other).isTrue() ? true : false;
    }
}
