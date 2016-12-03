/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.language.globals;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import org.jruby.truffle.options.OptionsBuilder;
import org.jruby.truffle.options.OptionsCatalog;

public class GlobalVariableStorage {

    private static final int GLOBAL_VARIABLE_MAX_INVALIDATIONS = OptionsBuilder.readSystemProperty(OptionsCatalog.GLOBAL_VARIABLE_MAX_INVALIDATIONS);

    private final CyclicAssumption unchangedAssumption = new CyclicAssumption("global variable unchanged");
    private int changes = 0;

    // This really means @CompilationFinal for compilation and volatile in interpreter
    @CompilationFinal private volatile boolean assumeConstant = true;

    private volatile Object value;

    GlobalVariableStorage(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public Assumption getUnchangedAssumption() {
        return unchangedAssumption.getAssumption();
    }

    public boolean isAssumeConstant() {
        return assumeConstant;
    }

    public void setValueInternal(Object value) {
        this.value = value;
    }

    @TruffleBoundary
    public void updateAssumeConstant() {
        synchronized (this) {
            if (!assumeConstant) {
                // Compiled code didn't see that we do not assumeConstant anymore
                return;
            }

            if (changes <= GLOBAL_VARIABLE_MAX_INVALIDATIONS) {
                changes++;
                unchangedAssumption.invalidate();
            } else {
                unchangedAssumption.getAssumption().invalidate();
                assumeConstant = false;
            }
        }
    }

}
