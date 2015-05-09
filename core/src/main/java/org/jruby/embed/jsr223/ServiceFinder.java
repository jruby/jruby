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
 * Copyright (C) 2009 Yoko Harada <yokolet@gmail.com>
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Finds SPI based services from classpath.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
class ServiceFinder<T> {

    private final Set<T> services;

    ServiceFinder(final String serviceName, ClassLoader loader) throws IOException {
        final Enumeration<URL> urls;

        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
            urls = ClassLoader.getSystemResources(serviceName);
        } else {
            urls = loader.getResources(serviceName);
        }

        List<String> classNames = readClassNames(urls);
        services = instantiateClasses(classNames, loader);
    }

    Collection<T> getServices() { return services; }

    private static List<String> readClassNames(Enumeration<URL> urls) {
        String encoding = System.getProperty("file.encoding");
        ArrayList<String> names = new ArrayList<String>();
        while ( urls.hasMoreElements() ) {
            URL url = null;
            try {
                url = urls.nextElement();
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(url.openStream(), encoding));
                String line;
                while ((line = reader.readLine()) != null) {
                    if ((line = deleteComments(line)) != null) {
                        names.add(line);
                    }
                }
            }
            catch (IOException e) {
                System.err.println("Failed to get a class name from " + url);
                continue;
            }
        }
        return names;
    }

    private static String deleteComments(final String line) {
        if ( line.startsWith("#") ) return null;
        if ( line.length() < 1 ) return null;
        StringTokenizer st = new StringTokenizer(line, "#");
        return ((String) st.nextElement()).trim();
    }

    private Set<T> instantiateClasses(final Collection<String> names,
        final ClassLoader loader) {
        HashSet<T> instances = new HashSet<T>( names.size() );
        for (String name : names) {
            try {
                @SuppressWarnings("unchecked")
                Class<T> clazz = (Class<T>) Class.forName(name, true, loader);
                instances.add( clazz.newInstance() );
            }
            catch (ClassNotFoundException e) {
                System.err.println(name + " was not found");
                continue;
            }
            catch (InstantiationException e) {
                System.err.println(name + " was not instantiated");
                continue;
            }
            catch (IllegalAccessException e) {
                System.err.println(name + " committed illegal access");
                continue;
            }
            catch (Throwable e) {
                System.err.println("failed to instantiate " + name);
                continue;
            }
        }
        return instances;
    }

}