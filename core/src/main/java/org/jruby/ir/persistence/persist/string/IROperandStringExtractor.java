package org.jruby.ir.persistence.persist.string;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.AsString;
import org.jruby.ir.operands.Backref;
import org.jruby.ir.operands.BacktickString;
import org.jruby.ir.operands.Bignum;
import org.jruby.ir.operands.BooleanLiteral;
import org.jruby.ir.operands.ClosureLocalVariable;
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
import org.jruby.ir.operands.Nil;
import org.jruby.ir.operands.NthRef;
import org.jruby.ir.operands.ObjectClass;
import org.jruby.ir.operands.Operand;
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
import org.jruby.ir.operands.TemporaryLocalVariable;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.UnexecutableNil;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.persistence.persist.string.producer.IROperandStringBuilder;
import org.jruby.util.KCode;
import org.jruby.util.RegexpOptions;

class IROperandStringExtractor extends IRVisitor {

    private final IROperandStringBuilder stringProducer;

    private IROperandStringExtractor(IROperandStringBuilder stringProducer) {
        this.stringProducer = stringProducer;
    }

    // Static factory that is used in translator
    static IROperandStringExtractor createToplevelInstance() {
        IROperandStringBuilder stringProducer = new IROperandStringBuilder(null);
        return new IROperandStringExtractor(stringProducer);
    }
    static IROperandStringExtractor createInstance(StringBuilder builder) {
        IROperandStringBuilder stringProducer = new IROperandStringBuilder(builder);
        return new IROperandStringExtractor(stringProducer);
    }

    public String extract(Operand operand) {
        produceString(operand);

        return stringProducer.getResultString();
    }

    public void produceString(Operand operand) {
        stringProducer.appendOperandType(operand);

        operand.visit(this);
    }

 // Operands

    // Operands without parameters
    @Override public void Nil(Nil nil) {}
    @Override public void ObjectClass(ObjectClass objectclass) {}
    @Override public void Self(Self self) {}
    @Override public void StandardError(StandardError standarderror) {}
    @Override public void UndefinedValue(UndefinedValue undefinedvalue) {}
    @Override public void UnexecutableNil(UnexecutableNil unexecutablenil) {}

    // Operands that have arrays as parameters

    // If we simply pass array directly to appendParameters
    //  than it will be unwrapped
    //  we want to pass single parameters which type is Operand[]
    @Override public void Array(Array array) {
        stringProducer.appendParameters(new Object[] { array.getElts() });
    }

    @Override public void BacktickString(BacktickString backtickstring) {
        stringProducer.appendParameters(new Object[] { backtickstring.pieces.toArray() });
    }

    @Override public void CompoundString(CompoundString compoundstring) {
        // No need to wrap pieces array here,
        // 2 parameters are passed,
        // so appendParameters is able to figure out that there are 2 parameters
        stringProducer.appendParameters(compoundstring.getPieces().toArray(), compoundstring.getEncoding());
    }

    @Override public void Hash(Hash hash) {
        List<Operand[]> keyValuePairArrays = Collections.emptyList();
        if (!hash.isBlank()) {
            List<KeyValuePair> pairs = hash.pairs;
            keyValuePairArrays = new ArrayList<Operand[]>(pairs.size());
            for (KeyValuePair keyValuePair : pairs) {
                keyValuePairArrays.add(new Operand[] { keyValuePair.getKey(), keyValuePair.getValue() });
            }
        }
        stringProducer.appendParameters(new Object[] { keyValuePairArrays.toArray() });
    }

    // Operands that takes another operands as parameters

    @Override public void AsString(AsString asstring) {
        stringProducer.appendParameters(asstring.getSource());
    }

    @Override public void CompoundArray(CompoundArray op) {
        stringProducer.appendParameters(op.getA1(), op.getA2(), op.isArgsPush());
    }

    @Override public void DynamicSymbol(DynamicSymbol op) {
        stringProducer.appendParameters(op.getSymbolName());
    }

    @Override public void MethodHandle(MethodHandle op) {
        stringProducer.appendParameters(op.getReceiver(), op.getMethodNameOperand());
    }

    @Override public void Range(Range range) {
        stringProducer.appendParameters(range.getBegin(), range.getEnd(), range.isExclusive());
    }

    @Override public void Regexp(Regexp regexp) {
        Operand regexpOperand = regexp.getRegexp();
        RegexpOptions options = regexp.options;
        KCode kCode = options.getKCode();
        boolean kcodeDefault = options.isKcodeDefault();

        stringProducer.appendParameters(regexpOperand, kCode, kcodeDefault);
    }

    @Override public void Splat(Splat splat) {
        stringProducer.appendParameters(splat.getArray());
    }

    @Override public void SValue(SValue svalue) {
        stringProducer.appendParameters(svalue.getArray());
    }

    // Operands that takes IRScope as parameter
    //  actually, all we need to persist is name of scope, by IRPersisterHelper will deal with this
    @Override public void CurrentScope(CurrentScope currentscope) {
        stringProducer.appendParameters(currentscope.getScope());
    }

    @Override public void ScopeModule(ScopeModule scopemodule) {
        stringProducer.appendParameters(scopemodule.getScope());
    }

    @Override public void WrappedIRClosure(WrappedIRClosure wrappedirclosure) {
        stringProducer.appendParameters(wrappedirclosure.getClosure());
    }

    // Parameters that takes string(or char) as parameters
    @Override public void Backref(Backref backref) {
        stringProducer.appendParameters(backref.type);
    }

    @Override public void StringLiteral(StringLiteral stringliteral) {
        stringProducer.appendParameters(stringliteral.string);
    }

    @Override public void Symbol(Symbol symbol) {
        stringProducer.appendParameters(symbol.getName());
    }

    @Override public void GlobalVariable(GlobalVariable variable) {
        stringProducer.appendParameters(variable.getName());
    }

    @Override public void IRException(IRException irexception) {
        String type = null;

        if (irexception == IRException.NEXT_LocalJumpError) type = "NEXT";
        else if (irexception == IRException.BREAK_LocalJumpError) type = "BREAK";
        else if (irexception == IRException.RETURN_LocalJumpError) type = "RETURN";
        else if (irexception == IRException.REDO_LocalJumpError) type = "REDO";
        else if (irexception == IRException.RETRY_LocalJumpError) type = "RETRY";
        else {/* TODO: Currently there is no other exception types, but if they will be introduced than we need to support them
                        Throw RuntimeException here?
         */
        }

        stringProducer.appendParameters(type);
    }

    @Override public void Label(Label label) {
        stringProducer.appendParameters(label.label);
    }

    @Override public void MethAddr(MethAddr methaddr) {
        stringProducer.appendParameters(methaddr.getName());
    }

    // Operands that takes java objects from standard library(or primitive types) as parameters
    //  exception for string types
    @Override public void Bignum(Bignum bignum) {
        stringProducer.appendParameters(bignum.value);
    }

    @Override public void BooleanLiteral(BooleanLiteral bool) {
        stringProducer.appendParameters(bool.isTrue());
    }

    @Override public void ClosureLocalVariable(ClosureLocalVariable variable) {
        commonForLocalVariables(variable);
    }

    @Override public void LocalVariable(LocalVariable variable) {
        commonForLocalVariables(variable);
    }

    private void commonForLocalVariables(LocalVariable variable) {
        stringProducer.appendParameters(variable.getName(), variable.getScopeDepth());
    }

    @Override public void Fixnum(Fixnum fixnum) {
        stringProducer.appendParameters(fixnum.value);
    }

    @Override public void Float(org.jruby.ir.operands.Float flote) {
        stringProducer.appendParameters(flote.value);
    }

    @Override public void NthRef(NthRef nthref) {
        stringProducer.appendParameters(nthref.matchNumber);
    }

    @Override public void TemporaryVariable(TemporaryVariable variable) {
        commonForTemproraryVariable(variable);
    }

    @Override public void TemporaryClosureVariable(TemporaryClosureVariable variable) {
        commonForTemproraryVariable(variable);
    }

    private void commonForTemproraryVariable(TemporaryVariable variable) {
        stringProducer.appendParameters(variable.getName());
    }

}

