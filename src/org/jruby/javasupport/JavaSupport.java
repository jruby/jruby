/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.javasupport;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.util.ObjectProxyCache;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.Callback;

public class JavaSupport {
    private final Ruby runtime;

    private final Map<String, RubyProc> exceptionHandlers = new HashMap<String, RubyProc>();
    
    private final ObjectProxyCache<IRubyObject,RubyClass> objectProxyCache = 
        // TODO: specifying soft refs, may want to compare memory consumption,
        // behavior with weak refs (specify WEAK in place of SOFT below)
        new ObjectProxyCache<IRubyObject,RubyClass>(ObjectProxyCache.ReferenceType.WEAK) {

        public IRubyObject allocateProxy(Object javaObject, RubyClass clazz) {
            IRubyObject proxy = clazz.allocate();
            proxy.getInstanceVariables().fastSetInstanceVariable("@java_object",
                    JavaObject.wrap(clazz.getRuntime(), javaObject));
            return proxy;
        }

    };

    
    // There's not a compelling reason to keep JavaClass instances in a weak map
    // (any proxies created are [were] kept in a non-weak map, so in most cases they will
    // stick around anyway), and some good reasons not to (JavaClass creation is
    // expensive, for one; many lookups are performed when passing parameters to/from
    // methods; etc.).
    // TODO: faster custom concurrent map
    private final ConcurrentHashMap<Class,JavaClass> javaClassCache =
        new ConcurrentHashMap<Class, JavaClass>(128);
    
    // FIXME: needs to be rethought
    private final Map matchCache = Collections.synchronizedMap(new HashMap(128));

    private Callback concreteProxyCallback;

    private RubyModule javaModule;
    private RubyModule javaUtilitiesModule;
    private RubyClass javaObjectClass;
    private RubyClass javaClassClass;
    private RubyClass javaArrayClass;
    private RubyClass javaProxyClass;
    private RubyModule javaInterfaceTemplate;
    private RubyModule packageModuleTemplate;
    private RubyClass arrayProxyClass;
    private RubyClass concreteProxyClass;
    
    
    public JavaSupport(Ruby ruby) {
        this.runtime = ruby;
    }

    final synchronized void setConcreteProxyCallback(Callback concreteProxyCallback) {
        if (this.concreteProxyCallback == null) {
            this.concreteProxyCallback = concreteProxyCallback;
        }
    }
    
    final Callback getConcreteProxyCallback() {
        return concreteProxyCallback;
    }
    
    final Map getMatchCache() {
        return matchCache;
    }

    
    public Class loadJavaClass(String className) {
        try {
            Class result = primitiveClass(className);
            if(result == null) {
                return (Ruby.isSecurityRestricted()) ? Class.forName(className) :
                   Class.forName(className, true, runtime.getJRubyClassLoader());
            }
            return result;
        } catch (ClassNotFoundException cnfExcptn) {
            throw runtime.newNameError("cannot load Java class " + className, className);
        } catch (SecurityException se) {
            throw runtime.newNameError("security: cannot load Java class " + className, className);
        }
    }

    public JavaClass getJavaClassFromCache(Class clazz) {
        return javaClassCache.get(clazz);
    }
    
    public void putJavaClassIntoCache(JavaClass clazz) {
        javaClassCache.put(clazz.javaClass(), clazz);
    }
    
    public void defineExceptionHandler(String exceptionClass, RubyProc handler) {
        exceptionHandlers.put(exceptionClass, handler);
    }

    public void handleNativeException(Throwable exception) {
        if (exception instanceof RaiseException) {
            throw (RaiseException) exception;
        }
        Class excptnClass = exception.getClass();
        RubyProc handler = exceptionHandlers.get(excptnClass.getName());
        while (handler == null &&
               excptnClass != Throwable.class) {
            excptnClass = excptnClass.getSuperclass();
        }
        if (handler != null) {
            handler.call(new IRubyObject[]{JavaUtil.convertJavaToRuby(runtime, exception)});
        } else {
            throw createRaiseException(exception);
        }
    }

    private RaiseException createRaiseException(Throwable exception) {
        RaiseException re = RaiseException.createNativeRaiseException(runtime, exception);
        
        return re;
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
    
    public ObjectProxyCache<IRubyObject,RubyClass> getObjectProxyCache() {
        return objectProxyCache;
    }

    // not synchronizing these methods, no harm if these values get set twice...
    
    public RubyModule getJavaModule() {
        if (javaModule == null) {
            javaModule = runtime.fastGetModule("Java");
        }
        return javaModule;
    }
    
    public RubyModule getJavaUtilitiesModule() {
        if (javaUtilitiesModule == null) {
            javaUtilitiesModule = runtime.fastGetModule("JavaUtilities");
        }
        return javaUtilitiesModule;
    }
    
    public RubyClass getJavaObjectClass() {
        if (javaObjectClass == null) {
            javaObjectClass = getJavaModule().fastGetClass("JavaObject");
        }
        return javaObjectClass;
    }

    public RubyClass getJavaArrayClass() {
        if (javaArrayClass == null) {
            javaArrayClass = getJavaModule().fastGetClass("JavaArray");
        }
        return javaArrayClass;
    }
    
    public RubyClass getJavaClassClass() {
        if(javaClassClass == null) {
            javaClassClass = getJavaModule().fastGetClass("JavaClass");
        }
        return javaClassClass;
    }
    
    public RubyModule getJavaInterfaceTemplate() {
        if (javaInterfaceTemplate == null) {
            javaInterfaceTemplate = runtime.fastGetModule("JavaInterfaceTemplate");
        }
        return javaInterfaceTemplate;
    }
    
    public RubyModule getPackageModuleTemplate() {
        if (packageModuleTemplate == null) {
            packageModuleTemplate = runtime.fastGetModule("JavaPackageModuleTemplate");
        }
        return packageModuleTemplate;
    }
    
    public RubyClass getJavaProxyClass() {
        if (javaProxyClass == null) {
            javaProxyClass = runtime.fastGetClass("JavaProxy");
        }
        return javaProxyClass;
    }
    
    public RubyClass getConcreteProxyClass() {
        if (concreteProxyClass == null) {
            concreteProxyClass = runtime.fastGetClass("ConcreteJavaProxy");
        }
        return concreteProxyClass;
    }
    
    public RubyClass getArrayProxyClass() {
        if (arrayProxyClass == null) {
            arrayProxyClass = runtime.fastGetClass("ArrayJavaProxy");
        }
        return arrayProxyClass;
    }

}
