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

import java.util.List;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.util.ClassCache;

/**
 * LocalContextProvider provides an instance of {@link org.jruby.embed.internal.LocalContext} from
 * a specified scope defined by {@link LocalContextScope}. Users can configure
 * Ruby runtime by using methods of this interface before Ruby runtime is used.
 * Default scope is LocalContextScope.THREADSAFE.
 * 
 * @author Yoko Harada
 */
public interface LocalContextProvider {
    /**
     * Sets a scripts' loading path to a Ruby runtime.
     *
     * @param loadPaths is a list of paths to load scritps
     */
	@Deprecated
    void setLoadPaths(List loadPaths);

    /**
     * Sets a class cash option to a Ruby runtime.
     * 
     * @param classCache is a class cache option
     */
	@Deprecated
    void setClassCache(ClassCache classCache);

    /**
     * Gets an instance of {@link org.jruby.RubyInstanceConfig}.
     *
     * @return an instance of RubyInstanceConfig.
     */
    RubyInstanceConfig getRubyInstanceConfig();

    /**
     * Tests whether Ruby runtime has been initialized or not.
     *
     * @return true if initialized, false otherwise.
     */
    boolean isRuntimeInitialized();

    /**
     * Returns a Ruby runtime of a specified scope.
     *
     * @return a Ruby runtime
     */
    Ruby getRuntime();
    
    /**
     * Returns a local variable behavior associated to the scope.
     *
     * @return a local variable behavior
     */
    LocalVariableBehavior getLocalVariableBehavior();
    /**
     * Returns a {@link BiVariableMap} of a specified scope.
     * 
     * @return a variable map
     */
    BiVariableMap getVarMap();

    /**
     * Returns an attribute map of a specified scope.
     *
     * @return an attribute map
     */
    Map getAttributeMap();
    
    /**
     * Clears up a variable map.
     */
    void terminate();
}
