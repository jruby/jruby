package org.jruby.demo.ext;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * This is a demonstration of how to bind method_missing in a JRuby extension
 */
public class MethodMissing extends RubyObject {
    public static void init(Ruby ruby) {
        RubyClass mm = ruby.defineClass("MethodMissing", ruby.getObject(), new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new MethodMissing(runtime, klazz);
            }
        });
        mm.defineAnnotatedMethods(MethodMissing.class);
    }

    public MethodMissing(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }

    @JRubyMethod(rest = true)
    public IRubyObject method_missing(IRubyObject[] args) {
        for (IRubyObject arg : args) {
            System.out.println(arg);
        }
        return getRuntime().getNil();
    }
}
