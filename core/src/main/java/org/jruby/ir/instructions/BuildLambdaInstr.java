package org.jruby.ir.instructions;

import org.jruby.RubyProc;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.*;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.ir.IRFlags;

public class BuildLambdaInstr extends OneOperandResultBaseInstr implements FixedArityInstr, ClosureAcceptingInstr {
    /** The position for the block */
    private final String file;
    private final int line;

    public BuildLambdaInstr(Variable result, Operand lambdaBody, String file, int line) {
        super(Operation.LAMBDA, result, lambdaBody);

        this.file = file;
        this.line = line;
    }

    public String getFile() {
        return file;
    }

    public int getLine() {
        return line;
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        scope.getFlags().add(IRFlags.BINDING_HAS_ESCAPED);
        return true;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BuildLambdaInstr(ii.getRenamedVariable(getResult()), getLambdaBody().cloneForInlining(ii),
                getFile(), getLine());
    }

    public Operand getLambdaBody() {
        return getOperand1();
    }

    public Operand getClosureArg() {
        return getOperand1();
    }

    public boolean hasLiteralClosure() { return getClosureArg() instanceof WrappedIRClosure; }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getLambdaBody());
        e.encode(getFile());
        e.encode(getLine());
    }

    public static BuildLambdaInstr decode(IRReaderDecoder d) {
        return new BuildLambdaInstr(d.decodeVariable(), d.decodeOperand(), d.decodeString(), d.decodeInt());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        // SSS FIXME: Copied this from ast/LambdaNode ... Is this required here as well?
        //
        // JRUBY-5686: do this before executing so first time sets cref module
        ((WrappedIRClosure) getLambdaBody()).getClosure().getStaticScope().determineModule();

        // CON: This must not be happening, because nil would never cast to Block
//        IRClosure body = getLambdaBody().getClosure();
//        Block block = (Block) (body == null ? context.runtime.getIRManager().getNil() : getLambdaBody()).retrieve(context, self, currScope, currDynScope, temp);
        Block block = (Block)getLambdaBody().retrieve(context, self, currScope, currDynScope, temp);
        // ENEBO: Now can live nil be passed as block reference?
        // SSS FIXME: Should we do the same %self retrieval as in the case of WrappedIRClosure? Or are lambdas special??
        return RubyProc.newProc(context.runtime, block, Block.Type.LAMBDA, getFile(), getLine());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BuildLambdaInstr(this);
    }
}
