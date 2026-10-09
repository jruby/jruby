package org.jruby.parser.prism.parser;

import jnr.ffi.Pointer;
import jnr.ffi.Struct;
import jnr.ffi.annotations.In;

/**
 * JNR binding to prism.{so,jnilib,dll}
 */
public interface ParserBindingPrism {
    Pointer pm_buffer_new();
    void pm_buffer_free(Pointer buffer);
    int pm_buffer_length(Pointer buffer);
    Pointer pm_buffer_value(Pointer buffer);
    void pm_serialize_parse(Pointer buffer, @In byte[] source, int size, @In byte[] metadata);
}
