package org.jruby.javasupport;

import java.lang.reflect.*;
import java.util.*;

import org.jruby.*;
import org.jruby.runtime.*;

public class JavaInterfaceConstructor implements Callback {
    private Class javaInterface;

    public JavaInterfaceConstructor(Class javaInterface) {
        this.javaInterface = javaInterface;
    }

    /*
     * @see Callback#execute(RubyObject, RubyObject[], Ruby)
     */
    public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
        return RubyJavaInterface.newJavaInterface(ruby, javaInterface, args[0]);
    }
}