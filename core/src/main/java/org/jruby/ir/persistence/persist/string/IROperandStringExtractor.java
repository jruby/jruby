package org.jruby.ir.persistence.persist.string;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jcodings.Encoding;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
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
    public void Nil(Nil nil) {}
    public void ObjectClass(ObjectClass objectclass) {}
    public void Self(Self self) {}
    public void StandardError(StandardError standarderror) {}
    public void UndefinedValue(UndefinedValue undefinedvalue) {}
    public void UnexecutableNil(UnexecutableNil unexecutablenil) {}
    
    // Operands that have arrays as parameters
    
    // If we simply pass array directly to appendParameters
    //  than it will be unwrapped
    //  we want to pass single parameters which type is Operand[]
    public void Array(Array array) {
        Operand[] elts = array.getElts();        
        
        stringProducer.appendParameters(new Object[] { elts });
    }
    
    public void BacktickString(BacktickString backtickstring) {
        List<Operand> pieces = backtickstring.pieces;
        
        stringProducer.appendParameters(new Object[] { pieces.toArray() });
    }

    public void CompoundString(CompoundString compoundstring) {
        List<Operand> pieces = compoundstring.getPieces();
        Encoding encoding = compoundstring.getEncoding();
        
        // No need to wrap pieces array here,
        // 2 parameters are passed,
        // so appendParameters is able to figure out that there are 2 parameters
        stringProducer.appendParameters(pieces.toArray(), encoding);
    }

    public void Hash(Hash hash) {
        List<Operand[]> keyValuePairArrays = Collections.emptyList();
        if (!hash.isBlank()) {
            List<KeyValuePair> pairs = hash.pairs;
            keyValuePairArrays = new ArrayList<Operand[]>(pairs.size());
            for (KeyValuePair keyValuePair : pairs) {
                Operand[] keyValuePairArray = { keyValuePair.getKey(), keyValuePair.getValue() };
                keyValuePairArrays.add(keyValuePairArray);
            }
        }        
        stringProducer.appendParameters(new Object[] { keyValuePairArrays.toArray() });
    }

    // Operands that takes another operands as parameters    
    
    public void AsString(AsString asstring) {
        Operand source = asstring.getSource();
        
        stringProducer.appendParameters(source);
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

    public void Range(Range range) {
        Operand begin = range.getBegin();
        Operand end = range.getEnd();
        boolean exclusive = range.isExclusive();
        
        stringProducer.appendParameters(begin, end, exclusive);
    }

    public void Regexp(Regexp regexp) {
        Operand regexpOperand = regexp.getRegexp();
        RegexpOptions options = regexp.options;
        KCode kCode = options.getKCode();
        boolean kcodeDefault = options.isKcodeDefault();
        
        stringProducer.appendParameters(regexpOperand, kCode, kcodeDefault);
    }

    public void Splat(Splat splat) {
        Operand array = splat.getArray();
        
        stringProducer.appendParameters(array);
    }

    public void SValue(SValue svalue) {
        Operand array = svalue.getArray();
        
        stringProducer.appendParameters(array);
    }
    
    // Operands that takes IRScope as parameter
    //  actually, all we need to persist is name of scope, by IRPersisterHelper will deal with this
    public void CurrentScope(CurrentScope currentscope) {
        IRScope scope = currentscope.getScope();
        
        stringProducer.appendParameters(scope);
    }

    public void ScopeModule(ScopeModule scopemodule) {
        IRScope scope = scopemodule.getScope();
        
        stringProducer.appendParameters(scope);
    }

    public void WrappedIRClosure(WrappedIRClosure wrappedirclosure) {
        IRClosure closure = wrappedirclosure.getClosure();
        
        stringProducer.appendParameters(closure);
    }
    
    // Parameters that takes string(or char) as parameters
    public void Backref(Backref backref) {
        char type = backref.type;
        
        stringProducer.appendParameters(type);
    }

    public void StringLiteral(StringLiteral stringliteral) {
        String string = stringliteral.string;
        
        stringProducer.appendParameters(string);
    }

    public void Symbol(Symbol symbol) {
        String name = symbol.getName();
        
        stringProducer.appendParameters(name);
    }

    public void GlobalVariable(GlobalVariable globalvariable) {        
        String name = globalvariable.getName();
        
        stringProducer.appendParameters(name);
    }

    public void IRException(IRException irexception) {
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

    public void Label(Label label) {
        String labelValue = label.label;
        
        stringProducer.appendParameters(labelValue);
    }

    public void MethAddr(MethAddr methaddr) {
        String name = methaddr.getName();
        
        stringProducer.appendParameters(name);
    }

    // Operands that takes java objects from standard library(or primitive types) as parameters
    //  exception for string types 
    public void Bignum(Bignum bignum) {
        BigInteger value = bignum.value;
        
        stringProducer.appendParameters(value);
    }

    public void BooleanLiteral(BooleanLiteral booleanliteral) {
        boolean bool = booleanliteral.isTrue();
        
        stringProducer.appendParameters(bool);
    }

    public void ClosureLocalVariable(ClosureLocalVariable closurelocalvariable) {
        commonForLocalVariables(closurelocalvariable);
    }

    public void LocalVariable(LocalVariable localvariable) {
        commonForLocalVariables(localvariable);
    }
    
    private void commonForLocalVariables(LocalVariable localVariable) {
        String name = localVariable.getName();
        int scopeDepth = localVariable.getScopeDepth();
        
        stringProducer.appendParameters(name, scopeDepth);
    }

    public void Fixnum(Fixnum fixnum) {
        Long value = fixnum.value;
        
        stringProducer.appendParameters(value);
    }

    public void Float(org.jruby.ir.operands.Float flote) {
        Double value = flote.value;
        
        stringProducer.appendParameters(value);
    }   

    public void NthRef(NthRef nthref) {
        int matchNumber = nthref.matchNumber;
        
        stringProducer.appendParameters(matchNumber);
    }

    public void TemporaryVariable(TemporaryVariable temporaryvariable) {
        commonForTemproraryVariable(temporaryvariable);
    }

    public void TemporaryClosureVariable(TemporaryClosureVariable temporaryclosurevariable) {
        commonForTemproraryVariable(temporaryclosurevariable);
    }

    private void commonForTemproraryVariable(TemporaryVariable temporaryVariable) {
        String name = temporaryVariable.getName();
        
        stringProducer.appendParameters(name);
    }

}

