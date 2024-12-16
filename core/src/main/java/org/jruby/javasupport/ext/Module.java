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
 * Copyright (C) 2021 The JRuby Team
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
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.NameError;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaPackage;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.JavaUtilities;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import static org.jruby.api.Access.moduleClass;
import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Error.argumentError;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.runtime.Visibility.PUBLIC;

/**
 * Module Java extensions, namely java_import and include_package.
 *
 * @author kares
 */
public class Module {

    public static void define(ThreadContext context, RubyClass _Module) {
        _Module.defineMethods(context, Module.class);
    }

    @JRubyMethod(name = "import", visibility = PRIVATE)
    public static IRubyObject import_(ThreadContext context, IRubyObject self, IRubyObject arg, Block block) {
        if (arg instanceof RubyString) {
            final String name = ((RubyString) arg).decodeString();
            int i = name.lastIndexOf('.');
            if (i != -1 && i + 1 < name.length() && Character.isUpperCase(name.charAt(i + 1))) {
                return java_import(context, self, arg, block);
            }
        } else if (arg instanceof RubyModule) {
            if (((RubyModule) arg).getJavaProxy() && !(arg instanceof JavaPackage)) {
                return java_import(context, self, arg, block);
            }
        }
        return include_package(context, self, arg);
    }

    @JRubyMethod(visibility = PRIVATE)
    public static IRubyObject java_import(ThreadContext context, IRubyObject self, IRubyObject arg, Block block) {
        return arg instanceof RubyArray ?
                java_import(context, self, ((RubyArray<?>) arg).toJavaArrayMaybeUnsafe(), block) :
                newArray(context, javaImport(context, (RubyModule) self, arg, block) );
    }

    @JRubyMethod(rest = true, visibility = PRIVATE)
    public static IRubyObject java_import(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        IRubyObject[] classes = ((RubyArray) RubyArray.newArrayNoCopy(context.runtime, args).flatten(context)).toJavaArrayMaybeUnsafe();
        for (int i = 0; i < classes.length; i++) {
            classes[i] = javaImport(context, (RubyModule) self, classes[i], block);
        }
        return newArray(context, classes);
    }

    private static IRubyObject javaImport(ThreadContext context, RubyModule target, IRubyObject klass, Block block) {
        final Class<?> javaClass;
        if (klass instanceof RubyString) {
            final String className = klass.asJavaString();
            if (!JavaUtilities.validJavaIdentifier(className)) {
                throw argumentError(context, "not a valid Java identifier: " + className);
            }
            if (className.contains("::")) {
                throw argumentError(context, "must use Java style name: " + className);
            }
            javaClass = Java.getJavaClass(context.runtime, className); // raises NameError if not found
        } else if (klass instanceof JavaPackage) {
            throw argumentError(context, "java_import does not work for Java packages (try include_package instead)");
        } else if (klass instanceof RubyModule) {
            javaClass = JavaUtil.getJavaClass((RubyModule) klass, null);
            if (javaClass == null) {
                throw argumentError(context, "not a Java class or interface: " + klass.inspect(context));
            }
        } else {
            throw argumentError(context, "invalid Java class or interface: " + klass.inspect(context) + " (of type " + klass.getType() + ")");
        }

        String constant;
        if (block.isGiven()) {
            int i = javaClass.getName().lastIndexOf('.');
            String packageName = i != -1 ? javaClass.getName().substring(0, i) : "";
            String className = javaClass.getSimpleName();
            IRubyObject ret = block.yieldSpecific(context, newString(context, packageName), newString(context, className));
            constant = ret.convertToString().asJavaString();
        } else {
            constant = javaClass.getSimpleName();
        }

        try {
            return Java.setProxyClass(context.runtime, target, constant, javaClass);
        } catch (NameError e) {
            String message = "cannot import Java class " + javaClass.getName() + " as '" + constant + "' : " + e.getException().getMessage();
            throw (RaiseException) context.runtime.newNameError(message, constant).initCause(e);
        }
    }

    @JRubyMethod(visibility = PRIVATE)
    public static IRubyObject java_alias(final ThreadContext context, final IRubyObject self, IRubyObject new_id, IRubyObject old_id) {
        final IncludedPackages includedPackages = getIncludedPackages(context, (RubyModule) self);
        if (!(new_id instanceof RubySymbol)) new_id = new_id.convertToString().intern();
        if (!(old_id instanceof RubySymbol)) old_id = old_id.convertToString().intern();

        includedPackages.javaAliases.put(((RubySymbol) new_id).idString(), ((RubySymbol) old_id).idString());
        return old_id;
    }

    @JRubyMethod(visibility = PRIVATE)
    public static IRubyObject include_package(final ThreadContext context, final IRubyObject self, IRubyObject pkg) {
        String packageName;
        if (pkg instanceof JavaPackage) {
            packageName = ((JavaPackage) pkg).getPackageName();
        } else if (pkg.respondsTo("package_name")) {
            packageName = pkg.callMethod(context, "package_name").convertToString().asJavaString();
        } else {
            packageName = pkg.convertToString().asJavaString();
        }

        final IncludedPackages includedPackages = getIncludedPackages(context, (RubyModule) self);
        return includedPackages.packages.add(packageName) ? pkg : context.nil;
    }

    private static IncludedPackages getIncludedPackages(final ThreadContext context, final RubyModule target) {
        IncludedPackages includedPackages = (IncludedPackages) target.getInternalVariable("includedPackages");
        if (includedPackages == null) {
            includedPackages = new IncludedPackages();
            target.setInternalVariable("includedPackages", includedPackages);
            ConstMissingMethod method = new ConstMissingMethod(target, includedPackages); // def self.const_missing(constant) :
            Helpers.addInstanceMethod(target.singletonClass(context), asSymbol(context, "const_missing"), method, PUBLIC, context);
        }
        return includedPackages;
    }

    private static class IncludedPackages {

        final Collection<String> packages;
        final Map<String, String> javaAliases;

        IncludedPackages() {
            packages = new LinkedHashSet<>(8);
            javaAliases = new HashMap<>(4);
        }

    }

    private static final class ConstMissingMethod extends JavaMethod.JavaMethodOne {

        private final IncludedPackages includedPackages;

        ConstMissingMethod(RubyModule implClass, IncludedPackages includedPackages) {
            super(implClass, PUBLIC, "const_missing");
            this.includedPackages = includedPackages;
        }

        @Override
        public IRubyObject call(final ThreadContext context, final IRubyObject self, final RubyModule klass,
                                final String name, final IRubyObject constant) {
            final Ruby runtime = context.runtime;

            final String constName = ((RubySymbol) constant).idString();
            final String realName = includedPackages.javaAliases.getOrDefault(constName, constName);

            Class<?> foundClass = null;
            for (String packageName : includedPackages.packages) {
                try {
                    foundClass = Java.loadJavaClass(runtime, packageName + '.' + realName);
                    break;
                } catch (ClassNotFoundException ignore) {
                    // continue try next package
                }
            }

            if (foundClass == null) {
                try {
                    return Helpers.invokeSuper(context, self, klass, "const_missing", constant, Block.NULL_BLOCK);
                } catch (NameError e) { // super didn't find anything either, raise a (new) NameError
                    throw runtime.newNameError(constant + " not found in packages: " + joinedPackageNames(), constant);
                }
            }

            try {
                return Java.setProxyClass(runtime, (RubyModule) self, constName, foundClass);
            } catch (NameError e) {
                String message = "cannot set Java class " + foundClass.getName() + " as '" + constant + "' : " + e.getException().getMessage();
                throw runtime.newNameError(message, constant);
            }
        }

        private String joinedPackageNames() {
            return String.join(", ", includedPackages.packages);
        }
    }

}
