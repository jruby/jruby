package org.jruby.compiler;

import org.jruby.MetaClass;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;

/**
 * Blocks and methods both share same full build mechanism so they implement this to be buildable.
 */
public interface Compilable<T> {
    void setCallCount(int count);
    default void completeBuild(T buildResult) {
        completeBuild(getImplementationClass().getRuntime().getCurrentContext(), buildResult);
    }
    default void completeBuild(ThreadContext context, T buildResult) {
        completeBuild(buildResult);
    }
    IRScope getIRScope();
    InterpreterContext ensureInstrsReady();

    /**
     * Return the owning module/class name.
     * @return method/block owner's name
     */
    default String getOwnerName() {
        RubyModule implClass = getImplementationClass();
        return implClass == null ? null : resolveFullName(implClass);
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

    @Deprecated
    default String getClassName(ThreadContext context) {
        return getOwnerName();
    }

    /**
     * Resolve the fully qualified name.
     * @param implementationClass
     * @return class/module name e.g. Foo::Bar::Baz
     */
    static String resolveFullName(RubyModule implementationClass) {
        String className;
        if (implementationClass.isSingleton()) {
            MetaClass metaClass = (MetaClass)implementationClass;
            RubyClass realClass = metaClass.getRealClass();
            // if real class is Class
            if (realClass == implementationClass.getRuntime().getClassClass()) {
                // use the attached class's name
                className = ((RubyClass) metaClass.getAttached()).getName();
            } else {
                // use the real class name
                className = realClass.getName();
            }
        } else {
            // use the class name
            className = implementationClass.getName();
        }
        return className;
    }

}
