
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Library;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyConstant;
import org.jruby.anno.JRubyMethod;
import org.jruby.ext.ffi.DirectMemoryIO;
import org.jruby.ext.ffi.InvalidMemoryIO;
import org.jruby.ext.ffi.NullMemoryIO;
import org.jruby.ext.ffi.Pointer;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name = "FFI::DynamicLibrary", parent = "Object")
public class DynamicLibrary extends RubyObject {
    
    @JRubyConstant public static final int RTLD_LAZY   = 0x00001;
    @JRubyConstant public static final int RTLD_NOW    = 0x00002;
    @JRubyConstant public static final int RTLD_LOCAL  = 0x00004;
    @JRubyConstant public static final int RTLD_GLOBAL = 0x00008;
    
    private final Library library;
    private final String name;
    public static RubyClass createDynamicLibraryClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder("DynamicLibrary",
                runtime.getObject(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

        RubyClass symClass = result.defineClassUnder("Symbol",
                module.fastGetClass("Pointer"), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        symClass.defineAnnotatedMethods(Symbol.class);
        result.defineAnnotatedMethods(DynamicLibrary.class);
        result.defineAnnotatedConstants(DynamicLibrary.class);

        return result;
    }
    private static final int getNativeLibraryFlags(IRubyObject rbFlags) {
        int f = 0, flags = RubyNumeric.fix2int(rbFlags);
        f |= (flags & RTLD_LAZY) != 0 ? Library.LAZY : 0;
        f |= (flags & RTLD_NOW) != 0 ? Library.NOW : 0;
        f |= (flags & RTLD_LOCAL) != 0 ? Library.LOCAL : 0;
        f |= (flags & RTLD_GLOBAL) != 0 ? Library.GLOBAL : 0;
        return f;
    }
    public DynamicLibrary(Ruby runtime, RubyClass klass, String name, Library library) {
        super(runtime, klass);
        this.name = name;
        this.library = library;
    }
    final Library getNativeLibrary() {
        return library;
    }
    @JRubyMethod(name = {  "open" }, meta = true)
    public static final  IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject libraryName, IRubyObject libraryFlags) {
        final String libName = libraryName.isNil() ? null : libraryName.toString();
        try {
            Library library = Library.getCachedInstance(libName, getNativeLibraryFlags(libraryFlags));
            if (library == null) {
                throw new UnsatisfiedLinkError(Library.getLastError());
            }
            return new DynamicLibrary(context.getRuntime(), (RubyClass) recv, 
                    libName, library);
        } catch (UnsatisfiedLinkError ex) {
            throw context.getRuntime().newLoadError(String.format("Could not open library '%s' : %s",
                    libName != null ? libName : "current process", ex.getMessage()));
        }
    }
    @JRubyMethod(name = {  "find_variable", "find_symbol" })
    public IRubyObject findVariable(ThreadContext context, IRubyObject symbolName) {
        final String sym = symbolName.toString();
        final long address = library.getSymbolAddress(sym);
        if (address == 0L) {
            return context.getRuntime().getNil();
        }

        return new Symbol(context.getRuntime(), this, sym,
                address != 0
                ? new DataSymbolMemoryIO(context.getRuntime(), this, address)
                : new NullMemoryIO(context.getRuntime()));
    }

    @JRubyMethod(name = {  "find_function" })
    public IRubyObject findFunction(ThreadContext context, IRubyObject symbolName) {
        final String sym = symbolName.toString();
        final long address = library.getSymbolAddress(sym);
        if (address == 0L) {
            return context.getRuntime().getNil();
        }
        return new Symbol(context.getRuntime(), this, sym, 
                new TextSymbolMemoryIO(context.getRuntime(), this, address));
    }
    @JRubyMethod(name = "name")
    public IRubyObject name(ThreadContext context) {
        return name != null ? RubyString.newString(context.getRuntime(), name) : context.getRuntime().getNil();
    }
    public static final class Symbol extends Pointer {
        private final DynamicLibrary library;
        private final String name;
        
        public Symbol(Ruby runtime, DynamicLibrary library, String name, DirectMemoryIO io) {
            super(runtime, runtime.fastGetModule("FFI").fastGetClass("DynamicLibrary").fastGetClass("Symbol"),
                    io, Long.MAX_VALUE);
            this.library = library;
            this.name = name;
        }

        @JRubyMethod(name = "library")
        public IRubyObject library(ThreadContext context) {
            return library;
        }

        @Override
        @JRubyMethod(name = "inspect")
        public IRubyObject inspect(ThreadContext context) {
            return RubyString.newString(context.getRuntime(),
                    String.format("#<Library:Symbol library=%s symbol=%s address=%#x>", library.name, name, getAddress()));
        }

        @Override
        @JRubyMethod(name = "to_s", optional = 1)
        public IRubyObject to_s(ThreadContext context, IRubyObject[] args) {
            return RubyString.newString(context.getRuntime(), name);
        }

        @Override
        public final String toString() {
            return name;
        }

        final String getName() {
            return name;
        }
    }

    /**
     * Small MemoryIO wrapper class to keep the library alive
     */
    private static final class DataSymbolMemoryIO extends NativeMemoryIO {
        private final DynamicLibrary library;

        public DataSymbolMemoryIO(Ruby runtime, DynamicLibrary library, long address) {
            super(runtime, address);
            this.library = library;
        }
    }
    
    /**
     * Since the text area of a dynamic library is usually not readable nor writable,
     * wrap the address in a MemoryIO instance that throws an exception on all accesses
     */
    private static final class TextSymbolMemoryIO extends InvalidMemoryIO implements DirectMemoryIO {
        private final DynamicLibrary library;
        private final long address;

        public TextSymbolMemoryIO(Ruby runtime, DynamicLibrary library, long address) {
            super(runtime, "Library text region is inaccessible");
            this.library = library;
            this.address = address;
        }

        public long getAddress() {
            return address;
        }

        public boolean isDirect() {
            return true;
        }

        public boolean isNull() {
            return false;
        }
    }
}
