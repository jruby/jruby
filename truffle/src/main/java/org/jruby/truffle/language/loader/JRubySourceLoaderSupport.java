/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 *
 * Some of the code in this class is modified from org.jruby.util.StringSupport,
 * licensed under the same EPL1.0/GPL 2.0/LGPL 2.1 used throughout.
 */
package org.jruby.truffle.language.loader;

import com.oracle.truffle.api.source.Source;
import org.jruby.truffle.aot.RootedFileVisitor;
import org.jruby.truffle.aot.SimpleRootedFileVisitor;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class JRubySourceLoaderSupport {
    public static final Map<String, CoreLibraryFile> allCoreLibraryFiles = getCoreLibrary();

    public static String canonicalizeResourcePath(String path) {
        String tmpPath = path;
        if (path.startsWith(SourceLoader.TRUFFLE_SCHEME)) {
            tmpPath = path.substring(SourceLoader.TRUFFLE_SCHEME.length());
        } else if (path.startsWith(SourceLoader.JRUBY_SCHEME)) {
            tmpPath = path.substring(SourceLoader.JRUBY_SCHEME.length());
        } else if (tmpPath.startsWith("core:/")) {
            tmpPath = tmpPath.substring("core:/".length());
        } else if (tmpPath.startsWith("uri:classloader:/")) {
            tmpPath = tmpPath.substring("uri:classloader:/".length());
        }

        if (tmpPath.startsWith("/")) {
            tmpPath = tmpPath.substring(1);
        }

        try {
            return new URI(tmpPath).normalize().getPath();
        } catch (URISyntaxException e) {
            return tmpPath;
        }
    }

    private static Set<String> getCoreLibraryFiles() {
        Set<String> coreLibraryFiles = new HashSet<>();

        RootedFileVisitor<Path> visitor = new SimpleRootedFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = getRoot().relativize(file).toString();
                if (fileName.endsWith(".rb")) {
                    coreLibraryFiles.add(fileName);
                }
                return FileVisitResult.CONTINUE;
            }
        };

        RootedFileVisitor.visitEachFileOnClassPath(visitor);
        return coreLibraryFiles;
    }

    @SuppressWarnings("deprecation")
    private static Map<String, CoreLibraryFile> getCoreLibrary() {
        Map<String, CoreLibraryFile> coreLibrary = new HashMap<>();
        Set<String> coreLibraryFiles = getCoreLibraryFiles();
        try (FileSystem jarFileSystem = FileSystems.newFileSystem(URI.create("jar:file:" + RootedFileVisitor.rubyJarPath()), Collections.emptyMap())) {
            for (String name : coreLibraryFiles) {
                Path filePath = jarFileSystem.getPath("/" + name);
                try (Reader reader = Files.newBufferedReader(filePath)) {
                    if (reader != null) {
                        Source source = Source.fromReader(reader, name);

                        byte[] code = source.getCode().getBytes(StandardCharsets.UTF_8);
                        coreLibrary.put(name, new CoreLibraryFile(code, null));
                    } else {
                        throw new Error("Unable to load ruby core library file " + name);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return coreLibrary;
    }

    public static class CoreLibraryFile {
        public final byte[] code;
        public final Map<Integer, CoreLibraryMethod> methods;

        public CoreLibraryFile(byte[] code, Map<Integer, CoreLibraryMethod> methods) {
            this.code = code;
            this.methods = methods;
        }
    }

    public static class CoreLibraryMethod {
        public final String name;
        public final byte[] code;

        public CoreLibraryMethod(String name, byte[] code) {
            this.name = name;
            this.code = code;
        }
    }

}
