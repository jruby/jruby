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
 * A RubyObject that provides a direct field for up to ten stored variables, to avoid
 * the overhead of creating and managing a separate array and reference.
 */
public abstract class ReifiedRubyObject extends RubyObject {
    public ReifiedRubyObject(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    @Override
    public Object getVariable(int i) {
        return super.getVariable(i);
    }

    @Override
    public void setVariable(int index, Object value) {
        super.setVariable(index, value);
    }

    public Object getVariable0() {
        return getVariable(0);
    }

    public Object getVariable1() {
        return getVariable(1);
    }

    public Object getVariable2() {
        return getVariable(2);
    }

    public Object getVariable3() {
        return getVariable(3);
    }

    public Object getVariable4() {
        return getVariable(4);
    }

    public Object getVariable5() {
        return getVariable(5);
    }

    public Object getVariable6() {
        return getVariable(6);
    }

    public Object getVariable7() {
        return getVariable(7);
    }

    public Object getVariable8() {
        return getVariable(8);
    }

    public Object getVariable9() {
        return getVariable(9);
    }

    public void setVariable0(Object value) {
        setVariable(0, value);
    }

    public void setVariable1(Object value) {
        setVariable(1, value);
    }

    public void setVariable2(Object value) {
        setVariable(2, value);
    }

    public void setVariable3(Object value) {
        setVariable(3, value);
    }

    public void setVariable4(Object value) {
        setVariable(4, value);
    }

    public void setVariable5(Object value) {
        setVariable(5, value);
    }

    public void setVariable6(Object value) {
        setVariable(6, value);
    }

    public void setVariable7(Object value) {
        setVariable(7, value);
    }

    public void setVariable8(Object value) {
        setVariable(8, value);
    }

    public void setVariable9(Object value) {
        setVariable(9, value);
    }
}
