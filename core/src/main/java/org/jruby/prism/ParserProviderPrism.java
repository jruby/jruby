package org.jruby.prism;

import jnr.ffi.LibraryLoader;
import org.jruby.Ruby;
import org.jruby.ir.builder.IRBuilderFactory;
import org.jruby.parser.Parser;
import org.jruby.parser.ParserManager;
import org.jruby.parser.ParserProvider;
import org.jruby.prism.builder.IRBuilderFactoryPrism;
import org.jruby.prism.parser.ParserBindingPrism;
import org.jruby.prism.parser.ParserPrismNative;
import org.jruby.prism.parser.ParserPrismWasm;

import java.io.File;

public class ParserProviderPrism implements ParserProvider {
    private static ParserBindingPrism prismLibrary;

    public void initialize(String path) {
        if (new File(path).exists()) {
            if (prismLibrary != null) {
                System.out.println("Prism already initialized");
                return;
            }
            prismLibrary = LibraryLoader.create(ParserBindingPrism.class).load(path);
            // We do something extra here as a side-effect which is how we get an UnsatisfiedLinkError
            // If the library didn't in fact find the .so or has other loading problems.
            ParserBindingPrism.Buffer buffer = new ParserBindingPrism.Buffer(jnr.ffi.Runtime.getRuntime(prismLibrary));
        } else {
            prismLibrary = null;
        }
    }

    public Parser getParser(Ruby runtime) {
        if (ParserManager.PARSER_WASM || prismLibrary == null) {
            // uninitialized dynamic lib or wasm requested
            return new ParserPrismWasm(runtime);
        }

        return new ParserPrismNative(runtime, prismLibrary);
    }

    public IRBuilderFactory getBuilderFactory() {
        return new IRBuilderFactoryPrism();
    }
}
