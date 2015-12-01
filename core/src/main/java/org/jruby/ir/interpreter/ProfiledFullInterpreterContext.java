package org.jruby.ir.interpreter;

import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.Instr;

/**
 * Created by enebo on 9/10/15.
 */
public class ProfiledFullInterpreterContext extends FullInterpreterContext {
    static int versionCount = 1;

    // ...
    private int version = versionCount++;

    // Numer of threadPoll instrs encountered while interpreting this scope.
    private int threadPollCounts = 0;

    // Number of clock counts encountered only within this scope.
    private int clockCount = 0;

    // Number of callsites encountered in this scope
    private int callsiteCount = 0;

    public ProfiledFullInterpreterContext(IRScope scope, Instr[] instructions) {
        super(scope, instructions);
    }
}
