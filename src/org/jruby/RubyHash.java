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

import static org.jruby.RubyEnumerator.enumeratorize;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.ByteList;
import org.jruby.util.TypeConverter;
import org.jruby.util.RecursiveComparator;

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
@JRubyClass(name = "Hash", include="Enumerable")
public class RubyHash extends RubyObject implements Map {

    public static RubyClass createHashClass(Ruby runtime) {
        RubyClass hashc = runtime.defineClass("Hash", runtime.getObject(), HASH_ALLOCATOR);
        runtime.setHash(hashc);
        hashc.index = ClassIndex.HASH;
        hashc.kindOf = new RubyModule.KindOf() {
            @Override
            public boolean isKindOf(IRubyObject obj, RubyModule type) {
                return obj instanceof RubyHash;
            }
        };

        hashc.includeModule(runtime.getEnumerable());

        hashc.defineAnnotatedMethods(RubyHash.class);

        return hashc;
    }

    private final static ObjectAllocator HASH_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyHash(runtime, klass);
        }
    };

    @Override
    public int getNativeTypeIndex() {
        return ClassIndex.HASH;
    }

    /** rb_hash_s_create
     *
     */
    @JRubyMethod(name = "[]", rest = true, frame = true, meta = true)
    public static IRubyObject create(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        RubyClass klass = (RubyClass) recv;
        Ruby runtime = context.getRuntime();
        RubyHash hash;

        if (args.length == 1) {
            IRubyObject tmp = TypeConverter.convertToTypeWithCheck(
                    args[0], runtime.getHash(), "to_hash");

            if (!tmp.isNil()) {
                RubyHash otherHash = (RubyHash) tmp;
                return new RubyHash(runtime, klass, otherHash);
            }

            tmp = TypeConverter.convertToTypeWithCheck(args[0], runtime.getArray(), "to_ary");
            if (!tmp.isNil()) {
                hash = (RubyHash)klass.allocate();
                RubyArray arr = (RubyArray)tmp;
                for(int i = 0, j = arr.getLength(); i<j; i++) {
                    IRubyObject v = TypeConverter.convertToTypeWithCheck(arr.entry(i), runtime.getArray(), "to_ary");
                    IRubyObject key = runtime.getNil();
                    IRubyObject val = runtime.getNil();
                    if(v.isNil()) {
                        continue;
                    }
                    switch(((RubyArray)v).getLength()) {
                    case 2:
                        val = ((RubyArray)v).entry(1);
                    case 1:
                        key = ((RubyArray)v).entry(0);
                        hash.fastASet(key, val);
                    }
                }
                return hash;
            }
        }

        if ((args.length & 1) != 0) {
            throw runtime.newArgumentError("odd number of args for Hash");
        }

        hash = (RubyHash)klass.allocate();
        for (int i=0; i < args.length; i+=2) hash.op_aset(context, args[i], args[i+1]);

        return hash;
    }

    @JRubyMethod(name = "try_convert", meta = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject try_convert(ThreadContext context, IRubyObject recv, IRubyObject args) {
        return TypeConverter.convertToTypeWithCheck(args, context.getRuntime().getHash(), "to_hash");
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

    private static final int PROCDEFAULT_HASH_F = 1 << 10;

    private IRubyObject ifNone;

    private RubyHash(Ruby runtime, RubyClass klass, RubyHash other) {
        super(runtime, klass);
        this.ifNone = runtime.getNil();
        threshold = INITIAL_THRESHOLD;
        table = other.internalCopyTable(head);
        size = other.size;
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
        this.ifNone = defaultValue;
        alloc();

        for (Iterator iter = valueMap.entrySet().iterator();iter.hasNext();) {
            Map.Entry e = (Map.Entry)iter.next();
            internalPut((IRubyObject)e.getKey(), (IRubyObject)e.getValue());
        }
    }

    private final void alloc() {
        threshold = INITIAL_THRESHOLD;
        generation++;
        head.nextAdded = head.prevAdded = head;
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

    private static final RubyHashEntry NO_ENTRY = new RubyHashEntry();
    private int generation = 0; // generation count for O(1) clears
    private final RubyHashEntry head = new RubyHashEntry();

    { head.prevAdded = head.nextAdded = head; }

    static final class RubyHashEntry implements Map.Entry {
        private IRubyObject key;
        private IRubyObject value;
        private RubyHashEntry next;
        private RubyHashEntry prevAdded;
        private RubyHashEntry nextAdded;
        private int hash;

        RubyHashEntry() {
            key = NEVER;
        }

        RubyHashEntry(int h, IRubyObject k, IRubyObject v, RubyHashEntry e, RubyHashEntry head) {
            key = k; value = v; next = e; hash = h;
            prevAdded = head.prevAdded;
            nextAdded = head;
            nextAdded.prevAdded = this;
            prevAdded.nextAdded = this;
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

        public Object getKey() {
            return key;
        }
        public Object getJavaifiedKey(){
            return key.toJava(Object.class);
        }

        public Object getValue() {
            return value;
        }
        public Object getJavaifiedValue() {
            return value.toJava(Object.class);
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
        return ((h & HASH_SIGN_BIT_MASK) % length);
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
        if (overThreshold()) {
            RubyHashEntry[] tbl = table;
            if (tbl.length == MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return;
            }
            resizeAndAdjustThreshold(table);
        }
    }
    
    private boolean overThreshold() {
        return size > threshold;
    }
    
    private void resizeAndAdjustThreshold(RubyHashEntry[] oldTable) {
        int newCapacity = oldTable.length << 1;
        resize(newCapacity);
        threshold = newCapacity - (newCapacity >> 2);
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
    private static final boolean MRI_HASH = true;
    private static final boolean MRI_HASH_RESIZE = true;

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
                if (internalKeyExist(entry, hash, key)) {
                    entry.value = value;
                    return;
                }
            }
        }

        table[i] = new RubyHashEntry(hash, key, value, table[i], head);
        size++;
    }

    // get implementation

    private final IRubyObject internalGet(IRubyObject key) { // specialized for value
        return internalGetEntry(key).value;
    }

    private final RubyHashEntry internalGetEntry(IRubyObject key) {
        final int hash = hashValue(key.hashCode());
        for (RubyHashEntry entry = table[bucketIndex(hash, table.length)]; entry != null; entry = entry.next) {
            if (internalKeyExist(entry, hash, key)) {
                return entry;
            }
        }
        return NO_ENTRY;
    }

    private boolean internalKeyExist(RubyHashEntry entry, int hash, IRubyObject key) {
        return (entry.hash == hash
            && (entry.key == key || (!isComparedByIdentity() && key.eql(entry.key))));
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
                    if (prior != null) {
                        prior.next = entry.next;
                    } else {
                        table[i] = entry.next;
                    }
                    entry.detach();
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
            return obj == key || (((IRubyObject)obj).eql(key));
        }
    };

    private static final EntryMatchType MATCH_ENTRY = new EntryMatchType() {
        public boolean matches(final RubyHashEntry entry, final Object obj) {
            return entry.equals(obj);
        }
    };

    private final RubyHashEntry[] internalCopyTable(RubyHashEntry destHead) {
         RubyHashEntry[]newTable = new RubyHashEntry[table.length];

         for (RubyHashEntry entry = head.nextAdded; entry != head; entry = entry.nextAdded) {
             int i = bucketIndex(entry.hash, table.length);
             newTable[i] = new RubyHashEntry(entry.hash, entry.key, entry.value, newTable[i], destHead);
         }
         return newTable;
    }

    public static abstract class Visitor {
        public abstract void visit(IRubyObject key, IRubyObject value);
    }

    public void visitAll(Visitor visitor) {
        int startGeneration = generation;
        for (RubyHashEntry entry = head.nextAdded; entry != head; entry = entry.nextAdded) {
            if (startGeneration != generation) {
                startGeneration = generation;
                entry = head.nextAdded;
                if (entry == head) break;
            }
            if (entry.isLive()) visitor.visit(entry.key, entry.value);
        }
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
    @JRubyMethod(name = "initialize", optional = 1, frame = true, visibility = Visibility.PRIVATE)
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
    @Deprecated
    public IRubyObject default_value_get(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 0: return default_value_get(context);
            case 1: return default_value_get(context, args[0]);
            default: throw context.getRuntime().newArgumentError(args.length, 1);
        }
    }
    @JRubyMethod(name = "default", frame = true)
    public IRubyObject default_value_get(ThreadContext context) {
        if ((flags & PROCDEFAULT_HASH_F) != 0) {
            return getRuntime().getNil();
        }
        return ifNone;
    }
    @JRubyMethod(name = "default", frame = true)
    public IRubyObject default_value_get(ThreadContext context, IRubyObject arg) {
        if ((flags & PROCDEFAULT_HASH_F) != 0) {
            return RuntimeHelpers.invoke(context, ifNone, "call", this, arg);
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
    private IRubyObject inspectHash(final ThreadContext context) {
        final ByteList buffer = new ByteList();
        buffer.append('{');
        final boolean[] firstEntry = new boolean[1];

        firstEntry[0] = true;
        visitAll(new Visitor() {
            public void visit(IRubyObject key, IRubyObject value) {
                if (!firstEntry[0]) buffer.append(',').append(' ');

                buffer.append(inspect(context, key).getByteList());
                buffer.append('=').append('>');
                buffer.append(inspect(context, value).getByteList());
                firstEntry[0] = false;
            }
        });
        buffer.append('}');
        return getRuntime().newString(buffer);
    }

    /** rb_hash_inspect
     *
     */
    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        if (size == 0) return getRuntime().newString("{}");
        if (getRuntime().isInspecting(this)) return getRuntime().newString("{...}");

        try {
            getRuntime().registerInspecting(this);
            return inspectHash(context);
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
    @Override
    public RubyArray to_a() {
        final Ruby runtime = getRuntime();
        try {
            final RubyArray result = RubyArray.newArray(runtime, size);

            visitAll(new Visitor() {
                public void visit(IRubyObject key, IRubyObject value) {
                    result.append(RubyArray.newArray(runtime, key, value));
                }
            });

            result.setTaint(isTaint());
            return result;
        } catch (NegativeArraySizeException nase) {
            throw concurrentModification();
        }
    }

    /** rb_hash_to_s & to_s_hash
     *
     */
    @JRubyMethod(name = "to_s")
    public IRubyObject to_s(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (runtime.isInspecting(this)) return runtime.newString("{...}");
        try {
            runtime.registerInspecting(this);
            return to_a().to_s();
        } finally {
            runtime.unregisterInspecting(this);
        }
    }

    @JRubyMethod(name = "to_s", compat = CompatVersion.RUBY1_9)
    public IRubyObject to_s19(ThreadContext context) {
        return inspect(context);
    }

    /** rb_hash_rehash
     *
     */
    @JRubyMethod(name = "rehash")
    public RubyHash rehash() {
        if (iteratorCount.get() > 0) {
            throw getRuntime().newRuntimeError("rehash during iteration");
        }

        modify();
        final RubyHashEntry[] oldTable = table;
        final RubyHashEntry[] newTable = new RubyHashEntry[oldTable.length];
        for (int j = 0; j < oldTable.length; j++) {
            RubyHashEntry entry = oldTable[j];
            oldTable[j] = null;
            while (entry != null) {
                RubyHashEntry next = entry.next;
                entry.hash = entry.key.hashCode(); // update the hash value
                int i = bucketIndex(entry.hash, newTable.length);
                entry.next = newTable[i];
                newTable[i] = entry;
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

    @Override
    public RubyHash convertToHash() {
        return this;
    }

    public final void fastASet(IRubyObject key, IRubyObject value) {
        internalPut(key, value);
    }

    public final RubyHash fastASetChained(IRubyObject key, IRubyObject value) {
        internalPut(key, value);
        return this;
    }
    
    public final void fastASetCheckString(Ruby runtime, IRubyObject key, IRubyObject value) {
      if (key instanceof RubyString) {
          op_asetForString(runtime, (RubyString) key, value);
      } else {
          internalPut(key, value);
      }
    }

    public final void fastASetCheckString19(Ruby runtime, IRubyObject key, IRubyObject value) {
      if (key.getMetaClass().getRealClass() == runtime.getString()) {
          op_asetForString(runtime, (RubyString) key, value);
      } else {
          internalPut(key, value);
      }
    }

    @Deprecated
    public IRubyObject op_aset(IRubyObject key, IRubyObject value) {
        return op_aset(getRuntime().getCurrentContext(), key, value);
    }

    /** rb_hash_aset
     *
     */
    @JRubyMethod(name = {"[]=", "store"}, required = 2, compat = CompatVersion.RUBY1_8)
    public IRubyObject op_aset(ThreadContext context, IRubyObject key, IRubyObject value) {
        modify();
        
        fastASetCheckString(context.getRuntime(), key, value);
        return value;
    }

    @JRubyMethod(name = {"[]=", "store"}, required = 2, compat = CompatVersion.RUBY1_9)
    public IRubyObject op_aset19(ThreadContext context, IRubyObject key, IRubyObject value) {
        modify();

        fastASetCheckString19(context.getRuntime(), key, value);
        return value;
    }

    private void op_asetForString(Ruby runtime, RubyString key, IRubyObject value) {
        final RubyHashEntry entry = internalGetEntry(key);
        if (entry != NO_ENTRY) {
            entry.value = value;
        } else {
            if (!key.isFrozen()) {
                key = key.strDup(runtime, key.getMetaClass().getRealClass());
                key.setFrozen(true);
            }
            internalPut(key, value, false);
        }
    }

    /**
     * Note: this is included as a compatibility measure for AR-JDBC
     * @deprecated use RubyHash.op_aset instead
     */
    public IRubyObject aset(IRubyObject key, IRubyObject value) {
        return op_aset(getRuntime().getCurrentContext(), key, value);
    }

    /**
     * Note: this is included as a compatibility measure for Mongrel+JRuby
     * @deprecated use RubyHash.op_aref instead
     */
    public IRubyObject aref(IRubyObject key) {
        return op_aref(getRuntime().getCurrentContext(), key);
    }

    public final IRubyObject fastARef(IRubyObject key) { // retuns null when not found to avoid unnecessary getRuntime().getNil() call
        return internalGet(key);
    }

    public RubyBoolean compare(final ThreadContext context, final String method, IRubyObject other, final Set<RecursiveComparator.Pair> seen) {

        Ruby runtime = context.getRuntime();

        if (!(other instanceof RubyHash)) {
            return runtime.getFalse();
        }

        final RubyHash otherHash = (RubyHash) other;

        if (this.size != otherHash.size) {
            return runtime.getFalse();
        }

        try {
            visitAll(new Visitor() {
                public void visit(IRubyObject key, IRubyObject value) {
                    IRubyObject value2 = otherHash.fastARef(key);

                    if (value2 == null) {
                        // other hash does not contain key
                        throw new Mismatch();
                    }

                    if (!RecursiveComparator.compare(context, method, value, value2, seen).isTrue()) {
                        throw new Mismatch();
                    }
                }
            });
        } catch (Mismatch e) {
            return runtime.getFalse();
        }
        
        return runtime.getTrue();
    }

    /** rb_hash_equal
     * 
     */
    @JRubyMethod(name = "==")
    public IRubyObject op_equal19(final ThreadContext context, IRubyObject other) {
        return RecursiveComparator.compare(context, "==", this, other, null);
    }

    /** rb_hash_eql
     * 
     */
    @JRubyMethod(name = "eql?")
    public IRubyObject op_eql19(final ThreadContext context, IRubyObject other) {
        return RecursiveComparator.compare(context, "eql?", this, other, null);
    }

    /** rb_hash_aref
     *
     */
    @JRubyMethod(name = "[]", required = 1)
    public IRubyObject op_aref(ThreadContext context, IRubyObject key) {
        IRubyObject value;
        return ((value = internalGet(key)) == null) ? callMethod(context, "default", key) : value;
    }

    /** rb_hash_hash
     * 
     */
    @JRubyMethod(name = "hash")
    public RubyFixnum hash() {
        final Ruby runtime = getRuntime();
        final ThreadContext context = runtime.getCurrentContext();
        if (size == 0 || runtime.isInspecting(this)) return RubyFixnum.zero(runtime);
        final long hash[] = new long[]{size};
        
        try {
            runtime.registerInspecting(this);
            visitAll(new Visitor() {
                public void visit(IRubyObject key, IRubyObject value) {
                    hash[0] ^= key.callMethod(context, "hash").convertToInteger().getLongValue();
                    hash[0] ^= value.callMethod(context, "hash").convertToInteger().getLongValue();
                }
            });
        } finally {
            runtime.unregisterInspecting(this);
        }
        return RubyFixnum.newFixnum(runtime, hash[0]);
    }

    /** rb_hash_fetch
     *
     */
    @JRubyMethod(name = "fetch", required = 1, optional = 1, frame = true)
    public IRubyObject fetch(ThreadContext context, IRubyObject[] args, Block block) {
        if (args.length == 2 && block.isGiven()) {
            getRuntime().getWarnings().warn(ID.BLOCK_BEATS_DEFAULT_VALUE, "block supersedes default value argument");
        }

        IRubyObject value;
        if ((value = internalGet(args[0])) == null) {
            if (block.isGiven()) return block.yield(context, args[0]);
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

    private static class Found extends RuntimeException {
        @Override
        public synchronized Throwable fillInStackTrace() {
            return null;
        }
    }

    private static final Found FOUND = new Found();

    private static class FoundKey extends Found {
        public final IRubyObject key;
        FoundKey(IRubyObject key) {
            super();
            this.key = key;
        }
    }

    private static class FoundPair extends FoundKey {
        public final IRubyObject value;
        FoundPair(IRubyObject key, IRubyObject value) {
            super(key);
            this.value = value;
        }
    }

    private boolean hasValue(final ThreadContext context, final IRubyObject expected) {
        try {
            visitAll(new Visitor() {
                public void visit(IRubyObject key, IRubyObject value) {
                    if (equalInternal(context, value, expected)) {
                        throw FOUND;
                    }
                }
            });
            return false;
        } catch (Found found) {
            return true;
        }
    }

    /** rb_hash_has_value
     *
     */
    @JRubyMethod(name = {"has_value?", "value?"}, required = 1)
    public RubyBoolean has_value_p(ThreadContext context, IRubyObject expected) {
        return getRuntime().newBoolean(hasValue(context, expected));
    }

    private AtomicInteger iteratorCount = new AtomicInteger(0);

    private void iteratorEntry() {
        iteratorCount.incrementAndGet();
    }

    private void iteratorExit() {
        iteratorCount.decrementAndGet();
    }

    private void iteratorVisitAll(Visitor visitor) {
        try {
            iteratorEntry();
            visitAll(visitor);
        } finally {
            iteratorExit();
        }
    }

    /** rb_hash_each
     *
     */
    public RubyHash each(final ThreadContext context, final Block block) {
        if (block.arity() == Arity.TWO_ARGUMENTS) {
            iteratorVisitAll(new Visitor() {
                public void visit(IRubyObject key, IRubyObject value) {
                    block.yieldSpecific(context, key, value);
                }
            });
        } else {
            final Ruby runtime = context.getRuntime();
            
            iteratorVisitAll(new Visitor() {
                public void visit(IRubyObject key, IRubyObject value) {
                    block.yield(context, RubyArray.newArray(runtime, key, value));
                }
            });
        }

        return this;
    }

    @JRubyMethod(name = "each", frame = true)
    public IRubyObject each19(final ThreadContext context, final Block block) {
        return block.isGiven() ? each(context, block) : enumeratorize(context.getRuntime(), this, "each");
    }

    /** rb_hash_each_pair
     *
     */
    public RubyHash each_pair(final ThreadContext context, final Block block) {
        final Ruby runtime = getRuntime();

        iteratorVisitAll(new Visitor() {
            public void visit(IRubyObject key, IRubyObject value) {
                // rb_yield_values(2,...) equivalent
                block.yield(context, RubyArray.newArray(runtime, key, value), null, null, true);
            }
        });

        return this;	
    }

    @JRubyMethod(name = "each_pair", frame = true)
    public IRubyObject each_pair19(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_pair(context, block) : enumeratorize(context.getRuntime(), this, "each_pair");
    }

    /** rb_hash_each_value
     *
     */
    public RubyHash each_value(final ThreadContext context, final Block block) {
        iteratorVisitAll(new Visitor() {
            public void visit(IRubyObject key, IRubyObject value) {
                block.yield(context, value);
            }
        });

        return this;
    }

    @JRubyMethod(name = "each_value", frame = true)
    public IRubyObject each_value19(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_value(context, block) : enumeratorize(context.getRuntime(), this, "each_value");
    }

    /** rb_hash_each_key
     *
     */
    public RubyHash each_key(final ThreadContext context, final Block block) {
        iteratorVisitAll(new Visitor() {
            public void visit(IRubyObject key, IRubyObject value) {
                block.yield(context, key);
            }
        });

        return this;
    }

    @JRubyMethod(name = "each_key", frame = true)
    public IRubyObject each_key19(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_key(context, block) : enumeratorize(context.getRuntime(), this, "each_key");
    }

    /** rb_hash_sort
     *
     */
    @JRubyMethod(name = "sort", frame = true)
    public IRubyObject sort(ThreadContext context, Block block) {
        return to_a().sort_bang(context, block);
    }

    /** rb_hash_index
     *
     */
    @JRubyMethod(name = "index", compat = CompatVersion.RUBY1_8)
    public IRubyObject index(ThreadContext context, IRubyObject expected) {
        IRubyObject key = internalIndex(context, expected);
        return key != null ? key : context.getRuntime().getNil();
    }

    @JRubyMethod(name = "index", compat = CompatVersion.RUBY1_9)
    public IRubyObject index19(ThreadContext context, IRubyObject expected) {
        context.getRuntime().getWarnings().warn(ID.DEPRECATED_METHOD, "Hash#index is deprecated; use Hash#key");
        return key(context, expected);
    }

    @JRubyMethod(name = "key", compat = CompatVersion.RUBY1_9)
    public IRubyObject key(ThreadContext context, IRubyObject expected) {
        IRubyObject key = internalIndex(context, expected);
        return key != null ? key : context.getRuntime().getNil();
    }

    private IRubyObject internalIndex(final ThreadContext context, final IRubyObject expected) {
        try {
            visitAll(new Visitor() {
                public void visit(IRubyObject key, IRubyObject value) {
                    if (equalInternal(context, value, expected)) {
                        throw new FoundKey(key);
                    }
                }
            });
            return null;
        } catch (FoundKey found) {
            return found.key;
        }
    }

    /** rb_hash_indexes
     *
     */
    @JRubyMethod(name = {"indexes", "indices"}, rest = true)
    public RubyArray indices(ThreadContext context, IRubyObject[] indices) {
        return values_at(context, indices);
    }

    /** rb_hash_keys
     *
     */
    @JRubyMethod(name = "keys")
    public RubyArray keys() {
        final Ruby runtime = getRuntime();
        try {
            final RubyArray keys = RubyArray.newArray(runtime, size);

            visitAll(new Visitor() {
                public void visit(IRubyObject key, IRubyObject value) {
                    keys.append(key);
                }
            });

            return keys;
        } catch (NegativeArraySizeException nase) {
            throw concurrentModification();
        }
    }

    /** rb_hash_values
     *
     */
    @JRubyMethod(name = "values")
    public RubyArray rb_values() {
        try {
            final RubyArray values = RubyArray.newArray(getRuntime(), size);

            visitAll(new Visitor() {
                public void visit(IRubyObject key, IRubyObject value) {
                    values.append(value);
                }
            });

            return values;
        } catch (NegativeArraySizeException nase) {
            throw concurrentModification();
        }
    }

    /** rb_hash_equal
     *
     */

    private static class Mismatch extends RuntimeException {}

    /** rb_hash_shift
     *
     */
    @JRubyMethod(name = "shift")
    public IRubyObject shift(ThreadContext context) {
        modify();

        RubyHashEntry entry = head.nextAdded;
        if (entry != head) {
            RubyArray result = RubyArray.newArray(getRuntime(), entry.key, entry.value);
            internalDeleteEntry(entry);
            return result;
        }

        if ((flags & PROCDEFAULT_HASH_F) != 0) {
            return RuntimeHelpers.invoke(context, ifNone, "call", this, getRuntime().getNil());
        } else {
            return ifNone;
        }
    }

    public final boolean fastDelete(IRubyObject key) {
        return internalDelete(key) != NO_ENTRY;
    }

    /** rb_hash_delete
     *
     */
    @JRubyMethod(name = "delete", required = 1, frame = true)
    public IRubyObject delete(ThreadContext context, IRubyObject key, Block block) {
        modify();

        final RubyHashEntry entry = internalDelete(key);
        if (entry != NO_ENTRY) return entry.value;

        if (block.isGiven()) return block.yield(context, key);
        return getRuntime().getNil();
    }

    /** rb_hash_select
     *
     */
    @JRubyMethod(name = "select", frame = true)
    public IRubyObject select(final ThreadContext context, final Block block) {
        final Ruby runtime = getRuntime();
        if (!block.isGiven()) return enumeratorize(runtime, this, "select");

        final RubyArray result = runtime.newArray();

        iteratorVisitAll(new Visitor() {
            public void visit(IRubyObject key, IRubyObject value) {
                if (block.yield(context, runtime.newArray(key, value), null, null, true).isTrue()) {
                    result.append(runtime.newArray(key, value));
                }
            }
        });

        return result;
    }

    @JRubyMethod(name = "select", frame = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject select19(final ThreadContext context, final Block block) {
        final Ruby runtime = context.getRuntime();
        if (!block.isGiven()) return enumeratorize(runtime, this, "select");

        final RubyHash result = newHash(runtime);

        iteratorVisitAll(new Visitor() {
            public void visit(IRubyObject key, IRubyObject value) {
                if (block.yield(context, runtime.newArray(key, value), null, null, true).isTrue()) {
                    result.fastASet(key, value);
                }
            }
        });

        return result;        
    }

    /** rb_hash_delete_if
     *
     */
    public RubyHash delete_if(final ThreadContext context, final Block block) {
        modify();

        final Ruby runtime = getRuntime();
        final RubyHash self = this;
        iteratorVisitAll(new Visitor() {
            public void visit(IRubyObject key, IRubyObject value) {
                if (block.yield(context, RubyArray.newArray(runtime, key, value), null, null, true).isTrue()) {
                    self.delete(context, key, Block.NULL_BLOCK);
                }
            }
        });

        return this;
    }

    @JRubyMethod(name = "delete_if", frame = true)
    public IRubyObject delete_if19(final ThreadContext context, final Block block) {
        return block.isGiven() ? delete_if(context, block) : enumeratorize(context.getRuntime(), this, "delete_if");
    }

    /** rb_hash_reject
     *
     */
    public RubyHash reject(ThreadContext context, Block block) {
        return ((RubyHash)dup()).delete_if(context, block);
    }

    @JRubyMethod(name = "reject", frame = true)
    public IRubyObject reject19(final ThreadContext context, final Block block) {
        return block.isGiven() ? reject(context, block) : enumeratorize(context.getRuntime(), this, "reject");
    }

    /** rb_hash_reject_bang
     *
     */
    public IRubyObject reject_bang(ThreadContext context, Block block) {
        int n = size;
        delete_if(context, block);
        if (n == size) return getRuntime().getNil();
        return this;
    }

    @JRubyMethod(name = "reject!", frame = true)
    public IRubyObject reject_bang19(final ThreadContext context, final Block block) {
        return block.isGiven() ? reject_bang(context, block) : enumeratorize(context.getRuntime(), this, "reject!");
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
        }

        return this;
    }

    /** rb_hash_invert
     *
     */
    @JRubyMethod(name = "invert")
    public RubyHash invert(final ThreadContext context) {
        final RubyHash result = newHash(getRuntime());

        visitAll(new Visitor() {
            public void visit(IRubyObject key, IRubyObject value) {
                result.op_aset(context, value, key);
            }
        });

        return result;
    }

    /** rb_hash_update
     *
     */
    @JRubyMethod(name = {"merge!", "update"}, required = 1, frame = true)
    public RubyHash merge_bang(final ThreadContext context, final IRubyObject other, final Block block) {
        final RubyHash otherHash = other.convertToHash();
        if (otherHash.empty_p().isTrue()) {
            return this;
        }

        modify();

        final Ruby runtime = getRuntime();
        final RubyHash self = this;
        otherHash.visitAll(new Visitor() {
            public void visit(IRubyObject key, IRubyObject value) {
                if (block.isGiven()) {
                    IRubyObject existing = self.internalGet(key);
                    if (existing != null)
                        value = block.yield(context, RubyArray.newArrayNoCopy(runtime, new IRubyObject[]{key, existing, value}));
                }
                self.op_aset(context, key, value);
            }
        });

        return this;
    }

    /** rb_hash_merge
     *
     */
    @JRubyMethod(name = "merge", required = 1, frame = true)
    public RubyHash merge(ThreadContext context, IRubyObject other, Block block) {
        return ((RubyHash)dup()).merge_bang(context, other, block);
    }

    /** rb_hash_replace
     *
     */
    @JRubyMethod(name = "initialize_copy", required = 1, visibility = Visibility.PRIVATE)
    public RubyHash initialize_copy(ThreadContext context, IRubyObject other) {
        return replace(context, other);
    }

    /** rb_hash_replace
     *
     */
    @JRubyMethod(name = "replace", required = 1, compat = CompatVersion.RUBY1_8)
    public RubyHash replace(final ThreadContext context, IRubyObject other) {
        final RubyHash self = this;
        return replaceCommon(context, other, new Visitor() {
            public void visit(IRubyObject key, IRubyObject value) {
                self.op_aset(context, key, value);
            }
        });
    }

    @JRubyMethod(name = "replace", required = 1, compat = CompatVersion.RUBY1_9)
    public RubyHash replace19(final ThreadContext context, IRubyObject other) {
        final RubyHash self = this;
        return replaceCommon(context, other, new Visitor() {
            public void visit(IRubyObject key, IRubyObject value) {
                self.op_aset19(context, key, value);
            }
        });
    }

    private RubyHash replaceCommon(final ThreadContext context, IRubyObject other, Visitor visitor) {
        final RubyHash otherHash = other.convertToHash();

        if (this == otherHash) return this;

        rb_clear();

        if (!isComparedByIdentity() && otherHash.isComparedByIdentity()) {
            setComparedByIdentity(true);
        }

        otherHash.visitAll(visitor);

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
    public RubyArray values_at(ThreadContext context, IRubyObject[] args) {
        RubyArray result = RubyArray.newArray(getRuntime(), args.length);
        for (int i = 0; i < args.length; i++) {
            result.append(op_aref(context, args[i]));
        }
        return result;
    }

    @JRubyMethod(name = "assoc", compat = CompatVersion.RUBY1_9)
    public IRubyObject assoc(final ThreadContext context, final IRubyObject obj) {
        try {
            visitAll(new Visitor() {
                public void visit(IRubyObject key, IRubyObject value) {
                    if (equalInternal(context, obj, key)) {
                        throw new FoundPair(key, value);
                    }
                }
            });
            return context.getRuntime().getNil();
        } catch (FoundPair found) {
            return context.getRuntime().newArray(found.key, found.value);
        }
    }

    @JRubyMethod(name = "rassoc", compat = CompatVersion.RUBY1_9)
    public IRubyObject rassoc(final ThreadContext context, final IRubyObject obj) {
        try {
            visitAll(new Visitor() {
                public void visit(IRubyObject key, IRubyObject value) {
                    if (equalInternal(context, obj, value)) {
                        throw new FoundPair(key, value);
                    }
                }
            });
            return context.getRuntime().getNil();
        } catch (FoundPair found) {
            return context.getRuntime().newArray(found.key, found.value);
        }
    }

    @JRubyMethod(name = "flatten", compat = CompatVersion.RUBY1_9)
    public IRubyObject flatten(ThreadContext context) {
        RubyArray ary = to_a(); 
        ary.callMethod(context, "flatten!", RubyFixnum.one(context.getRuntime()));
        return ary;
    }

    @JRubyMethod(name = "flatten", compat = CompatVersion.RUBY1_9)
    public IRubyObject flatten(ThreadContext context, IRubyObject level) {
        RubyArray ary = to_a();
        ary.callMethod(context, "flatten!", level);
        return ary;
    }

    @JRubyMethod(name = "compare_by_identity", frame = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject getCompareByIdentity(ThreadContext context) {
        modify();
        setComparedByIdentity(true);
        return this;
    }

    @JRubyMethod(name = "compare_by_identity?", frame = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject getCompareByIdentity_p(ThreadContext context) {
        return context.getRuntime().newBoolean(isComparedByIdentity());
    }

    @JRubyMethod(name = "dup", frame = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject dup(ThreadContext context) {
        RubyHash dup = (RubyHash) super.dup();
        dup.setComparedByIdentity(isComparedByIdentity());
        return dup;
    }

    @JRubyMethod(name = "clone", frame = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject rbClone(ThreadContext context) {
        RubyHash clone = (RubyHash) super.rbClone();
        clone.setComparedByIdentity(isComparedByIdentity());
        return clone;
    }

    public boolean hasDefaultProc() {
        return (flags & PROCDEFAULT_HASH_F) != 0;
    }

    public IRubyObject getIfNone(){
        return ifNone;
    }

    private static class VisitorIOException extends RuntimeException {
        VisitorIOException(Throwable cause) {
            super(cause);
        }
    }

    // FIXME:  Total hack to get flash in Rails marshalling/unmarshalling in session ok...We need
    // to totally change marshalling to work with overridden core classes.
    public static void marshalTo(final RubyHash hash, final MarshalStream output) throws IOException {
        output.registerLinkTarget(hash);
        output.writeInt(hash.size);
        try {
            hash.visitAll(new Visitor() {
                public void visit(IRubyObject key, IRubyObject value) {
                    try {
                        output.dumpObject(key);
                        output.dumpObject(value);
                    } catch (IOException e) {
                        throw new VisitorIOException(e);
                    }
                }
            });
        } catch (VisitorIOException e) {
            throw (IOException)e.getCause();
        }

        if (!hash.ifNone.isNil()) output.dumpObject(hash.ifNone);
    }

    public static RubyHash unmarshalFrom(UnmarshalStream input, boolean defaultValue) throws IOException {
        RubyHash result = newHash(input.getRuntime());
        input.registerLinkTarget(result);
        int size = input.unmarshalInt();
        ThreadContext context = input.getRuntime().getCurrentContext();
        for (int i = 0; i < size; i++) {
            result.op_aset(context, input.unmarshalObject(), input.unmarshalObject());
        }
        if (defaultValue) result.default_value_set(input.unmarshalObject());
        return result;
    }

    @Override
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
        return internalGet(JavaUtil.convertJavaToUsableRubyObject(getRuntime(), key)) != null;
    }

    public boolean containsValue(Object value) {
        return hasValue(getRuntime().getCurrentContext(), JavaUtil.convertJavaToUsableRubyObject(getRuntime(), value));
    }

    public Object get(Object key) {
        IRubyObject gotten = internalGet(JavaUtil.convertJavaToUsableRubyObject(getRuntime(), key));
        return gotten == null ? null : gotten.toJava(Object.class);
    }

    public Object put(Object key, Object value) {
        internalPut(JavaUtil.convertJavaToUsableRubyObject(getRuntime(), key), JavaUtil.convertJavaToUsableRubyObject(getRuntime(), value));
        return value;
    }

    public Object remove(Object key) {
        IRubyObject rubyKey = JavaUtil.convertJavaToUsableRubyObject(getRuntime(), key);
        return internalDelete(rubyKey).value;
    }

    public void putAll(Map map) {
        Ruby runtime = getRuntime();
        for (Iterator<Map.Entry> iter = map.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = iter.next();
            internalPut(JavaUtil.convertJavaToUsableRubyObject(runtime, entry.getKey()), JavaUtil.convertJavaToUsableRubyObject(runtime, entry.getValue()));
        }
    }

    public void clear() {
        rb_clear();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof RubyHash)) return false;
        if (this == other) return true;
        return op_equal(getRuntime().getCurrentContext(), (RubyHash)other).isTrue() ? true : false;
    }

    public Set keySet() {
        return new BaseSet(KEY_VIEW);
    }

    public Set directKeySet() {
        return new BaseSet(DIRECT_KEY_VIEW);
    }

    public Collection values() {
        return new BaseCollection(VALUE_VIEW);
    }

    public Collection directValues() {
        return new BaseCollection(DIRECT_VALUE_VIEW);
    }

    public Set entrySet() {
        return new BaseSet(ENTRY_VIEW);
    }

    public Set directEntrySet() {
        return new BaseSet(DIRECT_ENTRY_VIEW);
    }

    private final RaiseException concurrentModification() {
        return getRuntime().newConcurrencyError(
                "Detected invalid hash contents due to unsynchronized modifications with concurrent users");
    }

    /**
     * Is this object compared by identity or not? Shortcut for doing
     * getFlag(COMPARE_BY_IDENTITY_F).
     *
     * @return true if this object is compared by identity, false otherwise
     */
    protected boolean isComparedByIdentity() {
        return (flags & COMPARE_BY_IDENTITY_F) != 0;
    }

    /**
     * Sets whether this object is compared by identity or not. Shortcut for doing
     * setFlag(COMPARE_BY_IDENTITY_F, frozen).
     *
     * @param comparedByIdentity should this object be compared by identity?
     */
    public void setComparedByIdentity(boolean comparedByIdentity) {
        if (comparedByIdentity) {
            flags |= COMPARE_BY_IDENTITY_F;
        } else {
            flags &= ~COMPARE_BY_IDENTITY_F;
        }
    }

    private class BaseSet extends AbstractSet {
        final EntryView view;

        public BaseSet(EntryView view) {
            this.view = view;
        }

        public Iterator iterator() {
            return new BaseIterator(view);
        }

        @Override
        public boolean contains(Object o) {
            return view.contains(RubyHash.this, o);
        }

        @Override
        public void clear() {
            RubyHash.this.clear();
        }

        public int size() {
            return RubyHash.this.size;
        }

        @Override
        public boolean remove(Object o) {
            return view.remove(RubyHash.this, o);
        }
    }

    private class BaseCollection extends AbstractCollection {
        final EntryView view;

        public BaseCollection(EntryView view) {
            this.view = view;
        }

        public Iterator iterator() {
            return new BaseIterator(view);
        }

        @Override
        public boolean contains(Object o) {
            return view.contains(RubyHash.this, o);
        }

        @Override
        public void clear() {
            RubyHash.this.clear();
        }

        public int size() {
            return RubyHash.this.size;
        }

        @Override
        public boolean remove(Object o) {
            return view.remove(RubyHash.this, o);
        }
    }

    private class BaseIterator implements Iterator {
        final private EntryView view;
        private RubyHashEntry entry;
        private boolean peeking;
        private int startGeneration;

        public BaseIterator(EntryView view) {
            this.view = view;
            this.entry = head;
            this.startGeneration = generation;
        }

        private void advance(boolean consume) {
            if (!peeking) {
                do {
                    if (startGeneration != generation) {
                        startGeneration = generation;
                        entry = head;
                    }
                    entry = entry.nextAdded;
                } while (entry != head && !entry.isLive());
            }
            peeking = !consume;
        }

        public Object next() {
            advance(true);
            if (entry == head) {
                peeking = true; // remain where we are
                throw new NoSuchElementException();
            }
            return view.convertEntry(getRuntime(), entry);
        }

        // once hasNext has been called, we commit to next() returning
        // the entry it found, even if it were subsequently deleted
        public boolean hasNext() {
            advance(false);
            return entry != head;
        }

        public void remove() {
            if (entry == head) {
                throw new IllegalStateException("Iterator out of range");
            }
            internalDeleteEntry(entry);
        }
    }

    private static abstract class EntryView {
        public abstract Object convertEntry(Ruby runtime, RubyHashEntry value);
        public abstract boolean contains(RubyHash hash, Object o);
        public abstract boolean remove(RubyHash hash, Object o);
    }

    private static final EntryView DIRECT_KEY_VIEW = new EntryView() {
        public Object convertEntry(Ruby runtime, RubyHashEntry entry) {
            return entry.key;
        }
        public boolean contains(RubyHash hash, Object o) {
            if (!(o instanceof IRubyObject)) return false;
            return hash.internalGet((IRubyObject)o) != null;
        }
        public boolean remove(RubyHash hash, Object o) {
            if (!(o instanceof IRubyObject)) return false;
            return hash.internalDelete((IRubyObject)o) != NO_ENTRY;
        }
    };

    private static final EntryView KEY_VIEW = new EntryView() {
        public Object convertEntry(Ruby runtime, RubyHashEntry entry) {
            return entry.key.toJava(Object.class);
        }
        public boolean contains(RubyHash hash, Object o) {
            return hash.containsKey(o);
        }
        public boolean remove(RubyHash hash, Object o) {
            return hash.remove(o) != null;
        }
    };

    private static final EntryView DIRECT_VALUE_VIEW = new EntryView() {
        public Object convertEntry(Ruby runtime, RubyHashEntry entry) {
            return entry.value;
        }
        public boolean contains(RubyHash hash, Object o) {
            if (!(o instanceof IRubyObject)) return false;
            IRubyObject obj = (IRubyObject)o;
            return hash.hasValue(obj.getRuntime().getCurrentContext(), obj);
        }
        public boolean remove(RubyHash hash, Object o) {
            if (!(o instanceof IRubyObject)) return false;
            IRubyObject obj = (IRubyObject) o;
            IRubyObject key = hash.internalIndex(obj.getRuntime().getCurrentContext(), obj);
            if (key == null) return false;
            return hash.internalDelete(key) != NO_ENTRY;
        }
    };

    private final EntryView VALUE_VIEW = new EntryView() {
        public Object convertEntry(Ruby runtime, RubyHashEntry entry) {
            return entry.value.toJava(Object.class);
        }
        public boolean contains(RubyHash hash, Object o) {
            return hash.containsValue(o);
        }
        public boolean remove(RubyHash hash, Object o) {
            IRubyObject value = JavaUtil.convertJavaToUsableRubyObject(hash.getRuntime(), o);
            IRubyObject key = hash.internalIndex(hash.getRuntime().getCurrentContext(), value);
            if (key == null) return false;
            return hash.internalDelete(key) != NO_ENTRY;
        }
    };

    private final EntryView DIRECT_ENTRY_VIEW = new EntryView() {
        public Object convertEntry(Ruby runtime, RubyHashEntry entry) {
            return entry;
        }
        public boolean contains(RubyHash hash, Object o) {
            if (!(o instanceof RubyHashEntry)) return false;
            RubyHashEntry entry = (RubyHashEntry)o;
            RubyHashEntry candidate = internalGetEntry(entry.key);
            return candidate != NO_ENTRY && entry.equals(candidate);
        }
        public boolean remove(RubyHash hash, Object o) {
            if (!(o instanceof RubyHashEntry)) return false;
            return hash.internalDeleteEntry((RubyHashEntry)o) != NO_ENTRY;
        }
    };

    private final EntryView ENTRY_VIEW = new EntryView() {
        public Object convertEntry(Ruby runtime, RubyHashEntry entry) {
            return new ConvertingEntry(runtime, entry);
        }
        public boolean contains(RubyHash hash, Object o) {
            if (!(o instanceof ConvertingEntry)) return false;
            ConvertingEntry entry = (ConvertingEntry)o;
            RubyHashEntry candidate = hash.internalGetEntry(entry.entry.key);
            return candidate != NO_ENTRY && entry.entry.equals(candidate);
        }
        public boolean remove(RubyHash hash, Object o) {
            if (!(o instanceof ConvertingEntry)) return false;
            ConvertingEntry entry = (ConvertingEntry)o;
            return hash.internalDeleteEntry(entry.entry) != NO_ENTRY;
        }
    };

    private static class ConvertingEntry implements Map.Entry {
        private final RubyHashEntry entry;
        private final Ruby runtime;

        public ConvertingEntry(Ruby runtime, RubyHashEntry entry) {
            this.entry = entry;
            this.runtime = runtime;
        }

        public Object getKey() {
            return entry.key.toJava(Object.class);
        }
        public Object getValue() {
            return entry.value.toJava(Object.class);
        }
        public Object setValue(Object o) {
            return entry.setValue(JavaUtil.convertJavaToUsableRubyObject(runtime, o));
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ConvertingEntry)) {
                return false;
            }
            ConvertingEntry other = (ConvertingEntry)o;
            return entry.equals(other.entry);
        }
        
        @Override
        public int hashCode() {
            return entry.hashCode();
        }
    }
}
