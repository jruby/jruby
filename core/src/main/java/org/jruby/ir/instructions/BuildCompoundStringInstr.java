package org.jruby.ir.instructions;

import org.jcodings.Encoding;
import org.jruby.RubyString;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

// This represents a compound string in Ruby
// Ex: - "Hi " + "there"
//     - "Hi #{name}"
public class BuildCompoundStringInstr extends NOperandResultBaseInstr {
    final private Encoding encoding;
    final private boolean frozen;
    final private boolean debug;
    final private String file;
    final private int line;

    public BuildCompoundStringInstr(Variable result, Operand[] pieces, Encoding encoding, boolean frozen, boolean debug, String file, int line) {
        super(Operation.BUILD_COMPOUND_STRING, result, pieces);

        this.encoding = encoding;
        this.frozen = frozen;
        this.debug = debug;
        this.file = file;
        this.line = line;
    }

    public Operand[] getPieces() {
       return getOperands();
    }

    public Encoding getEncoding() {
        return encoding;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BuildCompoundStringInstr(ii.getRenamedVariable(result), cloneOperands(ii), encoding, frozen, debug, file, line);
    }

    public boolean isSameEncodingAndCodeRange(RubyString str, StringLiteral newStr) {
        return newStr.getByteList().getEncoding() == encoding && newStr.getCodeRange() == str.getCodeRange();
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getPieces());
        e.encode(encoding);
        e.encode(frozen);
        e.encode(file);
        e.encode(line);
    }

    public static BuildCompoundStringInstr decode(IRReaderDecoder d) {
        boolean debuggingFrozenStringLiteral = d.getCurrentScope().getManager().getInstanceConfig().isDebuggingFrozenStringLiteral();
        return new BuildCompoundStringInstr(d.decodeVariable(), d.decodeOperandArray(), d.decodeEncoding(), d.decodeBoolean(), debuggingFrozenStringLiteral, d.decodeString(), d.decodeInt());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        ByteList bytes = new ByteList();
        bytes.setEncoding(encoding);
        RubyString str = RubyString.newStringShared(context.runtime, bytes, StringSupport.CR_7BIT);
        for (Operand p : getOperands()) {
            if ((p instanceof StringLiteral) && (isSameEncodingAndCodeRange(str, (StringLiteral)p))) {
                str.getByteList().append(((StringLiteral)p).getByteList());
                str.setCodeRange(((StringLiteral)p).getCodeRange());
            } else {
                IRubyObject pval = (IRubyObject)p.retrieve(context, self, currScope, currDynScope, temp);
                str.append19(pval);
            }
        }

        if (frozen) {
            if (debug) {
                return IRRuntimeHelpers.freezeLiteralString(str, context, file, line);
            }
            return IRRuntimeHelpers.freezeLiteralString(str);
        }
        return str;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BuildCompoundStringInstr(this);
    }

    public boolean isFrozen() {
        return frozen;
    }

    public String getFile() {
        return file;
    }

    public int getLine() {
        return line;
    }
}
