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
 * Copyright (C) 2009-2011 Yoko Harada <yokolet@gmail.com>
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
 ***** END LICENSE BLOCK ****
 */
package org.jruby.embed.bsf;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.apache.bsf.BSFDeclaredBean;
import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.apache.bsf.util.BSFEngineImpl;
import org.apache.bsf.util.BSFFunctions;
import org.jruby.CompatVersion;
import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.embed.EmbedRubyObjectAdapter;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.util.SystemPropertyCatcher;
import org.jruby.embed.variable.BiVariable;
import org.jruby.embed.variable.VariableInterceptor;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariable;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.javasupport.JavaEmbedUtils.EvalUnit;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/** 
 * An implementation of a JRuby BSF engine.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class JRubyEngine extends BSFEngineImpl {
    private ScriptingContainer container;

    @Override
    public Object apply(String file, int line, int col, Object funcBody, Vector paramNames, Vector args) {
        ThreadContext context = container.getProvider().getRuntime().getCurrentContext();
        try {
            if (paramNames != null && args != null) {
                for (int i = 0; i < paramNames.size(); i++) {
                    Object o = paramNames.get(i);
                    if (o instanceof String) {
                        String name = (String) o;
                        container.put(name, args.get(i));
                    }
                }
            }
            return run(file, line, funcBody);
        } catch (StackOverflowError soe) {
            throw context.runtime.newSystemStackError("stack level too deep", soe);
        }
    }

    private Object run(String fileinfo, int line, Object scriptUnit) {
        EvalUnit unit = null;
        if (scriptUnit instanceof String) {
            unit = container.parse(scriptUnit.toString(), line);
        } else if (scriptUnit instanceof Reader) {
            unit = container.parse((Reader)scriptUnit, fileinfo, line);
        } else if (scriptUnit instanceof InputStream) {
            unit = container.parse((InputStream)scriptUnit, fileinfo, line);
        } else if (scriptUnit instanceof PathType) {
            unit = container.parse((PathType)scriptUnit, fileinfo, line);
        }
        if (unit == null) {
            return null;
        }
        IRubyObject ret = unit.run();
        return JavaEmbedUtils.rubyToJava(ret);
    }

    public Object eval(String file, int line, int col, Object expr) throws BSFException {
        try {
            return run(file, line, expr);
        } catch (Exception excptn) {
            throw new BSFException(BSFException.REASON_EXECUTION_ERROR, "Exception", excptn);
        }
    }

    @Override
    public void exec(String file, int line, int col, Object expr) throws BSFException {
        try {
            run(file, line, expr);
        } catch (Exception excptn) {
            throw new BSFException(BSFException.REASON_EXECUTION_ERROR, "Exception", excptn);
        }
    }

    public Object call(Object recv, String method, Object[] args) throws BSFException {
        try {
            EmbedRubyObjectAdapter adapter = container.newObjectAdapter();
            if (args == null) {
                return adapter.callMethod(recv, method, Object.class);
            } else {
                return adapter.callMethod(recv, method, args, Object.class);
            }
        } catch (Exception excptn) {
            throw new BSFException(BSFException.REASON_EXECUTION_ERROR, excptn.getMessage(), excptn);
        }
    }

    @Override
    public void initialize(BSFManager manager, String language, Vector someDeclaredBeans) throws BSFException {
        super.initialize(manager, language, someDeclaredBeans);
        LocalContextScope scope = SystemPropertyCatcher.getScope(LocalContextScope.SINGLETON);
        LocalVariableBehavior behavior = LocalVariableBehavior.BSF;
        container = new ScriptingContainer(scope, behavior);
        SystemPropertyCatcher.setConfiguration(container);
        //container.getProvider().setLoadPaths(getClassPath(manager));
        if (!SystemPropertyCatcher.isRuby19(language)) {
            container.getProvider().getRubyInstanceConfig().setCompatVersion(CompatVersion.RUBY1_8);
        }
        Ruby runtime = container.getProvider().getRuntime();

        if (someDeclaredBeans != null && someDeclaredBeans.size() > 0) {
            for (int i = 0; i < someDeclaredBeans.size(); i++) {
                BSFDeclaredBean bean = (BSFDeclaredBean) someDeclaredBeans.get(i);
                setVariable(bean);
            }
        }
        runtime.getGlobalVariables().defineReadonly("$bsf", new FunctionsGlobalVariable(runtime, new BSFFunctions(manager, this)), GlobalVariable.Scope.GLOBAL);
    }

    private void setVariable(BSFDeclaredBean bean) {
        String name = bean.name;
        if ("$bsf".equals(name)) {
            return;
        }
        if (!name.startsWith("$")) {
            name = "$".concat(name);
        }
        RubyObject receiver = (RubyObject)container.getProvider().getRuntime().getTopSelf();
        BiVariable v =
            VariableInterceptor.getVariableInstance(LocalVariableBehavior.BSF,receiver, name, bean.bean, bean.type);
        container.getVarMap().setVariable(receiver, v);
    }

    private List<String> getClassPath(BSFManager manager) {
        String classpath = manager.getClassPath();
        String s = System.getProperty("org.jruby.embed.class.path");
        if (s != null) {
            classpath = classpath + File.pathSeparator + s;
        }
    	return Arrays.asList(classpath.split(System.getProperty("path.separator")));
    }

    @Override
    public void declareBean(BSFDeclaredBean bean) throws BSFException {
        assert bean != null;
        setVariable(bean);
    }

    @Override
    public void undeclareBean(BSFDeclaredBean bean) throws BSFException {
        assert bean != null;
        String name = bean.name;
        if (!name.startsWith("$")) {
            name = "$".concat(name);
        }
        container.getVarMap().remove(name);
    }

    public void handleException(BSFException bsfExcptn) {
        printException(container.getProvider().getRuntime(), (Exception) bsfExcptn.getTargetException());
    }

    /**
     *
     * Prints out an error message.
     *
     * @param exception An Exception thrown by JRuby
     */
    private static void printException(Ruby runtime, Exception exception) {
        assert exception != null;
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
        container.getVarMap().clear();
        container.terminate();
        super.terminate();
    }
}
