package org.jruby.javasupport;

import org.jruby.*;
import org.jruby.runtime.Callback;
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
    public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
        return RubyJavaInterface.newJavaInterface(ruby, javaInterface, args[0]);
    }
}
