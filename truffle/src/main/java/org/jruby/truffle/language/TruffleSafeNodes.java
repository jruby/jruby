/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.CoreMethodNode;
import org.jruby.truffle.platform.UnsafeGroup;

import java.io.PrintStream;

@CoreClass("Truffle::Safe")
public abstract class TruffleSafeNodes {

    @CoreMethod(names = "puts", onSingleton = true, required = 1, unsafe = UnsafeGroup.SAFE_PUTS)
    public abstract static class PutsNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubyString(string)")
        public DynamicObject puts(DynamicObject string) {
            final StringBuilder builder = new StringBuilder();

            for (char c : string.toString().toCharArray()) {
                if (isAsciiPrintable(c)) {
                    builder.append(c);
                } else {
                    builder.append('?');
                }
            }

            builder.append(System.lineSeparator());

            new PrintStream(getContext().getEnv().out(), true).print(builder.toString());

            return nil();
        }

        private boolean isAsciiPrintable(char c) {
            return c >= 32 && c <= 126 || c == '\n' || c == '\t';
        }

    }

    @CoreMethod(names = "io_safe?", onSingleton = true)
    public abstract static class IsIOSafeNode extends CoreMethodNode {

        @Specialization
        public boolean ioSafe() {
            return getContext().getOptions().PLATFORM_SAFE_IO;
        }

    }

    @CoreMethod(names = "memory_safe?", onSingleton = true)
    public abstract static class IsMemorySafeNode extends CoreMethodNode {

        @Specialization
        public boolean memorySafe() {
            return getContext().getOptions().PLATFORM_SAFE_MEMORY;
        }

    }

    @CoreMethod(names = "signals_safe?", onSingleton = true)
    public abstract static class AreSignalsSafeNode extends CoreMethodNode {

        @Specialization
        public boolean signalsSafe() {
            return getContext().getOptions().PLATFORM_SAFE_SIGNALS;
        }

    }

    @CoreMethod(names = "processes_safe?", onSingleton = true)
    public abstract static class AreProcessesSafeNode extends CoreMethodNode {

        @Specialization
        public boolean processesSafe() {
            return getContext().getOptions().PLATFORM_SAFE_PROCESSES;
        }

    }

}
