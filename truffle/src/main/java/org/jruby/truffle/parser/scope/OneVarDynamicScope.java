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
package org.jruby.truffle.parser.scope;



/**
 * This is a DynamicScope that supports exactly three variables.
 */
public class OneVarDynamicScope extends NoVarsDynamicScope {
    private static final int SIZE = 1;
    private static final String SIZE_ERROR = "OneVarDynamicScope only supports scopes with one variable";
    private static final String GROW_ERROR = "OneVarDynamicScope cannot be grown; use ManyVarsDynamicScope";
    
    protected Object variableValueZero;

    public OneVarDynamicScope(StaticScope staticScope, DynamicScope parent) {
        super(staticScope, parent);
    }

    public OneVarDynamicScope(StaticScope staticScope) {
        super(staticScope);
    }
    
    @Override
    public void growIfNeeded() {
        growIfNeeded(SIZE, GROW_ERROR);
    }
    
    @Override
    public DynamicScope cloneScope() {
        return new OneVarDynamicScope(staticScope, parent);
    }

    @Override
    public Object[] getValues() {
        return new Object[] {variableValueZero};
    }
    
    /**
     * Get value from current scope or one of its captured scopes.
     * 
     * FIXME: block variables are not getting primed to nil so we need to null check those
     *  until we prime them properly.  Also add assert back in.
     * 
     * @param offset zero-indexed value that represents where variable lives
     * @param depth how many captured scopes down this variable should be set
     * @return the value here
     */
    @Override
    public Object getValue(int offset, int depth) {
        if (depth > 0) {
            return parent.getValue(offset, depth - 1);
        }
        assert offset < SIZE : SIZE_ERROR;
        return variableValueZero;
    }
    
    /**
     * Variation of getValue that checks for nulls, returning and setting the given value (presumably nil)
     */
    @Override
    public Object getValueOrNil(int offset, int depth, Object nil) {
        if (depth > 0) {
            return parent.getValueOrNil(offset, depth - 1, nil);
        } else {
            return getValueDepthZeroOrNil(offset, nil);
        }
    }
    
    @Override
    public Object getValueDepthZeroOrNil(int offset, Object nil) {
        Object value = variableValueZero;
        if (value == null) return variableValueZero = nil;
        return value;
    }
    @Override
    public Object getValueZeroDepthZeroOrNil(Object nil) {
        Object value = variableValueZero;
        if (value == null) return variableValueZero = nil;
        return value;
    }

    /**
     * Set value in current dynamic scope or one of its captured scopes.
     * 
     * @param offset zero-indexed value that represents where variable lives
     * @param value to set
     * @param depth how many captured scopes down this variable should be set
     */
    @Override
    public Object setValue(int offset, Object value, int depth) {
        if (depth > 0) {
            return parent.setValue(offset, value, depth - 1);
        } else {
            return variableValueZero = value;
        }
    }

    @Override
    public Object setValueDepthZero(Object value, int offset) {
        return variableValueZero = value;
    }
    @Override
    public Object setValueZeroDepthZero(Object value) {
        return variableValueZero = value;
    }
}
