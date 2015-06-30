/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import java.util.Arrays;

import com.oracle.truffle.api.nodes.Node;

import org.jruby.truffle.nodes.core.StringNodes;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Backtrace;

/**
 * Represents the Ruby {@code Exception} class.
 */
public class RubyException extends RubyBasicObject {

    private Object message;
    private Backtrace backtrace;

    public RubyException(RubyClass rubyClass) {
        super(rubyClass);
        message = StringNodes.createEmptyString(rubyClass.getContext().getCoreLibrary().getStringClass());
    }

    public RubyException(RubyClass rubyClass, Object message, Backtrace backtrace) {
        this(rubyClass);
        initialize(message);
        this.backtrace = backtrace;
    }

    public void initialize(Object message) {
        assert message != null;
        this.message = message;
    }

    // TODO (eregon 16 Apr. 2015): MRI does a dynamic calls to "message"
    public Object getMessage() {
        return message;
    }

    public Backtrace getBacktrace() {
        return backtrace;
    }

    public void setBacktrace(Backtrace backtrace) {
        this.backtrace = backtrace;
    }

    public RubyBasicObject asRubyStringArray() {
        assert backtrace != null;
        final String[] lines = Backtrace.EXCEPTION_FORMATTER.format(getContext(), this, backtrace);

        final Object[] array = new Object[lines.length];

        for (int n = 0;n < lines.length; n++) {
            array[n] = StringNodes.createString(getContext().getCoreLibrary().getStringClass(), lines[n]);
        }

        return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(), array);
    }

    @Override
    public String toString() {
        return message + " : " + super.toString() + "\n" +
                Arrays.toString(Backtrace.EXCEPTION_FORMATTER.format(getContext(), this, backtrace));
    }

    public static class ExceptionAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyException(rubyClass);
        }

    }

}
