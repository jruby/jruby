package org.jruby;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jruby.runtime.ObjectSpace;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ivars.StampedVariableAccessor;
import org.jruby.runtime.ivars.SynchronizedVariableAccessor;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.runtime.ivars.VariableAccessorField;
import org.jruby.util.unsafe.UnsafeHolder;

public class VariableTableManager {
    private final RubyClass realClass;
    
    private static String[] EMPTY_STRING_ARRAY = new String[0];
    private Map<String, VariableAccessor> variableAccessors = (Map<String, VariableAccessor>)Collections.EMPTY_MAP;
    private volatile String[] variableNames = EMPTY_STRING_ARRAY;

    private volatile boolean hasObjectID = false;
    
    private final VariableAccessorField objectIdVariableAccessorField = new VariableAccessorField("object_id");
    private final VariableAccessorField cextHandleVariableAccessorField = new VariableAccessorField("cext");
    private final VariableAccessorField ffiHandleVariableAccessorField = new VariableAccessorField("ffi");
    private final VariableAccessorField objectGroupVariableAccessorField = new VariableAccessorField("objectspace_group");
    
    public VariableTableManager(RubyClass realClass) {
        this.realClass = realClass;
    }

    public Map<String, VariableAccessor> getVariableAccessorsForRead() {
        return variableAccessors;
    }
    
    public boolean hasObjectID() {
        return hasObjectID;
    }
    
    public long getObjectId(RubyBasicObject self) {
        VariableAccessor objectIdAccessor = getObjectIdAccessorField().getVariableAccessorForRead();
        Long id = (Long)objectIdAccessor.get(self);
        if (id != null) return id;
        
        synchronized (self) {
            objectIdAccessor = getObjectIdAccessorField().getVariableAccessorForRead();
            id = (Long)objectIdAccessor.get(self);
            if (id != null) return id;

            return initObjectId(self, getObjectIdAccessorField().getVariableAccessorForWrite(this, realClass.id));
        }
    }

    /**
     * We lazily stand up the object ID since it forces us to stand up
     * per-object state for a given object. We also check for ObjectSpace here,
     * and normally we do not register a given object ID into ObjectSpace due
     * to the high cost associated with constructing the related weakref. Most
     * uses of id/object_id will only ever need it to be a unique identifier,
     * and the id2ref behavior provided by ObjectSpace is considered internal
     * and not generally supported.
     * 
     * @param objectIdAccessor The variable accessor to use for storing the
     * generated object ID
     * @return The generated object ID
     */
    protected synchronized long initObjectId(RubyBasicObject self, VariableAccessor objectIdAccessor) {
        Ruby runtime = self.getRuntime();
        long id;
        
        if (runtime.isObjectSpaceEnabled()) {
            id = runtime.getObjectSpace().createAndRegisterObjectId(self);
        } else {
            id = ObjectSpace.calculateObjectId(self);
        }
        
        // we use a direct path here to avoid frozen checks
        setObjectId(self, objectIdAccessor.getIndex(), id);

        return id;
    }

    private void setObjectId(RubyBasicObject self, int index, long value) {
        if (index < 0) return;
        setVariableInternal(realClass, self, index, value);
    }
    
    public void setVariableInternal(RubyBasicObject self, int index, Object value) {
        if(UnsafeHolder.U == null) {
            SynchronizedVariableAccessor.setVariable(self,realClass,index,value);
        } else {
            StampedVariableAccessor.setVariable(self,realClass,index,value);
        }
    }
    
    public static void setVariableInternal(RubyClass realClass, RubyBasicObject self, int index, Object value) {
        if(UnsafeHolder.U == null) {
            SynchronizedVariableAccessor.setVariable(self,realClass,index,value);
        } else {
            StampedVariableAccessor.setVariable(self,realClass,index,value);
        }
    }
    
    /**
     * Sync one this object's variables with other's - this is used to make
     * rbClone work correctly.
     */
    public void syncVariables(RubyBasicObject self, IRubyObject other) {
        RubyClass otherRealClass = other.getMetaClass().getRealClass();
        boolean sameTable = otherRealClass == realClass;

        if (sameTable) {
            int idIndex = otherRealClass.getObjectIdAccessorField().getVariableAccessorForRead().getIndex();
            
            Object[] otherVars = ((RubyBasicObject) other).varTable;
            
            if(UnsafeHolder.U == null)
            {
                synchronized (self) {
                    self.varTable = makeSyncedTable(self.varTable, otherVars, idIndex);
                }
            } else {
                for(;;) {
                    int oldStamp = self.varTableStamp;
                    // wait for read mode
                    if((oldStamp & 0x01) == 1)
                        continue;
                    // acquire exclusive write mode
                    if(!UnsafeHolder.U.compareAndSwapInt(self, RubyBasicObject.STAMP_OFFSET, oldStamp, ++oldStamp))
                        continue;
                    
                    Object[] currentTable = (Object[]) UnsafeHolder.U.getObjectVolatile(self, RubyBasicObject.VAR_TABLE_OFFSET);
                    Object[] newTable = makeSyncedTable(currentTable,otherVars, idIndex);
                    
                    UnsafeHolder.U.putOrderedObject(self, RubyBasicObject.VAR_TABLE_OFFSET, newTable);
                    
                    // release write mode
                    self.varTableStamp = oldStamp+1;
                    break;
                }

                
            }

        } else {
            for (Map.Entry<String, VariableAccessor> entry : otherRealClass.getVariableAccessorsForRead().entrySet()) {
                VariableAccessor accessor = entry.getValue();
                Object value = accessor.get(other);

                if (value != null) {
                    if (sameTable) {
                        accessor.set(self, value);
                    } else {
                        realClass.getVariableAccessorForWrite(accessor.getName()).set(self, value);
                    }
                }
            }
        }
    }

    private static Object[] makeSyncedTable(Object[] currentTable, Object[] otherTable, int objectIdIdx) {
        if(currentTable == null || currentTable.length < otherTable.length)
            currentTable = otherTable.clone();
        else
            System.arraycopy(otherTable, 0, currentTable, 0, otherTable.length);
    
        // null out object ID so we don't share it
        if (objectIdIdx >= 0 && objectIdIdx < currentTable.length) {
            currentTable[objectIdIdx] = null;
        }
        
        return currentTable;
    }

    public synchronized final VariableAccessor allocateVariableAccessor(String name, int id) {
        String[] myVariableNames = variableNames;

        int newIndex = myVariableNames.length;
        String[] newVariableNames = new String[newIndex + 1];

        VariableAccessor newVariableAccessor;
        if (UnsafeHolder.U == null) {
            newVariableAccessor = new SynchronizedVariableAccessor(realClass, name, newIndex, id);
        } else {
            newVariableAccessor = new StampedVariableAccessor(realClass, name, newIndex, id);
        }

        System.arraycopy(myVariableNames, 0, newVariableNames, 0, newIndex);

        newVariableNames[newIndex] = name;
        variableNames = newVariableNames;

        return newVariableAccessor;
    }

    public VariableAccessor getVariableAccessorForWrite(String name, int id) {
        VariableAccessor ivarAccessor = variableAccessors.get(name);
        if (ivarAccessor == null) {

            synchronized (realClass) {
                Map<String, VariableAccessor> myVariableAccessors = variableAccessors;
                ivarAccessor = myVariableAccessors.get(name);

                if (ivarAccessor == null) {
                    // allocate a new accessor and populate a new table
                    ivarAccessor = allocateVariableAccessor(name, id);
                    Map<String, VariableAccessor> newVariableAccessors = new HashMap<String, VariableAccessor>(myVariableAccessors.size() + 1);

                    newVariableAccessors.putAll(myVariableAccessors);
                    newVariableAccessors.put(name, ivarAccessor);

                    variableAccessors = newVariableAccessors;
                }
            }
        }
        return ivarAccessor;
    }

    public VariableAccessor getVariableAccessorForRead(String name) {
        VariableAccessor accessor = getVariableAccessorsForRead().get(name);
        if (accessor == null) accessor = VariableAccessor.DUMMY_ACCESSOR;
        return accessor;
    }

    public VariableAccessorField getObjectIdAccessorField() {
        return objectIdVariableAccessorField;
    }

    public VariableAccessorField getNativeHandleAccessorField() {
        return cextHandleVariableAccessorField;
    }

    public VariableAccessor getNativeHandleAccessorForRead() {
        return cextHandleVariableAccessorField.getVariableAccessorForRead();
    }

    public VariableAccessor getNativeHandleAccessorForWrite() {
        return cextHandleVariableAccessorField.getVariableAccessorForWrite(this, realClass.id);
    }

    public VariableAccessorField getFFIHandleAccessorField() {
        return ffiHandleVariableAccessorField;
    }

    public VariableAccessor getFFIHandleAccessorForRead() {
        return ffiHandleVariableAccessorField.getVariableAccessorForRead();
    }

    public VariableAccessor getFFIHandleAccessorForWrite() {
        return ffiHandleVariableAccessorField.getVariableAccessorForWrite(this, realClass.id);
    }

    public VariableAccessorField getObjectGroupAccessorField() {
        return objectGroupVariableAccessorField;
    }

    public VariableAccessor getObjectGroupAccessorForRead() {
        return objectGroupVariableAccessorField.getVariableAccessorForRead();
    }

    public VariableAccessor getObjectGroupAccessorForWrite() {
        return objectGroupVariableAccessorField.getVariableAccessorForWrite(this, realClass.id);
    }
    
    public final Object getNativeHandle(RubyBasicObject self) {
        return getNativeHandleAccessorForRead().get(self);
    }

    public final void setNativeHandle(RubyBasicObject self, Object value) {
        int index = getNativeHandleAccessorForRead().getIndex();
        setVariableInternal(realClass, self, index, value);
    }

    public final Object getFFIHandle(RubyBasicObject self) {
        return getFFIHandleAccessorForRead().get(self);
    }

    public final void setFFIHandle(RubyBasicObject self, Object value) {
        int index = getFFIHandleAccessorForWrite().getIndex();
        setVariableInternal(realClass, self, index, value);
    }

    public int getVariableTableSize() {
        return variableAccessors.size();
    }

    public int getVariableTableSizeWithExtras() {
        return variableNames.length;
    }

    public Map<String, VariableAccessor> getVariableTableCopy() {
        return new HashMap<String, VariableAccessor>(getVariableAccessorsForRead());
    }

    /**
     * Get an array of all the known instance variable names. The offset into
     * the array indicates the offset of the variable's value in the per-object
     * variable array.
     *
     * @return a copy of the array of known instance variable names
     */
    public String[] getVariableNames() {
        String[] original = variableNames;
        String[] copy = new String[original.length];
        System.arraycopy(original, 0, copy, 0, original.length);
        return copy;
    }
}
