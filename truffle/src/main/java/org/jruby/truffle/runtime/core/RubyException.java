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

import org.jruby.truffle.nodes.core.ExceptionNodes;
import org.jruby.truffle.nodes.core.StringNodes;
import org.jruby.truffle.runtime.backtrace.Backtrace;

/**
 * Represents the Ruby {@code Exception} class.
 */
public class RubyException extends RubyBasicObject {

    public Object message;
    public Backtrace backtrace;

    public RubyException(RubyClass rubyClass) {
        super(rubyClass);
        ExceptionNodes.setMessage(this, StringNodes.createEmptyString(rubyClass.getContext().getCoreLibrary().getStringClass()));
    }

    public RubyException(RubyClass rubyClass, Object message, Backtrace backtrace) {
        this(rubyClass);
        ExceptionNodes.setMessage(this, message);
        ExceptionNodes.setBacktrace(this, backtrace);
    }

}
