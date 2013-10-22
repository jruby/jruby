/***** BEGIN LICENSE BLOCK *****
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
 ***** END LICENSE BLOCK *****/

package org.jruby.runtime.opto;

import org.jruby.RubyModule;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A tuple representing the data needed to verify a cached constant:
 * 
 * <li>The value of the constant</li>
 * <li>The generation of the constant's invalidator at the time of caching</li>
 * <li>The constant's invalidator</li>
 * <li>(Optional) the hashcode of the module form which the constant was cache</li>
 */
public class ConstantCache {
    public final IRubyObject value;
    public final Object generation;
    public final Invalidator invalidator;
    public final int id;

    /**
     * Construct a new ConstantCache with the given elements.
     * 
     * @param value
     * @param generation
     * @param invalidator
     * @param targetHash 
     */
    public ConstantCache(IRubyObject value, Object generation, Invalidator invalidator, int targetHash) {
        this.value = value;
        this.generation = generation;
        this.invalidator = invalidator;
        this.id = targetHash;
    }

    /**
     * Construct a new ConstantCache with the given elements.
     * 
     * @param value
     * @param generation
     * @param invalidator 
     */
    public ConstantCache(IRubyObject value, Object generation, Invalidator invalidator) {
        this(value, generation, invalidator, -1);
    }

    /**
     * Check if the given ConstantCache is non-null and valid, given the target module.
     * 
     * This method is static to ensure it is inlinable as trivially as possible.
     */
    public static boolean isCachedFrom(RubyModule target, ConstantCache cache) {
        return cache != null
                && cache.value != null
                && cache.generation == cache.invalidator.getData()
                && cache.id == target.id;
    }

    /**
     * Check if the given ConstantCache is non-null and valid.
     * 
     * This method is static to ensure it is inlinable as trivially as possible.
     */
    public static boolean isCached(ConstantCache cache) {
        return cache != null
                && cache.value != null
                && cache.generation == cache.invalidator.getData();
    }
}
