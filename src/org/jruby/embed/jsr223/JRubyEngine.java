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
 * Copyright (C) 2009-2010 Yoko Harada <yokolet@gmail.com>
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
package org.jruby.embed.jsr223;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.jruby.embed.EmbedEvalUnit;
import org.jruby.embed.ScriptingContainer;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Implementation of javax.script.ScriptEngine/Compilable/Invocable.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class JRubyEngine implements Compilable, Invocable, ScriptEngine {

    private final ScriptingContainer container;
    private JRubyEngineFactory factory;
    private JRubyContext jrubyContext;

    JRubyEngine(ScriptingContainer container, JRubyEngineFactory factory) {
        this.container = container;
        this.factory = factory;
        jrubyContext = new JRubyContext(container);
    }

    public CompiledScript compile(String script) throws ScriptException {
        if (script == null) {
            throw new NullPointerException("script is null");
        }
        return new JRubyCompiledScript(container, this, script);
    }

    public CompiledScript compile(Reader reader) throws ScriptException {
        if (reader == null) {
            throw new NullPointerException("reader is null");
        }
        return new JRubyCompiledScript(container, this, reader);
    }

    public Object eval(String script, ScriptContext context) throws ScriptException {
        if (script == null || context == null) {
            throw new NullPointerException("either script or context is null");
        }
        JRubyContext tmpContext = JRubyContext.convert(container, context);
        container.setScriptFilename(Utils.getFilename(tmpContext));
        try {
            Utils.preEval(container, tmpContext);
            EmbedEvalUnit unit = container.parse(script, Utils.getLineNumber(tmpContext));
            IRubyObject ret = unit.run();
            return JavaEmbedUtils.rubyToJava(ret);
        } catch (Exception e) {
            throw wrapException(e);
        } finally {
            if(Utils.isTerminationOn(tmpContext)) {
                container.terminate();
            }
            Utils.postEval(container, tmpContext);
            if (tmpContext != context) JRubyContext.update(tmpContext, context);
        }
    }

    private ScriptException wrapException(Exception e) {
        if (e.getCause() instanceof Exception) {
            Writer w = container.getErrorWriter();
            if (w instanceof PrintWriter) {
                e.printStackTrace((PrintWriter) w);
            } else {
                try {
                    w.write(e.getMessage());
                } catch (IOException ex) {
                    return new ScriptException(ex);
                }
            }
            return new ScriptException((Exception) e.getCause());
        } else {
            return new ScriptException(e);
        }
    }

    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        if (reader == null || context == null) {
            throw new NullPointerException("either reader or context is null");
        }
        JRubyContext tmpContext = JRubyContext.convert(container, context);
        String filename = Utils.getFilename(tmpContext);
        try {
            Utils.preEval(container, tmpContext);
            EmbedEvalUnit unit = container.parse(reader, filename, Utils.getLineNumber(tmpContext));
            IRubyObject ret = unit.run();
            return JavaEmbedUtils.rubyToJava(ret);
        } catch (Exception e) {
            throw wrapException(e);
        } finally {
            if(Utils.isTerminationOn(tmpContext)) {
                container.terminate();
            }
            Utils.postEval(container, tmpContext);
            if (tmpContext != context) JRubyContext.update(tmpContext, context);
        }
    }

    public Object eval(String script, Bindings bindings) throws ScriptException {
        JRubyContext context = getScriptContext(bindings);
        return eval(script, context);
    }

    public Object eval(Reader reader, Bindings bindings) throws ScriptException {
        JRubyContext context = getScriptContext(bindings);
        return eval(reader, context);
    }

    public Object eval(String script) throws ScriptException {
        return eval(script, jrubyContext);
    }

    public Object eval(Reader reader) throws ScriptException {
        return eval(reader, jrubyContext);
    }

    protected JRubyContext getScriptContext(Bindings bindings) {
        if (bindings == null) {
            throw new NullPointerException("null bindings in engine scope");
        }

        JRubyContext newContext = new JRubyContext(container);
        newContext.setEngineScopeBindings(bindings);
        Bindings global = getBindings(ScriptContext.GLOBAL_SCOPE);
        if (global != null) {
            newContext.setGlobalScopeBindings(global);
        }
        newContext.setReader(jrubyContext.getReader(), false);
        newContext.setWriter(jrubyContext.getWriter());
        newContext.setErrorWriter(jrubyContext.getErrorWriter());

        return newContext;
    }

    public Object get(String key) {
        return jrubyContext.getAttribute(key, ScriptContext.ENGINE_SCOPE);
    }

    public void put(String key, Object value) {
        jrubyContext.getEngineScopeBindings().put(key, value);
    }

    public Bindings getBindings(int scope) {
        return jrubyContext.getBindings(scope);
    }

    public void setBindings(Bindings bindings, int scope) {
        jrubyContext.setBindings(bindings, scope);
    }

    public Bindings createBindings() {
        return new SimpleBindings();
    }

    public ScriptContext getContext() {
        return jrubyContext;
    }

    public void setContext(ScriptContext ctx) {
        if (ctx == null) {
            throw new NullPointerException("context is null");
        }
        jrubyContext = JRubyContext.convert(container, ctx);
    }

    public ScriptEngineFactory getFactory() {
        return factory;
    }

    public Object invokeMethod(Object receiver, String method, Object... args)
            throws ScriptException, NoSuchMethodException {
        if (method == null) {
            throw new NullPointerException("method is null");
        }
        if (receiver == null) {
            throw new NullPointerException("receiver is null");
        }
        try {
            Utils.preEval(container, jrubyContext);
            if (args == null || args.length == 0) {
                return container.callMethod(receiver, method, Object.class);
            }
            return container.callMethod(receiver, method, args, Object.class);
        } catch (Exception e) {
            if (e.getCause().getMessage().contains("undefined method")) {
                throw wrapMethodException(e);
            }
            throw wrapException(e);
        } finally {
            Utils.postEval(container, jrubyContext);
        }
    }

    private NoSuchMethodException wrapMethodException(Exception e) {
        Writer w = container.getErrorWriter();
        if (w instanceof PrintWriter) {
            e.printStackTrace((PrintWriter) w);
        } else {
            try {
                w.write(e.getMessage());
            } catch (IOException ex) {
                return new NoSuchMethodException(ex.getMessage());
            }
        }
        return new NoSuchMethodException(e.getCause().getMessage());
    }

    public Object invokeFunction(String method, Object... args)
            throws ScriptException, NoSuchMethodException {
        if (method == null) {
            throw new NullPointerException("method is null");
        }
        try {
            Utils.preEval(container, jrubyContext);
            if (args == null || args.length == 0) {
                return container.callMethod(null, method, Object.class);
            }
            return container.callMethod(null, method, args, Object.class);
        } catch (Exception e) {
            if (e.getCause().getMessage().contains("undefined method")) {
                throw wrapMethodException(e);
            }
            throw wrapException(e);
        } finally {
            Utils.postEval(container, jrubyContext);
        }
    }

    public <T> T getInterface(Class<T> returnType) {
        return getInterface(null, returnType);
    }

    public <T> T getInterface(Object receiver, Class<T> returnType) {
        return container.getInstance(receiver, returnType);
    }
}