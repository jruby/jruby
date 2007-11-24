package org.jruby.runtime;

import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

public abstract class DynamicScope {
    // Static scoping information for this scope
    protected StaticScope staticScope;
    
    // Captured dyanmic scopes
    protected DynamicScope parent;
    
    // A place to store that special hiding space that bindings need to implement things like:
    // eval("a = 1", binding); eval("p a").  All binding instances must get access to this
    // hidden shared scope.  We store it here.  This will be null if no binding has yet
    // been called.
    protected DynamicScope bindingScope;

    protected DynamicScope(StaticScope staticScope, DynamicScope parent) {
        this(staticScope);
        this.parent = parent;
    }

    protected DynamicScope(StaticScope staticScope) {
        this.staticScope = staticScope;
    }
    
    public static DynamicScope newDynamicScope(StaticScope staticScope, DynamicScope parent) {
        if (staticScope.getNumberOfVariables() == 1) {
            return new OneVarDynamicScope(staticScope, parent);
        } else {
            return new ManyVarsDynamicScope(staticScope, parent);
        }
    }
    
    public static DynamicScope newDynamicScope(StaticScope staticScope) {
        if (staticScope.getNumberOfVariables() == 1) {
            return new OneVarDynamicScope(staticScope);
        } else {
            return new ManyVarsDynamicScope(staticScope);
        }
    }
    
    public DynamicScope getBindingScope() {
        return bindingScope;
    }
    
    public void setBindingScope(DynamicScope bindingScope) {
        this.bindingScope = bindingScope;
    }
    
    /**
     * Get next 'captured' scope.
     * 
     * @return the scope captured by this scope for implementing closures
     *
     */
    public DynamicScope getNextCapturedScope() {
        return parent;
    }

    /**
     * Get the static scope associated with this DynamicScope.
     * 
     * @return static complement to this scope
     */
    public StaticScope getStaticScope() {
        return staticScope;
    }
    
    /**
     * Get all variable names captured (visible) by this scope (sans $~ and $_).
     * 
     * @return a list of variable names
     */
    public String[] getAllNamesInScope() {
        return staticScope.getAllNamesInScope();
    }
    
    public String toString() {
        return toString(new StringBuffer(), "");
    }
    
    public abstract void growIfNeeded();

    // Helper function to give a good view of current dynamic scope with captured scopes
    protected abstract String toString(StringBuffer buf, String indent);
    
    public abstract DynamicScope cloneScope();

    public abstract IRubyObject[] getValues();
    
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
    public abstract IRubyObject getValue(int offset, int depth);
    
    /**
     * Variation of getValue that checks for nulls, returning and setting the given value (presumably nil)
     */
    public abstract IRubyObject getValueOrNil(int offset, int depth, IRubyObject nil);
    
    /**
     * Variation of getValue that checks for nulls, returning and setting the given value (presumably nil)
     */
    public abstract IRubyObject getValueZeroDepthZeroOrNil(IRubyObject nil);

    /**
     * Set value in current dynamic scope or one of its captured scopes.
     * 
     * @param offset zero-indexed value that represents where variable lives
     * @param value to set
     * @param depth how many captured scopes down this variable should be set
     */
    public abstract void setValue(int offset, IRubyObject value, int depth);

    /**
     * Set value in current dynamic scope or one of its captured scopes.
     * 
     * @param offset zero-indexed value that represents where variable lives
     * @param value to set
     * @param depth how many captured scopes down this variable should be set
     */
    public abstract void setValueZeroDepthZero(IRubyObject value);

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
    public abstract void setArgValues(IRubyObject[] values, int size);
    
    public abstract void setBlockArgValues(IRubyObject[] blockArgValues, int size);

    /**
     * Copy variable values back for ZSuper call.
     */
    public abstract IRubyObject[] getArgValues();
}
