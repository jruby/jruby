package org.jruby.ir.persistence;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.operands.*;
import org.jruby.ir.operands.Boolean;
import org.jruby.ir.operands.Float;
import org.jruby.util.KeyValuePair;

/**
 * Can cycles develop or will IR output guarantee non-cyclical nested operands?
 */
class OperandEncoderMap extends IRVisitor {
    private final IRWriterEncoder encoder;

    public OperandEncoderMap(IRWriterEncoder encoder) {
        this.encoder = encoder;
    }
    // FIXME: Potentially some of these values should not need to have their type prefixed.
    public void encode(Operand operand) {
        encoder.encode(operand.getOperandType().getCoded());
        operand.visit(this);
    }

    @Override public void Array(Array array) {
        Operand[] elts = array.getElts();

        encoder.encode(elts.length);
        for (Operand elt: elts) {
            encode(elt);
        }
    }

    @Override public void AsString(AsString asstring) { encoder.encode(asstring.getSource()); }

    @Override public void Backref(Backref backref) { encoder.encode(backref.type); }

    @Override public void Bignum(Bignum bignum) { encoder.encode(bignum.value.toString()); }

    @Override public void Boolean(Boolean booleanliteral) { encoder.encode(booleanliteral.isTrue()); }

    @Override public void ClosureLocalVariable(ClosureLocalVariable variable) {
        // SSS FIXME: Need to dump definedLocally?
        encoder.encode(variable.getName());
        encoder.encode(variable.getScopeDepth());
    }

    @Override public void CurrentScope(CurrentScope scope) {
        encoder.encode(scope.getScopeNestingDepth());
    }

    //@Override public void DynamicSymbol(DynamicSymbol dsym) { encode(dsym.getSymbolName()); }
    @Override public void DynamicSymbol(DynamicSymbol dsym) {  }

    @Override public void Fixnum(Fixnum fixnum) { encoder.encode(fixnum.value); }

    @Override public void Float(Float flote) { encoder.encode(flote.value); }

    @Override public void FrozenString(FrozenString operand) { StringLiteral(operand); }

    @Override public void GlobalVariable(GlobalVariable variable) { encoder.encode(variable.getName()); }

    @Override public void Hash(Hash hash) {
        encoder.encode(hash.pairs.size());
        for (KeyValuePair<Operand, Operand> pair: hash.pairs) {
            encoder.encode(pair.getKey());
            encoder.encode(pair.getValue());
        }
    }

    @Override public void IRException(IRException irexception) { encoder.encode((byte) irexception.getType().ordinal()); }

    @Override public void Label(Label label) {
        encoder.encode(label.prefix);
        encoder.encode(label.id);
    }

    @Override public void LocalVariable(LocalVariable variable) {
        encoder.encode(variable.getName());
        encoder.encode(variable.getScopeDepth());
    }

    @Override public void Nil(Nil nil) {} // No data

    @Override public void NthRef(NthRef nthref) { encoder.encode(nthref.matchNumber); }

    @Override public void NullBlock(NullBlock nullblock) { } // No data

    @Override public void ObjectClass(ObjectClass objectclass) {} // No data

    @Override public void Regexp(Regexp regexp) {
        // FIXME: This is wrong
        encoder.encode(new String(regexp.getSource().bytes(), regexp.getSource().getEncoding().getCharset()));
        encoder.encode(regexp.options.isEncodingNone());
        encoder.encode(regexp.options.toEmbeddedOptions());
    }

    @Override public void ScopeModule(ScopeModule scope) { encoder.encode(scope.getScopeModuleDepth()); }

    @Override public void Self(Self self) {} // No data

    @Override public void Splat(Splat splat) { encode(splat.getArray()); }

    @Override public void StandardError(StandardError standarderror) {} // No data

    @Override public void StringLiteral(StringLiteral stringliteral) {
        encoder.encode(stringliteral.getByteList());
        encoder.encode(stringliteral.getCodeRange());
    }

    @Override public void SValue(SValue svalue) { encode(svalue.getArray()); }

    @Override public void Symbol(Symbol symbol) { encoder.encode(symbol.getName()); }

    @Override public void TemporaryBooleanVariable(TemporaryBooleanVariable variable) {
        encoder.encode((byte) variable.getType().ordinal());
        encoder.encode(variable.getOffset());
    }

    @Override public void TemporaryFixnumVariable(TemporaryFixnumVariable variable) {
        encoder.encode((byte) variable.getType().ordinal());
        encoder.encode(variable.getOffset());
    }

    @Override public void TemporaryFloatVariable(TemporaryFloatVariable variable) {
        encoder.encode((byte) variable.getType().ordinal());
        encoder.encode(variable.getOffset());
    }

    @Override public void TemporaryLocalVariable(TemporaryLocalVariable variable) {
        encoder.encode((byte) variable.getType().ordinal());

        if (variable.getType() == TemporaryVariableType.CLOSURE) {
            encoder.encode(((TemporaryClosureVariable) variable).getClosureId());
        }
        encoder.encode(variable.getOffset());
    }

    // Only for CURRENT_SCOPE and CURRENT_MODULE now which is weird
    @Override public void TemporaryVariable(TemporaryVariable variable) {
        encoder.encode((byte) variable.getType().ordinal());
        encoder.encode(((TemporaryLocalVariable) variable).getOffset());
    }

    @Override public void UnboxedBoolean(org.jruby.ir.operands.UnboxedBoolean booleanliteral) { encoder.encode(booleanliteral.isTrue()); }

    @Override public void UnboxedFixnum(UnboxedFixnum fixnum) { encoder.encode(fixnum.value); }

    @Override public void UnboxedFloat(UnboxedFloat flote) { encoder.encode(flote.value); }

    @Override public void UndefinedValue(UndefinedValue undefinedvalue) {} // No data

    @Override public void UnexecutableNil(UnexecutableNil unexecutablenil) {} // No data

    @Override public void WrappedIRClosure(WrappedIRClosure scope) {
        encoder.encode(scope.getSelf());
        encoder.encode(scope.getClosure());
    }
}
