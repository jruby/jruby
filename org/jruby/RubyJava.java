package org.jruby;

import org.jruby.runtime.*;

public class RubyJava {
    public static RubyModule createJavaModule(Ruby ruby) {
        RubyModule javaModule = ruby.defineModule("Java");

        javaModule.defineModuleFunction("import", CallbackFactory.getSingletonMethod(RubyJava.class, "rbImport", RubyString.class));
        javaModule.defineModuleFunction("name", CallbackFactory.getSingletonMethod(RubyJava.class, "name", RubyString.class, RubyString.class));

        return javaModule;
    }

	// Java methods
    public static RubyObject rbImport(Ruby ruby, RubyObject recv, RubyString packageName) {
		ruby.getJavaSupport().addImportPackage(packageName.getValue());
		
        return recv;
    }

    public static RubyObject name(Ruby ruby, RubyObject recv, RubyString javaName, RubyString rubyName) {
		ruby.getJavaSupport().getRenamedJavaClasses().put(rubyName.getValue(), javaName.getValue());
		
        return recv;
    }
}