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
 * Copyright (C) 2002-2011 JRuby Community
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
package org.jruby.embed.osgi.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.osgi.framework.Bundle;

/**
 * Various introspection tricks to access the file system from OSGi.
 * Tested for felix and equinox.
 * 
 * @author hmalphettes
 */
public class OSGiBundleClassLoaderHelper {
    
    private static boolean identifiedOsgiImpl = false;
    private static boolean isEquinox = false;
    private static boolean isFelix = false;

    private static void init(Bundle bundle)
    {
        identifiedOsgiImpl = true;
        try
        {
            isEquinox = bundle.getClass().getClassLoader().loadClass("org.eclipse.osgi.framework.internal.core.BundleHost") != null;
        }
        catch (Throwable t)
        {
            isEquinox = false;
        }
        if (!isEquinox)
        {
            try
            {
                isFelix = bundle.getClass().getClassLoader().loadClass("org.apache.felix.framework.BundleImpl") != null;
            }
            catch (Throwable t2)
            {
                isFelix = false;
            }
        }
        // System.err.println("isEquinox=" + isEquinox);
        // System.err.println("isFelix=" + isFelix);
    }

    /**
     * Assuming the bundle is started.
     * 
     * @param bundle
     * @return classloader object
     */
    public static ClassLoader getBundleClassLoader(Bundle bundle)
    {
        String bundleActivator = (String)bundle.getHeaders().get("Bundle-Activator");
        if (bundleActivator == null)
        {
            bundleActivator = (String)bundle.getHeaders().get("Jetty-ClassInBundle");
        }
        if (bundleActivator != null)
        {
            try
            {
                return bundle.loadClass(bundleActivator).getClassLoader();
            }
            catch (ClassNotFoundException e)
            {
                // should not happen as we are called if the bundle is started
                // anyways.
                e.printStackTrace();
            }
        }
        // resort to introspection
        if (!identifiedOsgiImpl)
        {
            init(bundle);
        }
        if (isEquinox)
        {
            return internalGetEquinoxBundleClassLoader(bundle);
        }
        else if (isFelix)
        {
            return internalGetFelixBundleClassLoader(bundle);
        }
        return null;
    }

    private static Method Equinox_BundleHost_getBundleLoader_method;
    private static Method Equinox_BundleLoader_createClassLoader_method;

    private static ClassLoader internalGetEquinoxBundleClassLoader(Bundle bundle)
    {
        // assume equinox:
        try
        {
            if (Equinox_BundleHost_getBundleLoader_method == null)
            {
                Equinox_BundleHost_getBundleLoader_method = bundle.getClass().getClassLoader().loadClass("org.eclipse.osgi.framework.internal.core.BundleHost")
                        .getDeclaredMethod("getBundleLoader",new Class[] {});
                Equinox_BundleHost_getBundleLoader_method.setAccessible(true);
            }
            Object bundleLoader = Equinox_BundleHost_getBundleLoader_method.invoke(bundle,new Object[] {});
            if (Equinox_BundleLoader_createClassLoader_method == null && bundleLoader != null)
            {
                Equinox_BundleLoader_createClassLoader_method = bundleLoader.getClass().getClassLoader().loadClass(
                        "org.eclipse.osgi.internal.loader.BundleLoader").getDeclaredMethod("createClassLoader",new Class[] {});
                Equinox_BundleLoader_createClassLoader_method.setAccessible(true);
            }
            return (ClassLoader)Equinox_BundleLoader_createClassLoader_method.invoke(bundleLoader,new Object[] {});
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        return null;
    }

    private static Field Felix_BundleImpl_m_modules_field;
    private static Field Felix_ModuleImpl_m_classLoader_field;

    private static ClassLoader internalGetFelixBundleClassLoader(Bundle bundle)
    {
        // assume felix:
        try
        {
            // now get the current module from the bundle.
            // and return the private field m_classLoader of ModuleImpl
            if (Felix_BundleImpl_m_modules_field == null)
            {
                Felix_BundleImpl_m_modules_field = bundle.getClass().getClassLoader().loadClass("org.apache.felix.framework.BundleImpl").getDeclaredField(
                        "m_modules");
                Felix_BundleImpl_m_modules_field.setAccessible(true);
            }
            Object[] moduleArray = (Object[])Felix_BundleImpl_m_modules_field.get(bundle);
            Object currentModuleImpl = moduleArray[moduleArray.length - 1];
            if (Felix_ModuleImpl_m_classLoader_field == null && currentModuleImpl != null)
            {
                Felix_ModuleImpl_m_classLoader_field = bundle.getClass().getClassLoader().loadClass("org.apache.felix.framework.ModuleImpl").getDeclaredField(
                        "m_classLoader");
                Felix_ModuleImpl_m_classLoader_field.setAccessible(true);
            }
            // first make sure that the classloader is ready:
            // the m_classLoader field must be initialized by the
            // ModuleImpl.getClassLoader() private method.
            ClassLoader cl = (ClassLoader)Felix_ModuleImpl_m_classLoader_field.get(currentModuleImpl);
            if (cl == null)
            {
                // looks like it was not ready:
                // the m_classLoader field must be initialized by the
                // ModuleImpl.getClassLoader() private method.
                // this call will do that.
                bundle.loadClass("java.lang.Object");
                cl = (ClassLoader)Felix_ModuleImpl_m_classLoader_field.get(currentModuleImpl);
                // System.err.println("Got the bundle class loader of felix_: "
                // + cl);
                return cl;
            }
            else
            {
                // System.err.println("Got the bundle class loader of felix: " +
                // cl);
                return cl;
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        return null;
    }

}
