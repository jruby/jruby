/*
 ***** BEGIN LICENSE BLOCK *****
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
package org.jruby.util.unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class UnsafeHolder {
    
    private UnsafeHolder(){}
        
    /**
     * Holds a reference to Unsafe if available, null otherwise.    
     */
    public static final sun.misc.Unsafe U = loadUnsafe();
    
    public static final boolean SUPPORTS_FENCES = supportsFences();
    public static final long    ARRAY_OBJECT_BASE_OFFSET = arrayObjectBaseOffset();
    public static final long    ARRAY_OBJECT_INDEX_SCALE = arrayObjectIndexScale();
    
    private static sun.misc.Unsafe loadUnsafe() {
        try {
            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field f = unsafeClass.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (sun.misc.Unsafe) f.get(null);
        } catch (Exception e) {
            return null;
        }
    }
    
    private static long arrayObjectBaseOffset() {
        if(U == null)
            return 0;
        return U.arrayBaseOffset(Object[].class);
    }
    
    private static long arrayObjectIndexScale() {
        if(U == null)
            return 0;
        return U.arrayIndexScale(Object[].class);
    }
    
    private static boolean supportsFences() {
        if(U == null)
            return false;
        try {
            Method m = U.getClass().getDeclaredMethod("fullFence");
            if(m != null)
                return true;
        } catch (Exception e) {
        }
        return false;
    }
    
    public static long fieldOffset(Class clazz, String name) {
        if(U == null)
            return -1;
        try {
            return U.objectFieldOffset(clazz.getDeclaredField(name));
        } catch (Exception e) {
            return sun.misc.Unsafe.INVALID_FIELD_OFFSET;
        }
    }
    
    //// The following methods are Java8 only. They will throw undefined method errors if invoked without checking for fence support 
    
    public static void fullFence() {
        U.fullFence();
    }
    
    public static void loadFence() {
        U.loadFence();
    }
    
    public static void storeFence() {
        U.storeFence();
    }


}
