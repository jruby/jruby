package org.jruby.javasupport;

import java.lang.reflect.*;
import java.util.*;

import org.jruby.*;
import org.jruby.runtime.*;
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

    /*
     * @see Callback#execute(RubyObject, RubyObject[], Ruby)
     */
    public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
        RubyProc proc = null;
        RubyMethod method = null;

        RubyObject sendRecv = null;
        RubyString sendMethod = null;

        if (ruby.isBlockGiven()) {
            proc = RubyProc.newProc(ruby, ruby.getClasses().getProcClass());
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
                    sendMethod = RubyString.newString(ruby, methodName);
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
            return RubyJavaInterface.newJavaInterface(ruby, interfaceMethod, proc);
        } else if (method != null) {
            return RubyJavaInterface.newJavaInterface(ruby, interfaceMethod, method);
        } else {
            return RubyJavaInterface.newJavaInterface(ruby, interfaceMethod, sendRecv, sendMethod);
        }
    }
}
