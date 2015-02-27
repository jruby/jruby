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
 * Copyright (C) 2009 Yoko Harada <yokolet@gmail.com>
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

/**
 * This is a substitute of javax.script.ScriptEngineManager.
 *
 * With this script engine manager, you can avoid two known troubles. One this
 * happens on OS X JDK 5 which tries to load AppleScriptEngine and ends up in the
 * exception. Another one happens when you use livetribe version of javax.script
 * and GLOBAL_SCOPE. The livetribe javax.script has a bug to handle GLOBAL_SCOPE.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class JRubyScriptEngineManager {

    static final String SERVICE = "META-INF/services/javax.script.ScriptEngineFactory";

    private final Collection<ScriptEngineFactory> factories;
    private final Map<String, ScriptEngineFactory> nameMap;
    private final Map<String, ScriptEngineFactory> extensionMap;
    private final Map<String, ScriptEngineFactory> mimetypeMap;
    private Bindings globalMap;

    public JRubyScriptEngineManager() throws ScriptException {
        this(null);
    }

    public JRubyScriptEngineManager(ClassLoader loader) throws ScriptException {
        nameMap = new HashMap<String, ScriptEngineFactory>();
        extensionMap = new HashMap<String, ScriptEngineFactory>();
        mimetypeMap = new HashMap<String, ScriptEngineFactory>();
        globalMap = new SimpleBindings();
        try {
            factories = new ServiceFinder<ScriptEngineFactory>(SERVICE, loader).getServices();
            if ( factories.isEmpty() ) {
                System.err.println("no factory"); // TODO this is fatal, right?
            }
            prepareMaps();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new ScriptException(e);
        }
    }

    private void prepareMaps() {
        for (ScriptEngineFactory factory : factories) {
            List<String> names = factory.getNames();
            for (String name : names) {
                nameMap.put(name, factory);
            }
            List<String> extensions = factory.getExtensions();
            for (String extension : extensions) {
                extensionMap.put(extension, factory);
            }
            List<String> mimeTypes = factory.getMimeTypes();
            for (String mimeType : mimeTypes) {
                mimetypeMap.put(mimeType, factory);
            }
        }
    }

    public void setBindings(final Bindings bindings) {
        if (bindings == null) {
            throw new IllegalArgumentException("Null bindings");
        }
        globalMap = bindings;
    }

    public Bindings getBindings() {
        return globalMap;
    }

    public void put(String key, Object value) {
        globalMap.put(key, value);
    }

    public Object get(String key) {
        return globalMap.get(key);
    }

    public ScriptEngine getEngineByName(String shortName) {
        if (shortName == null) {
            throw new NullPointerException("Null shortName");
        }
        ScriptEngineFactory factory = nameMap.get(shortName);
        if (factory == null) {
            throw new IllegalArgumentException("No engine for " + shortName);
        }
        ScriptEngine engine = factory.getScriptEngine();
        engine.getContext().setBindings(globalMap, ScriptContext.GLOBAL_SCOPE);
        return engine;
    }

    public ScriptEngine getEngineByExtension(String extension) {
        if (extension == null) {
            throw new NullPointerException("Null extension");
        }
        ScriptEngineFactory factory = extensionMap.get(extension);
        if (factory == null) {
            throw new IllegalArgumentException("No engine for " + extension);
        }
        ScriptEngine engine = factory.getScriptEngine();
        engine.getContext().setBindings(globalMap, ScriptContext.GLOBAL_SCOPE);
        return engine;
    }

    public ScriptEngine getEngineByMimeType(String mimeType) {
        if (mimeType == null) {
            throw new NullPointerException("Null mimeType");
        }
        ScriptEngineFactory factory = mimetypeMap.get(mimeType);
        if (factory == null) {
            throw new IllegalArgumentException("No engine for " + mimeType);
        }
        ScriptEngine engine = factory.getScriptEngine();
        engine.getContext().setBindings(globalMap, ScriptContext.GLOBAL_SCOPE);
        return engine;
    }

    public List<ScriptEngineFactory> getEngineFactories() {
        return Collections.unmodifiableList(new ArrayList<ScriptEngineFactory>(factories));
    }

    public void registerEngineName(String name, ScriptEngineFactory factory) {
        if (name == null || factory == null) {
            throw new NullPointerException("name and/or factory is null.");
        }
        nameMap.put(name, factory);
    }

    public void registerEngineMimeType(String type, ScriptEngineFactory factory) {
        if (type == null || factory == null) {
            throw new NullPointerException("type and/or factory is null.");
        }
        mimetypeMap.put(type, factory);
    }

    public void registerEngineExtension(String extension, ScriptEngineFactory factory) {
        if (extension == null || factory == null) {
            throw new NullPointerException("extension and/or factory is null.");
        }
        extensionMap.put(extension, factory);
    }
}