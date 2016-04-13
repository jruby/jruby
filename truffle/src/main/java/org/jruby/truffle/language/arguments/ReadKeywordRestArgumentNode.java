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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.hash.BucketsStrategy;
import org.jruby.truffle.core.hash.HashOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReadKeywordRestArgumentNode extends RubyNode {

    private final String[] excludedKeywords;

    @Child private ReadUserKeywordsHashNode readUserKeywordsHashNode;

    private final ConditionProfile noHash = ConditionProfile.createBinaryProfile();

    public ReadKeywordRestArgumentNode(RubyContext context, SourceSection sourceSection,
                                       int minimum, String[] excludedKeywords) {
        super(context, sourceSection);
        this.excludedKeywords = excludedKeywords;
        readUserKeywordsHashNode = new ReadUserKeywordsHashNode(context, sourceSection, minimum);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return lookupRestKeywordArgumentHash(frame);
    }

    private Object lookupRestKeywordArgumentHash(VirtualFrame frame) {
        final Object hash = readUserKeywordsHashNode.execute(frame);

        if (noHash.profile(hash == null)) {
            return Layouts.HASH.createHash(coreLibrary().getHashFactory(), null, 0, null, null, null, null, false);
        }

        CompilerDirectives.bailout("Ruby keyword arguments aren't optimized");

        final DynamicObject hashObject = (DynamicObject) hash;

        final List<Map.Entry<Object, Object>> entries = new ArrayList<>();

        outer: for (Map.Entry<Object, Object> keyValue : HashOperations.iterableKeyValues(hashObject)) {
            if (!RubyGuards.isRubySymbol(keyValue.getKey())) {
                continue;
            }

            for (String excludedKeyword : excludedKeywords) {
                if (excludedKeyword.equals(keyValue.getKey().toString())) {
                    continue outer;
                }
            }

            entries.add(keyValue);
        }

        return BucketsStrategy.create(getContext(), entries, Layouts.HASH.getCompareByIdentity(hashObject));
    }

}
