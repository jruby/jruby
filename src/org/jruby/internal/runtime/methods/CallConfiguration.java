/*
 * CallConfiguration.java
 * 
 * Created on Jul 13, 2007, 6:51:14 PM
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.internal.runtime.JumpTarget;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public abstract class CallConfiguration {
    public static final CallConfiguration RUBY_FULL = new CallConfiguration() {
        public void pre(ThreadContext context, IRubyObject self, RubyModule implementer, Arity arity, String name, IRubyObject[] args, Block block, StaticScope scope, JumpTarget jumpTarget) {
            context.preRubyMethodFull(implementer, name, self, args, arity.required(), block, scope, jumpTarget);
        }
        
        public void post(ThreadContext context) {
            context.postRubyMethodFull();
        }
    };
    public static final CallConfiguration JAVA_FULL = new CallConfiguration() {
        public void pre(ThreadContext context, IRubyObject self, RubyModule implementer, Arity arity, String name, IRubyObject[] args, Block block, StaticScope scope, JumpTarget jumpTarget) {
            context.preJavaMethodFull(implementer, name, self, args, arity.required(), block, jumpTarget);
        }
        
        public void post(ThreadContext context) {
            context.postJavaMethodFull();
        }
    };
    public static final CallConfiguration JAVA_FAST = new CallConfiguration() {
        public void pre(ThreadContext context, IRubyObject self, RubyModule implementer, Arity arity, String name, IRubyObject[] args, Block block, StaticScope scope, JumpTarget jumpTarget) {
        }
        
        public void post(ThreadContext context) {
        }
    };

    private CallConfiguration() {
    }
    
    public abstract void pre(ThreadContext context, IRubyObject self, RubyModule implementer, Arity arity, String name, IRubyObject[] args, Block block, StaticScope scope, JumpTarget jumpTarget);
    public abstract void post(ThreadContext context);
}
