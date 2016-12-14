/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import jnr.posix.SpawnFileAction;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.array.ArrayOperations;
import org.jruby.truffle.core.hash.HashOperations;
import org.jruby.truffle.core.hash.KeyValue;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.platform.UnsafeGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

@CoreClass("Truffle::Process")
public abstract class TruffleProcessNodes {

    @CoreMethod(names = "spawn", onSingleton = true, required = 4, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class SpawnNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = {
                "isRubyString(command)",
                "isRubyArray(arguments)",
                "isRubyArray(environmentVariables)",
                "isRubyHash(options)" })
        public int spawn(DynamicObject command,
                         DynamicObject arguments,
                         DynamicObject environmentVariables,
                         DynamicObject options) {

            Collection<SpawnFileAction> fileActions = parseOptions(options);

            int pid = call(
                    StringOperations.getString(command),
                    toStringArray(arguments),
                    toStringArray(environmentVariables),
                    fileActions);

            if (pid == -1) {
                // TODO (pitr 07-Sep-2015): needs compatibility improvements
                throw new RaiseException(coreExceptions().errnoError(getContext().getNativePlatform().getPosix().errno(), this));
            }

            return pid;
        }

        private String[] toStringArray(DynamicObject rubyStrings) {
            final int size = Layouts.ARRAY.getSize(rubyStrings);
            final Object[] unconvertedStrings = ArrayOperations.toObjectArray(rubyStrings);
            final String[] strings = new String[size];

            for (int i = 0; i < size; i++) {
                assert Layouts.STRING.isString(unconvertedStrings[i]);
                strings[i] = StringOperations.getString((DynamicObject) unconvertedStrings[i]);
            }

            return strings;
        }

        @TruffleBoundary
        private Collection<SpawnFileAction> parseOptions(DynamicObject options) {
            if (Layouts.HASH.getSize(options) == 0) {
                return Collections.emptyList();
            }

            Collection<SpawnFileAction> actions = new ArrayList<>();
            for (KeyValue keyValue : HashOperations.iterableKeyValues(options)) {
                final Object key = keyValue.getKey();
                final Object value = keyValue.getValue();

                if (Layouts.SYMBOL.isSymbol(key)) {
                    if (key == getSymbol("redirect_fd")) {
                        assert Layouts.ARRAY.isArray(value);
                        final DynamicObject array = (DynamicObject) value;
                        final int size = Layouts.ARRAY.getSize(array);
                        assert size % 2 == 0;
                        final Object[] store = ArrayOperations.toObjectArray(array);
                        for (int i = 0; i < size; i += 2) {
                            int from = (int) store[i];
                            int to = (int) store[i + 1];
                            if (to < 0) { // :child fd
                                to = -to - 1;
                            }
                            actions.add(SpawnFileAction.dup(to, from));
                        }
                        continue;
                    } else if (key == getSymbol("assign_fd")) {
                        assert Layouts.ARRAY.isArray(value);
                        final DynamicObject array = (DynamicObject) value;
                        final int size = Layouts.ARRAY.getSize(array);
                        assert size % 4 == 0;
                        final Object[] store = ArrayOperations.toObjectArray(array);
                        for (int i = 0; i < size; i += 4) {
                            int fd = (int) store[i];
                            String path = StringOperations.getString((DynamicObject) store[i + 1]);
                            int flags = (int) store[i + 2];
                            int perms = (int) store[i + 3];
                            actions.add(SpawnFileAction.open(path, fd, flags, perms));
                        }
                        continue;
                    }
                }
                throw new UnsupportedOperationException("Unsupported spawn option: " + key + " => " + value);
            }

            return actions;
        }

        @TruffleBoundary
        private int call(String command, String[] arguments, String[] environmentVariables, Collection<SpawnFileAction> fileActions) {
            return getContext().getNativePlatform().getPosix().posix_spawnp(
                    command,
                    fileActions,
                    Arrays.asList(arguments),
                    Arrays.asList(environmentVariables));

        }
    }

}
