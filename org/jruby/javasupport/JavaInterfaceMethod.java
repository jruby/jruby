package org.jruby.javasupport;

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Callback;
import org.jruby.runtime.Arity;
import org.jruby.RubyProc;
import org.jruby.RubyMethod;
import org.jruby.RubyString;
import org.jruby.RubyJavaInterface;

import java.util.Set;
import java.lang.reflect.Method;

/**
 * @author jpetersen
 * @version $Revision$
 **/
public class JavaInterfaceMethod implements Callback {
    private String methodName;

    private Set methodList;

    public JavaInterfaceMethod(String methodName, Set methodList) {
        this.methodName = methodName;
        this.methodList = methodList;
    }
    
    public Arity getArity() {
        return Arity.optional();
    }

    /*
     * @see Callback#execute(RubyObject, RubyObject[], Ruby)
     */
    public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
        RubyProc proc = null;
        RubyMethod method = null;

        IRubyObject sendRecv = null;
        RubyString sendMethod = null;

        if (recv.getRuntime().isBlockGiven()) {
            proc = RubyProc.newProc(recv.getRuntime(), recv.getRuntime().getClasses().getProcClass());
        } else {
            if (args.length == 2) {
                sendRecv = args[0];
                sendMethod = (RubyString)args[1];
            } else if (args.length == 1) {
                if (args[0] instanceof RubyProc) {
                    proc = (RubyProc) args[0];
                } else if (args[0] instanceof RubyMethod) {
                    method = (RubyMethod) args[0];
                } else {
                    sendRecv = args[0];
                    sendMethod = RubyString.newString(recv.getRuntime(), methodName);
                }
            }
        }

        Method interfaceMethod = (Method) methodList.iterator().next();

        if (methodList.size() > 1) {
            if (proc != null) {
            } else if (method != null) {
            }
        }

        if (proc != null) {
            return RubyJavaInterface.newJavaInterface(recv.getRuntime(), interfaceMethod, proc);
        } else if (method != null) {
            return RubyJavaInterface.newJavaInterface(recv.getRuntime(), interfaceMethod, method);
        } else {
            return RubyJavaInterface.newJavaInterface(recv.getRuntime(), interfaceMethod, sendRecv, sendMethod);
        }
    }
}
