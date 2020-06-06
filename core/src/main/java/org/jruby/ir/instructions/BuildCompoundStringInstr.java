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

// This represents a compound string in Ruby
// Ex: - "Hi " + "there"
//     - "Hi #{name}"
public class BuildCompoundStringInstr extends NOperandResultBaseInstr {
    final private Encoding encoding;
    final private boolean frozen;
    final private boolean debug;
    final private String file;
    final private int line;
    private final int estimatedSize;

    public BuildCompoundStringInstr(Variable result, Operand[] pieces, Encoding encoding, int estimatedSize, boolean frozen, boolean debug, String file, int line) {
        super(Operation.BUILD_COMPOUND_STRING, result, pieces);

        this.encoding = encoding;
        this.frozen = frozen;
        this.debug = debug;
        this.file = file;
        this.line = line;
        this.estimatedSize = estimatedSize;
    }

    public Operand[] getPieces() {
       return getOperands();
    }

    public Encoding getEncoding() {
        return encoding;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BuildCompoundStringInstr(ii.getRenamedVariable(result), cloneOperands(ii), encoding, estimatedSize, frozen, debug, file, line);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getPieces());
        e.encode(encoding);
        e.encode(estimatedSize);
        e.encode(frozen);
        e.encode(file);
        e.encode(line);
    }

    public static BuildCompoundStringInstr decode(IRReaderDecoder d) {
        boolean debuggingFrozenStringLiteral = d.getCurrentScope().getManager().getInstanceConfig().isDebuggingFrozenStringLiteral();
        return new BuildCompoundStringInstr(d.decodeVariable(), d.decodeOperandArray(), d.decodeEncoding(), d.decodeInt(), d.decodeBoolean(), debuggingFrozenStringLiteral, d.decodeString(), d.decodeInt());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        // use estimatedSize * 1.5 to give some initial room for interpolation
        RubyString str = RubyString.newStringLight(context.runtime, estimatedSize * 3 / 2, encoding);

        for (Operand p : getOperands()) {
            if (p instanceof StringLiteral) {
                StringLiteral strLiteral = (StringLiteral) p;
                ByteList byteList = strLiteral.getByteList();
                int cr = strLiteral.getCodeRange();
                str.cat(byteList, cr);
            } else {
                IRubyObject pval = (IRubyObject) p.retrieve(context, self, currScope, currDynScope, temp);
                str.appendAsDynamicString(pval);
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
