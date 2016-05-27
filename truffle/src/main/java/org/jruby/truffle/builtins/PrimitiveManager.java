/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.builtins;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GeneratedBy;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.core.ObjectNodesFactory;
import org.jruby.truffle.core.VMPrimitiveNodesFactory;
import org.jruby.truffle.core.array.ArrayNodesFactory;
import org.jruby.truffle.core.dir.DirNodesFactory;
import org.jruby.truffle.core.encoding.EncodingConverterNodesFactory;
import org.jruby.truffle.core.encoding.EncodingNodesFactory;
import org.jruby.truffle.core.exception.ExceptionNodesFactory;
import org.jruby.truffle.core.numeric.BignumNodesFactory;
import org.jruby.truffle.core.numeric.FixnumNodesFactory;
import org.jruby.truffle.core.numeric.FloatNodesFactory;
import org.jruby.truffle.core.rubinius.IOBufferPrimitiveNodesFactory;
import org.jruby.truffle.core.rubinius.IOPrimitiveNodesFactory;
import org.jruby.truffle.core.rubinius.NativeFunctionPrimitiveNodesFactory;
import org.jruby.truffle.core.rubinius.RandomizerPrimitiveNodesFactory;
import org.jruby.truffle.core.rubinius.RegexpPrimitiveNodesFactory;
import org.jruby.truffle.core.rubinius.StatPrimitiveNodesFactory;
import org.jruby.truffle.core.rubinius.UndefinedPrimitiveNodes;
import org.jruby.truffle.core.rubinius.UndefinedPrimitiveNodesFactory;
import org.jruby.truffle.core.rubinius.WeakRefPrimitiveNodesFactory;
import org.jruby.truffle.core.string.StringNodesFactory;
import org.jruby.truffle.core.symbol.SymbolNodesFactory;
import org.jruby.truffle.core.thread.ThreadNodesFactory;
import org.jruby.truffle.core.time.TimeNodesFactory;
import org.jruby.truffle.extra.ffi.PointerPrimitiveNodesFactory;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages the available Rubinius primitive calls.
 */
public class PrimitiveManager {

    private final ConcurrentMap<String, PrimitiveConstructor> primitives = new ConcurrentHashMap<String, PrimitiveConstructor>();

    public PrimitiveConstructor getPrimitive(String name) {
        final PrimitiveConstructor constructor = primitives.get(name);

        if (constructor == null) {
            return primitives.get(UndefinedPrimitiveNodes.NAME);
        }

        return constructor;
    }

    public void addAnnotatedPrimitives() {
        final List<NodeFactory<? extends RubyNode>> nodeFactories = new ArrayList<>();

        nodeFactories.addAll(VMPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(ObjectNodesFactory.getFactories());
        nodeFactories.addAll(TimeNodesFactory.getFactories());
        nodeFactories.addAll(StringNodesFactory.getFactories());
        nodeFactories.addAll(SymbolNodesFactory.getFactories());
        nodeFactories.addAll(FixnumNodesFactory.getFactories());
        nodeFactories.addAll(BignumNodesFactory.getFactories());
        nodeFactories.addAll(FloatNodesFactory.getFactories());
        nodeFactories.addAll(EncodingNodesFactory.getFactories());
        nodeFactories.addAll(EncodingConverterNodesFactory.getFactories());
        nodeFactories.addAll(RegexpPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(RandomizerPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(ArrayNodesFactory.getFactories());
        nodeFactories.addAll(StatPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(PointerPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(NativeFunctionPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(DirNodesFactory.getFactories());
        nodeFactories.addAll(IOPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(IOBufferPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(ExceptionNodesFactory.getFactories());
        nodeFactories.addAll(ThreadNodesFactory.getFactories());
        nodeFactories.addAll(WeakRefPrimitiveNodesFactory.getFactories());

        // This comes last as a catch-all
        nodeFactories.addAll(UndefinedPrimitiveNodesFactory.getFactories());

        for (NodeFactory<? extends RubyNode> nodeFactory : nodeFactories) {
            final GeneratedBy generatedBy = nodeFactory.getClass().getAnnotation(GeneratedBy.class);
            final Class<?> nodeClass = generatedBy.value();
            final Primitive annotation = nodeClass.getAnnotation(Primitive.class);
            if (annotation != null) {
                primitives.putIfAbsent(annotation.name(), new PrimitiveNodeConstructor(annotation, nodeFactory));
            }
        }
    }

    @TruffleBoundary
    public void installPrimitive(String name, DynamicObject method) {
        assert RubyGuards.isRubyMethod(method);
        primitives.put(name, new PrimitiveCallConstructor(method));
    }
}
