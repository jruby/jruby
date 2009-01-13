/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast.types;

import org.jruby.Ruby;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public interface IEqlNode {
    public boolean eql(IRubyObject otherValue, ThreadContext context, Ruby runtime, IRubyObject self, Block aBlock);
}
