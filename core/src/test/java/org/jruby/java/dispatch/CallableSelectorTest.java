/*
 * Copyright (c) 2015 JRuby.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jruby.java.dispatch;

import java.lang.reflect.Method;
import org.jruby.Ruby;
import org.jruby.RubyProc;
import org.jruby.javasupport.JavaMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.Frame;
import org.jruby.runtime.NullBlockBody;
import org.jruby.runtime.backtrace.BacktraceElement;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.IntHashMap;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author kares
 */
public class CallableSelectorTest {

    @Test
    public void testCallableProcToIfaceMatchIsNotOrderSensitive() throws Exception {
        final Ruby runtime = Ruby.newInstance();

        final Method list1 = java.io.File.class.getMethod("listFiles", java.io.FileFilter.class);
        final Method list2 = java.io.File.class.getMethod("listFiles", java.io.FilenameFilter.class);

        IntHashMap cache;
        JavaMethod[] methods;
        Binding binding = new Binding(new Frame(), null, null, new BacktraceElement());
        JavaMethod result; IRubyObject[] args;

        // arity 1 :

        BlockBody body1 = new NullBlockBody() {
            @Override public Arity arity() { return Arity.ONE_ARGUMENT; }
        };
        RubyProc dummyProc = RubyProc.newProc(runtime, new Block(body1, binding), Block.Type.PROC);

        cache = IntHashMap.nullMap();
        methods = new JavaMethod[] {
            new JavaMethod(runtime, list1), new JavaMethod(runtime, list2)
        };
        result = CallableSelector.matchingCallableArityOne(runtime, cache, methods, dummyProc);
        assertEquals(new JavaMethod(runtime, list1), result);

        cache = IntHashMap.nullMap();
        args = new IRubyObject[] { dummyProc };
        result = CallableSelector.matchingCallableArityN(runtime, cache, methods, args);
        assertEquals(new JavaMethod(runtime, list1), result);

        cache = IntHashMap.nullMap();
        methods = new JavaMethod[] { // "reverse" method order
            new JavaMethod(runtime, list2), new JavaMethod(runtime, list1)
        };
        result = CallableSelector.matchingCallableArityOne(runtime, cache, methods, dummyProc);
        assertEquals(new JavaMethod(runtime, list1), result);

        cache = IntHashMap.nullMap();
        args = new IRubyObject[] { dummyProc };
        result = CallableSelector.matchingCallableArityN(runtime, cache, methods, args);
        assertEquals(new JavaMethod(runtime, list1), result);

        // arity 2 :

        BlockBody body2 = new NullBlockBody() {
            @Override public Arity arity() { return Arity.TWO_ARGUMENTS; }
        };
        dummyProc = RubyProc.newProc(runtime, new Block(body2, binding), Block.Type.PROC);

        cache = IntHashMap.nullMap();
        methods = new JavaMethod[] {
            new JavaMethod(runtime, list1), new JavaMethod(runtime, list2)
        };
        result = CallableSelector.matchingCallableArityOne(runtime, cache, methods, dummyProc);
        assertEquals(new JavaMethod(runtime, list2), result);

        cache = IntHashMap.nullMap();
        args = new IRubyObject[] { dummyProc };
        result = CallableSelector.matchingCallableArityN(runtime, cache, methods, args);
        assertEquals(new JavaMethod(runtime, list2), result);

        cache = IntHashMap.nullMap();
        methods = new JavaMethod[] { // "reverse" method order
            new JavaMethod(runtime, list2), new JavaMethod(runtime, list1)
        };
        result = CallableSelector.matchingCallableArityOne(runtime, cache, methods, dummyProc);
        assertEquals(new JavaMethod(runtime, list2), result);

        cache = IntHashMap.nullMap();
        args = new IRubyObject[] { dummyProc };
        result = CallableSelector.matchingCallableArityN(runtime, cache, methods, args);
        assertEquals(new JavaMethod(runtime, list2), result);

        // arity -1 :

        BlockBody body_1 = new NullBlockBody() { // arity -1
            @Override public Arity arity() { return Arity.OPTIONAL; }
        };
        dummyProc = RubyProc.newProc(runtime, new Block(body_1, binding), Block.Type.PROC);

        cache = IntHashMap.nullMap();
        methods = new JavaMethod[] {
            new JavaMethod(runtime, list1), new JavaMethod(runtime, list2)
        };
        result = CallableSelector.matchingCallableArityOne(runtime, cache, methods, dummyProc);
        assertEquals(new JavaMethod(runtime, list1), result);

        cache = IntHashMap.nullMap();
        methods = new JavaMethod[] {
            new JavaMethod(runtime, list2), new JavaMethod(runtime, list1)
        };
        result = CallableSelector.matchingCallableArityOne(runtime, cache, methods, dummyProc);
        assertEquals(new JavaMethod(runtime, list1), result);

        // arity -3 :

        BlockBody body_3 = new NullBlockBody() { // arity -3
            @Override public Arity arity() { return Arity.TWO_REQUIRED; }
        };
        dummyProc = RubyProc.newProc(runtime, new Block(body_3, binding), Block.Type.PROC);

        cache = IntHashMap.nullMap();
        methods = new JavaMethod[] {
            new JavaMethod(runtime, list1), new JavaMethod(runtime, list2)
        };
        result = CallableSelector.matchingCallableArityOne(runtime, cache, methods, dummyProc);
        assertEquals(new JavaMethod(runtime, list2), result);

        cache = IntHashMap.nullMap();
        methods = new JavaMethod[] {
            new JavaMethod(runtime, list2), new JavaMethod(runtime, list1)
        };
        result = CallableSelector.matchingCallableArityOne(runtime, cache, methods, dummyProc);
        assertEquals(new JavaMethod(runtime, list2), result);

        // arity -2 :

        BlockBody body_2 = new NullBlockBody() { // arity -2 (arg1, *rest) should prefer (single)
            @Override public Arity arity() { return Arity.ONE_REQUIRED; }
        };
        dummyProc = RubyProc.newProc(runtime, new Block(body_2, binding), Block.Type.PROC);

        cache = IntHashMap.nullMap();
        methods = new JavaMethod[] {
            new JavaMethod(runtime, list1), new JavaMethod(runtime, list2)
        };
        result = CallableSelector.matchingCallableArityOne(runtime, cache, methods, dummyProc);
        assertEquals(new JavaMethod(runtime, list1), result);

        cache = IntHashMap.nullMap();
        methods = new JavaMethod[] {
            new JavaMethod(runtime, list2), new JavaMethod(runtime, list1)
        };
        result = CallableSelector.matchingCallableArityOne(runtime, cache, methods, dummyProc);
        assertEquals(new JavaMethod(runtime, list1), result);

    }

}
