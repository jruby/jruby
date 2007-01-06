/*
 * ObjectAllocator.java
 *
 * Created on January 6, 2007, 1:35 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.runtime;

import org.jruby.IRuby;
import org.jruby.RubyClass;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public interface ObjectAllocator {
    public IRubyObject allocate(IRuby runtime, RubyClass klazz);
    
    public static final ObjectAllocator NOT_ALLOCATABLE_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(IRuby runtime, RubyClass klass) {
            throw new RuntimeException("Ruby \"" + klass.getName() + "\" object can not be allocated");
        }
    };
}
