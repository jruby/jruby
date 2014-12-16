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

public class Bucket {

    private Object key;
    private Object value;

    private Bucket previousInLookup;
    private Bucket nextInLookup;

    private Bucket previousInSequence;
    private Bucket nextInSequence;

    public Bucket(Object key, Object value) {
        this.key = key;
        this.value = value;
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

    public Bucket getPreviousInLookup() {
        return previousInLookup;
    }

    public void setPreviousInLookup(Bucket previousInLookup) {
        this.previousInLookup = previousInLookup;
    }

    public Bucket getNextInLookup() {
        return nextInLookup;
    }

    public void setNextInLookup(Bucket nextInLookup) {
        this.nextInLookup = nextInLookup;
    }

    public Bucket getPreviousInSequence() {
        return previousInSequence;
    }

    public void setPreviousInSequence(Bucket previousInSequence) {
        this.previousInSequence = previousInSequence;
    }

    public Bucket getNextInSequence() {
        return nextInSequence;
    }

    public void setNextInSequence(Bucket nextInSequence) {
        this.nextInSequence = nextInSequence;
    }

}
