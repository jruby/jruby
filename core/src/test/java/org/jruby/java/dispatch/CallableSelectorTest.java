/*
 * Copyright (c) 2015 JRuby.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 */
package org.jruby.java.dispatch;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.jruby.Ruby;
import org.jruby.RubyProc;
import org.jruby.javasupport.JavaMethod;
import org.jruby.javasupport.ParameterTypes;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.JavaInternalBlockBody;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author kares
 */
public class CallableSelectorTest extends junit.framework.TestCase {

    private static final CallableSelector.CallableCache DUMMY = new CallableSelector.CallableCache() {
        @Override
        public ParameterTypes getSignature(int signatureCode) {
            return null;
        }

        @Override
        public void putSignature(int signatureCode, ParameterTypes callable) {

        }
    };

    private static class DummyBlockBody extends JavaInternalBlockBody {
        DummyBlockBody(Ruby runtime, Signature signature) {
            super(runtime, signature);
        }

        @Override
        public IRubyObject yield(ThreadContext context, IRubyObject[] args) {
            throw new AssertionError("unexpected block body call: " + Arrays.toString(args));
        }
    }

    @Test
    public void testCallableProcToIfaceMatchIsNotOrderSensitive() throws Exception {
        final Ruby runtime = Ruby.newInstance();

        final Method list1 = java.io.File.class.getMethod("listFiles", java.io.FileFilter.class);
        final Method list2 = java.io.File.class.getMethod("listFiles", java.io.FilenameFilter.class);

        JavaMethod[] methods;
        Binding binding = Block.NULL_BLOCK.getBinding();
        JavaMethod result; IRubyObject[] args;

        // arity 1 :

        BlockBody body1 = new DummyBlockBody(runtime, Signature.ONE_ARGUMENT);
        RubyProc dummyProc = RubyProc.newProc(runtime, new Block(body1, binding), Block.Type.PROC);

        methods = new JavaMethod[] {
            new JavaMethod(runtime, list1), new JavaMethod(runtime, list2)
        };
        result = (JavaMethod)CallableSelector.matchingCallableArityOne(runtime, DUMMY, methods, dummyProc);
        assertEquals(new JavaMethod(runtime, list1), result);

        args = new IRubyObject[] { dummyProc };
        result = (JavaMethod)CallableSelector.matchingCallableArityN(runtime, DUMMY, methods, args);
        assertEquals(new JavaMethod(runtime, list1), result);

        methods = new JavaMethod[] { // "reverse" method order
            new JavaMethod(runtime, list2), new JavaMethod(runtime, list1)
        };
        result = (JavaMethod)CallableSelector.matchingCallableArityOne(runtime, DUMMY, methods, dummyProc);
        assertEquals(new JavaMethod(runtime, list1), result);

        args = new IRubyObject[] { dummyProc };
        result = (JavaMethod)CallableSelector.matchingCallableArityN(runtime, DUMMY, methods, args);
        assertEquals(new JavaMethod(runtime, list1), result);

        // arity 2 :

        BlockBody body2 = new DummyBlockBody(runtime, Signature.TWO_ARGUMENTS);
        dummyProc = RubyProc.newProc(runtime, new Block(body2, binding), Block.Type.PROC);

        methods = new JavaMethod[] {
            new JavaMethod(runtime, list1), new JavaMethod(runtime, list2)
        };
        result = (JavaMethod)CallableSelector.matchingCallableArityOne(runtime, DUMMY, methods, dummyProc);
        assertEquals(new JavaMethod(runtime, list2), result);

        args = new IRubyObject[] { dummyProc };
        result = (JavaMethod)CallableSelector.matchingCallableArityN(runtime, DUMMY, methods, args);
        assertEquals(new JavaMethod(runtime, list2), result);

        methods = new JavaMethod[] { // "reverse" method order
            new JavaMethod(runtime, list2), new JavaMethod(runtime, list1)
        };
        result = (JavaMethod)CallableSelector.matchingCallableArityOne(runtime, DUMMY, methods, dummyProc);
        assertEquals(new JavaMethod(runtime, list2), result);

        args = new IRubyObject[] { dummyProc };
        result = (JavaMethod)CallableSelector.matchingCallableArityN(runtime, DUMMY, methods, args);
        assertEquals(new JavaMethod(runtime, list2), result);

        // arity -1 :

        BlockBody body_1 = new DummyBlockBody(runtime, Signature.OPTIONAL);
        dummyProc = RubyProc.newProc(runtime, new Block(body_1, binding), Block.Type.PROC);

        methods = new JavaMethod[] {
            new JavaMethod(runtime, list1), new JavaMethod(runtime, list2)
        };
        result = (JavaMethod)CallableSelector.matchingCallableArityOne(runtime, DUMMY, methods, dummyProc);
        assertEquals(new JavaMethod(runtime, list1), result);

        methods = new JavaMethod[] {
            new JavaMethod(runtime, list2), new JavaMethod(runtime, list1)
        };
        result = (JavaMethod)CallableSelector.matchingCallableArityOne(runtime, DUMMY, methods, dummyProc);
        assertEquals(new JavaMethod(runtime, list1), result);

        // arity -3 :

        BlockBody body_3 = new DummyBlockBody(runtime, Signature.TWO_REQUIRED);
        dummyProc = RubyProc.newProc(runtime, new Block(body_3, binding), Block.Type.PROC);

        methods = new JavaMethod[] {
            new JavaMethod(runtime, list1), new JavaMethod(runtime, list2)
        };
        result = (JavaMethod)CallableSelector.matchingCallableArityOne(runtime, DUMMY, methods, dummyProc);
        assertEquals(new JavaMethod(runtime, list2), result);

        methods = new JavaMethod[] {
            new JavaMethod(runtime, list2), new JavaMethod(runtime, list1)
        };
        result = (JavaMethod)CallableSelector.matchingCallableArityOne(runtime, DUMMY, methods, dummyProc);
        assertEquals(new JavaMethod(runtime, list2), result);

        // arity -2 :

        BlockBody body_2 = new DummyBlockBody(runtime, Signature.ONE_REQUIRED);;
        dummyProc = RubyProc.newProc(runtime, new Block(body_2, binding), Block.Type.PROC);

        methods = new JavaMethod[] {
            new JavaMethod(runtime, list1), new JavaMethod(runtime, list2)
        };
        result = (JavaMethod)CallableSelector.matchingCallableArityOne(runtime, DUMMY, methods, dummyProc);
        assertEquals(new JavaMethod(runtime, list1), result);

        methods = new JavaMethod[] {
            new JavaMethod(runtime, list2), new JavaMethod(runtime, list1)
        };
        result = (JavaMethod)CallableSelector.matchingCallableArityOne(runtime, DUMMY, methods, dummyProc);
        assertEquals(new JavaMethod(runtime, list1), result);

    }

}
