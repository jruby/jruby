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
public class ManyVarsDynamicScope extends DynamicScope {
    // Our values holder (name of variables are kept in staticScope)
    private IRubyObject[] variableValues;

    public ManyVarsDynamicScope(StaticScope staticScope, DynamicScope parent) {
        super(staticScope, parent);
    }

    public ManyVarsDynamicScope(StaticScope staticScope) {
        super(staticScope);
    }

    private void lazy() {
        if(variableValues == null) {
            int size = staticScope.getNumberOfVariables();
            variableValues = new IRubyObject[size];
        }
    }
    
    public DynamicScope cloneScope() {
        return new ManyVarsDynamicScope(staticScope, parent);
    }

    public IRubyObject[] getValues() {
        lazy();
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
        lazy();
        assert variableValues != null : "No variables in getValue for off: " + offset + ", Dep: " + depth;
        assert offset < variableValues.length : "Index to big for getValue off: " + offset + ", Dep: " + depth + ", O: " + this;
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
        lazy();
        assert variableValues != null : "No variables in getValue for off: " + offset + ", Dep: " + 0;
        assert offset < variableValues.length : "Index to big for getValue off: " + offset + ", Dep: " + 0 + ", O: " + this;
        // &foo are not getting set from somewhere...I want the following assert to be true though
        //assert variableValues[offset] != null : "Getting unassigned: " + staticScope.getVariables()[offset];
        IRubyObject value = variableValues[offset];
        if (value == null) {
            variableValues[offset] = nil;
            value = nil;
        }
        return value;
    }
    public IRubyObject getValueZeroDepthZeroOrNil(IRubyObject nil) {
        lazy();
        assert variableValues != null : "No variables in getValue for off: " + 0 + ", Dep: " + 0;
        assert 0 < variableValues.length : "Index to big for getValue off: " + 0 + ", Dep: " + 0 + ", O: " + this;
        // &foo are not getting set from somewhere...I want the following assert to be true though
        //assert variableValues[offset] != null : "Getting unassigned: " + staticScope.getVariables()[offset];
        IRubyObject value = variableValues[0];
        if (value == null) {
            variableValues[0] = nil;
            value = nil;
        }
        return value;
    }
    public IRubyObject getValueOneDepthZeroOrNil(IRubyObject nil) {
        lazy();
        assert variableValues != null : "No variables in getValue for off: " + 1 + ", Dep: " + 0;
        assert 1 < variableValues.length : "Index to big for getValue off: " + 1 + ", Dep: " + 0 + ", O: " + this;
        // &foo are not getting set from somewhere...I want the following assert to be true though
        //assert variableValues[offset] != null : "Getting unassigned: " + staticScope.getVariables()[offset];
        IRubyObject value = variableValues[1];
        if (value == null) {
            variableValues[1] = nil;
            value = nil;
        }
        return value;
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
            lazy();
            assert offset < variableValues.length : "Setting " + offset + " to " + value + ", O: " + this; 
            
            variableValues[offset] = value;
        }
    }

    public void setValueDepthZero(IRubyObject value, int offset) {
        lazy();
        assert offset < variableValues.length : "Setting " + offset + " to " + value + ", O: " + this; 

        variableValues[offset] = value;
    }
    public void setValueZeroDepthZero(IRubyObject value) {
        lazy();
        assert 0 < variableValues.length : "Setting " + 0 + " to " + value + ", O: " + this; 

        variableValues[0] = value;
    }
    public void setValueOneDepthZero(IRubyObject value) {
        lazy();
        assert 1 < variableValues.length : "Setting " + 1 + " to " + value + ", O: " + this; 

        variableValues[1] = value;
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
        lazy();
        System.arraycopy(values, 0, variableValues, 0, size);
    }

    /**
     * Copy variable values back for ZSuper call.
     */
    public IRubyObject[] getArgValues() {
        // if we're not the "argument scope" for zsuper, try our parent
        if (!staticScope.isArgumentScope()) {
            return parent.getArgValues();
        }
        lazy();
        int totalArgs = staticScope.getRequiredArgs() + staticScope.getOptionalArgs();
        
        // copy and splat arguments out of the scope to use for zsuper call
        if (staticScope.getRestArg() < 0) {
            // required and optional only
            IRubyObject[] argValues = new IRubyObject[totalArgs];
            System.arraycopy(variableValues, 0, argValues, 0, totalArgs);
            
            return argValues;
        } else {
            // rest arg must be splatted
            IRubyObject restArg = getValue(staticScope.getRestArg(), 0);
            assert restArg != null;
            
            // FIXME: not very efficient
            RubyArray splattedArgs = ASTInterpreter.splatValue(restArg.getRuntime(), restArg);            
            IRubyObject[] argValues = new IRubyObject[totalArgs + splattedArgs.size()];
            System.arraycopy(variableValues, 0, argValues, 0, totalArgs);
            System.arraycopy(splattedArgs.toJavaArray(), 0, argValues, totalArgs, splattedArgs.size());
            
            return argValues;
        }
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
        lazy();
        int dynamicSize = variableValues == null ? 0: variableValues.length;
        
        if (staticScope.getNumberOfVariables() > dynamicSize) {
            IRubyObject values[] = new IRubyObject[staticScope.getNumberOfVariables()];
            
            if (dynamicSize > 0) {
                System.arraycopy(variableValues, 0, values, 0, dynamicSize);
            }
            
            variableValues = values;
        }
    }

    // Helper function to give a good view of current dynamic scope with captured scopes
    public String toString(StringBuffer buf, String indent) {
        lazy();
        buf.append(indent).append("Static Type[" + hashCode() + "]: " + 
                (staticScope instanceof BlockStaticScope ? "block" : "local")+" [");
        int size = staticScope.getNumberOfVariables();
        
        if (size != 0) {
            String names[] = staticScope.getVariables();
            for (int i = 0; i < size-1; i++) {
                buf.append(names[i]).append("=");

                if (variableValues[i] == null) {
                    buf.append("null");
                } else {
                    buf.append(variableValues[i]);
                }
                
                buf.append(",");
            }
            buf.append(names[size-1]).append("=");
            
            assert variableValues.length == names.length : "V: " + variableValues.length + 
                " != N: " + names.length + " for " + buf;
            
            if (variableValues[size-1] == null) {
                buf.append("null");
            } else {
                buf.append(variableValues[size-1]);
            }
            
        }
        
        buf.append("]");
        if (parent != null) {
            buf.append("\n");
            parent.toString(buf, indent + "  ");
        }
        
        return buf.toString();
    }
}
