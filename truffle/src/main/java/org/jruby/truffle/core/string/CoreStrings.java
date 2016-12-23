/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.string;

import org.jruby.truffle.RubyContext;

public class CoreStrings {

    public final CoreString ASSIGNMENT;
    public final CoreString BACKTRACE_OMITTED_LIMIT;
    public final CoreString BACKTRACE_OMITTED_UNUSED;
    public final CoreString CALL;
    public final CoreString CANT_COMPRESS_NEGATIVE;
    public final CoreString CHARACTER_INDEX_NEGATIVE;
    public final CoreString CLASS;
    public final CoreString CLASS_VARIABLE;
    public final CoreString EMPTY_STRING;
    public final CoreString EXPRESSION;
    public final CoreString FALSE;
    public final CoreString GLOBAL_VARIABLE;
    public final CoreString INSTANCE_VARIABLE;
    public final CoreString INVALID_VALUE_FOR_FLOAT;
    public final CoreString LINE;
    public final CoreString LOCAL_VARIABLE;
    public final CoreString METHOD;
    public final CoreString NEGATIVE_ARRAY_SIZE;
    public final CoreString NIL;
    public final CoreString ONE_HASH_REQUIRED;
    public final CoreString OUT_OF_RANGE;
    public final CoreString PROC_WITHOUT_BLOCK;
    public final CoreString RESOURCE_TEMP_UNAVAIL;
    public final CoreString UNKNOWN;
    public final CoreString SELF;
    public final CoreString TIME_INTERVAL_MUST_BE_POS;
    public final CoreString TOO_FEW_ARGUMENTS;
    public final CoreString TRUE;
    public final CoreString WRONG_ARGS_ZERO_PLUS_ONE;
    public final CoreString X_OUTSIDE_OF_STRING;
    public final CoreString YIELD;

    public CoreStrings(RubyContext context) {
        ASSIGNMENT = new CoreString(context, "assignment");
        BACKTRACE_OMITTED_LIMIT = new CoreString(context, "(omitted due to -Xtruffle.backtraces.limit)");
        BACKTRACE_OMITTED_UNUSED = new CoreString(context, "(omitted as the rescue expression was pure; use -Xtruffle.backtraces.omit_unused=false to disable)");
        CALL = new CoreString(context, "call");
        CANT_COMPRESS_NEGATIVE = new CoreString(context, "can't compress negative numbers");
        CHARACTER_INDEX_NEGATIVE = new CoreString(context, "character index is negative");
        CLASS = new CoreString(context, "class");
        CLASS_VARIABLE = new CoreString(context, "class variable");
        EMPTY_STRING = new CoreString(context, "");
        EXPRESSION = new CoreString(context, "expression");
        FALSE = new CoreString(context, "false");
        GLOBAL_VARIABLE = new CoreString(context, "global-variable");
        INSTANCE_VARIABLE = new CoreString(context, "instance-variable");
        INVALID_VALUE_FOR_FLOAT = new CoreString(context, "invalid value for Float()");
        LINE = new CoreString(context, "line");
        LOCAL_VARIABLE = new CoreString(context, "local-variable");
        METHOD = new CoreString(context, "method");
        NEGATIVE_ARRAY_SIZE = new CoreString(context, "negative array size");
        NIL = new CoreString(context, "nil");
        ONE_HASH_REQUIRED = new CoreString(context, "one hash required");
        OUT_OF_RANGE = new CoreString(context, "out of range");
        PROC_WITHOUT_BLOCK = new CoreString(context, "tried to create Proc object without a block");
        RESOURCE_TEMP_UNAVAIL = new CoreString(context, "Resource temporarily unavailable");
        UNKNOWN = new CoreString(context, "(unknown)");
        SELF = new CoreString(context, "self");
        TIME_INTERVAL_MUST_BE_POS = new CoreString(context, "time interval must be positive");
        TOO_FEW_ARGUMENTS = new CoreString(context, "too few arguments");
        TRUE = new CoreString(context, "true");
        WRONG_ARGS_ZERO_PLUS_ONE = new CoreString(context, "wrong number of arguments (0 for 1+)");
        X_OUTSIDE_OF_STRING = new CoreString(context, "X outside of string");
        YIELD = new CoreString(context, "yield");
    }

}
