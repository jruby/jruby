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
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class LocalContext {

    private final RubyInstanceConfig config;
    private final LocalVariableBehavior behavior;
    private boolean lazy;

    private Ruby runtime = null;

    private BiVariableMap varMap;
    private Map<AttributeName, Object> attributes;

    public LocalContext(RubyInstanceConfig config, LocalVariableBehavior behavior) {
        this(config, behavior, false);
    }

    public LocalContext(RubyInstanceConfig config, LocalVariableBehavior behavior, boolean lazy) {
        this.config = config;
        this.behavior = behavior;
        this.lazy = lazy;
    }

    // This method is used only from ThreadLocalContextProvider.
    // Other providers should instantialte runtime in their own way.
    @Deprecated
    public Ruby getThreadSafeRuntime() {
        return getRuntime();
    }

    public BiVariableMap getVarMap(LocalContextProvider provider) {
        if (varMap == null) {
            synchronized(this) {
                if (varMap == null) {
                    varMap = new BiVariableMap(provider, lazy);
                }
            }
        }
        return varMap;
    }

    public LocalVariableBehavior getLocalVariableBehavior() {
        return behavior;
    }

    @SuppressWarnings("MapReplaceableByEnumMap")
    public Map<?, Object> getAttributeMap() {
        if (attributes == null) {
            synchronized(this) {
                if (attributes == null) {
                    attributes = new HashMap<AttributeName, Object>();
                    attributes.put(AttributeName.READER, new InputStreamReader(System.in));
                    attributes.put(AttributeName.WRITER, new PrintWriter(System.out, true));
                    attributes.put(AttributeName.ERROR_WRITER, new PrintWriter(System.err, true));
                }
            }
        }
        return attributes;
    }

    public void remove() {
        if (attributes != null) {
            synchronized(this) { attributes.clear(); }
        }
        if (varMap != null) {
            synchronized(this) { varMap.clear(); }
        }
    }

    Ruby getRuntime() {
        if (runtime == null) {
            synchronized(this) {
                if (runtime == null) {
                    runtime = Ruby.newInstance(config);
                }
            }
        }
        return runtime;
    }

    boolean isInitialized() { return runtime != null; }

}
