package org.jruby;

import java.lang.reflect.*;
import java.lang.reflect.Method;

import org.jruby.core.*;
import org.jruby.exceptions.*;
import org.jruby.javasupport.*;

public class RubyJavaInterface extends RubyJavaObject implements InvocationHandler {

    public RubyJavaInterface(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    /** Create the JavaInterface class and add it to the Ruby runtime.
     * 
     */
    public static RubyClass createJavaInterfaceClass(Ruby ruby) {
        RubyCallbackMethod s_new = new ReflectionCallbackMethod(
                RubyJavaInterface.class, "s_new", true, true);
        RubyCallbackMethod listener = new ReflectionCallbackMethod(
                RubyJavaInterface.class, "listener", 
                new Class[]{RubyString.class, RubyString.class, RubyProc.class},
                false, true);
        RubyCallbackMethod initialize = new ReflectionCallbackMethod(
                RubyJavaInterface.class, "initialize", true);
        RubyCallbackMethod assign = new ReflectionCallbackMethod(
                RubyJavaInterface.class, "assign", new Class[]{RubyString.class, RubyProc.class});
        
        RubyClass javaInterfaceClass = ruby.defineClass("JavaInterface", 
                ruby.getClasses().getJavaObjectClass());

        javaInterfaceClass.defineSingletonMethod("new", s_new);
        javaInterfaceClass.defineSingletonMethod("listener", listener);
        
        javaInterfaceClass.defineMethod("initialize", initialize);
        javaInterfaceClass.defineMethod("assign", assign);

        return javaInterfaceClass;
    }

    /** This method is called when a method in the interface is called.
     * It invokes the assigned Ruby method or proc.
     * 
     * @see InvocationHandler#invoke(Object, Method, Object[])
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RubyObject result = getRuby().getNil();

        RubyHash interfaceProcs = (RubyHash) getInstanceVar("interfaceProcs");

        RubyString methodName = RubyString.m_newString(getRuby(), method.getName());

        RubyObject proc;
        RubyObject rubyMethod;

        if (!(proc = interfaceProcs.m_aref(methodName)).isNil()) {
            RubyObject[] rubyArgs = new RubyObject[args.length];

            for (int i = 0; i < args.length; i++) {
                rubyArgs[i] = JavaUtil.convertJavaToRuby(getRuby(), args[i], 
                                                         method.getParameterTypes()[i]);
            }

            result = ((RubyProc) proc).call(rubyArgs);
        } else if (!(rubyMethod = m_method(methodName)).isNil()) {
            RubyObject[] rubyArgs = new RubyObject[args.length];

            for (int i = 0; i < args.length; i++) {
                rubyArgs[i] = JavaUtil.convertJavaToRuby(getRuby(),  args[i],
                                                         method.getParameterTypes()[i]);
            }

            result = ((RubyMethod) rubyMethod).call(rubyArgs);
        } else {
            RubyObject[] rubyArgs = new RubyObject[args.length + 1];

            for (int i = 0; i < args.length; i++) {
                rubyArgs[i + 1] = JavaUtil.convertJavaToRuby(getRuby(), args[i], 
                                                             method.getParameterTypes()[i]);
            }

            result = funcall(getRuby().intern("send"), rubyArgs);
        }

        return JavaUtil.convertRubyToJava(getRuby(), result, method.getReturnType());
    }

    public static RubyJavaInterface s_new(Ruby ruby, RubyObject recv, RubyObject[] args) {
        RubyJavaInterface newInterface = new RubyJavaInterface(ruby, (RubyClass) recv);

        newInterface.callInit(args);

        return newInterface;
    }

    public RubyObject initialize(RubyObject[] args) {
        if (args.length == 0) {
            throw new RubyArgumentException(getRuby(), "");
        }

        Class[] interfaces = new Class[args.length];

        for (int i = 0; i < args.length; i++) {
            interfaces[i] = loadJavaClass(getRuby(), (RubyString)args[i]);
        }

        try {
            setValue(Proxy.newProxyInstance(getClass().getClassLoader(), interfaces, this));
        } catch (IllegalArgumentException iaExcptn) {
        }
        
        setInstanceVar("interfaceProcs", RubyHash.m_newHash(getRuby()));

        return this;
    }
    
    public static RubyJavaInterface listener(Ruby ruby, RubyObject recv, 
                RubyString interfaceName, RubyString methodName, RubyProc proc) {
    	RubyJavaInterface newInterface = s_new(ruby, ruby.getClasses().getJavaInterfaceClass(), 
                                               new RubyObject[]{interfaceName});
    	
    	newInterface.assign(methodName, proc);
    	
    	return newInterface;
    }
    
    public RubyObject assign(RubyString methodName, RubyProc proc) {
    	((RubyHash) getInstanceVar("interfaceProcs")).m_aset(methodName, proc);
    	
    	return this;
    }
}