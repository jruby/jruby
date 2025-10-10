package org.jruby.runtime.scope;

import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ArraySupport;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

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

    public static final MethodHandle CONSTRUCTOR;
    static {
        try {
            // use lookup() to avoid IllegalAccessException with JRuby embed
            CONSTRUCTOR = MethodHandles.lookup()
                    .findConstructor(ManyVarsDynamicScope.class, MethodType.methodType(void.class, StaticScope.class, DynamicScope.class))
                    .asType(MethodType.methodType(DynamicScope.class, StaticScope.class, DynamicScope.class));
        } catch (Exception e) {
            throw new RuntimeException("BUG: could not initialize constructor handle", e);
        }
    }

    public ManyVarsDynamicScope(StaticScope staticScope, DynamicScope parent) {
        super(staticScope, parent);
        allocate();
    }

    private void allocate() {
        if(variableValues == null) variableValues = IRubyObject.array(staticScope.getNumberOfVariables());
    }

    // WARNING:  This is used when dup'ing an eval scope.  We know the size and that it will 100% always be
    // a ManyVarsDynamicScope.  This should not be used by anything else.  If there ever ends up being another
    // use then it should be documented in this warning.
    public void setVariableValues(IRubyObject[] variableValues) {
        this.variableValues = variableValues;
    }

    public IRubyObject[] getValues() {
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

        IRubyObject[] variableValues = this.variableValues;

        assert checkOffset(variableValues, offset) : failGet(this, offset, depth);

        return variableValues[offset];
    }

    public IRubyObject getValueDepthZero(int offset) {
        return variableValues[offset];
    }

    public IRubyObject getValueZeroDepthZero() {
        return variableValues[0];
    }

    public IRubyObject getValueOneDepthZero() {
        return variableValues[1];
    }

    public IRubyObject getValueTwoDepthZero() {
        return variableValues[2];
    }

    public IRubyObject getValueThreeDepthZero() {
        return variableValues[3];
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
        IRubyObject[] variableValues = this.variableValues;

        assert checkOffset(variableValues, offset) : failGet(this, offset);

        IRubyObject value = variableValues[offset];
        return value == null ? setValueDepthZero(nil, offset) : value;
    }

    public IRubyObject getValueZeroDepthZeroOrNil(IRubyObject nil) {
        IRubyObject[] variableValues = this.variableValues;

        assert checkOffset(variableValues, 0) : failGet(this, 0);

        IRubyObject value = variableValues[0];
        return value == null ? setValueZeroDepthZero(nil) : value;
    }
    
    public IRubyObject getValueOneDepthZeroOrNil(IRubyObject nil) {
        IRubyObject[] variableValues = this.variableValues;

        assert checkOffset(variableValues, 1) : failGet(this, 1);

        IRubyObject value = variableValues[1];
        return value == null ? setValueOneDepthZero(nil) : value;
    }
    
    public IRubyObject getValueTwoDepthZeroOrNil(IRubyObject nil) {
        IRubyObject[] variableValues = this.variableValues;

        assert checkOffset(variableValues, 2) : failGet(this, 2);

        IRubyObject value = variableValues[2];
        return value == null ? setValueTwoDepthZero(nil) : value;
    }
    
    public IRubyObject getValueThreeDepthZeroOrNil(IRubyObject nil) {
        IRubyObject[] variableValues = this.variableValues;

        assert checkOffset(variableValues, 3) : failGet(this, 3);

        IRubyObject value = variableValues[3];
        return value == null ? setValueThreeDepthZero(nil) : value;
    }

    /**
     * Set value in current dynamic scope or one of its captured scopes.
     * 
     * @param offset zero-indexed value that represents where variable lives
     * @param value to set
     * @param depth how many captured scopes down this variable should be set
     */
    public void setValueVoid(IRubyObject value, int offset, int depth) {
        if (depth > 0) {
            DynamicScope parent = this.parent;

            assert parent != null : "If depth > 0, then parent should not ever be null";

            parent.setValueVoid(value, offset, depth - 1);
        } else {
            IRubyObject[] variableValues = this.variableValues;

            assert checkOffset(variableValues, offset) : failSet(this, value, offset);

            setValueDepthZeroVoid(value, offset);
        }
    }

    public void setValueDepthZeroVoid(IRubyObject value, int offset) {
        IRubyObject[] variableValues = this.variableValues;

        assert checkOffset(variableValues, offset) : failSet(this, value, offset);

        variableValues[offset] = value;
    }
    public void setValueZeroDepthZeroVoid(IRubyObject value) {
        IRubyObject[] variableValues = this.variableValues;

        assert checkOffset(variableValues, 0) : failSet(this, value, 0);

        variableValues[0] = value;
    }
    public void setValueOneDepthZeroVoid(IRubyObject value) {
        IRubyObject[] variableValues = this.variableValues;

        assert checkOffset(variableValues, 1) : failSet(this, value, 1);

        variableValues[1] = value;
    }
    public void setValueTwoDepthZeroVoid(IRubyObject value) {
        IRubyObject[] variableValues = this.variableValues;

        assert checkOffset(variableValues, 2) : failSet(this, value, 2);

        variableValues[2] = value;
    }
    public void setValueThreeDepthZeroVoid(IRubyObject value) {
        IRubyObject[] variableValues = this.variableValues;

        assert checkOffset(variableValues, 3) : failSet(this, value, 3);

        variableValues[3] = value;
    }
    public void setValueFourDepthZeroVoid(IRubyObject value) {
        IRubyObject[] variableValues = this.variableValues;

        assert checkOffset(variableValues, 3) : failSet(this, value, 3);

        variableValues[4] = value;
    }
    public void setValueFiveDepthZeroVoid(IRubyObject value) {
        IRubyObject[] variableValues = this.variableValues;

        assert checkOffset(variableValues, 3) : failSet(this, value, 3);

        variableValues[5] = value;
    }
    public void setValueSixDepthZeroVoid(IRubyObject value) {
        IRubyObject[] variableValues = this.variableValues;

        assert checkOffset(variableValues, 3) : failSet(this, value, 3);

        variableValues[6] = value;
    }
    public void setValueSevenDepthZeroVoid(IRubyObject value) {
        IRubyObject[] variableValues = this.variableValues;

        assert checkOffset(variableValues, 3) : failSet(this, value, 3);

        variableValues[7] = value;
    }
    public void setValueEightDepthZeroVoid(IRubyObject value) {
        IRubyObject[] variableValues = this.variableValues;

        assert checkOffset(variableValues, 3) : failSet(this, value, 3);

        variableValues[8] = value;
    }
    public void setValueNineDepthZeroVoid(IRubyObject value) {
        IRubyObject[] variableValues = this.variableValues;

        assert checkOffset(variableValues, 3) : failSet(this, value, 3);

        variableValues[9] = value;
    }

    private static boolean checkOffset(IRubyObject[] variableValues, int offset) {
        return variableValues != null && offset < variableValues.length;
    }

    private static String failGet(ManyVarsDynamicScope scope, int offset) {
        return failGet(scope, offset, 0);
    }

    private static String failGet(ManyVarsDynamicScope scope, int offset, int depth) {
        return "No variables or index too big for getValue off: " + offset + ", Dep: " + depth + ", O: " + scope;
    }

    private static String failSet(ManyVarsDynamicScope scope, IRubyObject value, int offset) {
        return "Setting " + offset + " to " + value + ", O: " + scope;
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
        int dynamicSize = variableValues == null ? 0: variableValues.length;
        
        if (staticScope.getNumberOfVariables() > dynamicSize) {
            IRubyObject values[] = new IRubyObject[staticScope.getNumberOfVariables()];
            
            if (dynamicSize > 0) {
                ArraySupport.copy(variableValues, 0, values, 0, dynamicSize);
            }
            
            variableValues = values;
        }
    }

    @Deprecated(since = "9.1.6.0")
    public DynamicScope cloneScope() {
        // we construct new rather than clone to avoid sharing variableValues
        return new ManyVarsDynamicScope(staticScope, parent);
    }
}
