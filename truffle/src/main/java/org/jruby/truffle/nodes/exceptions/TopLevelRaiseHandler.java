/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.exceptions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.IntegerCastNode;
import org.jruby.truffle.nodes.cast.IntegerCastNodeGen;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.BacktraceFormatter;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.ThreadExitException;
import org.jruby.truffle.runtime.layouts.Layouts;

public class TopLevelRaiseHandler extends RubyNode {

    @Child private RubyNode body;
    @Child private IntegerCastNode integerCastNode;

    public TopLevelRaiseHandler(RubyContext context, SourceSection sourceSection, RubyNode body) {
        super(context, sourceSection);
        this.body = body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            return body.execute(frame);
        } catch (RaiseException e) {
            handleException(e);
        } catch (ThreadExitException e) {
            // Ignore
        }

        return nil();
    }

    @TruffleBoundary
    private void handleException(RaiseException e) {
        final DynamicObject rubyException = e.getRubyException();
        final int status;

        if (Layouts.BASIC_OBJECT.getLogicalClass(rubyException) == getContext().getCoreLibrary().getSystemExitClass()) {
            status = castToInt(rubyException.get("@status", null));
        } else {
            status = 1;
            BacktraceFormatter.createDefaultFormatter(getContext()).printBacktrace((DynamicObject) rubyException, Layouts.EXCEPTION.getBacktrace((DynamicObject) rubyException));
        }

        getContext().shutdown();

        System.exit(status);
    }

    private int castToInt(Object value) {
        if (integerCastNode == null) {
            CompilerDirectives.transferToInterpreter();
            integerCastNode = insert(IntegerCastNodeGen.create(getContext(), getSourceSection(), null));
        }
        return integerCastNode.executeCastInt(value);
    }

}
