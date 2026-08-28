/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
package org.jruby.util;

import org.objectweb.asm.ClassWriter;

/**
 * ASM helpers for JRuby.
 *
 * @author kares
 */
public abstract class ASM {

    private ASM() { /* no instances */ }

    public static ClassWriter newClassWriter() {
        return new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
    }

    public static ClassWriter newClassWriter(ClassLoader loader) {
        return newClassWriter(loader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
    }

    public static ClassWriter newClassWriter(ClassLoader loader, final int flags) {
        return new ClassLoaderAwareWriter(loader, flags);
    }

    /**
     * NOTE: required to account for ASM calculating stack-map frames
     * in which case it might need to know of JRuby's dynamically loaded classes
     *
     * @author kares
     */
    private static class ClassLoaderAwareWriter extends ClassWriter {

        private final ClassLoader loader;

        ClassLoaderAwareWriter(final ClassLoader loader, final int flags) {
            super(flags);
            this.loader = loader != null ? loader : ASM.class.getClassLoader();
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            try {
                return getCommonSuperClassImpl(type1, type2, loader);
            }
            catch (TypeNotPresentException ex) {
                throw ex;
            }
        }

        private static String getCommonSuperClassImpl(String type1, String type2, final ClassLoader loader)
                throws TypeNotPresentException {
            Class<?> class1;
            try {
                class1 = Class.forName(type1.replace('/', '.'), false, loader);
            }
            catch (ClassNotFoundException|Error e) {
                throw new TypeNotPresentException(type1, e);
            }
            Class<?> class2;
            try {
                class2 = Class.forName(type2.replace('/', '.'), false, loader);
            }
            catch (ClassNotFoundException|Error e) {
                throw new TypeNotPresentException(type2, e);
            }
            if (class1.isAssignableFrom(class2)) {
                return type1;
            }
            if (class2.isAssignableFrom(class1)) {
                return type2;
            }
            if (class1.isInterface() || class2.isInterface()) {
                return "java/lang/Object";
            }
            do {
                class1 = class1.getSuperclass();
            }
            while (!class1.isAssignableFrom(class2));

            return class1.getName().replace('.', '/');
        }

    }

}
