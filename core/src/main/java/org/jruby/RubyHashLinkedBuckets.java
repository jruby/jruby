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

import com.headius.backport9.buffer.Buffers;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.Create;
import org.jruby.api.Error;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.RaiseException;
import org.jruby.ir.runtime.IRBreakJump;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.Builtins;
import org.jruby.runtime.CallBlock19;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.JavaSites.HashSites;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.marshal.MarshalDumper;
import org.jruby.runtime.marshal.MarshalLoader;
import org.jruby.util.ByteList;
import org.jruby.util.RecursiveComparator;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.RubyInputStream;
import org.jruby.util.io.RubyOutputStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static org.jruby.RubyEnumerator.SizeFn;
import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.api.Access.hashClass;
import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newSharedString;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.runtimeError;
import static org.jruby.api.Error.typeError;
import static org.jruby.api.Warn.warn;
import static org.jruby.runtime.ThreadContext.hasKeywords;
import static org.jruby.runtime.ThreadContext.resetCallInfo;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.util.Inspector.EMPTY_HASH_BL;
import static org.jruby.util.Inspector.RECURSIVE_HASH_BL;

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
public class RubyHashLinkedBuckets extends RubyHash {
    protected int size = 0;
    private int threshold;

    private byte hashFlags;
    private IRubyObject ifNone;

    private int generation = 0; // generation count for O(1) clears
    private final RubyHashLinkedBuckets.RubyHashEntry head = new RubyHashLinkedBuckets.RubyHashEntry();

    RubyHashLinkedBuckets(Ruby runtime, RubyClass klass, RubyHash other) {
        super(runtime, klass, true, 0);
        this.ifNone = UNDEF;
        copyFrom(this, other, false);
    }

    protected RubyHashLinkedBuckets(Ruby runtime, RubyClass klass) {
        super(runtime, klass, true, 0);
        this.ifNone = UNDEF;
        allocFirst();
    }

    RubyHashLinkedBuckets(Ruby runtime, RubyClass klass, boolean objectSpace) {
        super(runtime, klass, objectSpace, 0);
        this.ifNone = UNDEF;
        allocFirst();
    }

    protected RubyHashLinkedBuckets(Ruby runtime, int buckets) {
        this(runtime, UNDEF, buckets);
    }

    protected RubyHashLinkedBuckets(Ruby runtime) {
        this(runtime, UNDEF);
    }

    protected RubyHashLinkedBuckets(Ruby runtime, IRubyObject defaultValue) {
        super(runtime, runtime.getHash(), true, 0);
        this.ifNone = defaultValue;
        allocFirst();
    }

    protected RubyHashLinkedBuckets(Ruby runtime, IRubyObject defaultValue, int buckets) {
        this(runtime, buckets, true);
        this.ifNone = defaultValue;
    }

    protected RubyHashLinkedBuckets(Ruby runtime, RubyClass metaClass, IRubyObject defaultValue, RubyHashEntry[] initialTable, int threshold) {
        super(runtime, metaClass, true, 0);
        this.ifNone = defaultValue;
        this.threshold = threshold;
        this.setTable(initialTable);
    }

    /*
     *  Constructor for internal usage (mainly for Array#|, Array#&, Array#- and Array#uniq)
     *  it doesn't initialize ifNone field
     */
    RubyHashLinkedBuckets(Ruby runtime, int buckets, boolean objectSpace) {
        super(runtime, runtime.getHash(), objectSpace, 0);
        this.ifNone = UNDEF;
        // FIXME: current hash implementation cannot deal with no buckets so we will add a single one
        //  (this constructor will go away once open addressing is added back ???)
        if (buckets <= 0) buckets = 1;
        allocFirst(buckets);
    }

    // TODO should this be deprecated ? (to be efficient, internals should deal with RubyHash directly)
    RubyHashLinkedBuckets(Ruby runtime, Map valueMap, IRubyObject defaultValue) {
        super(runtime, runtime.getHash(), true, 0);
        this.ifNone = defaultValue;
        allocFirst();

        for (Iterator iter = valueMap.entrySet().iterator();iter.hasNext();) {
            Entry e = (Entry)iter.next();
            internalPut((IRubyObject)e.getKey(), (IRubyObject)e.getValue());
        }
    }

    private static void copyFrom(RubyHashLinkedBuckets self, RubyHash other, boolean identity) {
        if (other instanceof RubyHashLinkedBuckets lbOther) {
            self.threshold = lbOther.threshold;
            self.setTable(lbOther.internalCopyTable(self.head));
            self.size = other.size();
        }

        self.setComparedByIdentity(identity);
    }

    public static RubyHashLinkedBuckets newLBHash(Ruby runtime, int buckets) {
        return new RubyHashLinkedBuckets(runtime, buckets);
    }

    public static RubyHashLinkedBuckets newLBHash(Ruby runtime, int buckets, boolean objectSpace) {
        return new RubyHashLinkedBuckets(runtime, buckets, objectSpace);
    }

    public static RubyHashLinkedBuckets newLBHash(Ruby runtime) {
        return new RubyHashLinkedBuckets(runtime);
    }

    public static RubyHashLinkedBuckets newLBHash(Ruby runtime, IRubyObject defaultValue) {
        return new RubyHashLinkedBuckets(runtime, defaultValue);
    }

    public static RubyHashLinkedBuckets newLBHash(Ruby runtime, IRubyObject defaultValue, int buckets) {
        return new RubyHashLinkedBuckets(runtime, defaultValue, buckets);
    }

    protected void set(int flag, boolean set) {
        if (set) {
            hashFlags |= flag;
        } else {
            hashFlags &= ~flag;
        }
    }

    protected boolean get(int flag) {
        return (hashFlags & flag) != 0;
    }

    private final void allocFirst() {
        threshold = INITIAL_THRESHOLD;
        setTable(new RubyHashEntry[MRI_HASH_RESIZE ? MRI_INITIAL_CAPACITY : JAVASOFT_INITIAL_CAPACITY]);
    }

    private final void allocFirst(int buckets) {
        if (buckets <= 0) throw new ArrayIndexOutOfBoundsException("invalid bucket size: " + buckets);
        threshold = INITIAL_THRESHOLD;
        setTable(new RubyHashEntry[buckets]);
    }

    private final void alloc() {
        generation++;
        head.prevAdded = head.nextAdded = head;
        allocFirst();
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

    { head.prevAdded = head.nextAdded = head; }

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
        final RubyHashEntry[] oldTable = getTable();
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

        setTable(newTable);
    }

    private final void JavaSoftCheckResize() {
        if (overThreshold()) {
            RubyHashEntry[] tbl = getTable();
            if (tbl.length == MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return;
            }
            resizeAndAdjustThreshold(getTable());
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
    private static final int ST_DEFAULT_MAX_DENSITY = 2;
    private final void MRICheckResize() {
        if (size / getTable().length > ST_DEFAULT_MAX_DENSITY) {
            int forSize = getTable().length + 1; // size + 1;
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

    protected final int hashValue(final IRubyObject key) {
        final int h = isComparedByIdentity() ? System.identityHashCode(key) : key.hashCode();
        return MRI_HASH ? MRIHashValue(h) : JavaSoftHashValue(h);
    }

    private static int bucketIndex(final int h, final int length) {
        return MRI_HASH ? MRIBucketIndex(h, length) : JavaSoftBucketIndex(h, length);
    }

    private void checkResize() {
        if (MRI_HASH_RESIZE) MRICheckResize(); else JavaSoftCheckResize();
    }

    protected final void checkIterating() {
        if (iteratorCount > 0) {
            throw metaClass.runtime.newRuntimeError("can't add a new key into hash during iteration");
        }
    }

    // put implementation

    public IRubyObject internalPut(final IRubyObject key, final IRubyObject value) {
        checkResize();
        return internalPutNoResize(key, value, true);
    }

    private void internalPutSmall(final IRubyObject key, final IRubyObject value) {
        internalPutNoResize(key, value, true);
    }

    private void internalPut(final IRubyObject key, final IRubyObject value, final boolean checkForExisting) {
        checkResize();
        internalPutNoResize(key, value, checkForExisting);
    }

    final boolean internalPutIfNoKey(final IRubyObject key, final IRubyObject value) {
        if (internalGetEntry(key) == NULL_ENTRY) {
            internalPut(key, value);
            return true;
        }
        return false;
    }

    protected IRubyObject internalPutNoResize(final IRubyObject key, final IRubyObject value, final boolean checkForExisting) {
        final int hash = hashValue(key);
        final RubyHashEntry[] table = this.getTable();

        final int i = bucketIndex(hash, table.length);

        if (checkForExisting) {
            for (RubyHashEntry entry = table[i]; entry != null; entry = entry.next) {
                if (internalKeyExist(entry.hash, entry.key, hash, key)) {
                    IRubyObject existing = entry.value;
                    entry.value = value;

                    return existing;
                }
            }
        }

        checkIterating();

        table[i] = new RubyHashEntry(hash, key, value, table[i], head);
        size++;

        // no existing entry
        return null;
    }

    // get implementation

    protected IRubyObject internalGet(IRubyObject key) { // specialized for value
        return internalGetEntry(key).value;
    }

    protected RubyHashEntry internalGetEntry(IRubyObject key) {
        if (size == 0) return NULL_ENTRY;

        final int hash = hashValue(key);
        final RubyHashEntry[] table = this.getTable();

        for (RubyHashEntry entry = table[bucketIndex(hash, table.length)]; entry != null; entry = entry.next) {
            if (internalKeyExist(entry.hash, entry.key, hash, key)) {
                return entry;
            }
        }
        return NULL_ENTRY;
    }

    @Deprecated
    @Override
    final RubyHashEntry getEntry(IRubyObject key) {
        return internalGetEntry(key);
    }

    private boolean internalKeyExist(int entryHash, IRubyObject entryKey, int hash, IRubyObject key) {
        return (entryHash == hash
                && (entryKey == key || (!isComparedByIdentity() && key.eql(entryKey))));
    }

    // delete implementation


    protected RubyHashEntry internalDelete(final IRubyObject key) {
        if (size == 0) return NULL_ENTRY;

        return internalDelete(hashValue(key), MATCH_KEY, key);
    }

    protected RubyHashEntry internalDeleteEntry(final RubyHashEntry entry) {
        // n.b. we need to recompute the hash in case the key object was modified
        return internalDelete(hashValue(entry.key), MATCH_ENTRY, entry);
    }

    private final RubyHashEntry internalDelete(final int hash, final EntryMatchType matchType, final Object obj) {
        final int i = bucketIndex(hash, getTable().length);

        RubyHashEntry entry = getTable()[i];
        if (entry != null) {
            RubyHashEntry prior = null;
            for (; entry != null; prior = entry, entry = entry.next) {
                if (entry.hash == hash && matchType.matches(entry, obj)) {
                    if (prior != null) {
                        prior.next = entry.next;
                    } else {
                        getTable()[i] = entry.next;
                    }
                    entry.detach();
                    size--;
                    return entry;
                }
            }
        }

        return NULL_ENTRY;
    }

    private RubyHashEntry[] getTable() {
        return (RubyHashEntry[]) state;
    }

    private void setTable(RubyHashEntry[] table) {
        this.state = table;
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
         RubyHashEntry[]newTable = new RubyHashEntry[getTable().length];

         for (RubyHashEntry entry = head.nextAdded; entry != head; entry = entry.nextAdded) {
             int i = bucketIndex(entry.hash, getTable().length);
             newTable[i] = new RubyHashEntry(entry.hash, entry.key, entry.value, newTable[i], destHead);
         }
         return newTable;
    }

    public <T> void visitAll(ThreadContext context, VisitorWithStateI visitor) {
        int startGeneration = generation;
        long count = size;
        int index = 0;
        // visit not more than size entries
        for (RubyHashEntry entry = head.nextAdded; entry != head && count != 0; entry = entry.nextAdded) {
            if (startGeneration != generation) {
                startGeneration = generation;
                entry = head.nextAdded;
                if (entry == head) break;
            }
            if (entry != null && entry.isLive()) {
                visitor.visit(context, this, entry.key, entry.value, index++);
                count--;
            }
        }
        // it does not handle all concurrent modification cases,
        // but at least provides correct marshal as we have exactly size entries visited (count == 0)
        // or if count < 0 - skipped concurrent modification checks
        if (count > 0) throw concurrentModification();
    }

    public <T> void visitAll(ThreadContext context, VisitorWithState visitor, T state) {
        // use -1 to disable concurrency checks
        visitLimited(context, visitor, -1, state);
    }

    protected <T> void visitLimited(ThreadContext context, VisitorWithState visitor, long size, T state) {
        int startGeneration = generation;
        long count = size;
        int index = 0;
        // visit not more than size entries
        for (RubyHashEntry entry = head.nextAdded; entry != head && count != 0; entry = entry.nextAdded) {
            if (startGeneration != generation) {
                startGeneration = generation;
                entry = head.nextAdded;
                if (entry == head) break;
            }
            if (entry != null && entry.isLive()) {
                visitor.visit(context, this, entry.key, entry.value, index++, state);
                count--;
            }
        }
        // it does not handle all concurrent modification cases,
        // but at least provides correct marshal as we have exactly size entries visited (count == 0)
        // or if count < 0 - skipped concurrent modification checks
        if (count > 0) throw concurrentModification();
    }

    public <T> boolean allSymbols() {
        int startGeneration = generation;
        // visit not more than size entries
        RubyHashEntry head = this.head;
        for (RubyHashEntry entry = head.nextAdded; entry != head; entry = entry.nextAdded) {
            if (startGeneration != generation) {
                startGeneration = generation;
                entry = head.nextAdded;
                if (entry == head) break;
            }
            if (entry != null && entry.isLive()) {
                if (!(entry.key instanceof RubySymbol)) return false;
            }
        }
        return true;
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
    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, final Block block) {
        modify();

        if (block.isGiven()) {
            ifNone = context.runtime.newProc(Block.Type.PROC, block);
            set(PROCDEFAULT_HASH, true);
        } else {
            ifNone = UNDEF;
        }
        return this;
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject _default, final Block block) {
        boolean keywords = hasKeywords(ThreadContext.resetCallInfo(context));
        modify();

        if (keywords) {
            IRubyObject[] opts = ArgsUtil.extractKeywordArgs(context, (RubyHash) _default, "capacity");
            // This will allocFirst twice since it already happened before initialize was called.  I think this is ok?
            if (opts[0] instanceof RubyFixnum fixnum && fixnum.asInt(context) > 0) allocFirst(fixnum.asInt(context));

            if (block.isGiven()) {
                ifNone = context.runtime.newProc(Block.Type.PROC, block);
                set(PROCDEFAULT_HASH, true);
            } else {
                ifNone = UNDEF;
            }
        } else {
            if (block.isGiven()) throw argumentError(context, 1, 0);

            ifNone = _default;
        }

        return this;
    }

    @JRubyMethod(visibility = PRIVATE, keywords = true)
    public IRubyObject initialize(ThreadContext context, IRubyObject _default, IRubyObject hash, final Block block) {
        if (!hasKeywords(ThreadContext.resetCallInfo(context))) throw argumentError(context, 2, 0, 1);

        IRubyObject[] opts = ArgsUtil.extractKeywordArgs(context, (RubyHash) hash, "capacity");

        // This will allocFirst twice since it already happened before initialize was called.  I think this is ok?
        if (opts[0] instanceof RubyFixnum fixnum && fixnum.asInt(context) > 0) allocFirst(fixnum.asInt(context));

        modify();

        if (block.isGiven()) throw argumentError(context, 1, 0);

        ifNone = _default;

        return this;
    }

    @JRubyMethod(name = "default")
    public IRubyObject default_value_get(ThreadContext context) {
        if (get(PROCDEFAULT_HASH)) {
            return context.nil;
        }
        return ifNone == UNDEF ? context.nil : ifNone;
    }

    @JRubyMethod(name = "default")
    public IRubyObject default_value_get(ThreadContext context, IRubyObject arg) {
        if (get(PROCDEFAULT_HASH)) {
            return sites(context).call.call(context, ifNone, ifNone, this, arg);
        }
        return ifNone == UNDEF ? context.nil : ifNone;
    }

    /** rb_hash_set_default
     *
     */
    @JRubyMethod(name = "default=")
    public IRubyObject default_value_set(ThreadContext context, final IRubyObject defaultValue) {
        modify();

        ifNone = defaultValue;
        set(PROCDEFAULT_HASH, false);

        return ifNone;
    }

    /** rb_hash_default_proc
     *
     */
    @JRubyMethod
    public IRubyObject default_proc(ThreadContext context) {
        return get(PROCDEFAULT_HASH) ? ifNone : context.nil;
    }

    /** default_proc_arity_check
     *
     */
    private void checkDefaultProcArity(ThreadContext context, Block block) {
        int n = block.getSignature().arityValue();

        if (block.type == Block.Type.LAMBDA && n != 2 && (n >= 0 || n < -3)) {
            if (n < 0) n = -n-1;
            throw typeError(context, "default_proc takes two arguments (2 for " + n + ")");
        }
    }

    /** rb_hash_set_default_proc
     *
     */
    @JRubyMethod(name = "default_proc=")
    public IRubyObject set_default_proc(ThreadContext context, IRubyObject proc) {
        modify();

        if (proc.isNil()) {
            ifNone = proc;
            set(PROCDEFAULT_HASH, false);
            return proc;
        }

        IRubyObject b = TypeConverter.convertToType(proc, context.runtime.getProc(), "to_proc");
        if (b.isNil() || !(b instanceof RubyProc)) throw typeError(context, "wrong default_proc type ", proc, " (expected Proc)");

        proc = b;
        checkDefaultProcArity(context, ((RubyProc) proc).getBlock());
        ifNone = proc;
        set(PROCDEFAULT_HASH, true);
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
    protected RubyString inspectHash(final ThreadContext context) {
        final RubyString str = RubyString.newStringLight(context.runtime, DEFAULT_INSPECT_STR_SIZE, USASCIIEncoding.INSTANCE);

        str.cat((byte)'{');

        visitAll(context, InspectVisitor, str);
        str.cat((byte)'}');
        return str;
    }

    private static final VisitorWithState<RubyString> InspectVisitor = new VisitorWithState<RubyString>() {
        @Override
        // MRI: inspect_i for inspect_hash in hash.c
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, RubyString str) {
            RubyString keyStr;

            boolean isSymbol = key instanceof RubySymbol;
            boolean quote = false;
            if (key instanceof RubySymbol symbol) {
                keyStr = symbol.fstring();
                quote = symbolKeyNeedsQuote(context, symbol);
            } else {
                keyStr = inspect(context, key);
            }

            if (str.size() > 1) {
                str.catString(", ");
            } else {
                str.setEncoding(keyStr.getEncoding());
            }

            if (quote) {
                str.append(keyStr.inspect(context));
            } else {
                str.append(keyStr);
            }

            str.catString(isSymbol ? ": " : " => ");

            RubyString valStr = inspect(context, value);
            str.append(valStr);
        }

        // MRI: symbol_key_needs_quote
        boolean symbolKeyNeedsQuote(ThreadContext context, RubySymbol sym) {
            RubyString str = sym.fstring();
            int len = str.size();
            if (len == 0 || !sym.isSimpleName(context)) return true;
            ByteList bytes = str.getByteList();
            int first = bytes.get(0);
            if (first == '@' || first == '$' || first == '!') return true;
            if (!RubyString.atCharBoundary(bytes.unsafeBytes(), bytes.begin(), bytes.begin() + bytes.getRealSize() - 1, bytes.begin() + bytes.realSize(), bytes.getEncoding())) return false;
            switch (bytes.get(len - 1)) {
                case '+':
                case '-':
                case '*':
                case '/':
                case '`':
                case '%':
                case '^':
                case '&':
                case '|':
                case ']':
                case '<':
                case '=':
                case '>':
                case '~':
                case '@':
                    return true;
                default:
                    return false;
            }
        }
    };

    /**
     * Append appropriate characters to indicate association (": " vs " => ").
     *
     * @param keyIsSymbol is the key a symbol
     * @param bytes buffer to which to append
     */
    protected void appendAssociation(boolean keyIsSymbol, ByteList bytes) {
        if (keyIsSymbol) {
            bytes.append(':').append(' ');
        } else {
            bytes.append(' ').append('=').append('>').append(' ');
        }
    }

    /** rb_hash_inspect
     *
     */
    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        final Ruby runtime = context.runtime;

        if (size() == 0) return newSharedString(context, EMPTY_HASH_BL);
        if (runtime.isInspecting(this)) return newSharedString(context, RECURSIVE_HASH_BL);

        try {
            runtime.registerInspecting(this);
            return inspectHash(context);
        } finally {
            runtime.unregisterInspecting(this);
        }
    }

    /** rb_hash_size
     *
     */
    @JRubyMethod(name = {"size", "length"})
    public RubyFixnum rb_size(ThreadContext context) {
        return asFixnum(context, size);
    }

    /**
     * A size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject size(ThreadContext context, RubyHashLinkedBuckets recv, IRubyObject[] args) {
        return recv.rb_size(context);
    }

    /** rb_hash_empty_p
     *
     */
    @JRubyMethod(name = "empty?")
    public RubyBoolean empty_p(ThreadContext context) {
        return size == 0 ? context.tru : context.fals;
    }

    /** rb_hash_to_a
     *
     */
    @JRubyMethod(name = "to_a")
    @Override
    public RubyArray to_a(ThreadContext context) {
        final Ruby runtime = context.runtime;
        try {
            final RubyArray result = RubyArrayNative.newBlankArrayInternal(runtime, size);

            visitAll(context, RubyHashLinkedBuckets.StoreKeyValueVisitor, result);

            return result;
        } catch (NegativeArraySizeException nase) {
            throw concurrentModification();
        }
    }

    private static final VisitorWithState<RubyArray> StoreKeyValueVisitor = new VisitorWithState<RubyArray>() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, RubyArray result) {
            result.storeInternal(context, index, newArray(context, key, value));
        }
    };

    /** rb_hash_to_s &amp; to_s_hash
     *
     */
    @JRubyMethod(name = "to_s")
    public IRubyObject to_s(ThreadContext context) {
        return inspect(context);
    }

    /** rb_hash_rehash
     *
     */
    @JRubyMethod(name = "rehash")
    public RubyHash rehash(ThreadContext context) {
        if (iteratorCount > 0) throw runtimeError(context, "rehash during iteration");

        modify();
        final RubyHashEntry[] oldTable = getTable();
        final RubyHashEntry[] newTable = new RubyHashEntry[oldTable.length];
        Set<Integer> rehashedIndexes = new HashSet<>();
        for (int j = 0; j < oldTable.length; j++) {
            RubyHashEntry entry = oldTable[j];
            oldTable[j] = null;
            while (entry != null) {
                RubyHashEntry next = entry.next;
                int oldHash = entry.hash;
                IRubyObject key = entry.key;
                int newHash = hashValue(key);

                int i = bucketIndex(newHash, newTable.length);

                RubyHashEntry newEntry = newTable[i];
                if (newEntry != null && internalKeyExist(newEntry.hash, newEntry.key, newHash, key)) {
                    RubyHashEntry tmpNext = entry.nextAdded;
                    RubyHashEntry tmpPrev = entry.prevAdded;
                    tmpPrev.nextAdded = tmpNext;
                    tmpNext.prevAdded = tmpPrev;
                    size--;
                } else {
                    // replace entry if hash changed
                    if (oldHash != newHash) {
                        entry = new RubyHashEntry(entry, newHash);
                        // memorize hash value
                        rehashedIndexes.add(newHash);
                    }

                    entry.next = newEntry;
                    newTable[i] = entry;
                }
                entry = next;
            }
        }
        setTable(newTable);

        // When a hash is large and contains duplicate keys, sometimes the above logic can not remove duplicate key.
        // searches duplicate keys and removes it.
        for (int hash : rehashedIndexes) {
            RubyHashEntry entry = getTable()[bucketIndex(hash, newTable.length)];
            while (entry != null) {
                if (entry.hash == hash) {
                    RubyHashEntry nextEntry = entry.next;
                    while (nextEntry != null) {
                        if (internalKeyExist(entry.hash, entry.key, nextEntry.hash, nextEntry.key)) {
                            RubyHashEntry tmpNext = entry.nextAdded;
                            RubyHashEntry tmpPrev = entry.prevAdded;
                            tmpPrev.nextAdded = tmpNext;
                            tmpNext.prevAdded = tmpPrev;
                            size--;
                        }
                        nextEntry = nextEntry.next;
                    }
                }
                entry = entry.next;
            }
        }
        return this;
    }

    /** rb_hash_to_hash
     *
     */
    @JRubyMethod(name = "to_hash")
    public RubyHash to_hash(ThreadContext context) {
        return this;
    }

    @JRubyMethod
    public RubyHash to_h(ThreadContext context, Block block) {
        if (block.isGiven()) return to_h_block(context, block);
        return getType() == hashClass(context) ? this : Create.newHash(context).replace(context, this);
    }

    protected RubyHash to_h_block(ThreadContext context, Block block) {
        RubyHash result = Create.newHash(context);

        visitAll(context, (ctxt, self, key, value, index) -> {
            IRubyObject elt = block.yieldArray(ctxt, newArray(ctxt, key, value), null);
            IRubyObject keyValue = elt.checkArrayType();

            if (keyValue == ctxt.nil) throw typeError(context, "wrong element type ", elt, " (expected array)");

            var ary = (RubyArray<?>) keyValue;
            if (ary.getLength() != 2) {
                throw argumentError(context, "element has wrong array length " + "(expected 2, was " + ary.getLength() + ")");
            }

            result.fastASet(ary.eltInternal(0), ary.eltInternal(1));
        });

        return result;
    }

    @Override
    public RubyHashLinkedBuckets convertToHash() {
        return this;
    }

    public final void fastASet(IRubyObject key, IRubyObject value) {
        internalPut(key, value);
    }

    public final void fastASetSmall(IRubyObject key, IRubyObject value) {
        internalPutSmall(key, value);
    }

    // MRI: rb_hash_set_pair, fast/small version
    public final void fastASetSmallPair(ThreadContext context, IRubyObject _pair) {
        IRubyObject pair;

        pair = TypeConverter.checkArrayType(context, _pair);
        if (pair.isNil()) {
            throw typeError(context, "wrong element type " + _pair.getType() + " (expected array)");
        }
        RubyArray pairAry = (RubyArray) pair;
        int len = pairAry.size();
        if (len != 2) {
            throw argumentError(context, "element has wrong array length (expected 2, was " + len + ")");
        }
        fastASetSmall(pairAry.eltOk(0), pairAry.eltOk(1));
    }

    public final void fastASetCheckString(Ruby runtime, IRubyObject key, IRubyObject value) {
      if (key instanceof RubyString strKey && !isComparedByIdentity()) {
          op_asetForString(runtime, strKey, value);
      } else {
          internalPut(key, value);
      }
    }

    public final void fastASetSmallCheckString(Ruby runtime, IRubyObject key, IRubyObject value) {
        if (key instanceof RubyString strKey) {
            op_asetSmallForString(runtime, strKey, value);
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

    /**
     * Set a key/value pair into this hash.
     *
     * @param context the current thread context
     * @param key the key
     * @param value the value
     * @return the value set
     */
    // MRI: rb_hash_aset
    @JRubyMethod(name = {"[]=", "store"})
    public IRubyObject op_aset(ThreadContext context, IRubyObject key, IRubyObject value) {
        modify();

        fastASetCheckString(context.runtime, key, value);
        return value;
    }


    protected void op_asetForString(Ruby runtime, RubyString key, IRubyObject value) {
        final RubyHashEntry entry = internalGetEntry(key);
        if (entry != NULL_ENTRY) {
            entry.value = value;
        } else {
            checkIterating();

            if (!key.isFrozen()) key = hashKeyString(runtime.getCurrentContext(), key);

            internalPut(key, value, false);
        }
    }

    protected void op_asetSmallForString(Ruby runtime, RubyString key, IRubyObject value) {
        final RubyHashEntry entry = internalGetEntry(key);
        if (entry != NULL_ENTRY) {
            entry.value = value;
        } else {
            checkIterating();

            if (!key.isFrozen()) key = hashKeyString(runtime.getCurrentContext(), key);

            internalPutNoResize(key, value, false);
        }
    }

    // MRI: rb_hash_key_str
    private static RubyString hashKeyString(ThreadContext context, RubyString key) {
        return key.isBare(context) ? context.runtime.freezeAndDedupString(key) : (RubyString) key.dupFrozen();
    }

    // returns null when not found to avoid unnecessary getRuntime().getNil() call
    public final IRubyObject fastARef(IRubyObject key) {
        return internalGet(key);
    }

    public RubyBoolean compare(final ThreadContext context, VisitorWithState<RubyHash> visitor, IRubyObject other, boolean eql) {
        if (!(other instanceof RubyHash)) {
            if (!sites(context).respond_to_to_hash.respondsTo(context, other, other)) {
                return context.fals;
            }

            if(eql) {
                return Helpers.rbEql(context, other, this);
            }else {
                return Helpers.rbEqual(context, other, this);

            }
        }

        final RubyHashLinkedBuckets otherHash = (RubyHashLinkedBuckets) other;

        if (this.size() != otherHash.size()) {
            return context.fals;
        }

        try {
            visitAll(context, visitor, otherHash);
        } catch (Mismatch e) {
            return context.fals;
        }

        return context.tru;
    }

    private static final VisitorWithState<RubyHash> FindMismatchUsingEqualVisitor = new VisitorWithState<RubyHash>() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, RubyHash otherHash) {
            IRubyObject value2 = otherHash.fastARef(key);

            if (value2 == null) {
                // other hash does not contain key
                throw MISMATCH;
            }

            if (!Helpers.rbEqual(context, value, value2).isTrue()) {
                throw MISMATCH;
            }
        }
    };

    private static final VisitorWithState<RubyHash> FindMismatchUsingEqlVisitor = new VisitorWithState<RubyHash>() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, RubyHash otherHash) {
            IRubyObject value2 = otherHash.fastARef(key);

            if (value2 == null) {
                // other hash does not contain key
                throw MISMATCH;
            }

            if (!Helpers.rbEql(context, value, value2).isTrue()) {
                throw MISMATCH;
            }
        }
    };

    /** rb_hash_equal
     *
     */
    @Override
    @JRubyMethod(name = "==")
    public IRubyObject op_equal(final ThreadContext context, IRubyObject other) {
        return hashEqual(context, this, other, false);
    }

    static IRubyObject hashEqual(final ThreadContext context, RubyHash hash1, IRubyObject _hash2, boolean eql) {
        if (hash1 == _hash2) return context.tru;
        if (!(_hash2 instanceof RubyHash hash2)) {
            if (!_hash2.respondsTo("to_hash")) {
                return context.fals;
            }
            if (eql) {
                if (Helpers.rbEql(context, _hash2, hash1).isTrue()) {
                    return context.tru;
                } else {
                    return context.fals;
                }
            } else {
                return Helpers.rbEqual(context, _hash2, hash1);
            }
        }
        if (hash1.size() != hash2.size()) return context.fals;
        if (!hash1.isEmpty() && !hash2.isEmpty()) {
            if (hash1.isComparedByIdentity() != hash2.isComparedByIdentity()) {
                return context.fals;
            } else {
                return RecursiveComparator.compare(
                        context,
                        eql ? FindMismatchUsingEqlVisitor : FindMismatchUsingEqualVisitor,
                        hash1, hash2, eql);
            }
        }
        return context.tru;
    }

    /** rb_hash_eql
     *
     */
    @JRubyMethod(name = "eql?")
    public IRubyObject op_eql(final ThreadContext context, IRubyObject other) {
        return hashEqual(context, this, other, true);
    }

    /** rb_hash_aref
     *
     */
    @JRubyMethod(name = "[]")
    public IRubyObject op_aref(ThreadContext context, IRubyObject key) {
        IRubyObject value;
        return ((value = internalGet(key)) == null) ? sites(context).self_default.call(context, this, this, key) : value;
    }

    /** hash_le_i
     *
     */
    private boolean hash_le(RubyHash other) {
        return other.directEntrySet().containsAll(directEntrySet());
    }

    @JRubyMethod(name = "<")
    public IRubyObject op_lt(ThreadContext context, IRubyObject other) {
        final RubyHash otherHash = ((RubyBasicObject) other).convertToHash();
        if (size() >= otherHash.size()) return context.fals;

        return asBoolean(context, hash_le(otherHash));
    }

    @JRubyMethod(name = "<=")
    public IRubyObject op_le(ThreadContext context, IRubyObject other) {
        final RubyHash otherHash = other.convertToHash();
        if (size() > otherHash.size()) return context.fals;

        return asBoolean(context, hash_le(otherHash));
    }

    @JRubyMethod(name = ">")
    public IRubyObject op_gt(ThreadContext context, IRubyObject other) {
        final RubyHash otherHash = other.convertToHash();
        return otherHash.op_lt(context, this);
    }

    @JRubyMethod(name = ">=")
    public IRubyObject op_ge(ThreadContext context, IRubyObject other) {
        final RubyHash otherHash = other.convertToHash();
        return otherHash.op_le(context, this);
    }

    // MRI: rb_hash_hash
    @JRubyMethod(name = "hash")
    public RubyFixnum hash(ThreadContext context) {
        final int size = size();

        long hash = Helpers.hashStart(context.runtime, size);

        if (size != 0) {
            long[] hval = {hash};

            iteratorVisitAll(context, CalculateHashVisitor, hval);

            hash = hval[0];
        }

        return asFixnum(context, hash);
    }

    private static final ThreadLocal<ByteBuffer> HASH_16_BYTE = ThreadLocal.withInitial(() -> ByteBuffer.allocate(16));

    private static final VisitorWithState<long[]> CalculateHashVisitor = new VisitorWithState<long[]>() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, long[] hval) {
            // perform hashing of key and value before populating shared buffer
            long keyHash = Helpers.safeHashLong(context, key);
            long valueHash = Helpers.safeHashLong(context, value);

            ByteBuffer buffer = HASH_16_BYTE.get();
            Buffers.clearBuffer(buffer).putLong(keyHash).putLong(valueHash);

            hval[0] ^= Helpers.multAndMix(Ruby.getHashSeed0(), Arrays.hashCode(buffer.array()));
        }
    };

    /** rb_hash_fetch
     *
     */
    public IRubyObject fetch(ThreadContext context, IRubyObject[] args, Block block) {
        Arity.checkArgumentCount(context, args.length, 1, 2);

        switch(args.length) {
            case 1: return fetch(context, args[0], block);
            case 2: return fetch(context, args[0], args[1], block);
        }

        return null;
    }

    @JRubyMethod(rest = true)
    public IRubyObject except(ThreadContext context, IRubyObject[] keys) {
        RubyHashLinkedBuckets result = hashCopyWithIdentity(context);

        for (int i = 0; i < keys.length; i++) {
            result.delete(context, keys[i]);
        }

        return result;
    }

    @JRubyMethod
    public IRubyObject fetch(ThreadContext context, IRubyObject key, Block block) {
        IRubyObject value = internalGet(key);

        if (value != null) return value;
        if (block.isGiven()) return block.yield(context, key);

        throw context.runtime.newKeyError("key not found: " + key.inspect(context), this, key);
    }

    @JRubyMethod
    public IRubyObject fetch(ThreadContext context, IRubyObject key, IRubyObject _default, Block block) {
        if (block.isGiven()) warn(context, "block supersedes default value argument");

        IRubyObject value = internalGet(key);

        if (value == null) {
            if (block.isGiven()) return block.yield(context, key);

            return _default;
        }

        return value;
    }

    /** rb_hash_has_key
     *
     */
    @JRubyMethod(name = {"has_key?", "key?", "include?", "member?"})
    public RubyBoolean has_key_p(ThreadContext context, IRubyObject key) {
        return hasKey(key) ? context.tru : context.fals;
    }

    /**
     * A Java API to test the presence of a (Ruby) key in the Hash
     * @param key the native (Ruby) key
     * @return true if the hash contains the provided key
     */
    public boolean hasKey(IRubyObject key) {
        return internalGetEntry(key) != NULL_ENTRY;
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

    private boolean hasValue(ThreadContext context, IRubyObject expected) {
        try {
            visitAll(context, FoundIfEqualVisitor, expected);
            return false;
        } catch (Found found) {
            return true;
        }
    }

    private static final VisitorWithState<IRubyObject> FoundIfEqualVisitor = new VisitorWithState<IRubyObject>() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, IRubyObject expected) {
            if (equalInternal(context, value, expected)) {
                throw FOUND;
            }
        }
    };

    /** rb_hash_has_value
     *
     */
    @JRubyMethod(name = {"has_value?", "value?"})
    public RubyBoolean has_value_p(ThreadContext context, IRubyObject expected) {
        return asBoolean(context, hasValue(context, expected));
    }

    private volatile int iteratorCount;

    private static final AtomicIntegerFieldUpdater<RubyHashLinkedBuckets> ITERATOR_UPDATER;
    static {
        AtomicIntegerFieldUpdater<RubyHashLinkedBuckets> iterUp = null;
        try {
            iterUp = AtomicIntegerFieldUpdater.newUpdater(RubyHashLinkedBuckets.class, "iteratorCount");
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

    private synchronized void iteratorExitSync() {
        --iteratorCount;
    }

    private void iteratorVisitAll(ThreadContext context, VisitorWithStateI visitor) {
        try {
            iteratorEntry();
            visitAll(context, visitor);
        } finally {
            iteratorExit();
        }
    }

    private <T> void iteratorVisitAll(ThreadContext context, VisitorWithState<T> visitor, T state) {
        try {
            iteratorEntry();
            visitAll(context, visitor, state);
        } finally {
            iteratorExit();
        }
    }

    /** rb_hash_each
     *
     */
    public RubyHashLinkedBuckets eachCommon(final ThreadContext context, final Block block) {
        iteratorVisitAll(context, YieldArrayVisitor, block);

        return this;
    }

    private static final VisitorWithState<Block> YieldArrayVisitor = new VisitorWithState<Block>() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, Block block) {
            block.yieldArray(context, newArray(context, key, value), null);
        }
    };

    @JRubyMethod(name = {"each", "each_pair"})
    public IRubyObject each(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_pairCommon(context, block) : enumeratorizeWithSize(context, this, "each", RubyHashLinkedBuckets::size);
    }

    /** rb_hash_each_pair
     *
     */
    public RubyHashLinkedBuckets each_pairCommon(final ThreadContext context, final Block block) {
        iteratorVisitAll(context, YieldKeyValueArrayVisitor, block);

        return this;
    }

    private static final VisitorWithState<Block> YieldKeyValueArrayVisitor = new VisitorWithState<Block>() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, Block block) {
            if (block.type == Block.Type.LAMBDA) {
                block.call(context, newArray(context, key, value));
            } else if (block.getSignature().isSpreadable()) {
                block.yieldSpecific(context, key, value);
            } else {
                block.yield(context, newArray(context, key, value));
            }
        }
    };

    /** rb_hash_each_value
     *
     */
    public RubyHashLinkedBuckets each_valueCommon(final ThreadContext context, final Block block) {
        iteratorVisitAll(context, YieldValueVisitor, block);

        return this;
    }

    private static final VisitorWithState<Block> YieldValueVisitor = new VisitorWithState<Block>() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, Block block) {
            block.yield(context, value);
        }
    };

    @JRubyMethod
    public IRubyObject each_value(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_valueCommon(context, block) : enumeratorizeWithSize(context, this, "each_value", RubyHashLinkedBuckets::size);
    }

    /** rb_hash_each_key
     *
     */
    public RubyHashLinkedBuckets each_keyCommon(final ThreadContext context, final Block block) {
        iteratorVisitAll(context, YieldKeyVisitor, block);

        return this;
    }

    private static final VisitorWithState<Block> YieldKeyVisitor = new VisitorWithState<Block>() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, Block block) {
            block.yield(context, key);
        }
    };

    @JRubyMethod
    public IRubyObject each_key(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_keyCommon(context, block) : enumeratorizeWithSize(context, this, "each_key", RubyHashLinkedBuckets::size);
    }

    @JRubyMethod(name = "transform_keys", rest = true)
    public IRubyObject transform_keys(final ThreadContext context, IRubyObject[] args, final Block block) {
        if (args.length == 0 && !block.isGiven()) {
            return enumeratorizeWithSize(context, this, "transform_keys", RubyHashLinkedBuckets::size);
        }

        IRubyObject transformHash = args.length > 0 ?
                TypeConverter.convertToTypeWithCheck(args[0], hashClass(context), "to_hash") :
                context.nil;
        RubyHash result = Create.newHash(context);

        if (!isEmpty()) {
            if (!transformHash.isNil()) {
                visitAll(context, (ctxt, self, key, value, index) -> {
                    IRubyObject newKey = ((RubyHashLinkedBuckets) transformHash).internalGet(key);
                    if (newKey == null) newKey = block.isGiven() ? block.yield(ctxt, key) : key;
                    result.fastASet(newKey, value);
                });
            } else {
                visitAll(context, (ctxt, self, key, value, index) -> result.fastASet(block.yield(ctxt, key), value));
            }
        }

        return result;
    }

    private RubyHashLinkedBuckets hashCopyWithIdentity(ThreadContext context) {
        RubyHashLinkedBuckets copy = new RubyHashLinkedBuckets(context.runtime, hashClass(context));

        copy.replaceWith(context, this);

        return copy;
    }

    @JRubyMethod(name = "transform_values")
    public IRubyObject transform_values(final ThreadContext context, final Block block) {
        return hashCopyWithIdentity(context).transform_values_bang(context, block);
    }

    @JRubyMethod(name = "transform_keys!", rest = true)
    public IRubyObject transform_keys_bang(final ThreadContext context, IRubyObject[] args, final Block block) {
        if (args.length == 0 && !block.isGiven()) {
            return enumeratorizeWithSize(context, this, "transform_keys!", RubyHashLinkedBuckets::size);
        }

        IRubyObject transformHash = args.length > 0 ?
                TypeConverter.convertToTypeWithCheck(args[0], hashClass(context), "to_hash") :
                context.nil;
        modify();

        if (!isEmpty()) {
            RubyArray pairs = (RubyArray) flatten(context);
            RubyHash newKeys = Create.newHash(context);
            int length = pairs.size();
            boolean aborted = false; // If break happens in blocks we stop transforming but still leave rest as-is.
            for (int i = 0; i < length; i += 2) {
                IRubyObject oldKey = pairs.eltOk(i);
                IRubyObject newKey;

                if (aborted) {
                    newKey = oldKey;
                } else {
                    if (transformHash.isNil()) {
                        try {
                            newKey = block.yield(context, oldKey);
                        } catch (IRBreakJump e) {
                            aborted = true;
                            continue;
                        }
                    } else {
                        newKey = ((RubyHashLinkedBuckets) transformHash).internalGet(oldKey);

                        if (newKey == null) {
                            if (block.isGiven()) {
                                try {
                                    newKey = block.yield(context, oldKey);
                                } catch (IRBreakJump e) {
                                    aborted = true;
                                    continue;
                                }
                            } else {
                                newKey = oldKey;
                            }
                        }
                    }
                }

                if (!newKeys.hasKey(oldKey)) fastDelete(oldKey);
                fastASet(newKey, pairs.eltOk(i + 1));
                newKeys.fastASet(newKey, null);
            }
        }

        return this;
    }

    @JRubyMethod(name = "transform_values!")
    public IRubyObject transform_values_bang(final ThreadContext context, final Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "transform_values!", RubyHashLinkedBuckets::size);

        testFrozen("Hash");
        iteratorVisitAll(context, (ctxt, self, key, value, index) -> self.op_aset(ctxt, key, block.yield(ctxt, value)));

        return this;
    }

    @JRubyMethod(name = "select!", alias = "filter!")
    public IRubyObject select_bang(final ThreadContext context, final Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "select!", RubyHashLinkedBuckets::size);

        return keep_ifCommon(context, block) ? this : context.nil;
    }

    @JRubyMethod
    public IRubyObject keep_if(final ThreadContext context, final Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "keep_if", RubyHashLinkedBuckets::size);

        keep_ifCommon(context, block);

        return this;
    }

    public boolean keep_ifCommon(final ThreadContext context, final Block block) {
        testFrozen("Hash");
        boolean[] modified = new boolean[] { false };
        iteratorVisitAll(context, (ctxt, self, key, value, index) -> {
            if (!block.yieldArray(ctxt, newArray(ctxt, key, value), null).isTrue()) {
                testFrozen();
                modified[0] = true;
                self.remove(key);
            }
        });
        return modified[0];
    }

    @JRubyMethod
    public IRubyObject key(ThreadContext context, IRubyObject expected) {
        IRubyObject key = internalIndex(context, expected);
        return key != null ? key : context.nil;
    }

    private IRubyObject internalIndex(final ThreadContext context, final IRubyObject expected) {
        try {
            visitAll(context, FoundKeyIfEqual, expected);
            return null;
        } catch (FoundKey found) {
            return found.key;
        }
    }

    private static final VisitorWithState<IRubyObject> FoundKeyIfEqual = new VisitorWithState<IRubyObject>() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, IRubyObject expected) {
            if (equalInternal(context, value, expected)) {
                throw new FoundKey(key);
            }
        }
    };

    /** rb_hash_keys
     *
     */

    @JRubyMethod(name = "keys")
    public RubyArray keys(final ThreadContext context) {
        try {
            RubyArray keys = RubyArrayNative.newBlankArrayInternal(context.runtime, size());

            visitAll(context, StoreKeyVisitor, keys);

            return keys;
        } catch (NegativeArraySizeException nase) {
            throw concurrentModification();
        }
    }

    public final RubyArray keys() {
        return keys(metaClass.runtime.getCurrentContext());
    }

    private static final VisitorWithState<RubyArray> StoreKeyVisitor = new VisitorWithState<RubyArray>() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, RubyArray keys) {
            keys.storeInternal(context, index, key);
        }
    };

    /** rb_hash_values
     *
     */

    @JRubyMethod(name = "values")
    public RubyArray values(final ThreadContext context) {
        try {
            RubyArray values = RubyArrayNative.newBlankArrayInternal(context.runtime, size());

            visitAll(context, StoreValueVisitor, values);

            return values;
        } catch (NegativeArraySizeException nase) {
            throw concurrentModification();
        }
    }

    public final RubyArray rb_values(ThreadContext context) {
        return values(context);
    }

    /** rb_hash_equal
     *
     */

    private static class Mismatch extends RuntimeException {
        public Throwable fillInStackTrace() {
            return this;
        }
    }
    private static final Mismatch MISMATCH = new Mismatch();

    /** rb_hash_shift
     *
     */
    @JRubyMethod(name = "shift")
    public IRubyObject shift(ThreadContext context) {
        modify();

        if (isEmpty()) return context.nil;

        RubyHashEntry entry = head.nextAdded;
        if (entry != head) {
            var result = newArray(context, entry.key, entry.value);
            internalDeleteEntry(entry);
            return result;
        }


        CachingCallSite self_default = sites(context).self_default;
        if (metaClass == context.runtime.getHash() && Builtins.checkHashDefault(context)) {
            return default_value_get(context, context.nil);
        }

        return self_default.call(context, this, this, context.nil);
    }

    public final boolean fastDelete(IRubyObject key) {
        return internalDelete(key) != NULL_ENTRY;
    }

    /** rb_hash_delete
     *
     */
    @JRubyMethod
    public IRubyObject delete(ThreadContext context, IRubyObject key, Block block) {
        modify();

        IRubyObject value = delete(key);
        if (value != null) return value;

        return block.isGiven() ? block.yield(context, key) : context.nil;
    }

    /**
     * Delete entry or null.
     */
    public IRubyObject delete(IRubyObject key) {
        RubyHashEntry entry = internalDelete(key);
        return entry != NULL_ENTRY ? entry.value : null;
    }

    public IRubyObject delete(ThreadContext context, IRubyObject key) {
        return delete(context, key, Block.NULL_BLOCK);
    }

    /** rb_hash_select
     *
     */
    @JRubyMethod(name = "select", alias = "filter")
    public IRubyObject select(final ThreadContext context, final Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "select", RubyHashLinkedBuckets::size);

        final RubyHashLinkedBuckets result = hashCopyWithIdentity(context);

        if (!isEmpty()) {
            result.keep_ifCommon(context, block);
        }

        return result;
    }


    /** rb_hash_slice
     *
     */
    @JRubyMethod(name = "slice", rest = true)
    public RubyHash slice(final ThreadContext context, final IRubyObject[] args) {
        RubyHash result = Create.newHash(context);
        result.setComparedByIdentity(isComparedByIdentity());

        for (int i = 0; i < args.length; i++) {
            IRubyObject key = args[i];
            IRubyObject value = internalGet(key);
            if (value != null) result.op_aset(context, key, value);
        }

        return result;
    }

    /** rb_hash_delete_if
     *
     */
    public RubyHashLinkedBuckets delete_ifInternal(ThreadContext context, Block block) {
        modify();

        iteratorVisitAll(context, DeleteIfVisitor, block);

        return this;
    }

    private static final VisitorWithState<Block> DeleteIfVisitor = new VisitorWithState<Block>() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, Block block) {
            if (block.yieldArray(context, newArray(context, key, value), null).isTrue()) {
                self.delete(context, key, Block.NULL_BLOCK);
            }
        }
    };

    @JRubyMethod
    public IRubyObject delete_if(final ThreadContext context, final Block block) {
        return block.isGiven() ? delete_ifInternal(context, block) : enumeratorizeWithSize(context, this, "delete_if", RubyHashLinkedBuckets::size);
    }

    /** rb_hash_reject
     *
     */
    public RubyHashLinkedBuckets rejectInternal(ThreadContext context, Block block) {
        final RubyHashLinkedBuckets result = hashCopyWithIdentity(context);

        if (!isEmpty()) {
            result.delete_ifInternal(context, block);
        }

        return result;
    }

    @JRubyMethod
    public IRubyObject reject(final ThreadContext context, final Block block) {
        return block.isGiven() ? rejectInternal(context, block) : enumeratorizeWithSize(context, this, "reject", RubyHashLinkedBuckets::size);
    }

    /** rb_hash_reject_bang
     *
     */
    public IRubyObject reject_bangInternal(ThreadContext context, Block block) {
        int n = size();
        delete_if(context, block);
        if (n == size()) return context.nil;
        return this;
    }

    @JRubyMethod(name = "reject!")
    public IRubyObject reject_bang(final ThreadContext context, final Block block) {
        return block.isGiven() ? reject_bangInternal(context, block) : enumeratorizeWithSize(context, this, "reject!", RubyHashLinkedBuckets::size);
    }

    /** rb_hash_clear
     *
     */
    @JRubyMethod(name = "clear")
    public RubyHash rb_clear(ThreadContext context) {
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
        final RubyHash result = Create.newHash(context);

        visitAll(context, InvertVisitor, result);

        return result;
    }

    private static final VisitorWithState<RubyHashLinkedBuckets> InvertVisitor = new VisitorWithState<RubyHashLinkedBuckets>() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, RubyHashLinkedBuckets state) {
            state.op_aset(context, value, key);
        }
    };

    /** rb_hash_update
     *
     */
    @JRubyMethod(name = {"merge!", "update"}, rest = true)
    public RubyHashLinkedBuckets merge_bang(ThreadContext context, IRubyObject[] others, Block block) {
        modify();

        if (others.length == 0) return this;

        for (int i = 0; i < others.length; i++) {
            final RubyHash otherHash = others[i].convertToHash();
            if (otherHash.empty_p(context).isTrue()) continue;
            otherHash.visitAll(context, (ctxt, self, key, value, index) -> {
                if (block.isGiven()) {
                    IRubyObject existing = internalGet(key);
                    if (existing != null) {
                        value = block.yield(ctxt, newArray(ctxt, key, existing, value));
                    }
                }
                op_aset(ctxt, key, value);
            });
        }

        return this;
    }

    public void addAll(ThreadContext context, RubyHash otherHash) {
        if (!otherHash.empty_p(context).isTrue()) {
            otherHash.visitAll(context, (ctxt, self, key, value, index) -> op_aset(ctxt, key, value));
        }
    }

    /** rb_hash_merge
     *
     */
    @JRubyMethod(rest = true)
    public RubyHash merge(ThreadContext context, IRubyObject[] others, Block block) {
        RubyHash dup = (RubyHash) dup(context);
        return dup.merge_bang(context, others, block);
    }

    @JRubyMethod(name = "initialize_copy", visibility = PRIVATE)
    public RubyHash initialize_copy(ThreadContext context, IRubyObject other) {
        return replace(context, other);
    }

    /** rb_hash_replace
     *
     */
    @JRubyMethod(name = "replace")
    public RubyHash replace(final ThreadContext context, IRubyObject other) {
        modify();

        final RubyHash otherHash = other.convertToHash();

        if (this == otherHash) return this;

        replaceWith(context, otherHash);

        ifNone = otherHash.getIfNone();

        if (otherHash.get(PROCDEFAULT_HASH)) {
            set(PROCDEFAULT_HASH, true);
        } else {
            set(PROCDEFAULT_HASH, false);
        }

        return this;
    }

    protected void replaceWith(ThreadContext context, RubyHash otherHash) {
        if (otherHash.getClass() == RubyHashLinkedBuckets.class) {
            alloc();
            copyFrom(this, otherHash, otherHash.isComparedByIdentity());
        } else {
            replaceExternally(context, otherHash);
        }
    }

    protected void replaceExternally(ThreadContext context, RubyHash otherHash) {
        rb_clear(context);

        if (!isComparedByIdentity() && otherHash.isComparedByIdentity()) {
            setComparedByIdentity(true);
        }

        otherHash.visitAll(context, ReplaceVisitor, this);
    }

    private static final VisitorWithState<RubyHash> ReplaceVisitor = new VisitorWithState<RubyHash>() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, RubyHash target) {
            target.op_aset(context, key, value);
        }
    };

    /** rb_hash_values_at
     *
     */
    @JRubyMethod(name = "values_at", rest = true)
    public RubyArray values_at(ThreadContext context, IRubyObject[] args) {
        RubyArray result = RubyArrayNative.newBlankArrayInternal(context.runtime, args.length);
        for (int i = 0; i < args.length; i++) {
            result.storeInternal(context, i, op_aref(context, args[i]));
        }
        return result;
    }

    @JRubyMethod(name = "fetch_values", rest = true)
    public RubyArray fetch_values(ThreadContext context, IRubyObject[] args, Block block) {
        RubyArray result = RubyArrayNative.newBlankArrayInternal(context.runtime, args.length);
        for (int i = 0; i < args.length; i++) {
            result.storeInternal(context, i, fetch(context, args[i], block));
        }
        return result;
    }

    @JRubyMethod(name = "assoc")
    public IRubyObject assoc(final ThreadContext context, final IRubyObject obj) {
        try {
            visitAll(context, FoundPairIfEqualKeyVisitor, obj);
            return context.nil;
        } catch (FoundPair found) {
            return newArray(context, found.key, found.value);
        }
    }

    @JRubyMethod(name = "rassoc")
    public IRubyObject rassoc(final ThreadContext context, final IRubyObject obj) {
        try {
            visitAll(context, FoundPairIfEqualValueVisitor, obj);
            return context.nil;
        } catch (FoundPair found) {
            return newArray(context, found.key, found.value);
        }
    }

    private static final VisitorWithState<IRubyObject> FoundPairIfEqualKeyVisitor = new VisitorWithState<IRubyObject>() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, IRubyObject obj) {
            if (equalInternal(context, obj, key)) {
                throw new FoundPair(key, value);
            }
        }
    };

    private static final VisitorWithState<IRubyObject> FoundPairIfEqualValueVisitor = new VisitorWithState<IRubyObject>() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, IRubyObject obj) {
            if (equalInternal(context, obj, value)) {
                throw new FoundPair(key, value);
            }
        }
    };

    @JRubyMethod
    public IRubyObject flatten(ThreadContext context) {
        RubyArray ary = to_a(context);
        sites(context).flatten_bang.call(context, ary, ary, RubyFixnum.one(context.runtime));
        return ary;
    }

    @JRubyMethod
    public IRubyObject flatten(ThreadContext context, IRubyObject level) {
        RubyArray ary = to_a(context);
        sites(context).flatten_bang.call(context, ary, ary, level);
        return ary;
    }

    @JRubyMethod(name = "compact")
    public IRubyObject compact(ThreadContext context) {
      IRubyObject res = dup();
      ((RubyHashLinkedBuckets)res).compact_bang(context);
      return res;
    }

    @JRubyMethod(name = "compact!")
    public IRubyObject compact_bang(ThreadContext context) {
      boolean changed = false;
      modify();
      iteratorEntry();
      try {
        for (RubyHashEntry entry = head.nextAdded; entry != head; entry = entry.nextAdded) {
          if (entry.value == context.nil) {
            internalDelete(entry.key);
            changed = true;
          }
        }
      } finally {
        iteratorExit();
      }
      return changed ? this : context.nil;
    }

    @JRubyMethod(name = "compare_by_identity")
    public IRubyObject compare_by_identity(ThreadContext context) {
        modify();
        setComparedByIdentity(true);
        return rehash(context);
    }

    @JRubyMethod(name = "compare_by_identity?")
    public IRubyObject compare_by_identity_p(ThreadContext context) {
        return asBoolean(context, isComparedByIdentity());
    }

    @JRubyMethod
    public IRubyObject dup(ThreadContext context) {
        RubyHashLinkedBuckets dup = (RubyHashLinkedBuckets) super.dup();
        dup.setComparedByIdentity(isComparedByIdentity());
        return dup;
    }

    public IRubyObject rbClone(ThreadContext context, IRubyObject opts) {
        RubyHash clone = (RubyHash) super.rbClone(context, opts);
        clone.setComparedByIdentity(isComparedByIdentity());
        return clone;
    }

    public IRubyObject rbClone(ThreadContext context) {
        return rbClone(context, context.nil);
    }

    @JRubyMethod(name = "any?")
    public IRubyObject any_p(ThreadContext context, Block block) {
        if (isEmpty()) return context.fals;
        if (!block.isGiven()) return context.tru;

        if (block.getSignature().arityValue() > 1) {
            return any_p_i_fast(context, block);
        }

        return any_p_i(context, block);
    }

    @JRubyMethod(name = "any?")
    public IRubyObject any_p(ThreadContext context, IRubyObject pattern, Block block) {
        if (isEmpty()) return context.fals;
        if (block.isGiven()) warn(context, "given block not used");

        return any_p_p(context, pattern);
    }

    protected IRubyObject any_p_i(ThreadContext context, Block block) {
        iteratorEntry();
        try {
            for (RubyHashEntry entry = head.nextAdded; entry != head; entry = entry.nextAdded) {
                IRubyObject newAssoc = newArray(context, entry.key, entry.value);
                if (block.yield(context, newAssoc).isTrue())
                    return context.tru;
            }
            return context.fals;
        } finally {
            iteratorExit();
        }
    }

    protected IRubyObject any_p_i_fast(ThreadContext context, Block block) {
        iteratorEntry();
        try {
            for (RubyHashEntry entry = head.nextAdded; entry != head; entry = entry.nextAdded) {
                if (block.yieldArray(context, newArray(context, entry.key, entry.value), null).isTrue()) return context.tru;
            }
            return context.fals;
        } finally {
            iteratorExit();
        }
    }

    protected IRubyObject any_p_p(ThreadContext context, IRubyObject pattern) {
        iteratorEntry();
        try {
            for (RubyHashEntry entry = head.nextAdded; entry != head; entry = entry.nextAdded) {
                IRubyObject newAssoc = newArray(context, entry.key, entry.value);
                if (pattern.callMethod(context, "===", newAssoc).isTrue()) return context.tru;
            }
            return context.fals;
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
    public RubyHashLinkedBuckets dupFast(final ThreadContext context) {
        final Ruby runtime = context.runtime;
        RubyHashLinkedBuckets dup = new RubyHashLinkedBuckets(runtime, metaClass, this);

        dup.setComparedByIdentity(this.isComparedByIdentity());

        dup.ifNone = this.ifNone;

        if (this.get(PROCDEFAULT_HASH)) {
            dup.set(PROCDEFAULT_HASH, true);
        } else {
            dup.set(PROCDEFAULT_HASH, false);
        }

        return dup;
    }

    public RubyHashLinkedBuckets withRuby2Keywords(boolean ruby2Keywords) {
        setRuby2KeywordHash(ruby2Keywords);
        return this;
    }

    public boolean hasDefaultProc() {
        return get(PROCDEFAULT_HASH);
    }

    public IRubyObject getIfNone(){
        return ifNone;
    }

    @JRubyMethod
    public IRubyObject deconstruct_keys(ThreadContext context, IRubyObject _arg1) {
        return this;
    }

    @JRubyMethod(name = "dig")
    public IRubyObject dig(ThreadContext context, IRubyObject arg0) {
        return op_aref( context, arg0 );
    }

    @JRubyMethod(name = "dig")
    public IRubyObject dig(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        final IRubyObject val = op_aref( context, arg0 );
        return RubyObject.dig1(context, val, arg1);
    }

    @JRubyMethod(name = "dig")
    public IRubyObject dig(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        final IRubyObject val = op_aref( context, arg0 );
        return RubyObject.dig2(context, val, arg1, arg2);
    }

    @JRubyMethod(name = "dig", required = 1, rest = true, checkArity = false)
    public IRubyObject dig(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 1, -1);

        final IRubyObject val = op_aref(context, args[0] );
        return argc == 1 ? val : RubyObject.dig(context, val, args, 1);
    }

    @JRubyMethod
    public IRubyObject to_proc(ThreadContext context) {
        Block block = CallBlock19.newCallClosure(
                this,
                this.metaClass,
                Signature.ONE_ARGUMENT,
                (context1, args, procBlock) -> {
                    Arity.checkArgumentCount(context1, args, 1, 1);
                    return op_aref(context1, args[0]);
                },
                context);

        return RubyProc.newProc(context.runtime, block, Block.Type.LAMBDA);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject ruby2_keywords_hash(ThreadContext context, IRubyObject _self, IRubyObject arg) {
        TypeConverter.checkType(context, arg, hashClass(context));

        RubyHashLinkedBuckets hash = (RubyHashLinkedBuckets) arg.dup();
        hash.setRuby2KeywordHash(true);

        return hash;
    }

    @JRubyMethod(meta = true, name = "ruby2_keywords_hash?")
    public static IRubyObject ruby2_keywords_hash_p(ThreadContext context, IRubyObject _self, IRubyObject arg) {
        TypeConverter.checkType(context, arg, hashClass(context));

        return asBoolean(context, ((RubyHashLinkedBuckets) arg).isRuby2KeywordHash());
    }

    public static void marshalTo(ThreadContext context, RubyOutputStream out, final RubyHashLinkedBuckets hash, final MarshalDumper output) {
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

        if (hash.ifNone != UNDEF) output.dumpObject(context, out, hash.ifNone);
    }

    public static RubyHash unmarshalFrom(ThreadContext context, RubyInputStream in, MarshalLoader input, boolean defaultValue, boolean identity) {
        RubyHash result = Create.newHash(context);
        if (identity) result.setComparedByIdentity(true);
        result = (RubyHashLinkedBuckets) input.entry(result);
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
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return internalGet(JavaUtil.convertJavaToUsableRubyObject(metaClass.runtime, key)) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        return hasValue(metaClass.runtime.getCurrentContext(), JavaUtil.convertJavaToUsableRubyObject(metaClass.runtime, value));
    }

    @Override
    public Object get(Object key) {
        IRubyObject gotten = internalGet(JavaUtil.convertJavaToUsableRubyObject(metaClass.runtime, key));
        return gotten == null ? null : gotten.toJava(Object.class);
    }

    @Override
    public Object put(Object key, Object value) {
        Ruby runtime = metaClass.runtime;
        IRubyObject existing = internalPutNoResize(JavaUtil.convertJavaToUsableRubyObject(runtime, key), JavaUtil.convertJavaToUsableRubyObject(runtime, value), true);
        return existing == null ? null : existing.toJava(Object.class);
    }

    @Override
    public Object remove(Object key) {
        IRubyObject rubyKey = JavaUtil.convertJavaToUsableRubyObject(metaClass.runtime, key);
        return internalDelete(rubyKey).value;
    }

    @Override
    public void putAll(Map map) {
        final Ruby runtime = metaClass.runtime;
        @SuppressWarnings("unchecked")
        final Iterator<Entry> iter = map.entrySet().iterator();
        while ( iter.hasNext() ) {
            Entry entry = iter.next();
            internalPut(
                JavaUtil.convertJavaToUsableRubyObject(runtime, entry.getKey()),
                JavaUtil.convertJavaToUsableRubyObject(runtime, entry.getValue())
            );
        }
    }

    @Override
    public void clear() {
        rb_clear(getRuntime().getCurrentContext());
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof RubyHashLinkedBuckets)) return false;
        if (this == other) return true;
        return op_equal(metaClass.runtime.getCurrentContext(), (RubyHashLinkedBuckets)other).isTrue();
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
        return metaClass.runtime.newConcurrencyError(
                "Detected invalid hash contents due to unsynchronized modifications with concurrent users");
    }

    /**
     * Is this object compared by identity or not?
     *
     * @return true if this object is compared by identity, false otherwise
     */
    public boolean isComparedByIdentity() {
        return get(COMPARE_BY_IDENTITY);
    }

    /**
     * Sets whether this object is compared by identity or not.
     *
     * @param comparedByIdentity should this object be compared by identity?
     */
    public void setComparedByIdentity(boolean comparedByIdentity) {
        set(COMPARE_BY_IDENTITY, comparedByIdentity);
    }

    public boolean isRuby2KeywordHash() {
        return get(RUBY2_KEYWORD);
    }

    public void setRuby2KeywordHash(boolean value) {
        set(RUBY2_KEYWORD, value);
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
            return view.contains(RubyHashLinkedBuckets.this, o);
        }

        @Override
        public void clear() {
            RubyHashLinkedBuckets.this.clear();
        }

        @Override
        public int size() {
            return RubyHashLinkedBuckets.this.size();
        }

        @Override
        public boolean remove(Object o) {
            return view.remove(RubyHashLinkedBuckets.this, o);
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
            return view.contains(RubyHashLinkedBuckets.this, o);
        }

        @Override
        public void clear() {
            RubyHashLinkedBuckets.this.clear();
        }

        @Override
        public int size() {
            return RubyHashLinkedBuckets.this.size();
        }

        @Override
        public boolean remove(Object o) {
            return view.remove(RubyHashLinkedBuckets.this, o);
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
        public abstract boolean contains(RubyHashLinkedBuckets hash, Object o);
        public abstract boolean remove(RubyHashLinkedBuckets hash, Object o);
    }

    private static final EntryView DIRECT_KEY_VIEW = new EntryView() {
        @Override
        public Object convertEntry(Ruby runtime, RubyHashEntry entry) {
            return entry.key;
        }
        @Override
        public boolean contains(RubyHashLinkedBuckets hash, Object o) {
            if (!(o instanceof IRubyObject)) return false;
            return hash.internalGet((IRubyObject)o) != null;
        }
        @Override
        public boolean remove(RubyHashLinkedBuckets hash, Object o) {
            if (!(o instanceof IRubyObject)) return false;
            return hash.internalDelete((IRubyObject)o) != NULL_ENTRY;
        }
    };

    private static final EntryView KEY_VIEW = new EntryView() {
        @Override
        public Object convertEntry(Ruby runtime, RubyHashEntry entry) {
            return entry.key.toJava(Object.class);
        }
        @Override
        public boolean contains(RubyHashLinkedBuckets hash, Object o) {
            return hash.containsKey(o);
        }
        @Override
        public boolean remove(RubyHashLinkedBuckets hash, Object o) {
            return hash.remove(o) != null;
        }
    };

    private static final EntryView DIRECT_VALUE_VIEW = new EntryView() {
        @Override
        public Object convertEntry(Ruby runtime, RubyHashEntry entry) {
            return entry.value;
        }
        @Override
        public boolean contains(RubyHashLinkedBuckets hash, Object o) {
            if (!(o instanceof IRubyObject)) return false;
            IRubyObject obj = (IRubyObject)o;
            return hash.hasValue(obj.getRuntime().getCurrentContext(), obj);
        }
        @Override
        public boolean remove(RubyHashLinkedBuckets hash, Object o) {
            if (!(o instanceof IRubyObject)) return false;
            IRubyObject obj = (IRubyObject) o;
            IRubyObject key = hash.internalIndex(obj.getRuntime().getCurrentContext(), obj);
            if (key == null) return false;
            return hash.internalDelete(key) != NULL_ENTRY;
        }
    };

    private static final EntryView VALUE_VIEW = new EntryView() {
        @Override
        public Object convertEntry(Ruby runtime, RubyHashEntry entry) {
            return entry.value.toJava(Object.class);
        }
        @Override
        public boolean contains(RubyHashLinkedBuckets hash, Object o) {
            return hash.containsValue(o);
        }
        @Override
        public boolean remove(RubyHashLinkedBuckets hash, Object o) {
            Ruby runtime = hash.metaClass.runtime;
            IRubyObject value = JavaUtil.convertJavaToUsableRubyObject(runtime, o);
            IRubyObject key = hash.internalIndex(runtime.getCurrentContext(), value);
            if (key == null) return false;
            return hash.internalDelete(key) != NULL_ENTRY;
        }
    };

    private static final EntryView DIRECT_ENTRY_VIEW = new EntryView() {
        @Override
        public Object convertEntry(Ruby runtime, RubyHashEntry entry) {
            return entry;
        }
        @Override
        public boolean contains(RubyHashLinkedBuckets hash, Object o) {
            if (!(o instanceof RubyHashEntry)) return false;
            RubyHashEntry entry = (RubyHashEntry)o;
            RubyHashEntry candidate = hash.internalGetEntry(entry.key);
            return candidate != NULL_ENTRY && entry.equals(candidate);
        }
        @Override
        public boolean remove(RubyHashLinkedBuckets hash, Object o) {
            if (!(o instanceof RubyHashEntry)) return false;
            return hash.internalDeleteEntry((RubyHashEntry)o) != NULL_ENTRY;
        }
    };

    private static final EntryView ENTRY_VIEW = new EntryView() {
        @Override
        public Object convertEntry(Ruby runtime, RubyHashEntry entry) {
            return new ConvertingEntry(runtime, entry);
        }
        @Override
        public boolean contains(RubyHashLinkedBuckets hash, Object o) {
            if (!(o instanceof ConvertingEntry)) return false;
            ConvertingEntry entry = (ConvertingEntry)o;
            RubyHashEntry candidate = hash.internalGetEntry(entry.entry.key);
            return candidate != NULL_ENTRY && entry.entry.equals(candidate);
        }
        @Override
        public boolean remove(RubyHashLinkedBuckets hash, Object o) {
            if (!(o instanceof ConvertingEntry)) return false;
            ConvertingEntry entry = (ConvertingEntry)o;
            return hash.internalDeleteEntry(entry.entry) != NULL_ENTRY;
        }
    };

    private static class ConvertingEntry implements Entry {
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

    private static HashSites sites(ThreadContext context) {
        return context.sites.Hash;
    }
}
