package org.jruby.ir.interpreter;

import java.util.List;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.instructions.Instr;

/**
 * Created by enebo on 10/21/14.
 */
public class ScriptBodyInterpreterContext extends InterpreterContext {
    private List<IRClosure> beginBlocks;
    private List<IRClosure> endBlocks;

    public ScriptBodyInterpreterContext(IRScriptBody scope, Instr[] instrs) {
        super(scope, instrs);

        this.beginBlocks = scope.getBeginBlocks();
        this.endBlocks = scope.getEndBlocks();
    }

    public List<IRClosure> getBeginBlocks() {
        return beginBlocks;
    }

    public List<IRClosure> getEndBlocks() {
        return endBlocks;
    }
}
