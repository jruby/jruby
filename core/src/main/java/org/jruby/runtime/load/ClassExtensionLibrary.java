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
        // Create package name, by splitting on / and joining all but the last elements with a ".", and downcasing them.
        String[] all = searchName.split("/");

        StringBuilder finName = new StringBuilder();
        for (int i = 0, j = (all.length - 1); i < j; i++) {
            finName.append(all[i].toLowerCase()).append(".");
        }

        try {
            // Make the class name look nice, by splitting on _ and capitalize each segment, then joining
            // the, together without anything separating them, and last put on "Service" at the end.
            String[] last = all[all.length - 1].split("_");
            for (int i = 0, j = last.length; i < j; i++) {
                if ("".equals(last[i])) break;
                finName.append(Character.toUpperCase(last[i].charAt(0))).append(last[i].substring(1));
            }
            finName.append("Service");

            // We don't want a package name beginning with dots, so we remove them
            String className = finName.toString().replaceAll("^\\.*", "");
            String classFile = className.replaceAll("\\.", "/") + ".class";

            // quietly try to load the class, which must be reachable as a .class resource
            URL resource = runtime.getJRubyClassLoader().getResource(classFile);
            if (resource != null) {
                Class theClass = runtime.getJavaSupport().loadJavaClass(className);
                return new ClassExtensionLibrary(className + ".java", theClass);
            }

            return null;
        } catch (ClassNotFoundException cnfe) {
            if (runtime.isDebug()) cnfe.printStackTrace();

            // So apparently the class doesn't exist
            return null;
        } catch (UnsupportedClassVersionError ucve) {
            if (runtime.isDebug()) ucve.printStackTrace();
            throw runtime.newLoadError("JRuby ext built for wrong Java version in `" + finName + "': " + ucve, finName.toString());
        }
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
