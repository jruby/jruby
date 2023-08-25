package org.jruby.parser;

import jnr.ffi.annotations.In;

/**
 * JNR binding to libyarp.{so,dylib,dll}
 */
public interface YARPParserBindings {
    // FIXME: buffer could be @Out with mechanism to free it but perhaps it is ByteBuffer?
    void parseAndSerialize(@In byte[] source, int size, byte[] buffer, @In byte[] metadata);
}
