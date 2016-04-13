/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.loader;

import com.oracle.truffle.api.source.Source;
import org.jruby.Ruby;
import org.jruby.truffle.RubyContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class SourceLoader {

    public static final String TRUFFLE_SCHEME = "truffle:";
    public static final String JRUBY_SCHEME = "jruby:";

    private final RubyContext context;

    public SourceLoader(RubyContext context) {
        this.context = context;
    }

    public Source load(String canonicalPath) throws IOException {
        if (canonicalPath.equals("-e")) {
            return loadInlineScript();
        } else if (canonicalPath.startsWith(TRUFFLE_SCHEME) || canonicalPath.startsWith(JRUBY_SCHEME)) {
            return loadResource(canonicalPath);
        } else {
            assert new File(canonicalPath).getCanonicalPath().equals(canonicalPath) : canonicalPath;
            return Source.fromFileName(canonicalPath);
        }
    }

    private Source loadInlineScript() {
        return Source.fromText(new String(context.getJRubyRuntime().getInstanceConfig().inlineScript(),
                StandardCharsets.UTF_8), "-e");
    }

    private Source loadResource(String path) throws IOException {
        if (!path.toLowerCase(Locale.ENGLISH).endsWith(".rb")) {
            throw new FileNotFoundException(path);
        }

        final Class<?> relativeClass;
        final String relativePath;

        if (path.startsWith(TRUFFLE_SCHEME)) {
            relativeClass = RubyContext.class;
            relativePath = path.substring(TRUFFLE_SCHEME.length());
        } else if (path.startsWith(JRUBY_SCHEME)) {
            relativeClass = Ruby.class;
            relativePath = path.substring(JRUBY_SCHEME.length());
        } else {
            throw new UnsupportedOperationException();
        }

        final String canonicalPath = (new File(relativePath)).getCanonicalPath();
        final InputStream stream = relativeClass.getResourceAsStream(canonicalPath);

        if (stream == null) {
            throw new FileNotFoundException(path);
        }

        return Source.fromReader(new InputStreamReader(stream, StandardCharsets.UTF_8), path);
    }

}
