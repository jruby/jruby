package org.jruby.util.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SlottedHashMap implements Map {
    private SlottedHashMap parent;
    private Map map;
    private List children;
    private Object owner;
    
    public class Slot {
        private Object value;

        public void setValue(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
        
        public SlottedHashMap getParent() {
            return SlottedHashMap.this;
        }
        
        public String toString() {
            return "Slot#" + hashCode() + "[parent = " + getParent().hashCode() + ", value = " + getValue() + "]";
        }
    }
    
    public SlottedHashMap(Object owner) {
        super();
        children = new ArrayList();
        map = new HashMap();
        this.owner = owner;
    }
    
    public SlottedHashMap(Object owner, SlottedHashMap smap) {
        this(owner);
        parent = smap;
        smap.addChild(this);
        
        for (Iterator iter = parent.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry)iter.next();
            
            map.put(entry.getKey(), entry.getValue());
        }
    }
    
    public void addChild(SlottedHashMap smap) {
        children.add(smap);
    }
    
    public void setParent(SlottedHashMap smap) {
        this.parent = smap;
    }

    /**
     * Put a new value into this SHM. If the key provided was originally slotted by a parent,
     * that association will be replaced. If no parents have provided an association we create a new one.
     * @param key
     * @param value
     * @return
     */
    public Object put(Object key, Object value) {
        Slot slot = (Slot)map.get(key);
        if (slot == null || slot.getParent() != this) {
            slot = new Slot();
        }
        
        slot.setValue(value);
        
        // update all children
        for (Iterator iter = children.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            SlottedHashMap shm = (SlottedHashMap)obj;
            
            shm.put(key, value, this);
        }
        
        return map.put(key, slot);
    }
    
    /**
     * Potentially insert the given key and value, if we have not already overridden it
     * 
     * @param key
     * @param value
     * @param parent
     * @return
     */
    protected Object put(Object key, Object value, SlottedHashMap parent) {
        Slot slot = getSlot(key);
        if (slot != null) {
            if (slot.getParent() == this) {
                // do not replace, we've overridden
                return get(key);
            }
        }

        slot = parent.getSlot(key);
        
        // update children
        for (Iterator iter = children.iterator(); iter.hasNext();) {
            SlottedHashMap shm = (SlottedHashMap)iter.next();
            shm.put(key, value, parent);
        }
        
        return map.put(key, slot);
    }

    public Object get(Object key) {
        Slot slot = getSlot(key);
        
        if (slot == null) return null;
        
        return slot.getValue();
    }
    
    public Slot getSlot(Object key) {
        return (Slot)map.get(key);
    }

    public int size() {
        return map.size();
    }

    public void clear() {
        throw new RuntimeException("clear not implemented");
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public boolean containsValue(Object value) {
        // TODO Auto-generated method stub
        return false;
    }

    public Collection values() {
        // TODO Auto-generated method stub
        return null;
    }

    public void putAll(Map t) {
        // TODO Auto-generated method stub
        
    }

    public Set entrySet() {
        return map.entrySet();
    }

    public Set keySet() {
        return map.keySet();
    }

    public Object remove(Object key) {
        Slot slot = getSlot(key);
        
        if (slot != null) {
            if (slot.getParent() == this) {
                map.remove(key);
                
                // update children
                for (Iterator iter = children.iterator(); iter.hasNext();) {
                    SlottedHashMap child = (SlottedHashMap)iter.next();
                    
                    child.remove(key, this);
                }
                
                return slot.getValue();
            } else {
                // do nothing, we have not overridden this
            }
        }
        
        return null;
    }
    
    protected void remove(Object key, SlottedHashMap parent) {
        Slot slot = getSlot(key);
        
        if (slot.getParent() == this) {
            // do nothing, we've overridden parent
            return;
        } else {
            map.remove(key);
        }
        
        // update children
        for (Iterator iter = children.iterator(); iter.hasNext();) {
            SlottedHashMap child = (SlottedHashMap)iter.next();
            
            child.remove(key, parent);
        }
    }

    public Object getOwner() {
        return owner;
    }
}
