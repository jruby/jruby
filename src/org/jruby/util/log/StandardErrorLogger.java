package org.jruby.util.log;

public class StandardErrorLogger implements Logger {

    private final String loggerName;

    public StandardErrorLogger(String loggerName) {
        this.loggerName = loggerName;
    }

    public String getName() {
        return loggerName;
    }

    public void warn(String message, Object... args) {
        write(message, args);
    }

    public void warn(Throwable throwable) {
        writeStackTrace(throwable);
    }

    public void warn(String message, Throwable throwable) {
        write(message, throwable);
    }

    public void error(String message, Object... args) {
        write(message, args);
    }

    public void error(Throwable throwable) {
        writeStackTrace(throwable);
    }

    public void error(String message, Throwable throwable) {
        write(message, throwable);
    }

    public void info(String message, Object... args) {
        write(message, args);
    }

    public void info(Throwable throwable) {
        writeStackTrace(throwable);
    }

    public void info(String message, Throwable throwable) {
        write(message, throwable);
    }

    public void debug(String message, Object... args) {
        write(message, args);
    }

    public void debug(Throwable throwable) {
        writeStackTrace(throwable);
    }

    public void debug(String message, Throwable throwable) {
        write(message, throwable);
    }

    private void write(String message, Object[] args) {
        final StringBuilder builder = new StringBuilder();
        if (message != null) {
            final String[] strings = message.split("\\{\\}");
            if (args.length == 0 || strings.length == args.length) {
                for (int i = 0; i < strings.length; i++) {
                    builder.append(strings[i]);
                    if (args.length > 0) {
                        builder.append(args[i]);
                    }
                }
            } else {
                System.err.println("wrong number of placeholders / arguments");
            }
        }
        System.err.println(builder.toString());
    }

    private void write(String message, Throwable throwable) {
        System.err.println(message);
        writeStackTrace(throwable);
    }

    private void writeStackTrace(Throwable throwable) {
        throwable.printStackTrace(System.err);
    }
}
