package org.jruby.runtime.scope;

import org.jruby.RubyArray;
import org.jruby.runtime.Helpers;
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
    
    protected Object variableValueZero;

    public OneVarDynamicScope(StaticScope staticScope, DynamicScope parent) {
        super(staticScope, parent);
    }

    public OneVarDynamicScope(StaticScope staticScope) {
        super(staticScope);
    }
    
    @Override
    public void growIfNeeded() {
        errorOnInvalidGrow(SIZE, GROW_ERROR);
    }
    
    @Override
    public DynamicScope cloneScope() {
        return new OneVarDynamicScope(staticScope, parent);
    }

    @Override
    public IRubyObject[] getValues() {
        return new IRubyObject[] {(IRubyObject)variableValueZero};
    }

    @Override
    public Object[] getObjectValues() {
        return new Object[] {variableValueZero};
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
        return (IRubyObject)getObjectValue(offset, depth);
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
    public Object getObjectValue(int offset, int depth) {
        if (depth > 0) {
            return parent.getObjectValue(offset, depth - 1);
        }
        assert offset < SIZE : SIZE_ERROR;
        switch (offset) {
            case 0:
                return variableValueZero;
            default:
                throw new RuntimeException(SIZE_ERROR);
        }
    }
    
    @Override
    public IRubyObject getValueDepthZeroOrNil(int offset, IRubyObject nil) {
        return (IRubyObject)getObjectValueDepthZeroOrDefault(offset, nil);
    }

    @Override
    public Object getObjectValueDepthZeroOrDefault(int offset, Object defval) {
        assert offset < SIZE : SIZE_ERROR;
        switch (offset) {
            case 0:
                if (variableValueZero == null) return variableValueZero = defval;
                return variableValueZero;
            default:
                throw new RuntimeException(SIZE_ERROR);
        }
    }

    @Override
    public IRubyObject getValueZeroDepthZeroOrNil(IRubyObject nil) {
        return (IRubyObject)getObjectValueZeroDepthZeroOrDefault(nil);
    }

    @Override
    public Object getObjectValueZeroDepthZeroOrDefault(Object defval) {
        if (variableValueZero == null) return variableValueZero = defval;
        return variableValueZero;
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
        return (IRubyObject)setObjectValue(offset, value, depth);
    }

    /**
     * Set value in current dynamic scope or one of its captured scopes.
     *
     * @param offset zero-indexed value that represents where variable lives
     * @param value to set
     * @param depth how many captured scopes down this variable should be set
     */
    @Override
    public Object setObjectValue(int offset, Object value, int depth) {
        if (depth > 0) {
            assert parent != null : "If depth > 0, then parent should not ever be null";

            return parent.setObjectValue(offset, value, depth - 1);
        } else {
            assert offset < SIZE : SIZE_ERROR;
            switch (offset) {
                case 0:
                    return variableValueZero = value;
                default:
                    throw new RuntimeException(SIZE_ERROR);
            }
        }
    }

    @Override
    public IRubyObject setValueDepthZero(IRubyObject value, int offset) {
        return (IRubyObject)setObjectValueDepthZero(value, offset);
    }

    @Override
    public Object setObjectValueDepthZero(Object value, int offset) {
        assert offset < SIZE : SIZE_ERROR;
        switch (offset) {
            case 0:
                return variableValueZero = value;
            default:
                throw new RuntimeException(SIZE_ERROR);
        }
    }

    @Override
    public IRubyObject setValueZeroDepthZero(IRubyObject value) {
        return (IRubyObject)setObjectValueZeroDepthZero(value);
    }

    @Override
    public Object setObjectValueZeroDepthZero(Object value) {
        return variableValueZero = value;
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
        setArgObjectValues(values, size);
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
    public void setArgObjectValues(Object[] values, int size) {
        assert size <= SIZE : SIZE_ERROR;
        switch (size) {
            case 1:
                variableValueZero = values[0];
        }
    }

    public void setArgValues(IRubyObject arg0) {
        variableValueZero = arg0;
    }
    public void setArgObjectValues(Object arg0) {
        variableValueZero = arg0;
    }

    @Override
    public void setEndArgValues(IRubyObject[] values, int index, int size) {
        setEndArgObjectValues(values, index, size);
    }

    /*
     * If we are setting post arguments we can assume there are no pre or others
     */
    @Override
    public void setEndArgObjectValues(Object[] values, int index, int size) {
        assert index == 0;
        assert size <= 1;
        
        if (size == 1) {
            variableValueZero = values[values.length - 1];
        }
    }

    @Override
    @Deprecated
    public IRubyObject[] getArgValues() {
        // if we're not the "argument scope" for zsuper, try our parent
        if (!staticScope.isArgumentScope()) {
            return parent.getArgValues();
        }
        int totalArgs = staticScope.getRequiredArgs() + staticScope.getOptionalArgs();
        
        // copy and splat arguments out of the scope to use for zsuper call
        if (staticScope.getRestArg() < 0) {
            if (totalArgs == 1) {
                return new IRubyObject[] {(IRubyObject)variableValueZero};
            } else {
                return IRubyObject.NULL_ARRAY;
            }
        } else {
            // rest arg must be splatted
            IRubyObject restArg = getValue(staticScope.getRestArg(), 0);
            
            // FIXME: not very efficient
            RubyArray splattedArgs = Helpers.splatValue(restArg);
            IRubyObject[] argValues = new IRubyObject[totalArgs + splattedArgs.size()];
            System.arraycopy(splattedArgs.toJavaArray(), 0, argValues, totalArgs, splattedArgs.size());
            
            if (totalArgs == 1) {
                argValues[0] = (IRubyObject)variableValueZero;
            }
            
            return argValues;
        }
    }
}
