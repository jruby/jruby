package org.jruby.runtime.scope;

import org.jruby.parser.BlockStaticScope;
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
public class NoVarsDynamicScope extends DynamicScope {
    public NoVarsDynamicScope(StaticScope staticScope, DynamicScope parent) {
        super(staticScope, parent);
    }

    public NoVarsDynamicScope(StaticScope staticScope) {
        super(staticScope);
    }
    
    public void growIfNeeded() {
        assert staticScope.getNumberOfVariables() == 0 : "NoVarsDynamicScope cannot be grown; use ManyVarsDynamicScope";
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
        return parent.getValue(offset, depth - 1);
    }
    
    /**
     * Variation of getValue that checks for nulls, returning and setting the given value (presumably nil)
     */
    public IRubyObject getValueOrNil(int offset, int depth, IRubyObject nil) {
        return parent.getValueOrNil(offset, depth - 1, nil);
    }
    
    public IRubyObject getValueDepthZeroOrNil(int offset, IRubyObject nil) {
        assert false : "NoVarsDynamicScope only supports scopes with no variables";
        return null;
    }
    public IRubyObject getValueZeroDepthZeroOrNil(IRubyObject nil) {
        assert false : "NoVarsDynamicScope only supports scopes with no variables";
        return null;
    }
    public IRubyObject getValueOneDepthZeroOrNil(IRubyObject nil) {
        assert false : "NoVarsDynamicScope only supports scopes with no variables";
        return null;
    }
    public IRubyObject getValueTwoDepthZeroOrNil(IRubyObject nil) {
        assert false : "NoVarsDynamicScope only supports scopes with no variables";
        return null;
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
        throw new RuntimeException("NoVarsDynamicScope only supports scopes with no variables");
    }
    public IRubyObject setValueZeroDepthZero(IRubyObject value) {
        throw new RuntimeException("NoVarsDynamicScope only supports scopes with no variables");
    }
    public IRubyObject setValueOneDepthZero(IRubyObject value) {
        throw new RuntimeException("NoVarsDynamicScope only supports scopes with no variables");
    }
    public IRubyObject setValueTwoDepthZero(IRubyObject value) {
        throw new RuntimeException("NoVarsDynamicScope only supports scopes with no variables");
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
        assert size == 0 : "NoVarsDynamicScope only supports scopes with no variables";
    }

    @Override
    public IRubyObject[] getArgValues() {
        // if we're not the "argument scope" for zsuper, try our parent
        if (!staticScope.isArgumentScope()) {
            return parent.getArgValues();
        }
        int totalArgs = staticScope.getRequiredArgs() + staticScope.getOptionalArgs();
        assert totalArgs == 0 : "NoVarsDynamicScope only supports scopes with no variables";
        
        return IRubyObject.NULL_ARRAY;
    }

    @Override
    public String toString(StringBuffer buf, String indent) {
        buf.append(indent).append("Static Type[" + hashCode() + "]: " + 
                (staticScope instanceof BlockStaticScope ? "block" : "local")+" []");
        
        if (parent != null) {
            buf.append("\n");
            parent.toString(buf, indent + "  ");
        }
        
        return buf.toString();
    }
}
