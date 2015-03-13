/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.java.codegen;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jruby.Ruby;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.ast.executable.RuntimeCache;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.compiler.util.BasicObjectStubGenerator;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.util.ClassDefiningClassLoader;
import org.jruby.util.ClassDefiningJRubyClassLoader;
import static org.jruby.util.CodegenUtils.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import static org.objectweb.asm.Opcodes.*;

/**
 *
 * @author headius
 */
public class RealClassGenerator {
    private static final boolean DEBUG = false;

    private static Map<String, List<Method>> buildSimpleToAllMap(Class[] interfaces, String[] superTypeNames) throws SecurityException {
        Map<String, List<Method>> simpleToAll = new HashMap<String, List<Method>>();
        for (int i = 0; i < interfaces.length; i++) {
            superTypeNames[i] = p(interfaces[i]);
            for (Method method : interfaces[i].getMethods()) {
                List<Method> methods = simpleToAll.get(method.getName());
                if (methods == null) {
                    simpleToAll.put(method.getName(), methods = new ArrayList<Method>());
                }
                methods.add(method);
            }
        }
        return simpleToAll;
    }

    public static Class createOldStyleImplClass(Class[] superTypes, RubyClass rubyClass, Ruby ruby, String name, ClassDefiningClassLoader classLoader) {
        String[] superTypeNames = new String[superTypes.length];
        Map<String, List<Method>> simpleToAll = buildSimpleToAllMap(superTypes, superTypeNames);
        
        Class newClass = defineOldStyleImplClass(ruby, name, superTypeNames, simpleToAll, classLoader);
        
        return newClass;
    }

    public static Class createRealImplClass(Class superClass, Class[] interfaces, RubyClass rubyClass, Ruby ruby, String name) {
        String[] superTypeNames = new String[interfaces.length];
        Map<String, List<Method>> simpleToAll = buildSimpleToAllMap(interfaces, superTypeNames);

        Class newClass = defineRealImplClass(ruby, name, superClass, superTypeNames, simpleToAll);

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
    public static Class defineOldStyleImplClass(Ruby ruby, String name, String[] superTypeNames, Map<String, List<Method>> simpleToAll, ClassDefiningClassLoader classLoader) {
        Class newClass;
        byte[] bytes;

        synchronized (classLoader) {
            // try to load the specified name; only if that fails, try to define the class
            try {
                newClass = classLoader.loadClass(name);
            } catch (ClassNotFoundException cnfe) {
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                String pathName = name.replace('.', '/');

                // construct the class, implementing all supertypes
                cw.visit(V1_5, ACC_PUBLIC | ACC_SUPER, pathName, null, p(Object.class), superTypeNames);
                cw.visitSource(pathName + ".gen", null);

                // fields needed for dispatch and such
                cw.visitField(ACC_STATIC | ACC_FINAL | ACC_PRIVATE, "$runtimeCache", ci(RuntimeCache.class), null, null).visitEnd();
                cw.visitField(ACC_PRIVATE | ACC_FINAL, "$self", ci(IRubyObject.class), null, null).visitEnd();

                // create static init
                SkinnyMethodAdapter clinitMethod = new SkinnyMethodAdapter(cw, ACC_PUBLIC | ACC_STATIC, "<clinit>", sig(void.class), null, null);

                // create constructor
                SkinnyMethodAdapter initMethod = new SkinnyMethodAdapter(cw, ACC_PUBLIC, "<init>", sig(void.class, IRubyObject.class), null, null);
                initMethod.aload(0);
                initMethod.invokespecial(p(Object.class), "<init>", sig(void.class));

                // store the wrapper
                initMethod.aload(0);
                initMethod.aload(1);
                initMethod.putfield(pathName, "$self", ci(IRubyObject.class));

                // end constructor
                initMethod.voidreturn();
                initMethod.end();

                int cacheSize = 0;

                // for each simple method name, implement the complex methods, calling the simple version
                for (Map.Entry<String, List<Method>> entry : simpleToAll.entrySet()) {
                    String simpleName = entry.getKey();
                    Set<String> nameSet = JavaUtil.getRubyNamesForJavaName(simpleName, entry.getValue());

                    Set<String> implementedNames = new HashSet<String>();

                    for (Method method : entry.getValue()) {
                        Class[] paramTypes = method.getParameterTypes();
                        Class returnType = method.getReturnType();

                        String fullName = simpleName + prettyParams(paramTypes);
                        if (implementedNames.contains(fullName)) continue;
                        implementedNames.add(fullName);

                        // indices for temp values
                        int baseIndex = 1;
                        for (Class paramType : paramTypes) {
                            if (paramType == double.class || paramType == long.class) {
                                baseIndex += 2;
                            } else {
                                baseIndex += 1;
                            }
                        }
                        int selfIndex = baseIndex;
                        int rubyIndex = selfIndex + 1;

                        SkinnyMethodAdapter mv = new SkinnyMethodAdapter(
                                cw, ACC_PUBLIC, simpleName, sig(returnType, paramTypes), null, null);
                        mv.start();
                        mv.line(1);

                        // TODO: this code should really check if a Ruby equals method is implemented or not.
                        if (simpleName.equals("equals") && paramTypes.length == 1 && paramTypes[0] == Object.class && returnType == Boolean.TYPE) {
                            mv.line(2);
                            mv.aload(0);
                            mv.aload(1);
                            mv.invokespecial(p(Object.class), "equals", sig(Boolean.TYPE, params(Object.class)));
                            mv.ireturn();
                        } else if (simpleName.equals("hashCode") && paramTypes.length == 0 && returnType == Integer.TYPE) {
                            mv.line(3);
                            mv.aload(0);
                            mv.invokespecial(p(Object.class), "hashCode", sig(Integer.TYPE));
                            mv.ireturn();
                        } else if (simpleName.equals("toString") && paramTypes.length == 0 && returnType == String.class) {
                            mv.line(4);
                            mv.aload(0);
                            mv.invokespecial(p(Object.class), "toString", sig(String.class));
                            mv.areturn();
                        } else if (simpleName.equals("__ruby_object") && paramTypes.length == 0 && returnType == IRubyObject.class) {
                            mv.aload(0);
                            mv.getfield(pathName, "$self", ci(IRubyObject.class));
                            mv.areturn();
                        } else {
                            mv.line(5);

                            int cacheIndex = cacheSize++;

                            // prepare temp locals
                            mv.aload(0);
                            mv.getfield(pathName, "$self", ci(IRubyObject.class));
                            mv.astore(selfIndex);
                            mv.aload(selfIndex);
                            mv.invokeinterface(p(IRubyObject.class), "getRuntime", sig(Ruby.class));
                            mv.astore(rubyIndex);

                            // get method from cache
                            mv.getstatic(pathName, "$runtimeCache", ci(RuntimeCache.class));
                            mv.aload(selfIndex);
                            mv.ldc(cacheIndex);
                            for (String eachName : nameSet) {
                                mv.ldc(eachName);
                            }
                            mv.invokevirtual(p(RuntimeCache.class), "searchWithCache",
                                    sig(DynamicMethod.class, params(IRubyObject.class, int.class, String.class, nameSet.size())));

                            // get current context
                            mv.aload(rubyIndex);
                            mv.invokevirtual(p(Ruby.class), "getCurrentContext", sig(ThreadContext.class));

                            // load self, class, and name
                            mv.aloadMany(selfIndex, selfIndex);
                            mv.invokeinterface(p(IRubyObject.class), "getMetaClass", sig(RubyClass.class));
                            mv.ldc(simpleName);

                            // coerce arguments
                            coerceArgumentsToRuby(mv, paramTypes, rubyIndex);

                            // load null block
                            mv.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));

                            // invoke method
                            mv.line(13);
                            mv.invokevirtual(p(DynamicMethod.class), "call", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject[].class, Block.class));

                            coerceResultAndReturn(mv, returnType);
                        }
                        mv.end();
                    }
                }

                // end setup method
                clinitMethod.newobj(p(RuntimeCache.class));
                clinitMethod.dup();
                clinitMethod.invokespecial(p(RuntimeCache.class), "<init>", sig(void.class));
                clinitMethod.dup();
                clinitMethod.ldc(cacheSize);
                clinitMethod.invokevirtual(p(RuntimeCache.class), "initMethodCache", sig(void.class, int.class));
                clinitMethod.putstatic(pathName, "$runtimeCache", ci(RuntimeCache.class));
                clinitMethod.voidreturn();
                clinitMethod.end();

                // end class
                cw.visitEnd();

                // create the class
                bytes = cw.toByteArray();

                newClass = classLoader.defineClass(name, cw.toByteArray());
            }
        }

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
    public static Class defineRealImplClass(Ruby ruby, String name, Class superClass, String[] superTypeNames, Map<String, List<Method>> simpleToAll) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        String pathName = name.replace('.', '/');

        boolean isRubyHierarchy = RubyBasicObject.class.isAssignableFrom(superClass);

        // construct the class, implementing all supertypes
        if (isRubyHierarchy) {
            // Ruby hierarchy...just extend it
            cw.visit(V1_5, ACC_PUBLIC | ACC_SUPER, pathName, null, p(superClass), superTypeNames);
        } else {
            // Non-Ruby hierarchy; add IRubyObject
            String[] plusIRubyObject = new String[superTypeNames.length + 1];
            plusIRubyObject[0] = p(IRubyObject.class);
            System.arraycopy(superTypeNames, 0, plusIRubyObject, 1, superTypeNames.length);
            
            cw.visit(V1_5, ACC_PUBLIC | ACC_SUPER, pathName, null, p(superClass), plusIRubyObject);
        }
        cw.visitSource(pathName + ".gen", null);

        // fields needed for dispatch and such
        cw.visitField(ACC_STATIC | ACC_FINAL | ACC_PRIVATE, "$runtimeCache", ci(RuntimeCache.class), null, null).visitEnd();

        // create static init
        SkinnyMethodAdapter clinitMethod = new SkinnyMethodAdapter(cw, ACC_PUBLIC | ACC_STATIC, "<clinit>", sig(void.class), null, null);

        // create constructor
        SkinnyMethodAdapter initMethod = new SkinnyMethodAdapter(cw, ACC_PUBLIC, "<init>", sig(void.class, Ruby.class, RubyClass.class), null, null);

        if (isRubyHierarchy) {
            // superclass is in the Ruby object hierarchy; invoke typical Ruby superclass constructor
            initMethod.aloadMany(0, 1, 2);
            initMethod.invokespecial(p(superClass), "<init>", sig(void.class, Ruby.class, RubyClass.class));
        } else {
            // superclass is not in Ruby hierarchy; store objects and call no-arg super constructor
            cw.visitField(ACC_FINAL | ACC_PRIVATE, "$ruby", ci(Ruby.class), null, null).visitEnd();
            cw.visitField(ACC_FINAL | ACC_PRIVATE, "$rubyClass", ci(RubyClass.class), null, null).visitEnd();

            initMethod.aloadMany(0, 1);
            initMethod.putfield(pathName, "$ruby", ci(Ruby.class));
            initMethod.aloadMany(0, 2);
            initMethod.putfield(pathName, "$rubyClass", ci(RubyClass.class));

            // only no-arg super constructor supported right now
            initMethod.aload(0);
            initMethod.invokespecial(p(superClass), "<init>", sig(void.class));
        }
        initMethod.voidreturn();
        initMethod.end();

        if (isRubyHierarchy) {
            // override toJava
            SkinnyMethodAdapter toJavaMethod = new SkinnyMethodAdapter(cw, ACC_PUBLIC, "toJava", sig(Object.class, Class.class), null, null);
            toJavaMethod.aload(0);
            toJavaMethod.areturn();
            toJavaMethod.end();
        } else {
            // decorate with stubbed IRubyObject methods
            BasicObjectStubGenerator.addBasicObjectStubsToClass(cw);

            // add getRuntime and getMetaClass impls based on captured fields
            SkinnyMethodAdapter getRuntimeMethod = new SkinnyMethodAdapter(cw, ACC_PUBLIC, "getRuntime", sig(Ruby.class), null, null);
            getRuntimeMethod.aload(0);
            getRuntimeMethod.getfield(pathName, "$ruby", ci(Ruby.class));
            getRuntimeMethod.areturn();
            getRuntimeMethod.end();

            SkinnyMethodAdapter getMetaClassMethod = new SkinnyMethodAdapter(cw, ACC_PUBLIC, "getMetaClass", sig(RubyClass.class), null, null);
            getMetaClassMethod.aload(0);
            getMetaClassMethod.getfield(pathName, "$rubyClass", ci(RubyClass.class));
            getMetaClassMethod.areturn();
            getMetaClassMethod.end();
        }

        int cacheSize = 0;
        
        // for each simple method name, implement the complex methods, calling the simple version
        for (Map.Entry<String, List<Method>> entry : simpleToAll.entrySet()) {
            String simpleName = entry.getKey();
            Set<String> nameSet = JavaUtil.getRubyNamesForJavaName(simpleName, entry.getValue());

            Set<String> implementedNames = new HashSet<String>();

            for (Method method : entry.getValue()) {
                Class[] paramTypes = method.getParameterTypes();
                Class returnType = method.getReturnType();

                String fullName = simpleName + prettyParams(paramTypes);
                if (implementedNames.contains(fullName)) continue;
                implementedNames.add(fullName);

                // indices for temp values
                int baseIndex = 1;
                for (Class paramType : paramTypes) {
                    if (paramType == double.class || paramType == long.class) {
                        baseIndex += 2;
                    } else {
                        baseIndex += 1;
                    }
                }
                int rubyIndex = baseIndex + 1;

                SkinnyMethodAdapter mv = new SkinnyMethodAdapter(
                        cw, ACC_PUBLIC, simpleName, sig(returnType, paramTypes), null, null);
                mv.start();
                mv.line(1);

                // TODO: this code should really check if a Ruby equals method is implemented or not.
                if(simpleName.equals("equals") && paramTypes.length == 1 && paramTypes[0] == Object.class && returnType == Boolean.TYPE) {
                    mv.line(2);
                    mv.aload(0);
                    mv.aload(1);
                    mv.invokespecial(p(Object.class), "equals", sig(Boolean.TYPE, params(Object.class)));
                    mv.ireturn();
                } else if(simpleName.equals("hashCode") && paramTypes.length == 0 && returnType == Integer.TYPE) {
                    mv.line(3);
                    mv.aload(0);
                    mv.invokespecial(p(Object.class), "hashCode", sig(Integer.TYPE));
                    mv.ireturn();
                } else if(simpleName.equals("toString") && paramTypes.length == 0 && returnType == String.class) {
                    mv.line(4);
                    mv.aload(0);
                    mv.invokespecial(p(Object.class), "toString", sig(String.class));
                    mv.areturn();
                } else {
                    mv.line(5);

                    int cacheIndex = cacheSize++;
                    
                    // prepare temp locals
                    mv.aload(0);
                    mv.invokeinterface(p(IRubyObject.class), "getRuntime", sig(Ruby.class));
                    mv.astore(rubyIndex);

                    // get method from cache
                    mv.getstatic(pathName, "$runtimeCache", ci(RuntimeCache.class));
                    mv.aload(0);
                    mv.ldc(cacheIndex);
                    for (String eachName : nameSet) {
                        mv.ldc(eachName);
                    }
                    mv.invokevirtual(p(RuntimeCache.class), "searchWithCache",
                            sig(DynamicMethod.class, params(IRubyObject.class, int.class, String.class, nameSet.size())));
                    
                    // get current context
                    mv.aload(rubyIndex);
                    mv.invokevirtual(p(Ruby.class), "getCurrentContext", sig(ThreadContext.class));

                    // load self, class, and name
                    mv.aloadMany(0, 0);
                    mv.invokeinterface(p(IRubyObject.class), "getMetaClass", sig(RubyClass.class));
                    mv.ldc(simpleName);

                    // coerce arguments
                    coerceArgumentsToRuby(mv, paramTypes, rubyIndex);

                    // load null block
                    mv.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));

                    // invoke method
                    mv.line(13);
                    mv.invokevirtual(p(DynamicMethod.class), "call", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject[].class, Block.class));

                    coerceResultAndReturn(mv, returnType);
                }
                mv.end();
            }
        }

        // end setup method
        clinitMethod.newobj(p(RuntimeCache.class));
        clinitMethod.dup();
        clinitMethod.invokespecial(p(RuntimeCache.class), "<init>", sig(void.class));
        clinitMethod.dup();
        clinitMethod.ldc(cacheSize);
        clinitMethod.invokevirtual(p(RuntimeCache.class), "initMethodCache", sig(void.class, int.class));
        clinitMethod.putstatic(pathName, "$runtimeCache", ci(RuntimeCache.class));
        clinitMethod.voidreturn();
        clinitMethod.end();

        // end class
        cw.visitEnd();

        // create the class
        byte[] bytes = cw.toByteArray();
        Class newClass;
        ClassDefiningJRubyClassLoader loader;
        if (superClass.getClassLoader() instanceof ClassDefiningJRubyClassLoader) {
            loader = new ClassDefiningJRubyClassLoader(superClass.getClassLoader());
        } else {
            loader = new ClassDefiningJRubyClassLoader(ruby.getJRubyClassLoader());
        }
        try {
            newClass = loader.loadClass(name);
        } catch (ClassNotFoundException cnfe) {
            newClass = loader.defineClass(name, cw.toByteArray());
        }

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

    public static void coerceArgumentsToRuby(SkinnyMethodAdapter mv, Class[] paramTypes, int rubyIndex) {
        // load arguments into IRubyObject[] for dispatch
        if (paramTypes.length != 0) {
            mv.pushInt(paramTypes.length);
            mv.anewarray(p(IRubyObject.class));

            // TODO: make this do specific-arity calling
            for (int i = 0, argIndex = 1; i < paramTypes.length; i++) {
                Class paramType = paramTypes[i];
                mv.dup();
                mv.pushInt(i);
                // convert to IRubyObject
                mv.aload(rubyIndex);
                if (paramTypes[i].isPrimitive()) {
                    if (paramType == byte.class || paramType == short.class || paramType == char.class || paramType == int.class) {
                        mv.iload(argIndex++);
                        mv.invokestatic(p(JavaUtil.class), "convertJavaToRuby", sig(IRubyObject.class, Ruby.class, int.class));
                    } else if (paramType == long.class) {
                        mv.lload(argIndex);
                        argIndex += 2; // up two slots, for long's two halves
                        mv.invokestatic(p(JavaUtil.class), "convertJavaToRuby", sig(IRubyObject.class, Ruby.class, long.class));
                    } else if (paramType == float.class) {
                        mv.fload(argIndex++);
                        mv.invokestatic(p(JavaUtil.class), "convertJavaToRuby", sig(IRubyObject.class, Ruby.class, float.class));
                    } else if (paramType == double.class) {
                        mv.dload(argIndex);
                        argIndex += 2; // up two slots, for long's two halves
                        mv.invokestatic(p(JavaUtil.class), "convertJavaToRuby", sig(IRubyObject.class, Ruby.class, double.class));
                    } else if (paramType == boolean.class) {
                        mv.iload(argIndex++);
                        mv.invokestatic(p(JavaUtil.class), "convertJavaToRuby", sig(IRubyObject.class, Ruby.class, boolean.class));
                    }
                } else {
                    mv.aload(argIndex++);
                    mv.invokestatic(p(JavaUtil.class), "convertJavaToUsableRubyObject", sig(IRubyObject.class, Ruby.class, Object.class));
                }
                mv.aastore();
            }
        } else {
            mv.getstatic(p(IRubyObject.class), "NULL_ARRAY", ci(IRubyObject[].class));
        }
    }

    public static void coerceResultAndReturn(SkinnyMethodAdapter mv, Class returnType) {
        // if we expect a return value, unwrap it
        if (returnType != void.class) {
            // TODO: move the bulk of this logic to utility methods
            if (returnType.isPrimitive()) {
                if (returnType == boolean.class) {
                    mv.getstatic(p(Boolean.class), "TYPE", ci(Class.class));
                    mv.invokeinterface(p(IRubyObject.class), "toJava", sig(Object.class, Class.class));
                    mv.checkcast(p(Boolean.class));
                    mv.invokevirtual(p(Boolean.class), "booleanValue", sig(boolean.class));
                    mv.ireturn();
                } else {
                    mv.getstatic(p(getBoxType(returnType)), "TYPE", ci(Class.class));
                    mv.invokeinterface(p(IRubyObject.class), "toJava", sig(Object.class, Class.class));
                    if (returnType == byte.class) {
                        mv.checkcast(p(Number.class));
                        mv.invokevirtual(p(Number.class), "byteValue", sig(byte.class));
                        mv.ireturn();
                    } else if (returnType == short.class) {
                        mv.checkcast(p(Number.class));
                        mv.invokevirtual(p(Number.class), "shortValue", sig(short.class));
                        mv.ireturn();
                    } else if (returnType == char.class) {
                        mv.checkcast(p(Character.class));
                        mv.invokevirtual(p(Character.class), "charValue", sig(char.class));
                        mv.ireturn();
                    } else if (returnType == int.class) {
                        mv.checkcast(p(Number.class));
                        mv.invokevirtual(p(Number.class), "intValue", sig(int.class));
                        mv.ireturn();
                    } else if (returnType == long.class) {
                        mv.checkcast(p(Number.class));
                        mv.invokevirtual(p(Number.class), "longValue", sig(long.class));
                        mv.lreturn();
                    } else if (returnType == float.class) {
                        mv.checkcast(p(Number.class));
                        mv.invokevirtual(p(Number.class), "floatValue", sig(float.class));
                        mv.freturn();
                    } else if (returnType == double.class) {
                        mv.checkcast(p(Number.class));
                        mv.invokevirtual(p(Number.class), "doubleValue", sig(double.class));
                        mv.dreturn();
                    }
                }
            } else {
                mv.ldc(Type.getType(returnType));
                mv.invokeinterface(p(IRubyObject.class), "toJava", sig(Object.class, Class.class));
                mv.checkcast(p(returnType));
                mv.areturn();
            }
        } else {
            mv.voidreturn();
        }
    }

    public static boolean isCacheOk(CacheEntry entry, IRubyObject self) {
        return CacheEntry.typeOk(entry, self.getMetaClass()) && entry.method != UndefinedMethod.INSTANCE;
    }
}
