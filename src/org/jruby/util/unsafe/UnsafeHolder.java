/*
 ***** BEGIN LICENSE BLOCK *****
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
 ***** END LICENSE BLOCK *****/
package org.jruby.util.unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

public final class UnsafeHolder {
    
    private UnsafeHolder(){}
        
    /**
     * Holds a reference to Unsafe if available, null otherwise.    
     */
    public static final sun.misc.Unsafe U = loadUnsafe();
    
    private static final java.lang.invoke.MethodHandle fullFence = loadFenceHandle("fullFence");
    private static final java.lang.invoke.MethodHandle loadFence = loadFenceHandle("loadFence");
    private static final java.lang.invoke.MethodHandle storeFence = loadFenceHandle("storeFence");
    
    public static final boolean SUPPORTS_FENCES = fullFence != null && loadFence != null && storeFence != null;
    
    private static sun.misc.Unsafe loadUnsafe() {
        try {
            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field f = unsafeClass.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (sun.misc.Unsafe) f.get(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private static java.lang.invoke.MethodHandle loadFenceHandle(String name) {
        if(U == null)
            return null;
        
        try {
            // check if this JRE supports method handles
            Class clazz = Class.forName("java.lang.invoke.MethodHandles");

            MethodType mt = MethodType.methodType(void.class);
            MethodHandles.Lookup lookups = MethodHandles.lookup();
            
            return lookups.findVirtual(U.getClass(), name, mt);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        
    }
    
    public static long fieldOffset(Class clazz, String name) {
        if(U == null)
            return -1;
        try {
            return U.objectFieldOffset(clazz.getDeclaredField(name));
        } catch (Exception e) {
            e.printStackTrace();
            return sun.misc.Unsafe.INVALID_FIELD_OFFSET;
        }
    }
    
    public static void fullFence() {
        try {
            fullFence.invokeExact(U);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    public static void loadFence() {
        try {
            loadFence.invokeExact(U);
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public static void storeFence() {
        try {
            storeFence.invokeExact(U);
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


}
