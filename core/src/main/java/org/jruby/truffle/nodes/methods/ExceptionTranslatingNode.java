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
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.ThreadExitException;
import org.jruby.truffle.runtime.control.TruffleFatalException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.util.cli.Options;

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
        } catch (TruffleFatalException | ThreadExitException exception) {
            throw exception;
        } catch (ControlFlowException exception) {
            controlProfile.enter();
            throw exception;
        } catch (RaiseException exception) {
            rethrowProfile.enter();
            throw exception;
        } catch (ArithmeticException exception) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(translate(exception));
        } catch (UnsupportedSpecializationException exception) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(translate(exception));
        } catch (Throwable exception) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(translate(exception));
        }
    }

    private RubyBasicObject translate(ArithmeticException exception) {
        if (Options.TRUFFLE_EXCEPTIONS_PRINT_JAVA.load()) {
            exception.printStackTrace();
        }

        throw new RaiseException(getContext().getCoreLibrary().zeroDivisionError(this));
    }

    private RubyBasicObject translate(UnsupportedSpecializationException exception) {
        if (Options.TRUFFLE_EXCEPTIONS_PRINT_JAVA.load()) {
            exception.printStackTrace();
        }

        final StringBuilder builder = new StringBuilder();
        builder.append("Truffle doesn't have a case for the ");
        builder.append(exception.getNode().getClass().getName());
        builder.append(" node with values of type ");

        for (Object value : exception.getSuppliedValues()) {
            builder.append(" ");

            if (value instanceof RubyBasicObject) {
                builder.append(((RubyBasicObject) value).getLogicalClass().getName());
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

        return getContext().getCoreLibrary().internalError(builder.toString(), this);
    }

    public RubyBasicObject translate(Throwable throwable) {
        if (throwable instanceof NullPointerException || throwable instanceof UnsupportedOperationException || Options.TRUFFLE_EXCEPTIONS_PRINT_JAVA.load()) {
            throwable.printStackTrace();
        }

        if (Options.TRUFFLE_PANIC_ON_JAVA_ASSERT.load() && throwable instanceof AssertionError) {
            DebugOperations.panic(getContext(), this, throwable.toString());
        }

        return getContext().getCoreLibrary().internalError(throwable.getClass().getSimpleName(), this);
    }

}
