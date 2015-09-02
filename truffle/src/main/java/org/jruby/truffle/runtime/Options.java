/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

public class Options {

    public static final String CORE_LOAD_PATH = org.jruby.util.cli.Options.TRUFFLE_CORE_LOAD_PATH.load();

    public static final int DISPATCH_POLYMORPHIC_MAX = org.jruby.util.cli.Options.TRUFFLE_DISPATCH_POLYMORPHIC_MAX.load();
    public static final int ARRAYS_UNINITIALIZED_SIZE = org.jruby.util.cli.Options.TRUFFLE_ARRAYS_UNINITIALIZED_SIZE.load();
    public static final int ARRAYS_SMALL = org.jruby.util.cli.Options.TRUFFLE_ARRAYS_SMALL.load();
    public static final int HASH_PACKED_ARRAY_MAX = org.jruby.util.cli.Options.TRUFFLE_HASH_PACKED_ARRAY_MAX.load();

    public static final int INSTRUMENTATION_SERVER_PORT = org.jruby.util.cli.Options.TRUFFLE_INSTRUMENTATION_SERVER_PORT.load();
    public static final boolean EXCEPTIONS_PRINT_JAVA = org.jruby.util.cli.Options.TRUFFLE_EXCEPTIONS_PRINT_JAVA.load();
    public static final boolean EXCEPTIONS_PRINT_UNCAUGHT_JAVA = org.jruby.util.cli.Options.TRUFFLE_EXCEPTIONS_PRINT_UNCAUGHT_JAVA.load();
    public static final boolean COVERAGE = org.jruby.util.cli.Options.TRUFFLE_COVERAGE.load();

    public static final boolean BACKTRACES_HIDE_CORE_FILES = org.jruby.util.cli.Options.TRUFFLE_BACKTRACES_HIDE_CORE_FILES.load();

    public static final boolean INLINER_ALWAYS_CLONE_YIELD = org.jruby.util.cli.Options.TRUFFLE_INLINER_ALWAYS_CLONE_YIELD.load();
    public static final boolean INLINER_ALWAYS_INLINE_YIELD = org.jruby.util.cli.Options.TRUFFLE_INLINER_ALWAYS_INLINE_YIELD.load();
    public static final boolean DISPATCH_METAPROGRAMMING_ALWAYS_UNCACHED = org.jruby.util.cli.Options.TRUFFLE_DISPATCH_METAPROGRAMMING_ALWAYS_UNCACHED.load();
    public static final boolean DISPATCH_METAPROGRAMMING_ALWAYS_INDIRECT = org.jruby.util.cli.Options.TRUFFLE_DISPATCH_METAPROGRAMMING_ALWAYS_INDIRECT.load();
    public static final boolean DISPATCH_METHODMISSING_ALWAYS_CLONED = org.jruby.util.cli.Options.TRUFFLE_DISPATCH_METHODMISSING_ALWAYS_CLONED.load();
    public static final boolean DISPATCH_METHODMISSING_ALWAYS_INLINED = org.jruby.util.cli.Options.TRUFFLE_DISPATCH_METHODMISSING_ALWAYS_INLINED.load();

    public static final int PACK_UNROLL_LIMIT = org.jruby.util.cli.Options.TRUFFLE_PACK_UNROLL_LIMIT.load();

}
