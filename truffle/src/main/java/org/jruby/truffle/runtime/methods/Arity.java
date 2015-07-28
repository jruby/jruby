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

import java.util.Collections;
import java.util.List;

public class Arity {

    public static final List<String> NO_KEYWORDS = Collections.emptyList();
    public static final Arity NO_ARGUMENTS = new Arity(0, 0, false);
    public static final Arity ONE_REQUIRED = new Arity(1, 0, false);
    public static final Arity AT_LEAST_ONE = new Arity(1, 0, true);

    private final int preRequired;
    private final int optional;
    private final boolean hasRest;
    private final int postRequired;
    private final boolean hasKeyRest;

    private final List<String> keywordArguments;

    public Arity(int preRequired, int optional, boolean hasRest) {
        this(preRequired, optional, hasRest, 0, NO_KEYWORDS, false);
    }

    public Arity(int preRequired, int optional, boolean hasRest, int postRequired, List<String> keywordArguments, boolean hasKeyRest) {
        this.preRequired = preRequired;
        this.optional = optional;
        this.hasRest = hasRest;
        this.postRequired = postRequired;
        this.keywordArguments = keywordArguments;
        this.hasKeyRest = hasKeyRest;

        assert keywordArguments != null && preRequired >= 0 && optional >= 0 && postRequired >= 0 : toString();
    }

    public int getPreRequired() {
        return preRequired;
    }

    public int getPostRequired() {
        return postRequired;
    }

    public int getRequired() {
        return preRequired + postRequired;
    }

    public int getOptional() {
        return optional;
    }

    public boolean hasRest() {
        return hasRest;
    }

    public int getRestPosition() {
        return preRequired + optional;
    }

    public boolean hasKeywords() {
        return keywordArguments != NO_KEYWORDS;
    }

    public int getCountKeywords() {
        return keywordArguments.size();
    }

    public boolean hasKeyRest() {
        return hasKeyRest;
    }

    public int getArityNumber() {
        int count = preRequired + postRequired;

        if (hasKeywords()) {
            count++;
        }

        if (optional > 0 || hasRest) {
            count = -count - 1;
        }

        return count;
    }

    public List<String> getKeywordArguments() {
        return keywordArguments;
    }

    @Override
    public String toString() {
        return "Arity{" +
                "preRequired=" + preRequired +
                ", optional=" + optional +
                ", hasRest=" + hasRest +
                ", postRequired=" + postRequired +
                ", keywordArguments=" + keywordArguments +
                ", hasKeyRest=" + hasKeyRest +
                '}';
    }
}
