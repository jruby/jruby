package org.jruby.parser.prism.parser;

import jnr.ffi.Pointer;
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

        Pointer buffer = prismLibrary.pm_buffer_new();
        prismLibrary.pm_serialize_parse(buffer, source, sourceLength, metadata);
        if (parserTiming) {
            ParserStats stats = runtime.getParserManager().getParserStats();

            stats.addPrismTimeCParseSerialize(System.nanoTime() - time);
        }

        Pointer serialized = prismLibrary.pm_buffer_value(buffer);
        int length = prismLibrary.pm_buffer_length(buffer);

        byte[] result = new byte[length];
        serialized.get(0, result, 0, length);

        prismLibrary.pm_buffer_free(buffer);

        return result;
    }

    public void close() {

    }
}
