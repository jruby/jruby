package org.jruby.prism.parser;

import org.jruby.Ruby;
import org.ruby_lang.prism.wasm.Prism;

public class ParserPrismWasm extends ParserPrismBase {
    private final Prism prism = new Prism();

    public ParserPrismWasm(Ruby runtime) {
        super(runtime);
    }

    protected synchronized byte[] parse(byte[] source, int sourceLength, byte[] metadata) {
        return prism.parse(source, 0, sourceLength, metadata);
    }
}
