/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.arguments;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.hash.HashLiteralNode;
import org.jruby.truffle.nodes.core.hash.HashNodes;
import org.jruby.truffle.nodes.methods.MarkerNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.runtime.hash.BucketsStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReadKeywordRestArgumentNode extends RubyNode {

    private final int minimum;
    private final String[] excludedKeywords;
    private final int kwIndex;

    public ReadKeywordRestArgumentNode(RubyContext context, SourceSection sourceSection, int minimum, String[] excludedKeywords, int kwIndex) {
        super(context, sourceSection);
        this.minimum = minimum;
        this.excludedKeywords = excludedKeywords;
        this.kwIndex = kwIndex;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (RubyArguments.isKwOptimized(frame.getArguments())) {
            Object restHash = RubyArguments.getOptimizedKeywordArgument(
                    frame.getArguments(), kwIndex);

            if (restHash instanceof MarkerNode.Marker) {
                // no rest keyword args hash passed
                return HashLiteralNode.create(getContext(), null,
                        new RubyNode[0]).execute(frame);
            } else {
                return restHash;
            }
        } else {
            return lookupRestKeywordArgumentHash(frame);
        }
    }

    private Object lookupRestKeywordArgumentHash(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        final DynamicObject hash = RubyArguments.getUserKeywordsHash(frame.getArguments(), minimum);

        if (hash == null) {
            return HashNodes.createEmptyHash(getContext().getCoreLibrary().getHashClass());
        }

        final List<Map.Entry<Object, Object>> entries = new ArrayList<>();

        outer: for (Map.Entry<Object, Object> keyValue : HashNodes.iterableKeyValues(hash)) {
            for (String excludedKeyword : excludedKeywords) {
                if (excludedKeyword.equals(keyValue.getKey().toString())) {
                    continue outer;
                }
            }

            entries.add(keyValue);
        }

        return BucketsStrategy.create(getContext().getCoreLibrary().getHashClass(), entries, HashNodes.isCompareByIdentity(hash));
    }

}
