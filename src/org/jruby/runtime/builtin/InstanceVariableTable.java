package org.jruby.runtime.builtin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jruby.RubyModule;

public final class InstanceVariableTable {
    private static final boolean USE_PACKED_FIELDS = true;
    private static final boolean USE_PACKED_ARRAY = !USE_PACKED_FIELDS;    
    private static final int MAX_PACKED = 5;

    // TODO: make it 16 now ?
    private static final int DEFAULT_CAPACITY = 8; // MUST be power of 2!
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final float LOAD_FACTOR = 0.75f;

    public static abstract class Visitor {
        public abstract void visit(String name, Object value);
    }

    public static abstract class TryLockVisitor extends Visitor {
        private Object object;
        public TryLockVisitor(Object object) {
            this.object = object;
        }
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

    private final class PackedFields {
        String name1, name2, name3, name4, name5;
        Object value1, value2, value3, value4, value5;

        PackedFields() {}
        PackedFields(String internedName, Object value) {
            name1 = internedName;
            value1 = value;
        }

        Object fastStore(String internedName, Object value) {
            assert internedName == internedName.intern() : internedName + " is not interned";
            if (internedName == name1) return value1 = value;
            if (internedName == name2) return value2 = value;
            if (internedName == name3) return value3 = value;
            if (internedName == name4) return value4 = value;
            if (internedName == name5) return value5 = value;
            return insert(internedName, value);
        }

        Object store(String name, Object value) {
            int hash = name.hashCode();
            if (name1 != null && name1.hashCode() == hash && name.equals(name1)) return value1 = value;
            if (name2 != null && name2.hashCode() == hash && name.equals(name2)) return value2 = value;
            if (name3 != null && name3.hashCode() == hash && name.equals(name3)) return value3 = value;
            if (name4 != null && name4.hashCode() == hash && name.equals(name4)) return value4 = value;
            if (name5 != null && name5.hashCode() == hash && name.equals(name5)) return value5 = value;
            return insert(name.intern(), value);
        }

        void unpack() {
            VariableTableEntry[]table =  new VariableTableEntry[DEFAULT_CAPACITY];
            unpackOne(table, name1, value1);
            unpackOne(table, name2, value2);
            unpackOne(table, name3, value3);
            unpackOne(table, name4, value4);
            unpackOne(table, name5, value5);
            packedVFields = null;
            vTableThreshold = (int)(DEFAULT_CAPACITY * LOAD_FACTOR);
            vTable = table;
        }

        void unpackOne(VariableTableEntry[]table, String name, Object value) {
            int index;
            int hash = name.hashCode();

            VariableTableEntry e;
            for (e = table[index = hash & (table.length - 1)]; e != null; e = e.next); 
            e = new VariableTableEntry(hash, name, value, table[index]);
            table[index] = e;
        }

        Object insert(String internedName, Object value) {
            switch (vTableSize) {
            case 0: name1 = internedName; value1 = value; break;
            case 1: name2 = internedName; value2 = value; break;
            case 2: name3 = internedName; value3 = value; break;
            case 3: name4 = internedName; value4 = value; break;
            case 4: name5 = internedName; value5 = value; break;
            case 5:
                unpack();
                return fastHashStore(internedName, value);
            }
            vTableSize++;
            return value;
        }

        boolean contains(String name) {
            int hash = name.hashCode();
            return 
                name1 != null && name1.hashCode() == hash && name.equals(name1) || 
                name2 != null && name2.hashCode() == hash && name.equals(name2) ||
                name3 != null && name3.hashCode() == hash && name.equals(name3) ||
                name4 != null && name4.hashCode() == hash && name.equals(name4) ||
                name5 != null && name5.hashCode() == hash && name.equals(name5);
        }

        boolean fastContains(String name) {
            return 
                name == name1 ||
                name == name2 ||
                name == name3 ||
                name == name4 ||
                name == name5;
        }
        
        Object fetch(String name) {
            int hash = name.hashCode();
            if (name1 != null && name1.hashCode() == hash && name.equals(name1)) return value1;
            if (name2 != null && name2.hashCode() == hash && name.equals(name2)) return value2;
            if (name3 != null && name3.hashCode() == hash && name.equals(name3)) return value3;
            if (name4 != null && name4.hashCode() == hash && name.equals(name4)) return value4;
            if (name5 != null && name5.hashCode() == hash && name.equals(name5)) return value5;
            return null;
        }
        
        Object fastFetch(String name) {
            if (name == name1) return value1;
            if (name == name2) return value2;
            if (name == name3) return value3;
            if (name == name4) return value4;
            if (name == name5) return value5;
            return null;
        }
        
        Object remove(String name) {
            int hash = name.hashCode();
            if (name1 != null && name1.hashCode() == hash && name.equals(name1)) return remove(1, value1);
            if (name2 != null && name2.hashCode() == hash && name.equals(name2)) return remove(2, value2);
            if (name3 != null && name3.hashCode() == hash && name.equals(name3)) return remove(3, value3);
            if (name4 != null && name4.hashCode() == hash && name.equals(name4)) return remove(4, value4);
            if (name5 != null && name5.hashCode() == hash && name.equals(name5)) return remove(5, value5);
            return null;
        }

        Object remove(int num, Object value) {
            switch (num) {
            case 1: name1 = name2; value1 = value2;
            case 2: name2 = name3; value2 = value3;
            case 3: name3 = name4; value3 = value4;
            case 4: name4 = name5; value4 = value5;
            case 5: name5 = null; value5 = null;
            }
            vTableSize--;
            return value;

        }
        
        void visit(Visitor visitor) {
            if (name1 != null) visitor.visit(name1, value1);
            if (name2 != null) visitor.visit(name2, value2);
            if (name3 != null) visitor.visit(name3, value3);
            if (name4 != null) visitor.visit(name4, value4);
            if (name5 != null) visitor.visit(name5, value5);
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
    private PackedFields packedVFields;
    private int vTableSize;
    private int vTableThreshold;

    public InstanceVariableTable(String name, Object value) {
        if (USE_PACKED_ARRAY) {
            // prefill ?
            packedVTable = new Object[MAX_PACKED * 2];
            assert name == name.intern() : name + " is not interned";
            packedVTable[0] = name;
            packedVTable[MAX_PACKED] = value;
        } else if (USE_PACKED_FIELDS) { 
            packedVFields = new PackedFields(name, value);
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
        sync(vars);
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
        if (USE_PACKED_ARRAY) {
            Object[]table = packedVTable; 
            if (table != null) {
                for (int i = 0; i < vTableSize; i++) {
                    visitor.visit((String)table[i], table[i + MAX_PACKED]);
                }
                return;
            }
        } else if (USE_PACKED_FIELDS) {
            if (packedVFields != null) {
                packedVFields.visit(visitor);
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
        if (USE_PACKED_ARRAY) {
            Object[]table = packedVTable; 
            if (table != null) {
                for (int i = 0; i < vTableSize; i++) {
                    visitor.visit((String)table[i], table[i + MAX_PACKED]);
                }
                return;
            }
        } else if (USE_PACKED_FIELDS) {
            if (packedVFields != null) {
                packedVFields.visit(visitor);
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
        if (USE_PACKED_ARRAY) {
            if (packedVTable != null) return packedStore(name, value); 
        } else if (USE_PACKED_FIELDS) {
            if (packedVFields != null) return packedVFields.store(name, value);
        }
        return hashStore(name, value);
    }

    private Object packedInsert(Object[]table, int index, String internedName, Object value) {
        assert internedName == internedName.intern() : internedName + " is not interned";
        if (index == MAX_PACKED) {
            unpack();
            return hashStore(internedName, value);
        }

        table[index] = internedName;
        table[index + MAX_PACKED] = value;
        vTableSize++;
        return value;
    }

    private Object packedStore(String name, Object value) {
        Object[]table = packedVTable;
        int hash = name.hashCode();
        int i = 0;
        for (i = 0; i < vTableSize; i++) {
            String n = (String)table[i];
            if (n.hashCode() == hash && name.equals(n)) {
                table[i + MAX_PACKED] = value;
                return value;
            }
        }
        return packedInsert(table, i, name.intern(), value);
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
        if (USE_PACKED_ARRAY) {
            if (packedVTable != null) return fastPackedStore(internedName, value); 
        } else if (USE_PACKED_FIELDS) {
            if (packedVFields != null) return packedVFields.fastStore(internedName, value);
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
        return packedInsert(table, i, internedName, value);
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
        if (USE_PACKED_ARRAY) {
            if (packedVTable != null) return packedRemove(name);
        } else if (USE_PACKED_FIELDS) {
            if (packedVFields != null) return packedVFields.remove(name);
        }
        return hashRemove(name);
    }

    private Object packedRemove(String name) {
        Object[]table = packedVTable;
        int hash = name.hashCode();
        int i = 0;
        for (i = 0; i < vTableSize; i++) {
            String n = (String)table[i];
            if (n.hashCode() == hash && name.equals(n)) {
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
        if (vars.size() <= MAX_PACKED && (USE_PACKED_ARRAY || USE_PACKED_FIELDS)) {
            if (USE_PACKED_ARRAY) {
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
            } else if (USE_PACKED_FIELDS) {
                packedVFields = new PackedFields();
                for (Variable<IRubyObject> var : vars) {
                    String name = var.getName();
                    assert name == name.intern() : name + " is not interned";
                    packedVFields.insert(name, var.getValue());
                }
            }
        } else {
            vTableThreshold = (int)(DEFAULT_CAPACITY * LOAD_FACTOR);
            vTable =  new VariableTableEntry[DEFAULT_CAPACITY];
            for (Variable<IRubyObject> var : vars) {
                store(var.getName(), var.getValue());
            }
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
        if (USE_PACKED_ARRAY) {
            if (packedVTable != null) return packedContains(name);
        } else if (USE_PACKED_FIELDS) {
            if (packedVFields != null) return packedVFields.contains(name);
        }
        return hashContains(name);
    }

    private boolean packedContains(String name) {
        Object[]table = packedVTable;
        int hash = name.hashCode();
        for (int i = 0; i < vTableSize; i++) {
            String n = (String)table[i];
            if (n.hashCode() == hash && name.equals(n)) return true;
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
        if (USE_PACKED_ARRAY) {
            if (packedVTable != null) return fastPackedContains(name);
        } else if (USE_PACKED_FIELDS) {
            if (packedVFields != null) return packedVFields.fastContains(name);
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
        if (USE_PACKED_ARRAY) {
            if (packedVTable != null) return packedFetch(name);
        } else if (USE_PACKED_FIELDS) {
            if (packedVFields != null) return packedVFields.fetch(name);
        }
        return hashFetch(name);
    }

    private Object packedFetch(String name) {
        Object[]table = packedVTable;
        int hash = name.hashCode();
        for (int i = 0; i < vTableSize; i++) {
            String n = (String)table[i];
            if (n.hashCode() == hash && name.equals(n)) return table[i + MAX_PACKED];
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
        if (USE_PACKED_ARRAY) {
            if (packedVTable != null) return fastPackedFetch(name);
        } else if (USE_PACKED_FIELDS) {
            if (packedVFields != null) return packedVFields.fastFetch(name);
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
