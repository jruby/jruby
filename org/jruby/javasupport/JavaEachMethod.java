package org.jruby.javasupport;

import org.jruby.*;
import org.jruby.runtime.Callback;

public class JavaEachMethod implements Callback {
    private String hasNextMethod;
    private String nextMethod;

    public JavaEachMethod(String hasNextMethod, String nextMethod) {
        this.hasNextMethod = hasNextMethod;
        this.nextMethod = nextMethod;
    }

    /*
     * @see Callback#execute(RubyObject, RubyObject[], Ruby)
     */
    public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
        while (recv.funcall(hasNextMethod).isTrue()) {
            if (nextMethod == null) {
                ruby.yield(recv);
            } else {
                ruby.yield(recv.funcall(nextMethod));
            }
        }

        return ruby.getNil();
    }
    
    public int getArity() {
        return 0;
    }
}
