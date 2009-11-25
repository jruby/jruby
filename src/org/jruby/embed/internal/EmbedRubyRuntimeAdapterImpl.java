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
import org.jruby.embed.AttributeName;
import org.jruby.embed.EmbedEvalUnit;
import org.jruby.embed.EmbedRubyRuntimeAdapter;
import org.jruby.embed.ParseFailedException;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.io.ReaderInputStream;
import org.jruby.embed.util.SystemPropertyCatcher;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.javasupport.JavaEmbedUtils.EvalUnit;
import org.jruby.parser.EvalStaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.ThreadContext;
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
        try {
            InputStream istream = null;
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
                    istream = container.getRuntime().getJRubyClassLoader().getResourceAsStream(filename);
                    break;
            }
            return parse(istream, filename, lines);
        } catch (FileNotFoundException e) {
            Writer w = container.getErrorWriter();
            if (w instanceof PrintWriter) {
                e.printStackTrace((PrintWriter)w);
            } else {
                try {
                    w.write(e.getMessage());
                } catch (IOException ex) {
                    throw new ParseFailedException(ex);
                }
            }
            throw new ParseFailedException(e);
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
            filename = "<script>";
        }
        IAccessor d = new ValueAccessor(RubyString.newString(container.getRuntime(), filename));
        container.getRuntime().getGlobalVariables().define("$PROGRAM_NAME", d);
        container.getRuntime().getGlobalVariables().define("$0", d);

        int line = 0;
        if (lines != null && lines.length > 0) {
            line = lines[0];
        }
        try {
            Ruby runtime = container.getRuntime();
            ManyVarsDynamicScope scope = getManyVarsDynamicScope(runtime, 0);
            Node node = null;
            if (input instanceof String) {
                node = container.getRuntime().parseEval((String)input, filename, scope, line);
            } else {
                node = container.getRuntime().parseFile((InputStream)input, filename, scope, line);
            }
            CompileMode compileMode = runtime.getInstanceConfig().getCompileMode();
            if (compileMode == CompileMode.JIT || compileMode == CompileMode.FORCE) {
                Script script = runtime.tryCompile(node);
                return new EmbedEvalUnitImpl(container, node, scope, script);
            }
            return new EmbedEvalUnitImpl(container, node, scope);
        } catch (RaiseException e) {
            container.getRuntime().printError(e.getException());
            throw new ParseFailedException(e.getMessage(), e);
        } catch (Throwable e) {
            Writer w = container.getErrorWriter();
            if (w instanceof PrintWriter) {
                e.printStackTrace((PrintWriter)w);
            } else {
                try {
                    w.write(e.getMessage());
                } catch (IOException ex) {
                    throw new ParseFailedException(ex);
                }
            }
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

    ManyVarsDynamicScope getManyVarsDynamicScope(Ruby runtime, int depth) {
        ManyVarsDynamicScope scope;
        ThreadContext context = runtime.getCurrentContext();
        DynamicScope currentScope = context.getCurrentScope();
        String[] names4Injection = container.getVarMap().getLocalVarNames();
        if (names4Injection == null || names4Injection.length == 0) {
            scope =
                new ManyVarsDynamicScope(new EvalStaticScope(currentScope.getStaticScope()), currentScope);
        } else {
            scope =
                new ManyVarsDynamicScope(new EvalStaticScope(currentScope.getStaticScope(), names4Injection), currentScope);
        }
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
