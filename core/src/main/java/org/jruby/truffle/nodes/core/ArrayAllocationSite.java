package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import org.jruby.util.cli.Options;

public class ArrayAllocationSite {

    @CompilerDirectives.CompilationFinal private boolean convertedIntToLong = false;
    private final Assumption assumption = Truffle.getRuntime().createAssumption();

    @CompilerDirectives.SlowPath
    public void convertedIntToLong() {
        if (Options.TRUFFLE_ARRAYS_OPTIMISTIC_LONG.load()) {
            convertedIntToLong = true;
            assumption.invalidate();
        }
    }

    public boolean hasConvertedIntToLong() {
        if (Options.TRUFFLE_ARRAYS_OPTIMISTIC_LONG.load()) {
            assumption.isValid();
            return convertedIntToLong;
        } else {
            return false;
        }
    }

}
