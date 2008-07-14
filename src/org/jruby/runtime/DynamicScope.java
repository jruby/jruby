package org.jruby.runtime;

import org.jruby.runtime.scope.ManyVarsDynamicScope;
import org.jruby.runtime.scope.NoVarsDynamicScope;
import org.jruby.runtime.scope.OneVarDynamicScope;
import org.jruby.parser.EvalStaticScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.ThreeVarDynamicScope;
import org.jruby.runtime.scope.TwoVarDynamicScope;

public abstract class DynamicScope {
    // Static scoping information for this scope
    protected StaticScope staticScope;
    
    // Captured dyanmic scopes
    protected DynamicScope parent;
    
    // A place to store that special hiding space that bindings need to implement things like:
    // eval("a = 1", binding); eval("p a").  All binding instances must get access to this
    // hidden shared scope.  We store it here.  This will be null if no binding has yet
    // been called.
    protected DynamicScope evalScope;

    protected DynamicScope(StaticScope staticScope, DynamicScope parent) {
        this(staticScope);
        this.parent = parent;
    }

    protected DynamicScope(StaticScope staticScope) {
        this.staticScope = staticScope;
    }
    
    public static DynamicScope newDynamicScope(StaticScope staticScope, DynamicScope parent) {
        switch (staticScope.getNumberOfVariables()) {
        case 0:
            return new NoVarsDynamicScope(staticScope, parent);
        case 1:
            return new OneVarDynamicScope(staticScope, parent);
        case 2:
            return new TwoVarDynamicScope(staticScope, parent);
        case 3:
            return new ThreeVarDynamicScope(staticScope, parent);
        default:
            return new ManyVarsDynamicScope(staticScope, parent);
        }
    }
    
    public static DynamicScope newDummyScope(StaticScope staticScope, DynamicScope parent) {
        return new ManyVarsDynamicScope(staticScope, parent);
    }

    /**
     * Returns the n-th parent scope of this scope.
     * May return <code>null</code>.
     * @param n - number of levels above to look.
     * @return The n-th parent scope or <code>null</code>.
     */
    public DynamicScope getNthParentScope(int n) {
        DynamicScope scope = this;
        for (int i = 0; i < n; i++) {
            if (scope != null) {
                scope = scope.getNextCapturedScope();
            } else {
                break;
            }
        }
        return scope;
    }

    public static DynamicScope newDynamicScope(StaticScope staticScope) {
        return newDynamicScope(staticScope, null);
    }
    
    public final DynamicScope getEvalScope() {
        // We create one extra dynamicScope on a binding so that when we 'eval "b=1", binding' the
        // 'b' will get put into this new dynamic scope.  The original scope does not see the new
        // 'b' and successive evals with this binding will.  I take it having the ability to have 
        // succesive binding evals be able to share same scope makes sense from a programmers 
        // perspective.   One crappy outcome of this design is it requires Dynamic and Static 
        // scopes to be mutable for this one case.
        
        // Note: In Ruby 1.9 all of this logic can go away since they will require explicit
        // bindings for evals.
        
        // We only define one special dynamic scope per 'logical' binding.  So all bindings for
        // the same scope should share the same dynamic scope.  This allows multiple evals with
        // different different bindings in the same scope to see the same stuff.
        
        // No binding scope so we should create one
        if (evalScope == null) {
            // If the next scope out has the same binding scope as this scope it means
            // we are evaling within an eval and in that case we should be sharing the same
            // binding scope.
            DynamicScope parent = getNextCapturedScope(); 
            if (parent != null && parent.getEvalScope() == this) {
                evalScope = this;
            } else {
                // bindings scopes must always be ManyVars scopes since evals can grow them
                evalScope = new ManyVarsDynamicScope(new EvalStaticScope(getStaticScope()), this);
            }
        }
        
        return evalScope;
    }
    
    /**
     * Find the scope to use for flip-flops. Flip-flops live either in the
     * topmost "method scope" or in their nearest containing "eval scope".
     * 
     * @return The scope to use for flip-flops
     */
    public DynamicScope getFlipScope() {
        if (staticScope.getLocalScope() == staticScope) {
            return this;
        } else {
            return parent.getFlipScope();
        }
    }
    
    /**
     * Get next 'captured' scope.
     * 
     * @return the scope captured by this scope for implementing closures
     *
     */
    public final DynamicScope getNextCapturedScope() {
        return parent;
    }

    /**
     * Get the static scope associated with this DynamicScope.
     * 
     * @return static complement to this scope
     */
    public final StaticScope getStaticScope() {
        return staticScope;
    }
    
    /**
     * Get all variable names captured (visible) by this scope (sans $~ and $_).
     * 
     * @return a list of variable names
     */
    public final String[] getAllNamesInScope() {
        return staticScope.getAllNamesInScope();
    }
    
    public String toString() {
        return toString(new StringBuffer(), "");
    }
    
    public abstract void growIfNeeded();

    // Helper function to give a good view of current dynamic scope with captured scopes
    public abstract String toString(StringBuffer buf, String indent);
    
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
     * getValueOrNil for depth 0
     */
    public abstract IRubyObject getValueDepthZeroOrNil(int offset, IRubyObject nil);
    
    /**
     * getValueOrNil for index 0, depth 0
     */
    public abstract IRubyObject getValueZeroDepthZeroOrNil(IRubyObject nil);
    
    /**
     * getValueOrNil for index 1, depth 0
     */
    public abstract IRubyObject getValueOneDepthZeroOrNil(IRubyObject nil);
    
    /**
     * getValueOrNil for index 1, depth 0
     */
    public abstract IRubyObject getValueTwoDepthZeroOrNil(IRubyObject nil);

    /**
     * Set value in current dynamic scope or one of its captured scopes.
     * 
     * @param offset zero-indexed value that represents where variable lives
     * @param value to set
     * @param depth how many captured scopes down this variable should be set
     */
    public abstract IRubyObject setValue(int offset, IRubyObject value, int depth);

    /**
     * setValue for depth zero
     * 
     * @param offset zero-indexed value that represents where variable lives
     * @param value to set
     * @param depth how many captured scopes down this variable should be set
     */
    public abstract IRubyObject setValueDepthZero(IRubyObject value, int offset);

    /**
     * Set value zero in this scope;
     */
    public abstract IRubyObject setValueZeroDepthZero(IRubyObject value);

    /**
     * Set value one in this scope.
     */
    public abstract IRubyObject setValueOneDepthZero(IRubyObject value);

    /**
     * Set value two in this scope.
     */
    public abstract IRubyObject setValueTwoDepthZero(IRubyObject value);

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

    /**
     * Copy variable values back for ZSuper call.
     */
    public abstract IRubyObject[] getArgValues();
}
