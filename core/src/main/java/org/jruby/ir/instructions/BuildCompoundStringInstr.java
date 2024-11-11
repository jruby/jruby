package org.jruby.ir.instructions;

import org.jcodings.Encoding;
import org.jruby.Appendable;
import org.jruby.RubyString;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.builder.StringStyle;
import org.jruby.ir.operands.ChilledString;
import org.jruby.ir.operands.FrozenString;
import org.jruby.ir.operands.ImmutableLiteral;
import org.jruby.ir.operands.MutableString;
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

import java.util.ArrayList;
import java.util.List;

import static org.jruby.api.Create.newString;
import static org.jruby.ir.builder.StringStyle.Chilled;
import static org.jruby.ir.builder.StringStyle.Frozen;
import static org.jruby.util.StringSupport.*;

// This represents a compound string in Ruby
// Ex: - "Hi " + "there"
//     - "Hi #{name}"
public class BuildCompoundStringInstr extends NOperandResultBaseInstr {
    final private Encoding encoding;
    final private StringStyle stringStyle;
    final private String file;
    final private int line;
    private final int estimatedSize;

    public BuildCompoundStringInstr(Variable result, Operand[] pieces, Encoding encoding, int estimatedSize, StringStyle stringStyle, String file, int line) {
        super(Operation.BUILD_COMPOUND_STRING, result, pieces);

        this.encoding = encoding;
        this.stringStyle = stringStyle;
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

    public int getInitialSize() {
        return estimatedSize * 3 / 2;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BuildCompoundStringInstr(ii.getRenamedVariable(result), cloneOperands(ii), encoding, estimatedSize, stringStyle, file, line);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getPieces());
        e.encode(encoding);
        e.encode(estimatedSize);
        e.encode(stringStyle == Frozen);
        e.encode(stringStyle == StringStyle.Mutable);
        e.encode(file);
        e.encode(line);
    }

    public static BuildCompoundStringInstr decode(IRReaderDecoder d) {
        return new BuildCompoundStringInstr(d.decodeVariable(), d.decodeOperandArray(), d.decodeEncoding(),
                d.decodeInt(), decodeStringStyle(d.decodeBoolean(), d.decodeBoolean()), d.decodeString(), d.decodeInt());
    }

    private static StringStyle decodeStringStyle(boolean frozen, boolean mutable) {
        if (frozen) return Frozen;
        if (mutable) return StringStyle.Mutable;
        return Chilled;
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        // use estimatedSize * 1.5 to give some initial room for interpolation
        RubyString str = RubyString.newStringLight(context.runtime, getInitialSize(), encoding);

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

        return switch (stringStyle) {
            case Frozen -> IRRuntimeHelpers.freezeLiteralString(str);
            case Chilled -> IRRuntimeHelpers.chillLiteralString(str);
            default -> str;
        };
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BuildCompoundStringInstr(this);
    }

    public boolean isFrozen() {
        return stringStyle == Frozen;
    }

    public boolean isChilled() {
        return stringStyle == Chilled;
    }

    public String getFile() {
        return file;
    }

    public int getLine() {
        return line;
    }


    /**
     * This will make an attempt to combine multiple pieces when they are contiguout frozen strings OR
     * immutable literals.  It may fail as not all strings can be concatenated together (e.g. mismatched
     * encodings).
     *
     * @param manager the IR manager
     * @return the simplified instruction or itself when it cannot be simplified.
     */
    @Override
    public Instr simplifyInstr(IRManager manager) {
        var piecesArray = getOperands();

        if (piecesArray.length == 0) { // not sure we can have an empty compound string but better safe than sorry.
            var newByteList = new ByteList(0);
            newByteList.setEncoding(encoding);
            Operand string = switch (stringStyle) {
                case Frozen -> new FrozenString(newByteList, CR_VALID, file, line);
                case Mutable -> new MutableString(newByteList, CR_VALID, file, line);
                default -> new ChilledString(newByteList, CR_VALID, file, line);
            };
            return new CopyInstr(getResult(), string);
        } else if (piecesArray.length == 1) { // not sure we can have a compound string with only one piece AND a non-string.
            return piecesArray[0] instanceof FrozenString froz ? copy(froz) : this;
        }

        ThreadContext context = manager.getRuntime().getCurrentContext();
        int[] i = new int[] { 0 };
        var pieces = new ArrayList<Operand>(piecesArray.length);
        FrozenString lastString = findNextFrozenString(context, piecesArray, pieces, i);

        if (lastString == null) return this; // nothing to be combined
        i[0]++;

        for (; i[0] < piecesArray.length; i[0]++) {
            Operand piece = piecesArray[i[0]];

            // We found something we can add onto current frozen string.
            if (piece instanceof ImmutableLiteral imm) {
                try {
                    FrozenString newOperand = combine(context, lastString, imm);
                    if (newOperand != null) {
                        lastString = newOperand;
                        continue;
                    }
                } catch (Exception e) {
                    // This is an error from trying to combine two incompatible strings together.
                    // As this has to end up as a runtime error vs a syntax error we need to just
                    // pretend this instr is not optimizable and let the runtime stumble over it
                    // if it is ever executed.
                    return this;
                }
            }

            // We did find something.  save last string + piece and get new last string.
            pieces.add(lastString);
            pieces.add(piece);
            i[0]++;
            lastString = findNextFrozenString(context, piecesArray, pieces, i);
            if (lastString == null) break;
        }

        if (lastString != null) pieces.add(lastString);

        if (pieces.size() != piecesArray.length) {
            return pieces.size() == 1 ?
                    copy((FrozenString) pieces.get(0)) :
                    new BuildCompoundStringInstr(result, pieces.toArray(Operand[]::new), encoding, estimatedSize, stringStyle, file, line);
        }

        return this;
    }

    private FrozenString findNextFrozenString(ThreadContext context, Operand[] piecesArray, List<Operand> pieces, int[] i) {
        for (; i[0] < piecesArray.length; i[0]++) {
            Operand piece = piecesArray[i[0]];

            if (piece instanceof ImmutableLiteral imm) return asFrozenString(context, imm);

            pieces.add(piece);
        }

        return null;
    }

    private FrozenString combine(ThreadContext context, FrozenString lastString, ImmutableLiteral piece) {
        if (lastString == null) return asFrozenString(context, piece);

        IRubyObject obj = (IRubyObject) piece.retrieve(context, null, null, null, null);
        if (obj instanceof Appendable app) {
            // Construct a new String vs retrieve since the retrieve would be a frozen string.
            RubyString last = newString(context, lastString.getByteList().dup());
            app.appendIntoString(last);
            ByteList newByteList = last.getByteList();
            return asOperand(newByteList);
        }

        return null;
    }

    private Instr copy(FrozenString string) {
        Operand value = switch (stringStyle) {
            case Frozen -> string;
            case Mutable -> new MutableString(string.bytelist, CR_VALID, file, line);
            default -> new ChilledString(string.bytelist, CR_VALID, file, line);
        };

        return new CopyInstr(result, value);
    }

    private FrozenString asFrozenString(ThreadContext context, ImmutableLiteral piece) {
        if (piece instanceof FrozenString str) return str;
        IRubyObject fix = (IRubyObject) piece.retrieve(context, null, null, null, null);
        return asOperand(fix.asString().getByteList());
    }

    private FrozenString asOperand(ByteList bytelist) {
        return new FrozenString(bytelist, CR_UNKNOWN, file, line);
    }
}
