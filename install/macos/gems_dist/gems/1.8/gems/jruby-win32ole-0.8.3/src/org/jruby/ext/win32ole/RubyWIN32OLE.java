package org.jruby.ext.win32ole;

import org.racob.com.Dispatch;
import org.racob.com.EnumVariant;
import org.racob.com.Variant;
import java.util.Calendar;
import java.util.Date;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyInteger;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.racob.com.SafeArray;
import win32ole.Win32oleService;

/**
 */
public class RubyWIN32OLE extends RubyObject {
    private static final Object[] EMPTY_OBJECT_ARGS = new Object[0];
    private static final int[] EMPTY_ERROR_ARGS = new int[0];
    
    public static ObjectAllocator WIN32OLE_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyWIN32OLE(runtime, klass);
        }
    };
    
    public Dispatch dispatch = null;

    public RubyWIN32OLE(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    private RubyWIN32OLE(Ruby runtime, RubyClass metaClass, Dispatch dispatch) {
        this(runtime, metaClass);

        this.dispatch = dispatch;
    }

    public Dispatch getDispatch() {
        return dispatch;
    }

    // Accessor for Ruby side of win32ole to get ahold of this object
    @JRubyMethod()
    public IRubyObject dispatch(ThreadContext context) {
        return JavaUtil.convertJavaToUsableRubyObject(context.getRuntime(), dispatch);
    }

    @JRubyMethod()
    public IRubyObject each(ThreadContext context, Block block) {
        Ruby runtime = context.getRuntime();
        EnumVariant enumVariant = dispatch.toEnumVariant();

        // FIXME: when no block is passed handling
        
        while (enumVariant.hasMoreElements()) {
            Variant value = enumVariant.nextElement();
            block.yield(context, fromVariant(runtime, value));
        }
	enumVariant.safeRelease();

        return runtime.getNil();
    }

    @JRubyMethod(required = 3)
    public IRubyObject _getproperty(ThreadContext context, IRubyObject dispid,
            IRubyObject args, IRubyObject argTypes) {
        Object[] objectArgs = makeObjectArgs(args.convertToArray());
        int id = (int) RubyInteger.num2long(dispid);
        Ruby runtime = context.getRuntime();
        
        if (objectArgs.length == 0) return fromObject(runtime, dispatch.callO(id));

        return fromVariant(runtime, dispatch.call(id, objectArgs));
    }

    @JRubyMethod(required = 1, rest = true)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        String id = args[0].convertToString().asJavaString();
        String progId = toProgID(id);

        dispatch = new Dispatch(progId);

        return this;
    }

    @JRubyMethod(required = 1, rest = true)
    public IRubyObject invoke(ThreadContext context, IRubyObject[] args) {
        return method_missing(context, args);
    }
    
    @JRubyMethod()
    public IRubyObject _invoke(ThreadContext context, IRubyObject dispid,
            IRubyObject args, IRubyObject typesArray) {
        return invokeInternal(context, dispid, args, args, Dispatch.Method);
    }

    @JRubyMethod(required = 1, rest = true)
    public IRubyObject method_missing(ThreadContext context, IRubyObject[] args) {
        String methodName = args[0].asJavaString();
        
        if (methodName.endsWith("=")) return invokeSet(context, 
                methodName.substring(0, methodName.length() - 1), args);

        return invokeMethodOrGet(context, methodName, args);
    }

    @JRubyMethod()
    public IRubyObject ole_free(ThreadContext context) {
        dispatch.safeRelease();

        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = "[]", required = 1)
    public IRubyObject op_aref(ThreadContext context, IRubyObject property) {
        String propertyName = property.asJavaString();
        
        return fromVariant(context.getRuntime(), dispatch.get(propertyName));
    }

    @JRubyMethod(name = "[]=", required = 2)
    public IRubyObject op_aset(ThreadContext context, IRubyObject property, IRubyObject value) {
        String propertyName = property.asJavaString();

        dispatch.put(propertyName, toObject(value));
        
        return context.getRuntime().getNil();
    }

    @JRubyMethod()
    public IRubyObject _setproperty(ThreadContext context, IRubyObject dispid,
            IRubyObject args, IRubyObject argTypes) {
        return invokeInternal(context, dispid, args, argTypes, Dispatch.Put);
    }

    @JRubyMethod(required = 1, rest = true)
    public IRubyObject setproperty(ThreadContext context, IRubyObject[] args) {
        String methodName = args[0].asJavaString();
        
        return invokeSet(context, methodName, args);
    }

    private IRubyObject invokeSet(ThreadContext context, String methodName, IRubyObject[] args) {
        Object[] objectArgs = makeObjectArgs(args, 1);
        int[] errorArgs = new int[objectArgs.length];

        dispatch.invoke(methodName, Dispatch.Put, objectArgs, errorArgs);
        return context.getRuntime().getNil();
    }

    private IRubyObject invokeInternal(ThreadContext context, IRubyObject dispid,
            IRubyObject args, IRubyObject argTypes, int dispatchType) {
        RubyArray argsArray = args.convertToArray();
        int dispatchId = (int) RubyInteger.num2long(dispid);
        Object[] objectArgs = makeObjectArgs(argsArray);
        int[] errorArgs = makeErrorArgs(objectArgs.length);
        Variant returnValue = dispatch.invoke(dispatchId, dispatchType,
                objectArgs, errorArgs);

        return fromVariant(context.getRuntime(), returnValue);
    }
    private int[] makeErrorArgs(int size) {
        return size <= 0 ? EMPTY_ERROR_ARGS : new int[size];
    }

    private Object[] makeObjectArgs(IRubyObject[] rubyArgs, int startIndex) {
        int length = rubyArgs.length;
        if (length - startIndex <= 0) return EMPTY_OBJECT_ARGS;

        Object[] objectArgs = new Object[length - startIndex];
        for (int i = startIndex; i < length; i++) {
            objectArgs[i - startIndex] = RubyWIN32OLE.toObject(rubyArgs[i]);
        }

        return objectArgs;
    }

    private Object[] makeObjectArgs(RubyArray argsArray) {
        int length = argsArray.size();
        if (length <= 0) return EMPTY_OBJECT_ARGS;

        Object[] objectArgs = new Object[length];
        for (int i = 0; i < length; i++) {
            Object object = toObject(argsArray.eltInternal(i));
            objectArgs[i] = object;
        }

        return objectArgs;
    }

    private IRubyObject invokeMethodOrGet(ThreadContext context, String methodName, IRubyObject[] args) {
        if (args.length == 1) { // No-arg call
            return fromObject(context.getRuntime(), dispatch.callO(methodName));
        } 
        return fromVariant(context.getRuntime(),
                dispatch.callN(methodName, makeObjectArgs(args, 1)));
    }

    @Override
    public Object toJava(Class klass) {
        return dispatch;
    }

    public static Object toObject(IRubyObject rubyObject) {
        return rubyObject.toJava(Object.class);
    }

    public static IRubyObject fromObject(Ruby runtime, Object object) {
        if (object == null) return runtime.getNil();

        if (object instanceof Boolean) {
            return runtime.newBoolean(((Boolean) object).booleanValue());
        } else if (object instanceof Dispatch) {
            return new RubyWIN32OLE(runtime, Win32oleService.getMetaClass(), (Dispatch) object);
        } else if (object instanceof Date) {
            return date2ruby(runtime, (Date) object);
        } else if (object instanceof Number) {
            if (object instanceof Double) {
                return runtime.newFloat(((Double) object).doubleValue());
            } else if (object instanceof Float) {
                return runtime.newFloat(((Float) object).doubleValue());
            }

            return runtime.newFixnum(((Number) object).intValue());
        } else if (object instanceof String) {
            return runtime.newString((String) object);
        } else if (object instanceof SafeArray) {
            return listFromSafeArray(runtime, (SafeArray) object);
        }

        return JavaUtil.convertJavaToUsableRubyObject(runtime, object);
    }

    private static IRubyObject listFromSafeArray(Ruby runtime, SafeArray list) {
            RubyArray newArray = runtime.newArray();

            for (int i = 0; i < list.size(); i++) {
                Object element = list.get(i);
                IRubyObject convertedElement = null;

                if (element instanceof SafeArray) {
                    convertedElement = null; //TODO: Borked
                } else if (element instanceof Variant) {
                    convertedElement = fromVariant(runtime, (Variant) element);
                } else {
                    throw runtime.newArgumentError("Unknown element found in SafeArray: " +
                            element.getClass());
                }
                newArray.append(convertedElement);
            }

            return newArray;
    }

    public static IRubyObject fromVariant(Ruby runtime, Variant variant) {
        if (variant == null) return runtime.getNil();

        if (variant.isArray()) {
            return listFromSafeArray(runtime, (SafeArray) variant.getArray());
        }

        switch (variant.getType()) {
            case Variant.VariantBoolean:
                return runtime.newBoolean(variant.getBoolean());
            case Variant.VariantDispatch:
                return new RubyWIN32OLE(runtime, Win32oleService.getMetaClass(), variant.getDispatch());
            case Variant.VariantDate:
                return date2ruby(runtime, variant.getDate());
            case Variant.VariantInt:
            case Variant.VariantShort:
                return runtime.newFixnum(variant.getInt());
            case Variant.VariantDouble:
                return runtime.newFloat(variant.getDouble());
            case Variant.VariantFloat:
                return runtime.newFloat(variant.getFloat());
            case Variant.VariantString:
                return runtime.newString(variant.getString());
        }

        return JavaUtil.convertJavaToUsableRubyObject(runtime, variant.toJavaObject());
    }

    public static IRubyObject date2ruby(Ruby runtime, Date date) {
        Calendar cal = Calendar.getInstance();

        cal.setTime(date);

        return runtime.newTime(cal.getTimeInMillis());
    }

    public static String toProgID(String id) {
        if (id != null && id.startsWith("{") && id.endsWith("}")) {
            return "clsid:" + id.substring(1, id.length() - 1);
        }

        return id;
    }
}
