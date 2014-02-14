package org.jruby.ir.persistence;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.jruby.RubyInstanceConfig;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRManager;

import static org.jruby.ir.operands.UnexecutableNil.U_NIL;

import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.AsString;
import org.jruby.ir.operands.Backref;
import org.jruby.ir.operands.BacktickString;
import org.jruby.ir.operands.Bignum;
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
import org.jruby.ir.operands.TemporaryCurrentModuleVariable;
import org.jruby.ir.operands.TemporaryCurrentScopeVariable;
import org.jruby.ir.operands.TemporaryFixnumVariable;
import org.jruby.ir.operands.TemporaryFloatVariable;
import org.jruby.ir.operands.TemporaryLocalVariable;
import org.jruby.ir.operands.TemporaryVariableType;
import org.jruby.ir.operands.UnboxedBoolean;
import org.jruby.ir.operands.UnboxedFixnum;
import org.jruby.ir.operands.UnboxedFloat;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.persistence.read.parser.NonIRObjectFactory;
import org.jruby.util.RegexpOptions;

/**
 *
 */
class OperandDecoderMap {
    private final IRReaderDecoder d;
    private final IRManager manager;

    public OperandDecoderMap(IRManager manager, IRReaderDecoder decoder) {
        this.manager = manager;
        this.d = decoder;
    }

    public Operand decode(OperandType type) {
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("Decoding operand " + type);

        switch (type) {
            case ARRAY: return new Array(d.decodeOperandList());
            case AS_STRING: return new AsString(d.decodeOperand());
            case BACKREF: return new Backref(d.decodeChar());
            case BACKTICK_STRING: return new BacktickString(d.decodeOperandList());
            case BIGNUM: return new Bignum(new BigInteger(d.decodeString()));
            case BOOLEAN: return new UnboxedBoolean(d.decodeBoolean());
            case COMPOUND_ARRAY: return new CompoundArray(d.decodeOperand(), d.decodeOperand(), d.decodeBoolean());
            case COMPOUND_STRING: return decodeCompoundString();
            case CURRENT_SCOPE: return new CurrentScope(d.decodeScope());
            case DYNAMIC_SYMBOL: return new DynamicSymbol((CompoundString) d.decodeOperand());
            case FIXNUM: return new Fixnum(d.decodeLong());
            case FLOAT: return new org.jruby.ir.operands.Float(d.decodeDouble());
            case GLOBAL_VARIABLE: return new GlobalVariable(d.decodeString());
            case HASH: return decodeHash();
            case IR_EXCEPTION: return IRException.getExceptionFromOrdinal(d.decodeByte());
            case LABEL: return decodeLabel();
            case LOCAL_VARIABLE: return d.getCurrentScope().getLocalVariable(d.decodeString(), d.decodeInt());
            case METHOD_HANDLE: return new MethodHandle(d.decodeOperand(), d.decodeOperand());
            case METH_ADDR: return new MethAddr(d.decodeString());
            case NIL: return manager.getNil();
            case NTH_REF: return new NthRef(d.decodeInt());
            case OBJECT_CLASS: return new ObjectClass();
            case RANGE: return new Range(d.decodeOperand(), d.decodeOperand(), d.decodeBoolean());
            case REGEXP: return decodeRegexp();
            case SCOPE_MODULE: return new ScopeModule(d.decodeScope());
            case SELF: return Self.SELF;
            case SPLAT: return new Splat(d.decodeOperand());
            case STANDARD_ERROR: return new StandardError();
            case STRING_LITERAL: return new StringLiteral(d.decodeString());
            case SVALUE: return new SValue(d.decodeOperand());
            case SYMBOL: return new Symbol(d.decodeString());
            case TEMPORARY_VARIABLE: return decodeTemporaryVariable();
            case UNBOXED_BOOLEAN: return new UnboxedBoolean(d.decodeBoolean());
            case UNBOXED_FIXNUM: return new UnboxedFixnum(d.decodeLong());
            case UNBOXED_FLOAT: return new UnboxedFloat(d.decodeDouble());
            case UNDEFINED_VALUE: return UndefinedValue.UNDEFINED;
            case UNEXECUTABLE_NIL: return U_NIL;
            case WRAPPED_IR_CLOSURE: return new WrappedIRClosure(d.decodeVariable(), (IRClosure) d.decodeScope());
        }

        return null;
    }

    private Operand decodeCompoundString() {
        String encodingString = d.decodeString();

        if (encodingString.equals("")) return new CompoundString(d.decodeOperandList());

        // FIXME: yuck
        return new CompoundString(d.decodeOperandList(), NonIRObjectFactory.createEncoding(encodingString));
    }

    private Operand decodeHash() {
        int size = d.decodeInt();
        List<KeyValuePair> pairs = new ArrayList(size);

        for (int i = 0; i < size; i++) {
            pairs.add(new KeyValuePair(d.decodeOperand(), d.decodeOperand()));
        }

        return new Hash(pairs);
    }

    private Operand decodeLabel() {
        String prefix = d.decodeString();
        int id = d.decodeInt();

        // Special case of label
        if ("_GLOBAL_ENSURE_BLOCK".equals(prefix)) return new Label("_GLOBAL_ENSURE_BLOCK", 0);

        // Check if this label was already created
        // Important! Program would not be interpreted correctly
        // if new name will be created every time
        String fullLabel = prefix + "_" + id;
        if (d.getVars().containsKey(fullLabel)) {
            return d.getVars().get(fullLabel);
        }

        Label newLabel = new Label(prefix, id);

        // Add to context for future reuse
        d.getVars().put(fullLabel, newLabel);

        return newLabel;
    }

    private Regexp decodeRegexp() {
        Operand regex = d.decodeOperand();
        boolean isNone = d.decodeBoolean();
        RegexpOptions options = RegexpOptions.fromEmbeddedOptions(d.decodeInt());
        options.setEncodingNone(isNone);
        return new Regexp(regex, options);
    }

    private Operand decodeTemporaryVariable() {
        TemporaryVariableType type = d.decodeTemporaryVariableType();

        switch(type) {
            case CLOSURE:
                return new TemporaryClosureVariable(d.decodeInt(), d.decodeInt());
            case CURRENT_MODULE:
                return new TemporaryCurrentModuleVariable(d.decodeInt());
            case CURRENT_SCOPE:
                return new TemporaryCurrentScopeVariable(d.decodeInt());
            case FLOAT:
                return new TemporaryFloatVariable(d.decodeInt());
            case FIXNUM:
                return new TemporaryFixnumVariable(d.decodeInt());
            case LOCAL:
                return new TemporaryLocalVariable(d.decodeInt());
        }

        return null;
    }
}
