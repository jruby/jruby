package org.jruby.parser;

import jnr.ffi.Struct;
import jnr.ffi.annotations.In;

/**
 * JNR binding to prism.{so,jnilib,dll}
 */
public interface ParserBindingPrism {

    class Buffer extends Struct {
        public Struct.Pointer value = new Struct.Pointer();
        public Struct.size_t length = new Struct.size_t();
        public Struct.size_t capacity = new Struct.size_t();

        public Buffer(jnr.ffi.Runtime runtime) {
            super(runtime);
        }
    }

    // FIXME: buffer could be @Out with mechanism to free it but perhaps it is ByteBuffer?
    // FIXME: consider source to add begin so that we can not be forced to arraycopy to right-sized byte[]
    void pm_buffer_init(Buffer buffer);
    void pm_parse_serialize(@In byte[] source, int size, Buffer buffer, @In byte[] metadata);
}
