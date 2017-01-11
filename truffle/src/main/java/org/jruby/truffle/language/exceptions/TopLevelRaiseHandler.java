/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.cast.IntegerCastNode;
import org.jruby.truffle.core.cast.IntegerCastNodeGen;
import org.jruby.truffle.core.kernel.AtExitManager;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.ExitException;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.objects.ReadObjectFieldNode;

public class TopLevelRaiseHandler extends RubyNode {

    @Child private RubyNode body;
    @Child private IntegerCastNode integerCastNode;
    @Child private SetExceptionVariableNode setExceptionVariableNode;

    public TopLevelRaiseHandler(RubyNode body) {
        this.body = body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        int exitCode = 0;

        try {
            body.execute(frame);
        } catch (RaiseException e) {
            DynamicObject rubyException = AtExitManager.handleAtExitException(getContext(), e);
            getSetExceptionVariableNode().setLastException(frame, rubyException);
            exitCode = statusFromException(rubyException);
        } catch (ExitException e) {
            exitCode = e.getCode();
        } finally {
            final DynamicObject atExitException = getContext().getAtExitManager().runAtExitHooks();

            if (atExitException != null) {
                exitCode = statusFromException(atExitException);
            }

        }

        return exitCode;
    }

    private int statusFromException(DynamicObject exception) {
        if (Layouts.BASIC_OBJECT.getLogicalClass(exception) == coreLibrary().getSystemExitClass()) {
            return castToInt(ReadObjectFieldNode.read(exception, "@status", null));
        } else {
            return 1;
        }
    }

    private int castToInt(Object value) {
        if (integerCastNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            integerCastNode = insert(IntegerCastNodeGen.create(null));
        }

        return integerCastNode.executeCastInt(value);
    }

    private SetExceptionVariableNode getSetExceptionVariableNode() {
        if (setExceptionVariableNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            setExceptionVariableNode = insert(new SetExceptionVariableNode(getContext()));
        }

        return setExceptionVariableNode;
    }

}
