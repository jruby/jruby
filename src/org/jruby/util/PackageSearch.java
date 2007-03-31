/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
 * Adapted from code by Alexandru Popescu
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

package org.jruby.util;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * Using JAR resource URLs, scan all loaded JAR files for the package name specified.
 * If found, look for all classes in the given package and return them as a list.
 */
public class PackageSearch {
    public static List findClassesInPackage(String packageName) throws IOException {
        return findClassesInPackage(packageName, null);
    }
    
    public static List findClassesInPackage(String packageName, Pattern pattern) throws IOException {
        String packageOnly = packageName;
        boolean recursive = false;
        if (packageName.endsWith(".*")) {
            packageOnly = packageName.substring(0, packageName.lastIndexOf(".*"));
            recursive = true;
        }
        
        List vResult = new ArrayList();
        String packageDirName = packageOnly.replace('.', '/');
        
        Enumeration dirs =
                Thread.currentThread().getContextClassLoader().getResources(packageDirName);
        while (dirs.hasMoreElements()) {
            URL url = (URL)dirs.nextElement();
            String protocol = url.getProtocol();
            
            if ("file".equals(protocol)) {
                // FIXME: need to handle filesystem files
            } else if ("jar".equals(protocol)) {
                JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
                
                Enumeration entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = (JarEntry)entries.nextElement();
                    String name = entry.getName();
                    
                    if (name.charAt(0) == '/') {
                        name = name.substring(1);
                    }
                    if (name.startsWith(packageDirName)) {
                        int idx = name.lastIndexOf('/');
                        
                        if (name.indexOf("$") != -1) {
                            // don't include inner classes
                            continue;
                        }
                        
                        if (idx != -1) {
                            packageName = name.substring(0, idx).replace('/', '.');
                        }
                        
                        if ((idx == packageDirName.length()) || recursive) {
                            //it's not inside a deeper dir
                            if (name.endsWith(".class") && !entry.isDirectory()) {
                                String className = name.substring(packageName.length()
                                        + 1, name.length() - 6);
                                
                                if (pattern != null && !pattern.matcher(className).matches()) {
                                    // doesn't match pattern, skip
                                    continue;
                                }

                                vResult.add(packageName + "." + className);
                            }
                        }
                    }
                }
            }
        }
        
        return vResult;
    }
}
