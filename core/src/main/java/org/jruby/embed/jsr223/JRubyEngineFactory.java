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
package org.jruby.embed.jsr223;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.util.SystemPropertyCatcher;

/**
 * This class implements javax.script.ScriptEngineFactory.
 * 
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class JRubyEngineFactory implements ScriptEngineFactory {
    private final String engineName;
    private final String engineVersion;
    private final List<String> extensions;
    private final String languageName;
    //setting languageVersion in constructor causes runtime initialization before setting all configs.
    //changed to get info on demand.
    private String languageVersion;
    private final List<String> mimeTypes;
    private final List<String> engineIds;
    private Map<String, Object> parameters;

    public JRubyEngineFactory() {
        engineName = "JSR 223 JRuby Engine";
        engineVersion = org.jruby.runtime.Constants.VERSION;
        extensions = Collections.unmodifiableList(Arrays.asList(new String[]{"rb"}));
        languageName = "ruby";
        languageVersion = "jruby " + org.jruby.runtime.Constants.VERSION;
        mimeTypes = Collections.unmodifiableList(Arrays.asList(new String[]{"application/x-ruby"}));
        engineIds = Collections.unmodifiableList(Arrays.asList(new String[]{"ruby", "jruby"}));
        // does followings on demand to avoid runtime initialization
        //languageVersion = container.getSupportedRubyVersion();
    }

    private void initParameters() {
        parameters = new HashMap();
        parameters.put(ScriptEngine.ENGINE, getEngineName());
        parameters.put(ScriptEngine.ENGINE_VERSION, getEngineVersion());
        parameters.put(ScriptEngine.NAME, getEngineName());
        parameters.put(ScriptEngine.LANGUAGE, getLanguageName());
        parameters.put(ScriptEngine.LANGUAGE_VERSION, getLanguageVersion());
        parameters.put("THREADING", "THREAD-ISOLATED");
    }

    public String getEngineName() {
        return engineName;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public List getExtensions() {
        return extensions;
    }

    public String getLanguageName() {
        return languageName;
    }

    public String getLanguageVersion() {
        return languageVersion;
    }

    public String getMethodCallSyntax(String obj, String m, String... args) {
        if (m == null || m.length() == 0) {
            return "";
        }
        if (args == null || args.length == 0) {  // no argument
            if (obj == null || obj.length() == 0) {
                return MessageFormat.format("{0}", m); //top level method
            } else {
                return MessageFormat.format("{0}.{1}", obj, m);
            }
        } else {  // with argument(s)
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (String arg : args) {
                if (!first) {
                    builder.append(", ");
                }
                first = false;
                builder.append(arg);
            }
            if (obj == null || obj.length() == 0) {
                return MessageFormat.format("{0}({1})", m, builder.toString()); //top level method
            } else {
                return MessageFormat.format("{0}.{1}({2})", obj, m, builder.toString());
            }
        }
    }

    public List getMimeTypes() {
        return mimeTypes;
    }

    public List getNames() {
        return engineIds;
    }

    public String getOutputStatement(String toDisplay) {
        if (toDisplay == null || toDisplay.length() == 0) {
            return "";
        }
        return "puts " + toDisplay + "\nor\nprint " + toDisplay;
    }

    public Object getParameter(String key) {
        if (parameters == null) {
            initParameters();
        }
        return parameters.get(key);
    }

    public String getProgram(String... statements) {
        if (statements == null || statements.length == 0) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        for (String s : statements) {
            sb.append(s);
            sb.append("\n");
        }
        return new String(sb);
    }

    public ScriptEngine getScriptEngine() {
        LocalContextScope scope = SystemPropertyCatcher.getScope(LocalContextScope.SINGLETON);
        LocalVariableBehavior behavior = SystemPropertyCatcher.getBehavior(LocalVariableBehavior.GLOBAL);
        boolean lazy = SystemPropertyCatcher.isLazy(false);
        ScriptingContainer container = new ScriptingContainer(scope, behavior, lazy);
        SystemPropertyCatcher.setClassLoader(container);
        SystemPropertyCatcher.setConfiguration(container);
        JRubyEngine engine = new JRubyEngine(container, this);
        return (ScriptEngine)engine;
    }

}
