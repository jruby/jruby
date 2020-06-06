/*
 * ObjectAllocator.java
 *
 * Created on January 6, 2007, 1:35 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * An allocator for a Ruby object.
 */
public interface ObjectAllocator {
    IRubyObject allocate(Ruby runtime, RubyClass klazz);
    
    ObjectAllocator NOT_ALLOCATABLE_ALLOCATOR = (runtime, klass) -> {
        throw runtime.newTypeError("allocator undefined for " + klass.getName());
    };
}
