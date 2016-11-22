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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.Source;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SourceCache {

    private final SourceLoader loader;

    private final Map<String, Source> sources = new HashMap<>();

    public SourceCache(SourceLoader loader) {
        this.loader = loader;
    }

    @TruffleBoundary
    public synchronized Source getMainSource(String path) throws IOException {
        Source mainSource = loader.loadMain(path);
        sources.put(path, mainSource);
        return mainSource;
    }

    @TruffleBoundary
    public synchronized Source getSource(String canonicalPath) throws IOException {
        Source source = sources.get(canonicalPath);

        if (source == null) {
            source = loader.load(canonicalPath);
            sources.put(canonicalPath, source);
        }

        return source;
    }

}
