package org.jruby;

import org.jruby.runtime.*;

public class RubyJava {
    public static RubyModule createJavaModule(Ruby ruby) {
        RubyModule javaModule = ruby.defineModule("Java");

        javaModule.defineModuleFunction("import", CallbackFactory.getSingletonMethod(RubyJava.class, "rbImport", RubyString.class));
        javaModule.defineModuleFunction("name", CallbackFactory.getSingletonMethod(RubyJava.class, "name", RubyString.class, RubyString.class));
        javaModule.defineModuleFunction("define_exception_handler", CallbackFactory.getOptSingletonMethod(RubyJava.class, "define_exception_handler"));

        return javaModule;
    }

	// Java methods
    public static RubyObject rbImport(Ruby ruby, RubyObject recv, RubyString packageName) {
		ruby.getJavaSupport().addImportPackage(packageName.getValue());
        return recv;
    }

    public static RubyObject name(Ruby ruby, RubyObject recv, RubyString javaName, RubyString rubyName) {
		ruby.getJavaSupport().rename(rubyName.getValue(), javaName.getValue());
        return recv;
    }

    public static RubyObject define_exception_handler(Ruby ruby, RubyObject recv, RubyObject[] args) {
        String name = args[0].toString();
        RubyProc handler = null;
        if (args.length > 1) {
            handler = (RubyProc)args[1];
        } else {
            handler = RubyProc.newProc(ruby, ruby.getClasses().getProcClass());
        }
        ruby.getJavaSupport().defineExceptionHandler(name, handler);

        return recv;
    }
}