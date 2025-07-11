/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 The JRuby Community <www.jruby.org>
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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

package org.jruby.internal.runtime.methods;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JavaMethodDescriptor;
import org.jruby.anno.TypePopulator;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.MethodFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ClassDefiningJRubyClassLoader;
import org.jruby.util.CodegenUtils;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.params;
import static org.jruby.util.CodegenUtils.sig;

/**
 * In order to avoid the overhead with reflection-based method handles, this
 * MethodFactory uses ASM to generate tiny invoker classes. This allows for
 * better performance and more specialization per-handle than can be supported
 * via reflection. It also allows optimizing away many conditionals that can
 * be determined once ahead of time.
 *
 * When running in secured environments, this factory may not function. When
 * this can be detected, MethodFactory will fall back on the reflection-based
 * factory instead.
 *
 * @see org.jruby.runtime.MethodFactory
 */
public class InvocationMethodFactory extends MethodFactory implements Opcodes {

    private static final Logger LOG = LoggerFactory.getLogger(InvocationMethodFactory.class);

    private static final boolean DEBUG = false;

    /** The outward call signature for compiled Ruby method handles. */
    private final static String COMPILED_CALL_SIG = sig(IRubyObject.class,
            params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject[].class));

    /** The outward call signature for compiled Ruby method handles. */
    private final static String COMPILED_CALL_SIG_BLOCK = sig(IRubyObject.class,
            params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject[].class, Block.class));

    /** The outward arity-zero call-with-block signature for compiled Ruby method handles. */
    private final static String COMPILED_CALL_SIG_ZERO_BLOCK = sig(IRubyObject.class,
            params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, Block.class));

    /** The outward arity-zero call-without-block signature for compiled Ruby method handles. */
    private final static String COMPILED_CALL_SIG_ZERO = sig(IRubyObject.class,
            params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class));

    /** The outward arity-one call-with-block signature for compiled Ruby method handles. */
    private final static String COMPILED_CALL_SIG_ONE_BLOCK = sig(IRubyObject.class,
            params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, Block.class));

    /** The outward arity-one call-without-block signature for compiled Ruby method handles. */
    private final static String COMPILED_CALL_SIG_ONE = sig(IRubyObject.class,
            params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class));

    /** The outward arity-two call-with-block signature for compiled Ruby method handles. */
    private final static String COMPILED_CALL_SIG_TWO_BLOCK = sig(IRubyObject.class,
            params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class, Block.class));

    /** The outward arity-two call-without-block signature for compiled Ruby method handles. */
    private final static String COMPILED_CALL_SIG_TWO = sig(IRubyObject.class,
            params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class));

    /** The outward arity-three call-with-block signature for compiled Ruby method handles. */
    private final static String COMPILED_CALL_SIG_THREE_BLOCK = sig(IRubyObject.class,
            params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));

    /** The outward arity-three call-without-block signature for compiled Ruby method handles. */
    private final static String COMPILED_CALL_SIG_THREE = sig(IRubyObject.class,
            params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));

    /** The super constructor signature for Java-based method handles. */
    private final static String JAVA_SUPER_SIG = sig(Void.TYPE, params(RubyModule.class, Visibility.class, String.class));

    /** The lvar index of "this" */
    public static final int THIS_INDEX = 0;

    /** The lvar index of the passed-in ThreadContext */
    public static final int THREADCONTEXT_INDEX = 1;

    /** The lvar index of the method-receiving object */
    public static final int RECEIVER_INDEX = 2;

    /** The lvar index of the RubyClass being invoked against */
    public static final int CLASS_INDEX = 3;

    /** The lvar index method name being invoked */
    public static final int NAME_INDEX = 4;

    /** The lvar index of the method args on the call */
    public static final int ARGS_INDEX = 5;

    /** The lvar index of the passed-in Block on the call */
    public static final int BLOCK_INDEX = 6;

    /** The classloader to use for code loading */
    protected final ClassDefiningJRubyClassLoader classLoader;

    /** An object to sync against when loading classes, to avoid dups */
    protected final Object syncObject;

    /**
     * Whether this factory has seen undefined methods already. This is used to
     * detect likely method handle collisions when we expect to create a new
     * handle for each call.
     */
    private boolean seenUndefinedClasses = false;

    /**
     * Whether we've informed the user that we've seen undefined methods; this
     * is to avoid a flood of repetitive information.
     */
    private boolean haveWarnedUser = false;

    /**
     * Construct a new InvocationMethodFactory using the specified classloader
     * to load code. If the target classloader is not an instance of
     * JRubyClassLoader, it will be wrapped with one.
     *
     * @param classLoader The classloader to use, or to wrap if it is not a
     * JRubyClassLoader instance.
     */
    public InvocationMethodFactory(ClassLoader classLoader) {
        // use the given classloader as our sync, regardless of whether we wrap it
        this.syncObject = classLoader;

        if (classLoader instanceof ClassDefiningJRubyClassLoader) {
            this.classLoader = (ClassDefiningJRubyClassLoader) classLoader;
        } else {
            this.classLoader = new ClassDefiningJRubyClassLoader(classLoader);
        }
    }

    //protected boolean safeFixedSignature(Signature signature) {
    //    return signature.isFixed() && signature.required() <= 3;
    //}

    private static final Class[] RubyModule_and_Visibility_and_Name = new Class[]{ RubyModule.class, Visibility.class, String.class };

    /**
     * Use code generation to provide a method handle based on an annotated Java
     * method.
     *
     * @see org.jruby.runtime.MethodFactory#getAnnotatedMethod
     */
    public DynamicMethod getAnnotatedMethod(RubyModule implementationClass, List<JavaMethodDescriptor> descs, String name) {
        JavaMethodDescriptor desc1 = descs.get(0);
        final String javaMethodName = desc1.name;

        if (DEBUG) LOG.debug("Binding multiple: " + desc1.declaringClassName + '.' + javaMethodName);

        try {
            Class c = getAnnotatedMethodClass(descs);

            DescriptorInfo info = new DescriptorInfo(descs);
            if (DEBUG) LOG.debug(" min: " + info.getMin() + ", max: " + info.getMax());

            JavaMethod ic = constructJavaMethod(implementationClass, desc1, name, c);

            TypePopulator.populateMethod(
                    ic,
                    Arity.optional().getValue(),
                    javaMethodName,
                    desc1.isStatic,
                    desc1.anno.notImplemented(),
                    implementationClass.getRuntime().isBootingCore(),
                    desc1.declaringClass,
                    desc1.name,
                    desc1.returnClass,
                    desc1.parameters);
            return ic;
        } catch(Exception e) {
            LOG.error(e);
            throw implementationClass.getRuntime().newLoadError(e.getMessage());
        }
    }

    /**
     * Use code generation to provide a method handle based on an annotated Java
     * method. Return the resulting generated or loaded class.
     *
     * @see org.jruby.runtime.MethodFactory#getAnnotatedMethod
     */
    public Class getAnnotatedMethodClass(List<JavaMethodDescriptor> descs) {
        JavaMethodDescriptor desc1 = descs.get(0);

        if (!Modifier.isPublic(desc1.declaringClass.getModifiers())) {
            LOG.warn("binding non-public class {} reflected handles won't work", desc1.declaringClassName);
        }

        String javaMethodName = desc1.name;

        if (DEBUG) {
            if (descs.size() > 1) {
                LOG.debug("Binding multiple: " + desc1.declaringClassName + '.' + javaMethodName);
            } else {
                LOG.debug("Binding single: " + desc1.declaringClassName + '.' + javaMethodName);
            }
        }

        String generatedClassName = CodegenUtils.getAnnotatedBindingClassName(javaMethodName, desc1.declaringClassName, desc1.isStatic, desc1.actualRequired, desc1.optional, descs.size() > 1, desc1.anno.frame());
        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            // in debug mode we append _DBG to class name to force it to regenerate (or use pre-generated debug version)
            generatedClassName += "_DBG";
        }
        String generatedClassPath = generatedClassName.replace('.', '/');

        DescriptorInfo info = new DescriptorInfo(descs);

        Class superclass = determineSuperclass(info);

        Class c = tryClass(generatedClassName, generatedClassPath, desc1.declaringClass, superclass);
        if (c == null) {
            synchronized (syncObject) {
                // try again
                c = tryClass(generatedClassName, generatedClassPath, desc1.declaringClass, superclass);
                if (c == null) {
                    if (DEBUG) LOG.debug("Generating " + generatedClassName + ", min: " + info.getMin() + ", max: " + info.getMax() + ", hasBlock: " + info.isBlock() + ", rest: " + info.isRest());

                    String superClassString = p(superclass);

                    ClassWriter cw = createJavaMethodCtor(generatedClassPath, superClassString, info.getParameterDesc());

                    addAnnotatedMethodInvoker(cw, superClassString, descs, info.acceptsKeywords());

                    c = endClass(cw, generatedClassName);
                }
            }
        }

        return c;
    }

    private static Class determineSuperclass(DescriptorInfo info) {
        Class superClass;
        if (info.getMin() == -1) {
            // normal all-rest method
            if (info.isBlock()) {
                superClass = JavaMethod.JavaMethodNBlock.class;
            } else {
                superClass = JavaMethod.JavaMethodN.class;
            }
        } else {
            if (info.isRest()) {
                if (info.isBlock()) {
                    superClass = JavaMethod.BLOCK_REST_METHODS[info.getMin()][info.getMax()];
                } else {
                    superClass = JavaMethod.REST_METHODS[info.getMin()][info.getMax()];
                }
            } else {
                if (info.isBlock()) {
                    superClass = JavaMethod.BLOCK_METHODS[info.getMin()][info.getMax()];
                } else {
                    superClass = JavaMethod.METHODS[info.getMin()][info.getMax()];
                }
            }
        }

        if (superClass == null) throw new RuntimeException("invalid multi combination");
        return superClass;
    }

    /**
     * Use code generation to provide a method handle based on an annotated Java
     * method.
     *
     * @see org.jruby.runtime.MethodFactory#getAnnotatedMethod
     */
    public DynamicMethod getAnnotatedMethod(RubyModule implementationClass, JavaMethodDescriptor desc, String name) {
        String javaMethodName = desc.name;

        try {
            Class c = getAnnotatedMethodClass(Collections.singletonList(desc));
            JavaMethod ic = constructJavaMethod(implementationClass, desc, name, c);

            TypePopulator.populateMethod(
                    ic,
                    Arity.fromAnnotation(desc.anno, desc.actualRequired).getValue(),
                    javaMethodName,
                    desc.isStatic,
                    desc.anno.notImplemented(),
                    implementationClass.getRuntime().isBootingCore(),
                    desc.declaringClass,
                    desc.name,
                    desc.returnClass,
                    desc.parameters);
            return ic;
        } catch(Exception e) {
            LOG.error(e);
            throw implementationClass.getRuntime().newLoadError(e.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    public JavaMethod constructJavaMethod(RubyModule implementationClass, JavaMethodDescriptor desc, String name, Class c) throws InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException, NoSuchMethodException {
        // In order to support older versions of generated JavaMethod invokers, we check for the Version
        // annotation to be present and > 0. If absent, we use a thread local to allow the deprecated constructor
        // to still provide a final method name.
        DynamicMethod.Version version = (DynamicMethod.Version) c.getAnnotation(DynamicMethod.Version.class);
        JavaMethod ic;
        if (version == null) {
            // Old constructor with no name, use thread-local to pass it.
            JavaMethod.NAME_PASSER.set(name);
            try {
                ic = (JavaMethod) c.getConstructor(RubyModule_and_Visibility).newInstance(implementationClass, desc.anno.visibility().getDefaultVisibilityFor(name));
            } finally {
                JavaMethod.NAME_PASSER.remove();
            }
        } else {
            // New constructor with name.
            ic = (JavaMethod) c.getConstructor(RubyModule_and_Visibility_and_Name).newInstance(implementationClass, desc.anno.visibility().getDefaultVisibilityFor(name), name);
        }
        return ic;
    }

    /**
     * Emit code to check the arity of a call to a Java-based method.
     *
     * @param jrubyMethod The annotation of the called method
     * @param method The code generator for the handle being created
     */
    private static void checkArity(JRubyMethod jrubyMethod, SkinnyMethodAdapter method, int specificArity) {
        switch (specificArity) {
        case 0:
        case 1:
        case 2:
        case 3:
            // for zero, one, two, three arities, JavaMethod.JavaMethod*.call(...IRubyObject[] args...) will check
            return;
        default:
            final Label arityError = new Label();
            final Label noArityError = new Label();
            boolean checkArity = false;
            if (jrubyMethod.rest()) {
                if (jrubyMethod.required() > 0) {
                    // just confirm minimum args provided
                    method.aload(ARGS_INDEX);
                    method.arraylength();
                    method.ldc(jrubyMethod.required());
                    method.if_icmplt(arityError);
                    checkArity = true;
                }
            } else if (jrubyMethod.optional() > 0) {
                if (jrubyMethod.required() > 0) {
                    // confirm minimum args provided
                    method.aload(ARGS_INDEX);
                    method.arraylength();
                    method.ldc(jrubyMethod.required());
                    method.if_icmplt(arityError);
                }

                // confirm maximum not greater than optional
                method.aload(ARGS_INDEX);
                method.arraylength();
                method.ldc(jrubyMethod.required() + jrubyMethod.optional());
                method.if_icmpgt(arityError);
                checkArity = true;
            } else {
                // just confirm args length == required
                method.aload(ARGS_INDEX);
                method.arraylength();
                method.ldc(jrubyMethod.required());
                method.if_icmpne(arityError);
                checkArity = true;
            }

            if (checkArity) {
                method.go_to(noArityError);

                // Raise an error if arity does not match requirements
                method.label(arityError);
                method.aload(THREADCONTEXT_INDEX);
                method.invokevirtual(p(ThreadContext.class), "getRuntime", sig(Ruby.class));
                method.aload(ARGS_INDEX);
                method.ldc(jrubyMethod.required());
                method.ldc(jrubyMethod.required() + jrubyMethod.optional());
                method.invokestatic(p(Arity.class), "checkArgumentCount", sig(int.class, Ruby.class, IRubyObject[].class, int.class, int.class));
                method.pop();

                method.label(noArityError);
            }
        }
    }

    private static ClassWriter createJavaMethodCtor(String namePath, String sup, String parameterDesc) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        String sourceFile = namePath.substring(namePath.lastIndexOf('/') + 1) + ".gen";
        cw.visit(RubyInstanceConfig.JAVA_VERSION, ACC_PUBLIC + ACC_SUPER, namePath, null, sup, null);
        cw.visitSource(sourceFile, null);

        AnnotationVisitor av = cw.visitAnnotation(ci(DynamicMethod.Version.class), true);
        av.visit("version", 0);
        av.visitEnd();

        SkinnyMethodAdapter mv = new SkinnyMethodAdapter(cw, ACC_PUBLIC, "<init>", JAVA_SUPER_SIG, null, null);
        mv.start();
        mv.aloadMany(0, 1, 2, 3);
        mv.invokespecial(sup, "<init>", JAVA_SUPER_SIG);
        mv.aload(0);
        mv.ldc(parameterDesc.toString());
        mv.invokevirtual(p(JavaMethod.class), "setParameterDesc", sig(void.class, String.class));
        mv.voidreturn();
        mv.end();

        return cw;
    }

    private void invokeCallConfigPre(SkinnyMethodAdapter mv, String superClass, int specificArity, boolean block, CallConfiguration callConfig) {
        // invoke pre method stuff
        if (callConfig.isNoop()) return;

        prepareForPre(mv, specificArity, block, callConfig);
        mv.invokevirtual(superClass, getPreMethod(callConfig), getPreSignature(callConfig));
    }

    private void invokeCallConfigPost(SkinnyMethodAdapter mv, String superClass, CallConfiguration callConfig) {
        if (callConfig.isNoop()) return;

        mv.aload(1);
        mv.invokestatic(superClass, getPostMethod(callConfig), sig(void.class, params(ThreadContext.class)));
    }

    private static void prepareForPre(SkinnyMethodAdapter mv, int specificArity, boolean block, CallConfiguration callConfig) {
        if (callConfig.isNoop()) return;

        mv.aloadMany(0, THREADCONTEXT_INDEX);

        switch (callConfig.framing()) {
        case Full:
            mv.aloadMany(RECEIVER_INDEX, CLASS_INDEX, NAME_INDEX); // self, name
            loadBlockForPre(mv, specificArity, block);
            break;
        case Backtrace:
            mv.aload(NAME_INDEX); // name
            break;
        case None:
            break;
        default: throw new RuntimeException("Unknown call configuration");
        }
    }

    private static String getPreMethod(CallConfiguration callConfig) {
        switch (callConfig) {
        case FrameFullScopeFull: return "preFrameAndScope";
        case FrameFullScopeDummy: return "preFrameAndDummyScope";
        case FrameFullScopeNone: return "preFrameOnly";
        case FrameBacktraceScopeFull: return "preBacktraceAndScope";
        case FrameBacktraceScopeDummy: return "preBacktraceDummyScope";
        case FrameBacktraceScopeNone:  return "preBacktraceOnly";
        case FrameNoneScopeFull: return "preScopeOnly";
        case FrameNoneScopeDummy: return "preNoFrameDummyScope";
        case FrameNoneScopeNone: return "preNoop";
        default: throw new RuntimeException("Unknown call configuration");
        }
    }

    private static String getPreSignature(CallConfiguration callConfig) {
        switch (callConfig) {
        case FrameFullScopeFull: return sig(void.class, params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, Block.class));
        case FrameFullScopeDummy: return sig(void.class, params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, Block.class));
        case FrameFullScopeNone: return sig(void.class, params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, Block.class));
        case FrameBacktraceScopeFull: return sig(void.class, params(ThreadContext.class, String.class));
        case FrameBacktraceScopeDummy: return sig(void.class, params(ThreadContext.class, String.class));
        case FrameBacktraceScopeNone:  return sig(void.class, params(ThreadContext.class, String.class));
        case FrameNoneScopeFull: return sig(void.class, params(ThreadContext.class));
        case FrameNoneScopeDummy: return sig(void.class, params(ThreadContext.class));
        case FrameNoneScopeNone: return sig(void.class);
        default: throw new RuntimeException("Unknown call configuration");
        }
    }

    public static String getPostMethod(CallConfiguration callConfig) {
        switch (callConfig) {
        case FrameFullScopeFull: return "postFrameAndScope";
        case FrameFullScopeDummy: return "postFrameAndScope";
        case FrameFullScopeNone: return "postFrameOnly";
        case FrameBacktraceScopeFull: return "postBacktraceAndScope";
        case FrameBacktraceScopeDummy: return "postBacktraceDummyScope";
        case FrameBacktraceScopeNone:  return "postBacktraceOnly";
        case FrameNoneScopeFull: return "postScopeOnly";
        case FrameNoneScopeDummy: return "postNoFrameDummyScope";
        case FrameNoneScopeNone: return "postNoop";
        default: throw new RuntimeException("Unknown call configuration");
        }
    }

    private static void loadArguments(SkinnyMethodAdapter mv, JavaMethodDescriptor desc, int specificArity) {
        Class[] argumentTypes;
        switch (specificArity) {
        default:
        case -1:
            mv.aload(ARGS_INDEX);
            break;
        case 0:
            // no args
            break;
        case 1:
            argumentTypes = desc.getArgumentTypes();
            loadArgumentWithCast(mv, 1, argumentTypes[0]);
            break;
        case 2:
            argumentTypes = desc.getArgumentTypes();
            loadArgumentWithCast(mv, 1, argumentTypes[0]);
            loadArgumentWithCast(mv, 2, argumentTypes[1]);
            break;
        case 3:
            argumentTypes = desc.getArgumentTypes();
            loadArgumentWithCast(mv, 1, argumentTypes[0]);
            loadArgumentWithCast(mv, 2, argumentTypes[1]);
            loadArgumentWithCast(mv, 3, argumentTypes[2]);
            break;
        }
    }

    private static void loadArgumentWithCast(SkinnyMethodAdapter mv, int argNumber, Class coerceType) {
        mv.aload(ARGS_INDEX + (argNumber - 1));
        if (coerceType != IRubyObject.class && coerceType != IRubyObject[].class) {
            if (coerceType == RubyString.class) {
                mv.invokeinterface(p(IRubyObject.class), "convertToString", sig(RubyString.class));
            } else {
                throw new RuntimeException("Unknown coercion target: " + coerceType);
            }
        }
    }

    /** load block argument for pre() call.  Since we have fixed-arity call
     * paths we need calculate where the last var holding the block is.
     *
     * is we don't have a block we setup NULL_BLOCK as part of our null pattern
     * strategy (we do not allow null in any field which accepts block).
     */
    private static void loadBlockForPre(SkinnyMethodAdapter mv, int specificArity, boolean getsBlock) {
        if (!getsBlock) {            // No block so load null block instance
            mv.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));
            return;
        }

        loadBlock(mv, specificArity, getsBlock);
    }

    /** load the block argument from the correct position.  Since we have fixed-
     * arity call paths we need to calculate where the last var holding the
     * block is.
     *
     * If we don't have a block then this does nothing.
     */
    private static void loadBlock(SkinnyMethodAdapter mv, int specificArity, boolean getsBlock) {
        if (!getsBlock) return;         // No block so nothing more to do

        switch (specificArity) {        // load block since it accepts a block
        case 0: case 1: case 2: case 3: // Fixed arities signatures
            mv.aload(BLOCK_INDEX - 1 + specificArity);
            break;
        default: case -1:
            mv.aload(BLOCK_INDEX);      // Generic arity signature
            break;
        }
    }

    private static void loadReceiver(String typePath, JavaMethodDescriptor desc, SkinnyMethodAdapter mv) {
        // load target for invocations
        if (Modifier.isStatic(desc.modifiers)) {
            if (desc.hasContext) {
                mv.aload(THREADCONTEXT_INDEX);
            }

            // load self object as IRubyObject, for recv param
            mv.aload(RECEIVER_INDEX);
        } else {
            // load receiver as original type for virtual invocation
            mv.aload(RECEIVER_INDEX);
            mv.checkcast(typePath);

            if (desc.hasContext) {
                mv.aload(THREADCONTEXT_INDEX);
            }
        }
    }

    private Class tryClass(String name, String path, Class targetClass, Class expectedSuperclass) {
        final Class c;
        try {
            if (!classLoader.hasClass(name)) {
                if (DEBUG) System.err.println("could not find class file for " + name);
                seenUndefinedClasses = true;
                return null;
            }
            c = classLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            if (DEBUG) LOG.debug(e);
            seenUndefinedClasses = true;
            return null;
        } catch (Exception e) {
            if (DEBUG) LOG.warn(e);
            seenUndefinedClasses = true;
            return null;
        }

        // For JRUBY-5038, ensure loaded class has superclass from same classloader as current JRuby
        if (c.getSuperclass() == expectedSuperclass) {
            if (seenUndefinedClasses && !haveWarnedUser) {
                haveWarnedUser = true;
                System.err.println("WARNING: while creating new bindings for " + targetClass + ",\n" +
                        "found an existing binding; you may want to run a clean build.");
            }

            return c;
        } else {
            seenUndefinedClasses = true;
            return null;
        }
    }

    protected Class endClass(ClassWriter cw, String name) {
        cw.visitEnd();

        final byte[] code = cw.toByteArray();
        if (DEBUG) CheckClassAdapter.verify(new ClassReader(code), classLoader, false, new PrintWriter(System.err));
        return classLoader.defineClass(name, code);
    }

    private SkinnyMethodAdapter beginMethod(ClassWriter cw, String methodName, int specificArity, boolean block) {
        switch (specificArity) {
        default:
        case -1:
            if (block) {
                return new SkinnyMethodAdapter(cw, ACC_PUBLIC, methodName, COMPILED_CALL_SIG_BLOCK, null, null);
            } else {
                return new SkinnyMethodAdapter(cw, ACC_PUBLIC, methodName, COMPILED_CALL_SIG, null, null);
            }
        case 0:
            if (block) {
                return new SkinnyMethodAdapter(cw, ACC_PUBLIC, methodName, COMPILED_CALL_SIG_ZERO_BLOCK, null, null);
            } else {
                return new SkinnyMethodAdapter(cw, ACC_PUBLIC, methodName, COMPILED_CALL_SIG_ZERO, null, null);
            }
        case 1:
            if (block) {
                return new SkinnyMethodAdapter(cw, ACC_PUBLIC, methodName, COMPILED_CALL_SIG_ONE_BLOCK, null, null);
            } else {
                return new SkinnyMethodAdapter(cw, ACC_PUBLIC, methodName, COMPILED_CALL_SIG_ONE, null, null);
            }
        case 2:
            if (block) {
                return new SkinnyMethodAdapter(cw, ACC_PUBLIC, methodName, COMPILED_CALL_SIG_TWO_BLOCK, null, null);
            } else {
                return new SkinnyMethodAdapter(cw, ACC_PUBLIC, methodName, COMPILED_CALL_SIG_TWO, null, null);
            }
        case 3:
            if (block) {
                return new SkinnyMethodAdapter(cw, ACC_PUBLIC, methodName, COMPILED_CALL_SIG_THREE_BLOCK, null, null);
            } else {
                return new SkinnyMethodAdapter(cw, ACC_PUBLIC, methodName, COMPILED_CALL_SIG_THREE, null, null);
            }
        }
    }

    private void addAnnotatedMethodInvoker(ClassWriter cw, String superClass, List<JavaMethodDescriptor> descs, boolean acceptsKeywords) {
        for (JavaMethodDescriptor desc: descs) {
            int specificArity = desc.calculateSpecificCallArity();

            SkinnyMethodAdapter mv = beginMethod(cw, "call", specificArity, desc.hasBlock);
            mv.visitCode();
            createAnnotatedMethodInvocation(desc, mv, superClass, specificArity, desc.hasBlock, acceptsKeywords);
            mv.end();
        }
    }

    private void createAnnotatedMethodInvocation(JavaMethodDescriptor desc, SkinnyMethodAdapter method,
                                                 String superClass, int specificArity, boolean block, boolean acceptsKeywords) {
        String typePath = desc.declaringClassPath;
        String javaMethodName = desc.name;

        // If a native method does not accept keywords then we wipe callInfo.
        if (!acceptsKeywords) {
            method.aload(1);
            method.ldc(0);
            method.putfield("org/jruby/runtime/ThreadContext", "callInfo", "I");
        }

        if (desc.anno.checkArity()) {
            checkArity(desc.anno, method, specificArity);
        }

        CallConfiguration callConfig = CallConfiguration.getCallConfigByAnno(desc.anno);
        if (!callConfig.isNoop()) {
            invokeCallConfigPre(method, superClass, specificArity, block, callConfig);
        }

        final boolean FULL_TRACE_ENABLED = RubyInstanceConfig.FULL_TRACE_ENABLED;

        int traceBoolIndex = -1;
        if (FULL_TRACE_ENABLED) {
            // load and store trace enabled flag
            switch (specificArity) {
            case -1:
                traceBoolIndex = ARGS_INDEX + (block ? 1 : 0) + 1;
                break;
            case 0:
                traceBoolIndex = ARGS_INDEX + (block ? 1 : 0);
                break;
            default:
                traceBoolIndex = ARGS_INDEX + specificArity + (block ? 1 : 0) + 1;
            }

            method.aload(1);
            method.invokevirtual(p(ThreadContext.class), "getRuntime", sig(Ruby.class));
            method.invokevirtual(p(Ruby.class), "hasEventHooks", sig(boolean.class));
            method.istore(traceBoolIndex);

            // call trace
            invokeCCallTrace(method, traceBoolIndex);
        }

        Label tryBegin = new Label();
        Label tryEnd = new Label();
        Label doFinally = new Label();

        if (!callConfig.isNoop()) {
            method.trycatch(tryBegin, tryEnd, doFinally, null);
        }

        method.label(tryBegin);
        {
            loadReceiver(typePath, desc, method);

            loadArguments(method, desc, specificArity);

            loadBlock(method, specificArity, block);

            if (Modifier.isStatic(desc.modifiers)) {
                if (Modifier.isInterface(desc.declaringClass.getModifiers())) {
                    method.invokestaticinterface(typePath, javaMethodName, sig(desc.returnClass, desc.parameters));
                } else {
                    method.invokestatic(typePath, javaMethodName, sig(desc.returnClass, desc.parameters));
                }
            } else {
                method.invokevirtual(typePath, javaMethodName, sig(desc.returnClass, desc.parameters));
            }

            if (desc.returnClass == void.class) {
                // void return type, so we need to load a nil for returning below
                method.aload(THREADCONTEXT_INDEX);
                method.getfield(p(ThreadContext.class), "nil", ci(IRubyObject.class));
            }
        }
        method.label(tryEnd);

        // normal finally and exit
        {
            if (FULL_TRACE_ENABLED) {
                invokeCReturnTrace(method, traceBoolIndex);
            }

            if (!callConfig.isNoop()) {
                invokeCallConfigPost(method, superClass, callConfig);
            }

            // return
            method.visitInsn(ARETURN);
        }

        // these are only needed if we have a non-noop call config
        if (!callConfig.isNoop()) {
            // finally handling for abnormal exit
            {
                method.label(doFinally);

                if (FULL_TRACE_ENABLED) {
                    invokeCReturnTrace(method, traceBoolIndex);
                }

                //call post method stuff (exception raised)
                if (!callConfig.isNoop()) {
                    invokeCallConfigPost(method, superClass, callConfig);
                }

                // rethrow exception
                method.athrow(); // rethrow it
            }
        }
    }

    private static void invokeCCallTrace(SkinnyMethodAdapter method, int traceBoolIndex) {
        method.aloadMany(0, 1); // method, threadContext
        method.iload(traceBoolIndex); // traceEnable
        method.aload(4); // invokedName
        method.invokevirtual(p(JavaMethod.class), "callTrace", sig(void.class, ThreadContext.class, boolean.class, String.class));
    }

    private static void invokeCReturnTrace(SkinnyMethodAdapter method, int traceBoolIndex) {
        method.aloadMany(0, 1); // method, threadContext
        method.iload(traceBoolIndex); // traceEnable
        method.aload(4); // invokedName
        method.invokevirtual(p(JavaMethod.class), "returnTrace", sig(void.class, ThreadContext.class, boolean.class, String.class));
    }

    @Deprecated
    private static final Class[] RubyModule_and_Visibility = new Class[]{ RubyModule.class, Visibility.class };
}
