/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.arguments;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.hash.HashOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;

import java.util.Map;

public class ReadKeywordArgumentNode extends RubyNode {

    private final String name;
    private final ConditionProfile defaultProfile = ConditionProfile.createBinaryProfile();
    
    @Child private RubyNode defaultValue;
    @Child private ReadUserKeywordsHashNode readUserKeywordsHashNode;

    public ReadKeywordArgumentNode(RubyContext context, SourceSection sourceSection, int minimum, String name, RubyNode defaultValue) {
        super(context, sourceSection);
        this.name = name;
        this.defaultValue = defaultValue;
        readUserKeywordsHashNode = new ReadUserKeywordsHashNode(context, sourceSection, minimum);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final DynamicObject hash = (DynamicObject) readUserKeywordsHashNode.execute(frame);

        if (defaultProfile.profile(hash == null)) {
            return defaultValue.execute(frame);
        }

        Object value = lookupKeywordInHash(hash);

        if (defaultProfile.profile(value == null)) {
            return defaultValue.execute(frame);
        }

        return value;
    }

    @TruffleBoundary
    private Object lookupKeywordInHash(DynamicObject hash) {
        assert RubyGuards.isRubyHash(hash);

        for (Map.Entry<Object, Object> keyValue : HashOperations.iterableKeyValues(hash)) {
            if (RubyGuards.isRubySymbol(keyValue.getKey()) && keyValue.getKey().toString().equals(name)) {
                return keyValue.getValue();
            }
        }

        return null;
    }

}
