package org.jruby.truffle.runtime.subsystems;

import jnr.constants.platform.Errno;
import jnr.posix.POSIXHandler;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;

public class TrufflePOSIXHandler implements POSIXHandler {

    private RubyContext context;

    public TrufflePOSIXHandler(RubyContext context) {
        this.context = context;
    }

    @Override
    public void error(Errno errno, String methodName) {
        // TODO CS 17-Apr-15 - not specialised, no way to build a good stacktrace, missing content for error messages

        if (errno == Errno.ENOTEMPTY) {
            throw new RaiseException(context.getCoreLibrary().dirNotEmptyError("unknown", null));
        }

        throw new UnsupportedOperationException("errno: " + errno.name());
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
        return context.getRuntime().isVerbose();
    }

    @Override
    public File getCurrentWorkingDirectory() {
        return new File(context.getRuntime().getCurrentDirectory());
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
        throw new UnsupportedOperationException();
    }

    @Override
    public PrintStream getErrorStream() {
        throw new UnsupportedOperationException();
    }

}
