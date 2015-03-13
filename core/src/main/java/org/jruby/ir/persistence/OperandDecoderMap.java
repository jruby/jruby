package org.jruby.ir.persistence;

import org.jruby.RubyInstanceConfig;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRManager;
import org.jruby.ir.operands.*;
import org.jruby.util.KeyValuePair;

import java.util.ArrayList;
import java.util.List;

import static org.jruby.ir.operands.UnexecutableNil.U_NIL;

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
            case ARRAY: return Array.decode(d);
            case AS_STRING: return AsString.decode(d);
            case BACKREF: return Backref.decode(d);
            case BIGNUM: return Bignum.decode(d);
            case BOOLEAN: return UnboxedBoolean.decode(d);
            case CURRENT_SCOPE: return CurrentScope.decode(d);
            case DYNAMIC_SYMBOL: return DynamicSymbol.decode(d);
            case FIXNUM: return Fixnum.decode(d);
            case FLOAT: return new org.jruby.ir.operands.Float(d.decodeDouble());
            case FROZEN_STRING: return FrozenString.decode(d);
            case GLOBAL_VARIABLE: return new GlobalVariable(d.decodeString());
            case HASH: return decodeHash();
            case IR_EXCEPTION: return IRException.getExceptionFromOrdinal(d.decodeByte());
            case LABEL: return decodeLabel();
            case LOCAL_VARIABLE: return d.getCurrentScope().getLocalVariable(d.decodeString(), d.decodeInt());
            case NIL: return manager.getNil();
            case NTH_REF: return NthRef.decode(d);
            case NULL_BLOCK: return NullBlock.decode(d);
            case OBJECT_CLASS: return new ObjectClass();
            case REGEXP: return Regexp.decode(d);
            case SCOPE_MODULE: return new ScopeModule(d.decodeInt());
            case SELF: return Self.SELF;
            case SPLAT: return new Splat(d.decodeOperand());
            case STANDARD_ERROR: return new StandardError();
            case STRING_LITERAL: return StringLiteral.decode(d);
            case SVALUE: return new SValue(d.decodeOperand());
            case SYMBOL: return Symbol.decode(d);
            case TEMPORARY_VARIABLE: return TemporaryLocalVariable.decode(d);
            case UNBOXED_BOOLEAN: return new UnboxedBoolean(d.decodeBoolean());
            case UNBOXED_FIXNUM: return new UnboxedFixnum(d.decodeLong());
            case UNBOXED_FLOAT: return new UnboxedFloat(d.decodeDouble());
            case UNDEFINED_VALUE: return UndefinedValue.UNDEFINED;
            case UNEXECUTABLE_NIL: return U_NIL;
            case WRAPPED_IR_CLOSURE: return new WrappedIRClosure(d.decodeVariable(), (IRClosure) d.decodeScope());
        }

        return null;
    }

    private Operand decodeHash() {
        int size = d.decodeInt();
        List<KeyValuePair<Operand, Operand>> pairs = new ArrayList<KeyValuePair<Operand, Operand>>(size);

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
}
