/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.exceptions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.exceptions.MainExitException;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.core.cast.IntegerCastNode;
import org.jruby.truffle.core.cast.IntegerCastNodeGen;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.core.AtExitManager;

public class TopLevelRaiseHandler extends RubyNode {

    @Child private RubyNode body;
    @Child private IntegerCastNode integerCastNode;

    public TopLevelRaiseHandler(RubyContext context, SourceSection sourceSection, RubyNode body) {
        super(context, sourceSection);
        this.body = body;
    }

    @SuppressWarnings("finally")
    @Override
    public Object execute(VirtualFrame frame) {
        DynamicObject lastException = null;

        try {
            body.execute(frame);
        } catch (RaiseException e) {
            lastException = AtExitManager.handleAtExitException(getContext(), e);
        } finally {
            final DynamicObject atExitException = getContext().runAtExitHooks();
            if (atExitException != null) {
                lastException = atExitException;
            }

            getContext().shutdown();

            throw new MainExitException(statusFromException(lastException));
        }
    }

    private int statusFromException(DynamicObject exception) {
        if (exception == null) {
            return 0;
        } else if (Layouts.BASIC_OBJECT.getLogicalClass(exception) == getContext().getCoreLibrary().getSystemExitClass()) {
            return castToInt(exception.get("@status", null));
        } else {
            return 1;
        }
    }

    private int castToInt(Object value) {
        if (integerCastNode == null) {
            CompilerDirectives.transferToInterpreter();
            integerCastNode = insert(IntegerCastNodeGen.create(getContext(), getSourceSection(), null));
        }
        return integerCastNode.executeCastInt(value);
    }

}
