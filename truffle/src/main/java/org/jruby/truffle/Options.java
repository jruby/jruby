/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle;

import static org.jruby.util.cli.Options.TRUFFLE_ALLOCATE_CLASS_CACHE;
import static org.jruby.util.cli.Options.TRUFFLE_ARRAY_SMALL;
import static org.jruby.util.cli.Options.TRUFFLE_ARRAY_UNINITIALIZED_SIZE;
import static org.jruby.util.cli.Options.TRUFFLE_BACKTRACES_HIDE_CORE_FILES;
import static org.jruby.util.cli.Options.TRUFFLE_BACKTRACES_INTERLEAVE_JAVA;
import static org.jruby.util.cli.Options.TRUFFLE_BACKTRACES_LIMIT;
import static org.jruby.util.cli.Options.TRUFFLE_BACKTRACES_OMIT_UNUSED;
import static org.jruby.util.cli.Options.TRUFFLE_BASICOPS_INLINE;
import static org.jruby.util.cli.Options.TRUFFLE_BINDING_LOCAL_VARIABLE_CACHE;
import static org.jruby.util.cli.Options.TRUFFLE_BIND_CACHE;
import static org.jruby.util.cli.Options.TRUFFLE_CALL_GRAPH;
import static org.jruby.util.cli.Options.TRUFFLE_CALL_GRAPH_WRITE;
import static org.jruby.util.cli.Options.TRUFFLE_CEXTS_LOG_LOAD;
import static org.jruby.util.cli.Options.TRUFFLE_CHAOS;
import static org.jruby.util.cli.Options.TRUFFLE_CLASS_CACHE;
import static org.jruby.util.cli.Options.TRUFFLE_CONSTANT_CACHE;
import static org.jruby.util.cli.Options.TRUFFLE_CORE_ALWAYS_CLONE;
import static org.jruby.util.cli.Options.TRUFFLE_CORE_LOAD_PATH;
import static org.jruby.util.cli.Options.TRUFFLE_CORE_PARALLEL_LOAD;
import static org.jruby.util.cli.Options.TRUFFLE_COVERAGE_GLOBAL;
import static org.jruby.util.cli.Options.TRUFFLE_DISPATCH_CACHE;
import static org.jruby.util.cli.Options.TRUFFLE_ENCODING_COMPATIBLE_QUERY_CACHE;
import static org.jruby.util.cli.Options.TRUFFLE_ENCODING_LOADED_CLASSES_CACHE;
import static org.jruby.util.cli.Options.TRUFFLE_EVAL_CACHE;
import static org.jruby.util.cli.Options.TRUFFLE_EXCEPTIONS_PRINT_JAVA;
import static org.jruby.util.cli.Options.TRUFFLE_EXCEPTIONS_PRINT_UNCAUGHT_JAVA;
import static org.jruby.util.cli.Options.TRUFFLE_EXCEPTIONS_STORE_JAVA;
import static org.jruby.util.cli.Options.TRUFFLE_HASH_PACKED_ARRAY_MAX;
import static org.jruby.util.cli.Options.TRUFFLE_INLINE_JS;
import static org.jruby.util.cli.Options.TRUFFLE_INLINE_NEEDS_CALLER_FRAME;
import static org.jruby.util.cli.Options.TRUFFLE_INSTANCE_VARIABLE_CACHE;
import static org.jruby.util.cli.Options.TRUFFLE_INSTRUMENTATION_SERVER_PORT;
import static org.jruby.util.cli.Options.TRUFFLE_INTEROP_CONVERT_CACHE;
import static org.jruby.util.cli.Options.TRUFFLE_INTEROP_EXECUTE_CACHE;
import static org.jruby.util.cli.Options.TRUFFLE_INTEROP_INVOKE_CACHE;
import static org.jruby.util.cli.Options.TRUFFLE_INTEROP_READ_CACHE;
import static org.jruby.util.cli.Options.TRUFFLE_INTEROP_WRITE_CACHE;
import static org.jruby.util.cli.Options.TRUFFLE_IS_A_CACHE;
import static org.jruby.util.cli.Options.TRUFFLE_METHODMISSING_ALWAYS_CLONE;
import static org.jruby.util.cli.Options.TRUFFLE_METHODMISSING_ALWAYS_INLINE;
import static org.jruby.util.cli.Options.TRUFFLE_METHOD_LOOKUP_CACHE;
import static org.jruby.util.cli.Options.TRUFFLE_METHOD_TO_PROC_CACHE;
import static org.jruby.util.cli.Options.TRUFFLE_PACK_CACHE;
import static org.jruby.util.cli.Options.TRUFFLE_PACK_RECOVER_LOOP_MIN;
import static org.jruby.util.cli.Options.TRUFFLE_PACK_UNROLL_LIMIT;
import static org.jruby.util.cli.Options.TRUFFLE_PLATFORM_SAFE_AT_EXIT;
import static org.jruby.util.cli.Options.TRUFFLE_PLATFORM_SAFE_EXIT;
import static org.jruby.util.cli.Options.TRUFFLE_PLATFORM_SAFE_IO;
import static org.jruby.util.cli.Options.TRUFFLE_PLATFORM_SAFE_LOAD;
import static org.jruby.util.cli.Options.TRUFFLE_PLATFORM_SAFE_MEMORY;
import static org.jruby.util.cli.Options.TRUFFLE_PLATFORM_SAFE_PROCESSES;
import static org.jruby.util.cli.Options.TRUFFLE_PLATFORM_SAFE_PUTS;
import static org.jruby.util.cli.Options.TRUFFLE_PLATFORM_SAFE_SIGNALS;
import static org.jruby.util.cli.Options.TRUFFLE_PLATFORM_SAFE_THREADS;
import static org.jruby.util.cli.Options.TRUFFLE_PLATFORM_USE_JAVA;
import static org.jruby.util.cli.Options.TRUFFLE_ROPE_CLASS_CACHE;
import static org.jruby.util.cli.Options.TRUFFLE_ROPE_LAZY_SUBSTRINGS;
import static org.jruby.util.cli.Options.TRUFFLE_ROPE_PRINT_INTERN_STATS;
import static org.jruby.util.cli.Options.TRUFFLE_SYMBOL_TO_PROC_CACHE;
import static org.jruby.util.cli.Options.TRUFFLE_THREAD_CACHE;
import static org.jruby.util.cli.Options.TRUFFLE_UNPACK_CACHE;
import static org.jruby.util.cli.Options.TRUFFLE_YIELD_ALWAYS_CLONE;
import static org.jruby.util.cli.Options.TRUFFLE_YIELD_ALWAYS_INLINE;
import static org.jruby.util.cli.Options.TRUFFLE_YIELD_CACHE;

public class Options {

    // Platform

    public final boolean PLATFORM_SAFE_LOAD = TRUFFLE_PLATFORM_SAFE_LOAD.load();
    public final boolean PLATFORM_SAFE_IO = TRUFFLE_PLATFORM_SAFE_IO.load();
    public final boolean PLATFORM_SAFE_MEMORY = TRUFFLE_PLATFORM_SAFE_MEMORY.load();
    public final boolean PLATFORM_SAFE_THREADS = TRUFFLE_PLATFORM_SAFE_THREADS.load();
    public final boolean PLATFORM_SAFE_PROCESSES = TRUFFLE_PLATFORM_SAFE_PROCESSES.load();
    public final boolean PLATFORM_SAFE_SIGNALS = TRUFFLE_PLATFORM_SAFE_SIGNALS.load();
    public final boolean PLATFORM_SAFE_EXIT = TRUFFLE_PLATFORM_SAFE_EXIT.load();
    public final boolean PLATFORM_SAFE_AT_EXIT = TRUFFLE_PLATFORM_SAFE_AT_EXIT.load();
    public final boolean PLATFORM_SAFE_PUTS = TRUFFLE_PLATFORM_SAFE_PUTS.load();
    public final boolean PLATFORM_USE_JAVA = TRUFFLE_PLATFORM_USE_JAVA.load();

    // Features

    public final boolean COVERAGE_GLOBAL = TRUFFLE_COVERAGE_GLOBAL.load();
    public final boolean INLINE_JS = TRUFFLE_INLINE_JS.load();

    public static final boolean SHARED_OBJECTS = org.jruby.util.cli.Options.TRUFFLE_SHARED_OBJECTS_ENABLED.load();
    public static final boolean SHARED_OBJECTS_DEBUG = org.jruby.util.cli.Options.TRUFFLE_SHARED_OBJECTS_DEBUG.load();
    public static final boolean SHARED_OBJECTS_FORCE = org.jruby.util.cli.Options.TRUFFLE_SHARED_OBJECTS_FORCE.load();
    public static final boolean SHARED_OBJECTS_SHARE_ALL = org.jruby.util.cli.Options.TRUFFLE_SHARED_OBJECTS_SHARE_ALL.load();

    // Resources

    public final String CORE_LOAD_PATH = TRUFFLE_CORE_LOAD_PATH.load();
    public final boolean CORE_PARALLEL_LOAD = TRUFFLE_CORE_PARALLEL_LOAD.load();

    // Data structures

    public final int ARRAY_UNINITIALIZED_SIZE = TRUFFLE_ARRAY_UNINITIALIZED_SIZE.load();
    public final int ARRAY_SMALL = TRUFFLE_ARRAY_SMALL.load();

    public final int HASH_PACKED_ARRAY_MAX = TRUFFLE_HASH_PACKED_ARRAY_MAX.load();

    public final boolean ROPE_LAZY_SUBSTRINGS = TRUFFLE_ROPE_LAZY_SUBSTRINGS.load();
    public final boolean ROPE_PRINT_INTERN_STATS = TRUFFLE_ROPE_PRINT_INTERN_STATS.load();

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
    public final int ENCODING_LOADED_CLASSES_CACHE = TRUFFLE_ENCODING_LOADED_CLASSES_CACHE.load();
    public final int THREAD_CACHE = TRUFFLE_THREAD_CACHE.load();
    public final int ROPE_CLASS_CACHE = TRUFFLE_ROPE_CLASS_CACHE.load();
    public final int INTEROP_CONVERT_CACHE = TRUFFLE_INTEROP_CONVERT_CACHE.load();
    public final int INTEROP_EXECUTE_CACHE = TRUFFLE_INTEROP_EXECUTE_CACHE.load();
    public final int INTEROP_READ_CACHE = TRUFFLE_INTEROP_READ_CACHE.load();
    public final int INTEROP_WRITE_CACHE = TRUFFLE_INTEROP_WRITE_CACHE.load();
    public final int INTEROP_INVOKE_CACHE = TRUFFLE_INTEROP_INVOKE_CACHE.load();

    // Cloning and inlining

    public final boolean CORE_ALWAYS_CLONE = TRUFFLE_CORE_ALWAYS_CLONE.load();
    public final boolean INLINE_NEEDS_CALLER_FRAME = TRUFFLE_INLINE_NEEDS_CALLER_FRAME.load();
    public final boolean YIELD_ALWAYS_CLONE = TRUFFLE_YIELD_ALWAYS_CLONE.load();
    public final boolean YIELD_ALWAYS_INLINE = TRUFFLE_YIELD_ALWAYS_INLINE.load();
    public final boolean METHODMISSING_ALWAYS_CLONE = TRUFFLE_METHODMISSING_ALWAYS_CLONE.load();
    public final boolean METHODMISSING_ALWAYS_INLINE = TRUFFLE_METHODMISSING_ALWAYS_INLINE.load();

    // Other tuning parameters

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
    public static final boolean BASICOPS_INLINE = TRUFFLE_BASICOPS_INLINE.load();

    // Call graph

    public final boolean CALL_GRAPH = TRUFFLE_CALL_GRAPH.load();
    public final String CALL_GRAPH_WRITE = TRUFFLE_CALL_GRAPH_WRITE.load();

    // Other tools

    public boolean CHAOS = TRUFFLE_CHAOS.load();

    // Cexts

    public final boolean CEXTS_LOG_LOAD = TRUFFLE_CEXTS_LOG_LOAD.load();

}
