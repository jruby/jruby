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
 *
 * @author headius
 */
public interface ObjectAllocator {
    public IRubyObject allocate(Ruby runtime, RubyClass klazz);
    
    public static final ObjectAllocator NOT_ALLOCATABLE_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            throw runtime.newTypeError("allocator undefined for " + klass.getName());
        }
    };
}
