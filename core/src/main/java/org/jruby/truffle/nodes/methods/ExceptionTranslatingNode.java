/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.TruffleFatalException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyException;
import org.jruby.truffle.runtime.core.RubyHash;

public class ExceptionTranslatingNode extends RubyNode {

    @Child protected RubyNode child;

    private final BranchProfile controlProfile = new BranchProfile();
    private final BranchProfile rethrowProfile = new BranchProfile();

    public ExceptionTranslatingNode(RubyContext context, SourceSection sourceSection, RubyNode child) {
        super(context, sourceSection);
        this.child = child;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            return child.execute(frame);
        } catch (ControlFlowException e) {
            controlProfile.enter();
            throw e;
        } catch (RaiseException e) {
            rethrowProfile.enter();
            throw e;
        } catch (TruffleFatalException e) {
            throw e;
        } catch (ArithmeticException e) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(translate(e));
        } catch (UnsupportedSpecializationException e) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(translate(e));
        } catch (Exception e) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(translate(e));
        }
    }

    private RubyBasicObject translate(ArithmeticException exception) {
        if (RubyContext.EXCEPTIONS_PRINT_JAVA) {
            exception.printStackTrace();
        }

        throw new RaiseException(getContext().getCoreLibrary().zeroDivisionError(this));
    }

    private RubyBasicObject translate(UnsupportedSpecializationException exception) {
        if (RubyContext.EXCEPTIONS_PRINT_JAVA) {
            exception.printStackTrace();
        }

        final StringBuilder builder = new StringBuilder();
        builder.append("Truffle doesn't have a case for the ");
        builder.append(exception.getNode().getClass().getName());
        builder.append(" node with values of type ");

        for (Object value : exception.getSuppliedValues()) {
            builder.append(" ");

            if (value instanceof RubyBasicObject) {
                builder.append(((RubyBasicObject) value).getRubyClass().getName());
                builder.append("(");
                builder.append(value.getClass().getName());
                builder.append(")");

                if (value instanceof RubyArray) {
                    final Object store = ((RubyArray) value).getStore();

                    if (store == null) {
                        builder.append("[null]");
                    } else {
                        builder.append("[");
                        builder.append(store.getClass().getName());
                        builder.append("]");
                    }
                } else if (value instanceof RubyHash) {
                    final Object store = ((RubyHash) value).getStore();

                    if (store == null) {
                        builder.append("[null]");
                    } else {
                        builder.append("[");
                        builder.append(store.getClass().getName());
                        builder.append("]");
                    }
                }
            } else {
                builder.append(value.getClass().getName());
            }
        }

        builder.append(" - this is either a feature we haven't implemented for Truffle yet, or it might be disallowed in Ruby anyway");

        return new RubyException(getContext().getCoreLibrary().getRubyTruffleErrorClass(), getContext().makeString(builder.toString()), RubyCallStack.getBacktrace(this));
    }

    public RubyBasicObject translate(Exception exception) {
        if (RubyContext.EXCEPTIONS_PRINT_JAVA) {
            exception.printStackTrace();
        }

        String message;

        if (exception.getMessage() == null) {
            message = exception.getClass().getSimpleName();
        } else {
            message = exception.getClass().getSimpleName() + ": " + exception.getMessage();
        }

        return new RubyException(getContext().getCoreLibrary().getRubyTruffleErrorClass(), getContext().makeString(message), RubyCallStack.getBacktrace(this));
    }

}
