/*
 ***** BEGIN LICENSE BLOCK *****
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
package org.jruby.runtime.ivars;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.runtime.ObjectSpace;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.unsafe.UnsafeHolder;

/**
 * This class encapculates all logic relating to the management of instance
 * variable tables in RubyBasicObject instances.
 * 
 * The logic originally lived in both RubyBasicObject and RubyClass, tightly
 * coupled to each and difficult to follow as it bounced back and forth. We
 * moved the logic here for a couple reasons:
 * 
 * <li>To have a single place from which we could follow ivar logic.</li>
 * <li>To make it easier to swap in new implementations of instance variable
 * logic as we work toward reifying ivars into fields.</li>
 * <li>To remove rather noisy logic from RubyBasicObject and RubyClass.</li>
 */
public class VariableTableManager {
    /** the "real" class associated with this table */
    private final RubyClass realClass;
    
    /** an empty array of String */
    private static String[] EMPTY_STRING_ARRAY = new String[0];
    
    /** a map from strings to accessors for this table */
    private Map<String, VariableAccessor> variableAccessors = (Map<String, VariableAccessor>)Collections.EMPTY_MAP;
    
    /** an array of all registered variable names */
    private volatile String[] variableNames = EMPTY_STRING_ARRAY;

    /** whether a slot has been allocated to object_id */
    private volatile boolean hasObjectID = false;
    
    /** whether objects associated with this table use fields */
    private volatile int fieldVariables = 0;
    
    /** a lazy accessor for object_id */
    private final VariableAccessorField objectIdVariableAccessorField = new VariableAccessorField("object_id");
    /** a lazy accessor for C ext handle */
    private final VariableAccessorField cextHandleVariableAccessorField = new VariableAccessorField("cext");
    /** a lazy accessor for FFI handle */
    private final VariableAccessorField ffiHandleVariableAccessorField = new VariableAccessorField("ffi");
    /** a lazy accessor for object group */
    private final VariableAccessorField objectGroupVariableAccessorField = new VariableAccessorField("objectspace_group");
    
    /**
     * Construct a new VariableTable Manager for the given "real" class.
     * 
     * @param realClass the "real" class associated with this table
     */
    public VariableTableManager(RubyClass realClass) {
        this.realClass = realClass;
    }

    /**
     * Get the map of all current variable accessors with intent to read from it.
     * 
     * @return a map of current variable accessors
     */
    public Map<String, VariableAccessor> getVariableAccessorsForRead() {
        return variableAccessors;
    }
    
    /**
     * Whether this table has been used to allocate space for an object_id.
     * 
     * @return true if object_id has been allocated; false otherwise
     */
    public boolean hasObjectID() {
        return hasObjectID;
    }
    
    /**
     * Get the object_id from a given RubyBasicObject, possibly allocating
     * space for it.
     * 
     * @param self the object from which to get object_id
     * @return the object's object_id (possibly new)
     */
    public long getObjectId(RubyBasicObject self) {
        VariableAccessor objectIdAccessor = getObjectIdAccessorField().getVariableAccessorForRead();
        Long id = (Long)objectIdAccessor.get(self);
        if (id != null) return id;
        
        synchronized (self) {
            objectIdAccessor = getObjectIdAccessorField().getVariableAccessorForRead();
            id = (Long)objectIdAccessor.get(self);
            if (id != null) return id;

            return initObjectId(self, getObjectIdAccessorField().getVariableAccessorForWrite(this));
        }
    }
    
    /**
     * Virtual entry point for setting a variable into an object.
     * 
     * @param self the object into which to set the value
     * @param index the index allocated for the value
     * @param value the value
     */
    public void setVariableInternal(RubyBasicObject self, int index, Object value) {
        if(UnsafeHolder.U == null) {
            SynchronizedVariableAccessor.setVariable(self,realClass,index,value);
        } else {
            StampedVariableAccessor.setVariable(self,realClass,index,value);
        }
    }
    
    /**
     * Static entry point for setting a variable in an object.
     * 
     * @param realClass the "real" class of the object
     * @param self the object into which to set the variable
     * @param index the index allocated for the variable
     * @param value the value of the variable
     */
    public static void setVariableInternal(RubyClass realClass, RubyBasicObject self, int index, Object value) {
        if(UnsafeHolder.U == null) {
            SynchronizedVariableAccessor.setVariable(self,realClass,index,value);
        } else {
            StampedVariableAccessor.setVariable(self,realClass,index,value);
        }
    }

    /**
     * Get the variable accessor for the given name with intent to use it for
     * writing.
     * 
     * @param name the name of the variable
     * @return an accessor appropriate for writing
     */
    public VariableAccessor getVariableAccessorForWrite(String name) {
        VariableAccessor ivarAccessor = variableAccessors.get(name);
        if (ivarAccessor == null) {

            synchronized (realClass) {
                Map<String, VariableAccessor> myVariableAccessors = variableAccessors;
                ivarAccessor = myVariableAccessors.get(name);

                if (ivarAccessor == null) {
                    // allocate a new accessor and populate a new table
                    ivarAccessor = allocateVariableAccessor(name);
                    Map<String, VariableAccessor> newVariableAccessors = new HashMap<String, VariableAccessor>(myVariableAccessors.size() + 1);

                    newVariableAccessors.putAll(myVariableAccessors);
                    newVariableAccessors.put(name, ivarAccessor);

                    variableAccessors = newVariableAccessors;
                }
            }
        }
        return ivarAccessor;
    }
    
    public VariableAccessor getVariableAccessorForVar(String name, int index) {
        VariableAccessor ivarAccessor = variableAccessors.get(name);
        if (ivarAccessor == null) {

            synchronized (realClass) {
                Map<String, VariableAccessor> myVariableAccessors = variableAccessors;
                ivarAccessor = myVariableAccessors.get(name);

                if (ivarAccessor == null) {
                    // allocate a new accessor and populate a new table
                    ivarAccessor = allocateVariableAccessorForVar(name, index);
                    Map<String, VariableAccessor> newVariableAccessors = new HashMap<String, VariableAccessor>(myVariableAccessors.size() + 1);

                    newVariableAccessors.putAll(myVariableAccessors);
                    newVariableAccessors.put(name, ivarAccessor);

                    variableAccessors = newVariableAccessors;
                }
            }
        }
        return ivarAccessor;
    }

    /**
     * Get the variable accessor for the given name with intent to use it for
     * reading.
     * 
     * @param name the name of the variable
     * @return an accessor appropriate for reading
     */
    public VariableAccessor getVariableAccessorForRead(String name) {
        VariableAccessor accessor = getVariableAccessorsForRead().get(name);
        if (accessor == null) accessor = VariableAccessor.DUMMY_ACCESSOR;
        return accessor;
    }

    /**
     * Retrieve the lazy accessor (VariableAccessorField) for object_id.
     * 
     * @return the lazy accessor for object_id
     */
    public VariableAccessorField getObjectIdAccessorField() {
        return objectIdVariableAccessorField;
    }

    /**
     * Retrieve the lazy accessor (VariableAccessorField) for C ext handle.
     * 
     * @return the lazy accessor for C ext handle
     */
    public VariableAccessorField getNativeHandleAccessorField() {
        return cextHandleVariableAccessorField;
    }

    /**
     * Retrieve the read accessor for C ext handle.
     * 
     * @return the read accessor for C ext handle
     */
    public VariableAccessor getNativeHandleAccessorForRead() {
        return cextHandleVariableAccessorField.getVariableAccessorForRead();
    }

    /**
     * Retrieve the write accessor for C ext handle.
     * 
     * @return the write accessor for C ext handle
     */
    public VariableAccessor getNativeHandleAccessorForWrite() {
        return cextHandleVariableAccessorField.getVariableAccessorForWrite(this);
    }

    /**
     * Retrieve the lazy accessor (VariableAccessorField) for FFI handle.
     * 
     * @return the lazy accessor for FFI handle
     */
    public VariableAccessorField getFFIHandleAccessorField() {
        return ffiHandleVariableAccessorField;
    }

    /**
     * Retrieve the read accessor for FFI handle.
     * 
     * @return the read accessor for FFI handle
     */
    public VariableAccessor getFFIHandleAccessorForRead() {
        return ffiHandleVariableAccessorField.getVariableAccessorForRead();
    }

    /**
     * Retrieve the write accessor for FFI handle.
     * 
     * @return the write accessor for FFI handle
     */
    public VariableAccessor getFFIHandleAccessorForWrite() {
        return ffiHandleVariableAccessorField.getVariableAccessorForWrite(this);
    }

    /**
     * Retrieve the lazy accessor (VariableAccessorField) for object group.
     * 
     * @return the lazy accessor for object group
     */
    public VariableAccessorField getObjectGroupAccessorField() {
        return objectGroupVariableAccessorField;
    }

    /**
     * Retrieve the read accessor for object group.
     * 
     * @return the read accessor for object group
     */
    public VariableAccessor getObjectGroupAccessorForRead() {
        return objectGroupVariableAccessorField.getVariableAccessorForRead();
    }

    /**
     * Retrieve the write accessor for object group.
     * 
     * @return the write accessor for object group
     */
    public VariableAccessor getObjectGroupAccessorForWrite() {
        return objectGroupVariableAccessorField.getVariableAccessorForWrite(this);
    }
    
    /**
     * Retrieve the C ext handle for the given object.
     * 
     * @param self the object
     * @return the object's C ext handle
     */
    public final Object getNativeHandle(RubyBasicObject self) {
        return getNativeHandleAccessorForRead().get(self);
    }

    /**
     * Set the C ext handle for the given object.
     * 
     * @param self the object
     * @param value the object's C ext handle
     */
    public final void setNativeHandle(RubyBasicObject self, Object value) {
        int index = getNativeHandleAccessorForRead().getIndex();
        if(index == -1) {
            return;
        }
        setVariableInternal(realClass, self, index, value);
    }

    /**
     * Retrieve the FFI ext handle for the given object.
     * 
     * @param self the object
     * @return the object's FFI handle
     */
    public final Object getFFIHandle(RubyBasicObject self) {
        return getFFIHandleAccessorForRead().get(self);
    }

    /**
     * Set the FFI handle for the given object.
     * 
     * @param self the object
     * @param self the object's FFI handle
     */
    public final void setFFIHandle(RubyBasicObject self, Object value) {
        int index = getFFIHandleAccessorForWrite().getIndex();
        setVariableInternal(realClass, self, index, value);
    }

    /**
     * Get the size of the variable table, excluding extra vars (object_id,
     * etc).
     * 
     * @return the variable table's size, excluding extras
     */
    public int getVariableTableSize() {
        return variableAccessors.size();
    }

    /**
     * Get the size of the variable table, including extra vars (object_etc,
     * etc).
     * 
     * @return 
     */
    public int getVariableTableSizeWithExtras() {
        return variableNames.length;
    }

    /**
     * Get a Map representing all variables registered in the variable table.
     * 
     * @return a map of names to accessors for all variables
     */
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
    
    /**
     * Sync one this object's variables with other's - this is used to make
     * rbClone work correctly.
     * 
     * @param self the object into which to sync variables
     * @param other the object from which to sync variables
     */
    public void syncVariables(RubyBasicObject self, IRubyObject other) {
        RubyClass otherRealClass = other.getMetaClass().getRealClass();
        boolean sameTable = otherRealClass == realClass;

        if (sameTable && fieldVariables == 0) {
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
    
    /**
     * Returns true if object has any variables, defined as:
     * <ul>
     * <li> instance variables
     * <li> class variables
     * <li> constants
     * <li> internal variables, such as those used when marshaling Ranges and Exceptions
     * </ul>
     * @return true if object has any variables, else false
     */
    public boolean hasVariables(RubyBasicObject object) {
        // we check both to exclude object_id
        Object[] myVarTable;
        return fieldVariables > 0 || getVariableTableSize() > 0 && (myVarTable = object.varTable) != null && myVarTable.length > 0;
    }

    public void serializeVariables(RubyBasicObject object, ObjectOutputStream oos) throws IOException {
        if (object.varTable != null) {
            Map<String, VariableAccessor> accessors = getVariableAccessorsForRead();
            oos.writeInt(accessors.size());
            for (VariableAccessor accessor : accessors.values()) {
                oos.writeUTF(RubyBasicObject.ERR_INSECURE_SET_INST_VAR);
                oos.writeObject(accessor.get(object));
            }
        } else {
            oos.writeInt(0);
        }
    }

    public void deserializeVariables(RubyBasicObject object, ObjectInputStream ois) throws IOException, ClassNotFoundException {
        int varCount = ois.readInt();
        for (int i = 0; i < varCount; i++) {
            String name = ois.readUTF();
            Object value = ois.readObject();
            getVariableAccessorForWrite(name).set(object, value);
        }
    }
    
    public Object clearVariable(RubyBasicObject object, String name) {
        synchronized(object) {
            Object value = getVariableAccessorForRead(name).get(object);
            getVariableAccessorForWrite(name).set(object, null);
            
            return value;
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
        setObjectId(realClass, self, objectIdAccessor.getIndex(), id);

        return id;
    }

    /**
     * Update object_id with the given value.
     * 
     * @param self the object into which to set object_id
     * @param index the index allocated to store object_id
     * @param value the value of object_id
     */
    private static void setObjectId(RubyClass realClass, RubyBasicObject self, int index, long value) {
        if (index < 0) return;
        setVariableInternal(realClass, self, index, value);
    }

    /**
     * Make a new variable table based on the values in a current and other
     * (incoming) table, excluding object_id at specified index.
     * 
     * @param currentTable the current table
     * @param otherTable the other (incoming) table
     * @param objectIdIdx the index of object_id to exclude
     * @return a new table formed by combining the given tables
     */
    private static Object[] makeSyncedTable(Object[] currentTable, Object[] otherTable, int objectIdIdx) {
        if(currentTable == null || currentTable.length < otherTable.length) {
            currentTable = otherTable.clone();
        } else {
            System.arraycopy(otherTable, 0, currentTable, 0, otherTable.length);
        }
    
        // null out object ID so we don't share it
        if (objectIdIdx >= 0 && objectIdIdx < currentTable.length) {
            currentTable[objectIdIdx] = null;
        }
        
        return currentTable;
    }

    /**
     * Allocate a new VariableAccessor for the named variable.
     * 
     * @param name the name of the variable
     * @return the new VariableAccessor
     */
    synchronized final VariableAccessor allocateVariableAccessor(String name) {
        int id = realClass.id;
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
    
    synchronized final VariableAccessor allocateVariableAccessorForVar(String name, int index) {
        int id = realClass.id;
        String[] myVariableNames = variableNames;

        int newIndex = myVariableNames.length;
        String[] newVariableNames = new String[newIndex + 1];
        
        fieldVariables += 1;

        VariableAccessor newVariableAccessor;
        switch (index) {
            case 0:
                newVariableAccessor = new VariableAccessorVar0(realClass, name, newIndex, id);
                break;
            case 1:
                newVariableAccessor = new VariableAccessorVar1(realClass, name, newIndex, id);
                break;
            case 2:
                newVariableAccessor = new VariableAccessorVar2(realClass, name, newIndex, id);
                break;
            case 3:
                newVariableAccessor = new VariableAccessorVar3(realClass, name, newIndex, id);
                break;
            case 4:
                newVariableAccessor = new VariableAccessorVar4(realClass, name, newIndex, id);
                break;
            case 5:
                newVariableAccessor = new VariableAccessorVar5(realClass, name, newIndex, id);
                break;
            case 6:
                newVariableAccessor = new VariableAccessorVar6(realClass, name, newIndex, id);
                break;
            case 7:
                newVariableAccessor = new VariableAccessorVar7(realClass, name, newIndex, id);
                break;
            case 8:
                newVariableAccessor = new VariableAccessorVar8(realClass, name, newIndex, id);
                break;
            case 9:
                newVariableAccessor = new VariableAccessorVar9(realClass, name, newIndex, id);
                break;
            default:
                throw new RuntimeException("unsupported var index in " + realClass + ": " + index);
        }

        System.arraycopy(myVariableNames, 0, newVariableNames, 0, newIndex);

        newVariableNames[newIndex] = name;
        variableNames = newVariableNames;

        return newVariableAccessor;
    }
}
