/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language;

import static org.jruby.util.cli.Options.*;

public class Options {

    // Features

    public final boolean COVERAGE = TRUFFLE_COVERAGE.load();
    public final boolean COVERAGE_GLOBAL = TRUFFLE_COVERAGE_GLOBAL.load();

    // Resources

    public final String CORE_LOAD_PATH = TRUFFLE_CORE_LOAD_PATH.load();
    public final boolean PLATFORM_USE_JAVA = TRUFFLE_PLATFORM_USE_JAVA.load();

    // Data structures

    public final int ARRAY_UNINITIALIZED_SIZE = TRUFFLE_ARRAY_UNINITIALIZED_SIZE.load();
    public final int ARRAY_SMALL = TRUFFLE_ARRAY_SMALL.load();

    public final int HASH_PACKED_ARRAY_MAX = TRUFFLE_HASH_PACKED_ARRAY_MAX.load();

    // Caches

    public final int METHOD_LOOKUP_CACHE = TRUFFLE_METHOD_LOOKUP_CACHE.load();
    public final int DISPATCH_CACHE = TRUFFLE_DISPATCH_CACHE.load();
    public final int YIELD_CACHE = TRUFFLE_YIELD_CACHE.load();
    public final int METHOD_TO_PROC_CACHE = TRUFFLE_METHOD_TO_PROC_CACHE.load();
    public final int IS_A_CACHE = TRUFFLE_IS_A_CACHE.load();
    public final int BIND_CACHE = TRUFFLE_BIND_CACHE.load();
    public final int CONSTANT_CACHE = TRUFFLE_CONSTANT_CACHE.load();
    public final int INSTANCE_VARIABLE_CACHE = TRUFFLE_INSTANCE_VARIABLE_CACHE.load();
    public final int BINDING_LOCAL_VARIABLE_CACHE = TRUFFLE_BINDING_LOCAL_VARIABLE_CACHE.load();
    public final int SYMBOL_TO_PROC_CACHE = TRUFFLE_SYMBOL_TO_PROC_CACHE.load();
    public final int ALLOCATE_CLASS_CACHE = TRUFFLE_ALLOCATE_CLASS_CACHE.load();
    public final int PACK_CACHE = TRUFFLE_PACK_CACHE.load();
    public final int UNPACK_CACHE = TRUFFLE_UNPACK_CACHE.load();
    public final int EVAL_CACHE = TRUFFLE_EVAL_CACHE.load();
    public final int CLASS_CACHE = TRUFFLE_CLASS_CACHE.load();
    public final int ENCODING_COMPATIBILE_QUERY_CACHE = TRUFFLE_ENCODING_COMPATIBLE_QUERY_CACHE.load();
    public final int THREAD_CACHE = TRUFFLE_THREAD_CACHE.load();

    // Cloning and inlining

    public final boolean CORE_ALWAYS_CLONE = TRUFFLE_CORE_ALWAYS_CLONE.load();
    public final boolean INLINE_NEEDS_CALLER_FRAME = TRUFFLE_INLINE_NEEDS_CALLER_FRAME.load();
    public final boolean YIELD_ALWAYS_CLONE = TRUFFLE_YIELD_ALWAYS_CLONE.load();
    public final boolean YIELD_ALWAYS_INLINE = TRUFFLE_YIELD_ALWAYS_INLINE.load();
    public final boolean METHODMISSING_ALWAYS_CLONE = TRUFFLE_METHODMISSING_ALWAYS_CLONE.load();
    public final boolean METHODMISSING_ALWAYS_INLINE = TRUFFLE_METHODMISSING_ALWAYS_INLINE.load();

    // Other tuning parameteres

    public final int PACK_UNROLL_LIMIT = TRUFFLE_PACK_UNROLL_LIMIT.load();
    public final int PACK_RECOVER_LOOP_MIN = TRUFFLE_PACK_RECOVER_LOOP_MIN.load();

    // Debugging

    public final int INSTRUMENTATION_SERVER_PORT = TRUFFLE_INSTRUMENTATION_SERVER_PORT.load();
    public final boolean EXCEPTIONS_STORE_JAVA = TRUFFLE_EXCEPTIONS_STORE_JAVA.load();
    public final boolean EXCEPTIONS_PRINT_JAVA = TRUFFLE_EXCEPTIONS_PRINT_JAVA.load();
    public final boolean EXCEPTIONS_PRINT_UNCAUGHT_JAVA = TRUFFLE_EXCEPTIONS_PRINT_UNCAUGHT_JAVA.load();
    public final boolean BACKTRACES_HIDE_CORE_FILES = TRUFFLE_BACKTRACES_HIDE_CORE_FILES.load();
    public final boolean BACKTRACES_INTERLEAVE_JAVA = TRUFFLE_BACKTRACES_INTERLEAVE_JAVA.load();
    public final int BACKTRACES_LIMIT = TRUFFLE_BACKTRACES_LIMIT.load();
    public final boolean BACKTRACES_OMIT_UNUSED = TRUFFLE_BACKTRACES_OMIT_UNUSED.load();
    public final boolean INCLUDE_CORE_FILE_CALLERS_IN_SET_TRACE_FUNC = TRUFFLE_INCLUDE_CORE_FILE_CALLERS_IN_SET_TRACE_FUNC.load();

    // Call graph

    public final boolean CALL_GRAPH = TRUFFLE_CALL_GRAPH.load();
    public final String CALL_GRAPH_WRITE = TRUFFLE_CALL_GRAPH_WRITE.load();

}
