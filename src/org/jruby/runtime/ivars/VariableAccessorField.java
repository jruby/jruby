/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.runtime.ivars;

import org.jruby.VariableTableManager;

/**
 *
 * @author headius
 */
public final class VariableAccessorField {
    private final String name;
    private volatile VariableAccessor variableAccessor = VariableAccessor.DUMMY_ACCESSOR;

    public VariableAccessorField(String name) {
        this.name = name;
    }

    public VariableAccessor getVariableAccessorForRead() {
        return variableAccessor;
    }

    public VariableAccessor getVariableAccessorForWrite(VariableTableManager tableMgr, int id) {
        return variableAccessor != VariableAccessor.DUMMY_ACCESSOR ? variableAccessor : allocateVariableAccessor(tableMgr, id);
    }

    private synchronized VariableAccessor allocateVariableAccessor(VariableTableManager tableMgr, int id) {
        if (variableAccessor == VariableAccessor.DUMMY_ACCESSOR) {
            variableAccessor = tableMgr.allocateVariableAccessor(name, id);
        }
        return variableAccessor;
    }
    
}
