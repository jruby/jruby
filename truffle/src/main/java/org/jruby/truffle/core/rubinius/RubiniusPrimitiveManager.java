/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.rubinius;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GeneratedBy;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages the available Rubinius primitive calls.
 */
public class RubiniusPrimitiveManager {

    private final ConcurrentMap<String, RubiniusPrimitiveConstructor> primitives = new ConcurrentHashMap<String, RubiniusPrimitiveConstructor>();

    public RubiniusPrimitiveConstructor getPrimitive(String name) {
        final RubiniusPrimitiveConstructor constructor = primitives.get(name);

        if (constructor == null) {
            return primitives.get(UndefinedPrimitiveNodes.NAME);
        }

        return constructor;
    }

    public void addAnnotatedPrimitives() {
        final List<NodeFactory<? extends RubyNode>> nodeFactories = new ArrayList<>();

        nodeFactories.addAll(VMPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(ObjectPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(TimePrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(StringPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(SymbolPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(FixnumPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(BignumPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(FloatPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(EncodingPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(EncodingConverterPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(RegexpPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(RandomizerPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(ArrayPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(StatPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(PointerPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(NativeFunctionPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(DirPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(IOPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(IOBufferPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(ExceptionPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(ThreadPrimitiveNodesFactory.getFactories());
        nodeFactories.addAll(WeakRefPrimitiveNodesFactory.getFactories());

        // This comes last as a catch-all
        nodeFactories.addAll(UndefinedPrimitiveNodesFactory.getFactories());

        for (NodeFactory<? extends RubyNode> nodeFactory : nodeFactories) {
            final GeneratedBy generatedBy = nodeFactory.getClass().getAnnotation(GeneratedBy.class);
            final Class<?> nodeClass = generatedBy.value();
            final RubiniusPrimitive annotation = nodeClass.getAnnotation(RubiniusPrimitive.class);
            primitives.putIfAbsent(annotation.name(), new RubiniusPrimitiveNodeConstructor(annotation, nodeFactory));
        }
    }

    @TruffleBoundary
    public void installPrimitive(String name, DynamicObject method) {
        assert RubyGuards.isRubyMethod(method);
        primitives.put(name, new RubiniusPrimitiveCallConstructor(method));
    }
}
