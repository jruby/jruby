/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
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
package org.jruby;

/**
 * A RubyObject that provides a direct field for four stored variables, to avoid
 * the overhead of creating and managing a separate array and reference.
 */
public class RubyObjectVar3 extends ReifiedRubyObject {
    /**
     * Standard path for object creation. Objects are entered into ObjectSpace
     * only if ObjectSpace is enabled.
     */
    public RubyObjectVar3(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public Object getVariable(int i) {
        switch (i) {
            case 0: return var0;
            case 1: return var1;
            case 2: return var2;
            case 3: return var3;
            default: return super.getVariable(i);
        }
    }

    @Override
    public void setVariable(int index, Object value) {
        ensureInstanceVariablesSettable();
        switch (index) {
            case 0: var0 = value; break;
            case 1: var1 = value; break;
            case 2: var2 = value; break;
            case 3: var3 = value; break;
            default: super.setVariable(index, value);
        }
    }

    public Object getVariable0() {
        return var0;
    }

    public Object getVariable1() {
        return var1;
    }

    public Object getVariable2() {
        return var2;
    }

    public Object getVariable3() {
        return var3;
    }

    public void setVariable0(Object value) {
        ensureInstanceVariablesSettable();
        var0 = value;
    }

    public void setVariable1(Object value) {
        ensureInstanceVariablesSettable();
        var1 = value;
    }

    public void setVariable2(Object value) {
        ensureInstanceVariablesSettable();
        var2 = value;
    }

    public void setVariable3(Object value) {
        ensureInstanceVariablesSettable();
        var3 = value;
    }

    public Object var0;
    public Object var1;
    public Object var2;
    public Object var3;
}
