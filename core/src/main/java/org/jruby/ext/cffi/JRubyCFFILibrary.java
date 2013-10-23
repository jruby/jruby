package org.jruby.ext.cffi;

import com.kenai.jffi.Platform;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.load.Library;

import java.io.IOException;

public class JRubyCFFILibrary implements Library {
    public static final String MODULE_NAME = "CFFI";
    private static boolean isWindows() { return com.kenai.jffi.Platform.getPlatform().getOS() == Platform.OS.WINDOWS; }

    @Override
    public void load(Ruby runtime, boolean wrap) throws IOException {
        RubyModule cffi = runtime.getModule("JRuby").defineOrGetModuleUnder("CFFI");
        Type.createTypeClass(runtime, cffi);
        MappedType.createConverterTypeClass(runtime, cffi);
        DynamicLibrary.createDynamicLibraryClass(runtime, cffi);
        createSimpleClass(runtime, cffi, "CallContext", CallContext.class);
        createSimpleClass(runtime, cffi, "Pointer", Pointer.class);
        createSimpleClass(runtime, cffi, "Function", Function.class);
        createAnnotatedModule(runtime, cffi, "DataConverter", DataConverter.class);
        RubyModule platform = cffi.defineModuleUnder("Platform");
        platform.defineConstant("LIBSUFFIX", RubyString.newString(runtime, isWindows() ? "dll" : "dylib"));
        platform.defineConstant("LIBPREFIX", RubyString.newString(runtime, isWindows() ? "" : "lib"));
        platform.defineConstant("OS", RubyString.newString(runtime, isWindows() ? "mswin32" : "other"));
    }
    
    static RubyModule getModule(Ruby runtime) {
        try {
            return (RubyModule) runtime.getModule("JRuby").getConstant(MODULE_NAME);
        } catch (ClassCastException cce) {
            throw runtime.newRuntimeError(cce.getMessage());
        }
    }
    
    static void annotate(RubyModule rubyModule, Class javaClass) {
        rubyModule.defineAnnotatedMethods(javaClass);
        rubyModule.defineAnnotatedConstants(javaClass);
    }
    
    static RubyClass createSimpleClass(Ruby runtime, RubyModule module, String name, Class javaClass) {
        RubyClass klass = module.defineClassUnder(name, runtime.getObject(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        annotate(klass, javaClass);
        
        return klass;
    }

    static RubyModule createAnnotatedModule(Ruby runtime, RubyModule module, String name, Class javaClass) {
        RubyModule newModule = module.defineModuleUnder(name);
        annotate(newModule, javaClass);

        return newModule;
    }
}
