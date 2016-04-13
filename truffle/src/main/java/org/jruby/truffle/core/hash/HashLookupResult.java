/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.hash;

/**
 * The result of looking for an entry (an {@link Entry}) in a Ruby hash. We get the previous entry in the lookup chain
 * for this index until the entry was found, the entry that was found, and the index that was used. There are three
 * possible outcomes for a search.
 * <ul>
 *     <li>There is nothing at that index, in which case the entry and previous entry in the chain will be
 *     {@code null}</li>
 *     <li>There were entries at that index, but none for our key, in which case the entry will be null, but the
 *     previous entry will be the last entry in the chain at that index, presumably where we will want to insert our
 *     new entry</li>
 *     <li>A entry was found for our key, in which case the entry will be the one correspond to the key, and the
 *     previous entry will be the one in the entry chain before that one</li>
 * </ul>
 */
public class HashLookupResult {

    private final int hashed;
    private final int index;
    private final Entry previousEntry;
    private final Entry entry;

    public HashLookupResult(int hashed, int index, Entry previousEntry, Entry entry) {
        this.hashed = hashed;
        this.index = index;
        this.previousEntry = previousEntry;
        this.entry = entry;
    }

    public int getHashed() {
        return hashed;
    }

    public int getIndex() {
        return index;
    }

    public Entry getPreviousEntry() {
        return previousEntry;
    }

    public Entry getEntry() {
        return entry;
    }

}
