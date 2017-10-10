/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2007 Nick Sieger
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.jruby.RubyArray;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.JavaEmbedUtils;

public class TestRaiseException extends TestRubyBase {

    public void testJavaExceptionTraceIncludesRubyTrace() throws Exception {
        String script = 
        "def one\n" +
        "  two\n" + 
        "end\n" +
        "def two\n" +
        "  raise 'here'\n" +
        "end\n" +
        "one\n";
        try {
            eval(script, "test_raise_exception1");
            fail("should have raised an exception");
        }
        catch (RaiseException ex) {
            String trace = printStackTrace(ex);
            // System.out.println(trace);
            assertTrue(trace.indexOf("here") >= 0);
            assertTrue(trace.indexOf("one") >= 0);
            assertTrue(trace.indexOf("two") >= 0);
            assertTrue(trace.indexOf("test_raise_exception1.one(test_raise_exception1:2)") >= 0);
        }
    }

    public static class ThrowFromJava {
        public void throwIt() { throw new RuntimeException("here"); }
    }

    public void testRubyExceptionTraceIncludesJavaPart() throws Exception {
        String script =
        "require 'java'\n" +
        "java_import('org.jruby.test.TestRaiseException$ThrowFromJava') { |p,c| 'ThrowFromJava' }\n" +
        "def throw_it; [ ThrowFromJava.new ].each { |tfj| tfj.throwIt } end\n" +
        "throw_it()\n";
        try {
            eval(script, "test_raise_exception2");
            fail("should have raised an exception");
        }
        catch (RuntimeException ex) {
            assertEquals(RuntimeException.class, ex.getClass());

            final StackTraceElement[] trace = ex.getStackTrace();
            final String fullTrace = printStackTrace(ex); // System.out.println( fullTrace );
            // NOTE: 'unknown' JRuby packages e.g. "org.jruby.test" should not be filtered
            //  ... if they are that hurts stack-traces from extensions such as jruby-rack and jruby-openssl
            assertTrue(trace[0].toString(), trace[0].toString().startsWith("org.jruby.test.TestRaiseException$ThrowFromJava.throwIt"));

            boolean found_each = false;
            for ( StackTraceElement element : trace ) {
                if ( "each".equals( element.getMethodName() ) && element.getClassName().contains("RubyArray") ) {
                    if ( found_each ) {
                        fail("duplicate " + element + " in : \n" + fullTrace);
                    }
                    found_each = true;
                }
            }
            assertTrue("missing org.jruby.RubyArray.each ... in : \n" + fullTrace, found_each);
        }
        // ~ FULL TRACE (no filtering) :
        //    java.lang.RuntimeException: here
        //    at org.jruby.test.TestRaiseException$ThrowFromJava.throwIt(org/jruby/test/TestRaiseException.java:63)
        //    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        //    at sun.reflect.NativeMethodAccessorImpl.invoke(sun/reflect/NativeMethodAccessorImpl.java:62)
        //    at sun.reflect.DelegatingMethodAccessorImpl.invoke(sun/reflect/DelegatingMethodAccessorImpl.java:43)
        //    at java.lang.reflect.Method.invoke(java/lang/reflect/Method.java:498)
        //    at org.jruby.javasupport.JavaMethod.invokeDirectWithExceptionHandling(org/jruby/javasupport/JavaMethod.java:438)
        //    at org.jruby.javasupport.JavaMethod.invokeDirect(org/jruby/javasupport/JavaMethod.java:302)
        //    at org.jruby.java.invokers.InstanceMethodInvoker.call(org/jruby/java/invokers/InstanceMethodInvoker.java:35)
        //    at org.jruby.runtime.callsite.CachingCallSite.cacheAndCall(org/jruby/runtime/callsite/CachingCallSite.java:293)
        //    at org.jruby.runtime.callsite.CachingCallSite.call(org/jruby/runtime/callsite/CachingCallSite.java:131)
        //    at test_raise_exception2.block in throw_it(test_raise_exception2:3)
        //    at org.jruby.runtime.CompiledIRBlockBody.yieldDirect(org/jruby/runtime/CompiledIRBlockBody.java:156)
        //    at org.jruby.runtime.BlockBody.yield(org/jruby/runtime/BlockBody.java:110)
        //    at org.jruby.runtime.Block.yield(org/jruby/runtime/Block.java:167)
        //    at org.jruby.RubyArray.each(org/jruby/RubyArray.java:1567)
        //    at org.jruby.runtime.callsite.CachingCallSite.cacheAndCall(org/jruby/runtime/callsite/CachingCallSite.java:303)
        //    at org.jruby.runtime.callsite.CachingCallSite.callBlock(org/jruby/runtime/callsite/CachingCallSite.java:141)
        //    at org.jruby.runtime.callsite.CachingCallSite.call(org/jruby/runtime/callsite/CachingCallSite.java:145)
        //    at test_raise_exception2.throw_it(test_raise_exception2:3)
        //    at org.jruby.internal.runtime.methods.CompiledIRMethod.invokeExact(org/jruby/internal/runtime/methods/CompiledIRMethod.java:232)
        //    at org.jruby.internal.runtime.methods.CompiledIRMethod.call(org/jruby/internal/runtime/methods/CompiledIRMethod.java:101)
        //    at org.jruby.internal.runtime.methods.DynamicMethod.call(org/jruby/internal/runtime/methods/DynamicMethod.java:189)
        //    at org.jruby.runtime.callsite.CachingCallSite.cacheAndCall(org/jruby/runtime/callsite/CachingCallSite.java:293)
        //    at org.jruby.runtime.callsite.CachingCallSite.call(org/jruby/runtime/callsite/CachingCallSite.java:131)
        //    at test_raise_exception2.<top>(test_raise_exception2:4)
        //    at java.lang.invoke.MethodHandle.invokeWithArguments(java/lang/invoke/MethodHandle.java:627)
        //    at org.jruby.ir.Compiler$1.load(org/jruby/ir/Compiler.java:111)
        //    at org.jruby.Ruby.runScript(org/jruby/Ruby.java:825)
        //    at org.jruby.Ruby.runScript(org/jruby/Ruby.java:817)
        //    at org.jruby.Ruby.runNormally(org/jruby/Ruby.java:755)
        //    at org.jruby.test.TestRubyBase.eval(org/jruby/test/TestRubyBase.java:84)
        //    at org.jruby.test.TestRaiseException.testRubyExceptionTraceIncludesJavaPart(org/jruby/test/TestRaiseException.java:73)
        //    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        //    at sun.reflect.NativeMethodAccessorImpl.invoke(sun/reflect/NativeMethodAccessorImpl.java:62)
        //    at sun.reflect.DelegatingMethodAccessorImpl.invoke(sun/reflect/DelegatingMethodAccessorImpl.java:43)
        //    at java.lang.reflect.Method.invoke(java/lang/reflect/Method.java:498)
        //    at junit.framework.TestCase.runTest(junit/framework/TestCase.java:176)

        // ~ FILTERING (all org.jruby) ~ 9.0.5.0 :
        //    java.lang.RuntimeException: here
        //    at java.lang.reflect.Method.invoke(java/lang/reflect/Method.java:498)
        //    at test_raise_exception2.block in throw_it(test_raise_exception2:3)
        //    at org.jruby.RubyArray.each(org/jruby/RubyArray.java:1567)
        //    at test_raise_exception2.throw_it(test_raise_exception2:3)
        //    at test_raise_exception2.<top>(test_raise_exception2:4)
        //    at java.lang.invoke.MethodHandle.invokeWithArguments(java/lang/invoke/MethodHandle.java:627)
        //    at java.lang.reflect.Method.invoke(java/lang/reflect/Method.java:498)
        //    at junit.framework.TestCase.runTest(junit/framework/TestCase.java:176)

        // ~ LESS FILTERING in 9.1 :
        //    java.lang.RuntimeException: here
        //    at org.jruby.test.TestRaiseException$ThrowFromJava.throwIt(org/jruby/test/TestRaiseException.java:63)
        //    at java.lang.reflect.Method.invoke(java/lang/reflect/Method.java:498)
        //    at org.jruby.java.invokers.InstanceMethodInvoker.call(org/jruby/java/invokers/InstanceMethodInvoker.java:35)
        //    at test_raise_exception2.block in throw_it(test_raise_exception2:3)
        //    at org.jruby.RubyArray.each(org/jruby/RubyArray.java:1567)
        //    at test_raise_exception2.throw_it(test_raise_exception2:3)
        //    at test_raise_exception2.<top>(test_raise_exception2:4)
        //    at java.lang.invoke.MethodHandle.invokeWithArguments(java/lang/invoke/MethodHandle.java:627)
        //    at org.jruby.Ruby.runScript(org/jruby/Ruby.java:825)
        //    at org.jruby.Ruby.runScript(org/jruby/Ruby.java:817)
        //    at org.jruby.Ruby.runNormally(org/jruby/Ruby.java:755)
        //    at org.jruby.test.TestRubyBase.eval(org/jruby/test/TestRubyBase.java:84)
        //    at org.jruby.test.TestRaiseException.testRubyExceptionTraceIncludesJavaPart(org/jruby/test/TestRaiseException.java:73)
        //    at java.lang.reflect.Method.invoke(java/lang/reflect/Method.java:498)
        //    at junit.framework.TestCase.runTest(junit/framework/TestCase.java:176)
    }

    public void testRubyExceptionBacktraceIncludesJavaOrigin() throws Exception {
        String script =
        "require 'java'\n" +
        "hash = Hash.new { org.jruby.test.TestRaiseException::ThrowFromJava.new.throwIt }\n" +
        "begin; hash['missing']; rescue java.lang.Exception => e; $ex_trace = e.backtrace end \n" +
        "$ex_trace" ;

        RubyArray trace = (RubyArray) runtime.evalScriptlet(script);

        String fullTrace = trace.join(runtime.getCurrentContext(), runtime.newString("\n")).toString();
        // System.out.println(fullTrace);

        // NOTE: 'unknown' JRuby packages e.g. "org.jruby.test" should not be filtered
        //  ... if they are that hurts stack-traces from extensions such as jruby-rack and jruby-openssl
        assertTrue(trace.get(0).toString(), trace.get(0).toString().startsWith("org.jruby.test.TestRaiseException$ThrowFromJava.throwIt"));

        boolean hash_default = false;
        for ( Object element : trace ) {
            if ( element.toString().contains("org.jruby.RubyHash.default")) {
                if ( hash_default ) fail("duplicate " + element + " in : \n" + fullTrace);
                hash_default = true;
            }
        }
        assertTrue("missing org.jruby.RubyHash.default ... in : \n" + fullTrace, hash_default);
    }

    public void testRubyExceptionWithoutCause() throws Exception {
        try {
            RubyRuntimeAdapter evaler = JavaEmbedUtils.newRuntimeAdapter();

            evaler.eval(runtime, "no_method_with_this_name");
            fail("Expected ScriptException");
        } catch (RaiseException re) {
            assertEquals("(NameError) undefined local variable or method `no_method_with_this_name' for main:Object", re.getMessage());
        }
    }

    private static String printStackTrace(final Throwable ex) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ex.printStackTrace(new PrintStream(baos));
        return baos.toString();
    }

}
