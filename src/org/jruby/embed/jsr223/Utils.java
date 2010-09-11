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

import java.util.Set;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import org.jruby.embed.AttributeName;
import org.jruby.embed.ScriptingContainer;

/**
 * A collection of JSR223 specific utility methods.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class Utils {
    /**
     * Gets line number value from engine's attribute map.
     *
     * @param engine JSR223 JRuby Engine
     * @return line number
     */
    static int getLineNumber(ScriptEngine engine) {
        Object obj = engine.getContext().getAttribute(AttributeName.LINENUMBER.toString(), ScriptContext.ENGINE_SCOPE);
        if (obj instanceof Integer) {
            return (Integer)obj;
        }
        return 0;
    }

    static String getFilename(ScriptEngine engine) {
        Object filename = engine.getContext().getAttribute(ScriptEngine.FILENAME);
        return filename != null ? (String)filename : "<script>";
    }

    static boolean isTerminationOn(ScriptEngine engine) {
        boolean termination = false;
        Object obj = engine.getContext().getAttribute(AttributeName.TERMINATION.toString());
        if (obj != null && obj instanceof Boolean && ((Boolean) obj) == true) {
            termination = true;
        }
        return termination;
    }

    static void preEval(ScriptingContainer container, JRubyContext jrubyContext) {
        Bindings bindings = getBindings(jrubyContext);
        if (bindings == null) return;
        Set<String> keys = bindings.keySet();
        for (String key : keys) {
            Object value = bindings.get(key);
            Object oldValue = put(container, key, value);
        }
    }

    private static Bindings getBindings(JRubyContext jrubyContext) {
        if (jrubyContext == null) return null;
        Bindings bindings = jrubyContext.getEngineScopeBindings();
        if (bindings == null) bindings = jrubyContext.getGlobalScopeBindings();
        return bindings;
    }

    static void postEval(ScriptingContainer container, JRubyContext jrubyContext) {
        if (jrubyContext == null) return;
        Set<String> keys = container.getVarMap().keySet();
        if (keys == null || keys.size() == 0) return;
        for (String key : keys) {
            Object value = container.getVarMap().get(key);
            jrubyContext.getEngineScopeBindings().put(key, value);
        }
    }

    private static Object put(ScriptingContainer container, String key, Object value) {
        Object oldValue = null;
        String adjustedKey = adjustKey(key);
        if (isRubyVariable(container, adjustedKey)) {
            oldValue = container.put(adjustedKey, value);
        } else {
            oldValue = container.setAttribute(adjustedKey, value);
            /* Maybe no need anymore?
            if (container.getAttributeMap().containsKey(BACKED_BINDING)) {
                Bindings b = (Bindings) container.getAttribute(BACKED_BINDING);
                b.put(key, value);
            }
             *
             */
        }
        return oldValue;
    }

    private static boolean isRubyVariable(ScriptingContainer container, String name) {
        return container.getVarMap().getVariableInterceptor().isKindOfRubyVariable(name);
    }

    private static String adjustKey(String key) {
        if (key.equals(ScriptEngine.ARGV)) {
            return "ARGV";
        } else {
            return key;
        }
    }
}
