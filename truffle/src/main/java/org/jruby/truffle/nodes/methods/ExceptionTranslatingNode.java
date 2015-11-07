/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.exceptions.MainExitException;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.ThreadExitException;
import org.jruby.truffle.runtime.control.TruffleFatalException;
import org.jruby.truffle.runtime.layouts.Layouts;

public class ExceptionTranslatingNode extends RubyNode {

    private final UnsupportedOperationBehavior unsupportedOperationBehavior;

    @Child private RubyNode child;

    private final BranchProfile controlProfile = BranchProfile.create();
    private final BranchProfile rethrowProfile = BranchProfile.create();

    public ExceptionTranslatingNode(RubyContext context, SourceSection sourceSection, RubyNode child) {
        this(context, sourceSection, child, UnsupportedOperationBehavior.TYPE_ERROR);
    }

    public ExceptionTranslatingNode(RubyContext context, SourceSection sourceSection, RubyNode child, UnsupportedOperationBehavior unsupportedOperationBehavior) {
        super(context, sourceSection);
        this.child = child;
        this.unsupportedOperationBehavior = unsupportedOperationBehavior;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            return child.execute(frame);
        } catch (StackOverflowError error) {
            // TODO: we might want to do sth smarter here to avoid consuming frames when we are almost out of it.
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(translate(error));
        } catch (TruffleFatalException | ThreadExitException exception) {
            throw exception;
        } catch (ControlFlowException exception) {
            controlProfile.enter();
            throw exception;
        } catch (RaiseException exception) {
            rethrowProfile.enter();
            throw exception;
        } catch (MainExitException exception) {
            CompilerDirectives.transferToInterpreter();
            throw exception;
        } catch (ArithmeticException exception) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(translate(exception));
        } catch (UnsupportedSpecializationException exception) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(translate(exception));
        } catch (org.jruby.exceptions.RaiseException e) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().toTruffle(e.getException(), this));
        } catch (Throwable exception) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(translate(exception));
        }
    }

    private DynamicObject translate(ArithmeticException exception) {
        if (getContext().getOptions().EXCEPTIONS_PRINT_JAVA) {
            exception.printStackTrace();
        }

        return getContext().getCoreLibrary().zeroDivisionError(this);
    }

    private DynamicObject translate(UnsupportedSpecializationException exception) {
        if (getContext().getOptions().EXCEPTIONS_PRINT_JAVA) {
            exception.printStackTrace();
        }

        final StringBuilder builder = new StringBuilder();
        builder.append("Truffle doesn't have a case for the ");
        builder.append(exception.getNode().getClass().getName());
        builder.append(" node with values of type ");

        for (Object value : exception.getSuppliedValues()) {
            builder.append(" ");

            if (value == null) {
                builder.append("null");
            } else if (value instanceof DynamicObject) {
                builder.append(Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(((DynamicObject) value))).getName());
                builder.append("(");
                builder.append(value.getClass().getName());
                builder.append(")");

                if (RubyGuards.isRubyArray(value)) {
                    final DynamicObject array = (DynamicObject) value;
                    builder.append("[");

                    if (Layouts.ARRAY.getStore(array) == null) {
                        builder.append("null");
                    } else {
                        builder.append(Layouts.ARRAY.getStore(array).getClass().getName());
                    }

                    builder.append(",");
                    builder.append(Layouts.ARRAY.getSize(array));
                    builder.append("]");
                } else if (RubyGuards.isRubyHash(value)) {
                    final Object store = Layouts.HASH.getStore((DynamicObject) value);

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

            if (value instanceof Number || value instanceof Boolean) {
                builder.append("=");
                builder.append(value.toString());
            }
        }

        switch (unsupportedOperationBehavior) {
            case TYPE_ERROR:
                return getContext().getCoreLibrary().typeError(builder.toString(), this);
            case ARGUMENT_ERROR:
                return getContext().getCoreLibrary().argumentError(builder.toString(), this);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public DynamicObject translate(Throwable throwable) {
        if (getContext().getOptions().EXCEPTIONS_PRINT_JAVA || (boolean) getContext().getOptions().EXCEPTIONS_PRINT_UNCAUGHT_JAVA) {
            throwable.printStackTrace();
        }

        if (throwable.getStackTrace().length > 0) {
            return getContext().getCoreLibrary().internalError(String.format("%s %s %s", throwable.getClass().getSimpleName(), throwable.getMessage(), throwable.getStackTrace()[0].toString()), this);
        } else {
            return getContext().getCoreLibrary().internalError(String.format("%s %s ???", throwable.getClass().getSimpleName(), throwable.getMessage()), this);
        }
    }

}
