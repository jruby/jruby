package org.jruby.runtime.ivars;

import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.util.ArraySupport;
import org.jruby.util.unsafe.UnsafeHolder;

/**
 * Shared operations for working atomically with a Ruby object's variable table.
 */
public class AtomicVariableTable {

    /**
     * Set the given variable index into the specified object using atomic semantics. The "real" class
     * and index are pass in to provide functional access.
     *
     * @param self the object into which to set the variable
     * @param realClass the "real" class for the object
     * @param fullFence if a full memory fence should be forced after updating the variable's value
     * @param index the index of the variable
     * @param value the variable's value
     */
    public static void setVariableAtomic(RubyBasicObject self, RubyClass realClass, boolean fullFence, int index, Object value) {
        while (true) {
            int currentStamp = self.varTableStamp;
            // spin-wait if odd
            if((currentStamp & 0x01) != 0)
                continue;

            Object[] currentTable = (Object[]) UnsafeHolder.U.getObjectVolatile(self, RubyBasicObject.VAR_TABLE_OFFSET);

            if (currentTable == null || index >= currentTable.length) {
                if (!createTableAtomic(self, currentStamp, realClass, currentTable, index, value)) continue;
            } else {
                if (!updateTableAtomic(self, currentStamp, fullFence, currentTable, index, value)) continue;
            }

            break;
        }
    }

    /**
     * Create or expand a variable table for the given object, using atomic operations to ensure visibility.
     *
     * @param self the object into which to set the variable
     * @param currentStamp the current variable table stamp
     * @param realClass the "real" class for the object
     * @param currentTable the current table
     * @param index the index of the variable
     * @param value the variable's value
     * @return whether the update was successful, for CAS retrying
     */
    private static boolean createTableAtomic(RubyBasicObject self, int currentStamp, RubyClass realClass, Object[] currentTable, int index, Object value) {
        // try to acquire exclusive access to the varTable field
        if (!UnsafeHolder.U.compareAndSwapInt(self, RubyBasicObject.STAMP_OFFSET, currentStamp, ++currentStamp)) {
            return false;
        }

        Object[] newTable = new Object[realClass.getVariableTableSizeWithExtras()];

        if (currentTable != null) {
            ArraySupport.copy(currentTable, 0, newTable, 0, currentTable.length);
        } else {
            // on first table create, run additional warning checks
            if (self instanceof JavaProxy) {
                ((JavaProxy) self).checkVariablesOnProxy();
            }
        }

        newTable[index] = value;

        UnsafeHolder.U.putOrderedObject(self, RubyBasicObject.VAR_TABLE_OFFSET, newTable);

        // release exclusive access
        self.varTableStamp = currentStamp + 1;

        return true;
    }

    /**
     * Update the given variable table for the given object, using atomic operations to ensure visibility.
     *
     * @param self the object into which to set the variable
     * @param currentStamp the current variable table stamp
     * @param fullFence if a full memory fence should be forced after updating the variable's value
     * @param currentTable the current table
     * @param index the index of the variable
     * @param value the variable's value
     * @return whether the update was successful, for CAS retrying
     */
    private static boolean updateTableAtomic(RubyBasicObject self, int currentStamp, boolean fullFence, Object[] currentTable, int index, Object value) {
        // shared access to varTable field.
        currentTable[index] = value;
        if (fullFence) UnsafeHolder.U.fullFence();

        // validate stamp. redo on concurrent modification
        return self.varTableStamp == currentStamp;
    }
}
