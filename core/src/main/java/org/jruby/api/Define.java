package org.jruby.api;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.embed.Extension;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;

import static org.jruby.api.Access.objectClass;

/**
 * Methods which help extension authors define their extension.
 */
public class Define {
    /**
     * Define a new class under the Object namespace. Roughly equivalent to
     * rb_define_class in MRI.  This is a convenience method as you could use
     * {@link RubyModule#defineClassUnder(ThreadContext, String, RubyClass, ObjectAllocator)} on
     * a reference to Object but this is such a common thing that it is its own method.
     * Note: If a class already exists for this name it returns it or errors if it is not a Class at all.
     *
     * @param context the current thread context
     * @param name The name for the new class
     * @param superClass The super class for the new class
     * @param allocator An ObjectAllocator instance that can construct
     * instances of the new class.
     * @return The new class
     */
    public static RubyClass defineClass(ThreadContext context, String name, RubyClass superClass, ObjectAllocator allocator) {
        return objectClass(context).defineClassUnder(context, name, superClass, allocator);
    }

    /**
     * Define a new module under the Object namespace. Roughly equivalent to
     * rb_define_module in MRI.  Note: If a module already exists for this name
     * it returns it or errors if it is not a Module at all.
     *
     * @param name The name of the new module
     * @return The new module or existing one if it has already been defined.
     */
    @Extension
    public static RubyModule defineModule(ThreadContext context, String name) {
        return context.runtime.defineModuleUnder(context, name, objectClass(context));
    }


}
