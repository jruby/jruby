package org.jruby.runtime.scope;

import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.JDKVersion;
import me.qmx.jitescript.JiteClass;
import org.jruby.Ruby;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ClassDefiningClassLoader;
import org.jruby.util.OneShotClassLoader;
import org.jruby.util.collections.NonBlockingHashMapLong;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.LabelNode;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

/**
 * A generator for DynamicScope subclasses, using fields for storage and specializing appropriate methods.
 */
public class DynamicScopeGenerator {
    private static final NonBlockingHashMapLong<MethodHandle> specializedFactories = new NonBlockingHashMapLong<>();
    private static ClassDefiningClassLoader CDCL = new OneShotClassLoader(Ruby.getClassLoader());

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

    public static final List<String> SPECIALIZED_GETS_OR_NIL = Collections.unmodifiableList(Arrays.asList(
            "getValueZeroDepthZeroOrNil",
            "getValueOneDepthZeroOrNil",
            "getValueTwoDepthZeroOrNil",
            "getValueThreeDepthZeroOrNil",
            "getValueFourDepthZeroOrNil",
            "getValueFiveDepthZeroOrNil",
            "getValueSixDepthZeroOrNil",
            "getValueSevenDepthZeroOrNil",
            "getValueEightDepthZeroOrNil",
            "getValueNineDepthZeroOrNil"
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
        MethodHandle h = getClassFromSize(size);

        if (h != null) return h;

        final String clsPath = "org/jruby/runtime/scopes/DynamicScope" + size;
        final String clsName = clsPath.replaceAll("/", ".");

        // try to load the class, in case we have parallel generation happening
        Class p;

        try {
            p = CDCL.loadClass(clsName);
        } catch (ClassNotFoundException cnfe) {
            // try again under lock
            synchronized (CDCL) {
                try {
                    p = CDCL.loadClass(clsName);
                } catch (ClassNotFoundException cnfe2) {
                    // proceed to actually generate the class
                    p = generateInternal(size, clsPath, clsName);
                }
            }
        }

        // acquire constructor handle and store it
        try {
            MethodHandle mh = MethodHandles.lookup().findConstructor(p, MethodType.methodType(void.class, StaticScope.class, DynamicScope.class));
            mh = mh.asType(MethodType.methodType(DynamicScope.class, StaticScope.class, DynamicScope.class));
            MethodHandle previousMH = specializedFactories.putIfAbsent(size, mh);
            if (previousMH != null) mh = previousMH;

            return mh;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Class generateInternal(final int size, final String clsPath, final String clsName) {
        // ensure only one thread will attempt to generate and define the new class
        synchronized (CDCL) {
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

                    line(0);

                    iload(2); // depth
                    ifne(parentCall);

                    if (size > 0) genGetSwitch(clsPath, newFields, this, 1);

                    line(1);

                    invokestatic(clsPath, "sizeError", sig(RuntimeException.class));
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

                    line(3);
                    iload(3); // depth
                    ifne(parentCall);

                    if (size > 0) genPutSwitch(clsPath, newFields, this, 2);

                    line(4);

                    invokestatic(clsPath, "sizeError", sig(RuntimeException.class));
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
                    line(6);

                    if (size > 0) genGetSwitch(clsPath, newFields, this, 1);

                    line(1);

                    invokestatic(clsPath, "sizeError", sig(RuntimeException.class));
                    athrow();
                }});

                defineMethod("setValueDepthZeroVoid", ACC_PUBLIC, sig(void.class, IRubyObject.class, int.class), new CodeBlock() {{
                    line(6);

                    if (size > 0) genPutSwitch(clsPath, newFields, this, 2);

                    line(1);

                    invokestatic(clsPath, "sizeError", sig(RuntimeException.class));
                    athrow();
                }});

                for (int i = 0; i < SPECIALIZED_GETS.size(); i++) {
                    final int offset = i;

                    defineMethod(SPECIALIZED_GETS.get(offset), ACC_PUBLIC, sig(IRubyObject.class), new CodeBlock() {{
                        line(6);

                        if (size <= offset) {
                            invokestatic(clsPath, "sizeError", sig(RuntimeException.class));
                            athrow();
                        } else {
                            aload(0);
                            getfield(clsPath, newFields[offset], ci(IRubyObject.class));
                            areturn();
                        }
                    }});
                }

                for (int i = 0; i < SPECIALIZED_GETS_OR_NIL.size(); i++) {
                    final int offset = i;

                    defineMethod(SPECIALIZED_GETS_OR_NIL.get(offset), ACC_PUBLIC, sig(IRubyObject.class, IRubyObject.class), new CodeBlock() {{
                        line(6);

                        if (size <= offset) {
                            invokestatic(clsPath, "sizeError", sig(RuntimeException.class));
                            athrow();
                        } else {
                            aload(0);
                            getfield(clsPath, newFields[offset], ci(IRubyObject.class));

                            dup();

                            LabelNode ok = new LabelNode(new Label());
                            ifnonnull(ok);

                            pop();

                            aload(0);
                            aload(1);
                            putfield(clsPath, newFields[offset], ci(IRubyObject.class));
                            aload(1);

                            label(ok);
                            areturn();
                        }
                    }});
                }

                for (int i = 0; i < SPECIALIZED_SETS.size(); i++) {
                    final int offset = i;

                    defineMethod(SPECIALIZED_SETS.get(offset), ACC_PUBLIC, sig(void.class, IRubyObject.class), new CodeBlock() {{
                        line(6);

                        if (size <= offset) {
                            invokestatic(clsPath, "sizeError", sig(RuntimeException.class));
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

                // utilities
                defineMethod("sizeError", ACC_PRIVATE | ACC_STATIC, sig(RuntimeException.class), new CodeBlock() {{
                    newobj(p(RuntimeException.class));
                    dup();
                    ldc(clsName + " only supports scopes with " + size + " variables");
                    invokespecial(p(RuntimeException.class), "<init>", sig(void.class, String.class));
                    areturn();
                }});
            }};

            return defineClass(jiteClass);
        }
    }

    private static void genGetSwitch(String clsPath, String[] newFields, CodeBlock block, int offsetVar) {
        LabelNode defaultError = new LabelNode(new Label());
        int size = newFields.length;
        LabelNode[] cases = new LabelNode[size];
        for (int i = 0; i < size; i++) {
            cases[i] = new LabelNode(new Label());
        }
        block.iload(offsetVar);
        block.tableswitch(0, size - 1, defaultError, cases);
        for (int i = 0; i < size; i++) {
            block.label(cases[i]);
            block.aload(0);
            block.getfield(clsPath, newFields[i], ci(IRubyObject.class));
            block.areturn();
        }
        block.label(defaultError);
    }

    private static void genPutSwitch(String clsPath, String[] newFields, CodeBlock block, int offsetVar) {
        LabelNode defaultError = new LabelNode(new Label());
        int size = newFields.length;
        LabelNode[] cases = new LabelNode[size];
        for (int i = 0; i < size; i++) {
            cases[i] = new LabelNode(new Label());
        }
        block.iload(offsetVar);
        block.tableswitch(0, size - 1, defaultError, cases);
        for (int i = 0; i < size; i++) {
            block.label(cases[i]);
            block.aload(0);
            block.aload(1);
            block.putfield(clsPath, newFields[i], ci(IRubyObject.class));
            block.voidreturn();
        }
        block.label(defaultError);
    }

    private static MethodHandle getClassFromSize(int size) {
        return specializedFactories.get(size);
    }

    private static Class defineClass(JiteClass jiteClass) {
        return CDCL.defineClass(classNameFromJiteClass(jiteClass), jiteClass.toBytes(JDKVersion.V1_7));
    }

    private static Class loadClass(JiteClass jiteClass) throws ClassNotFoundException {
        return CDCL.loadClass(classNameFromJiteClass(jiteClass));
    }

    private static String classNameFromJiteClass(JiteClass jiteClass) {
        return jiteClass.getClassName().replaceAll("/", ".");
    }

    private static String[] varList(int size) {
        String[] vars = new String[size];

        for (int i = 0; i < size; i++) {
            vars[i] = "var" + i;
        }

        return vars;
    }
}
