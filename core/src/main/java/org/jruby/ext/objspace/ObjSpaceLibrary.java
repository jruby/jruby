package org.jruby.ext.objspace;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.anno.CRuby;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Create.newString;
import static org.jruby.runtime.Visibility.PRIVATE;

/**
 * The allocation tracing part of the objspace library.
 */
public class ObjSpaceLibrary implements Library {
    public void load(Ruby runtime, boolean wrap) {
        RubyModule objectSpace = runtime.getObjectSpaceModule();
        // a profile without ObjectSpace leaves objspace.rb to define the module and its other methods
        if (objectSpace == null) return;

        objectSpace.defineMethods(runtime.getCurrentContext(), ObjSpaceLibrary.class);
    }

    private static AllocationTracer tracer(ThreadContext context) {
        return context.runtime.getAllocationTracer();
    }

    @CRuby(value = "trace_object_allocations", file = "ext/objspace/object_tracing.c")
    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject trace_object_allocations(ThreadContext context, IRubyObject self, Block block) {
        trace_object_allocations_start(context, self);
        try {
            return block.yield(context, context.nil);
        } finally {
            trace_object_allocations_stop(context, self);
        }
    }

    @CRuby(value = "trace_object_allocations_start", file = "ext/objspace/object_tracing.c")
    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject trace_object_allocations_start(ThreadContext context, IRubyObject self) {
        context.runtime.startAllocationTracing();
        return context.nil;
    }

    @CRuby(value = "trace_object_allocations_stop", file = "ext/objspace/object_tracing.c")
    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject trace_object_allocations_stop(ThreadContext context, IRubyObject self) {
        context.runtime.stopAllocationTracing();
        return context.nil;
    }

    @CRuby(value = "trace_object_allocations_clear", file = "ext/objspace/object_tracing.c")
    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject trace_object_allocations_clear(ThreadContext context, IRubyObject self) {
        tracer(context).clear();
        return context.nil;
    }

    @CRuby(value = "trace_object_allocations_debug_start", file = "ext/objspace/object_tracing.c",
            note = "does not keep entries for collected objects or add them to [BUG] reports")
    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject trace_object_allocations_debug_start(ThreadContext context, IRubyObject self) {
        return trace_object_allocations_start(context, self);
    }

    @CRuby(value = "allocation_sourcefile", file = "ext/objspace/object_tracing.c")
    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject allocation_sourcefile(ThreadContext context, IRubyObject self, IRubyObject object) {
        AllocationTracer.Info info = tracer(context).get(object);
        return info == null || info.path() == null ? context.nil : newString(context, info.path());
    }

    @CRuby(value = "allocation_sourceline", file = "ext/objspace/object_tracing.c")
    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject allocation_sourceline(ThreadContext context, IRubyObject self, IRubyObject object) {
        AllocationTracer.Info info = tracer(context).get(object);
        return info == null ? context.nil : asFixnum(context, info.line());
    }

    @CRuby(value = "allocation_class_path", file = "ext/objspace/object_tracing.c",
            note = "always nil: JRuby does not record the class of the method an object is allocated in")
    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject allocation_class_path(ThreadContext context, IRubyObject self, IRubyObject object) {
        return context.nil;
    }

    @CRuby(value = "allocation_method_id", file = "ext/objspace/object_tracing.c",
            note = "always nil: JRuby does not record the method an object is allocated in")
    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject allocation_method_id(ThreadContext context, IRubyObject self, IRubyObject object) {
        return context.nil;
    }

    @CRuby(value = "allocation_generation", file = "ext/objspace/object_tracing.c")
    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject allocation_generation(ThreadContext context, IRubyObject self, IRubyObject object) {
        AllocationTracer.Info info = tracer(context).get(object);
        return info == null ? context.nil : asFixnum(context, info.generation());
    }
}
