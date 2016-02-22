/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.language.loader;

import com.oracle.truffle.api.source.Source;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SourceCache {

    private final SourceLoader loader;

    private final Map<String, Source> sources = new HashMap<>();

    public SourceCache(SourceLoader loader) {
        this.loader = loader;
    }

    public synchronized Source getSource(String canonicalPath) throws IOException {
        Source source = sources.get(canonicalPath);

        if (source == null) {
            source = loader.load(canonicalPath);
            sources.put(canonicalPath, source);
        }

        return source;
    }

    public synchronized Source getBestSourceFuzzily(final String fuzzyPath) {
        final List<Map.Entry<String, Source>> matches = new ArrayList<>(sources.entrySet());

        Collections.sort(matches, new Comparator<Map.Entry<String, Source>>() {

            @Override
            public int compare(Map.Entry<String, Source> a, Map.Entry<String, Source> b) {
                return Integer.compare(
                        lengthOfCommonPrefix(fuzzyPath, b.getKey()),
                        lengthOfCommonPrefix(fuzzyPath, a.getKey()));
            }

        });

        if (matches.isEmpty()) {
            return null;
        } else {
            return matches.get(0).getValue();
        }
    }

    private int lengthOfCommonPrefix(String a, String b) {
        int n = 0;

        while (n < a.length()
                && n < b.length()
                && a.charAt(a.length() - n - 1) == b.charAt(b.length() - n - 1)) {
            n++;
        }

        return n;
    }

}
