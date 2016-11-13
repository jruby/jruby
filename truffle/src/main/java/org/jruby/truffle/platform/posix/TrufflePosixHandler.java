/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.platform.posix;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import jnr.constants.platform.Errno;
import jnr.posix.POSIXHandler;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.exception.ExceptionOperations;
import org.jruby.truffle.language.control.RaiseException;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;

public class TrufflePosixHandler implements POSIXHandler {

    private final RubyContext context;

    public TrufflePosixHandler(RubyContext context) {
        this.context = context;
    }

    @TruffleBoundary
    @Override
    public void error(Errno errno, String methodName) {
        // TODO CS 17-Apr-15 - not specialised, no way to build a good stacktrace, missing content for error messages

        throw new RaiseException(ExceptionOperations.createSystemCallError(
                context.getCoreLibrary().getErrnoClass(errno),
                context.getCoreLibrary().getNilObject(),
                null,
                errno.intValue()));
    }

    @Override
    public void error(Errno errno, String methodName, String extraData) {
        error(errno, methodName);
    }

    @Override
    public void unimplementedError(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void warn(WARNING_ID warning_id, String s, Object... objects) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isVerbose() {
        // Even if we are running in verbose mode we don't want jnr-posix's version of verbose
        return false;
    }

    @Override
    public File getCurrentWorkingDirectory() {
        return new File(context.getCurrentDirectory());
    }

    @Override
    public String[] getEnv() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getInputStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrintStream getOutputStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getPID() {
        return context.hashCode();
    }

    @Override
    public PrintStream getErrorStream() {
        throw new UnsupportedOperationException();
    }

}
