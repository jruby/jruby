package org.jruby.javasupport;

import java.lang.reflect.*;
import java.util.*;

import org.jruby.*;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
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
    
    public int getArity() {
        return -1;
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
