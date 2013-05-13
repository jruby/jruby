package org.jruby.runtime.ivars;

import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.util.unsafe.UnsafeHolder;

public class StampedVariableAccessor extends VariableAccessor {
    public StampedVariableAccessor(RubyClass realClass, String name, int index, int classId) {
        super(realClass, name, index, classId);
    }

    public void set(Object object, Object value) {
        setVariableStamped(realClass, (RubyBasicObject)object, index, value);
    }
    
    public static void setVariableStamped(RubyClass realClass, RubyBasicObject self, int index, Object value) {
        for(;;) {
            int currentStamp = self.varTableStamp;
            // spin-wait if odd
            if((currentStamp & 0x01) != 0)
               continue;
            
            Object[] currentTable = (Object[]) UnsafeHolder.U.getObjectVolatile(self, RubyBasicObject.VAR_TABLE_OFFSET);
            
            if(currentTable == null || index >= currentTable.length) {
                if (createTableUnsafe(self, currentStamp, realClass, currentTable, index, value)) continue;
            } else {
                if (updateTableUnsafe(currentTable, index, value, self, currentStamp)) continue;
            }
            
            break;
        }
    }

    private static boolean createTableUnsafe(RubyBasicObject self, int currentStamp, RubyClass realClass, Object[] currentTable, int index, Object value) {
        // try to acquire exclusive access to the varTable field
        if (!UnsafeHolder.U.compareAndSwapInt(self, RubyBasicObject.STAMP_OFFSET, currentStamp, ++currentStamp)) {
            return true;
        }
        Object[] newTable = new Object[realClass.getVariableTableSizeWithExtras()];
        if(currentTable != null)
            System.arraycopy(currentTable, 0, newTable, 0, currentTable.length);
        newTable[index] = value;
        UnsafeHolder.U.putOrderedObject(self, RubyBasicObject.VAR_TABLE_OFFSET, newTable);
        // release exclusive access
        self.varTableStamp = currentStamp + 1;
        return false;
    }

    private static boolean updateTableUnsafe(Object[] currentTable, int index, Object value, RubyBasicObject self, int currentStamp) {
        // shared access to varTable field.
        if(UnsafeHolder.SUPPORTS_FENCES) {
            currentTable[index] = value;
            UnsafeHolder.fullFence();
        } else {
            // TODO: maybe optimize by read and checking current value before setting
            UnsafeHolder.U.putObjectVolatile(currentTable, UnsafeHolder.ARRAY_OBJECT_BASE_OFFSET + UnsafeHolder.ARRAY_OBJECT_INDEX_SCALE * index, value);
        }
        // validate stamp. redo on concurrent modification
        if (self.varTableStamp != currentStamp) {
            return true;
        }
        return false;
    }
}
