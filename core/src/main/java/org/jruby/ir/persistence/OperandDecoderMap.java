package org.jruby.ir.persistence;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.jcodings.Encoding;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRManager;
import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.AsString;
import org.jruby.ir.operands.Backref;
import org.jruby.ir.operands.BacktickString;
import org.jruby.ir.operands.Bignum;
import org.jruby.ir.operands.BooleanLiteral;
import org.jruby.ir.operands.CompoundArray;
import org.jruby.ir.operands.CompoundString;
import org.jruby.ir.operands.CurrentScope;
import org.jruby.ir.operands.DynamicSymbol;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.GlobalVariable;
import org.jruby.ir.operands.Hash;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.operands.KeyValuePair;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.MethodHandle;
import org.jruby.ir.operands.NthRef;
import org.jruby.ir.operands.ObjectClass;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.OperandType;
import org.jruby.ir.operands.Range;
import org.jruby.ir.operands.Regexp;
import org.jruby.ir.operands.SValue;
import org.jruby.ir.operands.ScopeModule;
import org.jruby.ir.operands.Self;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.StandardError;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Symbol;
import org.jruby.ir.operands.TemporaryClosureVariable;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.UndefinedValue;
import static org.jruby.ir.operands.UnexecutableNil.U_NIL;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.util.KCode;
import org.jruby.util.RegexpOptions;

/**
 * 
 */
class OperandDecoderMap {
    private final IRReaderDecoder decoder;
    private final IRManager manager;
    
    public OperandDecoderMap(IRManager manager, IRReaderDecoder decoder) {
        this.manager = manager;
        this.decoder = decoder;
    }
    
    public Operand decode(OperandType type) {
        System.out.println("Decoding operand " + type);

        switch (type) {
            case ARRAY: return new Array(decoder.decodeOperandList());
            case AS_STRING: return new AsString(decoder.decodeOperand());
            case BACKREF: return new Backref(decoder.decodeChar());
            case BACKTICK_STRING: return new BacktickString(decoder.decodeOperandList());
            case BIGNUM: return new Bignum(new BigInteger(decoder.decodeString()));
            case BOOLEAN_LITERAL: return new BooleanLiteral(decoder.decodeBoolean());
            case COMPOUND_ARRAY: return new CompoundArray(decoder.decodeOperand(), decoder.decodeOperand(), decoder.decodeBoolean());
            case COMPOUND_STRING: return decodeCompoundString();
            case CURRENT_SCOPE: return new CurrentScope(decoder.decodeScope());
            case DYNAMIC_SYMBOL: return new DynamicSymbol((CompoundString) decoder.decodeOperand());
            case FIXNUM: return new Fixnum(decoder.decodeLong());
            case FLOAT: return new org.jruby.ir.operands.Float(decoder.decodeDouble());
            case GLOBAL_VARIABLE: return new GlobalVariable(decoder.decodeString());
            case HASH: return decodeHash();
            case IR_EXCEPTION: return IRException.getExceptionFromOrdinal(decoder.decodeByte());
            case LABEL: return new Label(decoder.decodeString());
            case LOCAL_VARIABLE: return new LocalVariable(decoder.decodeString(), decoder.decodeInt(), decoder.decodeInt());
            case METHOD_HANDLE: return new MethodHandle(decoder.decodeOperand(), decoder.decodeOperand());
            case METH_ADDR: return new MethAddr(decoder.decodeString());
            case NIL: return manager.getNil();
            case NTH_REF: return new NthRef(decoder.decodeInt());
            case OBJECT_CLASS: return new ObjectClass();
            case RANGE: return new Range(decoder.decodeOperand(), decoder.decodeOperand(), decoder.decodeBoolean());
            case REGEXP: return decodeRegexp();
            case SCOPE_MODULE: return new ScopeModule(decoder.decodeScope());
            case SELF: return Self.SELF;
            case SPLAT: return new Splat(decoder.decodeOperand());
            case STANDARD_ERROR: return new StandardError();
            case STRING_LITERAL: return new StringLiteral(decoder.decodeString());
            case SVALUE: return new SValue(decoder.decodeOperand());
            case SYMBOL: return new Symbol(decoder.decodeString());
            case TEMPORARY_VARIABLE: return new TemporaryVariable(decoder.decodeString(), decoder.decodeInt());
            case TEMPORARY_CLOSURE_VARIABLE: return new TemporaryClosureVariable(decoder.decodeString(), decoder.decodeInt());
            case UNDEFINED_VALUE: return UndefinedValue.UNDEFINED;
            case UNEXECUTABLE_NIL: return U_NIL;
            case WRAPPED_IR_CLOSURE: return new WrappedIRClosure((Variable) decoder.decodeOperand(), (IRClosure) decoder.decodeScope());
        }
        
        return null;
    }

    private Operand decodeCompoundString() {
        String encodingString = decoder.decodeString();
        
        if (encodingString.equals("")) return new CompoundString(decoder.decodeOperandList());
                
        return new CompoundString(decoder.decodeOperandList(), Encoding.load(encodingString));
    }

    private Operand decodeHash() {
        int size = decoder.decodeInt();
        List<KeyValuePair> pairs = new ArrayList(size);
        
        for (int i = 0; i < size; i++) {
            pairs.add(new KeyValuePair(decoder.decodeOperand(), decoder.decodeOperand()));
        }
        
        return new Hash(pairs);
    }

    private Regexp decodeRegexp() {
        Operand regexp = decoder.decodeOperand();
        KCode kcode = KCode.values()[decoder.decodeByte()];
        boolean isKCodeDefault = decoder.decodeBoolean();
        
        return new Regexp(regexp, new RegexpOptions(kcode, isKCodeDefault));
    }
}
