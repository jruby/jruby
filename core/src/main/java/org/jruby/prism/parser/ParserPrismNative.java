package org.jruby.prism.parser;

import org.jruby.Ruby;
import org.jruby.management.ParserStats;

public class ParserPrismNative extends ParserPrismBase {
    private final ParserBindingPrism prismLibrary;

    public ParserPrismNative(Ruby runtime, ParserBindingPrism prismLibrary) {
        super(runtime);
        this.prismLibrary = prismLibrary;
    }

    protected byte[] parse(byte[] source, int sourceLength, byte[] metadata) {
        long time = 0;
        if (parserTiming) time = System.nanoTime();

        ParserBindingPrism.Buffer buffer = new ParserBindingPrism.Buffer(jnr.ffi.Runtime.getRuntime(prismLibrary));
        prismLibrary.pm_buffer_init(buffer);
        prismLibrary.pm_serialize_parse(buffer, source, sourceLength, metadata);
        if (parserTiming) {
            ParserStats stats = runtime.getParserManager().getParserStats();

            stats.addPrismTimeCParseSerialize(System.nanoTime() - time);
        }

        int length = buffer.length.intValue();
        byte[] src = new byte[length];
        buffer.value.get().get(0, src, 0, length);

        return src;
    }
}
