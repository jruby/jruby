/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.util;

import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.subsystems.ThreadManager.BlockingActionWithoutGlobalLock;

public class FileUtils {

    public static byte[] readAllBytesInterruptedly(RubyContext context, String file) {
        RubyNode.notDesignedForCompilation();

        final Path path = Paths.get(file);

        return context.getThreadManager().runUntilResult(new BlockingActionWithoutGlobalLock<byte[]>() {
            @Override
            public byte[] block() throws InterruptedException {
                try {
                    // TODO: only read small chunks to avoid too much repeated execution.
                    return Files.readAllBytes(path);
                } catch (ClosedByInterruptException e) {
                    throw new InterruptedException();
                } catch (IOException e) {
                    // TODO: handle this more nicely
                    throw new RuntimeException(e);
                }
            }
        });
    }

}
