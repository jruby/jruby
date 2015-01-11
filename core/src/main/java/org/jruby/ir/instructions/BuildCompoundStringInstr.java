package org.jruby.ir.instructions;

import org.jcodings.Encoding;
import org.jruby.RubyString;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// This represents a compound string in Ruby
// Ex: - "Hi " + "there"
//     - "Hi #{name}"
public class BuildCompoundStringInstr extends Instr implements ResultInstr {
    final private Encoding encoding;

    public BuildCompoundStringInstr(Variable result, Operand[] pieces, Encoding encoding) {
        super(Operation.BUILD_COMPOUND_STRING, result, pieces);

        this.encoding = encoding;
    }

    public Operand[] getPieces() {
       return getOperands();
    }

    public Encoding getEncoding() {
        return encoding;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BuildCompoundStringInstr(ii.getRenamedVariable(result), cloneOperands(ii), encoding);
    }

    public boolean isSameEncodingAndCodeRange(RubyString str, StringLiteral newStr) {
        return newStr.bytelist.getEncoding() == encoding && newStr.getCodeRange() == str.getCodeRange();
    }

    public RubyString[] retrievePieces(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        int length = operands.length;
        RubyString[] strings = new RubyString[length];
        for (int i = 0; i < length; i++) {
            strings[i] = (RubyString) operands[i].retrieve(context, self, currScope, currDynScope, temp);
        }
        return strings;
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        ByteList bytes = new ByteList();
        bytes.setEncoding(encoding);
        RubyString str = RubyString.newStringShared(context.runtime, bytes, StringSupport.CR_7BIT);
        for (Operand p : operands) {
            if ((p instanceof StringLiteral) && (isSameEncodingAndCodeRange(str, (StringLiteral)p))) {
                str.getByteList().append(((StringLiteral)p).bytelist);
                str.setCodeRange(str.scanForCodeRange());
            } else {
                IRubyObject pval = (IRubyObject)p.retrieve(context, self, currScope, currDynScope, temp);
                str.append19(pval);
            }
        }

        return str;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BuildCompoundStringInstr(this);
    }
}
