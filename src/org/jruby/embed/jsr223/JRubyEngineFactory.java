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

import java.text.MessageFormat;
import java.util.ArrayList;
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
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class JRubyEngineFactory implements ScriptEngineFactory {
    private static final String jsr223Props = "org/jruby/embed/jsr223/Jsr223JRubyEngine.properties";
    private final ScriptingContainer container;
    private final String engineName;
    private final String engineVersion;
    private final List<String> extensions;
    private final String languageName;
    private final String languageVersion;
    private final List<String> mimeTypes;
    private final List<String> engineIds;
    private final Map<String, Object> parameters;
    //private final ScriptEngine engine;

    public JRubyEngineFactory() {
        LocalContextScope scope = SystemPropertyCatcher.getScope(LocalContextScope.THREADSAFE);
        LocalVariableBehavior behavior = SystemPropertyCatcher.getBehavior(LocalVariableBehavior.GLOBAL);
        container = new ScriptingContainer(scope, behavior, jsr223Props);
        SystemPropertyCatcher.setConfiguration(container);
        
        engineName = getSingleValue("engine.name").trim();
        engineVersion = getSingleValue("engine.version").trim();
        extensions = Collections.unmodifiableList(getMultipleValue("language.extension"));
        languageName = getSingleValue("language.name").trim();
        languageVersion = container.getSupportedRubyVersion();
        mimeTypes = Collections.unmodifiableList(getMultipleValue("language.mimetypes"));
        engineIds = Collections.unmodifiableList(getMultipleValue("engine.ids"));
        parameters = getParameters();
    }

    private String getSingleValue(String key) {
        String[] array = container.getProperty(key);
        if (array == null) {
            throw new NullPointerException(key + "is not defined");
        }
        return array[0];
    }

    private List getMultipleValue(String key) {
        String[] array = container.getProperty(key);
        if (array == null) {
            throw new NullPointerException(key + "is not defined");
        }
        List list = new ArrayList();
        for (String s : array) {
            list.add(s);
        }
        return list;
    }

    private Map<String, Object> getParameters() {
        Map map = new HashMap();
        map.put(ScriptEngine.ENGINE, getEngineName());
        map.put(ScriptEngine.ENGINE_VERSION, getEngineVersion());
        map.put(ScriptEngine.NAME, getEngineName());
        map.put(ScriptEngine.LANGUAGE, getLanguageName());
        map.put(ScriptEngine.LANGUAGE_VERSION, getLanguageVersion());
        map.put("THREADING", "THREAD-ISOLATED");
        return map;
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
            String argsString = "";
            for (String arg : args) {
                argsString += arg + ", ";
            }
            argsString = argsString.substring(0, argsString.length()-2);
            if (obj == null || obj.length() == 0) {
                return MessageFormat.format("{0}({1})", m, argsString); //top level method
            } else {
                return MessageFormat.format("{0}.{1}({2})", obj, m, argsString);
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
        JRubyEngine engine = new JRubyEngine(container, this);
        return (ScriptEngine)engine;
    }

}
