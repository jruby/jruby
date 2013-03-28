package org.jruby.java.proxies;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyHash.Visitor;
import org.jruby.RubyInteger;
import org.jruby.RubyMethod;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.common.IRubyWarnings;
import org.jruby.java.invokers.InstanceFieldGetter;
import org.jruby.java.invokers.InstanceFieldSetter;
import org.jruby.java.invokers.InstanceMethodInvoker;
import org.jruby.java.invokers.MethodInvoker;
import org.jruby.java.invokers.StaticFieldGetter;
import org.jruby.java.invokers.StaticFieldSetter;
import org.jruby.java.invokers.StaticMethodInvoker;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaMethod;
import org.jruby.javasupport.JavaObject;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.CodegenUtils;
import org.jruby.util.JRubyObjectInputStream;

public class JavaProxy extends RubyObject {
    private static final boolean DEBUG = false;
    private JavaObject javaObject;
    protected Object object;
    
    public JavaProxy(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }

    public JavaProxy(Ruby runtime, RubyClass klazz, Object object) {
        super(runtime, klazz);
        this.object = object;
    }

    @Override
    public Object dataGetStruct() {
        // for investigating and eliminating code that causes JavaObject to live
        if (DEBUG) Thread.dumpStack();
        lazyJavaObject();
        return javaObject;
    }

    @Override
    public void dataWrapStruct(Object object) {
        this.javaObject = (JavaObject)object;
        this.object = javaObject.getValue();
    }

    public Object getObject() {
        // FIXME: Added this because marshal_spec seemed to reconstitute objects without calling dataWrapStruct
        // this resulted in object being null after unmarshalling...
        if (object == null) {
            if (javaObject == null) {
                throw getRuntime().newRuntimeError("Java wrapper with no contents: " + this.getMetaClass().getName());
            } else {
                object = javaObject.getValue();
            }
        }
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    private void lazyJavaObject() {
        if (javaObject == null) {
            javaObject = JavaObject.wrap(getRuntime(), object);
        }
    }

    @Override
    public Class getJavaClass() {
        return object.getClass();
    }
    
    public static RubyClass createJavaProxy(ThreadContext context) {
        Ruby runtime = context.runtime;
        
        RubyClass javaProxy = runtime.defineClass("JavaProxy", runtime.getObject(), new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new JavaProxy(runtime, klazz);
            }
        });
        
        RubyClass singleton = javaProxy.getSingletonClass();
        
        singleton.addReadWriteAttribute(context, "java_class");
        
        javaProxy.defineAnnotatedMethods(JavaProxy.class);
        javaProxy.includeModule(runtime.getModule("JavaProxyMethods"));
        
        return javaProxy;
    }

    // framed for invokeSuper
    @JRubyMethod(frame = true, meta = true)
    public static IRubyObject inherited(ThreadContext context, IRubyObject recv, IRubyObject subclass) {
        IRubyObject subJavaClass = Helpers.invoke(context, subclass, "java_class");
        if (subJavaClass.isNil()) {
            subJavaClass = Helpers.invoke(context, recv, "java_class");
            Helpers.invoke(context, subclass, "java_class=", subJavaClass);
        }
        return Helpers.invokeSuper(context, recv, subclass, Block.NULL_BLOCK);
    }
    
    @JRubyMethod(meta = true)
    public static IRubyObject singleton_class(IRubyObject recv) {
        return ((RubyClass)recv).getSingletonClass();
    }
    
    @JRubyMethod(name = "[]", meta = true, rest = true)
    public static IRubyObject op_aref(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject javaClass = Helpers.invoke(context, recv, "java_class");
        if (args.length > 0) {
            // construct new array proxy (ArrayJavaProxy)
            ArrayJavaProxyCreator ajpc = new ArrayJavaProxyCreator(context.runtime);
            ajpc.setup(context, javaClass, args);
            return ajpc;
        } else {
            return Java.get_proxy_class(javaClass, Helpers.invoke(context, javaClass, "array_class"));
        }
    }
    
    @JRubyMethod(meta = true)
    public static IRubyObject new_array(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
        JavaClass javaClass = (JavaClass) Helpers.invoke(context, recv, "java_class");
        RubyClass proxyClass = (RubyClass)recv;
        Class componentType = javaClass.javaClass();
        
        // construct new array proxy (ArrayJavaProxy)
        return new ArrayJavaProxy(context.runtime, proxyClass, Array.newInstance(componentType, (int)((RubyInteger)arg0.convertToInteger()).getLongValue()));
    }

    @JRubyMethod(name = "__persistent__=", meta = true)
    public static IRubyObject persistent(IRubyObject cls, IRubyObject value) {
        ((RubyClass)cls).getRealClass().setCacheProxy(value.isTrue());
        return value;
    }

    @JRubyMethod(name = "__persistent__", meta = true)
    public static IRubyObject persistent(ThreadContext context, IRubyObject cls) {
        return context.runtime.newBoolean(((RubyClass)cls).getRealClass().getCacheProxy());
    }

    @Override
    public IRubyObject initialize_copy(IRubyObject original) {
        super.initialize_copy(original);
        // because we lazily init JavaObject in the data-wrapped slot, explicitly copy over the object
        setObject(((JavaProxy)original).object);
        return this;
    }

    private static Class<?> getJavaClass(ThreadContext context, RubyModule module) {
        try {
        IRubyObject jClass = Helpers.invoke(context, module, "java_class");


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
                    throw context.runtime.newSecurityError("Cannot change accessibility on fields in a restricted mode: field '" + field.getName() + "'");
                }
                
                String asName = entry.getValue();

                if (Modifier.isStatic(field.getModifiers())) {
                    if (asReader) module.getSingletonClass().addMethod(asName, new StaticFieldGetter(key, module, field));
                    if (asWriter) {
                        if (isFinal) {
                            throw context.runtime.newSecurityError("Cannot change final field '" + field.getName() + "'");
                        }
                        module.getSingletonClass().addMethod(asName + "=", new StaticFieldSetter(key, module, field));
                    }
                } else {
                    if (asReader) module.addMethod(asName, new InstanceFieldGetter(key, module, field));
                    if (asWriter) {
                        if (isFinal) {
                            throw context.runtime.newSecurityError("Cannot change final field '" + field.getName() + "'");
                        }
                        module.addMethod(asName + "=", new InstanceFieldSetter(key, module, field));
                    }
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
            throw JavaClass.undefinedFieldError(context.runtime,
                    topModule.getName(), fieldMap.keySet().iterator().next());
        }

    }
    
    @JRubyMethod(meta = true, rest = true)
    public static IRubyObject field_accessor(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        findFields(context, (RubyModule) recv, args, true, true);

        return context.runtime.getNil();
    }

    @JRubyMethod(meta = true, rest = true)
    public static IRubyObject field_reader(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        findFields(context, (RubyModule) recv, args, true, false);

        return context.runtime.getNil();
    }
    
    @JRubyMethod(meta = true, rest = true)
    public static IRubyObject field_writer(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        findFields(context, (RubyModule) recv, args, false, true);

        return context.runtime.getNil();
    }

    @JRubyMethod(name = "equal?")
    public IRubyObject equal_p(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;
        if (other instanceof JavaProxy) {
            boolean equal = getObject() == ((JavaProxy)other).getObject();
            return runtime.newBoolean(equal);
        }
        return runtime.getFalse();
    }

    @JRubyMethod
    public IRubyObject java_send(ThreadContext context, IRubyObject rubyName) {
        String name = rubyName.asJavaString();
        Ruby runtime = context.runtime;
        
        JavaMethod method = new JavaMethod(runtime, getMethod(name));
        return method.invokeDirect(getObject());
    }

    @JRubyMethod
    public IRubyObject java_send(ThreadContext context, IRubyObject rubyName, IRubyObject argTypes) {
        String name = rubyName.asJavaString();
        RubyArray argTypesAry = argTypes.convertToArray();
        Ruby runtime = context.runtime;

        if (argTypesAry.size() != 0) {
            Class[] argTypesClasses = (Class[])argTypesAry.toArray(new Class[argTypesAry.size()]);
            throw JavaMethod.newArgSizeMismatchError(runtime, argTypesClasses);
        }

        JavaMethod method = new JavaMethod(runtime, getMethod(name));
        return method.invokeDirect(getObject());
    }

    @JRubyMethod
    public IRubyObject java_send(ThreadContext context, IRubyObject rubyName, IRubyObject argTypes, IRubyObject arg0) {
        String name = rubyName.asJavaString();
        RubyArray argTypesAry = argTypes.convertToArray();
        Ruby runtime = context.runtime;

        if (argTypesAry.size() != 1) {
            Class[] argTypesClasses = (Class[])argTypesAry.toArray(new Class[argTypesAry.size()]);
            throw JavaMethod.newArgSizeMismatchError(runtime, argTypesClasses);
        }

        Class argTypeClass = (Class)argTypesAry.eltInternal(0).toJava(Class.class);

        JavaMethod method = new JavaMethod(runtime, getMethod(name, argTypeClass));
        return method.invokeDirect(getObject(), arg0.toJava(argTypeClass));
    }

    @JRubyMethod(required = 4, rest = true)
    public IRubyObject java_send(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        
        String name = args[0].asJavaString();
        RubyArray argTypesAry = args[1].convertToArray();
        int argsLen = args.length - 2;

        if (argTypesAry.size() != argsLen) {
            Class[] argTypesClasses = (Class[])argTypesAry.toArray(new Class[argTypesAry.size()]);
            throw JavaMethod.newArgSizeMismatchError(runtime, argTypesClasses);
        }

        Class[] argTypesClasses = (Class[])argTypesAry.toArray(new Class[argsLen]);

        Object[] argsAry = new Object[argsLen];
        for (int i = 0; i < argsLen; i++) {
            argsAry[i] = args[i + 2].toJava(argTypesClasses[i]);
        }

        JavaMethod method = new JavaMethod(runtime, getMethod(name, argTypesClasses));
        return method.invokeDirect(getObject(), argsAry);
    }

    @JRubyMethod
    public IRubyObject java_method(ThreadContext context, IRubyObject rubyName) {
        String name = rubyName.asJavaString();

        return getRubyMethod(name);
    }

    @JRubyMethod
    public IRubyObject java_method(ThreadContext context, IRubyObject rubyName, IRubyObject argTypes) {
        String name = rubyName.asJavaString();
        RubyArray argTypesAry = argTypes.convertToArray();
        Class[] argTypesClasses = (Class[])argTypesAry.toArray(new Class[argTypesAry.size()]);

        return getRubyMethod(name, argTypesClasses);
    }

    @JRubyMethod
    public IRubyObject marshal_dump() {
        if (Serializable.class.isAssignableFrom(object.getClass())) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);

                oos.writeObject(object);

                return getRuntime().newString(new ByteList(baos.toByteArray()));
            } catch (IOException ioe) {
                throw getRuntime().newTypeError("Java type is not serializable: " + ioe.getMessage());
            }
        } else {
            throw getRuntime().newTypeError("Java type is not serializable, cannot be marshaled " + getJavaClass());
        }
    }

    @JRubyMethod
    public IRubyObject marshal_load(ThreadContext context, IRubyObject str) {
        try {
            ByteList byteList = str.convertToString().getByteList();
            ByteArrayInputStream bais = new ByteArrayInputStream(byteList.getUnsafeBytes(), byteList.getBegin(), byteList.getRealSize());
            ObjectInputStream ois = new JRubyObjectInputStream(context.runtime, bais);

            object = ois.readObject();

            return this;
        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        } catch (ClassNotFoundException cnfe) {
            throw context.runtime.newTypeError("Class not found unmarshaling Java type: " + cnfe.getLocalizedMessage());
        }
    }

    /**
     * We override RubyBasicObject.inspectHashCode to be the identity hash of
     * the contained object, so it remains consistent across wrappers.
     *
     * @return The identity hashcode of the wrapped object
     */
    @Override
    protected int inspectHashCode() {
        return System.identityHashCode(object);
    }

    private Method getMethod(String name, Class... argTypes) {
        try {
            return getObject().getClass().getMethod(name, argTypes);
        } catch (NoSuchMethodException nsme) {
            throw JavaMethod.newMethodNotFoundError(getRuntime(), getObject().getClass(), name + CodegenUtils.prettyParams(argTypes), name);
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
        Object obj = getObject();
        Class cls = obj.getClass();
        
        if (type.isAssignableFrom(cls)) {
            return obj;
        }
        
        throw getRuntime().newTypeError("failed to coerce " + cls.getName() + " to " + type.getName());
    }

    @Override
    public Object getVariable(int index) {
        confirmCachedProxy(NONPERSISTENT_IVAR_MESSAGE);
        return super.getVariable(index);
    }

    @Override
    public void setVariable(int index, Object value) {
        confirmCachedProxy(NONPERSISTENT_IVAR_MESSAGE);
        super.setVariable(index, value);
    }

    /** rb_singleton_class
     *
     * Note: this method is specialized for RubyFixnum, RubySymbol,
     * RubyNil and RubyBoolean
     *
     * Will either return the existing singleton class for this
     * object, or create a new one and return that.
     */
    @Override
    public RubyClass getSingletonClass() {
        confirmCachedProxy(NONPERSISTENT_SINGLETON_MESSAGE);
        return super.getSingletonClass();
    }

    private void confirmCachedProxy(String message) {
        RubyClass realClass = metaClass.getRealClass();
        if (!realClass.getCacheProxy()) {
            if (Java.OBJECT_PROXY_CACHE) {
                getRuntime().getWarnings().warnOnce(IRubyWarnings.ID.NON_PERSISTENT_JAVA_PROXY, MessageFormat.format(message, realClass));
            } else {
                getRuntime().getWarnings().warn(MessageFormat.format(message, realClass));
                realClass.setCacheProxy(true);
                getRuntime().getJavaSupport().getObjectProxyCache().put(getObject(), this);
            }
        }
    }
    
    public Object unwrap() {
        return getObject();
    }

    private static final String NONPERSISTENT_IVAR_MESSAGE = "instance vars on non-persistent Java type {0} (http://wiki.jruby.org/Persistence)";
    private static final String NONPERSISTENT_SINGLETON_MESSAGE = "singleton on non-persistent Java type {0} (http://wiki.jruby.org/Persistence)";
}
