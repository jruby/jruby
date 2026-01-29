package org.jruby.compiler;

import org.jruby.MetaClass;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;

import static org.jruby.api.Access.classClass;

/**
 * Blocks and methods both share same full build mechanism so they implement this to be buildable.
 * @param <T> result value to compile
 */
public interface Compilable<T> {
    void setCallCount(int count);
    @Deprecated(since = "10.0.0.0")
    default void completeBuild(T buildResult) {
        completeBuild(getImplementationClass().getCurrentContext(), buildResult);
    }
    default void completeBuild(ThreadContext context, T buildResult) {
        completeBuild(buildResult);
    }

    /**
     * Force this Compilable to finish its build process.
     *
     * Override this and {@link #isBuildComplete()} for implementations that have a build process.
     *
     * @param context the current thread context
     * @return true if the forced build completed; false otherwise
     */
    default boolean forceBuild(ThreadContext context) {
        return false;
    }

    /**
     * Indicates whether this Compilable has completed its build process.
     *
     * Override this and {@link #forceBuild(ThreadContext)} for implementations that have a build process.
     *
     * @return true if the build has completed; false otherwise.
     */
    default boolean isBuildComplete() {
        return true;
    }

    IRScope getIRScope();
    InterpreterContext ensureInstrsReady();

    /**
     * Return the owning module/class name.
     * @return method/block owner's name
     */
    @Deprecated(since = "10.0.0.0")
    default String getOwnerName() {
        return getOwnerName(getImplementationClass().getCurrentContext());
    }

    default String getOwnerName(ThreadContext context) {
        RubyModule implClass = getImplementationClass();
        return implClass == null ? null : resolveFullName(context, implClass);
    }

    /**
     * @return method/closure identifier
     */
    String getName();

    /**
     * @return method/block source file
     */
    String getFile();

    /**
     * @return method/block source file line
     */
    int getLine();

    public RubyModule getImplementationClass();

    @Deprecated(since = "9.2.9.0")
    default String getClassName(ThreadContext context) {
        return getOwnerName(context);
    }

    /**
     * Resolve the fully qualified name.
     * @param implementationClass
     * @return class/module name e.g. Foo::Bar::Baz
     */
    static String resolveFullName(ThreadContext context, RubyModule implementationClass) {
        if (implementationClass.isSingleton()) {
            MetaClass metaClass = (MetaClass)implementationClass;
            RubyClass realClass = metaClass.getRealClass();
            // if real class is Class then use the attached class's name
            return realClass == classClass(context) ?
                    ((RubyClass) metaClass.getAttached()).getName(context) :
                    realClass.getName(context);
        }

        return implementationClass.getName(context);  // use the class name
    }

}
