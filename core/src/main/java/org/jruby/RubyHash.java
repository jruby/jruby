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

import static org.jruby.RubyEnumerator.enumeratorize;

import org.jcodings.specific.USASCIIEncoding;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.invokedynamic.MethodNames;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.RecursiveComparator;
import org.jruby.util.TypeConverter;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.runtime.Helpers.invokedynamic;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.runtime.invokedynamic.MethodNames.DEFAULT;
import static org.jruby.runtime.invokedynamic.MethodNames.HASH;
import static org.jruby.RubyEnumerator.SizeFn;

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
    public static final int DEFAULT_INSPECT_STR_SIZE = 20;

    public static RubyClass createHashClass(Ruby runtime) {
        RubyClass hashc = runtime.defineClass("Hash", runtime.getObject(), HASH_ALLOCATOR);
        runtime.setHash(hashc);

        hashc.setClassIndex(ClassIndex.HASH);
        hashc.setReifiedClass(RubyHash.class);
        
        hashc.kindOf = new RubyModule.JavaClassKindOf(RubyHash.class);

        hashc.includeModule(runtime.getEnumerable());

        hashc.defineAnnotatedMethods(RubyHash.class);

        return hashc;
    }

    private final static ObjectAllocator HASH_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyHash(runtime, klass);
        }
    };

    @Override
    public ClassIndex getNativeClassIndex() {
        return ClassIndex.HASH;
    }

    /** rb_hash_s_create
     *
     */
    @JRubyMethod(name = "[]", rest = true, meta = true)
    public static IRubyObject create(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        RubyClass klass = (RubyClass) recv;
        Ruby runtime = context.runtime;
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
                    IRubyObject key;
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
            throw runtime.newArgumentError("odd number of arguments for Hash");
        }

        hash = (RubyHash)klass.allocate();
        for (int i=0; i < args.length; i+=2) hash.op_aset(context, args[i], args[i+1]);

        return hash;
    }

    @JRubyMethod(name = "try_convert", meta = true)
    public static IRubyObject try_convert(ThreadContext context, IRubyObject recv, IRubyObject args) {
        return TypeConverter.convertToTypeWithCheck(args, context.runtime.getHash(), "to_hash");
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
    public static final RubyHash newSmallHash(Ruby runtime) {
        return new RubyHash(runtime, 1);
    }

    /** rb_hash_new
     *
     */
    public static final RubyHash newHash(Ruby runtime, Map valueMap, IRubyObject defaultValue) {
        assert defaultValue != null;

        return new RubyHash(runtime, valueMap, defaultValue);
    }

    private RubyHashEntry[] table;
    protected int size = 0;
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
        allocFirst();
    }

    public RubyHash(Ruby runtime, int buckets) {
        this(runtime, runtime.getNil(), buckets);
    }

    public RubyHash(Ruby runtime) {
        this(runtime, runtime.getNil());
    }

    public RubyHash(Ruby runtime, IRubyObject defaultValue) {
        super(runtime, runtime.getHash());
        this.ifNone = defaultValue;
        allocFirst();
    }

    public RubyHash(Ruby runtime, IRubyObject defaultValue, int buckets) {
        super(runtime, runtime.getHash());
        this.ifNone = defaultValue;
        allocFirst(buckets);
    }

    /*
     *  Constructor for internal usage (mainly for Array#|, Array#&, Array#- and Array#uniq)
     *  it doesn't initialize ifNone field
     */
    RubyHash(Ruby runtime, boolean objectSpace) {
        super(runtime, runtime.getHash(), objectSpace);
        allocFirst();
    }

    // TODO should this be deprecated ? (to be efficient, internals should deal with RubyHash directly)
    public RubyHash(Ruby runtime, Map valueMap, IRubyObject defaultValue) {
        super(runtime, runtime.getHash());
        this.ifNone = defaultValue;
        allocFirst();

        for (Iterator iter = valueMap.entrySet().iterator();iter.hasNext();) {
            Map.Entry e = (Map.Entry)iter.next();
            internalPut((IRubyObject)e.getKey(), (IRubyObject)e.getValue());
        }
    }

    private final void allocFirst() {
        threshold = INITIAL_THRESHOLD;
        table = new RubyHashEntry[MRI_HASH_RESIZE ? MRI_INITIAL_CAPACITY : JAVASOFT_INITIAL_CAPACITY];
    }

    private final void allocFirst(int buckets) {
        threshold = INITIAL_THRESHOLD;
        table = new RubyHashEntry[buckets];
    }

    private final void alloc() {
        generation++;
        head.prevAdded = head.nextAdded = head;
        allocFirst();
    }

    private final void alloc(int buckets) {
        generation++;
        head.prevAdded = head.nextAdded = head;
        allocFirst(buckets);
    }

    /* ============================
     * Here are hash internals
     * (This could be extracted to a separate class but it's not too large though)
     * ============================
     */

    public static final int MRI_PRIMES[] = {
        8 + 3, 16 + 3, 32 + 5, 64 + 3, 128 + 3, 256 + 27, 512 + 9, 1024 + 9, 2048 + 5, 4096 + 3,
        8192 + 27, 16384 + 43, 32768 + 3, 65536 + 45, 131072 + 29, 262144 + 3, 524288 + 21, 1048576 + 7,
        2097152 + 17, 4194304 + 15, 8388608 + 9, 16777216 + 43, 33554432 + 35, 67108864 + 15,
        134217728 + 29, 268435456 + 3, 536870912 + 11, 1073741824 + 85, 0
    };

    private static final int JAVASOFT_INITIAL_CAPACITY = 8; // 16 ?
    private static final int MRI_INITIAL_CAPACITY = MRI_PRIMES[0];

    private static final int INITIAL_THRESHOLD = JAVASOFT_INITIAL_CAPACITY - (JAVASOFT_INITIAL_CAPACITY >> 2);
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    public static final RubyHashEntry NO_ENTRY = new RubyHashEntry();
    private int generation = 0; // generation count for O(1) clears
    private final RubyHashEntry head = new RubyHashEntry();

    { head.prevAdded = head.nextAdded = head; }

    public static final class RubyHashEntry implements Map.Entry {
        private IRubyObject key;
        private IRubyObject value;
        private RubyHashEntry next;
        private RubyHashEntry prevAdded;
        private RubyHashEntry nextAdded;
        private int hash;

        RubyHashEntry() {
            key = NEVER;
        }

        public RubyHashEntry(int h, IRubyObject k, IRubyObject v, RubyHashEntry e, RubyHashEntry head) {
            key = k; value = v; next = e; hash = h;
            if (head != null) {
                prevAdded = head.prevAdded;
                nextAdded = head;
                nextAdded.prevAdded = this;
                prevAdded.nextAdded = this;
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

    private final synchronized void resize(int newCapacity) {
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

    protected static int hashValue(final int h) {
        return MRI_HASH ? MRIHashValue(h) : JavaSoftHashValue(h);
    }

    private static int bucketIndex(final int h, final int length) {
        return MRI_HASH ? MRIBucketIndex(h, length) : JavaSoftBucketIndex(h, length);
    }

    private void checkResize() {
        if (MRI_HASH_RESIZE) MRICheckResize(); else JavaSoftCheckResize();
    }

    private void checkIterating() {
        if (iteratorCount > 0) {
            throw getRuntime().newRuntimeError("can't add a new key into hash during iteration");
        }
    }
    // ------------------------------
    public static long collisions = 0;

    // put implementation

    private final void internalPut(final IRubyObject key, final IRubyObject value) {
        internalPut(key, value, true);
    }

    private final void internalPutSmall(final IRubyObject key, final IRubyObject value) {
        internalPutSmall(key, value, true);
    }

    protected void internalPut(final IRubyObject key, final IRubyObject value, final boolean checkForExisting) {
        checkResize();

        internalPutSmall(key, value, checkForExisting);
    }

    protected void internalPutSmall(final IRubyObject key, final IRubyObject value, final boolean checkForExisting) {
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

        checkIterating();

        table[i] = new RubyHashEntry(hash, key, value, table[i], head);
        size++;
    }

    // get implementation

    protected IRubyObject internalGet(IRubyObject key) { // specialized for value
        return internalGetEntry(key).value;
    }

    protected RubyHashEntry internalGetEntry(IRubyObject key) {
        if (size == 0) return NO_ENTRY;

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


    protected RubyHashEntry internalDelete(final IRubyObject key) {
        if (size == 0) return NO_ENTRY;

        return internalDelete(hashValue(key.hashCode()), MATCH_KEY, key);
    }

    protected RubyHashEntry internalDeleteEntry(final RubyHashEntry entry) {
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
        @Override
        public boolean matches(final RubyHashEntry entry, final Object obj) {
            final IRubyObject key = entry.key;
            return obj == key || (((IRubyObject)obj).eql(key));
        }
    };

    private static final EntryMatchType MATCH_ENTRY = new EntryMatchType() {
        @Override
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
        // use -1 to disable concurrency checks
        visitLimited(visitor, -1);
    }

    private void visitLimited(Visitor visitor, long size) {
        int startGeneration = generation;
        long count = size;
        // visit not more than size entries
        for (RubyHashEntry entry = head.nextAdded; entry != head && count != 0; entry = entry.nextAdded) {
            if (startGeneration != generation) {
                startGeneration = generation;
                entry = head.nextAdded;
                if (entry == head) break;
            }
            if (entry != null && entry.isLive()) {
                visitor.visit(entry.key, entry.value);
                count--;
            }
        }
        // it does not handle all concurrent modification cases,
        // but at least provides correct marshal as we have exactly size entries visited (count == 0)
        // or if count < 0 - skipped concurrent modification checks
        if (count > 0) throw concurrentModification();
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
    @JRubyMethod(optional = 1, visibility = PRIVATE)
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
            default:
                throw context.runtime.newArgumentError(args.length, 1);
        }
    }
    @JRubyMethod(name = "default")
    public IRubyObject default_value_get(ThreadContext context) {
        if ((flags & PROCDEFAULT_HASH_F) != 0) {
            return getRuntime().getNil();
        }
        return ifNone;
    }
    @JRubyMethod(name = "default")
    public IRubyObject default_value_get(ThreadContext context, IRubyObject arg) {
        if ((flags & PROCDEFAULT_HASH_F) != 0) {
            return Helpers.invoke(context, ifNone, "call", this, arg);
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
    @JRubyMethod
    public IRubyObject default_proc() {
        return (flags & PROCDEFAULT_HASH_F) != 0 ? ifNone : getRuntime().getNil();
    }

    /** default_proc_arity_check
     *
     */
    private void checkDefaultProcArity(IRubyObject proc) {
        int n = ((RubyProc)proc).getBlock().arity().getValue();

        if(((RubyProc)proc).getBlock().type == Block.Type.LAMBDA && n != 2 && (n >= 0 || n < -3)) {
            if(n < 0) n = -n-1;
            throw getRuntime().newTypeError("default_proc takes two arguments (2 for " + n + ")");
        }
    }

    /** rb_hash_set_default_proc
     *
     */
    public IRubyObject set_default_proc(IRubyObject proc) {
        return set_default_proc20(proc);
    }
    
    @JRubyMethod(name = "default_proc=")
    public IRubyObject set_default_proc20(IRubyObject proc) {
        modify();
        
        if (proc.isNil()) {
            ifNone = proc;
            return proc;
        }

        IRubyObject b = TypeConverter.convertToType(proc, getRuntime().getProc(), "to_proc");
        if(b.isNil() || !(b instanceof RubyProc)) {
            throw getRuntime().newTypeError("wrong default_proc type " + proc.getMetaClass() + " (expected Proc)");
        }
        proc = b;
        checkDefaultProcArity(proc);
        ifNone = proc;
        flags |= PROCDEFAULT_HASH_F;
        return proc;
    }

    /** rb_hash_modify
     *
     */
    public void modify() {
    	testFrozen("Hash");
    }

    /** inspect_hash
     *
     */
    private IRubyObject inspectHash(final ThreadContext context) {
        final RubyString str = RubyString.newStringLight(context.runtime, DEFAULT_INSPECT_STR_SIZE);
        str.cat((byte)'{');
        final boolean[] firstEntry = new boolean[1];

        firstEntry[0] = true;
        visitAll(new Visitor() {
            @Override
            public void visit(IRubyObject key, IRubyObject value) {
                if (!firstEntry[0]) str.cat((byte)',').cat((byte)' ');

                str.cat(inspect(context, key)).cat((byte)'=').cat((byte)'>').cat(inspect(context, value));
                
                firstEntry[0] = false;
            }
        });
        str.cat((byte)'}');
        return str;
    }
    
    private IRubyObject inspectHash19(final ThreadContext context) {
        final RubyString str = RubyString.newStringLight(context.runtime, DEFAULT_INSPECT_STR_SIZE, USASCIIEncoding.INSTANCE);
        str.cat((byte)'{');
        final boolean[] firstEntry = new boolean[1];

        firstEntry[0] = true;
        visitAll(new Visitor() {
            @Override
            public void visit(IRubyObject key, IRubyObject value) {
                if (!firstEntry[0]) str.cat((byte)',').cat((byte)' ');

                str.cat19(inspect(context, key)).cat((byte)'=').cat((byte)'>').cat19(inspect(context, value));
                
                firstEntry[0] = false;
            }
        });
        str.cat((byte)'}');
        return str;
    }    

    /** rb_hash_inspect
     *
     */
    public IRubyObject inspect(ThreadContext context) {
        return inspect19(context);
    }

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect19(ThreadContext context) {
        if (size == 0) return RubyString.newUSASCIIString(context.runtime, "{}");
        if (getRuntime().isInspecting(this)) return RubyString.newUSASCIIString(context.runtime, "{...}");

        try {
            getRuntime().registerInspecting(this);
            return inspectHash19(context);
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

    private SizeFn enumSizeFn() {
        final RubyHash self = this;
        return new RubyEnumerator.SizeFn() {
            @Override
            public IRubyObject size(IRubyObject[] args) {
                return self.rb_size();
            }
        };
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
                @Override
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
    public IRubyObject to_s(ThreadContext context) {
        return to_s19(context);
    }

    @JRubyMethod(name = "to_s")
    public IRubyObject to_s19(ThreadContext context) {
        return inspect19(context);
    }

    /** rb_hash_rehash
     *
     */
    @JRubyMethod(name = "rehash")
    public RubyHash rehash() {
        if (iteratorCount > 0) {
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
                entry.hash = hashValue(entry.key.hashCode()); // update the hash value
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
    
    @JRubyMethod
    public RubyHash to_h(ThreadContext context) {
        return getType() == getRuntime().getHash() ? this : newHash(getRuntime()).replace(context, this);
    }

    @Override
    public RubyHash convertToHash() {
        return this;
    }

    public final void fastASet(IRubyObject key, IRubyObject value) {
        internalPut(key, value);
    }

    public final void fastASetSmall(IRubyObject key, IRubyObject value) {
        internalPutSmall(key, value);
    }
    
    public final void fastASetCheckString(Ruby runtime, IRubyObject key, IRubyObject value) {
      if (key instanceof RubyString && !isComparedByIdentity()) {
          op_asetForString(runtime, (RubyString) key, value);
      } else {
          internalPut(key, value);
      }
    }

    public final void fastASetSmallCheckString(Ruby runtime, IRubyObject key, IRubyObject value) {
        if (key instanceof RubyString) {
            op_asetSmallForString(runtime, (RubyString) key, value);
        } else {
            internalPutSmall(key, value);
        }
    }

    public final void fastASet(Ruby runtime, IRubyObject key, IRubyObject value, boolean prepareString) {
        if (prepareString) {
            fastASetCheckString(runtime, key, value);
        } else {
            fastASet(key, value);
        }
    }

    public final void fastASetSmall(Ruby runtime, IRubyObject key, IRubyObject value, boolean prepareString) {
        if (prepareString) {
            fastASetSmallCheckString(runtime, key, value);
        } else {
            fastASetSmall(key, value);
        }
    }

    /** rb_hash_aset
     *
     */
    @JRubyMethod(name = {"[]=", "store"})
    public IRubyObject op_aset(ThreadContext context, IRubyObject key, IRubyObject value) {
        modify();

        fastASetCheckString(context.runtime, key, value);
        return value;
    }


    protected void op_asetForString(Ruby runtime, RubyString key, IRubyObject value) {
        final RubyHashEntry entry = internalGetEntry(key);
        if (entry != NO_ENTRY) {
            entry.value = value;
        } else {
            checkIterating();
            if (!key.isFrozen()) {
                if (isComparedByIdentity()) {
                    // when comparing by identity, we don't want to be too eager about deduping
                    key = key.strDup(runtime, key.getMetaClass().getRealClass());
                    key.setFrozen(true);
                } else {
                    key = runtime.freezeAndDedupString(key);
                }
            }
            internalPut(key, value, false);
        }
    }

    protected void op_asetSmallForString(Ruby runtime, RubyString key, IRubyObject value) {
        final RubyHashEntry entry = internalGetEntry(key);
        if (entry != NO_ENTRY) {
            entry.value = value;
        } else {
            checkIterating();
            if (isComparedByIdentity()) {
                // when comparing by identity, we don't want to be too eager about deduping
                key = key.strDup(runtime, key.getMetaClass().getRealClass());
                key.setFrozen(true);
            } else {
                key = runtime.freezeAndDedupString(key);
            }
            internalPutSmall(key, value, false);
        }
    }

    public final IRubyObject fastARef(IRubyObject key) { // retuns null when not found to avoid unnecessary getRuntime().getNil() call
        return internalGet(key);
    }

    public RubyBoolean compare(final ThreadContext context, final MethodNames method, IRubyObject other) {

        Ruby runtime = context.runtime;

        if (!(other instanceof RubyHash)) {
            if (!other.respondsTo("to_hash")) {
                return runtime.getFalse();
            } else {
                return Helpers.rbEqual(context, other, this);
            }
        }

        final RubyHash otherHash = (RubyHash) other;

        if (this.size != otherHash.size) {
            return runtime.getFalse();
        }

        try {
            visitAll(new Visitor() {
                @Override
                public void visit(IRubyObject key, IRubyObject value) {
                    IRubyObject value2 = otherHash.fastARef(key);

                    if (value2 == null) {
                        // other hash does not contain key
                        throw MISMATCH;
                    }

                    if (!(method == MethodNames.OP_EQUAL ?
                            Helpers.rbEqual(context, value, value2) :
                            Helpers.rbEql(context, value, value2)).isTrue()) {
                        throw MISMATCH;
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
    @Override
    public IRubyObject op_equal(final ThreadContext context, IRubyObject other) {
        return RecursiveComparator.compare(context, MethodNames.OP_EQUAL, this, other);
    }

    /** rb_hash_eql
     * 
     */
    @JRubyMethod(name = "eql?")
    public IRubyObject op_eql19(final ThreadContext context, IRubyObject other) {
        return RecursiveComparator.compare(context, MethodNames.EQL, this, other);
    }

    /** rb_hash_aref
     *
     */
    @JRubyMethod(name = "[]", required = 1)
    public IRubyObject op_aref(ThreadContext context, IRubyObject key) {
        IRubyObject value;
        return ((value = internalGet(key)) == null) ? invokedynamic(context, this, DEFAULT, key) : value;
    }

    /** rb_hash_hash
     * 
     */
    // FIXME: 
    @Override
    public RubyFixnum hash() {
        final Ruby runtime = getRuntime();
        final ThreadContext context = runtime.getCurrentContext();
        if (size == 0 || runtime.isInspecting(this)) return RubyFixnum.zero(runtime);
        final long hash[] = new long[]{size};
        try {
            runtime.registerInspecting(this);
            visitAll(new Visitor() {
                public void visit(IRubyObject key, IRubyObject value) {
                    hash[0] ^= invokedynamic(context, key, HASH).convertToInteger().getLongValue();
                    hash[0] ^= invokedynamic(context, value, HASH).convertToInteger().getLongValue();
                }
            });
        } finally {
            runtime.unregisterInspecting(this);
        }
        return RubyFixnum.newFixnum(runtime, hash[0]);
    }

    /** rb_hash_hash
     * 
     */
    @JRubyMethod(name = "hash")
    public RubyFixnum hash19() {
        final Ruby runtime = getRuntime();
        final ThreadContext context = runtime.getCurrentContext();
        return (RubyFixnum)getRuntime().execRecursiveOuter(new Ruby.RecursiveFunction() {
                @Override
                public IRubyObject call(IRubyObject obj, boolean recur) {
                    if(size == 0) {
                        return RubyFixnum.zero(runtime);
                    }
                    final long[] h = new long[]{1};
                    if(recur) {
                        h[0] ^= RubyNumeric.num2long(invokedynamic(context, runtime.getHash(), HASH));
                    } else {
                        visitAll(new Visitor() {
                                @Override
                                public void visit(IRubyObject key, IRubyObject value) {
                                    h[0] += invokedynamic(context, key, HASH).convertToInteger().getLongValue() ^ invokedynamic(context, value, HASH).convertToInteger().getLongValue();
                                }
                            });
                    }
                    return runtime.newFixnum(h[0]);
                }
            }, this);
    }

    /** rb_hash_fetch
     *
     */
    public IRubyObject fetch(ThreadContext context, IRubyObject[] args, Block block) {
        Arity.checkArgumentCount(context.runtime, args.length, 1, 2);
        
        switch(args.length) {
            case 1: return fetch(context, args[0], block);
            case 2: return fetch(context, args[0], args[1], block);
        }

        return null;
    }
    
    @JRubyMethod
    public IRubyObject fetch(ThreadContext context, IRubyObject key, Block block) {
        Ruby runtime = context.runtime;

        IRubyObject value = internalGet(key);
        
        if (value == null) {
            if (block.isGiven()) return block.yield(context, key);
            
            throw runtime.newKeyError("key not found: " + key);
        }
        
        return value;
    }
    
    @JRubyMethod
    public IRubyObject fetch(ThreadContext context, IRubyObject key, IRubyObject _default, Block block) {
        Ruby runtime = context.runtime;
        boolean blockGiven = block.isGiven();

        if (blockGiven) {
            runtime.getWarnings().warn(ID.BLOCK_BEATS_DEFAULT_VALUE, "block supersedes default value argument");
        }

        IRubyObject value = internalGet(key);
        
        if (value == null) {
            if (blockGiven) return block.yield(context, key);
            
            return _default;
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
        public Throwable fillInStackTrace() {
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
                @Override
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

    private volatile int iteratorCount;
    
    private static final AtomicIntegerFieldUpdater<RubyHash> ITERATOR_UPDATER;
    static {
        AtomicIntegerFieldUpdater<RubyHash> iterUp = null;
        try {
            iterUp = AtomicIntegerFieldUpdater.newUpdater(RubyHash.class, "iteratorCount");
        } catch (Exception e) {
            // ignore, leave null
        }
        ITERATOR_UPDATER = iterUp;
    }

    private void iteratorEntry() {
        if (ITERATOR_UPDATER == null) {
            iteratorEntrySync();
            return;
        }
        ITERATOR_UPDATER.incrementAndGet(this);
    }

    private void iteratorExit() {
        if (ITERATOR_UPDATER == null) {
            iteratorExitSync();
            return;
        }
        ITERATOR_UPDATER.decrementAndGet(this);
    }

    private synchronized void iteratorEntrySync() {
        ++iteratorCount;
    }

    private void iteratorExitSync() {
        --iteratorCount;
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
    public RubyHash eachCommon(final ThreadContext context, final Block block) {
        if (block.arity() == Arity.TWO_ARGUMENTS) {
            iteratorVisitAll(new Visitor() {
                @Override
                public void visit(IRubyObject key, IRubyObject value) {
                    block.yieldSpecific(context, key, value);
                }
            });
        } else {
            final Ruby runtime = context.runtime;
            
            iteratorVisitAll(new Visitor() {
                @Override
                public void visit(IRubyObject key, IRubyObject value) {
                    block.yield(context, RubyArray.newArray(runtime, key, value));
                }
            });
        }

        return this;
    }

    public IRubyObject each(final ThreadContext context, final Block block) {
        return each19(context, block);
    }

    @JRubyMethod(name = {"each", "each_pair"})
    public IRubyObject each19(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_pairCommon(context, block, true) : enumeratorizeWithSize(context, this, "each", enumSizeFn());
    }

    /** rb_hash_each_pair
     *
     */
    public RubyHash each_pairCommon(final ThreadContext context, final Block block, final boolean oneNine) {
        final Ruby runtime = getRuntime();

        iteratorVisitAll(new Visitor() {
            @Override
            public void visit(IRubyObject key, IRubyObject value) {
                // rb_yield_values(2,...) equivalent
                if (oneNine) {
                    block.yield(context, RubyArray.newArray(runtime, key, value));
                } else {
                    block.yieldArray(context, RubyArray.newArray(runtime, key, value), null);
                }
            }
        });

        return this;	
    }

    /** rb_hash_each_value
     *
     */
    public RubyHash each_valueCommon(final ThreadContext context, final Block block) {
        iteratorVisitAll(new Visitor() {
            @Override
            public void visit(IRubyObject key, IRubyObject value) {
                block.yield(context, value);
            }
        });

        return this;
    }

    @JRubyMethod
    public IRubyObject each_value(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_valueCommon(context, block) : enumeratorizeWithSize(context, this, "each_value", enumSizeFn());
    }

    /** rb_hash_each_key
     *
     */
    public RubyHash each_keyCommon(final ThreadContext context, final Block block) {
        iteratorVisitAll(new Visitor() {
            @Override
            public void visit(IRubyObject key, IRubyObject value) {
                block.yield(context, key);
            }
        });

        return this;
    }

    @JRubyMethod
    public IRubyObject each_key(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_keyCommon(context, block) : enumeratorizeWithSize(context, this, "each_key", enumSizeFn());
    }

    @JRubyMethod(name = "select!")
    public IRubyObject select_bang(final ThreadContext context, final Block block) {
        if (block.isGiven()) return keep_ifCommon(context, block) ? this : context.runtime.getNil();

        return enumeratorizeWithSize(context, this, "select!", enumSizeFn());
    }

    @JRubyMethod
    public IRubyObject keep_if(final ThreadContext context, final Block block) {
        if (block.isGiven()) {
            keep_ifCommon(context, block);
            return this;
        } 

        return enumeratorizeWithSize(context, this, "keep_if", enumSizeFn());
    }
    
    public boolean keep_ifCommon(final ThreadContext context, final Block block) {
        testFrozen("Hash");
        final boolean[] modified = {false};
        iteratorVisitAll(new Visitor() {
            @Override
            public void visit(IRubyObject key, IRubyObject value) {
                if (!block.yieldSpecific(context, key, value).isTrue()) {
                    modified[0] = true;
                    remove(key);
                }
            }
        });
        return modified[0];
    }

    @Deprecated
    public IRubyObject sort(ThreadContext context, Block block) {
        return to_a().sort_bang(context, block);
    }

    /** rb_hash_index
     *
     */
    public IRubyObject index(ThreadContext context, IRubyObject expected) {
        return index19(context, expected);
    }

    @JRubyMethod(name = "index")
    public IRubyObject index19(ThreadContext context, IRubyObject expected) {
        context.runtime.getWarnings().warn(ID.DEPRECATED_METHOD, "Hash#index is deprecated; use Hash#key");
        return key(context, expected);
    }

    @JRubyMethod
    public IRubyObject key(ThreadContext context, IRubyObject expected) {
        IRubyObject key = internalIndex(context, expected);
        return key != null ? key : context.runtime.getNil();
    }

    private IRubyObject internalIndex(final ThreadContext context, final IRubyObject expected) {
        try {
            visitAll(new Visitor() {
                @Override
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

    /** rb_hash_keys
     *
     */
    @JRubyMethod(name = "keys")
    public RubyArray keys() {
        final Ruby runtime = getRuntime();
        try {
            final RubyArray keys = RubyArray.newArray(runtime, size);

            visitAll(new Visitor() {
                @Override
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
                @Override
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
    private static final Mismatch MISMATCH = new Mismatch();

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
            return Helpers.invoke(context, ifNone, "call", this, getRuntime().getNil());
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
    @JRubyMethod
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
    public IRubyObject select(final ThreadContext context, final Block block) {
        return select19(context, block);
    }

    @JRubyMethod(name = "select")
    public IRubyObject select19(final ThreadContext context, final Block block) {
        final Ruby runtime = context.runtime;
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "select", enumSizeFn());

        final RubyHash result = newHash(runtime);

        iteratorVisitAll(new Visitor() {
            @Override
            public void visit(IRubyObject key, IRubyObject value) {
                if (block.yieldArray(context, runtime.newArray(key, value), null).isTrue()) {
                    result.fastASet(key, value);
                }
            }
        });

        return result;        
    }

    /** rb_hash_delete_if
     *
     */
    public RubyHash delete_ifInternal(final ThreadContext context, final Block block) {
        modify();

        final Ruby runtime = getRuntime();
        final RubyHash self = this;
        iteratorVisitAll(new Visitor() {
            @Override
            public void visit(IRubyObject key, IRubyObject value) {
                if (block.yieldArray(context, RubyArray.newArray(runtime, key, value), null).isTrue()) {
                    self.delete(context, key, Block.NULL_BLOCK);
                }
            }
        });

        return this;
    }

    @JRubyMethod
    public IRubyObject delete_if(final ThreadContext context, final Block block) {
        return block.isGiven() ? delete_ifInternal(context, block) : enumeratorizeWithSize(context, this, "delete_if", enumSizeFn());
    }

    /** rb_hash_reject
     *
     */
    public RubyHash rejectInternal(ThreadContext context, Block block) {
        return ((RubyHash)dup()).delete_ifInternal(context, block);
    }

    @JRubyMethod
    public IRubyObject reject(final ThreadContext context, final Block block) {
        return block.isGiven() ? rejectInternal(context, block) : enumeratorizeWithSize(context, this, "reject", enumSizeFn());
    }

    /** rb_hash_reject_bang
     *
     */
    public IRubyObject reject_bangInternal(ThreadContext context, Block block) {
        int n = size;
        delete_if(context, block);
        if (n == size) return getRuntime().getNil();
        return this;
    }

    @JRubyMethod(name = "reject!")
    public IRubyObject reject_bang(final ThreadContext context, final Block block) {
        return block.isGiven() ? reject_bangInternal(context, block) : enumeratorizeWithSize(context, this, "reject!", enumSizeFn());
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
            @Override
            public void visit(IRubyObject key, IRubyObject value) {
                result.op_aset(context, value, key);
            }
        });

        return result;
    }

    /** rb_hash_update
     *
     */
    public RubyHash merge_bang(final ThreadContext context, final IRubyObject other, final Block block) {
        return merge_bang19(context, other, block);
    }

    /** rb_hash_update
     *
     */
    @JRubyMethod(name = {"merge!", "update"}, required = 1)
    public RubyHash merge_bang19(final ThreadContext context, final IRubyObject other, final Block block) {
        modify();
        final RubyHash otherHash = other.convertToHash();
        
        if (otherHash.empty_p().isTrue()) return this;

        final Ruby runtime = getRuntime();
        final RubyHash self = this;
        otherHash.visitAll(new Visitor() {
            @Override
            public void visit(IRubyObject key, IRubyObject value) {
                if (block.isGiven()) {
                    IRubyObject existing = self.internalGet(key);
                    if (existing != null) {
                        value = block.yield(context, RubyArray.newArrayNoCopy(runtime, new IRubyObject[]{key, existing, value}));
                    }
                }
                self.op_aset(context, key, value);
            }
        });

        return this;
    }

    /** rb_hash_merge
     *
     */
    @JRubyMethod
    public RubyHash merge(ThreadContext context, IRubyObject other, Block block) {
        return ((RubyHash)dup()).merge_bang(context, other, block);
    }

    /** rb_hash_replace
     *
     */
    public RubyHash initialize_copy(ThreadContext context, IRubyObject other) {
        return initialize_copy19(context, other);
    }

    /** rb_hash_replace
     *
     */
    @JRubyMethod(name = "initialize_copy", required = 1, visibility = PRIVATE)
    public RubyHash initialize_copy19(ThreadContext context, IRubyObject other) {
        return replace19(context, other);
    }

    /** rb_hash_replace
     *
     */
    public RubyHash replace(final ThreadContext context, IRubyObject other) {
        return replace19(context, other);
    }

    @JRubyMethod(name = "replace", required = 1)
    public RubyHash replace19(final ThreadContext context, IRubyObject other) {
        final RubyHash self = this;
        return replaceCommon19(context, other, new Visitor() {
            @Override
            public void visit(IRubyObject key, IRubyObject value) {
                self.op_aset(context, key, value);
            }
        });
    }

    private RubyHash replaceCommon19(final ThreadContext context, IRubyObject other, Visitor visitor) {
        modify();
        
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

    @JRubyMethod(name = "assoc")
    public IRubyObject assoc(final ThreadContext context, final IRubyObject obj) {
        try {
            visitAll(new Visitor() {
                @Override
                public void visit(IRubyObject key, IRubyObject value) {
                    if (equalInternal(context, obj, key)) {
                        throw new FoundPair(key, value);
                    }
                }
            });
            return context.runtime.getNil();
        } catch (FoundPair found) {
            return context.runtime.newArray(found.key, found.value);
        }
    }

    @JRubyMethod(name = "rassoc")
    public IRubyObject rassoc(final ThreadContext context, final IRubyObject obj) {
        try {
            visitAll(new Visitor() {
                @Override
                public void visit(IRubyObject key, IRubyObject value) {
                    if (equalInternal(context, obj, value)) {
                        throw new FoundPair(key, value);
                    }
                }
            });
            return context.runtime.getNil();
        } catch (FoundPair found) {
            return context.runtime.newArray(found.key, found.value);
        }
    }

    @JRubyMethod
    public IRubyObject flatten(ThreadContext context) {
        RubyArray ary = to_a();
        ary.callMethod(context, "flatten!", RubyFixnum.one(context.runtime));
        return ary;
    }

    @JRubyMethod
    public IRubyObject flatten(ThreadContext context, IRubyObject level) {
        RubyArray ary = to_a();
        ary.callMethod(context, "flatten!", level);
        return ary;
    }

    @JRubyMethod(name = "compare_by_identity")
    public IRubyObject getCompareByIdentity(ThreadContext context) {
        modify();
        setComparedByIdentity(true);
        return this;
    }

    @JRubyMethod(name = "compare_by_identity?")
    public IRubyObject getCompareByIdentity_p(ThreadContext context) {
        return context.runtime.newBoolean(isComparedByIdentity());
    }

    @JRubyMethod
    public IRubyObject dup(ThreadContext context) {
        RubyHash dup = (RubyHash) super.dup();
        dup.setComparedByIdentity(isComparedByIdentity());
        return dup;
    }

    @JRubyMethod(name = "clone")
    public IRubyObject rbClone(ThreadContext context) {
        RubyHash clone = (RubyHash) super.rbClone();
        clone.setComparedByIdentity(isComparedByIdentity());
        return clone;
    }

    @JRubyMethod(name = "any?")
    public IRubyObject any_p(ThreadContext context, Block block) {
        if (isEmpty()) return context.runtime.getFalse();

        if (!block.isGiven()) return context.runtime.getTrue();

        if (block.arity().getValue() > 1)
            return any_p_i_fast(context, block);

        return any_p_i(context, block);
    }

    private IRubyObject any_p_i(ThreadContext context, Block block) {
        iteratorEntry();
        try {
            for (RubyHashEntry entry = head.nextAdded; entry != head; entry = entry.nextAdded) {
                IRubyObject newAssoc = RubyArray.newArray(context.runtime, entry.key, entry.value);
                if (block.yield(context, newAssoc).isTrue())
                    return context.getRuntime().getTrue();
            }
            return context.getRuntime().getFalse();
        } finally {
            iteratorExit();
        }
    }

    private IRubyObject any_p_i_fast(ThreadContext context, Block block) {
        iteratorEntry();
        try {
            for (RubyHashEntry entry = head.nextAdded; entry != head; entry = entry.nextAdded) {
                if (block.yieldSpecific(context, entry.key, entry.value).isTrue())
                    return context.getRuntime().getTrue();
            }
            return context.getRuntime().getFalse();
        } finally {
            iteratorExit();
        }
    }

    /**
     * A lightweight dup for internal use that does not dispatch to initialize_copy nor rehash the keys. Intended for
     * use in dup'ing keyword args for processing.
     *
     * @param context
     * @return
     */
    public RubyHash dupFast(final ThreadContext context) {
        final Ruby runtime = context.runtime;
        RubyHash dup = new RubyHash(runtime, getMetaClass(), this);

        dup.setComparedByIdentity(this.isComparedByIdentity());

        dup.ifNone = this.ifNone;

        if ((this.flags & PROCDEFAULT_HASH_F) != 0) {
            dup.flags |= PROCDEFAULT_HASH_F;
        } else {
            dup.flags &= ~PROCDEFAULT_HASH_F;
        }

        return dup;
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
       int hashSize = hash.size;
       output.writeInt(hashSize);
        try {
            hash.visitLimited(new Visitor() {
                public void visit(IRubyObject key, IRubyObject value) {
                    try {
                        output.dumpObject(key);
                        output.dumpObject(value);
                    } catch (IOException e) {
                        throw new VisitorIOException(e);
                    }
                }
            }, hashSize);
        } catch (VisitorIOException e) {
            throw (IOException)e.getCause();
        }

        if (!hash.ifNone.isNil()) output.dumpObject(hash.ifNone);
    }

    public static RubyHash unmarshalFrom(UnmarshalStream input, boolean defaultValue) throws IOException {
        RubyHash result = newHash(input.getRuntime());
        input.registerLinkTarget(result);
        int size = input.unmarshalInt();
        for (int i = 0; i < size; i++) {
            result.fastASetCheckString(input.getRuntime(), input.unmarshalObject(), input.unmarshalObject());
        }
        if (defaultValue) result.default_value_set(input.unmarshalObject());
        return result;
    }

    @Override
    public Class getJavaClass() {
        return Map.class;
    }

    // Satisfy java.util.Set interface (for Java integration)

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return internalGet(JavaUtil.convertJavaToUsableRubyObject(getRuntime(), key)) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        return hasValue(getRuntime().getCurrentContext(), JavaUtil.convertJavaToUsableRubyObject(getRuntime(), value));
    }

    @Override
    public Object get(Object key) {
        IRubyObject gotten = internalGet(JavaUtil.convertJavaToUsableRubyObject(getRuntime(), key));
        return gotten == null ? null : gotten.toJava(Object.class);
    }

    @Override
    public Object put(Object key, Object value) {
        internalPut(JavaUtil.convertJavaToUsableRubyObject(getRuntime(), key), JavaUtil.convertJavaToUsableRubyObject(getRuntime(), value));
        return value;
    }

    @Override
    public Object remove(Object key) {
        IRubyObject rubyKey = JavaUtil.convertJavaToUsableRubyObject(getRuntime(), key);
        return internalDelete(rubyKey).value;
    }

    @Override
    public void putAll(Map map) {
        Ruby runtime = getRuntime();
        for (Iterator<Map.Entry> iter = map.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = iter.next();
            internalPut(JavaUtil.convertJavaToUsableRubyObject(runtime, entry.getKey()), JavaUtil.convertJavaToUsableRubyObject(runtime, entry.getValue()));
        }
    }

    @Override
    public void clear() {
        rb_clear();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof RubyHash)) return false;
        if (this == other) return true;
        return op_equal(getRuntime().getCurrentContext(), (RubyHash)other).isTrue() ? true : false;
    }

    @Override
    public Set keySet() {
        return new BaseSet(KEY_VIEW);
    }

    public Set directKeySet() {
        return new BaseSet(DIRECT_KEY_VIEW);
    }

    @Override
    public Collection values() {
        return new BaseCollection(VALUE_VIEW);
    }

    public Collection directValues() {
        return new BaseCollection(DIRECT_VALUE_VIEW);
    }

    @Override
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

        @Override
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

        @Override
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

        @Override
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

        @Override
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

        @Override
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
        @Override
        public boolean hasNext() {
            advance(false);
            return entry != head;
        }

        @Override
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
        @Override
        public Object convertEntry(Ruby runtime, RubyHashEntry entry) {
            return entry.key;
        }
        @Override
        public boolean contains(RubyHash hash, Object o) {
            if (!(o instanceof IRubyObject)) return false;
            return hash.internalGet((IRubyObject)o) != null;
        }
        @Override
        public boolean remove(RubyHash hash, Object o) {
            if (!(o instanceof IRubyObject)) return false;
            return hash.internalDelete((IRubyObject)o) != NO_ENTRY;
        }
    };

    private static final EntryView KEY_VIEW = new EntryView() {
        @Override
        public Object convertEntry(Ruby runtime, RubyHashEntry entry) {
            return entry.key.toJava(Object.class);
        }
        @Override
        public boolean contains(RubyHash hash, Object o) {
            return hash.containsKey(o);
        }
        @Override
        public boolean remove(RubyHash hash, Object o) {
            return hash.remove(o) != null;
        }
    };

    private static final EntryView DIRECT_VALUE_VIEW = new EntryView() {
        @Override
        public Object convertEntry(Ruby runtime, RubyHashEntry entry) {
            return entry.value;
        }
        @Override
        public boolean contains(RubyHash hash, Object o) {
            if (!(o instanceof IRubyObject)) return false;
            IRubyObject obj = (IRubyObject)o;
            return hash.hasValue(obj.getRuntime().getCurrentContext(), obj);
        }
        @Override
        public boolean remove(RubyHash hash, Object o) {
            if (!(o instanceof IRubyObject)) return false;
            IRubyObject obj = (IRubyObject) o;
            IRubyObject key = hash.internalIndex(obj.getRuntime().getCurrentContext(), obj);
            if (key == null) return false;
            return hash.internalDelete(key) != NO_ENTRY;
        }
    };

    private static final EntryView VALUE_VIEW = new EntryView() {
        @Override
        public Object convertEntry(Ruby runtime, RubyHashEntry entry) {
            return entry.value.toJava(Object.class);
        }
        @Override
        public boolean contains(RubyHash hash, Object o) {
            return hash.containsValue(o);
        }
        @Override
        public boolean remove(RubyHash hash, Object o) {
            IRubyObject value = JavaUtil.convertJavaToUsableRubyObject(hash.getRuntime(), o);
            IRubyObject key = hash.internalIndex(hash.getRuntime().getCurrentContext(), value);
            if (key == null) return false;
            return hash.internalDelete(key) != NO_ENTRY;
        }
    };

    private static final EntryView DIRECT_ENTRY_VIEW = new EntryView() {
        @Override
        public Object convertEntry(Ruby runtime, RubyHashEntry entry) {
            return entry;
        }
        @Override
        public boolean contains(RubyHash hash, Object o) {
            if (!(o instanceof RubyHashEntry)) return false;
            RubyHashEntry entry = (RubyHashEntry)o;
            RubyHashEntry candidate = hash.internalGetEntry(entry.key);
            return candidate != NO_ENTRY && entry.equals(candidate);
        }
        @Override
        public boolean remove(RubyHash hash, Object o) {
            if (!(o instanceof RubyHashEntry)) return false;
            return hash.internalDeleteEntry((RubyHashEntry)o) != NO_ENTRY;
        }
    };

    private static final EntryView ENTRY_VIEW = new EntryView() {
        @Override
        public Object convertEntry(Ruby runtime, RubyHashEntry entry) {
            return new ConvertingEntry(runtime, entry);
        }
        @Override
        public boolean contains(RubyHash hash, Object o) {
            if (!(o instanceof ConvertingEntry)) return false;
            ConvertingEntry entry = (ConvertingEntry)o;
            RubyHashEntry candidate = hash.internalGetEntry(entry.entry.key);
            return candidate != NO_ENTRY && entry.entry.equals(candidate);
        }
        @Override
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

        @Override
        public Object getKey() {
            return entry.key.toJava(Object.class);
        }
        @Override
        public Object getValue() {
            return entry.value.toJava(Object.class);
        }
        @Override
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
    
    @Deprecated
    public IRubyObject op_aset19(ThreadContext context, IRubyObject key, IRubyObject value) {
        modify();

        fastASetCheckString19(context.runtime, key, value);
        return value;
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

    @Deprecated
    public final void fastASetCheckString19(Ruby runtime, IRubyObject key, IRubyObject value) {
        fastASetCheckString(runtime, key, value);
    }

    @Deprecated
    public final void fastASetSmallCheckString19(Ruby runtime, IRubyObject key, IRubyObject value) {
        fastASetSmallCheckString(runtime, key, value);
    }

    @Deprecated
    public IRubyObject op_aset(IRubyObject key, IRubyObject value) {
        return op_aset(getRuntime().getCurrentContext(), key, value);
    }

    @Deprecated
    public IRubyObject each_pair(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_pairCommon(context, block, true) : enumeratorizeWithSize(context, this, "each_pair", enumSizeFn());
    }
}
