package org.jruby.javasupport;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;

import org.jruby.exceptions.NameError;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.Ruby;
import org.jruby.RubyProc;

public class JavaSupport {
    private Ruby runtime;

    private Map exceptionHandlers = new HashMap();

    private ClassLoader javaClassLoader = ClassLoader.getSystemClassLoader();

    private Map javaObjectMap = new HashMap();

    public JavaSupport(Ruby ruby) {
        this.runtime = ruby;
    }

    public Class loadJavaClass(String className) {
        try {
            Class result = primitiveClass(className);
            if (result == null) {
                return javaClassLoader.loadClass(className);
            }
            return result;
        } catch (ClassNotFoundException cnfExcptn) {
            throw new NameError(runtime, "cannot load Java class " + className);
        }
    }

    public void addToClasspath(URL url) {
        javaClassLoader = new URLClassLoader(new URL[] { url }, javaClassLoader);
    }

    public void defineExceptionHandler(String exceptionClass, RubyProc handler) {
        exceptionHandlers.put(exceptionClass, handler);
    }

    public void handleNativeException(Exception exception) {
        Class excptnClass = exception.getClass();
        RubyProc handler = (RubyProc)exceptionHandlers.get(excptnClass.getName());
        while (handler == null &&
               excptnClass != Exception.class) {
            excptnClass = excptnClass.getSuperclass();
        }
        if (handler != null) {
            handler.call(new IRubyObject[]{JavaUtil.convertJavaToRuby(runtime, exception)});
        } else {
            throw createRaiseException(exception);
        }
    }

    private RaiseException createRaiseException(Exception exception) {
        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));

        StringBuffer sb = new StringBuffer();
        sb.append("Native Exception: '");
        sb.append(exception.getClass()).append("\'; Message: ");
        sb.append(exception.getMessage());
        sb.append("; StackTrace: ");
        sb.append(stackTrace.getBuffer().toString());
        RaiseException result = new RaiseException(runtime, "RuntimeError", sb.toString());
        result.initCause(exception);
        return result;
    }

    private static Class primitiveClass(String name) {
        if (name.equals("long")) {
            return Long.TYPE;
        } else if (name.equals("int")) {
            return Integer.TYPE;
        } else if (name.equals("boolean")) {
            return Boolean.TYPE;
        } else if (name.equals("char")) {
            return Character.TYPE;
        } else if (name.equals("short")) {
            return Short.TYPE;
        } else if (name.equals("byte")) {
            return Byte.TYPE;
        } else if (name.equals("float")) {
            return Float.TYPE;
        } else if (name.equals("double")) {
            return Double.TYPE;
        }
        return null;
    }

    public ClassLoader getJavaClassLoader() {
        return javaClassLoader;
    }
    
    public JavaObject getJavaObjectFromCache(Object object) {
        Integer hash = getHashFromObject(object);
        List cached = (List)javaObjectMap.get(hash);
        if (cached == null) {
            return null;
        } else {
            Iterator iter = cached.iterator();
            while (iter.hasNext()) {
                Reference ref = (Reference) iter.next();
                JavaObject javaObject = (JavaObject)ref.get();
                if (javaObject == null) {
                    iter.remove();
                } else if (javaObject.getValue() == object) {
                    return javaObject;
                }
            }
            return null;
        }
    }
    
    public void putJavaObjectIntoCache(JavaObject object) {
        Integer hash = getHashFromObject(object.getValue());
        List cached = (List)javaObjectMap.get(hash);
        if (cached == null) {
            cached = new LinkedList();
            javaObjectMap.put(hash, cached);
        }
        cached.add(new SoftReference(object));
    }
    
    private Integer getHashFromObject(Object value) {
        if (value == null) {
            return new Integer(0);
        } else if (value instanceof Proxy) {
            return new Integer(value.getClass().hashCode());
        } else {
            return new Integer(value.hashCode());
        }
    }
}
