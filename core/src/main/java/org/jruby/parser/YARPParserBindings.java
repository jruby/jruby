package org.jruby.parser;

import jnr.ffi.Struct;
import jnr.ffi.annotations.In;
import jnr.ffi.byref.ByteByReference;

/**
 * JNR binding to libyarp.{so,dylib,dll}
 */
public interface YARPParserBindings {

    class Buffer extends Struct {
        public Struct.Pointer value = new Struct.Pointer();
        public Struct.size_t length = new Struct.size_t();
        public Struct.size_t capacity = new Struct.size_t();

        public Buffer(jnr.ffi.Runtime runtime) {
            super(runtime);
        }
    }

//jnr.ffi.Pointer yp_version();

    //jnr.ffi.Pointer rb_ary_new();
    // FIXME: buffer could be @Out with mechanism to free it but perhaps it is ByteBuffer?
    void yp_buffer_init(Buffer buffer);
    void yp_parse_serialize(@In byte[] source, int size, Buffer buffer, @In byte[] metadata);
}
