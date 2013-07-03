package org.jruby.javasupport;

public interface ParameterTypes {
    Class<?>[] getParameterTypes();
    Class<?>[] getExceptionTypes();
    boolean isVarArgs();
}
