/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.runtime.subsystems;

import com.oracle.truffle.api.source.BytesDecoder;
import com.oracle.truffle.api.source.Source;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SourceManager {

    private final RubyContext context;

    private final Map<String, Source> sources = new HashMap<>();

    public SourceManager(RubyContext context) {
        this.context = context;
    }

    // TODO CS 28-Feb-15 best not to synchronize so coarsely in the future - would like to load files concurrently
    public synchronized Source forFile(String path) {
        final String canonicalPath;

        try {
            canonicalPath = new File(path).getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Source source = sources.get(canonicalPath);

        if (source == null) {
            final byte[] bytes = FileUtils.readAllBytesInterruptedly(context, path);

            // Assume UTF-8 for the moment
            source = Source.fromBytes(bytes, path, new BytesDecoder.UTF8BytesDecoder());

            sources.put(canonicalPath, source);
        }

        return source;
    }

    public synchronized Source forFileBestFuzzily(final String path) {
        final List<Map.Entry<String, Source>> matches = new ArrayList<>(sources.entrySet());

        // TODO CS 28-Feb-15 only need the max value - don't need a full sort, but no convenient API?

        Collections.sort(matches, new Comparator<Map.Entry<String, Source>>() {

            @Override
            public int compare(Map.Entry<String, Source> a, Map.Entry<String, Source> b) {
                return Integer.compare(common(path, b.getKey()), common(path, a.getKey()));
            }

        });

        return matches.get(0).getValue();
    }

    public int common(String path, String existingPath) {
        int n = 0;

        while (n < path.length()
                && n < existingPath.length()
                && path.charAt(path.length() - n - 1) == existingPath.charAt(existingPath.length() - n - 1)) {
            n++;
        }

        return n;
    }

}
