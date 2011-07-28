package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Nil;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.Ruby;
import org.jruby.RubyNil;
import org.jruby.RubyProc;
import org.jruby.runtime.Block;
import org.jruby.runtime.Frame;
import org.jruby.runtime.builtin.IRubyObject;

/* Make explicit within a closure the implicit closure arg instruction -- this ugly hack is because block_given?
 * does not take an explicit block arg which keeps the block argument hidden from closures */
public class RecordImplicitClosureArgInstr extends OneOperandInstr {
    public RecordImplicitClosureArgInstr(Variable block) {
        super(Operation.RECORD_CLOSURE, null, block);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new RecordImplicitClosureArgInstr((Variable)argument.cloneForInlining(ii));
    }

    @Interp
    @Override
    public Label interpret(InterpreterContext interp) {
		  Object blk = getArg().retrieve(interp);
		  if (blk instanceof RubyNil) blk = Block.NULL_BLOCK;
        else if (blk instanceof RubyProc) blk = ((RubyProc)blk).getBlock();
		  // SSS FIXME: All this drama just to update the block!
		  Frame f = interp.getContext().getCurrentFrame();
		  f.updateFrame(f.getKlazz(), (IRubyObject)interp.getSelf(), f.getName(), (Block)blk, f.getJumpTarget());
        return null;
    }
}
