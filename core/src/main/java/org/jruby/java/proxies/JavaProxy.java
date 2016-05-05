package org.jruby.java.proxies;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jruby.AbstractRubyMethod;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyHash.Visitor;
import org.jruby.RubyMethod;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyUnboundMethod;
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
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.CodegenUtils;
import org.jruby.util.JRubyObjectInputStream;

public class JavaProxy extends RubyObject {

    private transient JavaObject javaObject;
    Object object;

    public JavaProxy(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }

    public JavaProxy(Ruby runtime, RubyClass klazz, Object object) {
        super(runtime, klazz);
        this.object = object;
    }

    private static final ObjectAllocator ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
            return new JavaProxy(runtime, klazz);
        }
    };

    public static RubyClass createJavaProxy(ThreadContext context) {
        final Ruby runtime = context.runtime;

        RubyClass JavaProxy = runtime.defineClass("JavaProxy", runtime.getObject(), ALLOCATOR);

        JavaProxy.getSingletonClass().addReadWriteAttribute(context, "java_class");
        JavaProxy.defineAnnotatedMethods(JavaProxy.class);
        JavaProxy.includeModule(runtime.getModule("JavaProxyMethods"));

        return JavaProxy;
    }

    @Override
    public Object dataGetStruct() {
        return getJavaObject();
    }

    @Override
    public void dataWrapStruct(Object object) {
        this.javaObject = (JavaObject) object;
        this.object = javaObject.getValue();
    }

    public final Object getObject() {
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

    public Object unwrap() { return getObject(); }

    private JavaObject getJavaObject() {
        if (javaObject == null) {
            javaObject = JavaObject.wrap(getRuntime(), object);
        }
        return javaObject;
    }

    @Override
    public Class getJavaClass() {
        return getObject().getClass();
    }

    static JavaClass java_class(final ThreadContext context, final RubyModule module) {
        return (JavaClass) Helpers.invoke(context, module, "java_class");
    }

    @JRubyMethod(meta = true, frame = true) // framed for invokeSuper
    public static IRubyObject inherited(ThreadContext context, IRubyObject recv, IRubyObject subclass) {
        IRubyObject subJavaClass = Helpers.invoke(context, subclass, "java_class");
        if (subJavaClass.isNil()) {
            subJavaClass = Helpers.invoke(context, recv, "java_class");
            Helpers.invoke(context, subclass, "java_class=", subJavaClass);
        }
        return Helpers.invokeSuper(context, recv, subclass, Block.NULL_BLOCK);
    }

    @JRubyMethod(meta = true)
    public static RubyClass singleton_class(final IRubyObject self) {
        return ((RubyClass) self).getSingletonClass();
    }

    @JRubyMethod(name = "[]", meta = true, rest = true)
    public static IRubyObject op_aref(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        final JavaClass javaClass = java_class(context, (RubyModule) self);
        if ( args.length > 0 ) { // construct new array proxy (ArrayJavaProxy)
            return new ArrayJavaProxyCreator(context, javaClass, args); // e.g. Byte[64]
        }
        return Java.get_proxy_class(javaClass, Helpers.invoke(context, javaClass, "array_class"));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject new_array(ThreadContext context, IRubyObject self, IRubyObject len) {
        final JavaClass javaClass = java_class(context, (RubyModule) self);
        final Class<?> componentType = javaClass.javaClass();
        final int length = (int) len.convertToInteger().getLongValue();
        return ArrayJavaProxy.newArray(context.runtime, componentType, length);
    }

    @JRubyMethod(name = "__persistent__=", meta = true)
    public static IRubyObject persistent(final IRubyObject clazz, final IRubyObject value) {
        ((RubyClass) clazz).getRealClass().setCacheProxy( value.isTrue() );
        return value;
    }

    @JRubyMethod(name = "__persistent__", meta = true)
    public static IRubyObject persistent(final ThreadContext context, final IRubyObject clazz) {
        return context.runtime.newBoolean(((RubyClass) clazz).getRealClass().getCacheProxy());
    }

    @Override
    public IRubyObject initialize_copy(IRubyObject original) {
        super.initialize_copy(original);
        // because we lazily init JavaObject in the data-wrapped slot, explicitly copy over the object
        setObject( ((JavaProxy) original).cloneObject() );
        return this;
    }

    protected Object cloneObject() {
        final Object object = getObject();
        if (object instanceof Cloneable) {
            // sufficient for java.util collection classes e.g. HashSet, ArrayList
            Object clone = JavaUtil.clone(object);
            return clone == null ? object : clone;
        }
        return object; // this is what JRuby did prior to <= 9.0.5
    }

    /**
     * Create a name/newname map of fields to be exposed as methods.
     */
    private static Map<String, String> getFieldListFromArgs(final IRubyObject[] args) {
        final Map<String, String> map = new HashMap<String, String>();
        // Get map of all fields we want to define.
        for (int i = 0; i < args.length; i++) {
            final IRubyObject arg = args[i];
            if ( arg instanceof RubyHash ) {
                ((RubyHash) arg).visitAll(new Visitor() {
                    @Override
                    public void visit(IRubyObject key, IRubyObject value) {
                        map.put(key.asString().toString(), value.asString().toString());
                    }
                });
            } else {
                String value = arg.asString().toString();
                map.put(value, value);
            }
        }
        return map;
    }

    // Look through all mappings to find a match entry for this field
    private static void installField(final ThreadContext context,
        final Map<String, String> fieldMap, final Field field,
        final RubyModule module, boolean asReader, boolean asWriter) {

        final String fieldName = field.getName();

        final Iterator<Map.Entry<String,String>> iter = fieldMap.entrySet().iterator();
        while ( iter.hasNext() ) {
            final Map.Entry<String,String> entry = iter.next();
            if ( entry.getKey().equals( fieldName ) ) {

                if ( Ruby.isSecurityRestricted() && ! Modifier.isPublic(field.getModifiers()) ) {
                    throw context.runtime.newSecurityError("Cannot change accessibility on fields in a restricted mode: field '" + fieldName + "'");
                }

                String asName = entry.getValue();

                if ( Modifier.isStatic(field.getModifiers()) ) {
                    if ( asReader ) {
                        module.getSingletonClass().addMethod(asName, new StaticFieldGetter(fieldName, module, field));
                    }
                    if ( asWriter ) {
                        if ( Modifier.isFinal(field.getModifiers()) ) {
                            throw context.runtime.newSecurityError("Cannot change final field '" + fieldName + "'");
                        }
                        module.getSingletonClass().addMethod(asName + '=', new StaticFieldSetter(fieldName, module, field));
                    }
                } else {
                    if ( asReader ) {
                        module.addMethod(asName, new InstanceFieldGetter(fieldName, module, field));
                    }
                    if ( asWriter ) {
                        if ( Modifier.isFinal(field.getModifiers()) ) {
                            throw context.runtime.newSecurityError("Cannot change final field '" + fieldName + "'");
                        }
                        module.addMethod(asName + '=', new InstanceFieldSetter(fieldName, module, field));
                    }
                }

                iter.remove();
                break;
            }
        }
    }

    private static void findFields(final ThreadContext context,
        final RubyModule topModule, final IRubyObject[] args,
        final boolean asReader, final boolean asWriter) {
        final Map<String, String> fieldMap = getFieldListFromArgs(args);

        for (RubyModule module = topModule; module != null; module = module.getSuperClass()) {
            final Class<?> javaClass = JavaClass.getJavaClassIfProxy(context, module);
            // Hit a non-java proxy class (included Modules can be a cause of this...skip)
            if ( javaClass == null ) continue;

            Field[] fields = JavaClass.getDeclaredFields(javaClass);
            for (int j = 0; j < fields.length; j++) {
                installField(context, fieldMap, fields[j], module, asReader, asWriter);
            }
        }

        // We could not find all of them print out first one (we could print them all?)
        if ( ! fieldMap.isEmpty() ) {
            throw JavaClass.undefinedFieldError(context.runtime,
                    topModule.getName(), fieldMap.keySet().iterator().next());
        }

    }

    @JRubyMethod(meta = true, rest = true)
    public static IRubyObject field_accessor(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        findFields(context, (RubyModule) self, args, true, true);
        return context.nil;
    }

    @JRubyMethod(meta = true, rest = true)
    public static IRubyObject field_reader(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        findFields(context, (RubyModule) self, args, true, false);
        return context.nil;
    }

    @JRubyMethod(meta = true, rest = true)
    public static IRubyObject field_writer(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        findFields(context, (RubyModule) self, args, false, true);
        return context.nil;
    }

    @Override
    @JRubyMethod(name = "equal?")
    public IRubyObject equal_p(ThreadContext context, IRubyObject other) {
        if ( other instanceof JavaProxy ) {
            boolean equal = getObject() == ((JavaProxy) other).getObject();
            return context.runtime.newBoolean(equal);
        }
        return context.runtime.getFalse();
    }

    @JRubyMethod
    public IRubyObject java_send(ThreadContext context, IRubyObject rubyName) {
        String name = rubyName.asJavaString();
        Ruby runtime = context.runtime;

        JavaMethod method = new JavaMethod(runtime, getMethod(context, name));
        return method.invokeDirect(context, getObject());
    }

    @JRubyMethod
    public IRubyObject java_send(ThreadContext context, IRubyObject rubyName, IRubyObject argTypes) {
        String name = rubyName.asJavaString();
        RubyArray argTypesAry = argTypes.convertToArray();
        Ruby runtime = context.runtime;

        checkArgSizeMismatch(runtime, 0, argTypesAry);

        JavaMethod method = new JavaMethod(runtime, getMethod(context, name));
        return method.invokeDirect(context, getObject());
    }

    @JRubyMethod
    public IRubyObject java_send(ThreadContext context, IRubyObject rubyName, IRubyObject argTypes, IRubyObject arg0) {
        String name = rubyName.asJavaString();
        RubyArray argTypesAry = argTypes.convertToArray();
        Ruby runtime = context.runtime;

        checkArgSizeMismatch(runtime, 1, argTypesAry);

        Class argTypeClass = (Class) argTypesAry.eltInternal(0).toJava(Class.class);

        JavaMethod method = new JavaMethod(runtime, getMethod(context, name, argTypeClass));
        return method.invokeDirect(context, getObject(), arg0.toJava(argTypeClass));
    }

    @JRubyMethod(required = 1, rest = true)
    public IRubyObject java_send(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        String name = args[0].asJavaString();
        RubyArray argTypesAry = args[1].convertToArray();
        final int argsLen = args.length - 2;

        checkArgSizeMismatch(runtime, argsLen, argTypesAry);

        Class[] argTypesClasses = (Class[]) argTypesAry.toArray(new Class[argsLen]);

        Object[] javaArgs = new Object[argsLen];
        for ( int i = 0; i < argsLen; i++ ) {
            javaArgs[i] = args[i + 2].toJava( argTypesClasses[i] );
        }

        JavaMethod method = new JavaMethod(runtime, getMethod(context, name, argTypesClasses));
        return method.invokeDirect(context, getObject(), javaArgs);
    }

    private static void checkArgSizeMismatch(final Ruby runtime, final int expected, final RubyArray argTypes) {
        if ( argTypes.size() != expected ) {
            Class[] argTypesClasses = (Class[]) argTypes.toArray(new Class[argTypes.size()]);
            throw JavaMethod.newArgSizeMismatchError(runtime, argTypesClasses);
        }
    }

    @JRubyMethod
    public IRubyObject java_method(ThreadContext context, IRubyObject rubyName) {
        String name = rubyName.asJavaString();

        return getRubyMethod(context, name);
    }

    @JRubyMethod
    public IRubyObject java_method(ThreadContext context, IRubyObject rubyName, IRubyObject argTypes) {
        String name = rubyName.asJavaString();
        RubyArray argTypesAry = argTypes.convertToArray();
        Class[] argTypesClasses = (Class[]) argTypesAry.toArray(new Class[argTypesAry.size()]);

        return getRubyMethod(context, name, argTypesClasses);
    }

    @JRubyMethod
    public IRubyObject marshal_dump() {
        if ( ! Serializable.class.isAssignableFrom(object.getClass()) ) {
            throw getRuntime().newTypeError("Java type is not serializable, cannot be marshaled " + getJavaClass());
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            new ObjectOutputStream(bytes).writeObject(object);
            return getRuntime().newString(new ByteList(bytes.toByteArray(), false));
        }
        catch (IOException ex) {
            throw getRuntime().newTypeError("Java type is not serializable: " + ex.getMessage());
        }
    }

    @JRubyMethod
    public IRubyObject marshal_load(final ThreadContext context, final IRubyObject str) {
        try {
            ByteList byteList = str.convertToString().getByteList();
            ByteArrayInputStream bytes = new ByteArrayInputStream(byteList.getUnsafeBytes(), byteList.getBegin(), byteList.getRealSize());

            this.object = new JRubyObjectInputStream(context.runtime, bytes).readObject();

            return this;
        }
        catch (IOException ex) {
            throw context.runtime.newIOErrorFromException(ex);
        }
        catch (ClassNotFoundException ex) {
            throw context.runtime.newTypeError("Class not found unmarshaling Java type: " + ex.getLocalizedMessage());
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

    private Method getMethod(ThreadContext context, String name, Class... argTypes) {
        try {
            return getObject().getClass().getMethod(name, argTypes);
        } catch (NoSuchMethodException nsme) {
            throw JavaMethod.newMethodNotFoundError(context.runtime, getObject().getClass(), name + CodegenUtils.prettyParams(argTypes), name);
        }
    }

    private MethodInvoker getMethodInvoker(Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            return new StaticMethodInvoker(metaClass.getMetaClass(), method);
        } else {
            return new InstanceMethodInvoker(metaClass, method);
        }
    }

    private RubyMethod getRubyMethod(ThreadContext context, String name, Class... argTypes) {
        Method jmethod = getMethod(context, name, argTypes);
        if (Modifier.isStatic(jmethod.getModifiers())) {
            return RubyMethod.newMethod(metaClass.getSingletonClass(), CodegenUtils.prettyParams(argTypes).toString(), metaClass.getSingletonClass(), name, getMethodInvoker(jmethod), getMetaClass());
        } else {
            return RubyMethod.newMethod(metaClass, CodegenUtils.prettyParams(argTypes).toString(), metaClass, name, getMethodInvoker(jmethod), this);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object toJava(final Class type) {
        final Object object = getObject();
        final Class<?> clazz = object.getClass();

        if ( type.isAssignableFrom(clazz) ) return object;
        if ( type.isAssignableFrom(getClass()) ) return this; // e.g. IRubyObject.class

        throw getRuntime().newTypeError("failed to coerce " + clazz.getName() + " to " + type.getName());
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
        final RubyClass realClass = metaClass.getRealClass();
        if ( ! realClass.getCacheProxy() ) {
            final Ruby runtime = getRuntime();
            if (Java.OBJECT_PROXY_CACHE) {
                runtime.getWarnings().warnOnce(IRubyWarnings.ID.NON_PERSISTENT_JAVA_PROXY, MessageFormat.format(message, realClass));
            } else {
                runtime.getWarnings().warn(MessageFormat.format(message, realClass));
                realClass.setCacheProxy(true);
                runtime.getJavaSupport().getObjectProxyCache().put(getObject(), this);
            }
        }
    }

    private static final String NONPERSISTENT_IVAR_MESSAGE = "instance vars on non-persistent Java type {0} (http://wiki.jruby.org/Persistence)";
    private static final String NONPERSISTENT_SINGLETON_MESSAGE = "singleton on non-persistent Java type {0} (http://wiki.jruby.org/Persistence)";

    public static class ClassMethods {

        // handling non-public inner classes retrieval ... like private constants
        @JRubyMethod(name = "const_missing", required = 1, meta = true, visibility = Visibility.PRIVATE, frame = true)
        public static IRubyObject const_missing(ThreadContext context, IRubyObject self, IRubyObject name) {
            return Java.get_inner_class(context, (RubyModule) self, name);
        }

        @JRubyMethod(meta = true)
        public static IRubyObject java_method(ThreadContext context, IRubyObject proxyClass, IRubyObject rubyName) {
            String name = rubyName.asJavaString();

            return getRubyMethod(context, proxyClass, name);
        }

        @JRubyMethod(meta = true)
        public static IRubyObject java_method(ThreadContext context, IRubyObject proxyClass, IRubyObject rubyName, IRubyObject argTypes) {
            String name = rubyName.asJavaString();
            RubyArray argTypesAry = argTypes.convertToArray();
            Class[] argTypesClasses = (Class[])argTypesAry.toArray(new Class[argTypesAry.size()]);

            return getRubyMethod(context, proxyClass, name, argTypesClasses);
        }

        @JRubyMethod(meta = true)
        public static IRubyObject java_send(ThreadContext context, IRubyObject recv, IRubyObject rubyName) {
            String name = rubyName.asJavaString();
            final Ruby runtime = context.runtime;

            JavaMethod method = new JavaMethod(runtime, getMethodFromClass(context, recv, name));
            return method.invokeStaticDirect(context);
        }

        @JRubyMethod(meta = true)
        public static IRubyObject java_send(ThreadContext context, IRubyObject recv, IRubyObject rubyName, IRubyObject argTypes) {
            String name = rubyName.asJavaString();
            RubyArray argTypesAry = argTypes.convertToArray();
            final Ruby runtime = context.runtime;

            checkArgSizeMismatch(runtime, 0, argTypesAry);

            JavaMethod method = new JavaMethod(runtime, getMethodFromClass(context, recv, name));
            return method.invokeStaticDirect(context);
        }

        @JRubyMethod(meta = true)
        public static IRubyObject java_send(ThreadContext context, IRubyObject recv, IRubyObject rubyName, IRubyObject argTypes, IRubyObject arg0) {
            String name = rubyName.asJavaString();
            RubyArray argTypesAry = argTypes.convertToArray();
            final Ruby runtime = context.runtime;

            checkArgSizeMismatch(runtime, 1, argTypesAry);

            Class argTypeClass = (Class) argTypesAry.eltInternal(0).toJava(Class.class);

            JavaMethod method = new JavaMethod(runtime, getMethodFromClass(context, recv, name, argTypeClass));
            return method.invokeStaticDirect(context, arg0.toJava(argTypeClass));
        }

        @JRubyMethod(required = 1, rest = true, meta = true)
        public static IRubyObject java_send(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
            switch (args.length) {
                case 1: return java_send(context, recv, args[0]);
                case 2: return java_send(context, recv, args[0], args[1]);
                case 3: return java_send(context, recv, args[0], args[1], args[2]);
            }

            final Ruby runtime = context.runtime;

            String name = args[0].asJavaString();
            RubyArray argTypesAry = args[1].convertToArray();
            final int argsLen = args.length - 2;

            checkArgSizeMismatch(runtime, argsLen, argTypesAry);

            Class[] argTypesClasses = (Class[]) argTypesAry.toArray(new Class[argsLen]);

            Object[] javaArgs = new Object[argsLen];
            for ( int i = 0; i < argsLen; i++ ) {
                javaArgs[i] = args[i + 2].toJava( argTypesClasses[i] );
            }

            JavaMethod method = new JavaMethod(runtime, getMethodFromClass(context, recv, name, argTypesClasses));
            return method.invokeStaticDirect(context, javaArgs);
        }

        @JRubyMethod(meta = true, visibility = Visibility.PRIVATE)
        public static IRubyObject java_alias(ThreadContext context, IRubyObject clazz, IRubyObject newName, IRubyObject rubyName) {
            return java_alias(context, clazz, newName, rubyName, context.runtime.newEmptyArray());
        }

        @JRubyMethod(meta = true, visibility = Visibility.PRIVATE)
        public static IRubyObject java_alias(ThreadContext context, IRubyObject clazz, IRubyObject newName, IRubyObject rubyName, IRubyObject argTypes) {
            final Ruby runtime = context.runtime;
            if ( ! ( clazz instanceof RubyClass ) ) {
                throw runtime.newTypeError(clazz, runtime.getModule());
            }
            final RubyClass proxyClass = (RubyClass) clazz;

            String name = rubyName.asJavaString();
            String newNameStr = newName.asJavaString();
            RubyArray argTypesAry = argTypes.convertToArray();
            Class<?>[] argTypesClasses = (Class[]) argTypesAry.toArray(new Class[argTypesAry.size()]);

            final Method method = getMethodFromClass(context, clazz, name, argTypesClasses);
            final MethodInvoker invoker;

            if ( Modifier.isStatic( method.getModifiers() ) ) {
                invoker = new StaticMethodInvoker(proxyClass.getMetaClass(), method);
                // add alias to meta
                proxyClass.getSingletonClass().addMethod(newNameStr, invoker);
            }
            else {
                invoker = new InstanceMethodInvoker(proxyClass, method);
                proxyClass.addMethod(newNameStr, invoker);
            }

            return context.nil;
        }

        private static AbstractRubyMethod getRubyMethod(ThreadContext context, IRubyObject clazz, String name, Class... argTypesClasses) {
            final Ruby runtime = context.runtime;
            if ( ! ( clazz instanceof RubyModule ) ) {
                throw runtime.newTypeError(clazz, runtime.getModule());
            }
            final RubyModule proxyClass = (RubyModule) clazz;

            final Method method = getMethodFromClass(context, clazz, name, argTypesClasses);
            final String prettyName = name + CodegenUtils.prettyParams(argTypesClasses);

            if ( Modifier.isStatic( method.getModifiers() ) ) {
                MethodInvoker invoker = new StaticMethodInvoker(proxyClass, method);
                return RubyMethod.newMethod(proxyClass, prettyName, proxyClass, name, invoker, clazz);
            }

            MethodInvoker invoker = new InstanceMethodInvoker(proxyClass, method);
            return RubyUnboundMethod.newUnboundMethod(proxyClass, prettyName, proxyClass, name, invoker);
        }

        private static Method getMethodFromClass(final ThreadContext context, final IRubyObject proxyClass,
            final String name, final Class... argTypes) {
            final Class<?> clazz = JavaClass.getJavaClass(context, (RubyModule) proxyClass);
            try {
                return clazz.getMethod(name, argTypes);
            }
            catch (NoSuchMethodException nsme) {
                String prettyName = name + CodegenUtils.prettyParams(argTypes);
                String errorName = clazz.getName() + '.' + prettyName;
                throw context.runtime.newNameError("Java method not found: " + errorName, name);
            }
        }

    }

}
