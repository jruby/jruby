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
import org.jruby.anno.FrameField;
import org.jruby.anno.JRubyMethod;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public enum CallConfiguration {
    FrameFullScopeFull(Framing.Full, Scoping.Full) {
        void pre(ThreadContext context, IRubyObject self, RubyModule implementer, String name, Block block, StaticScope scope) {
            context.preMethodFrameAndScope(implementer, name, self, block, scope);
        }
        void post(ThreadContext context) {
            context.postMethodFrameAndScope();
        }
    },
    FrameFullScopeDummy(Framing.Full, Scoping.Dummy) {
        void pre(ThreadContext context, IRubyObject self, RubyModule implementer, String name, Block block, StaticScope scope) {
            context.preMethodFrameAndDummyScope(implementer, name, self, block, scope);
        }
        void post(ThreadContext context) {
            context.postMethodFrameAndScope();
        }
    },
    FrameFullScopeNone (Framing.Full, Scoping.None) {
        void pre(ThreadContext context, IRubyObject self, RubyModule implementer, String name, Block block, StaticScope scope) {
            context.preMethodFrameOnly(implementer, name, self, block);
        }
        void post(ThreadContext context) {
            context.postMethodFrameOnly();
        }
    },
    FrameBacktraceScopeFull (Framing.Backtrace, Scoping.Full) {
        void pre(ThreadContext context, IRubyObject self, RubyModule implementer, String name, Block block, StaticScope scope) {
            context.preMethodBacktraceAndScope(name, scope);
        }
        void post(ThreadContext context) {
            context.postMethodBacktraceAndScope();
        }
    },
    FrameBacktraceScopeDummy (Framing.Backtrace, Scoping.Dummy) {
        void pre(ThreadContext context, IRubyObject self, RubyModule implementer, String name, Block block, StaticScope scope) {
            context.preMethodBacktraceDummyScope(name, scope);
        }
        void post(ThreadContext context) {
            context.postMethodBacktraceDummyScope();
        }
    },
    FrameBacktraceScopeNone (Framing.Backtrace, Scoping.None) {
        void pre(ThreadContext context, IRubyObject self, RubyModule implementer, String name, Block block, StaticScope scope) {
            context.preMethodBacktraceOnly(name);
        }
        void post(ThreadContext context) {
            context.postMethodBacktraceOnly();
        }
    },
    FrameNoneScopeFull(Framing.None, Scoping.Full) {
        void pre(ThreadContext context, IRubyObject self, RubyModule implementer, String name, Block block, StaticScope scope) {
            context.preMethodScopeOnly(scope);
        }
        void post(ThreadContext context) {
            context.postMethodScopeOnly();
        }
    },
    FrameNoneScopeDummy(Framing.None, Scoping.Dummy) {
        void pre(ThreadContext context, IRubyObject self, RubyModule implementer, String name, Block block, StaticScope scope) {
            context.preMethodNoFrameAndDummyScope(scope);
        }
        void post(ThreadContext context) {
            context.postMethodScopeOnly();
        }
    },
    FrameNoneScopeNone(Framing.None, Scoping.None) {
        void pre(ThreadContext context, IRubyObject self, RubyModule implementer, String name, Block block, StaticScope scope) {
        }
        void post(ThreadContext context) {
        }
    };

    @Deprecated
    public static final CallConfiguration FRAME_AND_SCOPE = FrameFullScopeFull;
    @Deprecated
    public static final CallConfiguration FRAME_AND_DUMMY_SCOPE = FrameFullScopeDummy;
    @Deprecated
    public static final CallConfiguration FRAME_ONLY = FrameFullScopeNone;
    @Deprecated
    public static final CallConfiguration BACKTRACE_AND_SCOPE = FrameBacktraceScopeFull;
    @Deprecated
    public static final CallConfiguration BACKTRACE_DUMMY_SCOPE = FrameBacktraceScopeNone;
    @Deprecated
    public static final CallConfiguration BACKTRACE_ONLY = FrameBacktraceScopeNone;
    @Deprecated
    public static final CallConfiguration SCOPE_ONLY = FrameNoneScopeFull;
    @Deprecated
    public static final CallConfiguration NO_FRAME_DUMMY_SCOPE = FrameNoneScopeDummy;
    @Deprecated
    public static final CallConfiguration NO_FRAME_NO_SCOPE = FrameNoneScopeNone;

    /**
     * Produce a CallConfiguration that represents what *caller* methods must prepare for
     * the method with this annotation.
     *
     * @see org.jruby.anno.AnnotationHelper#getCallerCallConfigNameByAnno(org.jruby.anno.JRubyMethod)
     */
    public static CallConfiguration getCallerCallConfigByAnno(JRubyMethod jrubyMethod) {
        boolean frame = false;
        boolean scope = false;

        for (FrameField field : jrubyMethod.reads()) {
            frame |= field.needsFrame();
            scope |= field.needsScope();
        }
        for (FrameField field : jrubyMethod.writes()) {
            frame |= field.needsFrame();
            scope |= field.needsScope();
        }
        return getCallConfig(frame, scope);
    }

    /**
     * Produce a CallConfiguration name that represents what must be prepared around calls to
     * the method with this annotation.
     */
    public static CallConfiguration getCallConfigByAnno(JRubyMethod anno) {
        return getCallConfig(anno.frame());
    }

    /**
     * Given a frame and scope requirement, return the name of the appropriate CallConfiguration.
     *
     * @see org.jruby.anno.AnnotationHelper#getCallConfigName(boolean, boolean)
     */
    public static CallConfiguration getCallConfig(boolean frame) {
        if (frame) {
            return FrameFullScopeNone;
        } else {
            return FrameNoneScopeNone;
        }
    }

    private final Framing framing;
    private final Scoping scoping;

    CallConfiguration(Framing framing, Scoping scoping) {
        this.framing = framing;
        this.scoping = scoping;
    }

    public final Framing framing() {return framing;}
    public final Scoping scoping() {return scoping;}
    
    abstract void pre(ThreadContext context, IRubyObject self, RubyModule implementer, String name, Block block, StaticScope scope);
    abstract void post(ThreadContext context);
    boolean isNoop() { return framing == Framing.None && scoping == Scoping.None; }

    @Deprecated
    public static CallConfiguration getCallConfig(boolean frame, boolean scope, boolean backtrace) {
        return getCallConfig(frame, scope);
    }

    /**
     * Given a frame and scope requirement, return the name of the appropriate CallConfiguration.
     *
     * @see org.jruby.anno.AnnotationHelper#getCallConfigName(boolean, boolean)
     */
    @Deprecated
    public static CallConfiguration getCallConfig(boolean frame, boolean scope) {
        if (frame) {
            if (scope) {
                return FrameFullScopeFull;
            } else {
                return FrameFullScopeNone;
            }
        } else if (scope) {
            return FrameNoneScopeFull;
        } else {
            return FrameNoneScopeNone;
        }
    }
}
