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

/**
 * A lazy mechanism for accessing VariableAccessors for a given variable name.
 */
public final class VariableAccessorField {
    /** the name of the variable */
    private final String name;
    
    /** the accessor cached after it has been allocated */
    private volatile VariableAccessor variableAccessor = VariableAccessor.DUMMY_ACCESSOR;

    /**
     * Construct a new VariableAccessorField for the given named variable.
     * 
     * @param name the name of the variable
     */
    public VariableAccessorField(String name) {
        this.name = name;
    }

    /**
     * Retrieve the variable accessor for read.
     * 
     * @return the variable accessor appropriate for reads
     */
    public VariableAccessor getVariableAccessorForRead() {
        return variableAccessor;
    }

    /**
     * Retrieve the variable access for write.
     * 
     * @param tableMgr the VariableTableManager for which to allocate an
     * accessor, if it has not already been allocated.
     * @return the variable accessor appropriate for writes.
     */
    public VariableAccessor getVariableAccessorForWrite(VariableTableManager tableMgr) {
        return variableAccessor != VariableAccessor.DUMMY_ACCESSOR ? variableAccessor : allocateVariableAccessor(tableMgr);
    }

    /**
     * Retrieve or allocate the variable accessor for this variable. This
     * version differs from getVariableAccessorForWrite only in its visibility
     * and the fact that it is synchronized against this field to ensure
     * volatile behavior of the variableAccessor field.
     * 
     * @param tableMgr the VariableTableManager for which to allocate an
     * accessor, if it has not already been allocated.
     * @return the variable accessor appropriate for writes
     */
    private synchronized VariableAccessor allocateVariableAccessor(VariableTableManager tableMgr) {
        if (variableAccessor == VariableAccessor.DUMMY_ACCESSOR) {
            variableAccessor = tableMgr.allocateVariableAccessor(name);
        }
        return variableAccessor;
    }
    
}
