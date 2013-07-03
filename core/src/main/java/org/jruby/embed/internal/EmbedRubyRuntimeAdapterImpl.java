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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig.CompileMode;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.RubyString;
import org.jruby.ast.Node;
import org.jruby.ast.executable.Script;
import org.jruby.compiler.ASTInspector;
import org.jruby.embed.AttributeName;
import org.jruby.embed.EmbedEvalUnit;
import org.jruby.embed.EmbedRubyRuntimeAdapter;
import org.jruby.embed.ParseFailedException;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.io.ReaderInputStream;
import org.jruby.embed.util.SystemPropertyCatcher;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariable;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.javasupport.JavaEmbedUtils.EvalUnit;
import org.jruby.parser.StaticScope;
import org.jruby.parser.StaticScopeFactory;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.ManyVarsDynamicScope;

/**
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class EmbedRubyRuntimeAdapterImpl implements EmbedRubyRuntimeAdapter {
    private RubyRuntimeAdapter adapter = JavaEmbedUtils.newRuntimeAdapter();
    private ScriptingContainer container;

    public EmbedRubyRuntimeAdapterImpl(ScriptingContainer container) {
        this.container = container;
    }

    public EmbedEvalUnit parse(String script, int... lines) {
        if (script == null) {
            return null;
        }
        boolean unicode_escape = false;
        Object obj = container.getAttribute(AttributeName.UNICODE_ESCAPE);
        if (obj != null && obj instanceof Boolean) {
            unicode_escape = (Boolean)obj;
        }
        if (unicode_escape) {
            InputStream istream = new ReaderInputStream(new StringReader(script));
            return runParser(istream, null, lines);
        } else {
            return runParser(script, null, lines);
        }
    }

    public EmbedEvalUnit parse(Reader reader, String filename, int... lines) {
        if (reader != null) {
            InputStream istream = new ReaderInputStream(reader);
            return runParser(istream, filename, lines);
        } else {
            return null;
        }
    }

    public EmbedEvalUnit parse(PathType type, String filename, int... lines) {
        if (filename == null) {
            return null;
        }
        if (type == null) {
            type = PathType.ABSOLUTE;
        }
        InputStream istream = null;
        try {
            switch (type) {
                case ABSOLUTE:
                    istream = new FileInputStream(filename);
                    break;
                case RELATIVE:
                    String basedir = (String) container.getAttribute(AttributeName.BASE_DIR);
                    if (basedir == null) {
                        basedir = SystemPropertyCatcher.getBaseDir();
                    }
                    String absolutePath = basedir + File.separator + filename;
                    istream = new FileInputStream(absolutePath);
                    break;
                case CLASSPATH:
                    istream = container.getProvider().getRuntime().getJRubyClassLoader().getResourceAsStream(filename);
                    break;
            }
            return parse(istream, filename, lines);
        } catch (FileNotFoundException e) {
            throw new ParseFailedException(e);
        } finally {
            if (istream != null) {
                try {istream.close();} catch (IOException ioe) {}
            }
        }
    }

    public EmbedEvalUnit parse(InputStream istream, String filename, int... lines) {
        if (istream != null) {
            return runParser(istream, filename, lines);
        } else {
            return null;
        }
    }

    private EmbedEvalUnit runParser(Object input, String filename, int... lines) {
        if (input == null) {
            return null;
        }
        if (filename == null || filename.length() == 0) {
            filename = container.getScriptFilename();
        }
        Ruby runtime = container.getProvider().getRuntime();
        IAccessor d = new ValueAccessor(RubyString.newString(runtime, filename));
        runtime.getGlobalVariables().define("$PROGRAM_NAME", d, GlobalVariable.Scope.GLOBAL);
        runtime.getGlobalVariables().define("$0", d, GlobalVariable.Scope.GLOBAL);

        int line = 0;
        if (lines != null && lines.length > 0) {
            line = lines[0];
        }
        try {
            ManyVarsDynamicScope scope  = null;
            boolean sharing_variables = true;
            Object obj = container.getAttribute(AttributeName.SHARING_VARIABLES);
            if (obj != null && obj instanceof Boolean && ((Boolean) obj) == false) {
                sharing_variables = false;
            }
            if (sharing_variables) {
                scope = getManyVarsDynamicScope(container, 0);
            }
            Node node = null;
            if (input instanceof String) {
                node = runtime.parseEval((String)input, filename, scope, line);
            } else {
                node = runtime.parseFile((InputStream)input, filename, scope, line);
            }
            CompileMode compileMode = runtime.getInstanceConfig().getCompileMode();
            if (compileMode == CompileMode.FORCE) {
                // we pass in an ASTInspector that will force heap variables, so
                // it uses our shared scope
                ASTInspector inspector = new ASTInspector();
                inspector.setFlag(ASTInspector.SCOPE_AWARE);
                Script script = runtime.tryCompile(node, inspector);
                if (script != null) {
                    return new EmbedEvalUnitImpl(container, node, scope, script);
                } else {
                    return new EmbedEvalUnitImpl(container, node, scope);
                }
            }
            return new EmbedEvalUnitImpl(container, node, scope);
        } catch (RaiseException e) {
            runtime.printError(e.getException());
            throw new ParseFailedException(e.getMessage(), e);
        } catch (Throwable e) {
            throw new ParseFailedException(e);
        } finally {
            try {
                if (input instanceof InputStream) {
                    ((InputStream)input).close();
                }
            } catch (IOException ex) {
                throw new ParseFailedException(ex);
            }
        }
    }

    static ManyVarsDynamicScope getManyVarsDynamicScope(ScriptingContainer container, int depth) {
        ManyVarsDynamicScope scope;
        StaticScopeFactory scopeFactory = container.getProvider().getRuntime().getStaticScopeFactory();

        // root our parsing scope with a dummy scope
        StaticScope topStaticScope = scopeFactory.newLocalScope(null);
        topStaticScope.setModule(container.getProvider().getRuntime().getObject());

        DynamicScope currentScope = new ManyVarsDynamicScope(topStaticScope, null);
        String[] names4Injection = container.getVarMap().getLocalVarNames();
        StaticScope evalScope = names4Injection == null || names4Injection.length == 0 ?
                scopeFactory.newEvalScope(currentScope.getStaticScope()) :
                scopeFactory.newEvalScope(currentScope.getStaticScope(), names4Injection);
        scope = new ManyVarsDynamicScope(evalScope, currentScope);

        // JRUBY-5501: ensure we've set up a cref for the scope too
        scope.getStaticScope().determineModule();
        
        return scope;
    }

    public IRubyObject eval(Ruby runtime, String script) {
        return adapter.eval(runtime, script);
    }

    public EvalUnit parse(Ruby runtime, String script, String filename, int lineNumber) {
        return adapter.parse(runtime, script, filename, lineNumber);
    }

    public EvalUnit parse(Ruby runtime, InputStream istream, String filename, int lineNumber) {
        return adapter.parse(runtime, istream, filename, lineNumber);
    }
}
