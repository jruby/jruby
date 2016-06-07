package org.jruby.ir.persistence.flat;

import com.google.flatbuffers.FlatBufferBuilder;
import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ast.RootNode;
import org.jruby.ir.IRBuilder;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FlatIRInterpreter {

    public static void main(String[] args) {
        Ruby runtime = Ruby.newInstance();
        byte[] src;
        String outputFile;
        if (args[0].equals("-e")) {
            src = args[1].getBytes();
            outputFile = "dash_e.ir";
        } else {
            outputFile = args[0].replaceAll(".rb$", ".ir");
            try {
                FileInputStream fis = new FileInputStream(args[0]);
                src = new byte[(int)fis.getChannel().size()];
                int remaining = src.length;
                while (remaining > 0) {
                    remaining -= fis.read(src, src.length - remaining, remaining);
                }
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        ParseResult result = runtime.parseFromMain("blah.rb", new ByteArrayInputStream(src));
        InterpreterContext ic = IRBuilder.buildRoot(runtime.getIRManager(), (RootNode) result);
        IRScope scope = ic.getScope();
        FlatBufferBuilder builder = new FlatBufferBuilder();
        builder.forceDefaults(true);
        int index = FlatIRWriter.createIRScopeFlat(builder, scope);

        builder.finish(index);

        ByteBuffer buffer = builder.dataBuffer();
        int size = buffer.limit();

        try {
            FileOutputStream out = new FileOutputStream(outputFile);
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

        IRScopeFlat scopeFlat = IRScopeFlat.getRootAsIRScopeFlat(buffer);

        FlatIRInterpreter.interpretScope(runtime.getCurrentContext(), Block.NULL_BLOCK, runtime.getTopSelf(), scopeFlat, runtime.getTopSelf().getType(), "<script>", IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
    }

    public static Object interpretOperand(OperandFlat operandFlat, ThreadContext context, StaticScope currStaticScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        switch (operandFlat.operandType()) {
            case OperandUnion.CurrentScopeFlat:
                return currStaticScope;
            case OperandUnion.FrozenStringFlat: {
                FrozenStringFlat frozenStringFlat = new FrozenStringFlat();
                operandFlat.operand(frozenStringFlat);
                return createFrozenString(frozenStringFlat, context);
            }
            case OperandUnion.ScopeModuleFlat:
                return currStaticScope.getModule();
            case OperandUnion.SelfFlat:
                return self;
            case OperandUnion.StringLiteralFlat: {
                StringLiteralFlat stringLiteralFlat = new StringLiteralFlat();
                operandFlat.operand(stringLiteralFlat);
                FrozenStringFlat frozenStringFlat = stringLiteralFlat.frozenString();
                return createFrozenString(frozenStringFlat, context);
            }
            case OperandUnion.TemporaryVariableFlat: {
                TemporaryVariableFlat temporaryVariableFlat = new TemporaryVariableFlat();
                operandFlat.operand(temporaryVariableFlat);
                return temp[temporaryVariableFlat.offset()];
            }
            default:
                throw new RuntimeException("unexpected operand in map: " + operandFlat.operandType());
        }
    }

    public static Object createFrozenString(FrozenStringFlat frozenStringFlat, ThreadContext context) {
        ByteBuffer byteBuffer = frozenStringFlat.bytesAsByteBuffer();
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return context.runtime.newString(new ByteList(bytes, 0, bytes.length, context.runtime.getEncodingService().getEncodingFromString(frozenStringFlat.encoding()), false));
    }

    private static Object interpretScope(ThreadContext context, Block block, IRubyObject self,
                                         IRScopeFlat scopeFlat, RubyModule implClass,
                                         String name, IRubyObject[] args, Block blockArg) {
        Object result;

        StaticScope currScope = context.runtime.getStaticScopeFactory().getDummyScope();
        DynamicScope currDynScope = currScope.getDummyScope();

        Object[] temp = new Object[scopeFlat.tempVariables()];

        InstrFlat instrFlat = new InstrFlat();
        int instrsLength = scopeFlat.instrsLength();
        int ipc = 0;

        while (ipc < instrsLength) {
            scopeFlat.instrs(instrFlat, ipc);

            switch (instrFlat.instrType()) {
                case InstrUnion.CallFlat: {
                    CallFlat callFlat = new CallFlat();
                    instrFlat.instr(callFlat);
                    result = CallBase.interpret(instrFlat, callFlat, context, currScope, currDynScope, self, temp);
                    assignResult(instrFlat, result, temp);
                    break;
                }
                case InstrUnion.CopyFlat: {
                    assignResult(instrFlat, interpretOperand(instrFlat.operands(0), context, currScope, currDynScope, self, temp), temp);
                    break;
                }
                case InstrUnion.LineNumberFlat: {
                    LineNumberFlat lineNumberFlat = new LineNumberFlat();
                    instrFlat.instr(lineNumberFlat);
                    context.setLine(lineNumberFlat.line());
                    break;
                }
                case InstrUnion.LoadFrameClosureFlat:
                    assignResult(instrFlat, context.getFrameBlock(), temp);
                    break;
                case InstrUnion.LoadImplicitClosureFlat:
                    assignResult(instrFlat, blockArg, temp);
                    break;
                case InstrUnion.ReceiveSelfFlat:
                    break;
                case InstrUnion.ReturnFlat:
                    return interpretOperand(instrFlat.operands(0), context, currScope, currDynScope, self, temp);
                default:
                    throw new RuntimeException("unexpected instr type in map: " + instrFlat.instrType());
            }
            ipc++;
        }

        throw new RuntimeException("flatbuffer interpreter fell off the world: " + scopeFlat.name());
    }

    private static Object assignResult(InstrFlat instrFlat, Object result, Object[] temp) {
        TemporaryVariableFlat temporaryVariableFlat = instrFlat.result();
        return temp[temporaryVariableFlat.offset()] = result;
    }
}
