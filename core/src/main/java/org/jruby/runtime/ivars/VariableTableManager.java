/*
 ***** BEGIN LICENSE BLOCK *****
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
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.jruby.Ruby;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.java.codegen.Reified;
import org.jruby.javasupport.util.JavaClassConfiguration;
import org.jruby.javasupport.util.JavaClassConfiguration.DirectFieldConfiguration;
import org.jruby.runtime.ObjectSpace;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.specialized.RubyObjectSpecializer;
import org.jruby.util.ArraySupport;

import static org.jruby.api.Error.nameError;
import static org.jruby.api.Error.typeError;
import static org.jruby.util.StringSupport.EMPTY_STRING_ARRAY;

/**
 * This class encapculates all logic relating to the management of instance
 * variable tables in RubyBasicObject instances.
 *
 * The logic originally lived in both RubyBasicObject and RubyClass, tightly
 * coupled to each and difficult to follow as it bounced back and forth. We
 * moved the logic here for a couple reasons:
 * <ul>
 * <li>To have a single place from which we could follow ivar logic.</li>
 * <li>To make it easier to swap in new implementations of instance variable
 * logic as we work toward reifying ivars into fields.</li>
 * <li>To remove rather noisy logic from RubyBasicObject and RubyClass.</li>
 * </ul>
 */
public class VariableTableManager {
    /** the "real" class associated with this table */
    private final RubyClass realClass;

    /** a map from strings to accessors for this table */
    @SuppressWarnings("unchecked")
    private Map<String, VariableAccessor> variableAccessors = Collections.EMPTY_MAP;

    /** an array of all registered variable names */
    private volatile String[] variableNames = EMPTY_STRING_ARRAY;

    /** whether a slot has been allocated to object_id */
    private volatile int hasObjectID = 0;

    /** whether a slot has been allocated to ffi */
    private volatile int hasFFI = 0;

    /** whether a slot has been allocated to objectspace_group */
    private volatile int hasObjectspaceGroup = 0;

    /** whether objects associated with this table use fields */
    private volatile int fieldVariables = 0;

    /** a lazy accessor for object_id */
    private final VariableAccessorField objectIdVariableAccessorField = new VariableAccessorField("object_id");
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
     * Copy constructor with deep cloning.
     *
     * @param original VariableTableManager to copy
     */
    VariableTableManager(VariableTableManager original) {
        synchronized (original) {
            this.realClass = original.realClass;
            this.variableAccessors = copyVariableAccessors(original.variableAccessors);
            this.variableNames = original.variableNames.clone();
            this.hasObjectID = original.hasObjectID;
            this.hasFFI = original.hasFFI;
            this.hasObjectspaceGroup = original.hasObjectspaceGroup;
            this.fieldVariables = original.fieldVariables;
        }
    }

    public RubyClass getRealClass() {
        return realClass;
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
        return hasObjectID == 1;
    }

    /**
     * Get the object_id from a given RubyBasicObject, possibly allocating
     * space for it.
     *
     * @param self the object from which to get object_id
     * @return the object's object_id (possibly new)
     */
    public long getObjectId(RubyBasicObject self) {
        VariableAccessor objectIdAccessor = getObjectIdAccessorForRead();
        Number id = (Number)objectIdAccessor.get(self);
        if (id != null) return id.longValue();

        synchronized (self) {
            objectIdAccessor = getObjectIdAccessorForRead();
            id = (Number)objectIdAccessor.get(self);
            if (id != null) return id.longValue();

            objectIdAccessor = getObjectIdAccessorForWrite();
            return initObjectId(self, objectIdAccessor);
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
        AtomicVariableTable.setVariableAtomic(self,realClass,true,index,value);
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
        AtomicVariableTable.setVariableAtomic(self,realClass,true,index,value);
    }

    /**
     * This is internal API, don't call this directly if you aren't in the JRuby codebase, it may change
     * Request that the listed ivars (no @ in name) have field storage when we are reified
     */
    public synchronized void requestFieldStorage(String name, Class<?> fieldType,  Boolean unwrap, Class<?> toType) {
        DirectFieldConfiguration config = new JavaClassConfiguration.DirectFieldConfiguration(name, fieldType, unwrap, toType);
        if (realClass.reifiedClass() != null)
            requestFieldStorage(config);
        else {
            if (realClass.getClassConfig().requestedStorageVariables == null)
                realClass.getClassConfig().requestedStorageVariables = new ArrayList<>();
            realClass.getClassConfig().requestedStorageVariables.add(config);
        }
    }

    /**
     * Actually requests field storage for the ivar once we are reified
     * This is internal API, don't call this directly if you aren't in the JRuby codebase, it may change
     */
    public void requestFieldStorage(DirectFieldConfiguration config) {
        try {
            Class<? extends Reified> reifiedClass = realClass.reifiedClass();
            Class<?> fieldType = reifiedClass.getField(config.name).getType();
            if (fieldType != config.fieldType) {
                throw typeError(realClass.getClassRuntime().getCurrentContext(), "java_field " + config.name + " has incorrectly specified types for @ivar mapping");
            }
            
            // by default, unwrap if it's not assignable to IRO
            boolean unwrap = config.unwrap == null ? !fieldType.isAssignableFrom(IRubyObject.class) : config.unwrap.booleanValue();
            Class<?> unwrapType = config.unwrapType == null ? fieldType : config.unwrapType;
            if (unwrap && !fieldType.isAssignableFrom(unwrapType)) {
                throw typeError(realClass.getClassRuntime().getCurrentContext(), config.name + " has incorrectly specified unwrap type for @ivar mapping: type is incompatible");
            }
            
            getVariableAccessorForJavaMappedVar("@" + config.name,
                    unwrap,
                    fieldType,
                    unwrapType,
                    RubyObjectSpecializer.LOOKUP.findGetter(reifiedClass, config.name, fieldType),
                    RubyObjectSpecializer.LOOKUP.findSetter(reifiedClass, config.name, fieldType));
        } catch (NoSuchFieldException e) {
            throw nameError(realClass.getClassRuntime().getCurrentContext(), "java_field " + config.name + " was marked for @ivar mapping, but wasn't found (was the class reifed already?)", config.name);
        } catch (IllegalAccessException e) {
            throw realClass.getClassRuntime().newSecurityError("Error in accessing java_field " +config.name+ ": " + e.getMessage());
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
        return getVariableAccessorWithBuilder(name, makeTableVariableAccessorBuilder(name));
    }

    public VariableAccessor getVariableAccessorForRubyVar(String name, MethodHandle getter, MethodHandle setter) {
        return getVariableAccessorWithBuilder(name, makeRubyFieldAccessorBuilder(name, getter, setter));
    }
    
    public VariableAccessor getVariableAccessorForJavaMappedVar(String name, boolean unwrap, Class<?> unwrapType, Class<?> fieldType, MethodHandle getter, MethodHandle setter) {
        return getVariableAccessorWithBuilder(name, makeRawFieldAccessorBuilder(name, unwrap, unwrapType, fieldType, getter, setter));
    }
    
    /**
     * Get the variable accessor for the given name, or if it doesn't exist, create it 
     * with the provided builder.
     *
     * @param name the name of the variable
     * @param defaultAccessorBuilder a builder to use if the accessor doesn't exist yet
     * @return an accessor
     */
    VariableAccessor getVariableAccessorWithBuilder(String name, Function<Integer, VariableAccessor> defaultAccessorBuilder) {
        VariableAccessor ivarAccessor = variableAccessors.get(name);
        if (ivarAccessor == null) {

            synchronized (realClass) {
                Map<String, VariableAccessor> myVariableAccessors = variableAccessors;
                ivarAccessor = myVariableAccessors.get(name);

                if (ivarAccessor == null) {
                    // allocate a new accessor and populate a new table
                    ivarAccessor = allocateVariableAccessors(name, defaultAccessorBuilder);
                    variableAccessors = copyVariableAccessors(myVariableAccessors, name, ivarAccessor);
                }
            }
        }
        return ivarAccessor;
    }

    private static Map<String, VariableAccessor> copyVariableAccessors(Map<String, VariableAccessor> myVariableAccessors) {
        return new LinkedHashMap<>(myVariableAccessors);
    }

    private static Map<String, VariableAccessor> copyVariableAccessors(Map<String, VariableAccessor> myVariableAccessors, String name, VariableAccessor ivarAccessor) {
        LinkedHashMap<String, VariableAccessor> newVariableAccessors = new LinkedHashMap<>(myVariableAccessors.size() + 1);
        newVariableAccessors.putAll(myVariableAccessors);
        newVariableAccessors.put(name, ivarAccessor);
        return newVariableAccessors;
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
     * Retrieve the read accessor for object_id for reads. If no object_id has been prepared, this will return a dummy
     * accessor that just returns null.
     *
     * @return the read accessor for object_id
     */
    public VariableAccessor getObjectIdAccessorForRead() {
        return objectIdVariableAccessorField.getVariableAccessorForRead();
    }

    /**
     * Retrieve the write accessor for object_id.
     *
     * @return the write accessor for object_id
     */
    public VariableAccessor getObjectIdAccessorForWrite() {
        // set-only so no synchronization required
        if (hasObjectID == 0) hasObjectID = 1;

        return objectIdVariableAccessorField.getVariableAccessorForWrite(this);
    }

    /**
     * Retrieve the read accessor for FFI handle. If no object_id has been prepared, this will return a dummy
     * accessor that just returns null.
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
        if (hasFFI == 0) hasFFI = 1;
        return ffiHandleVariableAccessorField.getVariableAccessorForWrite(this);
    }

    /**
     * Retrieve the read accessor for object group. If no object_id has been prepared, this will return a dummy
     * accessor that just returns null.
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
        if (hasObjectspaceGroup == 0) hasObjectspaceGroup = 1;
        return objectGroupVariableAccessorField.getVariableAccessorForWrite(this);
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
        return new LinkedHashMap<String, VariableAccessor>(getVariableAccessorsForRead());
    }

    /**
     * Get an array of all the known instance variable names. The offset into
     * the array indicates the offset of the variable's value in the per-object
     * variable array.
     *
     * @return a copy of the array of known instance variable names
     */
    public String[] getVariableNames() {
        return variableNames.clone();
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
            int idIndex = otherRealClass.getVariableTableManager().getObjectIdAccessorForRead().getIndex();

            Object[] otherVars = ((RubyBasicObject) other).varTable;

            for(;;) {
                int oldStamp = self.varTableStamp;
                // wait for read mode
                if((oldStamp & 0x01) == 1)
                    continue;
                // acquire exclusive write mode
                if(!VariableAccessor.STAMP_HANDLE.compareAndSet(self, oldStamp, ++oldStamp))
                    continue;

                Object[] currentTable = (Object[]) VariableAccessor.VAR_TABLE_HANDLE.getVolatile(self);
                Object[] newTable = makeSyncedTable(currentTable,otherVars, idIndex);

                VariableAccessor.VAR_TABLE_HANDLE.setRelease(self, newTable);

                // release write mode
                self.varTableStamp = oldStamp+1;
                break;
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
        return fieldVariables > 0 ||
                (myVarTable = object.varTable) != null && myVarTable.length > hasObjectID + hasFFI + hasObjectspaceGroup;
    }

    public boolean hasInstanceVariables(RubyBasicObject object) {
        // we check both to exclude object_id
        Object[] myVarTable;
        if (fieldVariables > 0) return true;
        if ((myVarTable = object.varTable) != null
                && myVarTable.length > hasObjectID + hasFFI + hasObjectspaceGroup) {

            for (int i = 0; i < myVarTable.length; i++) {
                if (i == hasObjectID || i == hasFFI || i == hasObjectspaceGroup) continue;
                if (myVarTable[i] != null) return true;
            }
        }

        return false;
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

    public VariableTableManager duplicate() {
        return new VariableTableManager(this);
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
     * Note that this method does not need to be synchronized, since it is
     * only called from #getObjectId, which synchronizes against the target
     * object. The accessor is already present, and variable accesses are
     * thread-safe (albeit not atomic, which necessitates the synchronization
     * in getObjectId).
     *
     * Synchronization here ends up being a bottleneck for every object
     * created from the class that contains this VariableTableManager. See
     * GH #1400.
     *
     * @param objectIdAccessor The variable accessor to use for storing the
     * generated object ID
     * @return The generated object ID
     */
    private long initObjectId(RubyBasicObject self, VariableAccessor objectIdAccessor) {
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
        setVariableInternal(realClass, self, index, smallestBox(value));
    }

    /**
     * Return the smallest boxed Number that can hold the given value.
     *
     * @param value the value to box
     * @return the smallest Number box for that value
     */
    private static Number smallestBox(long value) {
        if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            return Byte.valueOf((byte) value);
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            return Short.valueOf((short) value);
        } else if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            return Integer.valueOf((int) value);
        } else {
            return Long.valueOf(value);
        }
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
        if (currentTable == null || currentTable.length < otherTable.length) {
            currentTable = otherTable.clone();
        } else {
            ArraySupport.copy(otherTable, currentTable, 0, otherTable.length);
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
    final VariableAccessor allocateVariableAccessor(String name) {
        return allocateVariableAccessors(name, makeTableVariableAccessorBuilder(name));
    }
    
    /**
     * Makes a standard table accessor builder. Pass this into getVariableAccessorWithBuilder
     */
    final Function<Integer, VariableAccessor> makeTableVariableAccessorBuilder(String name) {
        return (newIndex) -> new StampedVariableAccessor(realClass, name, newIndex, realClass.id);
    }

    /**
     * Makes a raw field accessor builder for reified classes with java_field. Pass this into getVariableAccessorWithBuilder
     */
    final Function<Integer, VariableAccessor> makeRawFieldAccessorBuilder(String name, boolean unwrap,Class<?> unwrapType, Class<?> fieldType, MethodHandle getter, MethodHandle setter)
    {
        return (newIndex) -> {
            fieldVariables += 1;
            return new RawFieldVariableAccessor(realClass, unwrap, unwrapType, fieldType, name, newIndex, realClass.id, getter, setter);
        };
    }

    /**
     * Makes an IRubyObject field accessor builder for reified classes. Pass this into getVariableAccessorWithBuilder
     */
    final Function<Integer, VariableAccessor> makeRubyFieldAccessorBuilder(String name, MethodHandle getter, MethodHandle setter)
    {
        return (newIndex) -> {
            fieldVariables += 1;
            return new FieldVariableAccessor(realClass, name, newIndex, realClass.id, getter, setter);
        };
    }

    /**
     * Allocation helper to map variables to names
     */
    synchronized final VariableAccessor allocateVariableAccessors(String name, Function<Integer, VariableAccessor> builder) {

        final String[] myVariableNames = variableNames;
        final int newIndex = myVariableNames.length;

        VariableAccessor newVariableAccessor = builder.apply(newIndex);

        final String[] newVariableNames = new String[newIndex + 1];
        ArraySupport.copy(myVariableNames, 0, newVariableNames, 0, newIndex);
        newVariableNames[newIndex] = name;
        variableNames = newVariableNames;

        return newVariableAccessor;
    }

    /**
     * Retrieve the lazy accessor (VariableAccessorField) for object_id.
     *
     * @return the lazy accessor for object_id
     * @deprecated Use {@link #getObjectIdAccessorForRead()} or {@link #getObjectIdAccessorForWrite()}
     */
    @Deprecated(since = "9.4-")
    public VariableAccessorField getObjectIdAccessorField() {
        return objectIdVariableAccessorField;
    }

    /**
     * Retrieve the lazy accessor (VariableAccessorField) for FFI handle.
     *
     * @return the lazy accessor for FFI handle
     * @deprecated Use {@link #getFFIHandleAccessorForRead()} or {@link #getFFIHandleAccessorForWrite()}
     */
    @Deprecated(since = "9.4-")
    public VariableAccessorField getFFIHandleAccessorField() {
        return ffiHandleVariableAccessorField;
    }

    /**
     * Retrieve the lazy accessor (VariableAccessorField) for object group.
     *
     * @return the lazy accessor for object group
     * @deprecated Use {@link #getObjectGroupAccessorForRead()} or {@link #getObjectGroupAccessorForWrite()}
     */
    @Deprecated(since = "9.4-")
    public VariableAccessorField getObjectGroupAccessorField() {
        return objectGroupVariableAccessorField;
    }
}
