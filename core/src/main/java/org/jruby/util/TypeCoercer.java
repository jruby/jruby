/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.util;

import org.jruby.runtime.builtin.IRubyObject;

public interface TypeCoercer {
    Object coerce(IRubyObject self);
}
