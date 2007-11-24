package org.jruby.runtime.scope;

import org.jruby.runtime.*;
import org.jruby.RubyArray;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.parser.BlockStaticScope;
import org.jruby.parser.StaticScope;
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
public class TwoVarDynamicScope extends DynamicScope {
    private IRubyObject variableValueZero;
    private IRubyObject variableValueOne;

    public TwoVarDynamicScope(StaticScope staticScope, DynamicScope parent) {
        super(staticScope, parent);
    }

    public TwoVarDynamicScope(StaticScope staticScope) {
        super(staticScope);
    }
    
    public void growIfNeeded() {
        if (staticScope.getNumberOfVariables() != 2) {
            throw new RuntimeException("TwoVarDynamicScope cannot be grown; use ManyVarsDynamicScope");
        }
    }
    
    public DynamicScope cloneScope() {
        return new TwoVarDynamicScope(staticScope, parent);
    }

    public IRubyObject[] getValues() {
        return new IRubyObject[] {variableValueZero, variableValueOne};
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
        assert offset < 2 : "TwoVarDynamicScope only supports scopes with two variables";
        switch (offset) {
        case 0:
            return variableValueZero;
        case 1:
            return variableValueOne;
        default:
            throw new RuntimeException("TwoVarDynamicScope only supports scopes with two variables");
        }
    }
    
    /**
     * Variation of getValue that checks for nulls, returning and setting the given value (presumably nil)
     */
    public IRubyObject getValueOrNil(int offset, int depth, IRubyObject nil) {
        if (depth > 0) {
            return parent.getValueOrNil(offset, depth - 1, nil);
        } else {
            assert offset < 2 : "TwoVarDynamicScope only supports scopes with two variables";
            switch (offset) {
            case 0:
                if (variableValueZero == null) return variableValueZero = nil;
                return variableValueZero;
            case 1:
                if (variableValueOne == null) return variableValueOne = nil;
                return variableValueOne;
            default:
                throw new RuntimeException("TwoVarDynamicScope only supports scopes with two variables");
            }
        }
    }
    
    public IRubyObject getValueZeroDepthZeroOrNil(IRubyObject nil) {
        if (variableValueZero == null) return variableValueZero = nil;
        return variableValueZero;
    }
    public IRubyObject getValueOneDepthZeroOrNil(IRubyObject nil) {
        if (variableValueOne == null) return variableValueOne = nil;
        return variableValueOne;
    }

    /**
     * Set value in current dynamic scope or one of its captured scopes.
     * 
     * @param offset zero-indexed value that represents where variable lives
     * @param value to set
     * @param depth how many captured scopes down this variable should be set
     */
    public void setValue(int offset, IRubyObject value, int depth) {
        if (depth > 0) {
            assert parent != null : "If depth > 0, then parent should not ever be null";
            
            parent.setValue(offset, value, depth - 1);
        } else {
            assert offset < 2 : "TwoVarDynamicScope only supports scopes with two variables";
            switch (offset) {
            case 0:
                variableValueZero = value;
            case 1:
                variableValueOne = value;
            default:
                throw new RuntimeException("TwoVarDynamicScope only supports scopes with two variables");
            }
        }
    }

    public void setValueZeroDepthZero(IRubyObject value) {
        variableValueZero = value;
    }
    public void setValueOneDepthZero(IRubyObject value) {
        variableValueOne = value;
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
        assert values.length == 2 : "TwoVarDynamicScope only supports scopes with two variables";
        variableValueZero = values[0];
        variableValueZero = values[1];
    }

    @Override
    public IRubyObject[] getArgValues() {
        // if we're not the "argument scope" for zsuper, try our parent
        if (!staticScope.isArgumentScope()) {
            return parent.getArgValues();
        }
        int totalArgs = staticScope.getRequiredArgs() + staticScope.getOptionalArgs();
        assert totalArgs <= 2 : "TwoVarDynamicScope only supports scopes with two variables";
        
        // copy and splat arguments out of the scope to use for zsuper call
        if (staticScope.getRestArg() < 0) {
            if (totalArgs == 1) {
                return new IRubyObject[] {variableValueZero, variableValueOne};
            } else {
                return IRubyObject.NULL_ARRAY;
            }
        } else {
            // rest arg must be splatted
            IRubyObject restArg = getValue(staticScope.getRestArg(), 0);
            assert restArg != null;
            
            // FIXME: not very efficient
            RubyArray splattedArgs = ASTInterpreter.splatValue(restArg.getRuntime(), restArg);            
            IRubyObject[] argValues = new IRubyObject[totalArgs + splattedArgs.size()];
            System.arraycopy(splattedArgs.toJavaArray(), 0, argValues, totalArgs, splattedArgs.size());
            
            return argValues;
        }
    }

    @Override
    public String toString(StringBuffer buf, String indent) {
        buf.append(indent).append("Static Type[" + hashCode() + "]: " + 
                (staticScope instanceof BlockStaticScope ? "block" : "local")+" [");
        
        String names[] = staticScope.getVariables();
        buf.append(names[0]).append("=");

        if (variableValueZero == null) {
            buf.append("null");
        } else {
            buf.append(variableValueZero);
        }
        
        buf.append(",");

        if (variableValueOne == null) {
            buf.append("null");
        } else {
            buf.append(variableValueOne);
        }
        
        buf.append("]");
        if (parent != null) {
            buf.append("\n");
            parent.toString(buf, indent + "  ");
        }
        
        return buf.toString();
    }
}
