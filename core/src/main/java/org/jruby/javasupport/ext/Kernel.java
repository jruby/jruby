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
 * Copyright (C) 2016 The JRuby Team
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

package org.jruby.javasupport.ext;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.javasupport.Java;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.runtime.Visibility.PUBLIC;

/**
 * Kernel added Java short-cut methods.
 *
 * @author kares
 */
public final class Kernel {

    public static void define(final Ruby runtime) {
        final RubyModule Kernel = runtime.getKernel();
        Kernel.addMethodInternal("java", new JavaPackageMethod(Kernel, "java"));
        Kernel.addMethodInternal("javax", new JavaPackageMethod(Kernel, "javax"));
        Kernel.addMethodInternal("javafx", new JavaPackageMethod(Kernel, "javafx"));
        Kernel.addMethodInternal("com", new JavaPackageMethod(Kernel, "com"));
        Kernel.addMethodInternal("org", new JavaPackageMethod(Kernel, "org"));
    }

    private static final class JavaPackageMethod extends JavaMethod.JavaMethodZero {
        private IRubyObject pkg;

        JavaPackageMethod(RubyModule implClass, String name) {
            super(implClass, PUBLIC, name);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            IRubyObject pkg = this.pkg;
            if (pkg == null) {
                this.pkg = pkg = get_pkg(context, name);
            }
            return pkg;
        }
    }

    private static IRubyObject get_pkg(final ThreadContext context, final String name) {
        RubyModule module = Java.getJavaPackageModule(context.runtime, name);
        return module == null ? context.nil : module;
    }

}
