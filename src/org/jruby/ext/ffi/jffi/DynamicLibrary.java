
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Address;
import com.kenai.jffi.Library;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ext.ffi.FFIProvider;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name = "FFI::DynamicLibrary", parent = "Object")
public class DynamicLibrary extends RubyObject {
    private static final Map<String, WeakReference<Library>> libraryCache
            = new ConcurrentHashMap<String, WeakReference<Library>>();
    private static final Map<IRubyObject, Library> libraryRefMap
            = Collections.synchronizedMap(new WeakHashMap<IRubyObject, Library>());

    private final Library library;
    private final String name;
    public static RubyClass createDynamicLibraryClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder("DynamicLibrary",
                runtime.getObject(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

        RubyClass symClass = result.defineClassUnder("Symbol",
                module.fastGetClass("BasePointer"), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        symClass.defineAnnotatedMethods(Symbol.class);
        result.defineAnnotatedMethods(DynamicLibrary.class);
        result.defineAnnotatedConstants(DynamicLibrary.class);

        return result;
    }
    
    public DynamicLibrary(Ruby runtime, RubyClass klass, String name, Library library) {
        super(runtime, klass);
        this.name = name;
        this.library = library;
    }
    @JRubyMethod(name = {  "open" }, meta = true)
    public static final  IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject libraryName, IRubyObject libraryFlags) {
        final String libName = libraryName.toString();
        final int libFlags = RubyNumeric.fix2int(libraryFlags);
        try {
            return new DynamicLibrary(context.getRuntime(), (RubyClass) recv, 
                    libName, LibraryCache.open(libName, libFlags));
        } catch (UnsatisfiedLinkError ex) {
            throw context.getRuntime().newLoadError(String.format("Could not open library '%s' : %s",
                    libName, Library.lastError()));
        }
    }
    @JRubyMethod(name = {  "find_symbol" })
    public IRubyObject findSymbol(ThreadContext context, IRubyObject symbolName) {
        final String sym = symbolName.toString();
        final Address address = library.findSymbol(sym);
        if (address == null || address.isNull()) {
            return context.getRuntime().getNil();
        }
        return new Symbol(context.getRuntime(), this, sym, address.nativeAddress());
    }
    static final class Symbol extends BasePointer {
        private final DynamicLibrary library;
        private final String name;
        public Symbol(Ruby runtime, DynamicLibrary library, String name, long address) {
            super(runtime, FFIProvider.getModule(runtime).fastGetClass("DynamicLibrary").fastGetClass("Symbol"),
                    new DirectMemoryIO(address), 0, Long.MAX_VALUE);
            this.library = library;
            this.name = name;
        }
        @Override
        @JRubyMethod(name = "inspect")
        public IRubyObject inspect(ThreadContext context) {
            return RubyString.newString(context.getRuntime(),
                    String.format("#<Library Symbol library=%s symbol=%s address=%#x>", library.name, name, getAddress()));
        }
    }
}
