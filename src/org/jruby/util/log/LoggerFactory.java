package org.jruby.util.log;

import org.jruby.util.SafePropertyAccessor;

import java.lang.reflect.Constructor;

public class LoggerFactory {

    private static final String LOGGER_CLASS = SafePropertyAccessor.getProperty("jruby.logger.class", "org.jruby.util.log.StandardErrorLogger");

    public static Logger getLogger(String loggerName) {
        try {
            final Class<?> cls = Class.forName(LOGGER_CLASS);
            final Constructor<?> ctor = cls.getDeclaredConstructor(String.class);
            Logger logger = (Logger) ctor.newInstance(loggerName);
            return logger;
        } catch (SecurityException e) {
            return new StandardErrorLogger(loggerName);
        } catch (Exception e) {
            throw new IllegalStateException("unable to instantiate logger", e);
        }
    }

}
