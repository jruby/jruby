package org.jruby.ir.persistence.flat;

import com.google.flatbuffers.FlatBufferBuilder;
import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.ast.RootNode;
import org.jruby.ir.IRBuilder;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.OperandType;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FlatIRWriter {
    public static int createIRScopeFlat(FlatBufferBuilder builder, IRScope scope) {
        int nameOffset = builder.createString(scope.getName());

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

            if (instr.getOperation().getFlat() == -1) throw new RuntimeException("unsupported operation: " + instr.getOperation());

            Operand[] operands = instr.getOperands();
            int[] operandOffsets = new int[operands.length];

            for (int j = 0; j < operands.length; j++) {
                Operand operand = operands[j];

                if (operand.getOperandType().getFlat() == -1) throw new RuntimeException("unsupported operand: " + operand.getOperandType());

                operandOffsets[j] = OperandFlat.createOperandFlat(builder, operand.getOperandType().getFlat());
            }

            int operandsOffset = InstrFlat.createOperandsVector(builder, operandOffsets);

            instrOffsets[i] = InstrFlat.createInstrFlat(builder, instr.getOperation().getFlat(), operandsOffset);
        }

        int instrsOffset = IRScopeFlat.createInstrsVector(builder, instrOffsets);

        return IRScopeFlat.createIRScopeFlat(builder, nameOffset, closuresOffset, childrenOffset, instrsOffset);
    }

    public static void main(String[] args) {
        Ruby runtime = Ruby.newInstance();
        byte[] src = "puts 'hello'".getBytes();
        ParseResult result = runtime.parseFromMain("blah.rb", new ByteArrayInputStream(src));
        InterpreterContext ic = IRBuilder.buildRoot(runtime.getIRManager(), (RootNode) result);
        IRScope scope = ic.getScope();
        FlatBufferBuilder builder = new FlatBufferBuilder();
        int index = createIRScopeFlat(builder, scope);

        builder.finish(index);

        ByteBuffer buffer = builder.dataBuffer();
        int size = buffer.limit();

        try {
            FileOutputStream out = new FileOutputStream("blah.ir");
            out.getChannel().write(buffer);
            out.close();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        try {
            RandomAccessFile file = new RandomAccessFile("blah.ir", "rw");
            buffer = file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, size);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        System.out.println("persisted and loaded:");
        IRScopeFlat scopeFlat = IRScopeFlat.getRootAsIRScopeFlat(buffer);
        System.out.println("IRScope: " + scopeFlat.name());
        System.out.println("Instructions:");
        InstrFlat instrFlat = new InstrFlat();
        for (int i = 0; i < scopeFlat.instrsLength(); i++) {
            scopeFlat.instrs(instrFlat, i);
            System.out.println("  " + Operation.flatMap(instrFlat.operation()));
            OperandFlat operandFlat = new OperandFlat();
            for (int j = 0; j < instrFlat.operandsLength(); j++) {
                instrFlat.operands(operandFlat, j);
                System.out.println("    " + OperandType.flatMap(operandFlat.operandType()));
            }
        }
    }
}
