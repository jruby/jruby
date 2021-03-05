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
 * Copyright (C) 2009-2011 Yoko Harada <yokolet@gmail.com>
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
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleBindings;
import org.jruby.embed.ScriptingContainer;

/**
 * Implementation of javax.script.ScriptContext.
 * 
 * @author Yoko Harada <yokolet@gmail.com>
 */
class JRubyContext implements ScriptContext {

    private static final int[] SCOPES = new int[] { ScriptContext.ENGINE_SCOPE, ScriptContext.GLOBAL_SCOPE };

    private static final List<Integer> SCOPE_LIST =
            Collections.unmodifiableList( Arrays.stream(SCOPES).boxed().collect(Collectors.toList()) );

    private final ScriptingContainer container;
    private final List<Integer> scopeList;
    private Bindings globalMap = null;
    private Bindings engineMap = new SimpleBindings();
    private Reader reader = null;
    private Writer writer = null;
    private Writer errorWriter = null;

    JRubyContext(ScriptingContainer container) {
        this.container = container;
        this.scopeList = SCOPE_LIST;
    }

    public Object getAttribute(String name) {
        Object ret = null;
        for (int scope : SCOPES) {
            ret = getAttributeFromScope(scope, name);
            if (ret != null) {
                return ret;
            }
        }
        return ret;
    }

    private Object getAttributeFromScope(final int scope, String name) {
        Object value;
        switch (scope) {
            case ScriptContext.ENGINE_SCOPE:
                value = engineMap.get(name);
                if (value == null && Utils.isRubyVariable(container, name)) {
                    value = container.get(Utils.getReceiver(this), name);
                    engineMap.put(name, value);
                }
                return value;
            case ScriptContext.GLOBAL_SCOPE:
                if (globalMap == null) {
                    return null;
                }
                return globalMap.get(name);
            default:
                throw new IllegalArgumentException("invalid scope");
        }
    }

    public Object getAttribute(String name, int scope) {
        return getAttributeFromScope(scope, name);
    }

    public int getAttributesScope(String name) {
        for (int scope : SCOPES) {
            Object ret = getAttributeFromScope(scope, name);
            if (ret != null) return scope;
        }
        return -1;
    }

    public Bindings getBindings(int scope) {
        switch (scope) {
            case ScriptContext.ENGINE_SCOPE:
                return engineMap;
            case ScriptContext.GLOBAL_SCOPE:
                return globalMap;
            default:
                throw new IllegalArgumentException("invalid scope");
        }
    }

    public Writer getErrorWriter() {
        return errorWriter;
    }

    public Reader getReader() {
        return reader;
    }

    public List<Integer> getScopes() {
        return scopeList;
    }

    public Writer getWriter() {
        return writer;
    }

    public Object removeAttribute(String name, int scope) {
        Bindings bindings = getBindings(scope);
        if (bindings == null) {
            return null;
        }
        return bindings.remove(name);
    }

    public void setAttribute(String key, Object value, int scope) {
        Bindings bindings = getBindings(scope);
        if (bindings == null) {
            return;
        }
        bindings.put(key, value);
    }

    public void setBindings(Bindings bindings, int scope) {
        switch (scope) {
            case ScriptContext.ENGINE_SCOPE:
                if (bindings == null) {
                    throw new NullPointerException("null bindings in ENGINE scope");
                }
                engineMap = bindings; break;
            case ScriptContext.GLOBAL_SCOPE:
                globalMap = bindings; break;
            default:
                throw new IllegalArgumentException("invalid scope");
        }
    }

    public void setErrorWriter(Writer errorWriter) {
        if (errorWriter == null) {
            return;
        }
        if (getErrorWriter() == errorWriter) {
            return;
        }
        this.errorWriter = errorWriter;
    }

    public void setReader(Reader reader) {
        if (reader == null) {
            return;
        }
        if (getReader() == reader) {
            return;
        }
        this.reader = reader;
    }

    public void setWriter(Writer writer) {
        if (writer == null) {
            return;
        }
        if (getWriter() == writer) {
            return;
        }
        this.writer = writer;
    }
}
