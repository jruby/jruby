package org.jruby.test;

import junit.framework.TestCase;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.api.Define;
import org.jruby.ext.AllMethodForms;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.callsite.MonomorphicCallSite;

public class TestMethodBinding extends TestCase {
    private Ruby runtime;
    private RubyClass AllMethodForms;

    protected void setUp() throws Exception {
        runtime = Ruby.newInstance();
        AllMethodForms = org.jruby.ext.AllMethodForms.define(runtime);
    }

    public void testBindings() throws Exception {
        CallSite cs;

        ThreadContext context = ThreadContext.newContext(runtime);
        AllMethodForms obj = new AllMethodForms(runtime, AllMethodForms);

        // static calls
        cs = new MonomorphicCallSite("s");
        assertEquals("static_zero", cs.call(context, AllMethodForms, AllMethodForms).toString());
        assertEquals("static_one", cs.call(context, AllMethodForms, AllMethodForms, obj).toString());
        assertEquals("static_two", cs.call(context, AllMethodForms, AllMethodForms, obj, obj).toString());
        assertEquals("static_three", cs.call(context, AllMethodForms, AllMethodForms, obj, obj, obj).toString());
        assertEquals("static_varargs", cs.call(context, AllMethodForms, AllMethodForms, obj, obj, obj, obj).toString());

        cs = new MonomorphicCallSite("sc");
        assertEquals("static_context_zero", cs.call(context, AllMethodForms, AllMethodForms).toString());
        assertEquals("static_context_one", cs.call(context, AllMethodForms, AllMethodForms, obj).toString());
        assertEquals("static_context_two", cs.call(context, AllMethodForms, AllMethodForms, obj, obj).toString());
        assertEquals("static_context_three", cs.call(context, AllMethodForms, AllMethodForms, obj, obj, obj).toString());
        assertEquals("static_context_varargs", cs.call(context, AllMethodForms, AllMethodForms, obj, obj, obj, obj).toString());

        cs = new MonomorphicCallSite("sb");
        assertEquals("static_block_zero", cs.call(context, AllMethodForms, AllMethodForms).toString());
        assertEquals("static_block_one", cs.call(context, AllMethodForms, AllMethodForms, obj).toString());
        assertEquals("static_block_two", cs.call(context, AllMethodForms, AllMethodForms, obj, obj).toString());
        assertEquals("static_block_three", cs.call(context, AllMethodForms, AllMethodForms, obj, obj, obj).toString());
        assertEquals("static_block_varargs", cs.call(context, AllMethodForms, AllMethodForms, obj, obj, obj, obj).toString());

        cs = new MonomorphicCallSite("scb");
        assertEquals("static_context_block_zero", cs.call(context, AllMethodForms, AllMethodForms).toString());
        assertEquals("static_context_block_one", cs.call(context, AllMethodForms, AllMethodForms, obj).toString());
        assertEquals("static_context_block_two", cs.call(context, AllMethodForms, AllMethodForms, obj, obj).toString());
        assertEquals("static_context_block_three", cs.call(context, AllMethodForms, AllMethodForms, obj, obj, obj).toString());
        assertEquals("static_context_block_varargs", cs.call(context, AllMethodForms, AllMethodForms, obj, obj, obj, obj).toString());

        // instance calls
        cs = new MonomorphicCallSite("i");
        assertEquals("instance_zero", cs.call(context, obj, obj).toString());
        assertEquals("instance_one", cs.call(context, obj, obj, obj).toString());
        assertEquals("instance_two", cs.call(context, obj, obj, obj, obj).toString());
        assertEquals("instance_three", cs.call(context, obj, obj, obj, obj, obj).toString());
        assertEquals("instance_varargs", cs.call(context, obj, obj, obj, obj, obj, obj).toString());

        cs = new MonomorphicCallSite("ic");
        assertEquals("instance_context_zero", cs.call(context, obj, obj).toString());
        assertEquals("instance_context_one", cs.call(context, obj, obj, obj).toString());
        assertEquals("instance_context_two", cs.call(context, obj, obj, obj, obj).toString());
        assertEquals("instance_context_three", cs.call(context, obj, obj, obj, obj, obj).toString());
        assertEquals("instance_context_varargs", cs.call(context, obj, obj, obj, obj, obj, obj).toString());

        cs = new MonomorphicCallSite("ib");
        assertEquals("instance_block_zero", cs.call(context, obj, obj).toString());
        assertEquals("instance_block_one", cs.call(context, obj, obj, obj).toString());
        assertEquals("instance_block_two", cs.call(context, obj, obj, obj, obj).toString());
        assertEquals("instance_block_three", cs.call(context, obj, obj, obj, obj, obj).toString());
        assertEquals("instance_block_varargs", cs.call(context, obj, obj, obj, obj, obj, obj).toString());

        cs = new MonomorphicCallSite("icb");
        assertEquals("instance_context_block_zero", cs.call(context, obj, obj).toString());
        assertEquals("instance_context_block_one", cs.call(context, obj, obj, obj).toString());
        assertEquals("instance_context_block_two", cs.call(context, obj, obj, obj, obj).toString());
        assertEquals("instance_context_block_three", cs.call(context, obj, obj, obj, obj, obj).toString());
        assertEquals("instance_context_block_varargs", cs.call(context, obj, obj, obj, obj, obj, obj).toString());
    }
}
