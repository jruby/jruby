package org.jruby.javasupport;

import org.jruby.runtime.Callback;
import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubyJavaInterface;

/**
 * @author jpetersen
 * @version $Revision$
 **/
public class JavaInterfaceConstructor implements Callback {
    private Class javaInterface;

    public JavaInterfaceConstructor(Class javaInterface) {
        this.javaInterface = javaInterface;
    }
    
    public Arity getArity() {
        return Arity.singleArgument();
    }

    /*
     * @see Callback#execute(RubyObject, RubyObject[], Ruby)
     */
    public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
        return RubyJavaInterface.newJavaInterface(recv.getRuntime(), javaInterface, args[0]);
    }
}
