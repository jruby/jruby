package org.jruby.javasupport;

import org.jruby.*;
import org.jruby.runtime.Callback;
import org.jruby.runtime.builtin.IRubyObject;

public class JavaEachMethod implements Callback {
    private String iteratorMethod;
    private String hasNextMethod;
    private String nextMethod;

    public JavaEachMethod(String iteratorMethod, String hasNextMethod, String nextMethod) {
        this.iteratorMethod = iteratorMethod;
        this.hasNextMethod = hasNextMethod;
        this.nextMethod = nextMethod;
    }

    /*
     * @see Callback#execute(RubyObject, RubyObject[], Ruby)
     */
    public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
        if (iteratorMethod != null) {
            recv = recv.callMethod(iteratorMethod);
        }
        while (recv.callMethod(hasNextMethod).isTrue()) {
            if (nextMethod == null) {
                recv.getRuntime().yield(recv);
            } else {
                recv.getRuntime().yield(recv.callMethod(nextMethod));
            }
        }

        return recv.getRuntime().getNil();
    }
    
    public int getArity() {
        return 0;
    }
}
