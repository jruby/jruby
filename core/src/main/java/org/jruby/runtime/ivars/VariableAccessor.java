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

/**
 * Abstraction of an accessor for instance or internal variables on Ruby
 * objects. Subclasses specialize this implementation appropriate to the
 * current JVM's capabilities and the mechanism for laying out variables.
 */
public class VariableAccessor {
    /** the name of the variable */
    protected final String name;
    /** the index allocated for it in the variable table */
    protected final int index;
    /** the ID of the class associated with this variable */
    protected final int classId;
    /** the "real" class associated with this variable */
    protected final RubyClass realClass;

    /**
     * Construct a new VariableAccessor for the given "real" class, name,
     * variable index, and class ID.
     * 
     * @param realClass the "real" class
     * @param name the variable's name
     * @param index the variable's index
     * @param classId the class's ID
     */
    public VariableAccessor(RubyClass realClass, String name, int index, int classId) {
        this.index = index;
        this.classId = classId;
        this.name = name;
        this.realClass = realClass;
    }

    /**
     * Get the ID of the class associated with this variable.
     * 
     * @return this variable's class's id
     */
    public int getClassId() {
        return classId;
    }

    /**
     * Get the index allocated for this variable.
     * 
     * @return this variable's index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Get the name of this variable.
     * 
     * @return this variable's name
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieve the variable's value from the given object.
     * 
     * @param object the object from which to retrieve this variable
     * @return the variable's value
     */
    public Object get(Object object) {
        return getVariable((RubyBasicObject)object, index);
    }

    /**
     * Retrieve the variable's value from the given object. This version is
     * static, allowing it to be more direct, and accepts nnly with
     * RubyBasicObject and subclasses along with the direct index of the
     * variable.
     * 
     * @param object the object from which to retrieve the variable
     * @param index the index of the variable
     * @return the variable's value
     */
    public static Object getVariable(RubyBasicObject object, int index) {
		Object[] ivarTable;
        if (index < 0 || (ivarTable = object.varTable) == null) return null;
        if (ivarTable.length > index) return ivarTable[index];
        return null;
    }

    /**
     * Set the value for this variable in the given object.
     * 
     * @param object the object into which to set this variable
     * @param value the value of the variable
     */
    public void set(Object object, Object value) {
        ((RubyBasicObject)object).checkFrozen();
        VariableTableManager.setVariableInternal(realClass, (RubyBasicObject)object, index, value);
    }

    /**
     * Verify if this is the correct accessor for the given object.
     *
     * @param object the object for which to test this accessor
     * @return true if this is the correct accessor for the given object, false otherwise
     */
    public boolean verify(Object object) {
        return ((RubyBasicObject)object).getMetaClass().getRealClass() == this.realClass;
    }
    
    /** a dummy accessor that will always return null */
    public static final VariableAccessor DUMMY_ACCESSOR = new VariableAccessor(null, null, -1, -1);
    
}
