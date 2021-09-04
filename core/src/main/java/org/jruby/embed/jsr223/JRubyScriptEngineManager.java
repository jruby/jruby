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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
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

    private final ScriptEngineFactory[] factories;
    private final Map<String, ScriptEngineFactory> nameMap;
    private final Map<String, ScriptEngineFactory> extensionMap;
    private final Map<String, ScriptEngineFactory> mimetypeMap;
    private Bindings globalMap;

    public JRubyScriptEngineManager() {
        this(null);
    }

    public JRubyScriptEngineManager(ClassLoader loader) {
        nameMap = new HashMap<>();
        extensionMap = new HashMap<>();
        mimetypeMap = new HashMap<>();
        globalMap = new SimpleBindings();

        ArrayList<ScriptEngineFactory> factories = new ArrayList<>();
        // lookup from: META-INF/services/javax.script.ScriptEngineFactory
        Iterator<ScriptEngineFactory> i = ServiceLoader.load(javax.script.ScriptEngineFactory.class, loader).iterator();
        while (i.hasNext()) {
            ScriptEngineFactory factory = i.next();

            for (String name : factory.getNames()) {
                nameMap.put(name, factory);
            }
            for (String extension : factory.getExtensions()) {
                extensionMap.put(extension, factory);
            }
            for (String mimeType : factory.getMimeTypes()) {
                mimetypeMap.put(mimeType, factory);
            }

            factories.add(factory);
        }

        if (factories.isEmpty()) {
            throw new IllegalStateException("no javax.script.ScriptEngineFactory service");
        }

        this.factories = factories.toArray(new ScriptEngineFactory[factories.size()]);
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
            throw new NullPointerException("Null name");
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
        return Collections.unmodifiableList(Arrays.asList(factories));
    }

    public void registerEngineName(String name, ScriptEngineFactory factory) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(factory, "factory");
        nameMap.put(name, factory);
    }

    public void registerEngineMimeType(String type, ScriptEngineFactory factory) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(factory, "factory");
        mimetypeMap.put(type, factory);
    }

    public void registerEngineExtension(String extension, ScriptEngineFactory factory) {
        Objects.requireNonNull(extension, "extension");
        Objects.requireNonNull(factory, "factory");
        extensionMap.put(extension, factory);
    }
}