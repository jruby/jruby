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
 * Copyright (C) 2015 The JRuby Team
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

package org.jruby.javasupport;

import org.jruby.IncludedModuleWrapper;
import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.NullMethod;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ClassProvider;
import org.jruby.util.TypeConverter;

import static org.jruby.runtime.Visibility.PRIVATE;

/**
 * A "thin" Java package wrapper (for the runtime to see them as Ruby objects).
 *
 * @since 9K
 * @note previously <code>JavaPackageModuleTemplate</code> in Ruby code
 * @author kares
 */
@JRubyClass(name="Java::JavaPackage", parent="Module")
public class JavaPackage extends RubyModule {

    static RubyModule createJavaPackageClass(final Ruby runtime, final RubyModule Java) {
        RubyClass superClass = new BlankSlateWrapper(runtime, runtime.getModule(), runtime.getKernel());
        RubyClass JavaPackage = RubyClass.newClass(runtime, superClass);
        JavaPackage.setMetaClass(runtime.getModule());
        JavaPackage.setAllocator(ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        ((MetaClass) JavaPackage.makeMetaClass(superClass)).setAttached(JavaPackage);

        JavaPackage.setBaseName("JavaPackage");

        JavaPackage.setParent(Java);
        Java.setConstant("JavaPackage", JavaPackage); // Java::JavaPackage
        // JavaPackage.setReifiedClass(JavaPackage.class);

        JavaPackage.defineAnnotatedMethods(JavaPackage.class);
        return JavaPackage;
    }

    static RubyModule newPackage(Ruby runtime, CharSequence name, RubyModule parent) {
        final JavaPackage pkgModule = new JavaPackage(runtime, name);
        // intentionally do NOT set pkgModule.setParent(parent);

        // this is where we'll get connected when classes are opened using
        // package module syntax.
        pkgModule.addClassProvider( JavaClassProvider.INSTANCE );
        return pkgModule;
    }

    static CharSequence buildPackageName(final RubyModule parentPackage, final String name) {
        return ((JavaPackage) parentPackage).packageRelativeName(name);
    }

    final String packageName;

    private JavaPackage(final Ruby runtime, final CharSequence packageName) {
        super(runtime, runtime.getJavaSupport().getJavaPackageClass());
        this.packageName = packageName.toString();
    }

    public String getPackageName() {
        return packageName;
    }

    // NOTE: name is Ruby name not pkg.name ~ maybe it should be just like with JavaClass?

    @JRubyMethod(name = "package_name", alias = "to_s")
    public RubyString package_name() {
        return getRuntime().newString(packageName);
    }

    @Override
    public RubyString to_s() { return package_name(); }

    @Override
    @JRubyMethod
    public IRubyObject inspect() {
        return getRuntime().newString(getName()); // super.to_s()
    }

    @Override
    @JRubyMethod(name = "===")
    public RubyBoolean op_eqq(ThreadContext context, IRubyObject obj) {
        // maybe we could handle java.lang === java.lang.reflect as well ?
        return RubyBoolean.newBoolean(context, obj == this || isInstance(obj));
    }

    @JRubyMethod(name = "const_missing", required = 1)
    public IRubyObject const_missing(final ThreadContext context, final IRubyObject name) {
        return relativeJavaClassOrPackage(context, name, false);
    }

    @JRubyMethod(name = "const_get", required = 1)
    public final IRubyObject const_get(final ThreadContext context, final IRubyObject name) {
        // skip constant validation and do not inherit or include object
        IRubyObject constant = getConstantNoConstMissing(name.toString(), false, false);
        if ( constant != null ) return constant;
        return relativeJavaClassOrPackage(context, name, false); // e.g. javax.const_get(:script)
    }

    @JRubyMethod(name = "const_get", required = 2)
    public final IRubyObject const_get(final ThreadContext context,
        final IRubyObject name, final IRubyObject inherit) {
        IRubyObject constant = getConstantNoConstMissing(name.toString(), inherit.isTrue(), false);
        if ( constant != null ) return constant;
        return relativeJavaClassOrPackage(context, name, false);
    }

    @Override // so that e.g. java::util gets stored as expected
    public final IRubyObject storeConstant(String name, IRubyObject value) {
        // skip constant name validation
        assert value != null : "value is null";

        ensureConstantsSettable();
        return constantTableStore(name, value);
    }

    @Override // skip constant name assert
    public final boolean hasConstant(String name) {
        return constantTableContains(name);
    }

    @Override // skip constant name assert
    public final IRubyObject fetchConstant(String name, boolean includePrivate) {
        ConstantEntry entry = constantEntryFetch(name);
        if (entry == null) return null;
        if (entry.hidden && !includePrivate) {
            throw getRuntime().newNameError("private constant " + getName() + "::" + name + " referenced", name);
        }
        return entry.value;
    }

    @Override // skip constant name assert
    public final IRubyObject deleteConstant(String name) {
        assert name != null : "name is null";
        ensureConstantsSettable();
        return constantTableRemove(name);
    }

    final CharSequence packageRelativeName(final CharSequence name) {
        final int length = packageName.length();
        final StringBuilder fullName = new StringBuilder(length + 1 + name.length());
        // packageName.length() > 0 ? package + '.' + name : name;
        fullName.append(packageName);
        if ( length > 0 ) fullName.append('.');
        return fullName.append(name);
    }

    private RubyModule relativeJavaClassOrPackage(final ThreadContext context,
        final IRubyObject name, final boolean cacheMethod) {
        return Java.getProxyOrPackageUnderPackage(context, this, name.toString(), cacheMethod);
    }

    RubyModule relativeJavaProxyClass(final Ruby runtime, final IRubyObject name) {
        final String fullName = packageRelativeName( name.toString() ).toString();
        return Java.getProxyClass(runtime, Java.getJavaClass(runtime, fullName));
    }

    @JRubyMethod(name = "respond_to?")
    public IRubyObject respond_to_p(final ThreadContext context, IRubyObject name) {
        return respond_to(context, name, false);
    }

    @JRubyMethod(name = "respond_to?")
    public IRubyObject respond_to_p(final ThreadContext context, IRubyObject name, IRubyObject includePrivate) {
        return respond_to(context, name, includePrivate.isTrue());
    }

    private IRubyObject respond_to(final ThreadContext context, IRubyObject mname, final boolean includePrivate) {
        RubySymbol name = TypeConverter.checkID(mname);

        if (getMetaClass().respondsToMethod(name.idString(), !includePrivate)) return context.tru;
        /*
        if ( ( name = BlankSlateWrapper.handlesMethod(name) ) != null ) {
            RubyBoolean bound = checkMetaClassBoundMethod(context, name, includePrivate);
            if ( bound != null ) return bound;
            return context.fals; // un-bound (removed) method
        }
        */

        //if ( ! (mname instanceof RubySymbol) ) mname = context.runtime.newSymbol(name);
        //IRubyObject respond = Helpers.invoke(context, this, "respond_to_missing?", mname, RubyBoolean.newBoolean(context, includePrivate));
        //return RubyBoolean.newBoolean(context, respond.isTrue());

        return context.nil; // NOTE: this is wrong - should be true but compatibility first, for now
    }

    private RubyBoolean checkMetaClassBoundMethod(final ThreadContext context, final String name, final boolean includePrivate) {
        // getMetaClass().isMethodBound(name, !includePrivate, true)
        DynamicMethod method = getMetaClass().searchMethod(name);
        if ( ! method.isUndefined() && ! method.isNotImplemented() ) {
            if ( ! includePrivate && method.getVisibility() == PRIVATE ) {
                return context.fals;
            }
            return context.tru;
        }
        return null;
    }

    @JRubyMethod(name = "respond_to_missing?")
    public IRubyObject respond_to_missing_p(final ThreadContext context, IRubyObject name) {
        return respond_to_missing(context, name, false);
    }

    @JRubyMethod(name = "respond_to_missing?")
    public IRubyObject respond_to_missing_p(final ThreadContext context, IRubyObject name, IRubyObject includePrivate) {
        return respond_to_missing(context, name, includePrivate.isTrue());
    }

    private RubyBoolean respond_to_missing(final ThreadContext context, IRubyObject mname, final boolean includePrivate) {
        return RubyBoolean.newBoolean(context, BlankSlateWrapper.handlesMethod(TypeConverter.checkID(mname).idString()) == null);
    }

    @JRubyMethod(name = "method_missing")
    public IRubyObject method_missing(ThreadContext context, final IRubyObject name) {
        // NOTE: getProxyOrPackageUnderPackage binds the (cached) method for us
        return Java.getProxyOrPackageUnderPackage(context, this, name.toString(), true);
    }

    @JRubyMethod(name = "method_missing", rest = true)
    public IRubyObject method_missing(ThreadContext context, final IRubyObject[] args) {
        if (args.length > 1) {
            throw packageMethodArgumentMismatch(context.runtime, this, args[0].toString(), args.length - 1);
        }
        return method_missing(context, args[0]);
    }
    
    static RaiseException packageMethodArgumentMismatch(final Ruby runtime, final RubyModule pkg,
        final String method, final int argsLength) {
        String packageName = ((JavaPackage) pkg).packageName;
        return runtime.newArgumentError(
                "Java package '" + packageName + "' does not have a method `" +
                        method + "' with " + argsLength + (argsLength == 1 ? " argument" : " arguments")
        );
    }

    public final boolean isAvailable() {
        // may be null if no package information is available from the archive or codebase
        return Package.getPackage(packageName) != null;
    }

    @JRubyMethod(name = "available?")
    public IRubyObject available_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context, isAvailable());
    }

    @JRubyMethod(name = "sealed?")
    public IRubyObject sealed_p(ThreadContext context) {
        final Package pkg = Package.getPackage(packageName);
        if ( pkg == null ) return context.nil;
        return RubyBoolean.newBoolean(context, pkg.isSealed());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T toJava(Class<T> target) {
        if ( target.isAssignableFrom( Package.class ) ) {
            return target.cast(Package.getPackage(packageName));
        }
        return super.toJava(target);
    }

    private static class JavaClassProvider implements ClassProvider {

        static final JavaClassProvider INSTANCE = new JavaClassProvider();

        public RubyClass defineClassUnder(RubyModule pkg, String name, RubyClass superClazz) {
            // shouldn't happen, but if a superclass is specified, it's not ours
            if ( superClazz != null ) return null;

            final String subPackageName = JavaPackage.buildPackageName(pkg, name).toString();

            final Ruby runtime = pkg.getRuntime();
            Class<?> javaClass = Java.getJavaClass(runtime, subPackageName);
            return (RubyClass) Java.getProxyClass(runtime, javaClass);
        }

        public RubyModule defineModuleUnder(RubyModule pkg, String name) {
            final String subPackageName = JavaPackage.buildPackageName(pkg, name).toString();

            final Ruby runtime = pkg.getRuntime();
            Class<?> javaClass = Java.getJavaClass(runtime, subPackageName);
            return Java.getInterfaceModule(runtime, javaClass);
        }

    }

    /**
     * This special module wrapper is used by the Java "package modules" in order to
     * simulate a blank slate. Only a certain subset of method names will carry
     * through to searching the superclass, with all others returning null and
     * triggering the method_missing call needed to handle lazy Java package
     * discovery.
     *
     * Because this is in the hierarchy, it does mean any methods that are not Java
     * packages or otherwise defined on the <code>Java::JavaPackage</code> will
     * be inaccessible.
     */
    static final class BlankSlateWrapper extends IncludedModuleWrapper {

        BlankSlateWrapper(Ruby runtime, RubyClass superClass, RubyModule delegate) {
            super(runtime, superClass, delegate);
        }

        @Override
        protected DynamicMethod searchMethodCommon(String id) {
            // this module is special and only searches itself;
            if ("superclass".equals(id)) {
                return new MethodValue(id, superClass); // JavaPackage.superclass
            }
            return (id = handlesMethod(id)) != null ? superClass.searchMethodInner(id) : NullMethod.INSTANCE;
        }

        private static class MethodValue extends DynamicMethod {

            private final IRubyObject value;

            MethodValue(final String name, final IRubyObject value) {
                super(name);
                this.value = value;
            }

            public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                return call(context, self, clazz, name);
            }

            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name) {
                return value;
            }

            @Override
            public DynamicMethod dup() {
                try {
                    return (DynamicMethod) super.clone();
                }
                catch (CloneNotSupportedException ex) {
                    throw new AssertionError(ex);
                }
            }

            @Deprecated @Override
            public Arity getArity() { return Arity.NO_ARGUMENTS; }

            public Signature getSignature() {
                return Signature.NO_ARGUMENTS;
            }
        }

        private static String handlesMethod(final String name) {
            // FIXME: We should consider pure-bytelist search here.
            switch (name) {
                case "class" : case "singleton_class" : return name;
                case "object_id" : case "name" : return name;
                // these are handled already at the JavaPackage.class :
                // case "const_get" : case "const_missing" : case "method_missing" :
                case "const_set" : return name;
                case "inspect" : case "to_s" : return name;
                // these are handled bellow in switch (name.charAt(0))
                // case "__method__" : case "__send__" : case "__id__" :

                //case "require" : case "load" :
                case "throw" : case "catch" : //case "fail" : case "raise" :
                //case "exit" : case "at_exit" :
                    return name;

                case "singleton_method_added" :
                // JavaPackageModuleTemplate handled "singleton_method_added"
                case "singleton_method_undefined" :
                case "singleton_method_removed" :
                case "define_singleton_method" :
                    return name;

                // NOTE: these should maybe get re-thought and deprecated (for now due compatibility)
                case "__constants__" : return "constants";
                case "__methods__" : return "methods";
            }

            final int last = name.length() - 1;
            if ( last >= 0 ) {
                switch (name.charAt(last)) {
                    case '?' : case '!' : case '=' :
                        return name;
                }
                switch (name.charAt(0)) {
                    case '<' : case '>' : case '=' : // e.g. ==
                        return name;
                    case '_' : // e.g. __send__
                        if ( last > 0 && name.charAt(1) == '_' ) {
                            return name;
                        }
                }
            }

            //if ( last >= 5 && (
            //       name.indexOf("method") >= 0 || // method, instance_methods, singleton_methods ...
            //       name.indexOf("variable") >= 0 || // class_variables, class_variable_get, instance_variables ...
            //       name.indexOf("constant") >= 0 ) ) { // constants, :public_constant, :private_constant
            //    return true;
            //}

            return null;
        }

        @Override
        public void addSubclass(RubyClass subclass) { /* noop */ }

    }

}
