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
package org.jruby.embed.jsr223;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.jruby.RubyNil;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.ScriptingContainer;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.javasupport.JavaEmbedUtils.EvalUnit;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class JRubyCompiledScript extends CompiledScript {
    private ScriptingContainer container;
    private JRubyEngine engine;
    private final EvalUnit unit;

    JRubyCompiledScript(ScriptingContainer container,
            JRubyEngine engine, String script) {
        this.container = container;
        this.engine = engine;
        unit = container.parse(script);
    }

    JRubyCompiledScript(ScriptingContainer container,
            JRubyEngine engine, Reader reader) {
        this.container = container;
        this.engine = engine;
        String filename = System.getProperty(ScriptEngine.FILENAME);
        unit = container.parse(reader, filename, Utils.getLineNumber(engine));
    }
    
    public Object eval(ScriptContext context) throws ScriptException {
        try {
            IRubyObject ret = unit.run();
            if (!(ret instanceof RubyNil)) {
                return JavaEmbedUtils.rubyToJava(ret);
            }
            return null;
        } catch (Exception e) {
            Writer w = container.getErrorWriter();
            if (w instanceof PrintWriter) {
                e.printStackTrace((PrintWriter)w);
            } else {
                try {
                    w.write(e.getMessage());
                } catch (IOException ex) {
                    throw new ScriptException(ex);
                }
            }
            if (e.getCause() instanceof Exception) {
                throw new ScriptException((Exception)e.getCause());
            } else {
                throw new ScriptException(e);
            }
        }
    }

    public ScriptEngine getEngine() {
        return engine;
    }
}
