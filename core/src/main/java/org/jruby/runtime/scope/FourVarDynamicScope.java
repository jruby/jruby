package org.jruby.runtime.scope;

import org.jruby.RubyArray;
import org.jruby.runtime.Helpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * This is a DynamicScope that supports exactly four variables.
 */
public class FourVarDynamicScope extends ThreeVarDynamicScope {
    private static final int SIZE = 4;
    private static final String SIZE_ERROR = "FourVarDynamicScope only supports scopes with four variables";
    private static final String GROW_ERROR = "FourVarDynamicScope cannot be grown; use ManyVarsDynamicScope";

    protected Object variableValueThree;

    public FourVarDynamicScope(StaticScope staticScope, DynamicScope parent) {
        super(staticScope, parent);
    }

    public FourVarDynamicScope(StaticScope staticScope) {
        super(staticScope);
    }
    
    @Override
    public void growIfNeeded() {
        errorOnInvalidGrow(SIZE, GROW_ERROR);
    }
    
    @Override
    public DynamicScope cloneScope() {
        return new FourVarDynamicScope(staticScope, parent);
    }

    @Override
    public IRubyObject[] getValues() {
        return new IRubyObject[] {(IRubyObject)variableValueZero, (IRubyObject)variableValueOne, (IRubyObject)variableValueTwo, (IRubyObject)variableValueThree};
    }

    @Override
    public Object[] getObjectValues() {
        return new Object[] {variableValueZero, variableValueOne, variableValueTwo, variableValueThree};
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
            case 3:
                return variableValueThree;
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
            case 3:
                if (variableValueThree == null) return variableValueThree = defval;
                return variableValueThree;
            default:
                throw new RuntimeException(SIZE_ERROR);
        }
    }

    @Override
    public IRubyObject getValueThreeDepthZeroOrNil(IRubyObject nil) {
        return (IRubyObject)getObjectValueThreeDepthZeroOrDefault(nil);
    }

    @Override
    public Object getObjectValueThreeDepthZeroOrDefault(Object defval) {
        if (variableValueThree == null) return variableValueThree = defval;
        return variableValueThree;
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
            case 3:
                return variableValueThree = value;
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
            case 3:
                return variableValueThree = value;
            default:
                throw new RuntimeException(SIZE_ERROR);
        }
    }

    @Override
    public IRubyObject setValueThreeDepthZero(IRubyObject value) {
        return (IRubyObject)setObjectValueThreeDepthZero(value);
    }

    @Override
    public Object setObjectValueThreeDepthZero(Object value) {
        return variableValueThree = value;
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
            case 4:
                variableValueThree = values[3];
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
        setEndArgValues(values, index, size);
    }

    @Override
    public void setEndArgObjectValues(Object[] values, int index, int size) {
        assert size <= 3 : "FourVarDynamicScope only supports scopes with four variables, not " + size;
        assert index + size <= 4 : "FourVarDynamicScope only supports scopes with four variables, not " + (index + size);

        int start = values.length - size;

        switch (index) {
            case 0:
                switch (size) {
                    case 4:
                        variableValueThree = values[start + 3];
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
                    case 4:
                        // should never happen
                    case 3:
                        variableValueThree = values[start + 2];
                    case 2:
                        variableValueTwo = values[start + 1];
                    case 1:
                        variableValueOne = values[start];
                        break;
                }
                break;
            case 2:
                switch (size) {
                    case 4:
                    case 3:
                        // should never happen
                    case 2:
                        variableValueThree = values[start + 1];
                    case 1:
                        variableValueTwo = values[start];
                        break;
                }
                break;
            case 3:
                switch (size) {
                    case 4:
                    case 3:
                    case 2:
                        // should never happen
                    case 1:
                        variableValueThree = values[start];
                        break;
                }
                break;
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
            case 4:
                return new IRubyObject[] {(IRubyObject)variableValueZero, (IRubyObject)variableValueOne, (IRubyObject)variableValueTwo, (IRubyObject)variableValueThree};
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
            case 4:
                argValues[3] = (IRubyObject)variableValueThree;
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
