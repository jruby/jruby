/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.hash;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.collections.BoundaryIterable;
import org.jruby.truffle.core.string.StringUtils;
import org.jruby.truffle.language.RubyGuards;

import java.util.Collections;
import java.util.Iterator;

public abstract class HashOperations {

    public static boolean verifyStore(RubyContext context, DynamicObject hash) {
        return verifyStore(context, Layouts.HASH.getStore(hash), Layouts.HASH.getSize(hash), Layouts.HASH.getFirstInSequence(hash), Layouts.HASH.getLastInSequence(hash));
    }

    public static boolean verifyStore(RubyContext context, Object store, int size, Entry firstInSequence, Entry lastInSequence) {
        assert store == null || store.getClass() == Object[].class || store instanceof Entry[];

        if (store == null) {
            assert size == 0;
            assert firstInSequence == null;
            assert lastInSequence == null;
        } else if (store instanceof Entry[]) {
            assert lastInSequence == null || lastInSequence.getNextInSequence() == null;

            final Entry[] entryStore = (Entry[]) store;

            Entry foundFirst = null;
            Entry foundLast = null;
            int foundSizeBuckets = 0;

            for (int n = 0; n < entryStore.length; n++) {
                Entry entry = entryStore[n];

                while (entry != null) {
                    foundSizeBuckets++;

                    if (entry == firstInSequence) {
                        assert foundFirst == null;
                        foundFirst = entry;
                    }

                    if (entry == lastInSequence) {
                        assert foundLast == null;
                        foundLast = entry;
                    }

                    entry = entry.getNextInLookup();
                }
            }

            assert foundSizeBuckets == size;
            assert firstInSequence == foundFirst;
            assert lastInSequence == foundLast;

            int foundSizeSequence = 0;
            Entry entry = firstInSequence;

            while (entry != null) {
                foundSizeSequence++;

                if (entry.getNextInSequence() == null) {
                    assert entry == lastInSequence;
                } else {
                    assert entry.getNextInSequence().getPreviousInSequence() == entry;
                }

                entry = entry.getNextInSequence();

                assert entry != firstInSequence;
            }

            assert foundSizeSequence == size : StringUtils.format("%d %d", foundSizeSequence, size);
        } else if (store.getClass() == Object[].class) {
            assert ((Object[]) store).length == context.getOptions().HASH_PACKED_ARRAY_MAX * PackedArrayStrategy.ELEMENTS_PER_ENTRY : ((Object[]) store).length;

            final Object[] packedStore = (Object[]) store;

            for (int n = 0; n < context.getOptions().HASH_PACKED_ARRAY_MAX; n++) {
                if (n < size) {
                    assert packedStore[n * 2] != null;
                    assert packedStore[n * 2 + 1] != null;
                }
            }

            assert firstInSequence == null;
            assert lastInSequence == null;
        }

        return true;
    }

    @TruffleBoundary
    public static Iterator<KeyValue> iterateKeyValues(DynamicObject hash) {
        assert RubyGuards.isRubyHash(hash);

        if (HashGuards.isNullHash(hash)) {
            return Collections.emptyIterator();
        } if (HashGuards.isPackedHash(hash)) {
            return PackedArrayStrategy.iterateKeyValues((Object[]) Layouts.HASH.getStore(hash), Layouts.HASH.getSize(hash));
        } else if (HashGuards.isBucketHash(hash)) {
            return BucketsStrategy.iterateKeyValues(Layouts.HASH.getFirstInSequence(hash));
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @TruffleBoundary
    public static BoundaryIterable<KeyValue> iterableKeyValues(final DynamicObject hash) {
        assert RubyGuards.isRubyHash(hash);

        return BoundaryIterable.wrap(() -> iterateKeyValues(hash));
    }

}
