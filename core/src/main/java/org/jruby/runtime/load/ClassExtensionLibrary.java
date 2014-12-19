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

import jnr.posix.JavaSecuredFile;
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
        // Create package name, by splitting on / and successively accumulating elements to form a class name
        String[] elts = searchName.split("/");

        boolean isAbsolute = new JavaSecuredFile(searchName).isAbsolute();

        String simpleName = buildSimpleName(elts[elts.length - 1]);

        int firstElement = isAbsolute ? elts.length - 1 : 0;
        for (; firstElement >= 0; firstElement--) {
            String className = buildClassName(elts, firstElement, elts.length - 1, simpleName);
            ClassExtensionLibrary library = tryServiceLoad(runtime, className);

            if (library != null) return library;
        }

        return null;
    }

    /**
     * Build the "simple" part of a service class name from the given require path element
     * by splitting on "_" and concatenating as CamelCase, plus "Service" suffix.
     *
     * @param element the element from which to build a simple service class name
     * @return the resulting simple service class name
     */
    private static String buildSimpleName(String element) {
        StringBuilder nameBuilder = new StringBuilder(element.length() + "Service".length());

        String[] last = element.split("_");
        for (String part : last) {
            if (part.isEmpty()) break;

            nameBuilder
                    .append(Character.toUpperCase(part.charAt(0)))
                    .append(part, 1, part.length());
        }
        nameBuilder.append("Service");

        return nameBuilder.toString();
    }

    /**
     * Build the full class name for a service by joining the specified package elements
     * with '.' and appending the given simple class name.
     *
     * @param elts the array from which to retrieve the package elements
     * @param firstElement the first element to use
     * @param end the end index at which to stop building the package name
     * @param simpleName the simple class name for the service
     * @return the full class name for the service
     */
    private static String buildClassName(String[] elts, int firstElement, int end, String simpleName) {
        StringBuilder nameBuilder = new StringBuilder();
        for (int offset = firstElement; offset < end; offset++) {
            // avoid blank elements from leading or double slashes
            if (elts[offset].isEmpty()) continue;

            nameBuilder
                    .append(elts[offset].toLowerCase())
                    .append('.');
        }

        nameBuilder.append(simpleName);

        return nameBuilder.toString();
    }

    /**
     * Try loading the given service class. Rather than raise ClassNotFoundException for
     * jars that do not contain any service class, we require that the service class be a
     * "normal" .class file accessible as a classloader resource. If it can be found
     * using ClassLoader.getResource, we proceed to attempt to load it as a class.
     *
     * @param runtime the Ruby runtime into which the extension service will load
     * @param className the class name of the service class
     * @return a ClassExtensionLibrary if the service class was found; null otherwise.
     */
    private static ClassExtensionLibrary tryServiceLoad(Ruby runtime, String className) {
        String classFile = className.replaceAll("\\.", "/") + ".class";

        try {
            // quietly try to load the class, which must be reachable as a .class resource
            URL resource = runtime.getJRubyClassLoader().getResource(classFile);
            if (resource != null) {
                Class theClass = runtime.getJavaSupport().loadJavaClass(className);
                return new ClassExtensionLibrary(className + ".java", theClass);
            }
        } catch (ClassNotFoundException cnfe) {
            if (runtime.isDebug()) cnfe.printStackTrace();
        } catch (UnsupportedClassVersionError ucve) {
            if (runtime.isDebug()) ucve.printStackTrace();
            throw runtime.newLoadError("JRuby ext built for wrong Java version in `" + className + "': " + ucve, className);
        }

        // The class doesn't exist
        return null;
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
