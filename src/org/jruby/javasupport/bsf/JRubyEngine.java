/*
 * Copyright (C) 2002 Jan Arne Petersen  <jpetersen@uni-bonn.de>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jruby.javasupport.bsf;

import java.util.Vector;

import org.ablaf.ast.INode;
import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.exceptions.BreakJump;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.ReturnJump;
import org.jruby.exceptions.ThrowJump;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.GlobalVariable;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import com.ibm.bsf.BSFDeclaredBean;
import com.ibm.bsf.BSFException;
import com.ibm.bsf.BSFManager;
import com.ibm.bsf.util.BSFEngineImpl;
import com.ibm.bsf.util.BSFFunctions;

/** An implementation of a JRuby BSF implementation.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class JRubyEngine extends BSFEngineImpl {
    private Ruby runtime;

    public Object apply(String file, int line, int col, Object funcBody, Vector paramNames, Vector args) {
        ThreadContext threadContext = runtime.getCurrentContext();
        try {
            // add a new method conext
            threadContext.getFrameStack().push();
            threadContext.pushDynamicVars();
            threadContext.getScopeStack().push(paramNames);

            // set global variables
            for (int i = 0, size = args.size(); i < size; i++) {
                runtime.currentScope().setValue(i, convertToRuby(args.get(i)));
            }

            runtime.setPosition(file, line);

            INode node = runtime.getParser().parse(file, funcBody.toString());
            return convertToJava(runtime.getTopSelf().eval(node), Object.class);
        } finally {
            threadContext.getScopeStack().pop();
            threadContext.popDynamicVars();
            threadContext.getFrameStack().pop();
        }
    }

    public Object eval(String file, int line, int col, Object expr) throws BSFException {
        try {
            runtime.setPosition(file, line);
            IRubyObject result = runtime.evalScript(expr.toString());
            return convertToJava(result, Object.class);
        } catch (Exception excptn) {
            throw new BSFException(BSFException.REASON_EXECUTION_ERROR, "Exception", excptn);
        }
    }

    public void exec(String file, int line, int col, Object expr) throws BSFException {
        try {
            runtime.setPosition(file, line);
            runtime.evalScript(expr.toString());
        } catch (Exception excptn) {
            throw new BSFException(BSFException.REASON_EXECUTION_ERROR, "Exception", excptn);
        }
    }

    public Object call(Object recv, String method, Object[] args) throws BSFException {
        try {
            IRubyObject rubyRecv = recv != null ? JavaUtil.convertJavaToRuby(runtime, recv) : runtime.getTopSelf();

            IRubyObject[] rubyArgs = JavaUtil.convertJavaArrayToRuby(runtime, args);

            IRubyObject result = rubyRecv.callMethod(method, rubyArgs);

            return convertToJava(result, Object.class);
        } catch (Exception excptn) {
            printException(runtime, excptn);
            throw new BSFException(BSFException.REASON_EXECUTION_ERROR, excptn.getMessage(), excptn);
        }
    }

    private IRubyObject convertToRuby(Object value) {
        IRubyObject result = JavaUtil.convertJavaToRuby(runtime, value);
        if (result instanceof JavaObject) {
            runtime.getLoadService().require("java");
            result =
                runtime.getClasses().getObjectClass().getConstant("JavaUtilities").callMethod(
                    "wrap",
                    new IRubyObject[] { result, RubyString.newString(runtime, value.getClass().getName())});
        }
        return result;
    }

    private Object convertToJava(IRubyObject value, Class type) {
        runtime.getLoadService().require("java");
        if (value.isKindOf(runtime.getClasses().getClass("JavaProxy"))) {
            value = value.getInstanceVariable("java_object");
        }
        value = Java.primitive_to_java(value, value);
        return JavaUtil.convertArgument(value, type);
    }

    public void initialize(BSFManager mgr, String lang, Vector declaredBeans) throws BSFException {
        super.initialize(mgr, lang, declaredBeans);

        runtime = Ruby.getDefaultInstance();

        for (int i = 0, size = declaredBeans.size(); i < size; i++) {
            BSFDeclaredBean bean = (BSFDeclaredBean) declaredBeans.elementAt(i);
            runtime.getGlobalVariables().define(
                GlobalVariable.variableName(bean.name),
                new BeanGlobalVariable(runtime, bean));
        }
        
        runtime.getGlobalVariables().defineReadonly("$bsf", new FunctionsGlobalVariable(runtime, new BSFFunctions(mgr, this)));
    }

    public void declareBean(BSFDeclaredBean bean) throws BSFException {
        runtime.getGlobalVariables().define(
            GlobalVariable.variableName(bean.name),
            new BeanGlobalVariable(runtime, bean));
    }

    public void undeclareBean(BSFDeclaredBean bean) throws BSFException {
        runtime.getGlobalVariables().set(GlobalVariable.variableName(bean.name), runtime.getNil());
    }

    public void handleException(BSFException bsfExcptn) {
        printException(runtime, (Exception) bsfExcptn.getTargetException());
    }

    /**
     *
     * Prints out an error message.
     *
     * @param exception An Exception thrown by JRuby
     */
    private static void printException(Ruby ruby, Exception exception) {
        if (exception instanceof RaiseException) {
            ruby.printError(((RaiseException) exception).getException());
        } else if (exception instanceof ThrowJump) {
            ruby.printError(((ThrowJump) exception).getNameError());
        } else if (exception instanceof BreakJump) {
            ruby.getErrorStream().println("break without block.");
        } else if (exception instanceof ReturnJump) {
            ruby.getErrorStream().println("return without block.");
        }
    }

    private static class BeanGlobalVariable implements IAccessor {
        private Ruby runtime;
        private BSFDeclaredBean bean;

        public BeanGlobalVariable(Ruby runtime, BSFDeclaredBean bean) {
            this.runtime = runtime;
            this.bean = bean;
        }

        public IRubyObject getValue() {
            IRubyObject result = JavaUtil.convertJavaToRuby(runtime, bean.bean, bean.type);
            if (result instanceof JavaObject) {
                runtime.getLoadService().require("java");
                result =
                    runtime.getClasses().getObjectClass().getConstant("JavaUtilities").callMethod(
                        "wrap",
                        new IRubyObject[] { result, RubyString.newString(runtime, bean.type.getName())});
            }
            return result;
        }

        public IRubyObject setValue(IRubyObject value) {
            runtime.getLoadService().require("java");
            if (value.isKindOf(runtime.getClasses().getClass("JavaProxy"))) {
                value = value.getInstanceVariable("java_object");
            }
            value = Java.primitive_to_java(value, value);
            bean.bean = JavaUtil.convertArgument(value, bean.type);
            return value;
        }
    }
    
    private static class FunctionsGlobalVariable implements IAccessor {
        private Ruby runtime;
        private BSFFunctions functions;

        public FunctionsGlobalVariable(Ruby runtime, BSFFunctions functions) {
            this.runtime = runtime;
            this.functions = functions;
        }

        public IRubyObject getValue() {
            IRubyObject result = JavaUtil.convertJavaToRuby(runtime, functions, BSFFunctions.class);
            if (result instanceof JavaObject) {
                runtime.getLoadService().require("java");
                result =
                    runtime.getClasses().getObjectClass().getConstant("JavaUtilities").callMethod(
                        "wrap",
                        new IRubyObject[] { result, RubyString.newString(runtime, BSFFunctions.class.getName())});
            }
            return result;
        }
        
        public IRubyObject setValue(IRubyObject value) {
            return value;
        }
    }

    /**
     * @see com.ibm.bsf.BSFEngine#terminate()
     */
    public void terminate() {
        runtime.dispose();
        runtime = null;
        super.terminate();
    }
}
