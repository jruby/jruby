package org.jruby.javasupport;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.jruby.exceptions.NameError;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyClass;
import org.jruby.RubyProc;

public class JavaSupport {
    private Ruby ruby;

    private Map loadedJavaClasses = new HashMap();
    private List importedPackages = new ArrayList();
    private Map renamedJavaClasses = new HashMap();
    private Map exceptionHandlers = new HashMap();

    private ClassLoader javaClassLoader = ClassLoader.getSystemClassLoader();

    public JavaSupport(Ruby ruby) {
        this.ruby = ruby;
    }

    public RubyModule loadClass(Class javaClass, String rubyName) {
        if (javaClass == Object.class) {
            return ruby.getClasses().getJavaObjectClass();
        }
        if (loadedJavaClasses.containsKey(javaClass)) {
            return (RubyModule) loadedJavaClasses.get(javaClass);
        }

        if (rubyName == null) {
            String javaName = javaClass.getName();
            rubyName = javaName.substring(javaName.lastIndexOf('.') + 1);
        }
        return createRubyClass(javaClass, rubyName);
    }

    private RubyClass createRubyClass(Class javaClass, String rubyName) {
        Class javaSuperClass = javaClass.getSuperclass();
        RubyClass superClass;
        if (javaSuperClass != null) {
            superClass = (RubyClass) loadClass(javaSuperClass, null);
        } else {
            superClass = ruby.getClasses().getObjectClass();
        }
        RubyClass rubyClass = ruby.defineClass(rubyName, superClass);

        loadedJavaClasses.put(javaClass, rubyClass);
        return rubyClass;
    }

    public Class loadJavaClass(String className) {
        try {
            Class result = primitiveClass(className);
            if (result == null) {
                return javaClassLoader.loadClass(className);
            }
            return result;
        } catch (ClassNotFoundException cnfExcptn) {
            Iterator iter = importedPackages.iterator();
            while (iter.hasNext()) {
                String packageName = (String) iter.next();
                try {
                    return javaClassLoader.loadClass(packageName + "." + className);
                } catch (ClassNotFoundException cnfExcptn_) {
                }
            }
        }
        throw new NameError(ruby, "cannot load Java class: " + className);
    }

    public void addToClasspath(URL url) {
        javaClassLoader = new URLClassLoader(new URL[] { url }, javaClassLoader);
    }

    public void addImportPackage(String packageName) {
        importedPackages.add(packageName);
    }

	public String getJavaName(String rubyName) {
		return (String) renamedJavaClasses.get(rubyName);
	}

	public void rename(String rubyName, String javaName) {
		renamedJavaClasses.put(rubyName, javaName);
	}

    public Class getJavaClass(RubyClass type) {
        Iterator iter = loadedJavaClasses.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            if (entry.getValue() == type) {
                return (Class)entry.getKey();
            }
        }
        return null;
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
            handler.call(new IRubyObject[]{JavaUtil.convertJavaToRuby(ruby, exception)});
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
        RaiseException result = new RaiseException(ruby, "RuntimeError", sb.toString());
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
}
