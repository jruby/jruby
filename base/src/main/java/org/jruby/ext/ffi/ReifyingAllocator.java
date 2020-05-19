package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

class ReifyingAllocator implements ObjectAllocator {
    private final Class klass;
    private final Constructor cons;

    public ReifyingAllocator(Class klass) {
        this.klass = klass;
        try {
            this.cons = klass.getDeclaredConstructor(Ruby.class, RubyClass.class);
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        }
    }

    public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
        try {
            if (klazz.getReifiedClass() == this.klass) {
                return (IRubyObject) cons.newInstance(runtime, klazz);
            }

            reifyWithAncestors(klazz);
            return klazz.getAllocator().allocate(runtime, klazz);

        } catch (InstantiationException ie) {
            throw runtime.newTypeError("could not allocate " + this.klass + " with default constructor:\n" + ie);
        } catch (IllegalAccessException iae) {
            throw runtime.newSecurityError("could not allocate " + this.klass + " due to inaccessible default constructor:\n" + iae);
        } catch (InvocationTargetException ite) {
            throw runtime.newSecurityError("could not allocate " + this.klass + " due to inaccessible default constructor:\n" + ite);
        }
    }
    
    private static void reifyWithAncestors(RubyClass klazz) {
        
        RubyClass realSuper = klazz.getSuperClass().getRealClass();

        if (realSuper.getReifiedClass() == null) reifyWithAncestors(realSuper);
        synchronized (klazz) {
            klazz.reify();
            klazz.setAllocator(new ReifyingAllocator(klazz.getReifiedClass()));
        }
    }
}
