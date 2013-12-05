package org.jruby.runtime.scope;

import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * This is a DynamicScope that does not support any variables.
 */
public class NoVarsDynamicScope extends DynamicScope {
    private static final int SIZE = 0;
    private static final String SIZE_ERROR = "NoVarsDynamicScope only supports scopes with one variable";
    private static final String GROW_ERROR = "NoVarsDynamicScope cannot be grown; use ManyVarsDynamicScope";
    
    public NoVarsDynamicScope(StaticScope staticScope, DynamicScope parent) {
        super(staticScope, parent);
    }

    public NoVarsDynamicScope(StaticScope staticScope) {
        super(staticScope);
    }
    
    public void growIfNeeded() {
        growIfNeeded(SIZE, GROW_ERROR);
    }

    protected void growIfNeeded(int size, String message) {
        if (staticScope.getNumberOfVariables() != size) {
            throw new RuntimeException(message);
        }
    }
    
    public DynamicScope cloneScope() {
        return new NoVarsDynamicScope(staticScope, parent);
    }

    public IRubyObject[] getValues() {
        return IRubyObject.NULL_ARRAY;
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
        assert depth != 0: SIZE_ERROR;
        return parent.getValue(offset, depth - 1);
    }
    
    /**
     * Variation of getValue that checks for nulls, returning and setting the given value (presumably nil)
     */
    public IRubyObject getValueOrNil(int offset, int depth, IRubyObject nil) {
        return parent.getValueOrNil(offset, depth - 1, nil);
    }
    
    public IRubyObject getValueDepthZeroOrNil(int offset, IRubyObject nil) {
        throw new RuntimeException(this.getClass().getSimpleName() + " does not support scopes with any variables");
    }
    public IRubyObject getValueZeroDepthZeroOrNil(IRubyObject nil) {
        throw new RuntimeException(this.getClass().getSimpleName() + " does not support scopes with one or more variables");
    }
    public IRubyObject getValueOneDepthZeroOrNil(IRubyObject nil) {
        throw new RuntimeException(this.getClass().getSimpleName() + " does not support scopes with two or more variables");
    }
    public IRubyObject getValueTwoDepthZeroOrNil(IRubyObject nil) {
        throw new RuntimeException(this.getClass().getSimpleName() + " does not support scopes with three or more variables");
    }
    public IRubyObject getValueThreeDepthZeroOrNil(IRubyObject nil) {
        throw new RuntimeException(this.getClass().getSimpleName() + " does not support scopes with four or more variables");
    }

    /**
     * Set value in current dynamic scope or one of its captured scopes.
     * 
     * @param offset zero-indexed value that represents where variable lives
     * @param value to set
     * @param depth how many captured scopes down this variable should be set
     */
    public IRubyObject setValue(int offset, IRubyObject value, int depth) {
        return parent.setValue(offset, value, depth - 1);
    }

    public IRubyObject setValueDepthZero(IRubyObject value, int offset) {
        throw new RuntimeException(this.getClass().getSimpleName() + " does not support scopes with any variables");
    }
    public IRubyObject setValueZeroDepthZero(IRubyObject value) {
        throw new RuntimeException(this.getClass().getSimpleName() + " does not support scopes with one or more variables");
    }
    public IRubyObject setValueOneDepthZero(IRubyObject value) {
        throw new RuntimeException(this.getClass().getSimpleName() + " does not support scopes with two or more variables");
    }
    public IRubyObject setValueTwoDepthZero(IRubyObject value) {
        throw new RuntimeException(this.getClass().getSimpleName() + " does not support scopes with three or more variables");
    }
    public IRubyObject setValueThreeDepthZero(IRubyObject value) {
        throw new RuntimeException(this.getClass().getSimpleName() + " does not support scopes with four or more variables");
    }

    /**
     * Set all values which represent 'normal' parameters in a call list to this dynamic
     * scope.  Function calls bind to local scopes by assuming that the indexes or the
     * arg list correspond to that of the local scope (plus 2 since $_ and $~ always take
     * the first two slots).  We pass in a second argument because we sometimes get more
     * values than we are expecting.  The rest get compacted by original caller into 
     * rest args.
     * 
     * @param values up to size specified to be mapped as ordinary parm values
     * @param size is the number of values to assign as ordinary parm values
     */
    public void setArgValues(IRubyObject[] values, int size) {
        assert size <= SIZE : this.getClass().getSimpleName() + " does not support scopes with " + size + " variables";
    }
    
    public void setArgValues(IRubyObject arg0) {
        throw new RuntimeException(this.getClass().getSimpleName() + " does not support scopes with 1 variable");
    }
    
    public void setArgValues(IRubyObject arg0, IRubyObject arg1) {
        throw new RuntimeException(this.getClass().getSimpleName() + " does not support scopes with 2 variables");
    }
    
    public void setArgValues(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        throw new RuntimeException(this.getClass().getSimpleName() + " does not support scopes with 3 variables");
    }

    public void setEndArgValues(IRubyObject[] values, int index, int size) {
        assert false : this.getClass().getSimpleName() + " does not support any variables";
    }

    @Override
    public IRubyObject[] getArgValues() {
        // if we're not the "argument scope" for zsuper, try our parent
        if (!staticScope.isArgumentScope()) {
            return parent.getArgValues();
        }
        int totalArgs = staticScope.getRequiredArgs() + staticScope.getOptionalArgs();
        assert totalArgs == 0 : this.getClass().getSimpleName() + " only supports scopes with no variables";
        
        return IRubyObject.NULL_ARRAY;
    }
}
