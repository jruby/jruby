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
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.util.CodegenUtils.*;

import org.jruby.util.OneShotClassLoader;
import org.jruby.util.collections.NonBlockingHashMapLong;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class DynamicScope implements Cloneable {
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

    public static DynamicScope newDynamicScope(StaticScope staticScope, DynamicScope parent) {
        return construct(staticScope, parent);
    }

    private static final NonBlockingHashMapLong<MethodHandle> prototypes = new NonBlockingHashMapLong<>();

    public static MethodHandle protoClassFromProps(int size) {
        return prototypes.get(size);
    }

    public static DynamicScope construct(StaticScope staticScope, DynamicScope parent) {
        MethodHandle tupleType = generate(staticScope.getNumberOfVariables());
        try {
            return (DynamicScope) tupleType.invokeExact(staticScope, parent);
        } catch (Throwable e) {
            Helpers.throwException(e);
            return null; // not reached
        }
    }

    public static final List<String> SPECIALIZED_GETS = Collections.unmodifiableList(Arrays.asList(
            "getValueZeroDepthZero",
            "getValueOneDepthZero",
            "getValueTwoDepthZero",
            "getValueThreeDepthZero",
            "getValueFourDepthZero",
            "getValueFiveDepthZero",
            "getValueSixDepthZero",
            "getValueSevenDepthZero",
            "getValueEightDepthZero",
            "getValueNineDepthZero"
    ));

    public static final List<String> SPECIALIZED_SETS = Collections.unmodifiableList(Arrays.asList(
            "setValueZeroDepthZeroVoid",
            "setValueOneDepthZeroVoid",
            "setValueTwoDepthZeroVoid",
            "setValueThreeDepthZeroVoid",
            "setValueFourDepthZeroVoid",
            "setValueFiveDepthZeroVoid",
            "setValueSixDepthZeroVoid",
            "setValueSevenDepthZeroVoid",
            "setValueEightDepthZeroVoid",
            "setValueNineDepthZeroVoid"
    ));

    public static MethodHandle generate(final int size) {
        MethodHandle h = protoClassFromProps(size);

        if (h != null) return h;

        Class p = null;
        final String clsPath = "org/jruby/runtime/scopes/DynamicScope" + size;
        final String clsName = clsPath.replaceAll("/", ".");

        try {
            // create a new one
            final String[] newFields = varList(size);

            final String baseName = p(DynamicScope.class);

            JiteClass jiteClass = new JiteClass(clsPath, baseName, new String[0]) {{
                // parent class constructor
                defineMethod("<init>", ACC_PUBLIC, sig(void.class, StaticScope.class, DynamicScope.class), new CodeBlock() {{
                    aload(0);
                    aload(1);
                    aload(2);
                    invokespecial(baseName, "<init>", sig(void.class, StaticScope.class, DynamicScope.class));
                    voidreturn();
                }});

                // required overrides
                defineMethod("getValue", ACC_PUBLIC, sig(IRubyObject.class, int.class, int.class), new CodeBlock() {{
                    LabelNode parentCall = new LabelNode(new Label());
                    LabelNode defaultError = new LabelNode(new Label());
                    LabelNode[] cases = new LabelNode[size];

                    for (int i = 0; i < size; i++) {
                        cases[i] = new LabelNode(new Label());
                    }

                    line(0);

                    iload(2); // depth
                    ifne(parentCall);

                    if (size > 0) {
                        iload(1);
                        tableswitch(0, size - 1, defaultError, cases);
                        for (int i = 0; i < size; i++) {
                            label(cases[i]);
                            aload(0);
                            getfield(clsPath, newFields[i], ci(IRubyObject.class));
                            areturn();
                        }
                        label(defaultError);
                    }

                    line(1);

                    newobj(p(RuntimeException.class));
                    dup();
                    ldc(clsName + " only supports scopes with " + size + " variables");
                    invokespecial(p(RuntimeException.class), "<init>", sig(void.class, String.class));
                    athrow();
                    label(parentCall);

                    line(2);

                    aload(0);
                    getfield(baseName, "parent", ci(DynamicScope.class));
                    iload(1);
                    iload(2);
                    pushInt(1);
                    isub();
                    invokevirtual(baseName, "getValue", sig(IRubyObject.class, int.class, int.class));
                    areturn();
                }});

                defineMethod("setValueVoid", ACC_PUBLIC, sig(void.class, IRubyObject.class, int.class, int.class), new CodeBlock() {{
                    LabelNode parentCall = new LabelNode(new Label());
                    LabelNode defaultError = new LabelNode(new Label());
                    LabelNode[] cases = new LabelNode[size];

                    for (int i = 0; i < size; i++) {
                        cases[i] = new LabelNode(new Label());
                    }

                    line(3);
                    iload(3); // depth
                    ifne(parentCall);
                    if (size > 0) {
                        iload(2);
                        tableswitch(0, size - 1, defaultError, cases);
                        for (int i = 0; i < size; i++) {
                            label(cases[i]);
                            aload(0);
                            aload(1);
                            putfield(clsPath, newFields[i], ci(IRubyObject.class));
                            voidreturn();
                        }
                        label(defaultError);
                    }
                    line(4);
                    newobj(p(RuntimeException.class));
                    dup();
                    ldc(clsName + " only supports scopes with " + size + " variables");
                    invokespecial(p(RuntimeException.class), "<init>", sig(void.class, String.class));
                    athrow();
                    label(parentCall);
                    line(5);
                    aload(0);
                    getfield(baseName, "parent", ci(DynamicScope.class));
                    aload(1);
                    iload(2);
                    iload(3);
                    pushInt(1);
                    isub();
                    invokevirtual(baseName, "setValueVoid", sig(void.class, IRubyObject.class, int.class, int.class));
                    voidreturn();
                }});

                // optional overrides
                defineMethod("getValueDepthZero", ACC_PUBLIC, sig(IRubyObject.class, int.class), new CodeBlock() {{
                    LabelNode defaultError = new LabelNode(new Label());
                    LabelNode[] cases = new LabelNode[size];

                    for (int i = 0; i < size; i++) {
                        cases[i] = new LabelNode(new Label());
                    }

                    line(6);
                    if (size > 0) {
                        iload(1);
                        tableswitch(0, size - 1, defaultError, cases);
                        for (int i = 0; i < size; i++) {
                            label(cases[i]);
                            aload(0);
                            getfield(clsPath, newFields[i], ci(IRubyObject.class));
                            areturn();
                        }
                        label(defaultError);
                    }
                    line(1);
                    newobj(p(RuntimeException.class));
                    dup();
                    ldc(clsName + " only supports scopes with " + size + " variables");
                    invokespecial(p(RuntimeException.class), "<init>", sig(void.class, String.class));
                    athrow();
                }});

                defineMethod("setValueDepthZeroVoid", ACC_PUBLIC, sig(void.class, IRubyObject.class, int.class), new CodeBlock() {{
                    LabelNode defaultError = new LabelNode(new Label());
                    LabelNode[] cases = new LabelNode[size];

                    line(6);

                    for (int i = 0; i < size; i++) {
                        cases[i] = new LabelNode(new Label());
                    }
                    if (size > 0) {
                        iload(2);
                        tableswitch(0, size - 1, defaultError, cases);
                        for (int i = 0; i < size; i++) {
                            label(cases[i]);
                            aload(0);
                            aload(1);
                            putfield(clsPath, newFields[i], ci(IRubyObject.class));
                            voidreturn();
                        }
                        label(defaultError);
                    }
                    line(1);
                    newobj(p(RuntimeException.class));
                    dup();
                    ldc(clsName + " only supports scopes with " + size + " variables");
                    invokespecial(p(RuntimeException.class), "<init>", sig(void.class, String.class));
                    athrow();
                }});

                for (int i = 0; i < SPECIALIZED_GETS.size(); i++) {
                    final int offset = i;

                    defineMethod(SPECIALIZED_GETS.get(offset), ACC_PUBLIC, sig(IRubyObject.class), new CodeBlock() {{
                        line(6);

                        if (size <= offset) {
                            newobj(p(RuntimeException.class));
                            dup();
                            ldc(clsName + " only supports scopes with " + size + " variables");
                            invokespecial(p(RuntimeException.class), "<init>", sig(void.class, String.class));
                            athrow();
                        } else {
                            aload(0);
                            getfield(clsPath, newFields[offset], ci(IRubyObject.class));
                            areturn();
                        }
                    }});
                }


                for (int i = 0; i < SPECIALIZED_SETS.size(); i++) {
                    final int offset = i;

                    defineMethod(SPECIALIZED_SETS.get(offset), ACC_PUBLIC, sig(void.class, IRubyObject.class), new CodeBlock() {{
                        line(6);

                        if (size <= offset) {
                            newobj(p(RuntimeException.class));
                            dup();
                            ldc(clsName + " only supports scopes with " + size + " variables");
                            invokespecial(p(RuntimeException.class), "<init>", sig(void.class, String.class));
                            athrow();
                        } else {
                            aload(0);
                            aload(1);
                            putfield(clsPath, newFields[offset], ci(IRubyObject.class));
                            voidreturn();
                        }
                    }});
                }

                // fields
                for (String prop : newFields) {
                    defineField(prop, ACC_PUBLIC, ci(IRubyObject.class), null);
                }
            }};

            p = defineClass(jiteClass);

            MethodHandle mh = MethodHandles.lookup().findConstructor(p, MethodType.methodType(void.class, StaticScope.class, DynamicScope.class));
            mh = mh.asType(MethodType.methodType(DynamicScope.class, StaticScope.class, DynamicScope.class));
            MethodHandle previousMH = prototypes.putIfAbsent(size, mh);
            if (previousMH != null) mh = previousMH;

            return mh;
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
        return DynamicScope.newDynamicScope(staticScope, parent);
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

    public void growIfNeeded() {
        throw new RuntimeException("BUG: scopes of type " + getClass().getName() + " cannot grow");
    }

    public DynamicScope cloneScope() {
        try {
            return (DynamicScope) clone();
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException("failed to clone scope type " + getClass().getName());
        }
    }

    public IRubyObject[] getValues() {
        int numberOfVariables = staticScope.getNumberOfVariables();
        IRubyObject[] values = new IRubyObject[numberOfVariables];
        for (int i = 0; i < numberOfVariables; i++) {
            values[i] = getValueDepthZero(i);
        }
        return values;
    };

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
        return getValueDepthZero(0);
    }

    /**
     * getValue for index 1, depth 0
     */
    public IRubyObject getValueOneDepthZero() {
        return getValueDepthZero(1);
    }

    /**
     * getValue for index 2, depth 0
     */
    public IRubyObject getValueTwoDepthZero() {
        return getValueDepthZero(2);
    }

    /**
     * getValue for index 3, depth 0
     */
    public IRubyObject getValueThreeDepthZero() {
        return getValueDepthZero(3);
    }

    /**
     * getValue for index 4, depth 0
     */
    public IRubyObject getValueFourDepthZero() {
        return getValueDepthZero(4);
    }

    /**
     * getValue for index 5, depth 0
     */
    public IRubyObject getValueFiveDepthZero() {
        return getValueDepthZero(5);
    }

    /**
     * getValue for index 6, depth 0
     */
    public IRubyObject getValueSixDepthZero() {
        return getValueDepthZero(6);
    }

    /**
     * getValue for index 7, depth 0
     */
    public IRubyObject getValueSevenDepthZero() {
        return getValueDepthZero(7);
    }

    /**
     * getValue for index 8, depth 0
     */
    public IRubyObject getValueEightDepthZero() {
        return getValueDepthZero(8);
    }

    /**
     * getValue for index 9, depth 0
     */
    public IRubyObject getValueNineDepthZero() {
        return getValueDepthZero(9);
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
        IRubyObject value = getValueZeroDepthZero();
        return value == null ? setValueDepthZero(nil, 0) : value;
    }

    /**
     * getValueOrNil for index 1, depth 0
     */
    public IRubyObject getValueOneDepthZeroOrNil(IRubyObject nil) {
        IRubyObject value = getValueOneDepthZero();
        return value == null ? setValueDepthZero(nil, 1) : value;
    }

    /**
     * getValueOrNil for index 2, depth 0
     */
    public IRubyObject getValueTwoDepthZeroOrNil(IRubyObject nil) {
        IRubyObject value = getValueTwoDepthZero();
        return value == null ? setValueDepthZero(nil, 2) : value;
    }

    /**
     * getValueOrNil for index 3, depth 0
     */
    public IRubyObject getValueThreeDepthZeroOrNil(IRubyObject nil) {
        IRubyObject value = getValueThreeDepthZero();
        return value == null ? setValueDepthZero(nil, 3) : value;
    }

    /**
     * Set value in current dynamic scope or one of its captured scopes.
     *
     * @param offset zero-indexed value that represents where variable lives
     * @param value to set
     * @param depth how many captured scopes down this variable should be set
     */
    public IRubyObject setValue(int offset, IRubyObject value, int depth) {
        setValueVoid(value, offset, depth);
        return value;
    }

    /**
     * Set value in current dynamic scope or one of its captured scopes.
     *
     * @param offset zero-indexed value that represents where variable lives
     * @param value to set
     * @param depth how many captured scopes down this variable should be set
     */
    public IRubyObject setValue(IRubyObject value, int offset, int depth) {
        setValueVoid(value, offset, depth);
        return value;
    }

    /**
     * Set value in current dynamic scope or one of its captured scopes.
     *
     * @param offset zero-indexed value that represents where variable lives
     * @param value to set
     * @param depth how many captured scopes down this variable should be set
     */
    public abstract void setValueVoid(IRubyObject value, int offset, int depth);

    /**
     * setValue for depth zero
     *
     * @param value to set
     * @param offset zero-indexed value that represents where variable lives
     */
    public IRubyObject setValueDepthZero(IRubyObject value, int offset) {
        setValueDepthZeroVoid(value, offset);
        return value;
    }

    /**
     * setValue for depth zero
     *
     * @param value to set
     * @param offset zero-indexed value that represents where variable lives
     */
    public void setValueDepthZeroVoid(IRubyObject value, int offset) {
        setValueVoid(value, offset, 0);
    }

    /**
     * Set value zero in this scope;
     */
    public IRubyObject setValueZeroDepthZero(IRubyObject value) {
        setValueZeroDepthZeroVoid(value);
        return value;
    }

    /**
     * Set value zero in this scope;
     */
    public void setValueZeroDepthZeroVoid(IRubyObject value) {
        setValueDepthZeroVoid(value, 0);
    }

    /**
     * Set value one in this scope.
     */
    public IRubyObject setValueOneDepthZero(IRubyObject value) {
        setValueOneDepthZeroVoid(value);
        return value;
    }

    /**
     * Set value one in this scope.
     */
    public void setValueOneDepthZeroVoid(IRubyObject value) {
        setValueDepthZeroVoid(value, 1);
    }

    /**
     * Set value two in this scope.
     */
    public IRubyObject setValueTwoDepthZero(IRubyObject value) {
        setValueTwoDepthZeroVoid(value);
        return value;
    }

    /**
     * Set value two in this scope.
     */
    public void setValueTwoDepthZeroVoid(IRubyObject value) {
        setValueDepthZeroVoid(value, 2);
    }

    /**
     * Set value three in this scope.
     */
    public IRubyObject setValueThreeDepthZero(IRubyObject value) {
        setValueThreeDepthZeroVoid(value);
        return value;
    }

    /**
     * Set value three in this scope.
     */
    public void setValueThreeDepthZeroVoid(IRubyObject value) {
        setValueDepthZeroVoid(value, 3);
    }

    /**
     * Set value four in this scope.
     */
    public void setValueFourDepthZeroVoid(IRubyObject value) {
        setValueDepthZeroVoid(value, 4);
    }

    /**
     * Set value five in this scope.
     */
    public void setValueFiveDepthZeroVoid(IRubyObject value) {
        setValueDepthZeroVoid(value, 5);
    }

    /**
     * Set value six in this scope.
     */
    public void setValueSixDepthZeroVoid(IRubyObject value) {
        setValueDepthZeroVoid(value, 6);
    }

    /**
     * Set value seven in this scope.
     */
    public void setValueSevenDepthZeroVoid(IRubyObject value) {
        setValueDepthZeroVoid(value, 7);
    }

    /**
     * Set value eight in this scope.
     */
    public void setValueEightDepthZeroVoid(IRubyObject value) {
        setValueDepthZeroVoid(value, 8);
    }

    /**
     * Set value nine in this scope.
     */
    public void setValueNineDepthZeroVoid(IRubyObject value) {
        setValueDepthZeroVoid(value, 9);
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
