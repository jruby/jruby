package org.jruby.javasupport;

import org.jruby.*;
import org.jruby.runtime.Callback;
import org.jruby.runtime.builtin.IRubyObject;
/**
 * @author jpetersen
 * @version $Revision$
 **/
public class JavaInterfaceConstructor implements Callback {
    private Class javaInterface;

    public JavaInterfaceConstructor(Class javaInterface) {
        this.javaInterface = javaInterface;
    }
    
    public int getArity() {
        return 1;
    }

    /*
     * @see Callback#execute(RubyObject, RubyObject[], Ruby)
     */
    public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
        return RubyJavaInterface.newJavaInterface(recv.getRuntime(), javaInterface, args[0]);
    }
}
