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
 * An entry in the Ruby hash. That is, a container for a key and a value, and a member of two lists - the chain of
 * buckets for a given index, and the chain of entries for the insertion order across the whole hash.
 */
public final class Entry {

    private int hashed;
    private Object key;
    private Object value;

    private Entry nextInLookup;

    private Entry previousInSequence;
    private Entry nextInSequence;

    public Entry(int hashed, Object key, Object value) {
        this.hashed = hashed;
        this.key = key;
        this.value = value;
    }

    public int getHashed() {
        return hashed;
    }

    public void setHashed(int hashed) {
        this.hashed = hashed;
    }

    public Object getKey() {
        return key;
    }

    public void setKey(Object key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setKeyValue(int hashed, Object key, Object value) {
        this.hashed = hashed;
        this.key = key;
        this.value = value;
    }

    public Entry getNextInLookup() {
        return nextInLookup;
    }

    public void setNextInLookup(Entry nextInLookup) {
        this.nextInLookup = nextInLookup;
    }

    public Entry getPreviousInSequence() {
        return previousInSequence;
    }

    public void setPreviousInSequence(Entry previousInSequence) {
        this.previousInSequence = previousInSequence;
    }

    public Entry getNextInSequence() {
        return nextInSequence;
    }

    public void setNextInSequence(Entry nextInSequence) {
        this.nextInSequence = nextInSequence;
    }

}
