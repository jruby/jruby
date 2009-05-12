package org.jruby.java.proxies;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyHash.Visitor;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.java.invokers.InstanceFieldGetter;
import org.jruby.java.invokers.InstanceFieldSetter;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class JavaProxy extends RubyObject {
    protected final RubyClass.VariableAccessor objectAccessor;
    public JavaProxy(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
        objectAccessor = klazz.getVariableAccessorForWrite("__wrap_struct__");
    }

    public Object dataGetStruct() {
        return objectAccessor.get(this);
    }

    public void dataWrapStruct(Object object) {
        objectAccessor.set(this, object);
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
    public static IRubyObject new_instance_for(IRubyObject recv, IRubyObject arg0) {
        return Java.new_instance_for(recv, arg0);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject to_java_object(IRubyObject recv) {
        return Java.to_java_object(recv);
    }

    @JRubyMethod(name = "equal?")
    public IRubyObject equal_p(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.getRuntime();
        if (other.dataGetStruct() instanceof JavaObject) {
            boolean equal = unwrap() == ((JavaObject)other.dataGetStruct()).getValue();
            return runtime.newBoolean(equal);
        }
        return runtime.getFalse();
    }
    
    public Object unwrap() {
        return ((JavaObject)dataGetStruct()).getValue();
    }
}
