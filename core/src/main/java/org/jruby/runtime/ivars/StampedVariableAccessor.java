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

import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.util.unsafe.UnsafeHolder;

/**
 * A variable accessor that uses a stamped volatile int and Unsafe methods to
 * ensure thread-friendly table updating.
 */
public class StampedVariableAccessor extends VariableAccessor {
    /**
     * Construct a new StampedVariableAccessor for the given "real" class,
     * variable name, variable index, and class ID.
     * 
     * @param realClass the "real" class
     * @param name the variable's name
     * @param index the variable's index
     * @param classId the class's ID
     */
    public StampedVariableAccessor(RubyClass realClass, String name, int index, int classId) {
        super(realClass, name, index, classId);
    }

    /**
     * Set this variable into the given object using Unsafe to ensure
     * safe updating of the variable table.
     * 
     * @param object the object into which to set this variable
     * @param value the variable's value
     */
    public void set(Object object, Object value) {
        ((RubyBasicObject)object).ensureInstanceVariablesSettable();
        setVariable((RubyBasicObject)object, realClass, index, value);
    }
    
    /**
     * Set the given variable index into the specified object. The "real" class
     * and index are pass in to provide functional access. This version checks
     * if self has been frozen before proceeding to set the variable.
     * 
     * @param self the object into which to set the variable
     * @param realClass the "real" class for the object
     * @param index the index of the variable
     * @param value the variable's value
     */
    public static void setVariableChecked(RubyBasicObject self, RubyClass realClass, int index, Object value) {
        self.ensureInstanceVariablesSettable();
        setVariable(self, realClass, index, value);
    }
    
    /**
     * Set the given variable index into the specified object. The "real" class
     * and index are pass in to provide functional access.
     * 
     * @param self the object into which to set the variable
     * @param realClass the "real" class for the object
     * @param index the index of the variable
     * @param value the variable's value
     */
    public static void setVariable(RubyBasicObject self, RubyClass realClass, int index, Object value) {
        while (true) {
            int currentStamp = self.varTableStamp;
            // spin-wait if odd
            if((currentStamp & 0x01) != 0)
               continue;
            
            Object[] currentTable = (Object[]) UnsafeHolder.U.getObjectVolatile(self, RubyBasicObject.VAR_TABLE_OFFSET);
            
            if (currentTable == null || index >= currentTable.length) {
                if (!createTableUnsafe(self, currentStamp, realClass, currentTable, index, value)) continue;
            } else {
                if (!updateTableUnsafe(self, currentStamp, currentTable, index, value)) continue;
            }
            
            break;
        }
    }

    /**
     * Create or exapand a table for the given object, using Unsafe CAS and
     * ordering operations to ensure visibility.
     * 
     * @param self the object into which to set the variable
     * @param currentStamp the current variable table stamp
     * @param realClass the "real" class for the object
     * @param currentTable the current table
     * @param index the index of the variable
     * @param value the variable's value
     * @return whether the update was successful, for CAS retrying
     */
    private static boolean createTableUnsafe(RubyBasicObject self, int currentStamp, RubyClass realClass, Object[] currentTable, int index, Object value) {
        // try to acquire exclusive access to the varTable field
        if (!UnsafeHolder.U.compareAndSwapInt(self, RubyBasicObject.STAMP_OFFSET, currentStamp, ++currentStamp)) {
            return false;
        }
        
        Object[] newTable = new Object[realClass.getVariableTableSizeWithExtras()];
        
        if(currentTable != null) {
            System.arraycopy(currentTable, 0, newTable, 0, currentTable.length);
        }
        
        newTable[index] = value;
        
        UnsafeHolder.U.putOrderedObject(self, RubyBasicObject.VAR_TABLE_OFFSET, newTable);
        
        // release exclusive access
        self.varTableStamp = currentStamp + 1;
        
        return true;
    }

    /**
     * Update the given table table for the given object, using Unsafe fence or
     * volatile operations to ensure visibility.
     * 
     * @param self the object into which to set the variable
     * @param currentStamp the current variable table stamp
     * @param currentTable the current table
     * @param index the index of the variable
     * @param value the variable's value
     * @return whether the update was successful, for CAS retrying
     */
    private static boolean updateTableUnsafe(RubyBasicObject self, int currentStamp, Object[] currentTable, int index, Object value) {
        // shared access to varTable field.
        if(UnsafeHolder.SUPPORTS_FENCES) {
            currentTable[index] = value;
            UnsafeHolder.fullFence();
        } else {
            // TODO: maybe optimize by read and checking current value before setting
            UnsafeHolder.U.putObjectVolatile(currentTable, UnsafeHolder.ARRAY_OBJECT_BASE_OFFSET + UnsafeHolder.ARRAY_OBJECT_INDEX_SCALE * index, value);
        }

        // validate stamp. redo on concurrent modification
        return self.varTableStamp == currentStamp;
    }
}
