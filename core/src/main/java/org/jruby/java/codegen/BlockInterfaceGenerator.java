package org.jruby.java.codegen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentMap;

import org.jruby.Ruby;
import org.jruby.RubyProc;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.java.proxies.BlockInterfaceTemplate;
import org.jruby.javasupport.Java;
import org.jruby.util.ASM;
import org.jruby.util.ClassDefiningClassLoader;
import org.jruby.util.JRubyClassLoader;
import org.jruby.util.collections.ConcurrentWeakHashMap;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.getBoxType;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.V11;

/**
 * Generates a concrete subclass of {@link BlockInterfaceTemplate} for a Java interface, where
 * every abstract method delegates straight into one of the inherited {@code __ruby_call} helpers.
 *
 * Default methods on the interface are intentionally <em>not</em> overridden, matching the pre-existing
 * {@code convertProcToInterface} behavior where only abstract methods were backed by the proc.
 *
 * <p>For an interface such as:
 * <pre>{@code
 *   public interface MyIface {
 *       int compute(int a, String b);
 *       default int compute2(int a, String b) { return compute(a, b) * 2; }
 *   }
 * }</pre>
 * the generated class is equivalent to:
 * <pre>{@code
 *   public final class BlockInterfaceImpl$<hash> extends BlockInterfaceTemplate implements MyIface {
 *       public BlockInterfaceImpl$<hash>(RubyProc proc) { super(proc); }
 *
 *       public int compute(int a, String b) {
 *           Object result = __ruby_call(Integer.TYPE, Integer.valueOf(a), b);
 *           return ((Number) result).intValue();
 *       }
 *       // compute2 is not overridden — the interface default is used
 *   }
 * }</pre>
 */
public final class BlockInterfaceGenerator {

    /**
     * Cache of generated {@link BlockInterfaceTemplate} subclass constructors, weakly keyed by the interface.
     *
     * <p>
     * The generated class lives in the {@link JRubyClassLoader}, so the value never roots a classloader outside of
     * JRuby itself; the only strong root held by a cache entry is the constructor (class pair) for the interface.
     */
    static final ConcurrentMap<Class<?>, Constructor<? extends BlockInterfaceTemplate>> CACHE = new ConcurrentWeakHashMap<>();

    public static Constructor<? extends BlockInterfaceTemplate> fromCache(final Class<?> interfaceType) {
        return CACHE.get(interfaceType);
    }

    /**
     * @return the constructor, which is usable with any Ruby block targeting the same interface
     */
    @SuppressWarnings("unchecked")
    public static Constructor<? extends BlockInterfaceTemplate> getConstructor(final Ruby runtime, final Class<?> interfaceType)
        throws ReflectiveOperationException {

        var constructor = CACHE.get(interfaceType);
        if (constructor != null) return constructor;

        assert interfaceType.isInterface();

        final String implClassName = makeImplClassName(interfaceType);
        final JRubyClassLoader loader = runtime.getJRubyClassLoader();

        synchronized (loader) {
            constructor = CACHE.get(interfaceType);
            if (constructor != null) return constructor;

            Class<?> implClass;
            try {
                implClass = Class.forName(implClassName, true, loader);
            } catch (ClassNotFoundException e) {
                assert Java.getFunctionalInterfaceMethod(interfaceType) != null : "not a functional-interface: " + interfaceType;
                implClass = defineImplClass(loader, interfaceType, implClassName);
            }

            constructor = (Constructor<? extends BlockInterfaceTemplate>) implClass.getConstructor(RubyProc.class);
            CACHE.put(interfaceType, constructor);
            return constructor;
        }
    }

    private static String makeImplClassName(final Class<?> interfaceType) {
        return "org.jruby.gen.BlockInterfaceImpl$" + interfaceType.getSimpleName() + Math.abs(interfaceType.hashCode());
    }

    /**
     * Emits the class header, the single {@code (RubyProc)} constructor, and one bridge method
     * per abstract method collected from the interface. Equivalent to:
     * <pre>{@code
     *   public final class <implClassName>
     *           extends BlockInterfaceTemplate
     *           implements <interfaceType> { ... }
     * }</pre>
     */
    private static Class<?> defineImplClass(final ClassDefiningClassLoader loader,
                                            final Class<?> interfaceType,
                                            final String implClassName) {
        final ClassWriter cw = ASM.newClassWriter((ClassLoader) loader);
        final String pathName = implClassName.replace('.', '/');

        cw.visit(V11,
                ACC_PUBLIC | ACC_SUPER | ACC_SYNTHETIC,
                pathName,
                null,
                p(BlockInterfaceTemplate.class),
                new String[] { p(interfaceType) });
        cw.visitSource(pathName + ".gen", null);

        defineConstructor(cw, pathName);
        Method implMethod = Java.getFunctionalInterfaceMethod(interfaceType);
        if (implMethod != null) defineBridgeMethod(cw, implMethod);

        cw.visitEnd();

        final byte[] bytecode = cw.toByteArray();
        return loader.defineClass(implClassName, bytecode);
    }

    /**
     * <pre>{@code
     *   public <implClass>(RubyProc proc) { super(proc); }
     * }</pre>
     */
    private static void defineConstructor(ClassWriter cw, String pathName) {
        final SkinnyMethodAdapter init = new SkinnyMethodAdapter(cw, ACC_PUBLIC, "<init>",
                sig(void.class, RubyProc.class), null, null);
        init.start();
        init.aload(0);
        init.aload(1);
        init.invokespecial(p(BlockInterfaceTemplate.class), "<init>", sig(void.class, RubyProc.class));
        init.voidreturn();
        init.end();
    }

    /**
     * Emits a bridge method that calls {@code __ruby_call(returnType, boxedArgs...)} on the inherited
     * {@link BlockInterfaceTemplate} and unboxes/casts the result to interface method's declared return type.
     *
     * <p>For a one-arg method {@code R accept(A a)}:
     * <pre>{@code
     *   public R accept(A a) {
     *       Object result = __ruby_call(<returnClass>, box(a));
     *       return (R) result;
     *   }
     * }</pre>
     */
    private static void defineBridgeMethod(ClassWriter cw, Method method) {
        final Class<?> returnType = method.getReturnType();
        final Class<?>[] paramTypes = method.getParameterTypes();
        final SkinnyMethodAdapter mv = new SkinnyMethodAdapter(cw, ACC_PUBLIC, method.getName(), sig(returnType, paramTypes), null, null);
        final GeneratorAdapter ga = RealClassGenerator.makeGenerator(mv);

        mv.start();
        // `this.__ruby_call(...)` - push receiver + the return-type class literal for every arity
        mv.aload(0);
        pushClassLiteral(mv, returnType);

        switch (paramTypes.length) {
            case 0: // __ruby_call(<returnClass>)
                mv.invokevirtual(p(BlockInterfaceTemplate.class), "__ruby_call", sig(Object.class, Class.class));
                break;
            case 1: // __ruby_call(<returnClass>, box(arg0))
                loadBoxedArg(ga, 0, paramTypes[0]);
                mv.invokevirtual(p(BlockInterfaceTemplate.class), "__ruby_call", sig(Object.class, Class.class, Object.class));
                break;
            case 2: // __ruby_call(<returnClass>, box(arg0), box(arg1))
                loadBoxedArg(ga, 0, paramTypes[0]);
                loadBoxedArg(ga, 1, paramTypes[1]);
                mv.invokevirtual(p(BlockInterfaceTemplate.class), "__ruby_call", sig(Object.class, Class.class, Object.class, Object.class));
                break;
            default: // __ruby_call(<returnClass>, new Object[] { box(arg0), box(arg1), ... })
                mv.pushInt(paramTypes.length);
                mv.anewarray(p(Object.class));
                for (int i = 0; i < paramTypes.length; i++) {
                    mv.dup();
                    mv.pushInt(i);
                    loadBoxedArg(ga, i, paramTypes[i]);
                    mv.aastore();
                }
                mv.invokevirtual(p(BlockInterfaceTemplate.class), "__ruby_call", sig(Object.class, Class.class, Object[].class));
                break;
        }

        emitReturn(mv, returnType);
        mv.end();
    }

    /**
     * Loads {@code argIndex} from the Java arg slots and boxes it if primitive.
     */
    private static void loadBoxedArg(GeneratorAdapter ga, int argIndex, Class<?> paramType) {
        ga.loadArg(argIndex);
        if (paramType.isPrimitive()) ga.box(Type.getType(paramType));
    }

    /**
     * Pushes the {@code Class<?>} literal for {@code type} on the stack.
     *
     * <ul>
     *   <li>primitive (non-void) → {@code Integer.TYPE}, {@code Long.TYPE}, ...
     *   <li>{@code void}         → {@code Void.TYPE}
     *   <li>reference            → {@code ldc <Type>.class}
     * </ul>
     */
    private static void pushClassLiteral(SkinnyMethodAdapter mv, Class<?> type) {
        if (type.isPrimitive()) {
            if (type == void.class) {
                mv.getstatic(p(Void.class), "TYPE", ci(Class.class));
            } else {
                mv.getstatic(p(getBoxType(type)), "TYPE", ci(Class.class));
            }
        } else {
            mv.ldc(Type.getType(type));
        }
    }

    private static void emitReturn(SkinnyMethodAdapter mv, Class<?> returnType) {
        RealClassGenerator.coerceResultAndReturn(mv, returnType);
    }
}
