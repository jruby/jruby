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

public class BucketSearchResult {

    private final Bucket endOfLookupChain;
    private final Bucket bucket;
    private final int index;

    public BucketSearchResult(int index, Bucket endOfLookupChain, Bucket bucket) {
        this.index = index;
        this.endOfLookupChain = endOfLookupChain;
        this.bucket = bucket;
    }

    public int getIndex() {
        return index;
    }

    public Bucket getEndOfLookupChain() {
        return endOfLookupChain;
    }

    public Bucket getBucket() {
        return bucket;
    }

}
