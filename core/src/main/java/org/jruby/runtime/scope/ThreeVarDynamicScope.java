package org.jruby.runtime.scope;

import org.jruby.RubyArray;
import org.jruby.runtime.Helpers;
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
    
    protected Object variableValueTwo;

    public ThreeVarDynamicScope(StaticScope staticScope, DynamicScope parent) {
        super(staticScope, parent);
    }

    public ThreeVarDynamicScope(StaticScope staticScope) {
        super(staticScope);
    }
    
    @Override
    public void growIfNeeded() {
        errorOnInvalidGrow(SIZE, GROW_ERROR);
    }
    
    @Override
    public DynamicScope cloneScope() {
        return new ThreeVarDynamicScope(staticScope, parent);
    }

    @Override
    public IRubyObject[] getValues() {
        return new IRubyObject[] {(IRubyObject)variableValueZero, (IRubyObject)variableValueOne, (IRubyObject)variableValueTwo};
    }

    @Override
    public Object[] getObjectValues() {
        return new Object[] {variableValueZero, variableValueOne, variableValueTwo};
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
            case 1:
                return variableValueOne;
            case 2:
                return variableValueTwo;
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
            case 1:
                if (variableValueOne == null) return variableValueOne = defval;
                return variableValueOne;
            case 2:
                if (variableValueTwo == null) return variableValueTwo = defval;
                return variableValueTwo;
            default:
                throw new RuntimeException(SIZE_ERROR);
        }
    }

    @Override
    public IRubyObject getValueTwoDepthZeroOrNil(IRubyObject nil) {
        return (IRubyObject)getObjectValueTwoDepthZeroOrDefault(nil);
    }

    @Override
    public Object getObjectValueTwoDepthZeroOrDefault(Object defval) {
        if (variableValueTwo == null) return variableValueTwo = defval;
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
        return (IRubyObject)setObjectValueDepthZero(value, offset);
    }

    @Override
    public Object setObjectValueDepthZero(Object value, int offset) {
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
        return (IRubyObject)setObjectValueTwoDepthZero(value);
    }

    @Override
    public Object setObjectValueTwoDepthZero(Object value) {
        return variableValueTwo = value;
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
            case 3:
                variableValueTwo = values[2];
            case 2:
                variableValueOne = values[1];
            case 1:
                variableValueZero = values[0];
        }
    }

    @Override
    public void setEndArgValues(IRubyObject[] values, int index, int size) {
        setEndArgObjectValues(values, index, size);
    }

    @Override
    public void setEndArgObjectValues(Object[] values, int index, int size) {
        assert size <= 2 : "ThreeVarDynamicScope only supports scopes with three variables, not " + size;
        assert index + size <= 3 : "ThreeVarDynamicScope only supports scopes with three variables, not " + (index + size);

        int start = values.length - size;

        switch (index) {
            case 0:
                switch (size) {
                    case 3:
                        variableValueTwo = values[start + 2];
                    case 2:
                        variableValueOne = values[start + 1];
                    case 1:
                        variableValueZero = values[start];
                }
                break;
            case 1:
                switch (size) {
                    case 3:
                        // should never happen
                    case 2:
                        variableValueTwo = values[start + 1];
                    case 1:
                        variableValueOne = values[start];
                        break;
                }
                break;
            case 2:
                switch (size) {
                    case 3:
                    case 2:
                        // should never happen
                    case 1:
                        variableValueTwo = values[start];
                        break;
                }
                break;
        }
    }

    @Override
    public void setArgValues(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        variableValueZero = arg0;
        variableValueOne = arg1;
        variableValueTwo = arg2;
    }

    @Override
    public void setArgObjectValues(Object arg0, Object arg1, Object arg2) {
        variableValueZero = arg0;
        variableValueOne = arg1;
        variableValueTwo = arg2;
    }

    @Override
    @Deprecated
    public IRubyObject[] getArgValues() {
        // if we're not the "argument scope" for zsuper, try our parent
        if (!staticScope.isArgumentScope()) {
            return parent.getArgValues();
        }
        int totalArgs = staticScope.getRequiredArgs() + staticScope.getOptionalArgs();
        assert totalArgs <= SIZE : SIZE_ERROR;
        
        // copy and splat arguments out of the scope to use for zsuper call
        if (staticScope.getRestArg() < 0) {
            switch (totalArgs) {
            case 0:
                return IRubyObject.NULL_ARRAY;
            case 1:
                return new IRubyObject[] {(IRubyObject)variableValueZero};
            case 2:
                return new IRubyObject[] {(IRubyObject)variableValueZero, (IRubyObject)variableValueOne};
            case 3:
                return new IRubyObject[] {(IRubyObject)variableValueZero, (IRubyObject)variableValueOne, (IRubyObject)variableValueTwo};
            default:
                throw new RuntimeException("more args requested than available variables");
            }
        } else {
            // rest arg must be splatted
            IRubyObject restArg = getValue(staticScope.getRestArg(), 0);
            assert restArg != null;
            
            // FIXME: not very efficient
            RubyArray splattedArgs = Helpers.splatValue(restArg);
            IRubyObject[] argValues = new IRubyObject[totalArgs + splattedArgs.size()];
            System.arraycopy(splattedArgs.toJavaArray(), 0, argValues, totalArgs, splattedArgs.size());
            switch (totalArgs) {
            case 3:
                argValues[2] = (IRubyObject)variableValueTwo;
            case 2:
                argValues[1] = (IRubyObject)variableValueOne;
            case 1:
                argValues[0] = (IRubyObject)variableValueZero;
            }
            
            return argValues;
        }
    }
}
