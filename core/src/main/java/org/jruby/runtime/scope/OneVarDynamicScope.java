package org.jruby.runtime.scope;

import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * This is a DynamicScope that supports exactly three variables.
 */
public class OneVarDynamicScope extends NoVarsDynamicScope {
    private static final int SIZE = 1;
    private static final String SIZE_ERROR = "OneVarDynamicScope only supports scopes with one variable";
    private static final String GROW_ERROR = "OneVarDynamicScope cannot be grown; use ManyVarsDynamicScope";
    
    protected IRubyObject variableValueZero;

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
    public IRubyObject[] getValues() {
        return new IRubyObject[] {variableValueZero};
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
    public IRubyObject getValue(int offset, int depth) {
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
    public IRubyObject getValueOrNil(int offset, int depth, IRubyObject nil) {
        if (depth > 0) {
            return parent.getValueOrNil(offset, depth - 1, nil);
        } else {
            return getValueDepthZeroOrNil(offset, nil);
        }
    }
    
    @Override
    public IRubyObject getValueDepthZeroOrNil(int offset, IRubyObject nil) {
        IRubyObject value = variableValueZero;
        if (value == null) return variableValueZero = nil;
        return value;
    }
    @Override
    public IRubyObject getValueZeroDepthZeroOrNil(IRubyObject nil) {
        IRubyObject value = variableValueZero;
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
    public IRubyObject setValue(int offset, IRubyObject value, int depth) {
        if (depth > 0) {
            return parent.setValue(offset, value, depth - 1);
        } else {
            return variableValueZero = value;
        }
    }

    @Override
    public IRubyObject setValueDepthZero(IRubyObject value, int offset) {
        return variableValueZero = value;
    }
    @Override
    public IRubyObject setValueZeroDepthZero(IRubyObject value) {
        return variableValueZero = value;
    }
}
