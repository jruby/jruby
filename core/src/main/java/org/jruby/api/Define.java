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
     * rb_define_class in MRI.
     *
     * @param context the current thread context
     * @param name The name for the new class
     * @param superClass The super class for the new class
     * @param allocator An ObjectAllocator instance that can construct
     * instances of the new class.
     * @return The new class
     */
    public static RubyClass defineClass(ThreadContext context, String name, RubyClass superClass, ObjectAllocator allocator) {
        return defineClassUnder(context, name, superClass, allocator, objectClass(context));
    }

    /**
     * Define a new class with the given name under the given module or class
     * namespace. Roughly equivalent to rb_define_class_under in MRI.
     *
     * If the name specified is already bound, its value will be returned if:
     * * It is a class
     * * No new superclass is being defined
     *
     * @param context the current thread context
     * @param name The name for the new class
     * @param superClass The super class for the new class
     * @param allocator An ObjectAllocator instance that can construct
     * instances of the new class.
     * @param parent The namespace under which to define the new class
     * @return The new class
     */
    public static RubyClass defineClassUnder(ThreadContext context, String name, RubyClass superClass,
                                             ObjectAllocator allocator, RubyModule parent) {
        return context.runtime.defineClassUnder(name, superClass, allocator, parent, null);
    }

    /**
     * Define a new module under the Object namespace. Roughly equivalent to
     * rb_define_module in MRI.  Note: If a module already exists for this name
     * it returns it.
     *
     * @param name The name of the new module
     * @return The new module or existing one if it has already been defined.
     */
    @Extension
    public static RubyModule defineModule(ThreadContext context, String name) {
        return context.runtime.defineModuleUnder(name, objectClass(context));
    }


}
