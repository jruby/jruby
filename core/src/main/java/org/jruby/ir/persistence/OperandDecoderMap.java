/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.persistence;

import java.math.BigInteger;
import org.jruby.ir.IRManager;
import org.jruby.ir.operands.AsString;
import org.jruby.ir.operands.Backref;
import org.jruby.ir.operands.Bignum;
import org.jruby.ir.operands.BooleanLiteral;
import org.jruby.ir.operands.CompoundArray;
import org.jruby.ir.operands.CompoundString;
import org.jruby.ir.operands.DynamicSymbol;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.GlobalVariable;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.MethodHandle;
import org.jruby.ir.operands.NthRef;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.OperandType;
import org.jruby.ir.operands.Range;
import org.jruby.ir.operands.Regexp;
import org.jruby.ir.operands.SValue;
import org.jruby.ir.operands.Self;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.StandardError;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Symbol;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.UndefinedValue;
import static org.jruby.ir.operands.UnexecutableNil.U_NIL;
import org.jruby.util.KCode;
import org.jruby.util.RegexpOptions;

/**
 *
 * @author enebo
 */
class OperandDecoderMap {
    private static final OperandType[] operands = OperandType.values();
    
    private final IRReaderDecoder decoder;
    private final IRManager manager;
    
    public OperandDecoderMap(IRManager manager, IRReaderDecoder decoder) {
        this.manager = manager;
        this.decoder = decoder;
    }

    public OperandType decodeOperandType(int ordinal) {
        if (ordinal >= operands.length) throw new IllegalArgumentException("Invalid Operation Type: " + ordinal);
        
        return operands[ordinal];
    }
    
    public Operand decode(OperandType type) {
        switch (type) {
            case ARRAY: return decodeArray();
            case AS_STRING: return new AsString(decoder.decodeOperand());
            case BACKREF: return new Backref(decoder.decodeChar());
            case BACKTICK_STRING: return decodeBacktickString();
            case BIGNUM: return new Bignum(new BigInteger(decoder.decodeString()));
            case BOOLEAN_LITERAL: return decodeBooleanLiteral();
            case COMPOUND_ARRAY: return new CompoundArray(decoder.decodeOperand(), decoder.decodeOperand(), decoder.decodeBoolean());
            case COMPOUND_STRING: return decodeCompoundString();
            case CURRENT_SCOPE: return decodeCurrentScope();
            case DYNAMIC_SYMBOL: return new DynamicSymbol((CompoundString) decoder.decodeOperand());
            case FIXNUM: return new Fixnum(decoder.decodeLong());
            case FLOAT: return new org.jruby.ir.operands.Float(decoder.decodeDouble());
            case GLOBAL_VARIABLE: return new GlobalVariable(decoder.decodeString());
            case HASH: return decodeHash();
            case IR_EXCEPTION: return IRException.getExceptionFromOrdinal(decoder.decodeByte());
            case LABEL: return decodeLabel();
            case LOCAL_VARIABLE: return new LocalVariable(decoder.decodeString(), decoder.decodeInt(), decoder.decodeInt());
            case METHOD_HANDLE: return new MethodHandle(decoder.decodeOperand(), decoder.decodeOperand());
            case METH_ADDR: return new MethAddr(decoder.decodeString());
            case NIL: return manager.getNil();
            case NTH_REF: return new NthRef(decoder.decodeInt());
            case OBJECT_CLASS: return decodeObjectClass();
            case RANGE: return new Range(decoder.decodeOperand(), decoder.decodeOperand(), decoder.decodeBoolean());
            case REGEXP: return decodeRegexp();
            case SCOPE_MODULE: return decodeScopeModule();
            case SELF: return Self.SELF;
            case SPLAT: return new Splat(decoder.decodeOperand());
            case STANDARD_ERROR: return new StandardError();
            case STRING_LITERAL: return new StringLiteral(decoder.decodeString());
            case SVALUE: return new SValue(decoder.decodeOperand());
            case SYMBOL: return new Symbol(decoder.decodeString());
            case TEMPORARY_VARIABLE: return new TemporaryVariable(decoder.decodeString(), decoder.decodeInt());
            case UNDEFINED_VALUE: return UndefinedValue.UNDEFINED;
            case UNEXECUTABLE_NIL: return U_NIL;
            case WRAPPED_IR_CLOSURE: return decodeWrappedIRClosure();
        }
        
        return null;
    }

    private Operand decodeArray() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private Operand decodeBacktickString() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
   
    private Operand decodeBooleanLiteral() {
        return new BooleanLiteral(decoder.decodeBoolean());
    }

    private Operand decodeCompoundString() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private Operand decodeCurrentScope() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private Operand decodeHash() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private Operand decodeLabel() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private Operand decodeObjectClass() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private Regexp decodeRegexp() {
        Operand regexp = decoder.decodeOperand();
        KCode kcode = KCode.values()[decoder.decodeByte()];
        boolean isKCodeDefault = decoder.decodeBoolean();
        
        return new Regexp(regexp, new RegexpOptions(kcode, isKCodeDefault));
    }
    
    private Operand decodeScopeModule() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private Operand decodeWrappedIRClosure() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
