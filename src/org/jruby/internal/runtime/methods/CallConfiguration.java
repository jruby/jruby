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
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.JumpTarget;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public abstract class CallConfiguration {
    public static final CallConfiguration FRAME_AND_SCOPE = new CallConfiguration() {
        public void pre(ThreadContext context, IRubyObject self, RubyModule implementer, String name, Block block, StaticScope scope, JumpTarget jumpTarget) {
            context.preMethodFrameAndScope(implementer, name, self, block, scope, jumpTarget);
        }
        
        public void post(ThreadContext context) {
            context.postMethodFrameAndScope();
        }
        
        public String name() {
            return "FRAME_AND_SCOPE";
        }
    };
    public static final CallConfiguration FRAME_ONLY = new CallConfiguration() {
        public void pre(ThreadContext context, IRubyObject self, RubyModule implementer, String name, Block block, StaticScope scope, JumpTarget jumpTarget) {
            context.preMethodFrameOnly(implementer, name, self, block, jumpTarget);
        }
        
        public void post(ThreadContext context) {
            context.postMethodFrameOnly();
        }
        
        public String name() {
            return "FRAME_ONLY";
        }
    };
    public static final CallConfiguration SCOPE_ONLY = new CallConfiguration() {
        public void pre(ThreadContext context, IRubyObject self, RubyModule implementer, String name, Block block, StaticScope scope, JumpTarget jumpTarget) {
            context.preMethodScopeOnly(implementer, scope);
        }
        
        public void post(ThreadContext context) {
            context.postMethodScopeOnly();
        }
        
        public String name() {
            return "SCOPE_ONLY";
        }
    };
    public static final CallConfiguration NO_FRAME_NO_SCOPE = new CallConfiguration() {
        public void pre(ThreadContext context, IRubyObject self, RubyModule implementer, String name, Block block, StaticScope scope, JumpTarget jumpTarget) {
        }
        
        public void post(ThreadContext context) {
        }
        
        public String name() {
            return "NO_FRAME_NO_SCOPE";
        }
        
        @Override
        public boolean isNoop() {
            return true;
        }
    };
    public static final CallConfiguration BACKTRACE_ONLY = new CallConfiguration() {
        public void pre(ThreadContext context, IRubyObject self, RubyModule implementer, String name, Block block, StaticScope scope, JumpTarget jumpTarget) {
            context.preMethodBacktraceOnly(name);
        }
        
        public void post(ThreadContext context) {
            context.postMethodBacktraceOnly();
        }
        
        public String name() {
            return "BACKTRACE_ONLY";
        }
    };
    public static final CallConfiguration BACKTRACE_AND_SCOPE = new CallConfiguration() {
        public void pre(ThreadContext context, IRubyObject self, RubyModule implementer, String name, Block block, StaticScope scope, JumpTarget jumpTarget) {
            context.preMethodBacktraceAndScope(name, implementer, scope);
        }
        
        public void post(ThreadContext context) {
            context.postMethodBacktraceAndScope();
        }
        
        public String name() {
            return "BACKTRACE_AND_SCOPE";
        }
    };
    
    public static CallConfiguration getCallConfigByAnno(JRubyMethod anno) {
        return getCallConfig(anno.frame(), anno.scope(), anno.backtrace());
    }
    
    public static CallConfiguration getCallConfig(boolean frame, boolean scope, boolean backtrace) {
        if (frame) {
            if (scope) {
                return FRAME_AND_SCOPE;
            } else {
                return FRAME_ONLY;
            }
        } else if (scope) {
            if (backtrace) {
                return BACKTRACE_AND_SCOPE;
            } else {
                return SCOPE_ONLY;
            }
        } else if (backtrace) {
            return BACKTRACE_ONLY;
        } else {
            return NO_FRAME_NO_SCOPE;
        }
    }

    private CallConfiguration() {
    }
    
    public abstract void pre(ThreadContext context, IRubyObject self, RubyModule implementer, String name, Block block, StaticScope scope, JumpTarget jumpTarget);
    public abstract void post(ThreadContext context);
    public abstract String name();
    public boolean isNoop() {
        return false;
    }
}
