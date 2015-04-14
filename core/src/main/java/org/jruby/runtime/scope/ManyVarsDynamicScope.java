package org.jruby.runtime.scope;

import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Represents the the dynamic portion of scoping information.  The variableValues are the
 * values of assigned local or block variables.  The staticScope identifies which sort of
 * scope this is (block or local).
 * 
 * Properties of Dynamic Scopes:
 * 1. static and dynamic scopes have the same number of names to values
 * 2. size of variables (and thus names) is determined during parsing.  So those structured do
 *    not need to change
 *
 * FIXME: When creating dynamic scopes we sometimes accidentally pass in extra parents.  This
 * is harmless (other than wasting memory), but we should not do that.  We can fix this in two
 * ways:
 * 1. Fix all callers
 * 2. Check parent that is passed in and make if new instance is local, then its parent is not local
 */
public class ManyVarsDynamicScope extends DynamicScope {
    // Our values holder (name of variables are kept in staticScope)
    private IRubyObject[] variableValues;

    public ManyVarsDynamicScope(StaticScope staticScope, DynamicScope parent) {
        super(staticScope, parent);
        allocate();
    }

    public ManyVarsDynamicScope(StaticScope staticScope) {
        super(staticScope);
        allocate();
    }

    private void allocate() {
        if(variableValues == null) {
            int size = staticScope.getNumberOfVariables();
            variableValues = new IRubyObject[size];
        }
    }
    
    public DynamicScope cloneScope() {
        return new ManyVarsDynamicScope(staticScope, parent);
    }

    public IRubyObject[] getValues() {
        return variableValues;
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
    public IRubyObject getValue(int offset, int depth) {
        if (depth > 0) {
            return parent.getValue(offset, depth - 1);
        }
        assertGetValue(offset, depth);
        // &foo are not getting set from somewhere...I want the following assert to be true though
        //assert variableValues[offset] != null : "Getting unassigned: " + staticScope.getVariables()[offset];
        return variableValues[offset];
    }
    
    /**
     * Variation of getValue that checks for nulls, returning and setting the given value (presumably nil)
     */
    public IRubyObject getValueOrNil(int offset, int depth, IRubyObject nil) {
        if (depth > 0) {
            return parent.getValueOrNil(offset, depth - 1, nil);
        } else {
            return getValueDepthZeroOrNil(offset, nil);
        }
    }
    
    public IRubyObject getValueDepthZeroOrNil(int offset, IRubyObject nil) {
        assertGetValueDepthZeroOrNil(offset);
        // &foo are not getting set from somewhere...I want the following assert to be true though
        //assert variableValues[offset] != null : "Getting unassigned: " + staticScope.getVariables()[offset];
        IRubyObject value = variableValues[offset];
        return value == null ? setValueDepthZero(nil, offset) : value;
    }
    
    public IRubyObject getValueZeroDepthZeroOrNil(IRubyObject nil) {
        assertGetValueZeroDepthZeroOrNil();
        // &foo are not getting set from somewhere...I want the following assert to be true though
        //assert variableValues[offset] != null : "Getting unassigned: " + staticScope.getVariables()[offset];
        IRubyObject value = variableValues[0];
        return value == null ? setValueZeroDepthZero(nil) : value;
    }
    
    public IRubyObject getValueOneDepthZeroOrNil(IRubyObject nil) {
        assertGetValueOneDepthZeroOrNil();
        // &foo are not getting set from somewhere...I want the following assert to be true though
        //assert variableValues[offset] != null : "Getting unassigned: " + staticScope.getVariables()[offset];
        IRubyObject value = variableValues[1];
        return value == null ? setValueOneDepthZero(nil) : value;
    }
    
    public IRubyObject getValueTwoDepthZeroOrNil(IRubyObject nil) {
        assertGetValueTwoDepthZeroOrNil();
        // &foo are not getting set from somewhere...I want the following assert to be true though
        //assert variableValues[offset] != null : "Getting unassigned: " + staticScope.getVariables()[offset];
        IRubyObject value = variableValues[2];
        return value == null ? setValueTwoDepthZero(nil) : value;
    }
    
    public IRubyObject getValueThreeDepthZeroOrNil(IRubyObject nil) {
        assertGetValueThreeDepthZeroOrNil();
        // &foo are not getting set from somewhere...I want the following assert to be true though
        //assert variableValues[offset] != null : "Getting unassigned: " + staticScope.getVariables()[offset];
        IRubyObject value = variableValues[3];
        return value == null ? setValueThreeDepthZero(nil) : value;
    }

    /**
     * Set value in current dynamic scope or one of its captured scopes.
     * 
     * @param offset zero-indexed value that represents where variable lives
     * @param value to set
     * @param depth how many captured scopes down this variable should be set
     */
    public IRubyObject setValue(int offset, IRubyObject value, int depth) {
        if (depth > 0) {
            assertParent();
            
            return parent.setValue(offset, value, depth - 1);
        } else {
            assertSetValue(offset, value);
            
            return setValueDepthZero(value, offset);
        }
    }

    public IRubyObject setValueDepthZero(IRubyObject value, int offset) {
        assertSetValueDepthZero(offset, value);

        return variableValues[offset] = value;
    }
    public IRubyObject setValueZeroDepthZero(IRubyObject value) {
        assertSetValueZeroDepthZero(value);

        return variableValues[0] = value;
    }
    public IRubyObject setValueOneDepthZero(IRubyObject value) {
        assertSetValueOneDepthZero(value);

        return variableValues[1] = value;
    }
    public IRubyObject setValueTwoDepthZero(IRubyObject value) {
        assertSetValueTwoDepthZero(value);

        return variableValues[2] = value;
    }
    public IRubyObject setValueThreeDepthZero(IRubyObject value) {
        assertSetValueThreeDepthZero(value);

        return variableValues[3] = value;
    }

    /**
     * 
     * Make a larger dynamic scope if the static scope grew.
     * 
     * Eval's with bindings require us to possibly change the size of the dynamic scope if
     * things like 'eval "b = 2", binding' happens.
     *
     */
    public void growIfNeeded() {
        int dynamicSize = variableValues == null ? 0: variableValues.length;
        
        if (staticScope.getNumberOfVariables() > dynamicSize) {
            IRubyObject values[] = new IRubyObject[staticScope.getNumberOfVariables()];
            
            if (dynamicSize > 0) {
                System.arraycopy(variableValues, 0, values, 0, dynamicSize);
            }
            
            variableValues = values;
        }
    }

    private void assertGetValue(int offset, int depth) {
        IRubyObject[] values = variableValues;
        assert values != null && offset < values.length : "No variables or index to big for getValue off: " + offset + ", Dep: " + depth + ", O: " + this;
    }

    private void assertGetValueDepthZeroOrNil(int offset) {
        IRubyObject[] values = variableValues;
        assert values != null && offset < values.length : "No variables or index too big for getValue off: " + offset + ", Dep: " + 0 + ", O: " + this;
    }

    private void assertGetValueZeroDepthZeroOrNil() {
        IRubyObject[] values = variableValues;
        assert values != null && 0 < values.length : "No variables or index to big for getValue off: " + 0 + ", Dep: " + 0 + ", O: " + this;
    }

    private void assertGetValueOneDepthZeroOrNil() {
        IRubyObject[] values = variableValues;
        assert values != null && 1 < values.length : "No variables or index to big for getValue off: " + 1 + ", Dep: " + 0 + ", O: " + this;
    }

    private void assertGetValueTwoDepthZeroOrNil() {
        IRubyObject[] values = variableValues;
        assert values != null && 3 < values.length : "No variables or index to big for getValue off: " + 3 + ", Dep: " + 0 + ", O: " + this;
    }

    private void assertGetValueThreeDepthZeroOrNil() {
        IRubyObject[] values = variableValues;
        assert values != null && 2 < values.length : "No variables or index to big for getValue off: " + 2 + ", Dep: " + 0 + ", O: " + this;
    }

    private void assertParent() {
        assert parent != null : "If depth > 0, then parent should not ever be null";
    }

    private void assertSetValue(int offset, IRubyObject value) {
        assert offset < variableValues.length : "Setting " + offset + " to " + value + ", O: " + this;
    }

    private void assertSetValueDepthZero(int offset, IRubyObject value) {
        assert offset < variableValues.length : "Setting " + offset + " to " + value + ", O: " + this;
    }

    private void assertSetValueZeroDepthZero(IRubyObject value) {
        assert 0 < variableValues.length : "Setting " + 0 + " to " + value + ", O: " + this;
    }

    private void assertSetValueOneDepthZero(IRubyObject value) {
        assert 1 < variableValues.length : "Setting " + 1 + " to " + value + ", O: " + this;
    }

    private void assertSetValueThreeDepthZero(IRubyObject value) {
        assert 3 < variableValues.length : "Setting " + 3 + " to " + value + ", O: " + this;
    }

    private void assertSetValueTwoDepthZero(IRubyObject value) {
        assert 2 < variableValues.length : "Setting " + 2 + " to " + value + ", O: " + this;
    }
}
