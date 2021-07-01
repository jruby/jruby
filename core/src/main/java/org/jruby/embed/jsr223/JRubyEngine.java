/**
 * **** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2009-2012 Yoko Harada <yokolet@gmail.com>
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
package org.jruby.embed.jsr223;

import java.io.Reader;
import java.util.Objects;
import java.util.function.Supplier;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import org.jruby.embed.EmbedEvalUnit;
import org.jruby.embed.ScriptingContainer;
import org.jruby.exceptions.NoMethodError;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Implementation of javax.script.ScriptEngine/Compilable/Invocable.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class JRubyEngine implements Compilable, Invocable, ScriptEngine {

    final ScriptingContainer container;
    private final JRubyEngineFactory factory;
    private ScriptContext context;

    JRubyEngine(ScriptingContainer container, JRubyEngineFactory factory) {
        this.container = container;
        this.factory = factory;
        this.context = new JRubyContext(container);
    }

    @Override
    public CompiledScript compile(String script) throws ScriptException {
        Objects.requireNonNull(script, "script");
        try {
            return new JRubyCompiledScript(container, this, script);
        } catch (RaiseException e) {
            throw wrapRaiseException(e);
        }
    }

    @Override
    public CompiledScript compile(Reader reader) throws ScriptException {
        Objects.requireNonNull(reader, "reader");
        try {
            return new JRubyCompiledScript(container, this, reader);
        } catch (RaiseException e) {
            throw wrapRaiseException(e);
        }
    }

    static Object doEval(ScriptingContainer container, ScriptContext context, Supplier<EmbedEvalUnit> unit) throws ScriptException {

        if (Utils.isClearVariablesOn(context)) {
            container.clear();
        }
        Utils.preEval(container, context);
        try {
            IRubyObject ret = unit.get().run();
            return JavaEmbedUtils.rubyToJava(ret);
        } catch (RaiseException e) {
            throw wrapRaiseException(e);
        } catch (Exception e) {
            throw wrapException(e);
        } finally {
            Utils.postEval(container, context);
            if (Utils.isTerminationOn(context)) {
                container.terminate();
            }
        }
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
        Objects.requireNonNull(script, "script");
        Objects.requireNonNull(context, "context");
        container.setScriptFilename(Utils.getFilename(context));
        return doEval(container, context, () -> container.parse(script, Utils.getLineNumber(context)));
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        Objects.requireNonNull(reader, "reader");
        Objects.requireNonNull(context, "context");
        final String filename = Utils.getFilename(context);
        return doEval(container, context, () -> container.parse(reader, filename, Utils.getLineNumber(context)));
    }

    @Override
    public Object eval(String script, Bindings bindings) throws ScriptException {
        Objects.requireNonNull(script, "script");
        Objects.requireNonNull(bindings, "bindings");
        ScriptContext context = getScriptContext(bindings);
        return eval(script, context);
    }

    @Override
    public Object eval(Reader reader, Bindings bindings) throws ScriptException {
        Objects.requireNonNull(reader, "reader");
        Objects.requireNonNull(bindings, "bindings");
        ScriptContext context = getScriptContext(bindings);
        return eval(reader, context);
    }

    @Override
    public Object eval(String script) throws ScriptException {
        return eval(script, context);
    }

    @Override
    public Object eval(Reader reader) throws ScriptException {
        return eval(reader, context);
    }

    protected ScriptContext getScriptContext(final Bindings bindings) {
        if (bindings == null) return context;

        ScriptContext newContext = new SimpleScriptContext();
        newContext.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        Bindings global = getBindings(ScriptContext.GLOBAL_SCOPE);
        if (global != null) {
            newContext.setBindings(global, ScriptContext.GLOBAL_SCOPE);
        }
        newContext.setReader(context.getReader());
        newContext.setWriter(context.getWriter());
        newContext.setErrorWriter(context.getErrorWriter());

        return newContext;
    }

    @Override
    public Object get(String key) {
        return context.getAttribute(key, ScriptContext.ENGINE_SCOPE);
    }

    @Override
    public void put(String key, Object value) {
        context.getBindings(ScriptContext.ENGINE_SCOPE).put(key, value);
    }

    @Override
    public Bindings getBindings(int scope) {
        return context.getBindings(scope);
    }

    @Override
    public void setBindings(Bindings bindings, int scope) {
        context.setBindings(bindings, scope);
    }

    @Override
    public Bindings createBindings() {
        return new SimpleBindings();
    }

    @Override
    public ScriptContext getContext() {
        return context;
    }

    @Override
    public void setContext(ScriptContext context) {
        Objects.requireNonNull(context, "context");
        this.context = context;
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return factory;
    }

    @Override
    public Object invokeMethod(Object receiver, String method, Object... args) throws ScriptException, NoSuchMethodException {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(receiver, "receiver");
        Utils.preEval(container, context);
        try {
            if (args == null || args.length == 0) {
                return container.callMethod(receiver, method, Object.class);
            }
            return container.callMethod(receiver, method, args, Object.class);
        } catch (NoMethodError e) {
            throw wrapMethodException(e);
        } catch (RaiseException e) {
            throw wrapRaiseException(e);
        } catch (Exception e) {
            throw wrapException(e);
        } finally {
            Utils.postEval(container, context);
        }
    }

    @Override
    public Object invokeFunction(String method, Object... args) throws ScriptException, NoSuchMethodException {
        Objects.requireNonNull(method, "method");
        Utils.preEval(container, context);
        try {
            IRubyObject receiver = container.getProvider().getRuntime().getTopSelf();
            if (args == null || args.length == 0) {
                return container.callMethod(receiver, method, Object.class);
            }
            return container.callMethod(receiver, method, args, Object.class);
        } catch (NoMethodError e) {
            throw wrapMethodException(e);
        } catch (RaiseException e) {
            throw wrapRaiseException(e);
        } catch (Exception e) {
            throw wrapException(e);
        } finally {
            Utils.postEval(container, context);
        }
    }

    public <T> T getInterface(Class<T> returnType) {
        return getInterface(null, returnType);
    }

    public <T> T getInterface(Object receiver, Class<T> returnType) {
        return container.getInstance(receiver, returnType);
    }

    private static ScriptException wrapException(Exception e) {
        return new ScriptException(e);
    }

    private static ScriptException wrapRaiseException(RaiseException e) {
        RubyStackTraceElement[] trace = e.getException().getBacktraceElements();

        if (trace.length > 0) {
            RubyStackTraceElement top = trace[0];

            String file = top.getFileName();
            int line = top.getLineNumber();

            if (file == null) file = "<script>";

            ScriptException se = new ScriptException("Error during evaluation of Ruby in " + file + " at line " + line + ": " + e.getMessage());
            se.initCause(e);
            return se;
        }

        return new ScriptException(e);
    }

    private static NoSuchMethodException wrapMethodException(RaiseException e) {
        return (NoSuchMethodException) new NoSuchMethodException(e.getMessage()).initCause(e);
    }

}