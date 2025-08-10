package org.jruby.java.proxies;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

import org.jruby.AbstractRubyMethod;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyMethod;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyUnboundMethod;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.Access;
import org.jruby.api.Convert;
import org.jruby.common.IRubyWarnings;
import org.jruby.exceptions.RaiseException;
import org.jruby.java.invokers.ConstructorInvoker;
import org.jruby.java.invokers.InstanceFieldGetter;
import org.jruby.java.invokers.InstanceFieldSetter;
import org.jruby.java.invokers.InstanceMethodInvoker;
import org.jruby.java.invokers.MethodInvoker;
import org.jruby.java.invokers.RubyToJavaInvoker;
import org.jruby.java.invokers.StaticFieldGetter;
import org.jruby.java.invokers.StaticFieldSetter;
import org.jruby.java.invokers.StaticMethodInvoker;
import org.jruby.java.util.ClassUtils;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaMethod;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.binding.MethodGatherer;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.InternalVariables;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.util.ByteList;
import org.jruby.util.CodegenUtils;
import org.jruby.util.JRubyObjectInputStream;

import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.castAsModule;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Create.newEmptyArray;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.nameError;
import static org.jruby.api.Error.typeError;
import static org.jruby.api.Warn.warn;
import static org.jruby.runtime.Helpers.arrayOf;

public class JavaProxy extends RubyObject {

    Object object;

    public JavaProxy(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }

    public JavaProxy(Ruby runtime, RubyClass klazz, Object object) {
        super(runtime, klazz);
        this.object = object;
    }

    public static RubyClass createJavaProxy(ThreadContext context, RubyClass Object, RubyModule JavaProxyMethods) {
        return defineClass(context, "JavaProxy", Object, JavaProxy::new).
                defineMethods(context, JavaProxy.class).
                include(context, JavaProxyMethods);
    }

    @Deprecated // Java::JavaObject compatibility
    @JRubyMethod(meta = true)
    public static IRubyObject wrap(final ThreadContext context, final IRubyObject self, final IRubyObject object) {
        final Object value = JavaUtil.unwrapJava(object, null);

        if (value == null) return context.nil;
        if (value instanceof Class clazz) return Java.getProxyClass(context, clazz);

        return value.getClass().isArray() ?
                new ArrayJavaProxy(context.runtime, Access.getClass(context, "ArrayJavaProxy"), value) :
                new ConcreteJavaProxy(context.runtime, Access.getClass(context, "ConcreteJavaProxy"), value);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject java_class(final IRubyObject self) {
        return getJavaClass((RubyClass) self);
    }

    //public static boolean isJavaProxy(final RubyClass target) {
    //    return target.getRealClass().hasInternalVariable("java_class");
    //}

    /**
     * @param target (Java) proxy module/class
     * @return a java.lang.Class instance proxy (e.g. a java.lang.Integer.class wrapper)
     */
    public static IRubyObject getJavaClass(final RubyModule target) {
        return (JavaProxy) target.getInternalVariable("java_class");
    }

    public static void setJavaClass(final RubyClass target, final Class<?> javaClass) {
        setJavaClass(target.getClassRuntime(), target, javaClass);
    }

    @Deprecated(since = "10.0")
    public static void setJavaClass(final IRubyObject target, final Class<?> javaClass) {
        setJavaClass(((RubyBasicObject) target).getCurrentContext(), target, javaClass);
    }

    public static void setJavaClass(ThreadContext context, final IRubyObject target, final Class<?> javaClass) {
        setJavaClass(context.runtime, target.getInternalVariables(), javaClass);
    }

    static void setJavaClass(final Ruby runtime, final InternalVariables target, final Class<?> javaClass) {
        setJavaClass(target, Java.getInstance(runtime, javaClass));
    }

    private static void setJavaClass(final InternalVariables target, final IRubyObject javaClass) {
        target.setInternalVariable("java_class", javaClass);
    }

    @Override
    public final Object dataGetStruct() {
        return object;
    }

    @Override
    @SuppressWarnings("deprecation")
    public final void dataWrapStruct(Object object) {
        if (object instanceof org.jruby.javasupport.JavaObject) {
            this.object = ((org.jruby.javasupport.JavaObject) object).getValue();
        } else if (object instanceof JavaProxy) {
            this.object = ((JavaProxy) object).object;
        } else {
            this.object = object;
        }
    }

    public final Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public Object unwrap() { return getObject(); }

    @Deprecated // not used
    @SuppressWarnings("deprecation")
    protected org.jruby.javasupport.JavaObject asJavaObject(final Object object) {
        return org.jruby.javasupport.JavaObject.wrap(getRuntime(), object);
    }

    @Override
    public Class<?> getJavaClass() {
        return getObject().getClass();
    }

    @JRubyMethod(meta = true, frame = true) // framed for invokeSuper
    public static IRubyObject inherited(ThreadContext context, IRubyObject recv, IRubyObject subclass) {
        if (getJavaClass((RubyClass) subclass) == null) {
            setJavaClass((RubyClass) subclass, getJavaClass((RubyClass) recv));
        }
        return Helpers.invokeSuper(context, recv, subclass, Block.NULL_BLOCK);
    }

    @Deprecated(since = "10.0")
    public static RubyClass singleton_class(final IRubyObject self) {
        return singleton_class(self.getRuntime().getCurrentContext(), self);
    }

    @JRubyMethod(meta = true)
    public static RubyClass singleton_class(ThreadContext context, final IRubyObject self) {
        return self.singletonClass(context);
    }

    @JRubyMethod(name = "[]", meta = true, rest = true)
    public static IRubyObject op_aref(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        final Class<?> type = JavaUtil.getJavaClass(context, (RubyModule) self);
        if ( args.length > 0 ) { // construct new array proxy (ArrayJavaProxy)
            return new ArrayJavaProxyCreator(context, type, args); // e.g. Byte[64]
        }
        final Class<?> arrayType = Array.newInstance(type, 0).getClass();
        return Java.getProxyClass(context, arrayType);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject new_array(ThreadContext context, IRubyObject self, IRubyObject len) {
        final Class<?> componentType = JavaUtil.getJavaClass(context, (RubyModule) self);
        final int length = toInt(context, len);
        return ArrayJavaProxy.newArray(context, componentType, length);
    }

    @JRubyMethod(name = "__persistent__=", meta = true)
    public static IRubyObject persistent(final IRubyObject clazz, final IRubyObject value) {
        ((RubyClass) clazz).getRealClass().setCacheProxy( value.isTrue() );
        return value;
    }

    @JRubyMethod(name = "__persistent__", meta = true)
    public static IRubyObject persistent(final ThreadContext context, final IRubyObject clazz) {
        return asBoolean(context, ((RubyClass) clazz).getRealClass().getCacheProxy());
    }

    @Override
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject original) {
        super.initialize_copy(context, original);
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
    private static Map<String, String> getFieldListFromArgs(ThreadContext context, IRubyObject[] args) {
        final HashMap<String, String> map = new HashMap<>(args.length, 1);
        // Get map of all fields we want to define.
        for (int i = 0; i < args.length; i++) {
            final IRubyObject arg = args[i];
            if ( arg instanceof RubyHash ) {
                ((RubyHash) arg).visitAll(context, MapPopulatorVisitor, map);
            } else {
                String value = arg.asString().toString();
                map.put(value, value);
            }
        }
        return map;
    }

    private static final RubyHash.VisitorWithState<Map> MapPopulatorVisitor = new RubyHash.VisitorWithState<Map>() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, Map map) {
            map.put(key.asString().toString(), value.asString().toString());
        }
    };

    // Look through all mappings to find a match entry for this field
    private static void installField(final ThreadContext context,
        final Map<String, String> fieldMap, final Field field,
        final RubyModule module, boolean asReader, boolean asWriter) {

        final String fieldName = field.getName();

        final Iterator<Map.Entry<String,String>> iter = fieldMap.entrySet().iterator();
        while ( iter.hasNext() ) {
            final Map.Entry<String,String> entry = iter.next();
            if ( entry.getKey().equals( fieldName ) ) {

                installField(context, entry.getValue(), field, module, asReader, asWriter);

                iter.remove(); break;
            }
        }
    }

    private static void installField(final ThreadContext context,
        final String asName, final Field field, final RubyModule target,
        boolean asReader, Boolean asWriter) {

        if ( Ruby.isSecurityRestricted() && ! Modifier.isPublic(field.getModifiers()) ) {
            throw context.runtime.newSecurityError("Cannot change accessibility on field in restricted mode  '" + field + "'");
        }

        final String fieldName = field.getName();

        if ( Modifier.isStatic(field.getModifiers()) ) {
            if ( asReader ) {
                target.singletonClass(context).addMethod(context, asName, new StaticFieldGetter(fieldName, target, field));
            }
            if ( asWriter == null || asWriter ) {
                if ( Modifier.isFinal(field.getModifiers()) ) {
                    if ( asWriter == null ) return;
                    // e.g. Cannot change final field 'private final char[] java.lang.String.value'
                    throw context.runtime.newSecurityError("Cannot change final field '" + field + "'");
                }
                target.singletonClass(context).addMethod(context, asName + '=', new StaticFieldSetter(fieldName, target, field));
            }
        } else {
            if ( asReader ) {
                target.addMethod(context, asName, new InstanceFieldGetter(fieldName, target, field));
            }
            if ( asWriter == null || asWriter ) {
                if ( Modifier.isFinal(field.getModifiers()) ) {
                    if ( asWriter == null ) return;
                    throw context.runtime.newSecurityError("Cannot change final field '" + field + "'");
                }
                target.addMethod(context, asName + '=', new InstanceFieldSetter(fieldName, target, field));
            }
        }
    }

    public static void installField(final ThreadContext context,
        final String asName, final Field field, final RubyModule target) {
        installField(context, asName, field, target, true, null);
    }

    private static void findFields(final ThreadContext context,
        final RubyModule topModule, final IRubyObject[] args,
        final boolean asReader, final boolean asWriter) {
        final Map<String, String> fieldMap = getFieldListFromArgs(context, args);

        for (RubyModule module = topModule; module != null; module = module.getSuperClass()) {
            final Class<?> javaClass = JavaUtil.getJavaClass(module, null);
            // Hit a non-java proxy class (included Modules can be a cause of this...skip)
            if ( javaClass == null ) continue;

            Field[] fields = ClassUtils.getDeclaredFields(javaClass);
            for (int j = 0; j < fields.length; j++) {
                installField(context, fieldMap, fields[j], module, asReader, asWriter);
            }
        }

        // We could not find all of them print out first one (we could print them all?)
        if (!fieldMap.isEmpty()) {
            throw undefinedFieldError(context, topModule.getName(context), fieldMap.keySet().iterator().next());
        }

    }

    private static RaiseException undefinedFieldError(ThreadContext context, String javaClassName, String name) {
        return nameError(context, "undefined field '" + name + "' for class '" + javaClassName + "'", name);
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
        return other instanceof JavaProxy proxy ? asBoolean(context, getObject() == proxy.getObject()) : context.fals;
    }

    @JRubyMethod
    public IRubyObject java_send(ThreadContext context, IRubyObject rubyName) {
        String name = rubyName.asJavaString();
        Ruby runtime = context.runtime;

        org.jruby.javasupport.JavaMethod method = new org.jruby.javasupport.JavaMethod(runtime, getMethod(context, name));
        return method.invokeDirect(context, getObject());
    }

    @JRubyMethod
    public IRubyObject java_send(ThreadContext context, IRubyObject rubyName, IRubyObject argTypes) {
        String name = rubyName.asJavaString();
        RubyArray argTypesAry = argTypes.convertToArray();
        Ruby runtime = context.runtime;

        checkArgSizeMismatch(runtime, 0, argTypesAry);

        org.jruby.javasupport.JavaMethod method = new org.jruby.javasupport.JavaMethod(runtime, getMethod(context, name));
        return method.invokeDirect(context, getObject());
    }

    @JRubyMethod
    public IRubyObject java_send(ThreadContext context, IRubyObject rubyName, IRubyObject argTypes, IRubyObject arg0) {
        String name = rubyName.asJavaString();
        RubyArray argTypesAry = argTypes.convertToArray();
        Ruby runtime = context.runtime;

        checkArgSizeMismatch(runtime, 1, argTypesAry);

        Class argTypeClass = argTypesAry.eltInternal(0).toJava(Class.class);

        JavaMethod method = new JavaMethod(runtime, getMethod(context, name, argTypeClass));
        return method.invokeDirect(context, getObject(), arg0.toJava(argTypeClass));
    }

    @JRubyMethod(required = 1, rest = true, checkArity = false)
    public IRubyObject java_send(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 1, -1);

        Ruby runtime = context.runtime;

        String name = args[0].asJavaString();
        RubyArray argTypesAry = args[1].convertToArray();
        final int argsLen = argc - 2;

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

    @Deprecated(since = "10.0")
    public IRubyObject marshal_dump() {
        return marshal_dump(getCurrentContext());
    }

    @JRubyMethod
    public IRubyObject marshal_dump(ThreadContext context) {
        if (!Serializable.class.isAssignableFrom(object.getClass())) {
            throw typeError(context, "Java type is not serializable, cannot be marshaled " + getJavaClass());
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            new ObjectOutputStream(bytes).writeObject(object);
            return newString(context, new ByteList(bytes.toByteArray(), false));
        } catch (IOException ex) {
            throw typeError(context, "Java type is not serializable: " + ex.getMessage());
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
            throw typeError(context, "Class not found unmarshaling Java type: " + ex.getLocalizedMessage());
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
        Class<?> originalClass = getObject().getClass();
        Method[] holder = {null};

        Predicate<Method[]> predicate = (classMethods) -> {
            for (Method method : classMethods) {
                if (method.getName().equals(name) && Arrays.equals(method.getParameterTypes(), argTypes)) {
                    holder[0] = method;
                    return false;
                }
            }

            return true;
        };

        MethodGatherer.eachAccessibleMethod(originalClass, predicate, predicate);

        if (holder[0] == null) {
            throw JavaMethod.newMethodNotFoundError(context, originalClass, name + CodegenUtils.prettyParams(argTypes), name);
        }

        return holder[0];
    }

    private MethodInvoker getMethodInvoker(Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            return new StaticMethodInvoker(metaClass.getMetaClass(), () -> arrayOf(method), method.getName());
        } else {
            return new InstanceMethodInvoker(metaClass, () -> arrayOf(method), method.getName());
        }
    }

    private RubyMethod getRubyMethod(ThreadContext context, String name, Class... argTypes) {
        Method jmethod = getMethod(context, name, argTypes);
        RubyClass sourceModule;

        if (Modifier.isStatic(jmethod.getModifiers())) {
            sourceModule = metaClass.singletonClass(context);
            return RubyMethod.newMethod(
                    sourceModule,
                    CodegenUtils.prettyParams(argTypes).toString(),
                    sourceModule,
                    name,
                    new CacheEntry(getMethodInvoker(jmethod), sourceModule, metaClass.getGeneration()),
                    getMetaClass());
        } else {
            sourceModule = metaClass;
            return RubyMethod.newMethod(
                    sourceModule,
                    CodegenUtils.prettyParams(argTypes).toString(),
                    sourceModule,
                    name,
                    new CacheEntry(getMethodInvoker(jmethod), sourceModule, metaClass.getGeneration()),
                    this);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T toJava(Class<T> type) {
        final Object object = getObject();
        final Class<?> clazz = object.getClass();

        if ( type.isAssignableFrom(clazz) ) return type.cast(object);
        if ( type.isAssignableFrom(getClass()) ) return type.cast(this); // e.g. IRubyObject.class

        throw typeError(getRuntime().getCurrentContext(), "failed to coerce " + clazz.getName() + " to " + type.getName());
    }

    @Override
    public Object getVariable(int index) {
        checkVariablesOnProxy();
        return super.getVariable(index);
    }

    @Override
    public void setVariable(int index, Object value) {
        checkVariablesOnProxy();
        super.setVariable(index, value);
    }

    public void checkVariablesOnProxy() {
        confirmCachedProxy(NONPERSISTENT_IVAR_MESSAGE);
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
    public RubyClass singletonClass(ThreadContext context) {
        confirmCachedProxy(NONPERSISTENT_SINGLETON_MESSAGE);
        return super.singletonClass(context);
    }

    private void confirmCachedProxy(String message) {
        final RubyClass realClass = metaClass.getRealClass();
        if ( ! realClass.getCacheProxy() ) {
            Ruby runtime = getRuntime();
            if (Java.OBJECT_PROXY_CACHE) {
                runtime.getWarnings().warnOnce(IRubyWarnings.ID.NON_PERSISTENT_JAVA_PROXY, MessageFormat.format(message, realClass));
            } else {
                warn(runtime.getCurrentContext(), MessageFormat.format(message, realClass));
                realClass.setCacheProxy(true);
                runtime.getJavaSupport().getObjectProxyCache().put(getObject(), this);
            }
        }
    }

    private static final String NONPERSISTENT_IVAR_MESSAGE = "instance vars on non-persistent Java type {0} (https://github.com/jruby/jruby/wiki/Persistence)";
    private static final String NONPERSISTENT_SINGLETON_MESSAGE = "singleton on non-persistent Java type {0} (https://github.com/jruby/jruby/wiki/Persistence)";

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

            Class argTypeClass = argTypesAry.eltInternal(0).toJava(Class.class);

            JavaMethod method = new JavaMethod(runtime, getMethodFromClass(context, recv, name, argTypeClass));
            return method.invokeStaticDirect(context, arg0.toJava(argTypeClass));
        }

        @JRubyMethod(required = 1, rest = true, checkArity = false, meta = true)
        public static IRubyObject java_send(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
            int argc = Arity.checkArgumentCount(context, args, 1, -1);

            switch (argc) {
                case 1: return java_send(context, recv, args[0]);
                case 2: return java_send(context, recv, args[0], args[1]);
                case 3: return java_send(context, recv, args[0], args[1], args[2]);
            }

            final Ruby runtime = context.runtime;

            String name = args[0].asJavaString();
            RubyArray argTypesAry = args[1].convertToArray();
            final int argsLen = argc - 2;

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
            return java_alias(context, clazz, newName, rubyName, newEmptyArray(context));
        }

        @JRubyMethod(meta = true, visibility = Visibility.PRIVATE)
        public static IRubyObject java_alias(ThreadContext context, IRubyObject klass, IRubyObject newName, IRubyObject rubyName, IRubyObject argTypes) {
            final RubyModule proxyClass = Convert.castAsModule(context, klass);

            String name = rubyName.asJavaString();
            String newNameStr = newName.asJavaString();
            Class<?>[] argTypesClasses = (Class[]) argTypes.convertToArray().toArray(ClassUtils.EMPTY_CLASS_ARRAY);

            final Class<?> clazz = JavaUtil.getJavaClass(context, proxyClass);
            final RubyToJavaInvoker invoker;
            switch (name) {
                case "<init>" :
                    final Constructor constructor = getConstructorFromClass(context, clazz, name, argTypesClasses);
                    invoker = new ConstructorInvoker(proxyClass, () -> arrayOf(constructor), newNameStr);
                    proxyClass.addMethod(context, newNameStr, invoker);

                    break;

                default :
                    final Method method = getMethodFromClass(context, clazz, name, argTypesClasses);
                    if ( Modifier.isStatic( method.getModifiers() ) ) {
                        invoker = new StaticMethodInvoker(proxyClass.getMetaClass(), () -> arrayOf(method), newNameStr);
                        proxyClass.singletonClass(context).addMethod(context, newNameStr, invoker); // add alias to meta
                    }
                    else {
                        invoker = new InstanceMethodInvoker(proxyClass, () -> arrayOf(method), newNameStr);
                        proxyClass.addMethod(context, newNameStr, invoker);
                    }
            }

            return context.nil;
        }

        private static AbstractRubyMethod getRubyMethod(ThreadContext context, IRubyObject clazz, String name, Class... argTypesClasses) {
            final RubyModule proxyClass = Convert.castAsModule(context, clazz);
            final Method method = getMethodFromClass(context, JavaUtil.getJavaClass(context, proxyClass), name, argTypesClasses);
            final String prettyName = name + CodegenUtils.prettyParams(argTypesClasses);

            if ( Modifier.isStatic( method.getModifiers() ) ) {
                MethodInvoker invoker = new StaticMethodInvoker(proxyClass, () -> arrayOf(method), name);
                return RubyMethod.newMethod(proxyClass, prettyName, proxyClass, name, new CacheEntry(invoker, proxyClass, proxyClass.getGeneration()), clazz);
            }

            MethodInvoker invoker = new InstanceMethodInvoker(proxyClass, () -> arrayOf(method), name);
            return RubyUnboundMethod.newUnboundMethod(proxyClass, prettyName, proxyClass, name, new CacheEntry(invoker, proxyClass, proxyClass.getGeneration()));
        }

        private static Method getMethodFromClass(final ThreadContext context, final IRubyObject klass, final String name, final Class... argTypes) {
            return getMethodFromClass(context, JavaUtil.getJavaClass(context, (RubyModule) klass), name, argTypes);
        }

        private static Method getMethodFromClass(final ThreadContext context, final Class<?> clazz, final String name, final Class... argTypes) {
            try {
                return clazz.getMethod(name, argTypes);
            } catch (NoSuchMethodException e) {
                throw newNameError(context, "Java method not found: " + format(clazz, name, argTypes), name, e);
            }
        }

        private static Constructor getConstructorFromClass(final ThreadContext context, final Class<?> clazz, final String name, final Class... argTypes) {
            try {
                return clazz.getConstructor(argTypes);
            } catch (NoSuchMethodException e) {
                throw newNameError(context, "Java initializer not found: " + format(clazz, name, argTypes), name, e);
            }
        }

        private static String format(final Class<?> clazz, final String name, final Class... argTypes) {
            return clazz.getName() + '.' + name + CodegenUtils.prettyParams(argTypes); // e.g. SomeClass.someMethod(java.lang.String)
        }

        private static RaiseException newNameError(ThreadContext context, final String msg, final String name,
                                                   final ReflectiveOperationException cause) {
            RaiseException ex = nameError(context, msg, name);
            ex.initCause(cause);
            return ex;
        }

    }

}
