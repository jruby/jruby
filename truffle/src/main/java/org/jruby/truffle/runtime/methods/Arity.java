/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.methods;

public class Arity {

    private final int required;
    private final int optional;
    private final boolean allowsMore;
    private final boolean hasKeywords;

    public Arity(int required, int optional, boolean allowsMore, boolean hasKeywords) {
        this.required = required;
        this.optional = optional;
        this.allowsMore = allowsMore;
        this.hasKeywords = hasKeywords;
    }

    public int getRequired() {
        return required;
    }

    public int getOptional() {
        return optional;
    }

    public boolean allowsMore() {
        return allowsMore;
    }

    public boolean hasKeywords() {
        return hasKeywords;
    }

}
