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

package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.runtime.scope.ManyVarsDynamicScope;
import org.jruby.runtime.scope.NoVarsDynamicScope;
import org.jruby.runtime.scope.OneVarDynamicScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.DummyDynamicScope;
import org.jruby.runtime.scope.FourVarDynamicScope;
import org.jruby.runtime.scope.ThreeVarDynamicScope;
import org.jruby.runtime.scope.TwoVarDynamicScope;

public abstract class DynamicScope {
    // Static scoping information for this scope
    protected final StaticScope staticScope;

    // Captured dynamic scopes
    protected final DynamicScope parent;

    // A place to store that special hiding space that bindings need to implement things like:
    // eval("a = 1", binding); eval("p a").  All binding instances must get access to this
    // hidden shared scope.  We store it here.  This will be null if no binding has yet
    // been called.
    protected DynamicScope evalScope;

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

    public static DynamicScope newDummyScope(StaticScope staticScope, DynamicScope parent) {
        return new DummyDynamicScope(staticScope, parent);
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
            if (scope != null) {
                scope = scope.getNextCapturedScope();
            } else {
                break;
            }
        }
        return scope;
    }

    public static DynamicScope newDynamicScope(StaticScope staticScope) {
        return newDynamicScope(staticScope, null);
    }

    public final DynamicScope getEvalScope(Ruby runtime) {
        // We create one extra dynamicScope on a binding so that when we 'eval "b=1", binding' the
        // 'b' will get put into this new dynamic scope.  The original scope does not see the new
        // 'b' and successive evals with this binding will.  I take it having the ability to have
        // succesive binding evals be able to share same scope makes sense from a programmers
        // perspective.   One crappy outcome of this design is it requires Dynamic and Static
        // scopes to be mutable for this one case.

        // Note: In Ruby 1.9 all of this logic can go away since they will require explicit
        // bindings for evals.

        // We only define one special dynamic scope per 'logical' binding.  So all bindings for
        // the same scope should share the same dynamic scope.  This allows multiple evals with
        // different different bindings in the same scope to see the same stuff.

        // No binding scope so we should create one
        if (evalScope == null) {
            // If the next scope out has the same binding scope as this scope it means
            // we are evaling within an eval and in that case we should be sharing the same
            // binding scope.
            DynamicScope parent = getNextCapturedScope();
            if (parent != null && parent.getEvalScope(runtime) == this) {
                evalScope = this;
            } else {
                // bindings scopes must always be ManyVars scopes since evals can grow them
                evalScope = new ManyVarsDynamicScope(runtime.getStaticScopeFactory().newEvalScope(getStaticScope()), this);
            }
        }

        return evalScope;
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
     * Get next 'captured' scope.
     *
     * @return the scope captured by this scope for implementing closures
     *
     */
    public final DynamicScope getNextCapturedScope() {
        return parent;
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

    public abstract IRubyObject[] getValues();

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
    public abstract IRubyObject getValue(int offset, int depth);

    /**
     * Variation of getValue that checks for nulls, returning and setting the given value (presumably nil)
     */
    public abstract IRubyObject getValueOrNil(int offset, int depth, IRubyObject nil);

    /**
     * getValueOrNil for depth 0
     */
    public abstract IRubyObject getValueDepthZeroOrNil(int offset, IRubyObject nil);

    /**
     * getValueOrNil for index 0, depth 0
     */
    public abstract IRubyObject getValueZeroDepthZeroOrNil(IRubyObject nil);

    /**
     * getValueOrNil for index 1, depth 0
     */
    public abstract IRubyObject getValueOneDepthZeroOrNil(IRubyObject nil);

    /**
     * getValueOrNil for index 2, depth 0
     */
    public abstract IRubyObject getValueTwoDepthZeroOrNil(IRubyObject nil);

    /**
     * getValueOrNil for index 3, depth 0
     */
    public abstract IRubyObject getValueThreeDepthZeroOrNil(IRubyObject nil);

    /**
     * Set value in current dynamic scope or one of its captured scopes.
     *
     * @param offset zero-indexed value that represents where variable lives
     * @param value to set
     * @param depth how many captured scopes down this variable should be set
     */
    public abstract IRubyObject setValue(int offset, IRubyObject value, int depth);

    /**
     * Set value in current dynamic scope or one of its captured scopes.
     *
     * @param offset zero-indexed value that represents where variable lives
     * @param value to set
     * @param depth how many captured scopes down this variable should be set
     */
    public IRubyObject setValue(IRubyObject value, int offset, int depth) {
        return setValue(offset, value, depth);
    }

    /**
     * setValue for depth zero
     *
     * @param offset zero-indexed value that represents where variable lives
     * @param value to set
     * @param depth how many captured scopes down this variable should be set
     */
    public abstract IRubyObject setValueDepthZero(IRubyObject value, int offset);

    /**
     * Set value zero in this scope;
     */
    public abstract IRubyObject setValueZeroDepthZero(IRubyObject value);

    /**
     * Set value one in this scope.
     */
    public abstract IRubyObject setValueOneDepthZero(IRubyObject value);

    /**
     * Set value two in this scope.
     */
    public abstract IRubyObject setValueTwoDepthZero(IRubyObject value);

    /**
     * Set value three in this scope.
     */
    public abstract IRubyObject setValueThreeDepthZero(IRubyObject value);

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
    public abstract void setArgValues(IRubyObject[] values, int size);

    public void setArgValues() {
        setArgValues(IRubyObject.NULL_ARRAY, 0);
    }
    public void setArgValues(IRubyObject arg0) {
        setArgValues(new IRubyObject[] {arg0}, 1);
    }
    public void setArgValues(IRubyObject arg0, IRubyObject arg1) {
        setArgValues(new IRubyObject[] {arg0, arg1}, 2);
    }
    public void setArgValues(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        setArgValues(new IRubyObject[] {arg0, arg1, arg2}, 3);
    }
    public void setArgValues(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        setArgValues(new IRubyObject[] {arg0, arg1, arg2, arg3}, 4);
    }
    public void setArgValues(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4) {
        setArgValues(new IRubyObject[] {arg0, arg1, arg2, arg3, arg4}, 5);
    }
    public void setArgValues(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, IRubyObject arg5) {
        setArgValues(new IRubyObject[] {arg0, arg1, arg2, arg3, arg4, arg5}, 6);
    }
    public void setArgValues(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, IRubyObject arg5, IRubyObject arg6) {
        setArgValues(new IRubyObject[] {arg0, arg1, arg2, arg3, arg4, arg5, arg6}, 7);
    }
    public void setArgValues(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, IRubyObject arg5, IRubyObject arg6, IRubyObject arg7) {
        setArgValues(new IRubyObject[] {arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7}, 8);
    }
    public void setArgValues(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, IRubyObject arg5, IRubyObject arg6, IRubyObject arg7, IRubyObject arg8) {
        setArgValues(new IRubyObject[] {arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8}, 9);
    }
    public void setArgValues(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, IRubyObject arg5, IRubyObject arg6, IRubyObject arg7, IRubyObject arg8, IRubyObject arg9) {
        setArgValues(new IRubyObject[] {arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9}, 10);
    }


    /**
     *
     * @param values group where last n(size) values are used
     * @param index index in the dynamic scope to start setting these values
     * @param size which of the last size arguments of values parameter to set
     */
    public abstract void setEndArgValues(IRubyObject[] values, int index, int size);

    /**
     * Copy variable values back for ZSuper call.
     */
    public abstract IRubyObject[] getArgValues();

    @Override
    public String toString() {
        return toString(new StringBuffer(), "");
    }

    // Helper function to give a good view of current dynamic scope with captured scopes
    public String toString(StringBuffer buf, String indent) {
        buf.append(indent).append("Static Type[" + hashCode() + "]: " +
                (staticScope.isBlockScope() ? "block" : "local")+" [");
        int size = staticScope.getNumberOfVariables();
        IRubyObject[] variableValues = getValues();

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
