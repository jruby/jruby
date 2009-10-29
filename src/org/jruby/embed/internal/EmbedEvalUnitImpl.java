/**
 * **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2009 Yoko Harada <yokolet@gmail.com>
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
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import org.jruby.RubyInstanceConfig.CompileMode;
import org.jruby.ast.Node;
import org.jruby.ast.executable.Script;
import org.jruby.embed.EmbedEvalUnit;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.ScriptingContainer;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.ManyVarsDynamicScope;

/**
 * Implementation of org.jruby.javasupport.JavaEmbedUtils.EvalUnit for Embeddiing.
 * This class is created when a Ruby script has been parsed. Once parsed, the script
 * is ready to run many times without parsing.
 *
 * <p>Users do not instantiate explicitly. Instead, they can get the instance by parsing
 * Ruby script by parse method of {@link ScriptingContainer}.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class EmbedEvalUnitImpl implements EmbedEvalUnit {
    private ScriptingContainer container;
    private Node node;
    private ManyVarsDynamicScope scope;
    private Script script;

    public EmbedEvalUnitImpl(ScriptingContainer container, Node node, ManyVarsDynamicScope scope) {
        this(container, node, scope, null);
    }

    public EmbedEvalUnitImpl(ScriptingContainer container, Node node, ManyVarsDynamicScope scope, Script script) {
        this.container = container;
        this.node = node;
        this.scope = scope;
        this.script = script;
    }

    /**
     * Returns a root node of parsed Ruby script.
     *
     * @return a root node of parsed Ruby script
     */
    public Node getNode() {
        return node;
    }

    /**
     * Returns a ManyVarsDynamicScope used to parse a script. A returned value
     * is used to inject Ruby's local variable when script is evaluated.
     * 
     * @return a scope to refer local variables
     */
    public ManyVarsDynamicScope getScope() {
        return scope;
    }

    /**
     * Evaluates a Ruby script, which has been parsed before.
     * 
     * @return results of executing this evaluation unit
     */
    public IRubyObject run() {
        if (node == null && script == null) {
            return null;
        }
        BiVariableMap vars = container.getVarMap();
        try {
            vars.inject(scope, 0, null);
            container.getRuntime().getCurrentContext().pushScope(scope);
            IRubyObject ret;
            CompileMode mode = container.getRuntime().getInstanceConfig().getCompileMode();
            if (mode == CompileMode.FORCE || mode == CompileMode.JIT) {
                ret = container.getRuntime().runScript(script);
            } else {
                ret = container.getRuntime().runInterpreter(node);
            }
            vars.retrieve(ret);
            return ret;
        } catch (RaiseException e) {
            container.getRuntime().printError(e.getException());
            throw new EvalFailedException(e.getMessage(), e);
        } catch (StackOverflowError soe) {
            throw container.getRuntime().newSystemStackError("stack level too deep", soe);
        } catch (Throwable e) {
            Writer w = container.getErrorWriter();
            if (w instanceof PrintWriter) {
                e.printStackTrace((PrintWriter)w);
            } else {
                try {
                    w.write(e.getMessage());
                } catch (IOException ex) {
                    throw new EvalFailedException(ex);
                }
            }
            throw new EvalFailedException(e);
        } finally {
            container.getRuntime().getCurrentContext().popScope();
            JavaEmbedUtils.terminate(container.getRuntime());
            vars.terminate();
            /* Below lines doesn't work. Neither does classCache.flush(). How to clear cache?
            ClassCache classCache = JavaEmbedUtils.createClassCache(getRuntime().getClassLoader());
            getRuntime().getInstanceConfig().setClassCache(classCache);
            */
        }
    }
}
