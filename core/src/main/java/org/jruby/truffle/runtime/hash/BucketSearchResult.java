/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.hash;

/**
 * The result of looking for a bucket (a {@link Bucket}) in a Ruby hash. We get the previous bucket in the lookup chain
 * for this index until the bucket was found, the bucket that was found, and the index that was used. There are three
 * possible outcomes for a search.
 * <ul>
 *     <li>There is nothing at that index, in which case the bucket and last bucket in the chain will be
 *     {@code null}</li>
 *     <li>There were buckets at that index, but none for our key, in which case the bucket will be null, but the
 *     previous bucket will be the last bucket in the chain at that index, presumably where we will want to insert our
 *     new bucket</li>
 *     <li>A bucket was found for our key, in which case the bucket will be the one correspond to the key, and the
 *     previous bucket will be the one in the bucket chain before that one</li>
 * </ul>
 */
public class BucketSearchResult {

    private final Bucket previousBucket;
    private final Bucket bucket;
    private final int index;

    public BucketSearchResult(int index, Bucket previousBucket, Bucket bucket) {
        this.index = index;
        this.previousBucket = previousBucket;
        this.bucket = bucket;
    }

    public int getIndex() {
        return index;
    }

    public Bucket getPreviousBucket() {
        return previousBucket;
    }

    public Bucket getBucket() {
        return bucket;
    }

}
