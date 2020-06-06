/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.util;

import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public interface TypeCoercer {
    public Object coerce(IRubyObject self);
}
