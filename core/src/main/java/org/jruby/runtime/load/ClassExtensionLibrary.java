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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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
package org.jruby.runtime.load;

import org.jruby.Ruby;

import java.net.URL;

/**
 * The ClassExtensionLibrary wraps a class which implements BasicLibraryService,
 * and when asked to load the service, does a basicLoad of the BasicLibraryService.
 * 
 * When the time comes to add other loading mechanisms for loading a class, this
 * is the place where they will be added. The load method will check interface
 * you can load a class with, and do the right thing.
 */
public class ClassExtensionLibrary implements Library {
    private final Class theClass;
    private final String name;

    /**
     * Try to locate an extension service in the current classloader resources. This happens
     * after the jar has been added to JRuby's URLClassLoader (JRubyClassLoader) and is how
     * extensions can magically load.
     *
     * The basic logic is to use the require name (@param searchName) to build the name of
     * a class ending in "Service", and then invoke that class to boot the extension.
     *
     * The looping logic here was in response to a RubyGems change that started absolutizing
     * the path to the extension jar under some circumstances, leading to an incorrect
     * package/class name for the service contained therein. The new logic will try
     * successively more trailing elements until one of them is not a valid package
     * name or all elements have been exhausted.
     *
     * @param runtime the current JRuby runtime
     * @param searchName the name passed to `require`
     * @return a ClassExtensionLibrary that will boot the ext, or null if none was found
     */
    static ClassExtensionLibrary tryFind(Ruby runtime, String searchName) {
        // Create package name, by splitting on / and joining all but the last elements with a ".", and downcasing them.
        String[] all = searchName.split("/");

        // make service name out of last element
        String serviceName = buildServiceName(all[all.length - 1]);

        // allocate once with plenty of space, to reduce object churn
        StringBuilder classNameBuilder = new StringBuilder(searchName.length() * 2);
        StringBuilder classFileBuilder = new StringBuilder(searchName.length() * 2);

        for (int i = all.length - 1; i >= 0; i--) {
            buildClassName(classNameBuilder, classFileBuilder, all, i, serviceName);

            // look for the filename in classloader resources
            URL resource = runtime.getJRubyClassLoader().getResource(classFileBuilder.toString());
            if (resource == null) continue;

            String className = classNameBuilder.toString();

            try {
                Class theClass = runtime.getJavaSupport().loadJavaClass(className);
                return new ClassExtensionLibrary(className + ".java", theClass);
            } catch (ClassNotFoundException cnfe) {
                // file was found but class couldn't load; continue to next package segment
                continue;
            } catch (UnsupportedClassVersionError ucve) {
                if (runtime.isDebug()) ucve.printStackTrace();
                throw runtime.newLoadError("JRuby ext built for wrong Java version in `" + className + "': " + ucve, className.toString());
            }
        }

        // not found
        return null;
    }

    private static void buildClassName(StringBuilder nameBuilder, StringBuilder fileBuilder, String[] all, int i, String serviceName) {
        nameBuilder.setLength(0);
        fileBuilder.setLength(0);
        for (int j = i; j < all.length - 1; j++) {
            nameBuilder.append(all[j]).append(".");
            fileBuilder.append(all[j]).append("/");
        }
        nameBuilder.append(serviceName);
        fileBuilder.append(serviceName).append(".class");
    }

    private static String buildServiceName(String jarName) {
        String[] last = jarName.split("_");
        StringBuilder serviceName = new StringBuilder();
        for (int i = 0, j = last.length; i < j; i++) {
            if ("".equals(last[i])) break;
            serviceName.append(Character.toUpperCase(last[i].charAt(0))).append(last[i].substring(1));
        }
        serviceName.append("Service");
        return serviceName.toString();
    }

    public ClassExtensionLibrary(String name, Class extension) {
        theClass = extension;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void load(Ruby runtime, boolean wrap) {
        if(BasicLibraryService.class.isAssignableFrom(theClass)) {
            try {
                runtime.loadExtension(name, (BasicLibraryService)theClass.newInstance(), wrap);
            } catch(final Exception ee) {
                throw new RuntimeException(ee.getMessage(),ee);
            }
        }
    }
}
