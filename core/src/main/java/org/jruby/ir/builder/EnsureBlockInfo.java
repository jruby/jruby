package org.jruby.ir.builder;

import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.ExceptionRegionEndMarkerInstr;
import org.jruby.ir.instructions.ExceptionRegionStartMarkerInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.LabelInstr;
import org.jruby.ir.instructions.RuntimeHelperCall;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.NullBlock;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

import java.util.ArrayList;
import java.util.List;

import static org.jruby.ir.instructions.RuntimeHelperCall.Methods.RESET_GVAR_UNDERSCORE;

/* -----------------------------------------------------------------------------------
 * Every ensure block has a start label and end label
 *
 * This ruby code will translate to the IR shown below
 * -----------------
 *   begin
 *       ... protected body ...
 *   ensure
 *       ... ensure block to run
 *   end
 * -----------------
 *  L_region_start
 *     IR instructions for the protected body
 *     .. copy of ensure block IR ..
 *  L_dummy_rescue:
 *     e = recv_exc
 *  L_start:
 *     .. ensure block IR ..
 *     throw e
 *  L_end:
 * -----------------
 *
 * If N is a node in the protected body that might exit this scope (exception rethrows
 * and returns), N has to first run the ensure block before exiting.
 *
 * Since we can have a nesting of ensure blocks, we are maintaining a stack of these
 * well-nested ensure blocks.  Every node N that will exit this scope will have to
 * run the stack of ensure blocks in the right order.
 * ----------------------------------------------------------------------------------- */
class EnsureBlockInfo {
    final Label start;         // Beginning of this ensure region
    final Label bodyStart;     // Beginning of any actual instrs in the ensure body (will not be emitted if none)
    final Label end;           // End of this ensure region
    final Label ensureRescue;  // Where exceptions in an ensure flow through
    Variable savedGlobalException;
    boolean needsBacktrace;

    // Label of block that will rescue exceptions raised by ensure code
    final Label    bodyRescuer;

    // Innermost loop within which this ensure block is nested, if any
    final IRLoop   innermostLoop;

    // This ensure block's instructions
    final List<Instr> instrs;

    public EnsureBlockInfo(IRScope s, IRLoop l, Label bodyRescuer, int sourceLine) {
        // this technically may be any block and not specifically rescue but for the sake of looking at the CFG
        // it is more or less a begin block with exception handling around it.
        start = s.getNewLabel("ENSURE_BEGIN_@" + sourceLine);
        bodyStart = s.getNewLabel("ENSURE_BODY_:@" + sourceLine);
        end = s.getNewLabel("ENSURE_END_:@" + sourceLine);
        ensureRescue = s.getNewLabel("ENSURE_CATCH_@" + sourceLine);
        instrs = new ArrayList<>(4);
        savedGlobalException = null;
        innermostLoop = l;
        this.bodyRescuer = bodyRescuer;
        needsBacktrace = true;
    }

    public void addInstr(Instr i) {
        instrs.add(i);
    }

    public void addInstrAtBeginning(Instr i) {
        instrs.add(0, i);
    }

    /**
     * Emit the saved instrs for the ensure body into the current location in the provided builder.
     * @param builder
     */
    public void emitEnsureBody(IRBuilder builder) {
        builder.addInstr(new LabelInstr(bodyStart));
        for (Instr i: instrs) {
            builder.addInstr(i);
        }
    }

    public void cloneIntoHostScope(IRBuilder builder) {
        // $! should be restored before the ensure block is run
        if (savedGlobalException != null) {
            // We need make sure on all outgoing paths in optimized short-hand rescues we restore the backtrace
            if (!needsBacktrace) builder.addInstr(builder.getManager().needsBacktrace(true));
            addInstr(new RuntimeHelperCall(builder.temp(), RESET_GVAR_UNDERSCORE, new Operand[] { savedGlobalException }));
        }

        // Sometimes we process a rescue and it hits something like non-local flow like a 'next' and
        // there are no actual instrs pushed yet (but ebi has reserved a frame for it -- e.g. the rescue/ensure
        // the next is in).  Since it is doing nothing we have nothing to clone.  By skipping this we prevent
        // setting exception regions and simplify CFG construction.
        if (instrs.isEmpty()) return;

        SimpleCloneInfo ii = new SimpleCloneInfo(builder.scope, true);

        // Clone required labels.
        // During normal cloning below, labels not found in the rename map
        // are not cloned.
        ii.renameLabel(bodyStart);
        for (Instr i: instrs) {
            if (i instanceof LabelInstr) ii.renameLabel(((LabelInstr)i).getLabel());
        }

        // Clone instructions now
        builder.addInstr(new LabelInstr(ii.getRenamedLabel(bodyStart)));
        builder.addInstr(new ExceptionRegionStartMarkerInstr(bodyRescuer));
        for (Instr instr: instrs) {
            Instr clonedInstr = instr.clone(ii);
            if (clonedInstr instanceof CallBase) {
                CallBase call = (CallBase)clonedInstr;
                Operand block = call.getClosureArg(NullBlock.INSTANCE);
                if (block instanceof WrappedIRClosure) builder.scope.addClosure(((WrappedIRClosure)block).getClosure());
            }
            builder.addInstr(clonedInstr);
        }
        builder.addInstr(new ExceptionRegionEndMarkerInstr());
    }
}
