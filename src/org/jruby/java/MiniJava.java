/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.java;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.compiler.util.HandleFactory;
import org.jruby.compiler.util.HandleFactory.Handle;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import static org.jruby.util.CodegenUtils.*;
import org.jruby.util.IdUtil;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import static org.objectweb.asm.Opcodes.*;

/**
 *
 * @author headius
 */
public class MiniJava implements Library {
    private static final boolean DEBUG = false;
    
    public void load(Ruby runtime, boolean wrap) {
        runtime.getErr().print("Warning: minijava is experimental and subject to change\n");
        
        runtime.getKernel().defineAnnotatedMethods(MiniJava.class);

        // load up object and add a few useful methods
        RubyModule javaObject = getMirrorForClass(runtime, Object.class);

        javaObject.addMethod("to_s", new JavaMethod.JavaMethodZero(javaObject, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                return context.getRuntime().newString(((JavaObjectWrapper) self).object.toString());
            }
        });

        javaObject.addMethod("hash", new JavaMethod.JavaMethodZero(javaObject, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                return self.getRuntime().newFixnum(((JavaObjectWrapper) self).object.hashCode());
            }
        });

        javaObject.addMethod("==", new JavaMethod.JavaMethodOne(javaObject, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg) {
                if (arg instanceof JavaObjectWrapper) {
                    return context.getRuntime().newBoolean(((JavaObjectWrapper) self).object.equals(((JavaObjectWrapper) arg).object));
                } else {
                    return context.getRuntime().getFalse();
                }
            }
        });

        // open up the 'to_java' and 'as' coercion methods on Ruby Objects, via Kernel
        RubyModule rubyKernel = runtime.getKernel();
        rubyKernel.addModuleFunction("to_java", new JavaMethod.JavaMethodZeroOrOne(rubyKernel, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                return ((RubyObject) self).to_java();
            }

            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg) {
                return ((RubyObject) self).as(getJavaClassFromObject(arg));
            }
        });
    }
    
    @JRubyMethod(name = "new_class", rest = true, module = true)
    public static IRubyObject new_class(ThreadContext context, IRubyObject self, IRubyObject[] interfaces) {
        Class[] javaInterfaces = new Class[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            javaInterfaces[i] = getJavaClassFromObject(interfaces[i]);
        }
        
        return createImplClass(javaInterfaces, context.getRuntime(), "I" + System.currentTimeMillis());
    }

    @JRubyMethod(name = "import", module = true)
    public static IRubyObject rb_import(ThreadContext context, IRubyObject self, IRubyObject name) {
        String className = name.toString();
        try {
            Class cls = findClass(context.getRuntime().getJRubyClassLoader(), className);

            RubyModule namespace;
            if (self instanceof RubyModule) {
                namespace = (RubyModule) self;
            } else {
                namespace = self.getMetaClass().getRealClass();
            }

            namespace.defineConstant(cls.getSimpleName(), getMirrorForClass(context.getRuntime(), cls));

            return context.getRuntime().getNil();
        } catch (Exception e) {
            if (context.getRuntime().getDebug().isTrue()) {
                e.printStackTrace();
            }
            throw context.getRuntime().newTypeError("Could not find class " + className + ", exception: " + e);
        }
    }

    @JRubyMethod(name = "import", module = true)
    public static IRubyObject rb_import(ThreadContext context, IRubyObject self, IRubyObject name, IRubyObject as) {
        String className = name.toString();
        try {
            Class cls = findClass(context.getRuntime().getJRubyClassLoader(), className);

            RubyModule namespace;
            if (self instanceof RubyModule) {
                namespace = (RubyModule) self;
            } else {
                namespace = self.getMetaClass().getRealClass();
            }

            namespace.defineConstant(as.toString(), getMirrorForClass(context.getRuntime(), cls));

            return context.getRuntime().getNil();
        } catch (Exception e) {
            if (context.getRuntime().getDebug().isTrue()) {
                e.printStackTrace();
            }
            throw context.getRuntime().newTypeError("Could not find class " + className + ", exception: " + e);
        }
    }

    public static RubyClass createImplClass(Class[] superTypes, Ruby ruby, String name) {
        String[] superTypeNames = new String[superTypes.length];
        Map<String, List<Method>> simpleToAll = new HashMap<String, List<Method>>();
        for (int i = 0; i < superTypes.length; i++) {
            superTypeNames[i] = p(superTypes[i]);
            
            for (Method method : superTypes[i].getMethods()) {
                List<Method> methods = simpleToAll.get(method.getName());
                if (methods == null) simpleToAll.put(method.getName(), methods = new ArrayList<Method>());
                methods.add(method);
            }
        }
        
        Class newClass = defineImplClass(ruby, name, superTypeNames, simpleToAll);
        RubyClass rubyCls = populateImplClass(ruby, newClass, simpleToAll);
        
        return rubyCls;
    }

    public static Class createOldStyleImplClass(Class[] superTypes, RubyClass rubyClass, Ruby ruby, String name) {
        String[] superTypeNames = new String[superTypes.length];
        Map<String, List<Method>> simpleToAll = new HashMap<String, List<Method>>();
        for (int i = 0; i < superTypes.length; i++) {
            superTypeNames[i] = p(superTypes[i]);
            
            for (Method method : superTypes[i].getMethods()) {
                List<Method> methods = simpleToAll.get(method.getName());
                if (methods == null) simpleToAll.put(method.getName(), methods = new ArrayList<Method>());
                methods.add(method);
            }
        }
        
        Class newClass = defineOldStyleImplClass(ruby, name, superTypeNames, simpleToAll);
        populateOldStyleImplClass(ruby, rubyClass, newClass, simpleToAll);
        
        return newClass;
    }
    
    public static Class defineImplClass(Ruby ruby, String name, String[] superTypeNames, Map<String, List<Method>> simpleToAll) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        
        // construct the class, implementing all supertypes
        cw.visit(V1_5, ACC_PUBLIC | ACC_SUPER, name, null, p(Object.class), superTypeNames);
        
        // fields needed for dispatch and such
        cw.visitField(ACC_STATIC | ACC_PRIVATE, "ruby", ci(Ruby.class), null, null).visitEnd();
        cw.visitField(ACC_STATIC | ACC_PRIVATE, "rubyClass", ci(RubyClass.class), null, null).visitEnd();
        cw.visitField(ACC_PRIVATE | ACC_FINAL, "self", ci(IRubyObject.class), null, null).visitEnd();
        
        // create constructor
        SkinnyMethodAdapter initMethod = new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, "<init>", sig(void.class), null, null));
        initMethod.aload(0);
        initMethod.invokespecial(p(Object.class), "<init>", sig(void.class));
        
        // wrap self and store the wrapper
        initMethod.aload(0);
        initMethod.getstatic(name, "ruby", ci(Ruby.class));
        initMethod.aload(0);
        initMethod.invokestatic(p(MiniJava.class), "javaToRuby", sig(IRubyObject.class, Ruby.class, Object.class));
        initMethod.putfield(name, "self", ci(IRubyObject.class));
        
        // end constructor
        initMethod.voidreturn();
        initMethod.end();
        
        // start setup method
        SkinnyMethodAdapter setupMethod = new SkinnyMethodAdapter(cw.visitMethod(ACC_STATIC | ACC_PUBLIC | ACC_SYNTHETIC, "__setup__", sig(void.class, RubyClass.class), null, null));
        setupMethod.start();
        
        // set RubyClass
        setupMethod.aload(0);
        setupMethod.dup();
        setupMethod.putstatic(name, "rubyClass", ci(RubyClass.class));
        
        // set Ruby
        setupMethod.invokevirtual(p(RubyClass.class), "getClassRuntime", sig(Ruby.class));
        setupMethod.putstatic(name, "ruby", ci(Ruby.class));
        
        // for each simple method name, implement the complex methods, calling the simple version
        for (Map.Entry<String, List<Method>> entry : simpleToAll.entrySet()) {
            String simpleName = entry.getKey();
            
            // all methods dispatch to the simple version by default, which is method_missing normally
            cw.visitField(ACC_STATIC | ACC_PUBLIC | ACC_VOLATILE, simpleName, ci(DynamicMethod.class), null, null).visitEnd();
            
            for (Method method : entry.getValue()) {
                Class[] paramTypes = method.getParameterTypes();
                Class returnType = method.getReturnType();
                
                SkinnyMethodAdapter mv = new SkinnyMethodAdapter(
                        cw.visitMethod(ACC_PUBLIC, simpleName, sig(returnType, paramTypes), null, null));
                mv.start();
                String fieldName = mangleMethodFieldName(simpleName, paramTypes);
                
                // try specific name first, falling back on simple name
                Label dispatch = new Label();
                cw.visitField(ACC_STATIC | ACC_PUBLIC | ACC_VOLATILE, fieldName, ci(DynamicMethod.class), null, null).visitEnd();
                mv.getstatic(name, fieldName, ci(DynamicMethod.class));
                mv.dup();
                mv.ifnonnull(dispatch);
                mv.pop();
                mv.getstatic(name, simpleName, ci(DynamicMethod.class));
                mv.dup();
                mv.ifnonnull(dispatch);
                mv.pop();
                mv.getstatic(name, "rubyClass", ci(RubyClass.class));
                mv.ldc("method_missing");
                mv.invokevirtual(p(RubyClass.class), "searchMethod", sig(DynamicMethod.class, String.class));
                mv.label(dispatch);
                
                // get current context
                mv.getstatic(name, "ruby", ci(Ruby.class));
                mv.invokevirtual(p(Ruby.class), "getCurrentContext", sig(ThreadContext.class));
                
                // load self, class, and name
                mv.aload(0);
                mv.getfield(name, "self", ci(IRubyObject.class));
                mv.getstatic(name, "rubyClass", ci(RubyClass.class));
                mv.ldc(simpleName);
                
                // load arguments into IRubyObject[] for dispatch
                if (method.getParameterTypes().length != 0) {
                    mv.pushInt(method.getParameterTypes().length);
                    mv.anewarray(p(IRubyObject.class));
                    
                    for (int i = 0; i < paramTypes.length; i++) {
                        mv.dup();
                        mv.pushInt(i);
                        // convert to IRubyObject
                        mv.getstatic(name, "ruby", ci(Ruby.class));
                        mv.aload(i + 1);
                        mv.invokestatic(p(MiniJava.class), "javaToRuby", sig(IRubyObject.class, Ruby.class, Object.class));
                        mv.aastore();
                    }
                } else {
                    mv.getstatic(p(IRubyObject.class), "NULL_ARRAY", ci(IRubyObject[].class));
                }
                
                // load null block
                mv.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));
                
                // invoke method
                mv.invokevirtual(p(DynamicMethod.class), "call", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject[].class, Block.class));
                
                // if we expect a return value, unwrap it
                if (method.getReturnType() != void.class) {
                    mv.invokestatic(p(MiniJava.class), "rubyToJava", sig(Object.class, IRubyObject.class));
                    mv.checkcast(p(returnType));

                    mv.areturn();
                } else {
                    mv.voidreturn();
                }
                mv.end();
            }
        }
        
        // end setup method
        setupMethod.voidreturn();
        setupMethod.end();
        
        // end class
        cw.visitEnd();
        
        // create the class
        byte[] bytes = cw.toByteArray();
        Class newClass = ruby.getJRubyClassLoader().defineClass(name, cw.toByteArray());
        
        if (DEBUG) {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(name + ".class");
                fos.write(bytes);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                try {fos.close();} catch (Exception e) {}
            }
        }
        
        return newClass;
    }
    
    /**
     * This variation on defineImplClass uses all the classic type coercion logic
     * for passing args and returning results.
     * 
     * @param ruby
     * @param name
     * @param superTypeNames
     * @param simpleToAll
     * @return
     */
    public static Class defineOldStyleImplClass(Ruby ruby, String name, String[] superTypeNames, Map<String, List<Method>> simpleToAll) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        
        // construct the class, implementing all supertypes
        cw.visit(V1_5, ACC_PUBLIC | ACC_SUPER, name, null, p(Object.class), superTypeNames);
        
        // fields needed for dispatch and such
        cw.visitField(ACC_STATIC | ACC_PRIVATE, "ruby", ci(Ruby.class), null, null).visitEnd();
        cw.visitField(ACC_STATIC | ACC_PRIVATE, "rubyClass", ci(RubyClass.class), null, null).visitEnd();
        cw.visitField(ACC_PRIVATE | ACC_FINAL, "self", ci(IRubyObject.class), null, null).visitEnd();
        
        // create constructor
        SkinnyMethodAdapter initMethod = new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, "<init>", sig(void.class, IRubyObject.class), null, null));
        initMethod.aload(0);
        initMethod.invokespecial(p(Object.class), "<init>", sig(void.class));
        
        // store the wrapper
        initMethod.aload(0);
        initMethod.aload(1);
        initMethod.putfield(name, "self", ci(IRubyObject.class));
        
        // end constructor
        initMethod.voidreturn();
        initMethod.end();
        
        // start setup method
        SkinnyMethodAdapter setupMethod = new SkinnyMethodAdapter(cw.visitMethod(ACC_STATIC | ACC_PUBLIC | ACC_SYNTHETIC, "__setup__", sig(void.class, RubyClass.class), null, null));
        setupMethod.start();
        
        // set RubyClass
        setupMethod.aload(0);
        setupMethod.dup();
        setupMethod.putstatic(name, "rubyClass", ci(RubyClass.class));
        
        // set Ruby
        setupMethod.invokevirtual(p(RubyClass.class), "getClassRuntime", sig(Ruby.class));
        setupMethod.putstatic(name, "ruby", ci(Ruby.class));
        
        // for each simple method name, implement the complex methods, calling the simple version
        for (Map.Entry<String, List<Method>> entry : simpleToAll.entrySet()) {
            String simpleName = entry.getKey();
            
            Set<String> nameSet = new HashSet<String>();
            nameSet.add(simpleName);

            getRubyNamesForJavaName(simpleName, nameSet, entry.getValue());
                
            // all methods dispatch to the simple version by default, or method_missing if it's not present
            cw.visitField(ACC_STATIC | ACC_PUBLIC | ACC_VOLATILE, simpleName, ci(DynamicMethod.class), null, null).visitEnd();
            
            for (Method method : entry.getValue()) {
                Class[] paramTypes = method.getParameterTypes();
                Class returnType = method.getReturnType();
                
                SkinnyMethodAdapter mv = new SkinnyMethodAdapter(
                        cw.visitMethod(ACC_PUBLIC, simpleName, sig(returnType, paramTypes), null, null));
                mv.start();
                
                Label dispatch = new Label();
                Label end = new Label();

                // Try to look up field for simple name

                // lock self
                mv.getstatic(name, "rubyClass", ci(RubyClass.class));
                mv.monitorenter();
                
                // get field
                mv.getstatic(name, simpleName, ci(DynamicMethod.class));
                mv.dup();
                mv.ifnonnull(dispatch);
                
                // not retrieved yet, retrieve it now
                mv.pop();
                mv.getstatic(name, "rubyClass", ci(RubyClass.class));
                for (String eachName : nameSet) {
                    mv.ldc(eachName);
                }
                mv.invokestatic(p(MiniJava.class), "searchMethod", sig(DynamicMethod.class, params(RubyClass.class, String.class, nameSet.size())));
                mv.dup();
                
                // check if it's UndefinedMethod
                mv.getstatic(p(UndefinedMethod.class), "INSTANCE", ci(UndefinedMethod.class));
                mv.if_acmpne(dispatch);
                
                // undefined, call method_missing
                mv.pop();
                // exit monitor before making call
                mv.getstatic(name, "rubyClass", ci(RubyClass.class));
                mv.monitorexit();
                mv.aload(0);
                mv.getfield(name, "self", ci(IRubyObject.class));
                mv.ldc(simpleName);
                coerceArgumentsToRuby(mv, paramTypes, name);
                mv.invokestatic(p(RuntimeHelpers.class), "invokeMethodMissing", sig(IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class));
                mv.go_to(end);
                
                // re-save the method, release monitor, and dispatch
                mv.label(dispatch);
                mv.dup();
                mv.putstatic(name, simpleName, ci(DynamicMethod.class));
                
                mv.getstatic(name, "rubyClass", ci(RubyClass.class));
                mv.monitorexit();
                
                // get current context
                mv.getstatic(name, "ruby", ci(Ruby.class));
                mv.invokevirtual(p(Ruby.class), "getCurrentContext", sig(ThreadContext.class));
                
                // load self, class, and name
                mv.aload(0);
                mv.getfield(name, "self", ci(IRubyObject.class));
                mv.getstatic(name, "rubyClass", ci(RubyClass.class));
                mv.ldc(simpleName);
                
                // coerce arguments
                coerceArgumentsToRuby(mv, paramTypes, name);
                
                // load null block
                mv.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));
                
                // invoke method
                mv.invokevirtual(p(DynamicMethod.class), "call", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject[].class, Block.class));
                
                mv.label(end);
                coerceResultAndReturn(method, mv, returnType);
                
                mv.end();
            }
        }
        
        // end setup method
        setupMethod.voidreturn();
        setupMethod.end();
        
        // end class
        cw.visitEnd();
        
        // create the class
        byte[] bytes = cw.toByteArray();
        Class newClass = ruby.getJRubyClassLoader().defineClass(name, cw.toByteArray());
        
        if (DEBUG) {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(name + ".class");
                fos.write(bytes);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                try {fos.close();} catch (Exception e) {}
            }
        }
        
        return newClass;
    }

    private static void coerceArgumentsToRuby(SkinnyMethodAdapter mv, Class[] paramTypes, String name) {
        // load arguments into IRubyObject[] for dispatch
        if (paramTypes.length != 0) {
            mv.pushInt(paramTypes.length);
            mv.anewarray(p(IRubyObject.class));

            // TODO: make this do specific-arity calling
            for (int i = 0; i < paramTypes.length; i++) {
                mv.dup();
                mv.pushInt(i);
                // convert to IRubyObject
                mv.getstatic(name, "ruby", ci(Ruby.class));
                if (paramTypes[i].isPrimitive()) {
                    if (paramTypes[i] == byte.class || paramTypes[i] == short.class || paramTypes[i] == char.class || paramTypes[i] == int.class) {
                        mv.iload(i + 1);
                        mv.invokestatic(p(JavaUtil.class), "convertJavaToRuby", sig(IRubyObject.class, Ruby.class, int.class));
                    } else if (paramTypes[i] == long.class) {
                        mv.lload(i + 1);
                        mv.invokestatic(p(JavaUtil.class), "convertJavaToRuby", sig(IRubyObject.class, Ruby.class, long.class));
                    } else if (paramTypes[i] == float.class) {
                        mv.fload(i + 1);
                        mv.invokestatic(p(JavaUtil.class), "convertJavaToRuby", sig(IRubyObject.class, Ruby.class, float.class));
                    } else if (paramTypes[i] == double.class) {
                        mv.dload(i + 1);
                        mv.invokestatic(p(JavaUtil.class), "convertJavaToRuby", sig(IRubyObject.class, Ruby.class, double.class));
                    } else if (paramTypes[i] == boolean.class) {
                        mv.iload(i + 1);
                        mv.invokestatic(p(JavaUtil.class), "convertJavaToRuby", sig(IRubyObject.class, Ruby.class, boolean.class));
                    }
                } else {
                    mv.aload(i + 1);
                    mv.invokestatic(p(JavaUtil.class), "convertJavaToUsableRubyObject", sig(IRubyObject.class, Ruby.class, Object.class));
                }
                mv.aastore();
            }
        } else {
            mv.getstatic(p(IRubyObject.class), "NULL_ARRAY", ci(IRubyObject[].class));
        }
    }

    private static void coerceResultAndReturn(Method method, SkinnyMethodAdapter mv, Class returnType) {
        // if we expect a return value, unwrap it
        if (method.getReturnType() != void.class) {
            if (method.getReturnType().isPrimitive()) {
                if (method.getReturnType() == byte.class) {
                    mv.invokestatic(p(JavaUtil.class), "convertRubyToJavaByte", sig(byte.class, IRubyObject.class));
                    mv.ireturn();
                } else if (method.getReturnType() == short.class) {
                    mv.invokestatic(p(JavaUtil.class), "convertRubyToJavaShort", sig(short.class, IRubyObject.class));
                    mv.ireturn();
                } else if (method.getReturnType() == char.class) {
                    mv.invokestatic(p(JavaUtil.class), "convertRubyToJavaChar", sig(char.class, IRubyObject.class));
                    mv.ireturn();
                } else if (method.getReturnType() == int.class) {
                    mv.invokestatic(p(JavaUtil.class), "convertRubyToJavaInt", sig(int.class, IRubyObject.class));
                    mv.ireturn();
                } else if (method.getReturnType() == long.class) {
                    mv.invokestatic(p(JavaUtil.class), "convertRubyToJavaLong", sig(long.class, IRubyObject.class));
                    mv.lreturn();
                } else if (method.getReturnType() == float.class) {
                    mv.invokestatic(p(JavaUtil.class), "convertRubyToJavaFloat", sig(float.class, IRubyObject.class));
                    mv.freturn();
                } else if (method.getReturnType() == double.class) {
                    mv.invokestatic(p(JavaUtil.class), "convertRubyToJavaDouble", sig(double.class, IRubyObject.class));
                    mv.dreturn();
                } else if (method.getReturnType() == boolean.class) {
                    mv.invokestatic(p(JavaUtil.class), "convertRubyToJavaBoolean", sig(boolean.class, IRubyObject.class));
                    mv.ireturn();
                }
            } else {
                mv.ldc(Type.getType(method.getReturnType()));
                mv.invokestatic(p(JavaUtil.class), "convertRubyToJava", sig(Object.class, IRubyObject.class, Class.class));
                mv.checkcast(p(returnType));

                mv.areturn();
            }
        } else {
            mv.voidreturn();
        }
    }

    public static RubyClass populateImplClass(Ruby ruby, Class newClass, Map<String, List<Method>> simpleToAll) {
        RubyClass rubyCls = (RubyClass)getMirrorForClass(ruby, newClass);
        
        // setup the class
        try {
            newClass.getMethod("__setup__", new Class[]{RubyClass.class}).invoke(null, new Object[]{rubyCls});
        } catch (IllegalAccessException ex) {
            throw error(ruby, ex, "Could not setup class: " + newClass);
        } catch (IllegalArgumentException ex) {
            throw error(ruby, ex, "Could not setup class: " + newClass);
        } catch (InvocationTargetException ex) {
            throw error(ruby, ex, "Could not setup class: " + newClass);
        } catch (NoSuchMethodException ex) {
            throw error(ruby, ex, "Could not setup class: " + newClass);
        }

        // now, create a method_added that can replace the DynamicMethod fields as they're redefined
        final Map<String, Field> allFields = new HashMap<String, Field>();
        try {
            for (Map.Entry<String, List<Method>> entry : simpleToAll.entrySet()) {
                String simpleName = entry.getKey();

                Field simpleField = newClass.getField(simpleName);
                allFields.put(simpleName, simpleField);

                for (Method method : entry.getValue()) {
                    String complexName = simpleName + prettyParams(method.getParameterTypes());
                    String fieldName = mangleMethodFieldName(simpleName, method.getParameterTypes());
                    allFields.put(complexName, newClass.getField(fieldName));
                }
            }
        } catch (IllegalArgumentException ex) {
            throw error(ruby, ex, "Could not prepare method fields: " + newClass);
        } catch (NoSuchFieldException ex) {
            throw error(ruby, ex, "Could not prepare method fields: " + newClass);
        }

        DynamicMethod method_added = new JavaMethod(rubyCls.getSingletonClass(), Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                RubyClass selfClass = (RubyClass)self;
                Ruby ruby = selfClass.getClassRuntime();
                String methodName = args[0].asJavaString();
                Field field = allFields.get(methodName);

                if (field == null) {
                    // do nothing, it's a non-impl method
                } else {
                    try {
                        field.set(null, selfClass.searchMethod(methodName));
                    } catch (IllegalAccessException iae) {
                        throw error(ruby, iae, "Could not set new method into field: " + selfClass + "." + methodName);
                    } catch (IllegalArgumentException iae) {
                        throw error(ruby, iae, "Could not set new method into field: " + selfClass + "." + methodName);
                    }
                }

                return context.getRuntime().getNil();
            }
        };
        rubyCls.getSingletonClass().addMethod("method_added", method_added);
        
        return rubyCls;
    }

    public static void populateOldStyleImplClass(Ruby ruby, RubyClass rubyCls, final Class newClass, Map<String, List<Method>> simpleToAll) {
        // setup the class
        try {
            newClass.getMethod("__setup__", new Class[]{RubyClass.class}).invoke(null, rubyCls);
        } catch (IllegalAccessException ex) {
            throw error(ruby, ex, "Could not setup class: " + newClass);
        } catch (IllegalArgumentException ex) {
            throw error(ruby, ex, "Could not setup class: " + newClass);
        } catch (InvocationTargetException ex) {
            throw error(ruby, ex, "Could not setup class: " + newClass);
        } catch (NoSuchMethodException ex) {
            throw error(ruby, ex, "Could not setup class: " + newClass);
        }

        // now, create a method_added that can replace the DynamicMethod fields as they're redefined
        final Map<String, Field> allFields = new HashMap<String, Field>();
        try {
            for (Map.Entry<String, List<Method>> entry : simpleToAll.entrySet()) {
                String simpleName = entry.getKey();
                String rubyName = JavaUtil.getRubyCasedName(simpleName);

                Field simpleField = newClass.getField(simpleName);
                allFields.put(simpleName, simpleField);
                allFields.put(rubyName, simpleField);
                
                Set<String> nameSet = new HashSet<String>();
                getRubyNamesForJavaName(simpleName, nameSet, entry.getValue());
                for (String name : nameSet) {
                    allFields.put(name, simpleField);
                }
            }
        } catch (IllegalArgumentException ex) {
            throw error(ruby, ex, "Could not prepare method fields: " + newClass);
        } catch (NoSuchFieldException ex) {
            throw error(ruby, ex, "Could not prepare method fields: " + newClass);
        }

        DynamicMethod method_added = new JavaMethod(rubyCls.getSingletonClass(), Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                RubyClass selfClass = (RubyClass)self;
                Ruby ruby = selfClass.getClassRuntime();
                String methodName = args[0].asJavaString();
                Field field = allFields.get(methodName);

                if (field == null) {
                    // do nothing, it's a non-impl method
                } else {
                    try {
                        synchronized (self) {
                            DynamicMethod method = selfClass.searchMethod(methodName);
                            if (method != UndefinedMethod.INSTANCE) {
                                field.set(null, method);
                            }
                        }
                    } catch (IllegalAccessException iae) {
                        throw error(ruby, iae, "Could not set new method into field: " + selfClass + "." + methodName);
                    } catch (IllegalArgumentException iae) {
                        throw error(ruby, iae, "Could not set new method into field: " + selfClass + "." + methodName);
                    }
                }

                return context.getRuntime().getNil();
            }
        };
        rubyCls.getSingletonClass().addMethod("method_added", method_added);
    }
    
    protected static String mangleMethodFieldName(String baseName, Class[] paramTypes) {
        String fieldName = baseName + prettyParams(paramTypes);
        fieldName = fieldName.replace('.', '\\');
        
        return fieldName;
    }
    
    /**
     * Given a simple Java method name and the Java Method objects that represent
     * all its overloads, add to the given nameSet all possible Ruby names that would
     * be valid.
     * 
     * @param simpleName
     * @param nameSet
     * @param methods
     */
    protected static void getRubyNamesForJavaName(String simpleName, Set<String> nameSet, List<Method> methods) {
            String javaPropertyName = JavaUtil.getJavaPropertyName(simpleName);
            String rubyName = JavaUtil.getRubyCasedName(simpleName);
            nameSet.add(rubyName);
            String rubyPropertyName = null;
            for (Method method: methods) {
                Class<?>[] argTypes = method.getParameterTypes();
                Class<?> resultType = method.getReturnType();
                int argCount = argTypes.length;

                // Add property name aliases
                if (javaPropertyName != null) {
                    if (rubyName.startsWith("get_")) {
                        rubyPropertyName = rubyName.substring(4);
                        if (argCount == 0 ||                                // getFoo      => foo
                            argCount == 1 && argTypes[0] == int.class) {    // getFoo(int) => foo(int)

                            nameSet.add(javaPropertyName);
                            nameSet.add(rubyPropertyName);
                            if (resultType == boolean.class) {              // getFooBar() => fooBar?, foo_bar?(*)
                                nameSet.add(javaPropertyName + '?');
                                nameSet.add(rubyPropertyName + '?');
                            }
                        }
                    } else if (rubyName.startsWith("set_")) {
                        rubyPropertyName = rubyName.substring(4);
                        if (argCount == 1 && resultType == void.class) {    // setFoo(Foo) => foo=(Foo)
                            nameSet.add(javaPropertyName + '=');
                            nameSet.add(rubyPropertyName + '=');
                        }
                    } else if (rubyName.startsWith("is_")) {
                        rubyPropertyName = rubyName.substring(3);
                        if (resultType == boolean.class) {                  // isFoo() => foo, isFoo(*) => foo(*)
                            nameSet.add(javaPropertyName);
                            nameSet.add(javaPropertyName + '?');
                            nameSet.add(rubyPropertyName);
                            nameSet.add(rubyPropertyName + '?');
                        }
                    }
                }

                // Additionally add ?-postfixed aliases to any boolean methods and properties.
                if (resultType == boolean.class) {
                    // is_something?, contains_thing?
                    nameSet.add(simpleName + '?');
                    nameSet.add(rubyName + '?');
                }
            }
    }
    
    protected static Class findClass(ClassLoader classLoader, String className) throws ClassNotFoundException {
        if (className.indexOf('.') == -1 && Character.isLowerCase(className.charAt(0))) {
            // probably a primitive
            switch (className.charAt(0)) {
            case 'b':
                return byte.class;
            case 's':
                return short.class;
            case 'c':
                return char.class;
            case 'i':
                return int.class;
            case 'l':
                return long.class;
            case 'f':
                return float.class;
            case 'd':
                return double.class;
            default:
                return classLoader.loadClass(className);
            }
        } else {
            return classLoader.loadClass(className);
        }
    }
    static Map<Class, RubyModule> classMap = new HashMap<Class, RubyModule>();

    public static RubyModule getMirrorForClass(Ruby ruby, Class cls) {
        if (cls == null) {
            return ruby.getObject();
        }

        RubyModule rubyCls = classMap.get(cls);

        if (rubyCls == null) {
            rubyCls = createMirrorForClass(ruby, cls);

            classMap.put(cls, rubyCls);
            populateMirrorForClass(rubyCls, cls);
            rubyCls = classMap.get(cls);
        }

        return rubyCls;
    }

    protected static RubyModule createMirrorForClass(Ruby ruby, Class cls) {
        if (cls.isInterface()) {
            // interfaces are handled as modules
            RubyModule rubyMod = RubyModule.newModule(ruby);
            return rubyMod;
        } else {
            // construct the mirror class and parent classes
            RubyClass rubyCls = RubyClass.newClass(ruby, (RubyClass) getMirrorForClass(ruby, cls.getSuperclass()));
            return rubyCls;
        }
    }

    protected static void populateMirrorForClass(RubyModule rubyMod, final Class cls) {
        Ruby ruby = rubyMod.getRuntime();

        // set the full name
        rubyMod.setBaseName(cls.getCanonicalName());

        // include all interfaces
        Class[] interfaces = cls.getInterfaces();
        for (Class ifc : interfaces) {
            rubyMod.includeModule(getMirrorForClass(ruby, ifc));
        }

        // if it's an inner class and it's not public, we can't access it;
        // skip population of declared elements
        if (cls.getEnclosingClass() != null && !Modifier.isPublic(cls.getModifiers())) {
            return;
        }

        RubyModule rubySing = rubyMod.getSingletonClass();

        // if it's an array, only add methods for aref, aset, and length
        if (cls.isArray()) {
            populateMirrorForArrayClass(rubyMod, cls);
        } else {
            populateDeclaredMethods(rubyMod, cls, true);

            populateConstructors(rubySing, cls);

            populateArrayConstructors(rubySing);

            populateFields(rubyMod, cls);
        }

        populateSpecialMethods(rubyMod, rubySing, cls);
    }
    private static void populateArrayConstructors(RubyModule rubySing) {
        // add array construction methods
        rubySing.addMethod("[]", new JavaMethod.JavaMethodOneOrTwoOrThree(rubySing, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg) {
                Class javaClass = getJavaClassFromObject(self);
                int size = RubyFixnum.fix2int(arg.convertToInteger());
                return javaToRuby(context.getRuntime(), Array.newInstance(javaClass, size));
            }

            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
                Class javaClass = getJavaClassFromObject(self);
                int x = RubyFixnum.fix2int(arg0.convertToInteger());
                int y = RubyFixnum.fix2int(arg1.convertToInteger());
                return javaToRuby(context.getRuntime(), Array.newInstance(javaClass, new int[]{x, y}));
            }

            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
                Class javaClass = getJavaClassFromObject(self);
                int x = RubyFixnum.fix2int(arg0.convertToInteger());
                int y = RubyFixnum.fix2int(arg1.convertToInteger());
                int z = RubyFixnum.fix2int(arg2.convertToInteger());
                return javaToRuby(context.getRuntime(), Array.newInstance(javaClass, new int[]{x, y, z}));
            }
        });
    }

    private static void populateConstructors(RubyModule rubySing, final Class cls) {
        final Ruby ruby = rubySing.getRuntime();
        
        // add all public constructors (note: getConstructors only returns public ones)
        Constructor[] constructors = cls.getConstructors();
        for (final Constructor constructor : constructors) {
            DynamicMethod dynMethod;
            if (constructor.getParameterTypes().length == 0) {
                dynMethod = new JavaMethod.JavaMethodZero(rubySing, Visibility.PUBLIC) {
                    @Override
                    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                        try {
                            return javaToRuby(context.getRuntime(), constructor.newInstance());
                        } catch (InstantiationException ex) {
                            if (ruby.getDebug().isTrue()) ex.printStackTrace();
                            throw ruby.newTypeError("Could not instantiate " + cls.getCanonicalName() + " using " + prettyParams(constructor.getParameterTypes()));
                        } catch (IllegalAccessException ex) {
                            if (ruby.getDebug().isTrue()) ex.printStackTrace();
                            throw ruby.newTypeError("Could not instantiate " + cls.getCanonicalName() + " using " + prettyParams(constructor.getParameterTypes()));
                        } catch (IllegalArgumentException ex) {
                            if (ruby.getDebug().isTrue()) ex.printStackTrace();
                            throw ruby.newTypeError("Could not instantiate " + cls.getCanonicalName() + " using " + prettyParams(constructor.getParameterTypes()));
                        } catch (InvocationTargetException ex) {
                            if (ruby.getDebug().isTrue()) ex.printStackTrace();
                            throw ruby.newTypeError("Could not instantiate " + cls.getCanonicalName() + " using " + prettyParams(constructor.getParameterTypes()));
                        }
                    }
                };
            } else {
                dynMethod = new JavaMethod.JavaMethodNoBlock(rubySing, Visibility.PUBLIC) {
                    @Override
                    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] rubyArgs) {
                        Object[] args = new Object[rubyArgs.length];

                        for (int i = 0; i < args.length; i++) {
                            args[i] = rubyToJava(rubyArgs[i]);
                        }
                        try {
                            return javaToRuby(ruby, constructor.newInstance(args));
                        } catch (InstantiationException ex) {
                            if (ruby.getDebug().isTrue()) ex.printStackTrace();
                            throw ruby.newTypeError("Could not instantiate " + cls.getCanonicalName() + " using " + prettyParams(constructor.getParameterTypes()));
                        } catch (IllegalAccessException ex) {
                            if (ruby.getDebug().isTrue()) ex.printStackTrace();
                            throw ruby.newTypeError("Could not instantiate " + cls.getCanonicalName() + " using " + prettyParams(constructor.getParameterTypes()));
                        } catch (IllegalArgumentException ex) {
                            if (ruby.getDebug().isTrue()) ex.printStackTrace();
                            throw ruby.newTypeError("Could not instantiate " + cls.getCanonicalName() + " using " + prettyParams(constructor.getParameterTypes()));
                        } catch (InvocationTargetException ex) {
                            if (ruby.getDebug().isTrue()) ex.printStackTrace();
                            throw ruby.newTypeError("Could not instantiate " + cls.getCanonicalName() + " using " + prettyParams(constructor.getParameterTypes()));
                        }
                    }
                };
            }

            // if not already defined, we add a 'new' that guesses at which signature to use
            // TODO: just adding first one right now...add in signature-guessing logic
            if (rubySing.getMethods().get("new") == null) {
                rubySing.addMethod("new", dynMethod);
            }

            // add 'new' with full signature, so it's guaranteed to be directly accessible
            // TODO: no need for this to be a full, formal JVM signature
            rubySing.addMethod("new" + prettyParams(constructor.getParameterTypes()), dynMethod);
        }
    }

    private static void populateDeclaredMethods(RubyModule rubyMod, final Class cls, boolean includeStatic) throws SecurityException {
        // add all instance and static methods
        Method[] methods = cls.getDeclaredMethods();
        for (final Method method : methods) {
            String name = method.getName();
            RubyModule target;

            // only public methods
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }

            if (Modifier.isStatic(method.getModifiers())) {
                if (!includeStatic) continue; // only include static methods if specified
                
                target = rubyMod.getSingletonClass();
            } else {
                target = rubyMod;
            }

            JavaMethodFactory factory = getMethodFactory(method.getReturnType());
            DynamicMethod dynMethod = factory.createMethod(target, method);

            // if not overloaded, we add a method that guesses at which signature to use
            // TODO: just adding first one right now...add in signature-guessing logic
            if (target.getMethods().get(name) == null) {
                target.addMethod(name, dynMethod);
            }

            // add method with full signature, so it's guaranteed to be directly accessible
            // TODO: no need for this to be a full, formal JVM signature
            name = name + prettyParams(method.getParameterTypes());
            target.addMethod(name, dynMethod);
        }
    }
    
    private static void populateSpecialMethods(RubyModule rubyMod, RubyModule rubySing, final Class cls) {
        final Ruby ruby = rubyMod.getRuntime();
        
        // add a few type-specific special methods
        rubySing.addMethod("java_class", new JavaMethod.JavaMethodZero(rubySing, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                return javaToRuby(ruby, cls);
            }
        });
    }

    private static void populateFields(RubyModule rubyMod, final Class cls) throws RaiseException, SecurityException {
        Ruby ruby = rubyMod.getRuntime();
        
        // add all static variables
        Field[] fields = cls.getDeclaredFields();
        for (Field field : fields) {
            // only public static fields that are valid constants
            if (Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers()) && IdUtil.isConstant(field.getName())) {
                Object value = null;
                try {
                    value = field.get(null);
                } catch (Exception e) {
                    throw ruby.newTypeError("Could not access field " + cls.getCanonicalName() + "::" + field.getName() + " using " + ci(field.getType()));
                }
                rubyMod.defineConstant(field.getName(), new JavaObjectWrapper((RubyClass) getMirrorForClass(ruby, value.getClass()), value));
            }
        }
    }

    protected static void populateMirrorForArrayClass(RubyModule rubyMod, Class cls) {
        final Ruby ruby = rubyMod.getRuntime();
        
        rubyMod.addMethod("[]", new JavaMethod.JavaMethodOneOrTwo(rubyMod, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg) {
                Object array = rubyToJava(self);
                int x = RubyFixnum.fix2int(arg.convertToInteger());
                return javaToRuby(ruby, Array.get(array, x));
            }

            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
                Object array = rubyToJava(self);
                int x = RubyFixnum.fix2int(arg0.convertToInteger());
                int y = RubyFixnum.fix2int(arg1.convertToInteger());
                return javaToRuby(ruby, Array.get(Array.get(array, x), y));
            }
        });

        rubyMod.addMethod("[]=", new JavaMethod.JavaMethodTwoOrThree(rubyMod, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
                Object array = rubyToJava(self);
                int x = RubyFixnum.fix2int(arg0.convertToInteger());
                Object obj = rubyToJava(arg1);
                Array.set(array, x, obj);

                return arg1;
            }

            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
                Object array = rubyToJava(self);
                int x = RubyFixnum.fix2int(arg0.convertToInteger());
                int y = RubyFixnum.fix2int(arg1.convertToInteger());
                Object obj = rubyToJava(arg2);
                Array.set(Array.get(array, x), y, obj);

                return arg2;
            }
        });

        rubyMod.addMethod("length", new JavaMethod.JavaMethodZero(rubyMod, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                Object array = rubyToJava(self);
                return javaToRuby(ruby, Array.getLength(array));
            }
        });
    }
    static final Map<Class, JavaMethodFactory> methodFactories = new HashMap();
    static final JavaMethodFactory JAVA_OBJECT_METHOD_FACTORY = new JavaMethodFactory() {
        public DynamicMethod createMethod(RubyClass klazz, Method method) {
            return new JavaObjectWrapperMethod(klazz, method);
        }
    };

    protected static JavaMethodFactory getMethodFactory(Class returnType) {
        JavaMethodFactory factory = methodFactories.get(returnType);

        if (factory == null) {
            return JAVA_OBJECT_METHOD_FACTORY;
        }

        return factory;
    }
    

    static {
        methodFactories.put(void.class, new JavaMethodFactory() {
            @Override
            public DynamicMethod createMethod(RubyModule klazz, Method method) {
                Class[] parameters = method.getParameterTypes();
                if (parameters.length > 0) {
                    return new JavaVoidWrapperMethod(klazz, method);
                } else {
                    return new JavaVoidWrapperMethodZero(klazz, method);
                }
            }
        });
    }

    public static class JavaMethodFactory {
        public DynamicMethod createMethod(RubyModule klazz, Method method) {
            Class[] params = method.getParameterTypes();
            if (params.length > 0) {
                return new JavaObjectWrapperMethod(klazz, method);
            } else {
                return new JavaObjectWrapperMethodZero(klazz, method);
            }
        }
    }

    public static abstract class AbstractJavaWrapperMethodZero extends JavaMethod.JavaMethodZero {
        protected final Handle handle;
        protected final boolean isStatic;
        protected final String className;
        protected final String methodName;
        protected final String prettySig;
        protected final Ruby ruby;

        public AbstractJavaWrapperMethodZero(RubyModule klazz, Method method) {
            super(klazz, Visibility.PUBLIC);

            this.handle = HandleFactory.createHandle(klazz.getRuntime().getJRubyClassLoader(), method);
            this.isStatic = Modifier.isStatic(method.getModifiers());
            this.className = method.getDeclaringClass().getCanonicalName();
            this.methodName = method.getName();
            this.prettySig = prettyParams(method.getParameterTypes());
            this.ruby = klazz.getRuntime();
        }

        protected RaiseException error(ThreadContext context, Exception e) throws RaiseException {
            if (ruby.getDebug().isTrue()) {
                e.printStackTrace();
            }
            throw ruby.newTypeError("Could not dispatch to " + className + "#" + methodName + " using " + prettySig);
        }
    }

    public static abstract class AbstractJavaWrapperMethod extends JavaMethod {
        protected final Handle handle;
        protected final boolean isStatic;
        protected final String className;
        protected final String methodName;
        protected final String prettySig;
        protected final Ruby ruby;

        public AbstractJavaWrapperMethod(RubyModule klazz, Method method) {
            super(klazz, Visibility.PUBLIC);

            this.handle = HandleFactory.createHandle(klazz.getRuntime().getJRubyClassLoader(), method);
            this.isStatic = Modifier.isStatic(method.getModifiers());
            this.className = method.getDeclaringClass().getCanonicalName();
            this.methodName = method.getName();
            this.prettySig = prettyParams(method.getParameterTypes());
            this.ruby = klazz.getRuntime();
        }

        protected RaiseException error(Exception e) throws RaiseException {
            return MiniJava.error(ruby, e, "Could not dispatch to " + className + "#" + methodName + " using " + prettySig);
        }
    }
    
    protected static RaiseException error(Ruby ruby, Exception e, String message) throws RaiseException {
        if (ruby.getDebug().isTrue()) {
            e.printStackTrace();
        }
        throw ruby.newTypeError(message);
    }

    protected static class JavaObjectWrapperMethodZero extends AbstractJavaWrapperMethodZero {
        public JavaObjectWrapperMethodZero(RubyModule klazz, Method method) {
            super(klazz, method);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            Object result = (Object) handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object);

            return javaToRuby(ruby, result);
        }
    }

    protected static class JavaObjectWrapperMethod extends AbstractJavaWrapperMethod {
        public JavaObjectWrapperMethod(RubyModule klazz, Method method) {
            super(klazz, method);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            Object[] newArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                IRubyObject arg = args[i];
                newArgs[i] = rubyToJava(arg);
            }

            Object result = (Object) handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, newArgs);

            return javaToRuby(ruby, result);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
            Object result = (Object) handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object);

            return javaToRuby(ruby, result);
    }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
            Object result = (Object) handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0));

            return javaToRuby(ruby, result);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
            Object result = (Object) handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0), rubyToJava(arg1));

            return javaToRuby(ruby, result);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            Object result = (Object) handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0), rubyToJava(arg1), rubyToJava(arg2));

            return javaToRuby(ruby, result);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            Object[] newArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                IRubyObject arg = args[i];
                newArgs[i] = rubyToJava(arg);
            }

            Object result = (Object) handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, newArgs);

            return javaToRuby(ruby, result);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            Object result = (Object) handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object);

            return javaToRuby(ruby, result);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
            Object result = (Object) handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0));

            return javaToRuby(ruby, result);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
            Object result = (Object) handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0), rubyToJava(arg1));

            return javaToRuby(ruby, result);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
            Object result = (Object) handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0), rubyToJava(arg1), rubyToJava(arg2));

            return javaToRuby(ruby, result);
        }
    }

    protected static class JavaVoidWrapperMethod extends AbstractJavaWrapperMethod {
        public JavaVoidWrapperMethod(RubyModule klazz, Method method) {
            super(klazz, method);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            Object[] newArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                IRubyObject arg = args[i];
                newArgs[i] = rubyToJava(arg);
            }

            handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, newArgs);

            return self;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
            handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object);

            return self;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
            handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0));

            return self;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
            handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0), rubyToJava(arg1));

            return self;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0), rubyToJava(arg1), rubyToJava(arg2));

            return self;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            Object[] newArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                IRubyObject arg = args[i];
                newArgs[i] = rubyToJava(arg);
            }

            handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, newArgs);

            return self;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object);

            return self;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
            handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0));

            return self;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
            handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0), rubyToJava(arg1));

            return self;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
            handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0), rubyToJava(arg1), rubyToJava(arg2));

            return self;
        }
    }

    protected static class JavaVoidWrapperMethodZero extends AbstractJavaWrapperMethodZero {
        public JavaVoidWrapperMethodZero(RubyModule klazz, Method method) {
            super(klazz, method);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object);

            return self;
        }
    }

    public static Object rubyToJava(IRubyObject object) {
        if (object.isNil()) {
            return null;
        } else if (object instanceof JavaObjectWrapper) {
            return ((JavaObjectWrapper) object).object;
        } else {
            return object;
        }
    }

    public static IRubyObject javaToRuby(Ruby ruby, Object object) {
        if (object == null) {
            return ruby.getNil();
        } else if (object instanceof IRubyObject) {
            return (IRubyObject) object;
        } else {
            return new JavaObjectWrapper((RubyClass) getMirrorForClass(ruby, object.getClass()), object);
        }
    }

    public static class JavaObjectWrapper extends RubyObject {
        Object object;

        public JavaObjectWrapper(RubyClass klazz, Object object) {
            super(klazz.getRuntime(), klazz);
            this.object = object;
        }
    };

    public static Class getJavaClassFromObject(IRubyObject obj) {
        if (!obj.respondsTo("java_class")) {
            throw obj.getRuntime().newTypeError(obj.getMetaClass().getBaseName() + " is not a Java type");
        } else {
            return (Class) rubyToJava(obj.callMethod(obj.getRuntime().getCurrentContext(), "java_class"));
        }
    }
    
    public static DynamicMethod searchMethod(RubyClass clazz, String name1) {
        return clazz.searchMethod(name1);
    }
    
    public static DynamicMethod searchMethod(RubyClass clazz, String name1, String name2) {
        DynamicMethod method = clazz.searchMethod(name1);
        if (method == UndefinedMethod.INSTANCE) {
            return searchMethod(clazz, name2);
        }
        return method;
    }
    
    public static DynamicMethod searchMethod(RubyClass clazz, String name1, String name2, String name3) {
        DynamicMethod method = clazz.searchMethod(name1);
        if (method == UndefinedMethod.INSTANCE) {
            return searchMethod(clazz, name2, name3);
        }
        return method;
    }
    
    public static DynamicMethod searchMethod(RubyClass clazz, String name1, String name2, String name3, String name4) {
        DynamicMethod method = clazz.searchMethod(name1);
        if (method == UndefinedMethod.INSTANCE) {
            return searchMethod(clazz, name2, name3, name4);
        }
        return method;
    }
    
    public static DynamicMethod searchMethod(RubyClass clazz, String name1, String name2, String name3, String name4, String name5) {
        DynamicMethod method = clazz.searchMethod(name1);
        if (method == UndefinedMethod.INSTANCE) {
            return searchMethod(clazz, name2, name3, name4, name5);
        }
        return method;
    }
    
    public static DynamicMethod searchMethod(RubyClass clazz, String name1, String name2, String name3, String name4, String name5, String name6) {
        DynamicMethod method = clazz.searchMethod(name1);
        if (method == UndefinedMethod.INSTANCE) {
            return searchMethod(clazz, name2, name3, name4, name5, name6);
        }
        return method;
    }
    
    public static DynamicMethod searchMethod(RubyClass clazz, String name1, String name2, String name3, String name4, String name5, String name6, String name7) {
        DynamicMethod method = clazz.searchMethod(name1);
        if (method == UndefinedMethod.INSTANCE) {
            return searchMethod(clazz, name2, name3, name4, name5, name6, name7);
        }
        return method;
    }
    
    public static DynamicMethod searchMethod(RubyClass clazz, String name1, String name2, String name3, String name4, String name5, String name6, String name7, String name8) {
        DynamicMethod method = clazz.searchMethod(name1);
        if (method == UndefinedMethod.INSTANCE) {
            return searchMethod(clazz, name2, name3, name4, name5, name6, name8);
        }
        return method;
    }
}
