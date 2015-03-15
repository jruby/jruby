/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.persistence;

import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jruby.RubyInstanceConfig;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScopeType;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.AsString;
import org.jruby.ir.operands.Backref;
import org.jruby.ir.operands.Bignum;
import org.jruby.ir.operands.CurrentScope;
import org.jruby.ir.operands.DynamicSymbol;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.FrozenString;
import org.jruby.ir.operands.GlobalVariable;
import org.jruby.ir.operands.Hash;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.NthRef;
import org.jruby.ir.operands.NullBlock;
import org.jruby.ir.operands.ObjectClass;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.OperandType;
import org.jruby.ir.operands.Regexp;
import org.jruby.ir.operands.SValue;
import org.jruby.ir.operands.ScopeModule;
import org.jruby.ir.operands.Self;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.StandardError;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Symbol;
import org.jruby.ir.operands.TemporaryLocalVariable;
import org.jruby.ir.operands.TemporaryVariableType;
import org.jruby.ir.operands.UnboxedBoolean;
import org.jruby.ir.operands.UnboxedFixnum;
import org.jruby.ir.operands.UnboxedFloat;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.UnexecutableNil;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.parser.StaticScope;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jruby.util.ByteList;

/**
 *
 * @author enebo
 */
public class IRReaderStream implements IRReaderDecoder, IRPersistenceValues {
    private final ByteBuffer buf;
    private IRManager manager;
    private final InstrDecoderMap instrDecoderMap;
    private final List<IRScope> scopes = new ArrayList<>();
    private IRScope currentScope = null; // FIXME: This is not thread-safe and more than a little gross

    public IRReaderStream(IRManager manager, InputStream stream) {
        ByteBuffer buf = readIntoBuffer(stream);
        this.manager = manager;
        this.buf = buf;
        this.instrDecoderMap = new InstrDecoderMap(this);
    }

    public IRReaderStream(IRManager manager, File file) {
        this.manager = manager;
        ByteBuffer buf = null;
        try (FileInputStream fis = new FileInputStream(file)){
            buf = readIntoBuffer(fis);
        } catch (IOException ex) {
            Logger.getLogger(IRReaderStream.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.buf = buf;
        this.instrDecoderMap = new InstrDecoderMap(this);
    }

    private ByteBuffer readIntoBuffer(InputStream stream) {
        ByteBuffer buf = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] bytes = new byte[8192];
            int r;
            while ((r = stream.read(bytes)) > 0) baos.write(bytes, 0, r);
            if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("READ IN " + baos.size() + " BYTES OF DATA FROM");
            buf = ByteBuffer.wrap(baos.toByteArray());
        } catch (IOException ex) {
            Logger.getLogger(IRReaderStream.class.getName()).log(Level.SEVERE, null, ex);
        }
        return buf;
    }

    @Override
    public ByteList decodeByteList() {
        return new ByteList(decodeByteArray(), decodeEncoding());
    }

    @Override
    public byte[] decodeByteArray() {
        int size = decodeInt();
        byte[] bytes = new byte[size];
        buf.get(bytes);
        return bytes;
    }

    @Override
    public Encoding decodeEncoding() {
        byte[] encodingName = decodeByteArray();
        return EncodingDB.getEncodings().get(encodingName).getEncoding();
    }

    @Override
    public Label decodeLabel() {
        return (Label) decodeOperand();
    }

    @Override
    public String decodeString() {
        int strLength = decodeInt();
        byte[] bytes = new byte[strLength]; // FIXME: This seems really innefficient
        buf.get(bytes);

        String newString = new String(bytes).intern();

        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("STR<" + newString + ">");

        return newString;
    }

    @Override
    public void addScope(IRScope scope) {
        scopes.add(scope);
    }

    @Override
    public IRScope getCurrentScope() {
        return currentScope;
    }

    @Override
    public String[] decodeStringArray() {
        int arrayLength = decodeInt();
        String[] array = new String[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            array[i] = decodeString();
        }
        return array;
    }

    private Map<String, Operand> vars = null;

    @Override
    public Map<String, Operand> getVars() {
        return vars;
    }

    @Override
    public List<Instr> decodeInstructionsAt(IRScope scope, int offset) {
        currentScope = scope;
        vars = new HashMap<>();
        buf.position(offset);

        int numberOfInstructions = decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("Number of Instructions: " + numberOfInstructions);
        List<Instr> instrs = new ArrayList<>(numberOfInstructions);

        for (int i = 0; i < numberOfInstructions; i++) {
            Instr decodedInstr = decodeInstr();

            if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println(">INSTR = " + decodedInstr);

            // FIXME: It would be nice to not run this and just record flag state at encode time
            decodedInstr.computeScopeFlags(scope);
            instrs.add(decodedInstr);
        }

        return instrs;
    }

    @Override
    public Instr decodeInstr() {
        return instrDecoderMap.decode(decodeOperation());
    }

    @Override
    public IRScopeType decodeIRScopeType() {
        return IRScopeType.fromOrdinal(decodeInt());
    }

    @Override
    public TemporaryVariableType decodeTemporaryVariableType() {
        return TemporaryVariableType.fromOrdinal(decodeInt());
    }

    @Override
    public StaticScope.Type decodeStaticScopeType() {
        return StaticScope.Type.fromOrdinal(decodeInt());
    }

    @Override
    public Operation decodeOperation() {
        Operation operation = Operation.fromOrdinal(decodeInt());
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("INSTR<" + operation);
        return operation;
    }

    @Override
    public Operand decodeOperand() {
        OperandType operandType = decodeOperandType();

        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("OP<" + operandType);

        Operand decodedOperand = decode(operandType);

        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println(">OP = " + decodedOperand);

        return decodedOperand;
    }

    @Override
    public Variable decodeVariable() {
        return (Variable) decodeOperand();
    }

    @Override
    public Operand[] decodeOperandArray() {
        int size = decodeInt();
        Operand[] list = new Operand[size];

        for (int i = 0; i < size; i++) {
            list[i] = decodeOperand();
        }

        return list;
    }

    @Override
    public List<Operand> decodeOperandList() {
        int size = decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("OPERAND LIST of size: " + size);
        List<Operand> list = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("OPERAND #" + i);
            list.add(decodeOperand());
        }

        return list;
    }

    @Override
    public OperandType decodeOperandType() {
        return OperandType.fromCoded(decodeByte());
    }

    @Override
    public boolean decodeBoolean() {
        byte value = buf.get();
        if (value == TRUE) return true;
        if (value == FALSE) return false;

        throw new IllegalArgumentException("Value (" + ((int) value) + ", " + (char) value + ") is not a boolean.");
    }

    @Override
    public byte decodeByte() {
        return buf.get();
    }

    @Override
    public char decodeChar() {
        return buf.getChar();
    }

    @Override
    public int decodeInt() {
        byte b = buf.get();
        return b == FULL ? buf.getInt() : (int) b;
    }

    @Override
    public int decodeIntRaw() {
        return buf.getInt();
    }

    @Override
    public long decodeLong() {
        byte b = buf.get();
        return b == FULL ? buf.getLong() : (int) b;
    }

    @Override
    public double decodeDouble() {
        return buf.getDouble();
    }

    @Override
    public float decodeFloat() {
        return buf.getFloat();
    }

    @Override
    public IRScope decodeScope() {
        return scopes.get(decodeInt());
    }

    @Override
    public void seek(int headersOffset) {
        buf.position(headersOffset);
    }

    public Operand decode(OperandType type) {
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("Decoding operand " + type);

        switch (type) {
            case ARRAY: return Array.decode(this);
            case AS_STRING: return AsString.decode(this);
            case BACKREF: return Backref.decode(this);
            case BIGNUM: return Bignum.decode(this);
            case BOOLEAN: return org.jruby.ir.operands.Boolean.decode(this);
            case CURRENT_SCOPE: return CurrentScope.decode(this);
            case DYNAMIC_SYMBOL: return DynamicSymbol.decode(this);
            case FIXNUM: return Fixnum.decode(this);
            case FLOAT: return org.jruby.ir.operands.Float.decode(this);
            case FROZEN_STRING: return FrozenString.decode(this);
            case GLOBAL_VARIABLE: return GlobalVariable.decode(this);
            case HASH: return Hash.decode(this);
            case IR_EXCEPTION: return IRException.decode(this);
            case LABEL: return Label.decode(this);
            case LOCAL_VARIABLE: return LocalVariable.decode(this);
            case NIL: return manager.getNil();
            case NTH_REF: return NthRef.decode(this);
            case NULL_BLOCK: return NullBlock.decode(this);
            case OBJECT_CLASS: return new ObjectClass();
            case REGEXP: return Regexp.decode(this);
            case SCOPE_MODULE: return ScopeModule.decode(this);
            case SELF: return Self.SELF;
            case SPLAT: return Splat.decode(this);
            case STANDARD_ERROR: return new StandardError();
            case STRING_LITERAL: return StringLiteral.decode(this);
            case SVALUE: return SValue.decode(this);
            case SYMBOL: return Symbol.decode(this);
            case TEMPORARY_VARIABLE: return TemporaryLocalVariable.decode(this);
            case UNBOXED_BOOLEAN: return new UnboxedBoolean(decodeBoolean());
            case UNBOXED_FIXNUM: return new UnboxedFixnum(decodeLong());
            case UNBOXED_FLOAT: return new UnboxedFloat(decodeDouble());
            case UNDEFINED_VALUE: return UndefinedValue.UNDEFINED;
            case UNEXECUTABLE_NIL: return UnexecutableNil.U_NIL;
            case WRAPPED_IR_CLOSURE: return WrappedIRClosure.decode(this);
        }

        return null;
    }
}
