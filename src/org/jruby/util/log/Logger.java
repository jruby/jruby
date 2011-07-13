package org.jruby.util.log;

public interface Logger {

    public String getName();

    public void warn(String message, Object... args);

    public void warn(Throwable throwable);

    public void warn(String message, Throwable throwable);

    public void error(String message, Object... args);

    public void error(Throwable throwable);

    public void error(String message, Throwable throwable);

    public void info(String message, Object... args);

    public void info(Throwable throwable);

    public void info(String message, Throwable throwable);

    public void debug(String message, Object... args);

    public void debug(Throwable throwable);

    public void debug(String message, Throwable throwable);

}
