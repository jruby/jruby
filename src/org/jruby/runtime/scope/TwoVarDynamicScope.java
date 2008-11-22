package org.jruby.runtime.scope;

import org.jruby.RubyArray;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.parser.BlockStaticScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * This is a DynamicScope that supports exactly three variables.
 */
public class TwoVarDynamicScope extends OneVarDynamicScope {
    protected IRubyObject variableValueOne;

    public TwoVarDynamicScope(StaticScope staticScope, DynamicScope parent) {
        super(staticScope, parent);
    }

    public TwoVarDynamicScope(StaticScope staticScope) {
        super(staticScope);
    }
    
    @Override
    public void growIfNeeded() {
        if (staticScope.getNumberOfVariables() != 2) {
            throw new RuntimeException("TwoVarDynamicScope cannot be grown; use ManyVarsDynamicScope");
        }
    }
    
    @Override
    public DynamicScope cloneScope() {
        return new TwoVarDynamicScope(staticScope, parent);
    }

    @Override
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
    @Override
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
    
    @Override
    public IRubyObject getValueDepthZeroOrNil(int offset, IRubyObject nil) {
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
    @Override
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
    @Override
    public IRubyObject setValue(int offset, IRubyObject value, int depth) {
        if (depth > 0) {
            assert parent != null : "If depth > 0, then parent should not ever be null";
            
            return parent.setValue(offset, value, depth - 1);
        } else {
            assert offset < 2 : "TwoVarDynamicScope only supports scopes with two variables";
            switch (offset) {
            case 0:
                return variableValueZero = value;
            case 1:
                return variableValueOne = value;
            default:
                throw new RuntimeException("TwoVarDynamicScope only supports scopes with two variables");
            }
        }
    }

    @Override
    public IRubyObject setValueDepthZero(IRubyObject value, int offset) {
        assert offset < 2 : "TwoVarDynamicScope only supports scopes with two variables";
        switch (offset) {
        case 0:
            return variableValueZero = value;
        case 1:
            return variableValueOne = value;
        default:
            throw new RuntimeException("TwoVarDynamicScope only supports scopes with two variables");
        }
    }
    @Override
    public IRubyObject setValueOneDepthZero(IRubyObject value) {
        return variableValueOne = value;
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
    @Override
    public void setArgValues(IRubyObject[] values, int size) {
        assert size <= 2 : "TwoVarDynamicScope only supports scopes with two variables, not " + size;
        switch (size) {
        case 2:
            variableValueOne = values[1];
        case 1:
            variableValueZero = values[0];
        }
    }

    @Override
    public void setEndArgValues(IRubyObject[] values, int index, int size) {
        assert size <= 2 : "TwoVarDynamicScope only supports scopes with two variables, not " + size;

        switch (size) {
        case 2:
            variableValueZero = values[values.length - 2];
        case 1:
            variableValueOne = values[values.length - 1];
        }
    }
    
    @Override
    public void setArgValues(IRubyObject arg0) {
        variableValueZero = arg0;
    }
    
    @Override
    public void setArgValues(IRubyObject arg0, IRubyObject arg1) {
        variableValueZero = arg0;
        variableValueOne = arg1;
    }
    
    @Override
    public void setArgValues(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        assert false : "TwoVarDynamicScope only supports two variables not three";
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
            switch (totalArgs) {
            case 0:
                return IRubyObject.NULL_ARRAY;
            case 1:
                return new IRubyObject[] {variableValueZero};
            case 2:
                return new IRubyObject[] {variableValueZero, variableValueOne};
            default:
                throw new RuntimeException("more args requested than available variables");
            }
        } else {
            // rest arg must be splatted
            IRubyObject restArg = getValue(staticScope.getRestArg(), 0);
            assert restArg != null;
            
            // FIXME: not very efficient
            RubyArray splattedArgs = RuntimeHelpers.splatValue(restArg);            
            IRubyObject[] argValues = new IRubyObject[totalArgs + splattedArgs.size()];
            System.arraycopy(splattedArgs.toJavaArray(), 0, argValues, totalArgs, splattedArgs.size());
            switch (totalArgs) {
            case 2:
                argValues[1] = variableValueOne;
            case 1:
                argValues[0] = variableValueZero;
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
