package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;

public class ArrayAllocationSite {

    @CompilerDirectives.CompilationFinal private boolean convertedIntToLong = false;
    private final Assumption assumption = Truffle.getRuntime().createAssumption();

    @CompilerDirectives.SlowPath
    public void convertedIntToLong() {
        convertedIntToLong = true;
        assumption.invalidate();
    }

    public boolean hasConvertedIntToLong() {
        assumption.isValid();
        return convertedIntToLong;
    }

}
