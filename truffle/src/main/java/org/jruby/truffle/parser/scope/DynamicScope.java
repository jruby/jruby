/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.truffle.parser.scope;

public abstract class DynamicScope {
    // Static scoping information for this scope
    protected final StaticScope staticScope;

    // Captured dynamic scopes
    protected final DynamicScope parent;

    private boolean lambda;

    protected DynamicScope(StaticScope staticScope, DynamicScope parent) {
        this.staticScope = staticScope;
        this.parent = parent;
    }

    protected DynamicScope(StaticScope staticScope) {
        this(staticScope, null);
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
        case 4:
            return new FourVarDynamicScope(staticScope, parent);
        default:
            return new ManyVarsDynamicScope(staticScope, parent);
        }
    }

    /**
     * Get parent (capturing) scope.  This is used by eval and closures to
     * walk up to hard lexical boundary.
     *
     */
    public final DynamicScope getParentScope() {
        return parent;
    }

    @Deprecated
    public DynamicScope getNextCapturedScope() {  // Used by ruby-debug-ide
        return getParentScope();
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
            if (scope == null) break;
            scope = scope.getParentScope();
        }
        return scope;
    }

    public static DynamicScope newDynamicScope(StaticScope staticScope) {
        return newDynamicScope(staticScope, null);
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

    public abstract void growIfNeeded();

    public abstract DynamicScope cloneScope();

    public abstract Object[] getValues();

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
    public abstract Object getValue(int offset, int depth);

    /**
     * Variation of getValue that checks for nulls, returning and setting the given value (presumably nil)
     */
    public abstract Object getValueOrNil(int offset, int depth, Object nil);

    /**
     * getValueOrNil for depth 0
     */
    public abstract Object getValueDepthZeroOrNil(int offset, Object nil);

    /**
     * getValueOrNil for index 0, depth 0
     */
    public abstract Object getValueZeroDepthZeroOrNil(Object nil);

    /**
     * getValueOrNil for index 1, depth 0
     */
    public abstract Object getValueOneDepthZeroOrNil(Object nil);

    /**
     * getValueOrNil for index 2, depth 0
     */
    public abstract Object getValueTwoDepthZeroOrNil(Object nil);

    /**
     * getValueOrNil for index 3, depth 0
     */
    public abstract Object getValueThreeDepthZeroOrNil(Object nil);

    /**
     * Set value in current dynamic scope or one of its captured scopes.
     *
     * @param offset zero-indexed value that represents where variable lives
     * @param value to set
     * @param depth how many captured scopes down this variable should be set
     */
    public abstract Object setValue(int offset, Object value, int depth);

    /**
     * Set value in current dynamic scope or one of its captured scopes.
     *
     * @param offset zero-indexed value that represents where variable lives
     * @param value to set
     * @param depth how many captured scopes down this variable should be set
     */
    public Object setValue(Object value, int offset, int depth) {
        return setValue(offset, value, depth);
    }

    /**
     * Set value in current dynamic scope or one of its captured scopes.
     *
     * @param offset zero-indexed value that represents where variable lives
     * @param value to set
     * @param depth how many captured scopes down this variable should be set
     */
    public void setValueVoid(Object value, int offset, int depth) {
        setValue(offset, value, depth);
    }

    /**
     * setValue for depth zero
     *
     * @param value to set
     * @param offset zero-indexed value that represents where variable lives
     */
    public abstract Object setValueDepthZero(Object value, int offset);

    /**
     * setValue for depth zero
     *
     * @param value to set
     * @param offset zero-indexed value that represents where variable lives
     */
    public void setValueDepthZeroVoid(Object value, int offset) {
        setValueDepthZero(value, offset);
    }

    /**
     * Set value zero in this scope;
     */
    public abstract Object setValueZeroDepthZero(Object value);

    /**
     * Set value zero in this scope;
     */
    public void setValueZeroDepthZeroVoid(Object value) {
        setValueZeroDepthZero(value);
    }

    /**
     * Set value one in this scope.
     */
    public abstract Object setValueOneDepthZero(Object value);

    /**
     * Set value one in this scope.
     */
    public void setValueOneDepthZeroVoid(Object value) {
        setValueOneDepthZero(value);
    }

    /**
     * Set value two in this scope.
     */
    public abstract Object setValueTwoDepthZero(Object value);

    /**
     * Set value two in this scope.
     */
    public void setValueTwoDepthZeroVoid(Object value) {
        setValueTwoDepthZero(value);
    }

    /**
     * Set value three in this scope.
     */
    public abstract Object setValueThreeDepthZero(Object value);

    /**
     * Set value three in this scope.
     */
    public void setValueThreeDepthZeroVoid(Object value) {
        setValueThreeDepthZero(value);
    }

    @Override
    public String toString() {
        return toString(new StringBuffer(), "");
    }

    // Helper function to give a good view of current dynamic scope with captured scopes
    public String toString(StringBuffer buf, String indent) {
        buf.append(indent).append("Static Type[" + hashCode() + "]: " +
                (staticScope.isBlockScope() ? "block" : "local")+" [");
        int size = staticScope.getNumberOfVariables();
        Object[] variableValues = getValues();

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

    public void setLambda(boolean lambda) {
        this.lambda = lambda;
    }

    public boolean isLambda() {
        return lambda;
    }
}
