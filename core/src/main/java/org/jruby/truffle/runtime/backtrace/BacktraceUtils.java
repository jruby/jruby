/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.backtrace;

public class BacktraceUtils {

    public static void align(String[] lines, int ignore, String alignAt) {
        int max = 0;

        for (int n = ignore; n < lines.length; n++) {
            max = Math.max(max, lines[n].indexOf(alignAt));
        }

        for (int n = ignore; n < lines.length; n++) {
            final StringBuilder align = new StringBuilder();

            for (int i = 0; i < max - lines[n].indexOf(alignAt); i++) {
                align.append(' ');
            }

            lines[n] = align + lines[n];
        }
    }

}
