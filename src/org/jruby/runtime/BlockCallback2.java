/*
 * BlockCallback2.java
 *
 * Created on January 4, 2007, 1:12 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.runtime;

import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public interface BlockCallback2 {
    public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject[] args);
}
