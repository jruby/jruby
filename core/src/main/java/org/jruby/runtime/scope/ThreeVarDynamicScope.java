package org.jruby.runtime.scope;

import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * This is a DynamicScope that supports exactly three variables.
 */
public class ThreeVarDynamicScope extends TwoVarDynamicScope {
    private static final int SIZE = 3;
    private static final String SIZE_ERROR = "ThreeVarDynamicScope only supports scopes with three variables";
    private static final String GROW_ERROR = "ThreeVarDynamicScope cannot be grown; use ManyVarsDynamicScope";
    
    protected IRubyObject variableValueTwo;

    public ThreeVarDynamicScope(StaticScope staticScope, DynamicScope parent) {
        super(staticScope, parent);
    }

    public ThreeVarDynamicScope(StaticScope staticScope) {
        super(staticScope);
    }
    
    @Override
    public void growIfNeeded() {
        growIfNeeded(SIZE, GROW_ERROR);
    }
    
    @Override
    public DynamicScope cloneScope() {
        return new ThreeVarDynamicScope(staticScope, parent);
    }

    @Override
    public IRubyObject[] getValues() {
        return new IRubyObject[] {variableValueZero, variableValueOne, variableValueTwo};
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
        switch (offset) {
        case 0:
            return variableValueZero;
        case 1:
            return variableValueOne;
        case 2:
            return variableValueTwo;
        default:
            throw new RuntimeException(SIZE_ERROR);
        }
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
        assert offset < SIZE : SIZE_ERROR;
        switch (offset) {
        case 0:
            if (variableValueZero == null) return variableValueZero = nil;
            return variableValueZero;
        case 1:
            if (variableValueOne == null) return variableValueOne = nil;
            return variableValueOne;
        case 2:
            if (variableValueTwo == null) return variableValueTwo = nil;
            return variableValueTwo;
        default:
            throw new RuntimeException(SIZE_ERROR);
        }
    }
    @Override
    public IRubyObject getValueTwoDepthZeroOrNil(IRubyObject nil) {
        if (variableValueTwo == null) return variableValueTwo = nil;
        return variableValueTwo;
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
            assert parent != null : "If depth > 0, then parent should not ever be null";
            
            return parent.setValue(offset, value, depth - 1);
        } else {
            assert offset < SIZE : SIZE_ERROR;
            switch (offset) {
            case 0:
                return variableValueZero = value;
            case 1:
                return variableValueOne = value;
            case 2:
                return variableValueTwo = value;
            default:
                throw new RuntimeException(SIZE_ERROR);
            }
        }
    }

    @Override
    public IRubyObject setValueDepthZero(IRubyObject value, int offset) {
        assert offset < SIZE : SIZE_ERROR;
        switch (offset) {
        case 0:
            return variableValueZero = value;
        case 1:
            return variableValueOne = value;
        case 2:
            return variableValueTwo = value;
        default:
            throw new RuntimeException(SIZE_ERROR);
        }
    }
    @Override
    public IRubyObject setValueTwoDepthZero(IRubyObject value) {
        return variableValueTwo = value;
    }
}
