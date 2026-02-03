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

import com.headius.invokebinder.Binder;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.specialized.RubyObjectSpecializer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * A variable accessor that accesses a field directly;
 */
public class FieldVariableAccessor extends VariableAccessor {
    protected final MethodHandle getter;
    protected final MethodHandle setter;
    
    /**
     * Construct a new FieldVariableAccessor for the given "real" class,
     * variable name, variable index, class ID, and field offset
     * 
     * @param realClass the "real" class
     * @param name the variable's name
     * @param index the variable's index
     * @param classId the class's ID
     * @param getter the getter handle for the field
     * @param setter the setter handle for the field
     */
    public FieldVariableAccessor(RubyClass realClass, String name, int index, int classId, MethodHandle getter, MethodHandle setter) {
        super(realClass, name, index, classId);

        this.getter = getter;
        this.setter = wrapSetter(setter);
    }

    public FieldVariableAccessor cloneFor(RubyClass newRealClass) {
        return new FieldVariableAccessor(newRealClass, name, index, classId, getter, setter);
    }

    protected MethodHandle wrapSetter(MethodHandle setter) {
        // mix frozen check into setter
        return MethodHandles.foldArguments(setter, ENSURE_SETTABLE.asType(setter.type()));
    }

    public MethodHandle getGetter() {
        return getter;
    }

    public MethodHandle getSetter() {
        return setter;
    }

    /**
     * Retrieve the variable's value from the given object.
     *
     * @param object the object from which to retrieve this variable
     * @return the variable's value
     */
    public Object get(Object object) {
        try {
            return getter.invoke(object);
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null;
        }
    }

    /**
     * Retrieve the variable's value from the given object.
     *
     * @param object the object from which to retrieve this variable
     * @return the variable's value
     */
    public IRubyObject getOrNil(Object object, ThreadContext context) {
        try {
            Object value =  getter.invoke(object);
            return value == null ? context.nil : (IRubyObject) value;
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null;
        }
    }

    /**
     * Set this variable into the given object using Unsafe to ensure
     * safe updating of the variable table.
     *
     * @param object the object into which to set this variable
     * @param value the variable's value
     */
    public void set(Object object, Object value) {
        try {
            setter.invoke(object, value);
        } catch (Throwable t) {
            Helpers.throwException(t);
        }
    }

    private static final MethodHandle ENSURE_SETTABLE = Binder
            .from(Object.class, Object.class, Object.class)
            .dropLast()
            .cast(void.class, RubyBasicObject.class)
            .invokeVirtualQuiet(RubyObjectSpecializer.LOOKUP, "ensureInstanceVariablesSettable");
}
