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

package org.jruby.embed.osgi.internal;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.jruby.embed.ScriptingContainer;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.framework.FrameworkUtil;

/**
 * Closest thing to JRubyClassLoader's addURL but for OSGi bundles.
 * Used as the parent classloader the usual jruby's bundle's classloader.
 * 
 * @author hmalphettes
 *
 */
public class JRubyOSGiBundleClassLoader extends ClassLoader implements BundleReference {

    private static final Logger LOG = LoggerFactory.getLogger(JRubyOSGiBundleClassLoader.class.getName());
    private static final IOSGiClassLoaderAdapter ADAPTER;

    static {
        IOSGiClassLoaderAdapter chosenAdapter = null;
        try {
            Bundle b = FrameworkUtil.getBundle(JRubyOSGiBundleClassLoader.class);
            // I don't want to actually reference this class, in case it's unavailable
            b.loadClass("org.osgi.framework.wiring.BundleWiring");
            // But, if it can be loaded, we know we can use the BundleWiringOSGiClassLoaderAdapter
            chosenAdapter = new BundleWiringOSGiClassLoaderAdapter();
        } catch (Exception e) {
            LOG.warn("Could not load BundleWiring.  Falling back to reflection.");
            // Otherwise, just use the old method.
            chosenAdapter = new ReflectiveOSGiClassLoaderAdapter();
        }
        ADAPTER = chosenAdapter;
    }

    /**
     * look in OSGi first? true by default for now.
     * we could look in jruby first if that makes more sense.
     */
    private boolean _lookInOsgiFirst = true;
    
    private LinkedHashMap<Bundle,ClassLoader> _libraries;

    /**
     * @param 
     * @throws IOException
     */
    public JRubyOSGiBundleClassLoader()
    {
        super(ScriptingContainer.class.getClassLoader());
        _libraries = new LinkedHashMap<Bundle, ClassLoader>();
    }
    /**
     * @param 
     * @throws IOException
     */
    public JRubyOSGiBundleClassLoader(Bundle creator)
    {
        this();
        addBundle(creator);
    }
    /**
     * @param 
     * @throws IOException
     */
    public void addBundle(Class<?> classInOsgiBundle)
    {
        Bundle b = FrameworkUtil.getBundle(classInOsgiBundle);
        if (b == null) {
            throw new IllegalArgumentException(classInOsgiBundle
                    + " is not loaded by a bundle. Its classloader is "
                    + classInOsgiBundle.getClassLoader() + " does not implement "
                    + "org.osgi.framework.BundleReference");
        }
        _libraries.put(b, classInOsgiBundle.getClassLoader());
    }
    /**
     * @param parent The parent classloader. In this case jrubyLoader
     * @param context The WebAppContext
     * @param contributor The bundle that defines this web-application.
     * @throws IOException
     */
    public boolean addBundle(Bundle bundle)
    {
        return _libraries.put(bundle, ADAPTER.getClassLoader(bundle)) != null;
    }
    
    /**
     * @param parent The parent classloader. In this case jrubyLoader
     * @param context The WebAppContext
     * @param contributor The bundle that defines this web-application.
     * @throws IOException
     */
    public boolean removeBundle(Bundle bundle)
    throws IOException
    {
        return _libraries.remove(bundle) != null;
    }
    
    /**
     * Returns the <code>Bundle</code> that defined this web-application.
     * 
     * @return The <code>Bundle</code> object associated with this
     *         <code>BundleReference</code>.
     */
    public Bundle getBundle()
    {
        return _libraries.keySet().iterator().next();
    }

    /**
     * TODO: optimize: we should not have to look for the resources everywhere
     * until called for it.
     */
    @Override
    public Enumeration<URL> getResources(String name) throws IOException
    {
        LinkedList<Enumeration<URL>> enums = new LinkedList<Enumeration<URL>>();
        for (ClassLoader cl : _libraries.values()) {
            enums.add(cl.getResources(name));
        }
        Enumeration<URL> urls = super.getResources(name);
        if (_lookInOsgiFirst)
        {
            enums.addFirst(urls);
        }
        else
        {
            enums.addLast(urls);
        }
        return Collections.enumeration(toList(enums));
    }
    
    @Override
    public URL getResource(String name)
    {
        if (_lookInOsgiFirst)
        {
            URL url = null;
            for (ClassLoader cl : _libraries.values()) { 
                url = cl.getResource(name);
                if (url != null) {
                    return url;
                }
            }
            return super.getResource(name);
        }
        else 
        {
            URL url = super.getResource(name);
            if (url != null) {
                return url;
            }
            for (ClassLoader cl : _libraries.values()) { 
                url = cl.getResource(name);
                if (url != null) {
                    return url;
                }
            }
            return null;
        }       
    }
    
    private List<URL> toList(List<Enumeration<URL>> l)
    {
        List<URL> list = new LinkedList<URL>();
        for (Enumeration<URL> e : l) {
            while (e!=null && e.hasMoreElements()) {
                list.add(e.nextElement());
            }
        }
        return list;
    }

    /**
     * 
     */
    protected Class<?> findClass(String name) throws ClassNotFoundException
    {
        if (_lookInOsgiFirst) {
            for (ClassLoader cl : _libraries.values()) {
                try {
                    return cl.loadClass(name);
                } catch (ClassNotFoundException cne) {
                }
            }
            return super.findClass(name);
        } else {
            try {
                return super.findClass(name);
            } catch (ClassNotFoundException cne) {
                for (ClassLoader cl : _libraries.values()) {
                    try {
                        return cl.loadClass(name);
                    } catch (ClassNotFoundException cnfe) {
                    }
                }
                throw cne;
            }
            
        }
    }

}
