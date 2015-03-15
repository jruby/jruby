package org.jruby.ir.persistence;

import org.jruby.RubyInstanceConfig;
import org.jruby.ir.IRManager;
import org.jruby.ir.operands.*;

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
            case FLOAT: return org.jruby.ir.operands.Float.decode(d);
            case FROZEN_STRING: return FrozenString.decode(d);
            case GLOBAL_VARIABLE: return GlobalVariable.decode(d);
            case HASH: return Hash.decode(d);
            case IR_EXCEPTION: return IRException.decode(d);
            case LABEL: return Label.decode(d);
            case LOCAL_VARIABLE: return LocalVariable.decode(d);
            case NIL: return manager.getNil();
            case NTH_REF: return NthRef.decode(d);
            case NULL_BLOCK: return NullBlock.decode(d);
            case OBJECT_CLASS: return new ObjectClass();
            case REGEXP: return Regexp.decode(d);
            case SCOPE_MODULE: return ScopeModule.decode(d);
            case SELF: return Self.SELF;
            case SPLAT: return Splat.decode(d);
            case STANDARD_ERROR: return new StandardError();
            case STRING_LITERAL: return StringLiteral.decode(d);
            case SVALUE: return SValue.decode(d);
            case SYMBOL: return Symbol.decode(d);
            case TEMPORARY_VARIABLE: return TemporaryLocalVariable.decode(d);
            case UNBOXED_BOOLEAN: return new UnboxedBoolean(d.decodeBoolean());
            case UNBOXED_FIXNUM: return new UnboxedFixnum(d.decodeLong());
            case UNBOXED_FLOAT: return new UnboxedFloat(d.decodeDouble());
            case UNDEFINED_VALUE: return UndefinedValue.UNDEFINED;
            case UNEXECUTABLE_NIL: return U_NIL;
            case WRAPPED_IR_CLOSURE: return WrappedIRClosure.decode(d);
        }

        return null;
    }
}
