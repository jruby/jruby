package org.jruby.runtime.scope;

import org.jruby.RubyArray;
import org.jruby.javasupport.util.RuntimeHelpers;
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
public class OneVarDynamicScope extends DynamicScope {
    // Our values holder (name of variables are kept in staticScope)
    private IRubyObject variableValue;

    public OneVarDynamicScope(StaticScope staticScope, DynamicScope parent) {
        super(staticScope, parent);
    }

    public OneVarDynamicScope(StaticScope staticScope) {
        super(staticScope);
    }
    
    public void growIfNeeded() {
        if (staticScope.getNumberOfVariables() != 1) {
            throw new RuntimeException("OneVarDynamicScope cannot be grown; use ManyVarsDynamicScope");
        }
    }
    
    public DynamicScope cloneScope() {
        return new OneVarDynamicScope(staticScope, parent);
    }

    public IRubyObject[] getValues() {
        return new IRubyObject[] {variableValue};
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
        assert offset == 0 : "SingleVarDynamicScope only supports scopes with one variable";
        return variableValue;
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
        assert offset == 0 : "SingleVarDynamicScope only supports scopes with one variable";
        if (variableValue == null) return variableValue = nil;
        return variableValue;
    }
    public IRubyObject getValueZeroDepthZeroOrNil(IRubyObject nil) {
        if (variableValue == null) return variableValue = nil;
        return variableValue;
    }
    public IRubyObject getValueOneDepthZeroOrNil(IRubyObject nil) {
        throw new RuntimeException("SingleVarDynamicScope only supports scopes with one variable");
    }
    public IRubyObject getValueTwoDepthZeroOrNil(IRubyObject nil) {
        throw new RuntimeException("SingleVarDynamicScope only supports scopes with one variable");
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
            assert parent != null : "If depth > 0, then parent should not ever be null";
            
            return parent.setValue(offset, value, depth - 1);
        } else {
            assert offset == 0 : "SingleVarDynamicScope only supports one variable";
            return variableValue = value;
        }
    }

    public IRubyObject setValueDepthZero(IRubyObject value, int offset) {
        assert offset == 0 : "SingleVarDynamicScope only supports one variable";
        return variableValue = value;
    }
    public IRubyObject setValueZeroDepthZero(IRubyObject value) {
        return variableValue = value;
    }
    public IRubyObject setValueOneDepthZero(IRubyObject value) {
        throw new RuntimeException("SingleVarDynamicScope only supports scopes with one variable");
    }
    public IRubyObject setValueTwoDepthZero(IRubyObject value) {
        throw new RuntimeException("SingleVarDynamicScope only supports scopes with one variable");
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
        assert values.length == 1 : "SingleVarDynamicScope only supports one variable";
        if (size == 1) {
            variableValue = values[0];
        }
    }

    @Override
    public IRubyObject[] getArgValues() {
        // if we're not the "argument scope" for zsuper, try our parent
        if (!staticScope.isArgumentScope()) {
            return parent.getArgValues();
        }
        int totalArgs = staticScope.getRequiredArgs() + staticScope.getOptionalArgs();
        assert totalArgs <= 1 : "OneVarDynamicScope only supports one variable";
        
        // copy and splat arguments out of the scope to use for zsuper call
        if (staticScope.getRestArg() < 0) {
            if (totalArgs == 1) {
                return new IRubyObject[] {variableValue};
            } else {
                return IRubyObject.NULL_ARRAY;
            }
        } else {
            // rest arg must be splatted
            IRubyObject restArg = getValue(staticScope.getRestArg(), 0);
            assert restArg != null;
            
            // FIXME: not very efficient
            RubyArray splattedArgs = RuntimeHelpers.splatValue(restArg);            
            IRubyObject[] argValues = new IRubyObject[totalArgs + splattedArgs.size()];
            System.arraycopy(splattedArgs.toJavaArray(), 0, argValues, totalArgs, splattedArgs.size());
            
            if (totalArgs == 1) {
                argValues[0] = variableValue;
            }
            
            return argValues;
        }
    }

    @Override
    public String toString(StringBuffer buf, String indent) {
        buf.append(indent).append("Static Type[" + hashCode() + "]: " + 
                (staticScope instanceof BlockStaticScope ? "block" : "local")+" [");
        
        String names[] = staticScope.getVariables();
        buf.append(names[0]).append("=");

        if (variableValue == null) {
            buf.append("null");
        } else {
            buf.append(variableValue);
        }
        
        buf.append("]");
        if (parent != null) {
            buf.append("\n");
            parent.toString(buf, indent + "  ");
        }
        
        return buf.toString();
    }
}
