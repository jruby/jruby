package org.jruby;

import org.jruby.runtime.*;

public class RubyJavaInterface {
    public static RubyClass createJavaInterfaceClass(Ruby ruby) {
        RubyClass javaInterfaceClass = ruby.defineClass("JavaInterface", ruby.getClasses().getObjectClass());

        javaInterfaceClass.defineSingletonMethod("listener", CallbackFactory.getOptSingletonMethod(RubyJavaInterface.class, "listener", new Class[]{RubyString.class, RubyString.class, RubyObject[].class}));
        
        javaInterfaceClass.defineMethod("method_missing", CallbackFactory.getOptSingletonMethod(RubyJavaInterface.class, "method_missing", RubyObject.class));

        return javaInterfaceClass;
    }

    public static RubyJavaIObject listener(Ruby ruby, RubyObject recv, RubyString interfaceName, RubyString methodName, RubyObject[] proc) {
    	RubyJavaIObject newInterface = RubyJavaIObject.newInstance(ruby, ruby.getClasses().getJavaIObjectClass(), new RubyObject[]{interfaceName});
    	
    	newInterface.assign(methodName, proc.length > 0 ? (RubyProc)proc[0] : RubyProc.newProc(ruby, ruby.getClasses().getProcClass()));
    	
    	return newInterface;
    }
    
    public static RubyObject method_missing(Ruby ruby, RubyObject recv, RubyObject symbol, RubyObject[] args) {
        return listener(ruby, ruby.getClasses().getJavaInterfaceClass(), (RubyString)recv.getInstanceVar("interfaceName"), RubyString.newString(ruby, symbol.toId()), args);
    }
}