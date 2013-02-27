/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2008 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
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
import org.jruby.RubyRuntimeAdapter;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.GlobalVariable;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/** 
 * An implementation of a JRuby BSF implementation.
 */
public class JRubyEngine extends BSFEngineImpl {
    private Ruby runtime;
    private RubyRuntimeAdapter evaler = JavaEmbedUtils.newRuntimeAdapter();

    @Override
    public Object apply(String file, int line, int col, Object funcBody, Vector paramNames, Vector args) {
        ThreadContext context = runtime.getCurrentContext();
        try {
            // Construct local variables based on parameter names passed in
            String[] names = new String[paramNames.size()];
            paramNames.toArray(names);
            context.preBsfApply(names);

            // Populate values for the parameter names
            DynamicScope scope = context.getCurrentScope();
            for (int i = 0, size = args.size(); i < size; i++) {
                scope.setValue(i, JavaEmbedUtils.javaToRuby(runtime, args.get(i)), 0);
            }

            return JavaEmbedUtils.rubyToJava(evaler.parse(runtime, funcBody.toString(), file, line).run());
        } catch (StackOverflowError soe) {
            throw context.runtime.newSystemStackError("stack level too deep", soe);
        } finally {
            context.postBsfApply();
        }
    }

    public Object eval(String file, int line, int col, Object expr) throws BSFException {
        try {
            return JavaEmbedUtils.rubyToJava(evaler.parse(runtime, expr.toString(), file, line).run());
        } catch (Exception excptn) {
            throw new BSFException(BSFException.REASON_EXECUTION_ERROR, "Exception", excptn);
        }
    }

    @Override
    public void exec(String file, int line, int col, Object expr) throws BSFException {
        try {
            evaler.parse(runtime, expr.toString(), file, line).run();
        } catch (Exception excptn) {
            throw new BSFException(BSFException.REASON_EXECUTION_ERROR, "Exception", excptn);
        }
    }

    public Object call(Object recv, String method, Object[] args) throws BSFException {
        try {
        	return JavaEmbedUtils.invokeMethod(runtime, recv, method, args, Object.class);
        } catch (Exception excptn) {
            throw new BSFException(BSFException.REASON_EXECUTION_ERROR, excptn.getMessage(), excptn);
        }
    }

    @Override
    public void initialize(BSFManager manager, String language, Vector someDeclaredBeans) throws BSFException {
        super.initialize(manager, language, someDeclaredBeans);

        runtime = JavaEmbedUtils.initialize(getClassPath(manager));

        for (int i = 0, size = someDeclaredBeans.size(); i < size; i++) {
            BSFDeclaredBean bean = (BSFDeclaredBean) someDeclaredBeans.elementAt(i);
            runtime.getGlobalVariables().define(
                    GlobalVariable.variableName(bean.name),
                    new BeanGlobalVariable(runtime, bean),
                    org.jruby.internal.runtime.GlobalVariable.Scope.GLOBAL);
        }

        runtime.getGlobalVariables().defineReadonly("$bsf", new FunctionsGlobalVariable(runtime, new BSFFunctions(manager, this)), org.jruby.internal.runtime.GlobalVariable.Scope.GLOBAL);
    }
    
    private List getClassPath(BSFManager manager) {
    	return Arrays.asList(manager.getClassPath().split(System.getProperty("path.separator")));
    }

    @Override
    public void declareBean(BSFDeclaredBean bean) throws BSFException {
        runtime.getGlobalVariables().define(
                GlobalVariable.variableName(bean.name),
                new BeanGlobalVariable(runtime, bean),
                org.jruby.internal.runtime.GlobalVariable.Scope.GLOBAL);
    }

    @Override
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
            JumpException je = (JumpException)exception;
            if (je instanceof RaiseException) {
                runtime.printError(((RaiseException)je).getException());
            } else if (je instanceof JumpException.BreakJump) {
                runtime.getErrorStream().println("break without block.");
            } else if (je instanceof JumpException.ReturnJump) {
                runtime.getErrorStream().println("return without block.");
            }
    	}
    }

    private static class BeanGlobalVariable implements IAccessor {
        private final Ruby runtime;
        private final BSFDeclaredBean bean;

        public BeanGlobalVariable(Ruby runtime, BSFDeclaredBean bean) {
            this.runtime = runtime;
            this.bean = bean;
        }

        public IRubyObject getValue() {
            IRubyObject result = JavaUtil.convertJavaToRuby(runtime, bean.bean, bean.type);
            
            return result instanceof JavaObject ? Java.wrap(runtime, result) : result;
        }

        public IRubyObject setValue(IRubyObject value) {
            bean.bean = value.toJava(Object.class);
            return value;
        }
    }

    private static class FunctionsGlobalVariable implements IAccessor {
        private final Ruby runtime;
        private final BSFFunctions functions;

        public FunctionsGlobalVariable(Ruby runtime, BSFFunctions functions) {
            this.runtime = runtime;
            this.functions = functions;
        }

        public IRubyObject getValue() {
            IRubyObject result = JavaUtil.convertJavaToRuby(runtime, functions, BSFFunctions.class);

            return result instanceof JavaObject ? Java.wrap(runtime, result) : result;
        }

        public IRubyObject setValue(IRubyObject value) {
            return value;
        }
    }

    /**
     * @see org.apache.bsf.BSFEngine#terminate()
     */
    @Override
    public void terminate() {
    	JavaEmbedUtils.terminate(runtime);
        runtime = null;
        super.terminate();
    }
}
