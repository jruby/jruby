package org.jruby.ext;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * This demonstrates an extension with all forms of method binding, to be used for testing dispatch to those forms.
 */
public class AllMethodForms extends RubyObject {
    public AllMethodForms(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    public static RubyClass define(Ruby runtime){
        RubyClass amf = runtime.defineClass("AllMethodForms", runtime.getObject(), AllMethodForms::new);
        amf.defineAnnotatedMethods(AllMethodForms.class);
        return amf;
    }

    @JRubyMethod(rest = true, meta = true)
    public static IRubyObject s(IRubyObject self, IRubyObject[] args) {
        return self.getRuntime().newString("static_varargs");
    }

    @JRubyMethod(meta = true)
    public static IRubyObject s(IRubyObject self) {
        return self.getRuntime().newString("static_zero");
    }

    @JRubyMethod(meta = true)
    public static IRubyObject s(IRubyObject self, IRubyObject arg0) {
        return self.getRuntime().newString("static_one");
    }

    @JRubyMethod(meta = true)
    public static IRubyObject s(IRubyObject self, IRubyObject arg0, IRubyObject arg1) {
        return self.getRuntime().newString("static_two");
    }

    @JRubyMethod(meta = true)
    public static IRubyObject s(IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return self.getRuntime().newString("static_three");
    }

    public static IRubyObject s(IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return self.getRuntime().newString("static_four");
    }

    @JRubyMethod(rest = true, meta = true)
    public static IRubyObject sc(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        return self.getRuntime().newString("static_context_varargs");
    }

    @JRubyMethod(meta = true)
    public static IRubyObject sc(ThreadContext context, IRubyObject self) {
        return self.getRuntime().newString("static_context_zero");
    }

    @JRubyMethod(meta = true)
    public static IRubyObject sc(ThreadContext context, IRubyObject self, IRubyObject arg0) {
        return self.getRuntime().newString("static_context_one");
    }

    @JRubyMethod(meta = true)
    public static IRubyObject sc(ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1) {
        return self.getRuntime().newString("static_context_two");
    }

    @JRubyMethod(meta = true)
    public static IRubyObject sc(ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return self.getRuntime().newString("static_context_three");
    }

    public static IRubyObject sc(ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return self.getRuntime().newString("static_context_four");
    }

    @JRubyMethod(rest = true, meta = true)
    public static IRubyObject sb(IRubyObject self, IRubyObject[] args, Block block) {
        return self.getRuntime().newString("static_block_varargs");
    }

    @JRubyMethod(meta = true)
    public static IRubyObject sb(IRubyObject self, Block block) {
        return self.getRuntime().newString("static_block_zero");
    }

    @JRubyMethod(meta = true)
    public static IRubyObject sb(IRubyObject self, IRubyObject arg0, Block block) {
        return self.getRuntime().newString("static_block_one");
    }

    @JRubyMethod(meta = true)
    public static IRubyObject sb(IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block) {
        return self.getRuntime().newString("static_block_two");
    }

    @JRubyMethod(meta = true)
    public static IRubyObject sb(IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return self.getRuntime().newString("static_block_three");
    }

    public static IRubyObject sb(IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        return self.getRuntime().newString("static_block_four");
    }

    @JRubyMethod(rest = true, meta = true)
    public static IRubyObject scb(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        return self.getRuntime().newString("static_context_block_varargs");
    }

    @JRubyMethod(meta = true)
    public static IRubyObject scb(ThreadContext context, IRubyObject self, Block block) {
        return self.getRuntime().newString("static_context_block_zero");
    }

    @JRubyMethod(meta = true)
    public static IRubyObject scb(ThreadContext context, IRubyObject self, IRubyObject arg0, Block block) {
        return self.getRuntime().newString("static_context_block_one");
    }

    @JRubyMethod(meta = true)
    public static IRubyObject scb(ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block) {
        return self.getRuntime().newString("static_context_block_two");
    }

    @JRubyMethod(meta = true)
    public static IRubyObject scb(ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return self.getRuntime().newString("static_context_block_three");
    }

    public static IRubyObject scb(ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        return self.getRuntime().newString("static_context_block_four");
    }

    @JRubyMethod(rest = true)
    public IRubyObject i(IRubyObject[] args) {
        return getRuntime().newString("instance_varargs");
    }

    @JRubyMethod
    public IRubyObject i() {
        return getRuntime().newString("instance_zero");
    }

    @JRubyMethod
    public IRubyObject i(IRubyObject arg0) {
        return getRuntime().newString("instance_one");
    }

    @JRubyMethod
    public IRubyObject i(IRubyObject arg0, IRubyObject arg1) {
        return getRuntime().newString("instance_two");
    }

    @JRubyMethod
    public IRubyObject i(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return getRuntime().newString("instance_three");
    }

    public IRubyObject i(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return getRuntime().newString("instance_four");
    }

    @JRubyMethod(rest = true)
    public IRubyObject ic(ThreadContext context, IRubyObject[] args) {
        return getRuntime().newString("instance_context_varargs");
    }

    @JRubyMethod
    public IRubyObject ic(ThreadContext context) {
        return getRuntime().newString("instance_context_zero");
    }

    @JRubyMethod
    public IRubyObject ic(ThreadContext context, IRubyObject arg0) {
        return getRuntime().newString("instance_context_one");
    }

    @JRubyMethod
    public IRubyObject ic(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return getRuntime().newString("instance_context_two");
    }

    @JRubyMethod
    public IRubyObject ic(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return getRuntime().newString("instance_context_three");
    }

    public IRubyObject ic(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return getRuntime().newString("instance_context_four");
    }

    @JRubyMethod(rest = true)
    public IRubyObject ib(IRubyObject[] args, Block block) {
        return getRuntime().newString("instance_block_varargs");
    }

    @JRubyMethod
    public IRubyObject ib(Block block) {
        return getRuntime().newString("instance_block_zero");
    }

    @JRubyMethod
    public IRubyObject ib(IRubyObject arg0, Block block) {
        return getRuntime().newString("instance_block_one");
    }

    @JRubyMethod
    public IRubyObject ib(IRubyObject arg0, IRubyObject arg1, Block block) {
        return getRuntime().newString("instance_block_two");
    }

    @JRubyMethod
    public IRubyObject ib(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return getRuntime().newString("instance_block_three");
    }

    public IRubyObject ib(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        return getRuntime().newString("instance_block_four");
    }

    @JRubyMethod(rest = true)
    public IRubyObject icb(ThreadContext context, IRubyObject[] args, Block block) {
        return getRuntime().newString("instance_context_block_varargs");
    }

    @JRubyMethod
    public IRubyObject icb(ThreadContext context, Block block) {
        return getRuntime().newString("instance_context_block_zero");
    }

    @JRubyMethod
    public IRubyObject icb(ThreadContext context, IRubyObject arg0, Block block) {
        return getRuntime().newString("instance_context_block_one");
    }

    @JRubyMethod
    public IRubyObject icb(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return getRuntime().newString("instance_context_block_two");
    }

    @JRubyMethod
    public IRubyObject icb(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return getRuntime().newString("instance_context_block_three");
    }

    public IRubyObject icb(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        return getRuntime().newString("instance_context_block_four");
    }
}
