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

import org.jruby.ast.*;

import java.util.ArrayList;
import java.util.List;

public class Arity {

    public static final Arity NO_ARGUMENTS = new Arity(0, 0, false, false, false, 0);
    public static final Arity ONE_REQUIRED = new Arity(1, 0, false, false, false, 0);

    private final int required;
    private final int optional;
    private final boolean allowsMore;
    private final int definedKeywords;
    private final boolean hasKeywords;
    private final boolean hasKeyRest;

    private final List<String> keywordArguments;

    public Arity(int required, int optional, boolean allowsMore, boolean hasKeywords, boolean hasKeyRest, int definedKeywords) {
        this.required = required;
        this.optional = optional;
        this.allowsMore = allowsMore;
        this.definedKeywords = definedKeywords;
        this.hasKeywords = hasKeywords;
        this.hasKeyRest = hasKeyRest;
        keywordArguments = null;
    }

    public Arity(int required, int optional, boolean allowsMore, boolean hasKeywords, boolean hasKeyRest, int definedKeywords, ArgsNode argsNode) {
        this.required = required;
        this.optional = optional;
        this.allowsMore = allowsMore;
        this.definedKeywords = definedKeywords;
        this.hasKeywords = hasKeywords;
        this.hasKeyRest = hasKeyRest;

        if (argsNode.hasKwargs()) {
            keywordArguments = new ArrayList<>();
            if (argsNode.getKeywords() != null) {
                for (Node node : argsNode.getKeywords().childNodes()) {
                    final KeywordArgNode kwarg = (KeywordArgNode) node;
                    final AssignableNode assignableNode = kwarg.getAssignable();

                    if (assignableNode instanceof LocalAsgnNode) {
                        keywordArguments.add(((LocalAsgnNode) assignableNode)
                                .getName());
                    } else if (assignableNode instanceof DAsgnNode) {
                        keywordArguments.add(((DAsgnNode) assignableNode)
                                .getName());
                    } else {
                        throw new UnsupportedOperationException(
                                "unsupported keyword arg " + node);
                    }
                }
            }
        } else {
            keywordArguments = null;
        }
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

    public int getCountKeywords() {
        return definedKeywords;
    }

    public boolean hasKeyRest() {
        return hasKeyRest;
    }

    public int getArityNumber() {
        int count = required;

        if (hasKeywords) {
            count++;
        }

        if (optional > 0 || allowsMore) {
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
                "required=" + required +
                ", optional=" + optional +
                ", allowsMore=" + allowsMore +
                ", definedKeywords=" + definedKeywords +
                ", hasKeywords=" + hasKeywords +
                ", hasKeyRest=" + hasKeyRest +
                '}';
    }
}
