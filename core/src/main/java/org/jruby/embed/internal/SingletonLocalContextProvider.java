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
package org.jruby.embed.internal;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.embed.AttributeName;
import org.jruby.embed.LocalVariableBehavior;

/**
 * Singleton type local context provider.
 * As of JRuby 1.5.0 Ruby runtime returned from the getRuntime() method is a
 * classloader-global runtime.
 * 
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class SingletonLocalContextProvider extends AbstractLocalContextProvider {
    private static LocalContext localContext = null;
    private static BiVariableMap varMap = null;
    private static HashMap attribute = null;
    
    public static LocalContext getLocalContextInstance(RubyInstanceConfig config, LocalVariableBehavior behavior, boolean lazy) {
        if (localContext == null) {
            synchronized (LocalContext.class) {
                localContext = new LocalContext(config, behavior, lazy);
            }
        }
        return localContext;
    }
    
    private static BiVariableMap getBiVariableInstance(LocalContextProvider provider, boolean lazy) {
        if (varMap == null) {
            synchronized (BiVariableMap.class) {
                varMap = new BiVariableMap(provider, lazy);
            }
        }
        return varMap;
    }
    
    private static HashMap getAttributeInstance() {
        if (attribute == null) {
            synchronized (HashMap.class) {
                attribute = new HashMap();
                attribute.put(AttributeName.READER, new InputStreamReader(System.in));
                attribute.put(AttributeName.WRITER, new PrintWriter(System.out, true));
                attribute.put(AttributeName.ERROR_WRITER, new PrintWriter(System.err, true));
            }
        }
        return attribute;
    }
    
    public static LocalVariableBehavior getLocalVariableBehaviorOrNull() {
        if (localContext == null) return null;
        else return localContext.getLocalVariableBehavior();
    }

    public SingletonLocalContextProvider(LocalVariableBehavior behavior, boolean lazy) {
        this.behavior = behavior;
        this.lazy = lazy;
    }
    
    public Ruby getRuntime() {
        if (!Ruby.isGlobalRuntimeReady()) {
            return Ruby.newInstance(config);
        }
        return Ruby.getGlobalRuntime();
    }

    @Override
    public RubyInstanceConfig getRubyInstanceConfig() {
        if (Ruby.isGlobalRuntimeReady()) return Ruby.getGlobalRuntime().getInstanceConfig();
        else return config;
    }

    public BiVariableMap getVarMap() {
        return SingletonLocalContextProvider.getBiVariableInstance(this, lazy);
    }

    public Map getAttributeMap() {
        return SingletonLocalContextProvider.getAttributeInstance();
    }

    public boolean isRuntimeInitialized() {
        return Ruby.isGlobalRuntimeReady();
    }
    
    public void terminate() {
        LocalContext context = SingletonLocalContextProvider.getLocalContextInstance(config, behavior, lazy);
        context.remove();
        context = null;
    }
}
