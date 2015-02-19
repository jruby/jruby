/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import org.jruby.util.cli.Options;

public class ArrayAllocationSite {

    public static final boolean ARRAYS_OPTIMISTIC_LONG = Options.TRUFFLE_ARRAYS_OPTIMISTIC_LONG.load();

    @CompilerDirectives.CompilationFinal private boolean convertedIntToLong = false;
    private final Assumption assumption = Truffle.getRuntime().createAssumption("array-allocation");

    @CompilerDirectives.TruffleBoundary
    public void convertedIntToLong() {
        if (ARRAYS_OPTIMISTIC_LONG) {
            convertedIntToLong = true;
            assumption.invalidate();
        }
    }

    public boolean hasConvertedIntToLong() {
        if (ARRAYS_OPTIMISTIC_LONG) {
            assumption.isValid();
            return convertedIntToLong;
        } else {
            return false;
        }
    }

}
