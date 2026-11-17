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
 * Copyright (C) 2009-2012 Yoko Harada <yokolet@gmail.com>
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

import java.io.PrintStream;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import org.jruby.Ruby;
import org.jruby.RubyGlobal.OutputGlobalVariable;
import org.jruby.RubyIO;
import org.jruby.embed.AttributeName;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.io.WriterOutputStream;
import org.jruby.embed.variable.TransientLocalVariable;
import org.jruby.embed.variable.VariableInterceptor;
import org.jruby.internal.runtime.GlobalVariable;

import static org.jruby.api.Access.globalVariables;
import static org.jruby.api.Access.objectClass;

/**
 * A collection of JSR223 specific utility methods.
 *
 * @author Yoko Harada &lt;<a href="mailto:yokolet@gmail.com">yokolet@gmail.com</a>&gt;
 */
public class Utils {
    /**
     * Gets line number value from engine's attribute map.
     *
     * @param context ScriptContext to be used to the evaluation
     * @return line number
     */
    static int getLineNumber(ScriptContext context) {
        Object obj = context.getAttribute(AttributeName.LINENUMBER.toString(), ScriptContext.ENGINE_SCOPE);
        return obj instanceof Integer ? (Integer) obj : 0;
    }

    /**
     * Gets a receiver object from engine's attribute map.
     *
     * @param context ScriptContext to be used to the evaluation
     * @return receiver object or null if the attribute doesn't exist
     */
    static Object getReceiver(ScriptContext context) {
        return context.getAttribute(AttributeName.RECEIVER.toString(), ScriptContext.ENGINE_SCOPE);
    }

    static String getFilename(ScriptContext context) {
        return getFilename(context, "<script>");
    }

    static String getFilename(ScriptContext context, String defaultName) {
        Object filename = context.getAttribute(ScriptEngine.FILENAME);
        return filename != null ? (String) filename : defaultName;
    }

    static boolean isTerminationOn(ScriptContext context) {
        Object obj = context.getAttribute(AttributeName.TERMINATION.toString());
        if (obj instanceof Boolean && ((Boolean) obj).booleanValue()) {
            return true;
        }
        return false;
    }
    
    static boolean isClearVariablesOn(ScriptContext context) {
        Object obj = context.getAttribute(AttributeName.CLEAR_VARAIBLES.toString());
        if (obj instanceof Boolean && ((Boolean) obj).booleanValue()) {
            return true;
        }
        return false;
    }

    static void preEval(ScriptingContainer container, ScriptContext context) {
        Object receiver = Utils.getReceiverObject(context);

        Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
        for (Map.Entry<String, Object> entry : bindings.entrySet()) {
            put(container, receiver, entry.getKey(), entry.getValue(), context);
        }

        //container.setReader(context.getReader());
        setStdOut(container, context.getWriter());
        setStdErr(container, context.getErrorWriter());

        // if key of globalMap exists in engineMap, this key-value pair should be skipped.
        bindings = context.getBindings(ScriptContext.GLOBAL_SCOPE);
        if (bindings == null) return;
        for (Map.Entry<String, Object> entry : bindings.entrySet()) {
            if (container.getVarMap().containsKey(entry.getKey())) continue;
            put(container, receiver, entry.getKey(), entry.getValue(), context);
        }
    }

    private static Object getReceiverObject(ScriptContext context) {
        if (context == null) return null;
        return context.getAttribute(AttributeName.RECEIVER.toString(), ScriptContext.ENGINE_SCOPE);
    }
    
    private static void setStdOut(ScriptingContainer container, Writer writer) {
        if (writer == null) {
            return;
        }
        Map map = container.getAttributeMap();
        if (map.containsKey(AttributeName.WRITER)) {
            Writer old = (Writer) map.get(AttributeName.WRITER);
            if (old == writer) {
                return;
            }
        }
        map.put(AttributeName.WRITER, writer);
        
        var context = container.getProvider().getRuntime().getCurrentContext();
        RubyIO io = getRubyIO(context.runtime, writer);
        context.runtime.defineVariable(new OutputGlobalVariable(context.runtime, "$stdout", io), GlobalVariable.Scope.GLOBAL);
        objectClass(context).storeConstant(context, "STDOUT", io);
        globalVariables(context).alias("$>", "$stdout");
        globalVariables(context).alias("$defout", "$stdout");
    }
    
    private static void setStdErr(ScriptingContainer container, Writer writer) {
        if (writer == null) {
            return;
        }
        Map map = container.getAttributeMap();
        if (map.containsKey(AttributeName.ERROR_WRITER)) {
            Writer old = (Writer) map.get(AttributeName.ERROR_WRITER);
            if (old == writer) {
                return;
            }
        }
        map.put(AttributeName.ERROR_WRITER, writer);
        
        var context = container.getProvider().getRuntime().getCurrentContext();
        RubyIO io = getRubyIO(context.runtime, writer);
        context.runtime.defineVariable(new OutputGlobalVariable(context.runtime, "$stderr", io), GlobalVariable.Scope.GLOBAL);
        objectClass(context).storeConstant(context, "STDERR", io);
        globalVariables(context).alias("$deferr", "$stderr");
    }
    
    private static RubyIO getRubyIO(Ruby runtime, Writer writer) {
        PrintStream stream = new PrintStream(new WriterOutputStream(writer, runtime.getDefaultCharset().name()), true);
        RubyIO io = new RubyIO(runtime, stream, false);
        boolean locked = io.getOpenFile().lock();
        try {
            io.getOpenFile().setSync(true);
            io.getOpenFile().io_fflush(runtime.getCurrentContext());
            return io;
        } finally {
            if (locked) io.getOpenFile().unlock();
        }
    }

    static void postEval(ScriptingContainer container, ScriptContext context) {
        if (context == null) return;
        Object receiver = Utils.getReceiverObject(context);

        Bindings engineMap = context.getBindings(ScriptContext.ENGINE_SCOPE);
        Iterator<Map.Entry<String, Object>> iter = engineMap.entrySet().iterator();
        for (;iter.hasNext();) {
            Map.Entry<String, Object> entry = iter.next();
            if (Utils.shouldLVarBeDeleted(container, entry.getKey())) {
                iter.remove();
            }
        }
        Set<String> keys = container.getVarMap().keySet();
        if (keys != null && keys.size() > 0) {
            for (String key : keys) {
                Object value = container.getVarMap().get(key);
                engineMap.put(Utils.adjustKey(key), value);
            }
        }

        Bindings globalMap = context.getBindings(ScriptContext.GLOBAL_SCOPE);
        if (globalMap == null) return;
        keys = globalMap.keySet();
        if (keys != null && keys.size() > 0) {
            for (String key : keys) {
                if (engineMap.containsKey(key)) continue;
                Object value = container.getVarMap().get(receiver, key);
                globalMap.put(key, value);
            }
        }
    }

    private static Object put(ScriptingContainer container, Object receiver, String key, Object value, ScriptContext context) {
        Object oldValue = null;
        String adjustedKey = Utils.adjustKey(key);
        if (Utils.isRubyVariable(container, adjustedKey)) {
            boolean sharing_variables = true;
            Object obj = context.getAttribute(AttributeName.SHARING_VARIABLES.toString(), ScriptContext.ENGINE_SCOPE);
            if (obj instanceof Boolean && ((Boolean) obj) == false) {
                sharing_variables = false;
            }
            if (sharing_variables || "ARGV".equals(adjustedKey)) {
                oldValue = container.put(receiver, adjustedKey, value);
            }
        } else {
            if (adjustedKey.equals(AttributeName.SHARING_VARIABLES.toString())) {
                oldValue = container.setAttribute(AttributeName.SHARING_VARIABLES, value);
            } else {
                oldValue = container.setAttribute(adjustedKey, value);
            }
        }
        return oldValue;
    }

    static boolean isRubyVariable(ScriptingContainer container, String name) {
        return VariableInterceptor.isKindOfRubyVariable(container.getProvider().getLocalVariableBehavior(), name);
    }

    private static String adjustKey(String key) {
        if (key.equals(ScriptEngine.ARGV)) {
            return "ARGV";
        } if ("ARGV".equals(key)) {
            return ScriptEngine.ARGV;
        } else {
            return key;
        }
    }
    
    private static boolean shouldLVarBeDeleted(ScriptingContainer container, String key) {
        LocalVariableBehavior behavior = container.getProvider().getLocalVariableBehavior();
        if (behavior != LocalVariableBehavior.TRANSIENT) return false;
        return TransientLocalVariable.isValidName(key);
    }
}
