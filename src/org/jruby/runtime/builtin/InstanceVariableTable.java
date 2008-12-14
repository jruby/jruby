package org.jruby.runtime.builtin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jruby.RubyModule;

public final class InstanceVariableTable {
    public static final boolean USE_PACKED = true;
    private static final int MAX_PACKED = 5;

    // TODO: make it 16 now ?
    private static final int DEFAULT_CAPACITY = 8; // MUST be power of 2!
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final float LOAD_FACTOR = 0.75f;

    public static abstract class Visitor {
        public abstract void visit(String name, Object value);
    }

    public static abstract class TryLockVisitor {
        private Object object;
        public TryLockVisitor(Object object) {
            this.object = object;
        }
        public abstract void visit(String name, Object value);
    }

    /**
     * Every entry in the variable map is represented by an instance
     * of this class.
     */
    public static final class VariableTableEntry {
        public final int hash;
        public final String name;
        public volatile Object value;
        public final VariableTableEntry next;

        VariableTableEntry(int hash, String name, Object value, VariableTableEntry next) {
            assert name == name.intern() : name + " is not interned";
            this.hash = hash;
            this.name = name;
            this.value = value;
            this.next = next;
        }
    }

    /**
     * The variableTable contains variables for an object, defined as:
     * <ul>
     * <li> instance variables
     * <li> class variables (for classes/modules)
     * <li> internal variables (such as those used when marshaling RubyRange and RubyException)
     * </ul>
     *
     * Constants are stored separately, see {@link RubyModule}.
     *
     */
    private VariableTableEntry[] vTable;
    private Object[] packedVTable;
    private int vTableSize;
    private int vTableThreshold;

    public InstanceVariableTable(String name, Object value) {
        if (USE_PACKED) {
            // prefill ?
            packedVTable = new Object[MAX_PACKED * 2];
            assert name == name.intern() : name + " is not interned";
            packedVTable[0] = name;
            packedVTable[MAX_PACKED] = value;
        } else {
            vTable = new VariableTableEntry[DEFAULT_CAPACITY];
            int hash = name.hashCode();
            VariableTableEntry e = new VariableTableEntry(hash, name, value, null);
            vTable[hash & (DEFAULT_CAPACITY - 1)] = e;
            vTableThreshold = (int)(DEFAULT_CAPACITY * LOAD_FACTOR);
        }
        vTableSize = 1;
    }

    public InstanceVariableTable(List<Variable<IRubyObject>> vars) {
        if (USE_PACKED) {
            if (vars.size() > MAX_PACKED) {
                vTableThreshold = (int)(DEFAULT_CAPACITY * LOAD_FACTOR);
                vTable =  new VariableTableEntry[DEFAULT_CAPACITY];
                for (Variable<IRubyObject> var : vars) {
                    store(var.getName(), var.getValue());
                }
            } else {
                // prefill ?
                packedVTable = new Object[MAX_PACKED * 2];
                int i = 0;
                for (Variable<IRubyObject> var : vars) {
                    String name = var.getName();
                    assert name == name.intern() : name + " is not interned";
                    packedVTable[i] = name;
                    packedVTable[i + MAX_PACKED] = var.getValue();
                    i++;
                }
                vTableSize = vars.size();
            }
        } else {
            vTableThreshold = (int)(DEFAULT_CAPACITY * LOAD_FACTOR);
            vTable =  new VariableTableEntry[DEFAULT_CAPACITY];
            for (Variable<IRubyObject> var : vars) {
                store(var.getName(), var.getValue());
            }
        }
    }

    public int getSize() {
        return vTableSize;
    }

    public VariableTableEntry[] getVariableTable() {
        return vTable;
    }
    
    public Object[] getPackageTable() {
        return packedVTable;
    }
    
    public void visit(Visitor visitor) {
        if (USE_PACKED) {
            Object[]table = packedVTable; 
            if (table != null) {
                for (int i = 0; i < vTableSize; i++) {
                    visitor.visit((String)table[i], table[i + MAX_PACKED]);
                }
                return;
            }
        }
        VariableTableEntry[] table = vTable;
        for (int i = table.length; --i >= 0; ) {
            for (VariableTableEntry e = table[i]; e != null; e = e.next) {
                visitor.visit(e.name, e.value);
            }
        }
    }

    public void visit(TryLockVisitor visitor) {
        if (USE_PACKED) {
            Object[]table = packedVTable; 
            if (table != null) {
                for (int i = 0; i < vTableSize; i++) {
                    visitor.visit((String)table[i], table[i + MAX_PACKED]);
                }
                return;
            }
        }
        VariableTableEntry[] table = vTable;
        for (int i = table.length; --i >= 0; ) {
            for (VariableTableEntry e = table[i]; e != null; e = e.next) {
                Object readValue = e.value;
                if (readValue == null) {
                    synchronized (visitor.object) {
                        readValue = e.value;
                    }
                }
                visitor.visit(e.name, readValue);               
            }
        }
    }

    private void unpack() {
        VariableTableEntry[]table =  new VariableTableEntry[DEFAULT_CAPACITY];
        Object[]packed = packedVTable;
        for (int i = 0; i < MAX_PACKED; i++) {
            String name = (String)packed[i];
            int index;
            int hash = name.hashCode();

            VariableTableEntry e;
            for (e = table[index = hash & (table.length - 1)]; e != null; e = e.next); 
            e = new VariableTableEntry(hash, name, packed[i + MAX_PACKED], table[index]);
            table[index] = e;
        }
        packedVTable = null;
        vTableThreshold = (int)(DEFAULT_CAPACITY * LOAD_FACTOR);
        vTable = table;
    }

    /**
     * Rehashes the variable table. Must be called from a synchronized
     * block.
     */
    // MUST be called from synchronized/locked block!
    // should only be called by variableTableStore/variableTableFastStore
    private VariableTableEntry[] rehash() {
        VariableTableEntry[] oldTable = vTable;
        int oldCapacity;
        if ((oldCapacity = oldTable.length) >= MAXIMUM_CAPACITY) {
            return oldTable;
        }

        int newCapacity = oldCapacity << 1;
        VariableTableEntry[] newTable = new VariableTableEntry[newCapacity];
        vTableThreshold = (int)(newCapacity * LOAD_FACTOR);
        int sizeMask = newCapacity - 1;
        VariableTableEntry e;
        for (int i = oldCapacity; --i >= 0; ) {
            // We need to guarantee that any existing reads of old Map can
            //  proceed. So we cannot yet null out each bin.
            e = oldTable[i];

            if (e != null) {
                VariableTableEntry next = e.next;
                int idx = e.hash & sizeMask;

                //  Single node on list
                if (next == null)
                    newTable[idx] = e;

                else {
                    // Reuse trailing consecutive sequence at same slot
                    VariableTableEntry lastRun = e;
                    int lastIdx = idx;
                    for (VariableTableEntry last = next;
                         last != null;
                         last = last.next) {
                        int k = last.hash & sizeMask;
                        if (k != lastIdx) {
                            lastIdx = k;
                            lastRun = last;
                        }
                    }
                    newTable[lastIdx] = lastRun;

                    // Clone all remaining nodes
                    for (VariableTableEntry p = e; p != lastRun; p = p.next) {
                        int k = p.hash & sizeMask;
                        VariableTableEntry m = new VariableTableEntry(p.hash, p.name, p.value, newTable[k]);
                        newTable[k] = m;
                    }
                }
            }
        }
        vTable = newTable;
        return newTable;
    }

    public Object store(String name, Object value) {
        if (USE_PACKED) {
            if (packedVTable != null) return packedStore(name, value); 
        }
        return hashStore(name, value);
    }

    private Object packedStore(String name, Object value) {
        Object[]table = packedVTable;
        int i = 0;
        for (i = 0; i < vTableSize; i++) {
            if (table[i].equals(name)) {
                table[i + MAX_PACKED] = value;
                return value;
            }
        }

        if (i == MAX_PACKED) {
            unpack();
            return hashStore(name, value);
        }

        table[i] = name.intern();
        table[i + MAX_PACKED] = value;
        vTableSize++;
        return value;
    }

    private Object hashStore(String name, Object value) {
        VariableTableEntry[] table = vTableSize + 1 > vTableThreshold ? rehash() : vTable;

        int index;
        int hash = name.hashCode();
        VariableTableEntry e;
        for (e = table[index = hash & (table.length - 1)]; e != null; e = e.next) {
            if (hash == e.hash && name.equals(e.name)) {
                e.value = value;
                return value;
            }
        }
        e = new VariableTableEntry(hash, name.intern(), value, table[index]);
        table[index] = e;
        vTableSize++;
        vTable = table; // write-volatile
        return value;
    }

    public Object fastStore(String internedName, Object value) {
        if (USE_PACKED) {
            if (packedVTable != null) return fastPackedStore(internedName, value); 
        }
        return fastHashStore(internedName, value);
    }

    private Object fastPackedStore(String internedName, Object value) {
        Object[]table = packedVTable;
        int i;
        for (i = 0; i < vTableSize; i++) {
            if (table[i] == internedName) {
                table[i + MAX_PACKED] = value;
                return value;
            }
        }

        if (i == MAX_PACKED) {
            unpack();
            return fastHashStore(internedName, value);
        }

        assert internedName == internedName.intern() : internedName + " is not interned";
        table[i] = internedName;
        table[i + MAX_PACKED] = value;
        vTableSize++;

        return value;
    }

    private Object fastHashStore(String internedName, Object value) {
        VariableTableEntry[] table = vTableSize + 1 > vTableThreshold ? rehash() : vTable;

        int index;
        int hash = internedName.hashCode();
        VariableTableEntry e;
        for (e = table[index = hash & (table.length - 1)]; e != null; e = e.next) {
            if (internedName == e.name) {
                e.value = value;
                return value;
            }
        }
        e = new VariableTableEntry(hash, internedName, value, table[index]);
        table[index] = e;
        vTableSize++;
        vTable = table; // write-volatile
        return value;
    }
    
    public Object remove(String name) {
        if (USE_PACKED) {
            if (packedVTable != null) return packedRemove(name);
        }
        return hashRemove(name);
    }

    private Object packedRemove(String name) {
        Object[]table = packedVTable;
        int i = 0;
        for (i = 0; i < vTableSize; i++) {
            if (table[i].equals(name)) {
                Object value = table[i + MAX_PACKED];
                for (int j = i; j < vTableSize - 1; j++) {
                    table[j] = table[j + 1];
                    table[j + MAX_PACKED] = table[j + 1 + MAX_PACKED];
                }
                vTableSize--;
                return value;
            }
        }
        return null;
    }

    private Object hashRemove(String name) {
        VariableTableEntry[] table = vTable;
        int hash = name.hashCode();
        int index = hash & (table.length - 1);
        VariableTableEntry first = table[index];
        for (VariableTableEntry e = first; e != null; e = e.next) {
            if (hash == e.hash && name.equals(e.name)) {
                Object oldValue = e.value;
                // All entries following removed node can stay
                // in list, but all preceding ones need to be
                // cloned.
                VariableTableEntry newFirst = e.next;
                for (VariableTableEntry p = first; p != e; p = p.next) {
                    newFirst = new VariableTableEntry(p.hash, p.name, p.value, newFirst);
                }
                table[index] = newFirst;
                vTableSize--;
                vTable = table; // write-volatile
                return oldValue;
            }
        }
        return null;
    }

    public void sync(List<Variable<IRubyObject>> vars) {
        vTableSize = 0;
        vTableThreshold = (int)(DEFAULT_CAPACITY * LOAD_FACTOR);
        vTable =  new VariableTableEntry[DEFAULT_CAPACITY];
        for (Variable<IRubyObject> var : vars) {
            store(var.getName(), var.getValue());
        }
    }

    /**
     * Reads the value of the specified entry, locked on the current
     * object.
     */
    protected synchronized Object readLocked(VariableTableEntry entry) {
        return entry.value;
    }

    public Map getMap(IRubyObject object) {
        return getMap(object, new HashMap());
    }

    @SuppressWarnings("unchecked")
    public Map getMap(Object object, final Map map) {
        visit(new TryLockVisitor(object) {
            public void visit(String name, Object value) {
                map.put(name, value);
            }
        });
        return map;
    }

    public boolean contains(String name) {
        if (USE_PACKED) {
            if (packedVTable != null) return packedContains(name);
        }
        return hashContains(name);
    }

    private boolean packedContains(String name) {
        Object[]table = packedVTable;
        for (int i = 0; i < vTableSize; i++) {
            if (table[i].equals(name)) return true;
        }
        return false;
    }

    private boolean hashContains(String name) {
        VariableTableEntry[] table = vTable;
        int hash = name.hashCode();
        for (VariableTableEntry e = table[hash & (table.length - 1)]; e != null; e = e.next) {
            if (hash == e.hash && name.equals(e.name)) {
                return true;
            }
        }
        return false;
    }

    public boolean fastContains(String name) {
        if (USE_PACKED) {
            if (packedVTable != null) return fastPackedContains(name);
        }
        return fastHashContains(name);
    }

    private boolean fastPackedContains(String name) {
        Object[]table = packedVTable;
        for (int i = 0; i < vTableSize; i++) {
            if (table[i] == name) return true;
        }
        return false;
    }
    
    private boolean fastHashContains(String internedName) {
        VariableTableEntry[] table = vTable;
        for (VariableTableEntry e = table[internedName.hashCode() & (table.length - 1)]; e != null; e = e.next) {
            if (internedName == e.name) {
                return true;
            }
        }
        return false;

    }

    public Object fetch(String name) {
        if (USE_PACKED) {
            if (packedVTable != null) return packedFetch(name);
        }
        return hashFetch(name);
    }

    private Object packedFetch(String name) {
        Object[]table = packedVTable;
        for (int i = 0; i < vTableSize; i++) {
            if (table[i].equals(name)) return table[i + MAX_PACKED];
        }
        return null;
    }

    private Object hashFetch(String name) {
        VariableTableEntry[] table = vTable;
        Object readValue;
        int hash = name.hashCode();
        for (VariableTableEntry e = table[hash & (table.length - 1)]; e != null; e = e.next) {
            if (hash == e.hash && name.equals(e.name)) {
                if ((readValue = e.value) != null) return readValue;
                return readLocked(e);
            }
        }
        return null;
    }

    public Object fastFetch(String name) {
        if (USE_PACKED) {
            if (packedVTable != null) return fastPackedFetch(name);
        }
        return fastHashFetch(name);
    }

    private Object fastPackedFetch(String name) {
        Object[]table = packedVTable;
        for (int i = 0; i < vTableSize; i++) {
            if (table[i] == name) return table[i + MAX_PACKED];
        }
        return null;
    }

    private Object fastHashFetch(String internedName) {
        VariableTableEntry[] table = vTable;
        Object readValue;
        for (VariableTableEntry e = table[internedName.hashCode() & (table.length - 1)]; e != null; e = e.next) {
            if (internedName == e.name) {
                if ((readValue = e.value) != null) return readValue;
                return readLocked(e);
            }
        }
        return null;
    }
}
