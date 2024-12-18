
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Library;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyConstant;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.Access;
import org.jruby.ext.ffi.InvalidMemoryIO;
import org.jruby.ext.ffi.MemoryIO;
import org.jruby.ext.ffi.Pointer;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.FileResource;
import org.jruby.util.JRubyClassLoader;
import org.jruby.util.JRubyFile;

import static org.jruby.api.Create.newString;
import static org.jruby.runtime.ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR;

@JRubyClass(name = "FFI::DynamicLibrary", parent = "Object")
public class DynamicLibrary extends RubyObject {
    
    @JRubyConstant public static final int RTLD_LAZY   = 0x00001;
    @JRubyConstant public static final int RTLD_NOW    = 0x00002;
    @JRubyConstant public static final int RTLD_LOCAL  = 0x00004;
    @JRubyConstant public static final int RTLD_GLOBAL = 0x00008;
    
    private final Library library;
    private final String name;
    public static RubyClass createDynamicLibraryClass(ThreadContext context, RubyModule FFI, RubyClass Object) {
        RubyClass DynamicLibary = FFI.defineClassUnder(context, "DynamicLibrary", Object, NOT_ALLOCATABLE_ALLOCATOR).
                defineMethods(context, DynamicLibrary.class).
                defineConstants(context, DynamicLibrary.class);

        DynamicLibary.defineClassUnder(context, "Symbol", FFI.getClass(context, "Pointer"), NOT_ALLOCATABLE_ALLOCATOR).
                defineMethods(context, Symbol.class);

        return DynamicLibary;
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
        String loadName;
        // Look for potential URI's, excluding Windows paths (C:\\...)
        if (libName != null && libName.contains(":") && !new File(libName).isAbsolute()) {
            // Use internal logic to parse the URI
            FileResource resource = JRubyFile.createResource(context, libName);
            if (JRubyFile.isResourceRegularFile(resource)) {
                loadName = resource.absolutePath();
            } else try (InputStream internalStream = resource.openInputStream()){
                loadName = extractLibrary(resource.path(), internalStream);
            } catch (IOException e) {
                // let it fail normally, file not found
                loadName = resource.absolutePath();
            }
        }
        else {
            loadName = libName; // not a uri, must be a file path
        }
        try {
            Library library = Library.getCachedInstance(loadName, getNativeLibraryFlags(libraryFlags));
            if (library == null) {
                throw new UnsatisfiedLinkError(Library.getLastError());
            }
            return new DynamicLibrary(context.runtime, (RubyClass) recv,
                    libName, library);
        } catch (UnsatisfiedLinkError ex) {
            throw context.runtime.newLoadError(String.format("Could not open library '%s' : %s",
                    libName != null ? libName : "current process", ex.getMessage()));
        }
    }

    private static HashMap<String, String> extractedLibraries = null;
    private static synchronized String extractLibrary(String name, InputStream resourceAsStream) throws IOException {
        if (resourceAsStream == null) return name;

        // check the cache, don't need to extract this multiple times
        if (extractedLibraries == null) {
            extractedLibraries = new HashMap<>();
        } else if (extractedLibraries.containsKey(name)) {
            return extractedLibraries.get(name);
        }

        // not in cache, extract the file
        String basename = new File(name).getName();
        String[] names = basename.split("\\.");

        // by putting the file into the jruby temp dir, it's automatically cleaned up
        Path tempfile = Files.createTempFile(JRubyClassLoader.getTempDir().toPath(), names[0], "." + names[names.length - 1]);
        Files.copy(resourceAsStream, tempfile, StandardCopyOption.REPLACE_EXISTING);

        File file = tempfile.toFile();
        file.setExecutable(true); // do this after we write the file, otherwise dlopen doesn't work
        extractedLibraries.put(name, file.getAbsolutePath());
        return file.getAbsolutePath();
    }

    @JRubyMethod(name = {  "find_variable", "find_symbol" })
    public IRubyObject findVariable(ThreadContext context, IRubyObject symbolName) {
        final String sym = symbolName.toString();
        final long address = library.getSymbolAddress(sym);
        if (address == 0L) {
            return context.nil;
        }

        return new Symbol(context.runtime, this, sym, new DataSymbolMemoryIO(context.runtime, this, address));
    }

    @JRubyMethod(name = {  "find_function" })
    public IRubyObject findFunction(ThreadContext context, IRubyObject symbolName) {
        final String sym = symbolName.toString();
        final long address = library.getSymbolAddress(sym);
        if (address == 0L) {
            return context.nil;
        }
        return new Symbol(context.runtime, this, sym,
                new TextSymbolMemoryIO(context.runtime, this, address));
    }
    @JRubyMethod(name = "name")
    public IRubyObject name(ThreadContext context) {
        return name != null ? newString(context, name) : context.nil;
    }
    public static final class Symbol extends Pointer {
        private final DynamicLibrary library;
        private final String name;
        
        public Symbol(Ruby runtime, DynamicLibrary library, String name, MemoryIO io) {
            super(runtime, Access.getClass(runtime.getCurrentContext(), "FFI", "DynamicLibrary", "Symbol"),
                    io, Long.MAX_VALUE);
            this.library = library;
            this.name = name;
        }

        @JRubyMethod(name = "library")
        public IRubyObject library(ThreadContext context) {
            return library;
        }
        
        @JRubyMethod(name = "inspect")
        public IRubyObject inspect(ThreadContext context) {
            return newString(context, String.format("#<%s library=%s symbol=%s address=%#x>",
                    getMetaClass().getName(), library.name, name, getAddress()));
        }

        @JRubyMethod(name = "to_s")
        public IRubyObject to_s(ThreadContext context) {
            return newString(context, name);
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
    private static final class TextSymbolMemoryIO extends InvalidMemoryIO {
        private final DynamicLibrary library;

        public TextSymbolMemoryIO(Ruby runtime, DynamicLibrary library, long address) {
            super(runtime, true, address, "Library text region is inaccessible");
            this.library = library;
        }
    }
}
