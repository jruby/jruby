/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.backtrace.MRIBacktraceFormatter;

/**
 * Represents the Ruby {@code Exception} class.
 */
public class RubyException extends RubyObject {

    /**
     * The class from which we create the object that is {@code Exception}. A subclass of
     * {@link RubyClass} so that we can override {@link RubyClass#newInstance} and allocate a
     * {@link RubyException} rather than a normal {@link RubyBasicObject}.
     */
    public static class RubyExceptionClass extends RubyClass {

        public RubyExceptionClass(RubyClass superClass, String name) {
            super(null, null, superClass, name);
        }

        @Override
        public RubyBasicObject newInstance(RubyNode currentNode) {
            return new RubyException(this);
        }

    }

    private RubyString message;
    private Backtrace backtrace;

    public RubyException(RubyClass rubyClass) {
        super(rubyClass);
        message = rubyClass.getContext().makeString("(object uninitialized)");
        backtrace = null;
    }

    public RubyException(RubyClass rubyClass, RubyString message, Backtrace backtrace) {
        this(rubyClass);
        initialize(message, backtrace);
    }

    public void initialize(RubyString message, Backtrace backtrace) {
        assert message != null;
        assert backtrace != null;
        this.message = message;
        this.backtrace = backtrace;
    }

    public RubyString getMessage() {
        return message;
    }

    public Backtrace getBacktrace() {
        return backtrace;
    }

    public RubyArray asRubyStringArray() {
        final String[] lines = new MRIBacktraceFormatter().format(getRubyClass().getContext(), this, backtrace);

        final Object[] array = new Object[lines.length];

        for (int n = 0;n < lines.length; n++) {
            array[n] = getRubyClass().getContext().makeString(lines[n]);
        }

        return RubyArray.fromObjects(getRubyClass().getContext().getCoreLibrary().getArrayClass(), array);
    }

}
