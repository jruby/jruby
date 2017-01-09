/*
 * Copyright (c) 2016, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;
import java.util.Locale;
import java.util.regex.Pattern;

public final class RubyFileTypeDetector extends FileTypeDetector {

    private static final String[] KNOWN_RUBY_FILES = new String[]{ "Gemfile", "Rakefile", "Mavenfile" };
    private static final String[] KNOWN_RUBY_SUFFIXES = new String[]{ RubyLanguage.EXTENSION, ".rake", ".gemspec" };
    private static final Pattern SHEBANG_REGEXP = Pattern.compile("^#! ?/usr/bin/(env +ruby|ruby).*");

    @Override
    public String probeContentType(Path path) throws IOException {
        final Path fileNamePath = path.getFileName();
        if (fileNamePath == null) {
            return null;
        }

        final String fileName = fileNamePath.toString();
        final String lowerCaseFileName = fileName.toLowerCase(Locale.ROOT);

        for (String candidate : KNOWN_RUBY_SUFFIXES) {
            if (lowerCaseFileName.endsWith(candidate)) {
                return RubyLanguage.MIME_TYPE;
            }
        }

        for (String candidate : KNOWN_RUBY_FILES) {
            if (fileName.equals(candidate)) {
                return RubyLanguage.MIME_TYPE;
            }
        }

        try (BufferedReader fileContent = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            final String firstLine = fileContent.readLine();
            if (firstLine != null && SHEBANG_REGEXP.matcher(firstLine).matches()) {
                return RubyLanguage.MIME_TYPE;
            }
        } catch (IOException e) {
            // Reading random files as UTF-8 could cause all sorts of errors
        }

        return null;
    }
}
