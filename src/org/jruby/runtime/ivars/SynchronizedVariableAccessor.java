package org.jruby.runtime.ivars;

import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.VariableTableManager;

public class SynchronizedVariableAccessor extends VariableAccessor {
    public SynchronizedVariableAccessor(RubyClass realClass, String name, int index, int classId) {
        super(realClass, name, index, classId);
    }

    public void set(Object object, Object value) {
        setVariableSynchronized(realClass, (RubyBasicObject)object, index, value);
    }

    public static void setVariableSynchronized(RubyClass realClass, RubyBasicObject self, int index, Object value) {
        synchronized (self) {
            ensureTable(self, realClass, index)[index] = value;
        }
    }

    private static Object[] ensureTable(RubyBasicObject self, RubyClass realClass, int index) {
        Object[] currentTable = self.varTable;
        if (currentTable == null) {
            return createTable(self, realClass);
        } else if (currentTable.length <= index) {
            return growTable(realClass, currentTable, self);
        }
        return currentTable;
    }
    
    private static Object[] createTable(RubyBasicObject self, RubyClass realClass) {
        return self.varTable = new Object[realClass.getVariableTableSizeWithExtras()];
    }

    private static Object[] growTable(RubyClass realClass, Object[] currentTable, RubyBasicObject self) {
        Object[] newTable = new Object[realClass.getVariableTableSizeWithExtras()];
        System.arraycopy(currentTable, 0, newTable, 0, currentTable.length);
        return self.varTable = newTable;
    }
}
