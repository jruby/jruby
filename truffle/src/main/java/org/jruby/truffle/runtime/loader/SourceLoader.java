/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.loader;

import com.oracle.truffle.api.source.Source;
import org.jruby.Ruby;
import org.jruby.truffle.runtime.RubyContext;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class SourceLoader {

    private final RubyContext context;

    public SourceLoader(RubyContext context) {
        this.context = context;
    }

    public Source load(String canonicalPath) throws IOException {
        if (canonicalPath.equals("-e")) {
            return loadInlineScript();
        } else if (canonicalPath.startsWith("truffle:/") || canonicalPath.startsWith("jruby:/")) {
            return loadResource(canonicalPath);
        } else {
            assert new File(canonicalPath).getCanonicalPath().equals(canonicalPath) : canonicalPath;
            return Source.fromFileName(canonicalPath);
        }
    }

    private Source loadInlineScript() {
        return Source.fromText(new String(context.getRuntime().getInstanceConfig().inlineScript(),
                StandardCharsets.UTF_8), "-e");
    }

    private Source loadResource(String canonicalPath) throws IOException {
        final Class relativeClass;

        if (canonicalPath.startsWith("truffle:/")) {
            relativeClass = RubyContext.class;
        } else if (canonicalPath.startsWith("jruby:/")) {
            relativeClass = Ruby.class;
        } else {
            throw new UnsupportedOperationException();
        }

        final InputStream stream = relativeClass.getResourceAsStream(canonicalPath);

        if (stream == null) {
            throw new FileNotFoundException(canonicalPath);
        }

        return Source.fromReader(new InputStreamReader(stream), canonicalPath);
    }

}
