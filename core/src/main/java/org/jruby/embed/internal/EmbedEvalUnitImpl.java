/**
 * **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2009-2010 Yoko Harada <yokolet@gmail.com>
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
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed.internal;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig.CompileMode;
import org.jruby.RubySystemExit;
import org.jruby.ast.Node;
import org.jruby.ast.executable.Script;
import org.jruby.embed.AttributeName;
import org.jruby.embed.EmbedEvalUnit;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.ScriptingContainer;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.ManyVarsDynamicScope;

/**
 * Implementation of org.jruby.javasupport.JavaEmbedUtils.EvalUnit for embeddiing.
 * This class is created when a Ruby script has been parsed. Once parsed, the script
 * is ready to run many times without parsing.
 *
 * <p>Users do not instantiate explicitly. Instead, they can get the instance by parsing
 * Ruby script by parse method of {@link ScriptingContainer}.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class EmbedEvalUnitImpl implements EmbedEvalUnit {

    private final ScriptingContainer container;
    private final Node node;
    private final ManyVarsDynamicScope scope;
    private final Script script;

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
        final Ruby runtime = container.getProvider().getRuntime();
        final BiVariableMap vars = container.getVarMap();
        final boolean sharing_variables = isSharingVariables();

        // Keep reference to current context to prevent it being collected.
        final ThreadContext threadContext = runtime.getCurrentContext();
        if (sharing_variables) {
            vars.inject(scope, 0, null);
            threadContext.pushScope(scope);
        }
        try {
            final IRubyObject ret;
            CompileMode mode = runtime.getInstanceConfig().getCompileMode();
            if (mode == CompileMode.FORCE) {
                ret = runtime.runScriptBody(script);
            } else {
                ret = runtime.runInterpreter(node);
            }
            if (sharing_variables) {
                vars.retrieve(ret);
            }
            return ret;
        }
        catch (RaiseException e) {
            // handle exits as simple script termination
            if ( e.getException() instanceof RubySystemExit ) {
                return ((RubySystemExit) e.getException()).status();
            }
            runtime.printError(e.getException());
            throw new EvalFailedException(e.getMessage(), e);
        }
        catch (StackOverflowError soe) {
            throw runtime.newSystemStackError("stack level too deep", soe);
        }
        catch (Throwable e) {
            throw new EvalFailedException(e);
        }
        finally {
            if (sharing_variables) {
                threadContext.popScope();
            }
            vars.terminate();
            /* Below lines doesn't work. Neither does classCache.flush(). How to clear cache?
            ClassCache classCache = JavaEmbedUtils.createClassCache(getRuntime().getClassLoader());
            getRuntime().getInstanceConfig().setClassCache(classCache);
            */
        }
    }

    private boolean isSharingVariables() {
        final Object sharing = container.getAttribute(AttributeName.SHARING_VARIABLES);
        if ( sharing != null && sharing instanceof Boolean &&
                ((Boolean) sharing).booleanValue() == false ) {
            return false;
        }
        return true;
    }

}
