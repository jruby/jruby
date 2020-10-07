package org.jruby.runtime.scope;

import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.JDKVersion;
import me.qmx.jitescript.JiteClass;
import org.jruby.Ruby;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ClassDefiningClassLoader;
import org.jruby.util.ClassDefiningJRubyClassLoader;
import org.jruby.util.OneShotClassLoader;
import org.jruby.util.collections.NonBlockingHashMapLong;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.LabelNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

/**
 * A generator for DynamicScope subclasses, using fields for storage and specializing appropriate methods.
 */
public class DynamicScopeGenerator {
    private static final NonBlockingHashMapLong<MethodHandle> specializedFactories = new NonBlockingHashMapLong<>();
    private static final ClassDefiningClassLoader CDCL = new OneShotClassLoader(Ruby.getClassLoader());

    public static final String SCOPES_PACKAGE = "org.jruby.runtime.scopes";
    public static final String SCOPES_PATH = SCOPES_PACKAGE.replaceAll("\\.", "/");

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
        ClassDefiningClassLoader cdcl = CDCL;

        MethodHandle h = getClassFromSize(size);

        if (h != null) return h;

        Class p = loadClassForSize(cdcl, size);

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

    /**
     * Pregenerate a number of scope shapes to the path given.
     *
     * @param args args[0] should be the path in which the classes are dumped
     */
    public static void main(String[] args) {
        String targetPath = args[0];
        Map<String, byte[]> definedClasses = new HashMap<>();

        ClassDefiningClassLoader cdcl = new OneShotClassLoader(DynamicScopeGenerator.class.getClassLoader()) {
            @Override

            public Class<?> defineClass(String name, byte[] bytes) {
                definedClasses.put(name, bytes);
                Class<?> cls = super.defineClass(name, bytes, 0, bytes.length, ClassDefiningJRubyClassLoader.DEFAULT_DOMAIN);
                resolveClass(cls);
                return cls;
            }
        };

        // Generate from zero to MAX all dynamic scope sizes we would specialize at runtime
        for (int i = 0; i <= StaticScope.MAX_SPECIALIZED_SIZE; i++) {
            generateClassForSize(cdcl, i);
        }

        new File(targetPath + "/" + SCOPES_PATH).mkdirs();

        definedClasses.forEach((key, value) -> {
            try (FileOutputStream fos = new FileOutputStream(targetPath + "/" + key.replaceAll("\\.", "/") + ".class")) {
                fos.write(value);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public static Class generateClassForSize(ClassDefiningClassLoader cdcl, int size) {
        final String clsPath = SCOPES_PATH + "/DynamicScope" + size;
        final String clsName = clsPath.replace('/', '.');

        return generateInternal(cdcl, size, clsPath, clsName);
    }

    private static Class loadClassForSize(ClassDefiningClassLoader cdcl, int size) {
        final String clsPath = SCOPES_PATH + "/DynamicScope" + size;
        final String clsName = clsPath.replace('/', '.');

        // try to load the class, in case we have parallel generation happening
        Class p;

        try {
            p = cdcl.loadClass(clsName);
        } catch (ClassNotFoundException cnfe) {
            // try again under lock
            synchronized (cdcl) {
                try {
                    p = cdcl.loadClass(clsName);
                } catch (ClassNotFoundException cnfe2) {
                    // proceed to actually generate the class
                    p = generateInternal(cdcl, size, clsPath, clsName);
                }
            }
        }
        return p;
    }

    private static Class generateInternal(final ClassDefiningClassLoader cdcl, final int size, final String clsPath, final String clsName) {
        // ensure only one thread will attempt to generate and define the new class
        synchronized (cdcl) {
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

            return defineClass(cdcl, jiteClass);
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

    private static Class defineClass(ClassDefiningClassLoader cdcl, JiteClass jiteClass) {
        return cdcl.defineClass(classNameFromJiteClass(jiteClass), jiteClass.toBytes(JDKVersion.V1_7));
    }

    private static String classNameFromJiteClass(JiteClass jiteClass) {
        return jiteClass.getClassName().replace('/', '.');
    }

    private static String[] varList(int size) {
        String[] vars = new String[size];

        for (int i = 0; i < size; i++) {
            vars[i] = "var" + i;
        }

        return vars;
    }
}
