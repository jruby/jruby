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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.source.Source;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.core.string.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Locale;

public class SourceLoader {

    public static final String TRUFFLE_SCHEME = "truffle:";
    public static final String JRUBY_SCHEME = "jruby:";

    private final RubyContext context;

    public SourceLoader(RubyContext context) {
        this.context = context;
    }

    @TruffleBoundary
    public Source loadMain(String path) throws IOException {
        if (path.equals("-e")) {
            return loadFragment(new String(context.getOptions().INLINE_SCRIPT, StandardCharsets.UTF_8), "-e");
        } else if (path.equals("-")) {
            return Source.newBuilder(new InputStreamReader(System.in)).name(path).mimeType(RubyLanguage.MIME_TYPE).build();
        } else {
            final File file = new File(path).getCanonicalFile();
            ensureReadable(path, file);

            // The main source file *must* be named as it's given for __FILE__
            return Source.newBuilder(file).name(path).mimeType(RubyLanguage.MIME_TYPE).build();
        }
    }

    @TruffleBoundary
    public Source load(String canonicalPath) throws IOException {
        if (canonicalPath.startsWith(TRUFFLE_SCHEME) || canonicalPath.startsWith(JRUBY_SCHEME)) {
            return loadResource(canonicalPath);
        } else {
            final File file = new File(canonicalPath).getCanonicalFile();
            ensureReadable(canonicalPath, file);

            if (canonicalPath.toLowerCase().endsWith(".su")) {
                return Source.newBuilder(file).name(file.getPath()).mimeType(RubyLanguage.CEXT_MIME_TYPE).build();
            } else {
                // We need to assume all other files are Ruby, so the file type detection isn't enough
                return Source.newBuilder(file).name(file.getPath()).mimeType(RubyLanguage.MIME_TYPE).build();
            }
        }
    }

    @TruffleBoundary
    public Source loadFragment(String fragment, String name) {
        return Source.newBuilder(fragment).name(name).mimeType(RubyLanguage.MIME_TYPE).build();
    }

    @TruffleBoundary
    private Source loadResource(String path) throws IOException {
        if (TruffleOptions.AOT) {
            if (!(path.startsWith(SourceLoader.TRUFFLE_SCHEME) || path.startsWith(SourceLoader.JRUBY_SCHEME))) {
                throw new UnsupportedOperationException();
            }

            final String canonicalPath = JRubySourceLoaderSupport.canonicalizeResourcePath(path);
            final JRubySourceLoaderSupport.CoreLibraryFile coreFile = JRubySourceLoaderSupport.allCoreLibraryFiles.get(canonicalPath);
            if (coreFile == null) {
                throw new FileNotFoundException(path);
            }

            return Source.newBuilder(new InputStreamReader(new ByteArrayInputStream(coreFile.code), StandardCharsets.UTF_8)).name(path).mimeType(RubyLanguage.MIME_TYPE).build();
        } else {
            if (!path.toLowerCase(Locale.ENGLISH).endsWith(".rb")) {
                throw new FileNotFoundException(path);
            }

            final Class<?> relativeClass;
            final Path relativePath;

            if (path.startsWith(TRUFFLE_SCHEME)) {
                relativeClass = RubyContext.class;
                relativePath = FileSystems.getDefault().getPath(path.substring(TRUFFLE_SCHEME.length()));
            } else if (path.startsWith(JRUBY_SCHEME)) {
                relativeClass = jrubySchemeRelativeClass();
                relativePath = FileSystems.getDefault().getPath(path.substring(JRUBY_SCHEME.length()));
            } else {
                throw new UnsupportedOperationException();
            }

            final Path normalizedPath = relativePath.normalize();
            final InputStream stream = relativeClass.getResourceAsStream(StringUtils.replace(normalizedPath.toString(), '\\', '/'));

            if (stream == null) {
                throw new FileNotFoundException(path);
            }

            return Source.newBuilder(new InputStreamReader(stream, StandardCharsets.UTF_8)).name(path).mimeType(RubyLanguage.MIME_TYPE).build();
        }
    }

    private static Class<?> jrubySchemeRelativeClass() {
        // TODO CS 3-Dec-16 AOT?

        try {
            return Class.forName("org.jruby.Ruby");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static void ensureReadable(String path, File file) throws IOException {
        if (!file.canRead()) {
            throw new IOException("Can't read file " + path);
        }
    }

}
