package org.jruby.java.proxies;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyHash.Visitor;
import org.jruby.RubyMethod;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodNBlock;
import org.jruby.java.addons.ArrayJavaAddons;
import org.jruby.java.invokers.InstanceFieldGetter;
import org.jruby.java.invokers.InstanceFieldSetter;
import org.jruby.java.invokers.InstanceMethodInvoker;
import org.jruby.java.invokers.MethodInvoker;
import org.jruby.java.invokers.StaticMethodInvoker;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaMethod;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CodegenUtils;

public class JavaProxy extends RubyObject {
    protected final RubyClass.VariableAccessor objectAccessor;
    private Object object;
    
    public JavaProxy(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
        objectAccessor = klazz.getVariableAccessorForWrite("__wrap_struct__");
    }

    public Object dataGetStruct() {
        return objectAccessor.get(this);
    }

    public void dataWrapStruct(Object object) {
        objectAccessor.set(this, object);
        this.object = ((JavaObject)object).getValue();
    }

    public Object getObject() {
        // FIXME: Added this because marshal_spec seemed to reconstitute objects without calling dataWrapStruct
        // this resulted in object being null after unmarshalling...
        if (object == null) object = ((JavaObject)dataGetStruct()).getValue();
        return object;
    }

    private JavaObject getJavaObject() {
        return (JavaObject)dataGetStruct();
    }
    
    public static RubyClass createJavaProxy(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        
        RubyClass javaProxy = runtime.defineClass("JavaProxy", runtime.getObject(), new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new JavaProxy(runtime, klazz);
            }
        });
        
        RubyClass singleton = javaProxy.getSingletonClass();
        
        singleton.addReadWriteAttribute(context, "java_class");
        
        javaProxy.defineAnnotatedMethods(JavaProxy.class);
        javaProxy.includeModule(runtime.fastGetModule("JavaProxyMethods"));
        
        return javaProxy;
    }
    
    @JRubyMethod(frame = true, meta = true)
    public static IRubyObject inherited(ThreadContext context, IRubyObject recv, IRubyObject subclass) {
        IRubyObject subJavaClass = RuntimeHelpers.invoke(context, subclass, "java_class");
        if (subJavaClass.isNil()) {
            subJavaClass = RuntimeHelpers.invoke(context, recv, "java_class");
            RuntimeHelpers.invoke(context, subclass, "java_class=", subJavaClass);
        }
        return RuntimeHelpers.invokeSuper(context, recv, subclass, Block.NULL_BLOCK);
    }
    
    @JRubyMethod(meta = true)
    public static IRubyObject singleton_class(IRubyObject recv) {
        return ((RubyClass)recv).getSingletonClass();
    }
    
    @JRubyMethod(name = "[]", meta = true, rest = true)
    public static IRubyObject op_aref(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject javaClass = RuntimeHelpers.invoke(context, recv, "java_class");
        if (args.length > 0) {
            // construct new array proxy (ArrayJavaProxy)
            IRubyObject[] newArgs = new IRubyObject[args.length + 1];
            newArgs[0] = javaClass;
            System.arraycopy(args, 0, newArgs, 1, args.length);
            return context.getRuntime().fastGetClass("ArrayJavaProxyCreator").newInstance(context, newArgs, Block.NULL_BLOCK);
        } else {
            return Java.get_proxy_class(javaClass, RuntimeHelpers.invoke(context, javaClass, "array_class"));
        }
    }

    private static Class<?> getJavaClass(ThreadContext context, RubyModule module) {
        try {
        IRubyObject jClass = RuntimeHelpers.invoke(context, module, "java_class");


        return !(jClass instanceof JavaClass) ? null : ((JavaClass) jClass).javaClass();
        } catch (Exception e) { return null; }
    }
    
    /**
     * Create a name/newname map of fields to be exposed as methods.
     */
    private static Map<String, String> getFieldListFromArgs(IRubyObject[] args) {
        final Map<String, String> map = new HashMap<String, String>();
        
        // Get map of all fields we want to define.  
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof RubyHash) {
                ((RubyHash) args[i]).visitAll(new Visitor() {
                    @Override
                    public void visit(IRubyObject key, IRubyObject value) {
                        map.put(key.asString().toString(), value.asString().toString());
                    }
                });
            } else {
                String value = args[i].asString().toString();
                map.put(value, value);
            }
        }
        
        return map;
    }

    // Look through all mappings to find a match entry for this field
    private static void installField(ThreadContext context, Map<String, String> fieldMap,
            Field field, RubyModule module, boolean asReader, boolean asWriter) {
        boolean isFinal = Modifier.isFinal(field.getModifiers());

        for (Iterator<Map.Entry<String,String>> iter = fieldMap.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<String,String> entry = iter.next();
            String key = entry.getKey();
            if (key.equals(field.getName())) {
                if (Ruby.isSecurityRestricted() && !Modifier.isPublic(field.getModifiers())) {
                    throw context.getRuntime().newSecurityError("Cannot change accessibility on fields in a restricted mode: field '" + field.getName() + "'");
                }
                
                String asName = entry.getValue();

                if (asReader) module.addMethod(asName, new InstanceFieldGetter(key, module, field));
                if (asWriter) {
                    if (isFinal) throw context.getRuntime().newSecurityError("Cannot change final field '" + field.getName() + "'");
                    module.addMethod(asName + "=", new InstanceFieldSetter(key, module, field));
                }
                
                iter.remove();
                break;
            }
        }
    }    

    private static void findFields(ThreadContext context, RubyModule topModule,
            IRubyObject args[], boolean asReader, boolean asWriter) {
        Map<String, String> fieldMap = getFieldListFromArgs(args);
        
        for (RubyModule module = topModule; module != null; module = module.getSuperClass()) {
            Class<?> javaClass = getJavaClass(context, module);
            
            // Hit a non-java proxy class (included Modules can be a cause of this...skip)
            if (javaClass == null) continue;

            Field[] fields = JavaClass.getDeclaredFields(javaClass);
            for (int j = 0; j < fields.length; j++) {
                installField(context, fieldMap, fields[j], module, asReader, asWriter);
            }
        }
        
        // We could not find all of them print out first one (we could print them all?)
        if (!fieldMap.isEmpty()) {
            throw JavaClass.undefinedFieldError(context.getRuntime(),
                    topModule.getName(), fieldMap.keySet().iterator().next());
        }

    }
    
    @JRubyMethod(meta = true, rest = true)
    public static IRubyObject field_accessor(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        findFields(context, (RubyModule) recv, args, true, true);

        return context.getRuntime().getNil();
    }

    @JRubyMethod(meta = true, rest = true)
    public static IRubyObject field_reader(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        findFields(context, (RubyModule) recv, args, true, false);

        return context.getRuntime().getNil();
    }
    
    @JRubyMethod(meta = true, rest = true)
    public static IRubyObject field_writer(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        findFields(context, (RubyModule) recv, args, false, true);

        return context.getRuntime().getNil();
    }

    @JRubyMethod(meta = true)
    public static IRubyObject to_java_object(IRubyObject recv) {
        return recv.getInstanceVariables().fastGetInstanceVariable("@java_class");
    }

    @JRubyMethod(name = "equal?")
    public IRubyObject equal_p(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.getRuntime();
        if (other instanceof JavaProxy) {
            boolean equal = getObject() == ((JavaProxy)other).getObject();
            return runtime.newBoolean(equal);
        }
        return runtime.getFalse();
    }

    @JRubyMethod(backtrace = true)
    public IRubyObject java_send(ThreadContext context, IRubyObject rubyName) {
        String name = rubyName.asJavaString();
        Ruby runtime = context.getRuntime();
        
        JavaMethod method = new JavaMethod(runtime, getMethod(name));
        return method.invokeDirect(getObject());
    }

    @JRubyMethod(backtrace = true)
    public IRubyObject java_send(ThreadContext context, IRubyObject rubyName, IRubyObject argTypes) {
        throw context.getRuntime().newArgumentError(2, 3);
    }

    @JRubyMethod(backtrace = true)
    public IRubyObject java_send(ThreadContext context, IRubyObject rubyName, IRubyObject argTypes, IRubyObject arg0) {
        String name = rubyName.asJavaString();
        RubyArray argTypesAry = argTypes.convertToArray();
        Ruby runtime = context.getRuntime();

        if (argTypesAry.size() != 1) {
            throw JavaMethod.newArgSizeMismatchError(runtime, argTypesAry.size(), 1);
        }

        Class argTypeClass = (Class)argTypesAry.eltInternal(0).toJava(Class.class);

        JavaMethod method = new JavaMethod(runtime, getMethod(name, argTypeClass));
        return method.invokeDirect(getObject(), arg0.toJava(argTypeClass));
    }

    @JRubyMethod(required = 4, rest = true, backtrace = true)
    public IRubyObject java_send(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        
        String name = args[0].asJavaString();
        RubyArray argTypesAry = args[1].convertToArray();
        int argsLen = args.length - 2;

        if (argTypesAry.size() != argsLen) {
            throw JavaMethod.newArgSizeMismatchError(runtime, argTypesAry.size(), argsLen);
        }

        Class[] argTypesClasses = (Class[])argTypesAry.toArray(new Class[argsLen]);

        Object[] argsAry = new Object[argsLen];
        for (int i = 0; i < argsLen; i++) {
            argsAry[i] = args[i + 2].toJava(argTypesClasses[i]);
        }

        JavaMethod method = new JavaMethod(runtime, getMethod(name, argTypesClasses));
        return method.invokeDirect(getObject(), argsAry);
    }

    @JRubyMethod(backtrace = true)
    public IRubyObject java_method(ThreadContext context, IRubyObject rubyName) {
        String name = rubyName.asJavaString();

        return getRubyMethod(name);
    }

    @JRubyMethod(backtrace = true)
    public IRubyObject java_method(ThreadContext context, IRubyObject rubyName, IRubyObject argTypes) {
        String name = rubyName.asJavaString();
        RubyArray argTypesAry = argTypes.convertToArray();
        Class[] argTypesClasses = (Class[])argTypesAry.toArray(new Class[argTypesAry.size()]);

        return getRubyMethod(name, argTypesClasses);
    }

    private Method getMethod(String name, Class... argTypes) {
        Class jclass = getJavaObject().getJavaClass();
        try {
            return jclass.getMethod(name, argTypes);
        } catch (NoSuchMethodException nsme) {
            throw JavaMethod.newMethodNotFoundError(getRuntime(), jclass, name + CodegenUtils.prettyParams(argTypes), name);
        }
    }

    private MethodInvoker getMethodInvoker(Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            return new StaticMethodInvoker(metaClass.getMetaClass(), method);
        } else {
            return new InstanceMethodInvoker(metaClass, method);
        }
    }

    private RubyMethod getRubyMethod(String name, Class... argTypes) {
        Method jmethod = getMethod(name, argTypes);
        if (Modifier.isStatic(jmethod.getModifiers())) {
            return RubyMethod.newMethod(metaClass.getSingletonClass(), CodegenUtils.prettyParams(argTypes), metaClass.getSingletonClass(), name, getMethodInvoker(jmethod), getMetaClass());
        } else {
            return RubyMethod.newMethod(metaClass, CodegenUtils.prettyParams(argTypes), metaClass, name, getMethodInvoker(jmethod), this);
        }
    }

    @Override
    public Object toJava(Class type) {
        getRuntime().getJavaSupport().getObjectProxyCache().put(getObject(), this);
        return getObject();
    }
    
    public Object unwrap() {
        return getObject();
    }
}
