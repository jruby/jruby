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

import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.util.ArraySupport;
import org.jruby.util.unsafe.UnsafeHolder;

/**
 * A VariableAccessor that directly updates instance variables without an explicit memory fence
 * or synchronization.
 *
 * If the table itself must be created for the first time or grown to accommodate a new variable,
 * those operations will be done in a thread-safe (volatile, atomic) way. However updates of an
 * entry in an existing table will not have any explicit memory fence or synchronization.
 */
public class NonvolatileVariableAccessor extends VariableAccessor {
    /**
     * Construct a new NonvolatileVariableAccessor for the given "real" class,
     * variable name, variable index, and class ID.
     *
     * @param realClass the "real" class
     * @param name the variable's name
     * @param index the variable's index
     * @param classId the class's ID
     */
    public NonvolatileVariableAccessor(RubyClass realClass, String name, int index, int classId) {
        super(realClass, name, index, classId);
    }

    /**
     * Set this variable into the given object using Unsafe to ensure
     * safe creation or growth of the variable table.
     *
     * @param object the object into which to set this variable
     * @param value the variable's value
     */
    public void set(Object object, Object value) {
        ((RubyBasicObject) object).ensureInstanceVariablesSettable();
        AtomicVariableTable.setVariableAtomic((RubyBasicObject) object, realClass, false, index, value);
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
        AtomicVariableTable.setVariableAtomic(self, realClass, false, index, value);
    }
}
