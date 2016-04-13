/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.instrument;

import com.oracle.truffle.api.instrument.SyntaxTag;

/**
 * Syntax tags for Ruby-language statements. The set of tags are derived from the events provided for the
 * Kernel#set_trace_func and TracePoint APIs.
 */
public enum RubySyntaxTag implements SyntaxTag {

    LINE("line", "Execute code on a new line"),

    CLASS("class", "Start a class or module definition"),

    END("end", "Finish a class or module definition"),

    CALL("call", "Call a Ruby method"),

    RETURN("return", "Return from a Ruby method"),

    C_CALL("c_call", "Call a C-language routine"),

    C_RETURN("c_return", "Return from a C-language routine"),

    RAISE("raise", "Raise an exception"),

    B_CALL("b_call", "Event hook at block entry"),

    B_RETURN("b_return", "Event hook at block ending"),

    THREAD_BEGIN("thread_begin", "Event hook at thread beginning"),

    THREAD_END("thread_end", "Event hook at thread ending");

    private final String name;
    private final String description;

    RubySyntaxTag(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
