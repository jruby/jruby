package org.jruby.ext.posix;

public interface POSIXHandler {
    public void error(POSIX.ERRORS error, String extraData);
    public void unimplementedError(String message);
    public void warn(String message);
}
