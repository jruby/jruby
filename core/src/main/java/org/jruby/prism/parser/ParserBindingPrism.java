package org.jruby.prism.parser;

import jnr.ffi.Struct;
import jnr.ffi.annotations.In;

/**
 * JNR binding to prism.{so,jnilib,dll}
 */
public interface ParserBindingPrism {

    class Buffer extends Struct {
        public size_t length = new size_t();
        public size_t capacity = new size_t();
        public Pointer value = new Pointer();

        public Buffer(jnr.ffi.Runtime runtime) {
            super(runtime);
        }
    }

    // FIXME: buffer could be @Out with mechanism to free it but perhaps it is ByteBuffer?
    // FIXME: consider source to add begin so that we can not be forced to arraycopy to right-sized byte[]
    void pm_buffer_init(Buffer buffer);
    void pm_serialize_parse(Buffer buffer, @In byte[] source, int size, @In byte[] metadata);
}
