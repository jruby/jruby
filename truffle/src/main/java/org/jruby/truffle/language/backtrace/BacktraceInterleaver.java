/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.backtrace;

import org.jruby.truffle.core.string.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class BacktraceInterleaver {

    public static List<String> interleave(List<String> rubyBacktrace, StackTraceElement[] javaStacktrace) {
        final List<String> interleaved = new ArrayList<>();

        int javaIndex = 0;

        for (String rubyLine : rubyBacktrace) {
            if (javaIndex < javaStacktrace.length) {
                interleaved.add(format(javaStacktrace[javaIndex]));
                javaIndex++;

                while (javaIndex < javaStacktrace.length && !isCallBoundary(javaStacktrace[javaIndex])) {
                    interleaved.add(format(javaStacktrace[javaIndex]));
                    javaIndex++;
                }
            }

            interleaved.add(rubyLine);
        }

        return interleaved;
    }

    private static boolean isCallBoundary(StackTraceElement element) {
        return element.toString().startsWith("com.oracle.graal.truffle.OptimizedCallTarget.callProxy")
                || element.toString().startsWith("com.oracle.truffle.api.impl.DefaultCallTarget.call");
    }

    private static String format(StackTraceElement element) {
        return StringUtils.format("\t\t%s", element);
    }

}
