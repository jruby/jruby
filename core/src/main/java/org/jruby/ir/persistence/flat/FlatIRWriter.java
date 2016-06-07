package org.jruby.ir.persistence.flat;

import com.google.flatbuffers.FlatBufferBuilder;
import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ast.RootNode;
import org.jruby.ir.IRBuilder;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.LineNumberInstr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.operands.FrozenString;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Self;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.TemporaryLocalVariable;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FlatIRWriter {
    public static int createIRScopeFlat(FlatBufferBuilder builder, IRScope scope) {
        int scopeNameOffset = builder.createString(scope.getName());

        int[] children = new int[scope.getLexicalScopes().size()];

        for (int i = 0; i < scope.getLexicalScopes().size(); i++) {
            IRScope child = scope.getLexicalScopes().get(i);

            children[i] = createIRScopeFlat(builder, child);
        }

        int childrenOffset = IRScopeFlat.createLexicalChildrenVector(builder, children);

        int closuresOffset = IRScopeFlat.createNestedClosuresVector(builder, new int[0]);

        Instr[] instrs = scope.getInterpreterContext().getInstructions();
        int[] instrOffsets = new int[instrs.length];

        for (int i = 0; i < instrs.length; i++) {
            Instr instr = instrs[i];

            int instrType = instr.getOperation().getFlat();

            if (instrType == -1) throw new RuntimeException("unsupported operation: " + instr.getOperation());

            Operand[] operands = instr.getOperands();
            int[] operandOffsets = new int[operands.length];

            for (int j = 0; j < operands.length; j++) {
                Operand operand = operands[j];

                int operandType = operand.getOperandType().getFlat();

                if (operandType == -1) throw new RuntimeException("unsupported operand: " + operand.getOperandType());

                int operandOffset = -1;

                switch (operandType) {
                    case OperandUnion.CurrentScopeFlat:
                        operandOffset = CurrentScopeFlat.createCurrentScopeFlat(builder, (byte) -1);
                        break;
                    case OperandUnion.ScopeModuleFlat:
                        operandOffset = ScopeModuleFlat.createScopeModuleFlat(builder, (byte) -1);
                        break;
                    case OperandUnion.SelfFlat:
                        operandOffset = SelfFlat.createSelfFlat(builder, (byte) -1);
                        break;
                    case OperandUnion.StringLiteralFlat: {
                        int frozenStringOffset = createFrozenString(builder, ((StringLiteral) operand).frozenString);
                        operandOffset = StringLiteralFlat.createStringLiteralFlat(builder, frozenStringOffset);
                        break;
                    }
                    case OperandUnion.FrozenStringFlat:
                        operandOffset = createFrozenString(builder, ((StringLiteral) operand).frozenString);
                        break;
                    case OperandUnion.TemporaryVariableFlat:
                        operandOffset = TemporaryVariableFlat.createTemporaryVariableFlat(builder, ((TemporaryLocalVariable) operand).offset);
                        break;
                    default:
                        throw new RuntimeException("unsupported operand: " + operand.getOperandType());
                }
                operandOffsets[j] = OperandFlat.createOperandFlat(builder, (byte) operandType, operandOffset);
            }

            int operandsOffset = InstrFlat.createOperandsVector(builder, operandOffsets);

            int instrOffset = -1;

            switch (instrType) {
                case InstrUnion.CallFlat: {
                    CallInstr callInstr = (CallInstr) instr;
                    boolean[] splatMap = callInstr.splatMap();
                    if (splatMap == null) splatMap = new boolean[0];
                    int splatMapOffset = CallFlat.createSplatMapVector(builder, splatMap);
                    int callNameOffset = builder.createString(callInstr.getName());
                    instrOffset = CallFlat.createCallFlat(
                            builder,
                            callInstr.getCallType().ordinal(),
                            callNameOffset,
                            callInstr.getArgsCount(),
                            callInstr.hasLiteralClosure(),
                            callInstr.canBeEval(),
                            callInstr.targetRequiresCallersBinding(),
                            callInstr.targetRequiresCallersFrame(),
                            splatMapOffset,
                            callInstr.isPotentiallyRefined());
                    break;
                }
                case InstrUnion.CopyFlat:
                    instrOffset = CopyFlat.createCopyFlat(builder, (byte) -1);
                    break;
                case InstrUnion.LineNumberFlat:
                    instrOffset = LineNumberFlat.createLineNumberFlat(builder, ((LineNumberInstr) instr).lineNumber);
                    break;
                case InstrUnion.LoadFrameClosureFlat:
                    instrOffset = LoadFrameClosureFlat.createLoadFrameClosureFlat(builder, (byte) -1);
                    break;
                case InstrUnion.LoadImplicitClosureFlat:
                    instrOffset = LoadImplicitClosureFlat.createLoadImplicitClosureFlat(builder, (byte) -1);
                    break;
                case InstrUnion.ReceiveSelfFlat:
                    instrOffset = ReceiveSelfFlat.createReceiveSelfFlat(builder, (byte) -1);
                    break;
                case InstrUnion.ReturnFlat:
                    instrOffset = ReturnFlat.createReturnFlat(builder, (byte) -1);
                    break;
                default:
                    throw new RuntimeException("unsupported operation: " + instr.getOperation());
            }

            int resultOffset = -1;
            if (instr instanceof ResultInstr) {
                Variable result = ((ResultInstr) instr).getResult();

                if (result instanceof TemporaryLocalVariable) {
                    resultOffset = TemporaryVariableFlat.createTemporaryVariableFlat(builder, ((TemporaryLocalVariable) result).offset);
                } else if (result instanceof Self) {
                    // dummy for now until all Variable types are represented
                    resultOffset = TemporaryVariableFlat.createTemporaryVariableFlat(builder, -1);
                } else {
                    throw new RuntimeException("unsupported result type: " + result.getClass());
                }
            } else {
                resultOffset = TemporaryVariableFlat.createTemporaryVariableFlat(builder, -1);
            }

            instrOffsets[i] = InstrFlat.createInstrFlat(builder, resultOffset, operandsOffset, (byte) instrType, instrOffset);
        }

        int instrsOffset = IRScopeFlat.createInstrsVector(builder, instrOffsets);

        return IRScopeFlat.createIRScopeFlat(builder, scopeNameOffset, closuresOffset, childrenOffset, instrsOffset, (short) scope.getTemporaryVariablesCount(), scope.receivesKeywordArgs());
    }

    private static int createFrozenString(FlatBufferBuilder builder, FrozenString frozenString) {
        int bytesOffset = FrozenStringFlat.createBytesVector(builder, frozenString.bytelist.bytes());
        int encodingOffset = builder.createString(frozenString.bytelist.getEncoding().toString());
        int strOffset = builder.createString(frozenString.string);
        int fileOffset = builder.createString(frozenString.file);
        return FrozenStringFlat.createFrozenStringFlat(builder, bytesOffset, encodingOffset, strOffset, frozenString.coderange, fileOffset, frozenString.line);
    }
}
