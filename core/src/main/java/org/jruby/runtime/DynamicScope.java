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

import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.JDKVersion;
import me.qmx.jitescript.JiteClass;
import me.qmx.jitescript.internal.org.objectweb.asm.Label;
import me.qmx.jitescript.internal.org.objectweb.asm.tree.LabelNode;
import org.jruby.EvalType;
import org.jruby.Ruby;
import org.jruby.runtime.scope.NoVarsDynamicScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.DummyDynamicScope;

import static org.jruby.util.CodegenUtils.*;

import org.jruby.util.OneShotClassLoader;
import org.jruby.util.collections.NonBlockingHashMapLong;

public abstract class DynamicScope {
    // Static scoping information for this scope
    protected final StaticScope staticScope;

    // Captured dynamic scopes
    protected final DynamicScope parent;

    private EvalType evalType;

    private boolean lambda;

    protected DynamicScope(StaticScope staticScope, DynamicScope parent) {
        this.staticScope = staticScope;
        this.parent = parent;
        this.evalType = EvalType.NONE;
    }

    protected DynamicScope(StaticScope staticScope) {
        this(staticScope, null);
    }

    public static DynamicScope newDynamicScope(StaticScope staticScope, DynamicScope parent) {
        switch (staticScope.getNumberOfVariables()) {
        case 0:
            return new NoVarsDynamicScope(staticScope, parent);
        default:
            return construct(staticScope, parent);
        }
    }

    private static final NonBlockingHashMapLong<Class<? extends DynamicScope>> prototypes = new NonBlockingHashMapLong<>();

    public static Class protoClassFromProps(int size) {
        return prototypes.get(size);
    }

    public static DynamicScope construct(StaticScope staticScope, DynamicScope parent) {
        Class<DynamicScope> tupleType = generate(staticScope.getNumberOfVariables());
        try {
            return tupleType.getConstructor(StaticScope.class, DynamicScope.class).newInstance(staticScope, parent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<DynamicScope> generate(final int size) {
        Class p = protoClassFromProps(size);
        final String name = "org/jruby/runtime/scopes/DynamicScope" + size;

        try {
            if (p == null) {
                // create a new one
                final Class<DynamicScope> base = DynamicScope.class;
                final String[] newFields = varList(size);

                JiteClass jiteClass = new JiteClass(name, p(DynamicScope.class), new String[0]) {{
                    // parent class constructor
                    defineMethod("<init>", ACC_PUBLIC, sig(void.class, StaticScope.class, DynamicScope.class), new CodeBlock() {{
                        aload(0);
                        aload(1);
                        aload(2);
                        invokespecial(p(base), "<init>", sig(void.class, StaticScope.class, DynamicScope.class));
                        voidreturn();
                    }});

                    // required overrides
                    defineMethod("getValue", ACC_PUBLIC, sig(IRubyObject.class, int.class, int.class), new CodeBlock() {{
                        line(0);
                        iload(2); // depth
                        LabelNode superCall = new LabelNode(new Label());
                        LabelNode defaultError = new LabelNode(new Label());
                        LabelNode[] cases = new LabelNode[size];
                        for (int i = 0; i < size; i++) {
                            cases[i] = new LabelNode(new Label());
                        }
                        ifne(superCall);
                        iload(1);
                        tableswitch(0, size - 1, defaultError, cases);
                        for (int i = 0; i < size; i++) {
                            label(cases[i]);
                            aload(0);
                            getfield(name, newFields[i], ci(IRubyObject.class));
                            areturn();
                        }
                        label(defaultError);
                        line(1);
                        newobj(p(RuntimeException.class));
                        dup();
                        ldc(name + " only supports scopes with " + size + " variables");
                        invokespecial(p(RuntimeException.class), "<init>", sig(void.class, String.class));
                        athrow();
                        label(superCall);
                        line(2);
                        aload(0);
                        getfield(p(DynamicScope.class), "parent", ci(DynamicScope.class));
                        iload(1);
                        iload(2);
                        pushInt(1);
                        isub();
                        invokevirtual(p(DynamicScope.class), "getValue", sig(IRubyObject.class, int.class, int.class));
                        areturn();
                    }});

                    // required overrides
                    defineMethod("setValue", ACC_PUBLIC, sig(IRubyObject.class, int.class, IRubyObject.class, int.class), new CodeBlock() {{
                        line(3);
                        iload(3); // depth
                        LabelNode superCall = new LabelNode(new Label());
                        LabelNode defaultError = new LabelNode(new Label());
                        LabelNode[] cases = new LabelNode[size];
                        for (int i = 0; i < size; i++) {
                            cases[i] = new LabelNode(new Label());
                        }
                        ifne(superCall);
                        iload(1);
                        tableswitch(0, size - 1, defaultError, cases);
                        for (int i = 0; i < size; i++) {
                            label(cases[i]);
                            aload(0);
                            aload(2);
                            putfield(name, newFields[i], ci(IRubyObject.class));
                            aload(2);
                            areturn();
                        }
                        label(defaultError);
                        line(4);
                        newobj(p(RuntimeException.class));
                        dup();
                        ldc(name + " only supports scopes with " + size + " variables");
                        invokespecial(p(RuntimeException.class), "<init>", sig(void.class, String.class));
                        athrow();
                        label(superCall);
                        line(5);
                        aload(0);
                        getfield(p(DynamicScope.class), "parent", ci(DynamicScope.class));
                        iload(1);
                        aload(2);
                        iload(3);
                        pushInt(1);
                        isub();
                        invokevirtual(p(DynamicScope.class), "setValue", sig(IRubyObject.class, int.class, IRubyObject.class, int.class));
                        areturn();
                    }});

                    // fields
                    for (String prop : newFields) {
                        defineField(prop, ACC_PUBLIC, ci(IRubyObject.class), null);
                    }
                }};

                p = defineClass(jiteClass);
                prototypes.put(size, p);
            }

            return p;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Class defineClass(JiteClass jiteClass) {
        return new OneShotClassLoader(Ruby.getClassLoader())
                .defineClass(jiteClass.getClassName().replaceAll("/", "."), jiteClass.toBytes(JDKVersion.V1_7));
    }

    public static String[] varList(int size) {
        String[] vars = new String[size];

        for (int i = 0; i < size; i++) {
            vars[i] = "var" + i;
        }

        return vars;
    }

    public static DynamicScope newDynamicScope(StaticScope staticScope, DynamicScope parent, EvalType evalType) {
        DynamicScope newScope = newDynamicScope(staticScope, parent);
        newScope.setEvalType(evalType);
        return newScope;
    }

    public static DynamicScope newDummyScope(StaticScope staticScope, DynamicScope parent) {
        return new DummyDynamicScope(staticScope, parent);
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

    public abstract IRubyObject[] getValues();

    /**
     * Get value from current scope or one of its captured scopes.
     *
     * @param offset zero-indexed value that represents where variable lives
     * @param depth how many captured scopes down this variable should be set
     * @return the value here
     */
    public abstract IRubyObject getValue(int offset, int depth);

    /**
     * Variation of getValue for depth 0
     */
    public IRubyObject getValueDepthZero(int offset) {
        return getValue(offset, 0);
    }

    /**
     * getValue for index 0, depth 0
     */
    public IRubyObject getValueZeroDepthZero() {
        return getValue(0, 0);
    }

    /**
     * getValue for index 1, depth 0
     */
    public IRubyObject getValueOneDepthZero() {
        return getValue(1, 0);
    }

    /**
     * getValue for index 2, depth 0
     */
    public IRubyObject getValueTwoDepthZero() {
        return getValue(2, 0);
    }

    /**
     * getValue for index 3, depth 0
     */
    public IRubyObject getValueThreeDepthZero() {
        return getValue(3, 0);
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

    /**
     * getValueOrNil for depth 0
     */
    public IRubyObject getValueDepthZeroOrNil(int offset, IRubyObject nil) {
        IRubyObject value = getValueDepthZero(offset);
        return value == null ? setValueDepthZero(nil, offset) : value;
    }

    /**
     * getValueOrNil for index 0, depth 0
     */
    public IRubyObject getValueZeroDepthZeroOrNil(IRubyObject nil) {
        IRubyObject value = getValueDepthZero(0);
        return value == null ? setValueDepthZero(nil, 0) : value;
    }

    /**
     * getValueOrNil for index 1, depth 0
     */
    public IRubyObject getValueOneDepthZeroOrNil(IRubyObject nil) {
        IRubyObject value = getValueDepthZero(1);
        return value == null ? setValueDepthZero(nil, 1) : value;
    }

    /**
     * getValueOrNil for index 2, depth 0
     */
    public IRubyObject getValueTwoDepthZeroOrNil(IRubyObject nil) {
        IRubyObject value = getValueDepthZero(2);
        return value == null ? setValueDepthZero(nil, 2) : value;
    }

    /**
     * getValueOrNil for index 3, depth 0
     */
    public IRubyObject getValueThreeDepthZeroOrNil(IRubyObject nil) {
        IRubyObject value = getValueDepthZero(3);
        return value == null ? setValueDepthZero(nil, 3) : value;
    }

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
     * Set value in current dynamic scope or one of its captured scopes.
     *
     * @param offset zero-indexed value that represents where variable lives
     * @param value to set
     * @param depth how many captured scopes down this variable should be set
     */
    public void setValueVoid(IRubyObject value, int offset, int depth) {
        setValue(offset, value, depth);
    }

    /**
     * setValue for depth zero
     *
     * @param value to set
     * @param offset zero-indexed value that represents where variable lives
     */
    public IRubyObject setValueDepthZero(IRubyObject value, int offset) {
        return setValue(offset, value, 0);
    }

    /**
     * setValue for depth zero
     *
     * @param value to set
     * @param offset zero-indexed value that represents where variable lives
     */
    public void setValueDepthZeroVoid(IRubyObject value, int offset) {
        setValueDepthZero(value, offset);
    }

    /**
     * Set value zero in this scope;
     */
    public IRubyObject setValueZeroDepthZero(IRubyObject value) {
        return setValue(0, value, 0);
    }

    /**
     * Set value zero in this scope;
     */
    public void setValueZeroDepthZeroVoid(IRubyObject value) {
        setValueZeroDepthZero(value);
    }

    /**
     * Set value one in this scope.
     */
    public IRubyObject setValueOneDepthZero(IRubyObject value) {
        return setValue(1, value, 0);}

    /**
     * Set value one in this scope.
     */
    public void setValueOneDepthZeroVoid(IRubyObject value) {
        setValueOneDepthZero(value);
    }

    /**
     * Set value two in this scope.
     */
    public IRubyObject setValueTwoDepthZero(IRubyObject value) {
        return setValue(2, value, 0);}

    /**
     * Set value two in this scope.
     */
    public void setValueTwoDepthZeroVoid(IRubyObject value) {
        setValueTwoDepthZero(value);
    }

    /**
     * Set value three in this scope.
     */
    public IRubyObject setValueThreeDepthZero(IRubyObject value) {
        return setValue(3, value, 0);
    }

    /**
     * Set value three in this scope.
     */
    public void setValueThreeDepthZeroVoid(IRubyObject value) {
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

    public boolean inInstanceEval() {
        return evalType == EvalType.INSTANCE_EVAL;
    }

    public boolean inModuleEval() {
        return evalType == EvalType.MODULE_EVAL;
    }

    public boolean inBindingEval() {
        return evalType == EvalType.BINDING_EVAL;
    }

    public void setEvalType(EvalType evalType) {
        this.evalType = evalType == null ? EvalType.NONE : evalType;
    }

    public EvalType getEvalType() {
        return this.evalType;
    }

    public void clearEvalType() {
        evalType = EvalType.NONE;
    }

    public void setLambda(boolean lambda) {
        this.lambda = lambda;
    }

    public boolean isLambda() {
        return lambda;
    }
}
