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
package org.jruby.truffle.aot;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

/**
 * Visit files with knowledge of the root of the subtree the traversal started from.
 */
interface RootedFileVisitor<T> extends FileVisitor<T> {
    void setRoot(T root);

    T getRoot();

    static String rubyJarPath() {
        String svmJarPath = RootedFileVisitor.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        String parentDir = new File(svmJarPath).getParent();

        try {
            // First try reading in the primary Ruby binary suite JAR.
            File ret = new File(parentDir + "/../mx.imports/binary/jruby/mxbuild/dists/ruby.jar");

            if (ret.exists()) {
                return ret.getCanonicalPath();
            } else {
                // Fallback to the locally built source suite JAR.
                return new File(parentDir + "/../../../jruby/mxbuild/dists/ruby.jar").getCanonicalPath();
            }

        } catch (IOException e) {
            return null;
        }
    }

    static void visitEachFileOnClassPath(RootedFileVisitor<Path> visitor) {
        try (FileSystem jarFileSystem = FileSystems.newFileSystem(URI.create("jar:file:" + rubyJarPath()), Collections.emptyMap())) {
            Path root = jarFileSystem.getPath("/");
            visitor.setRoot(root);
            Files.walkFileTree(root, visitor);
        } catch (IOException ex) {
            throw new Error(ex);
        }
    }
}
