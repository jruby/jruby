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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Finds SPI based services from classpath.
 * 
 * @author Yoko Harada <yokolet@gmail.com>
 */
class ServiceFinder {
    private ClassLoader loader = null;
    private String serviceName;
    private HashSet<?> services;

    ServiceFinder(ClassLoader loader, String serviceName) throws IOException {
        this.serviceName = serviceName;
        Enumeration<URL> urls = findResources(loader);
        List<String> classNames = getClassNames(urls);
        services = instantiateClasses(classNames);
    }

    HashSet<?> getServices() {
        return services;
    }

    private Enumeration<URL> findResources(ClassLoader loader) throws IOException {
        Enumeration<URL> urls;
        if (loader == null) {
            this.loader = ClassLoader.getSystemClassLoader();
            urls = ClassLoader.getSystemResources(serviceName);
        } else {
            this.loader = loader;
            urls = loader.getResources(serviceName);
        }
        return urls;
    }

    private List<String> getClassNames(Enumeration<URL> urls) {
        String encoding = System.getProperty("file.encoding");
        List<String> names = new ArrayList();
        URL url = null;
        while (urls.hasMoreElements()) {
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
            } catch (IOException e) {
                System.err.println("Failed to get a class name from " + url.toString());
                continue;
            }
        }
        return names;
    }

    private String deleteComments(String line) {
        if (line.startsWith("#")) {
            return null;
        }
        if (line.length() < 1) {
            return null;
        }
        StringTokenizer st = new StringTokenizer(line, "#");
        return ((String) st.nextElement()).trim();
    }

    private <T> HashSet<T> instantiateClasses(List<String> names) {
        HashSet<T> instances = new HashSet<T>();
        for (String name : names) {
            try {
                Class clazz = Class.forName(name, true, loader);
                T instance = (T) clazz.newInstance();
                instances.add(instance);
            } catch (ClassNotFoundException e) {
                System.err.println(name + " was not found");
                continue;
            } catch (InstantiationException e) {
                System.err.println(name + " was not instantiated");
                continue;
            } catch (IllegalAccessException e) {
                System.err.println(name + " committed illegal access");
                continue;
            } catch (Throwable e) {
                System.err.println("failed to instantiate " + name);
                continue;
            }
        }
        return instances;
    }
}