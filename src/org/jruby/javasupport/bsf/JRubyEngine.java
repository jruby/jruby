/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.javasupport.bsf;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.apache.bsf.BSFDeclaredBean;
import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.apache.bsf.util.BSFEngineImpl;
import org.apache.bsf.util.BSFFunctions;
import org.jruby.Ruby;
import org.jruby.ast.Node;
import org.jruby.exceptions.BreakJump;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.ReturnJump;
import org.jruby.exceptions.ThrowJump;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Frame;
import org.jruby.runtime.GlobalVariable;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.Scope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

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
            threadContext.getFrameStack().push(new Frame(threadContext));
            threadContext.pushDynamicVars();
            Scope scope = (Scope) threadContext.getScopeStack().push(new Scope(runtime, paramNames));

            // set global variables
            for (int i = 0, size = args.size(); i < size; i++) {
                scope.setValue(i, convertToRuby(args.get(i)));
            }

            runtime.setPosition(file, line);

            Node node = runtime.getParser().parse(file, funcBody.toString());
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

            // Create Ruby proxies for any input arguments that are not primitives.
            IRubyObject javaUtilities = runtime.getClasses().getObjectClass().getConstant("JavaUtilities");
            for (int i = 0; i < rubyArgs.length; i++) {
                IRubyObject obj = rubyArgs[i];

                if (obj instanceof JavaObject) {
                    rubyArgs[i] = javaUtilities.callMethod("wrap", new IRubyObject[] {
                        obj, runtime.newString(obj.getClass().getName())
                    });
                }
            }

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
                    new IRubyObject[] { result, runtime.newString(value.getClass().getName())});
        }
        return result;
    }

    private Object convertToJava(IRubyObject value, Class type) {
        runtime.getLoadService().require("java");
        if (value.isKindOf(runtime.getModule("JavaProxy"))) {
        	IRubyObject variable = value.getInstanceVariable("@java_object"); 
            value = variable == null ? runtime.getNil() : variable;
        }
        value = Java.primitive_to_java(value, value);
        return JavaUtil.convertArgument(value, type);
    }

    public void initialize(BSFManager mgr, String lang, Vector declaredBeans) throws BSFException {
        super.initialize(mgr, lang, declaredBeans);

        runtime = Ruby.getDefaultInstance();
        runtime.getLoadService().init(getClassPath(mgr));

        for (int i = 0, size = declaredBeans.size(); i < size; i++) {
            BSFDeclaredBean bean = (BSFDeclaredBean) declaredBeans.elementAt(i);
            runtime.getGlobalVariables().define(
                GlobalVariable.variableName(bean.name),
                new BeanGlobalVariable(runtime, bean));
        }

        runtime.getGlobalVariables().defineReadonly("$bsf", new FunctionsGlobalVariable(runtime, new BSFFunctions(mgr, this)));
    }
    
    private List getClassPath(BSFManager mgr) {
    	return Arrays.asList(mgr.getClassPath().split(System.getProperty("path.separator")));
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
    private static void printException(Ruby runtime, Exception exception) {
        if (exception instanceof RaiseException) {
            runtime.printError(((RaiseException) exception).getException());
        } else if (exception instanceof ThrowJump) {
            runtime.printError(((ThrowJump) exception).getNameError());
        } else if (exception instanceof BreakJump) {
            runtime.getErrorStream().println("break without block.");
        } else if (exception instanceof ReturnJump) {
            runtime.getErrorStream().println("return without block.");
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
                        new IRubyObject[] { result, runtime.newString(bean.type.getName())});
            }
            return result;
        }

        public IRubyObject setValue(IRubyObject value) {
            runtime.getLoadService().require("java");
            if (value.isKindOf(runtime.getModule("JavaProxy"))) {
            	IRubyObject variable = value.getInstanceVariable("@java_object"); 
                value = variable == null ? runtime.getNil() : variable;
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
                        new IRubyObject[] { result, runtime.newString(BSFFunctions.class.getName())});
            }
            return result;
        }

        public IRubyObject setValue(IRubyObject value) {
            return value;
        }
    }

    /**
     * @see org.apache.bsf.BSFEngine#terminate()
     */
    public void terminate() {
        runtime.tearDown();
        runtime.getThreadService().disposeCurrentThread();
        runtime = null;
        super.terminate();
    }
}
