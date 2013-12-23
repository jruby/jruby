package org.jruby.ir.persistence;

import java.util.List;
import org.jcodings.Encoding;
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
import org.jruby.util.RegexpOptions;

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
        encoder.encode((byte) operand.getOperandType().ordinal());
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
    
    @Override public void BacktickString(BacktickString backtickstring) {
        List<Operand> operands = backtickstring.pieces;
        
        encoder.encode(operands.size());
        
        for (Operand operand: operands) {
            encode(operand);
        }
    }
    @Override public void Bignum(Bignum bignum) { encoder.encode(bignum.value.toString()); }
    
    @Override public void BooleanLiteral(BooleanLiteral booleanliteral) { encoder.encode(booleanliteral.isTrue()); }
    
    @Override public void ClosureLocalVariable(ClosureLocalVariable variable) {
        // We can refigure out closure scope it is in.
        encoder.encode(variable.getName());
        encoder.encode(variable.getScopeDepth());        
    }
    
    @Override public void CompoundArray(CompoundArray compoundarray) { 
        encode(compoundarray.getA1());
        encode(compoundarray.getA2());
        encoder.encode(compoundarray.isArgsPush());
    }
    
    @Override public void CompoundString(CompoundString compoundstring) {
        Encoding encoding = compoundstring.getEncoding();
        
        if (encoding == null) {
            encoder.encode("");
        } else {
            encoder.encode(encoding.toString());
        }
        List<Operand> pieces = compoundstring.getPieces();
        encoder.encode(pieces.size());
        
        for (Operand piece: pieces) {
            encode(piece);
        }
    }
    
    // FIXME: Can't I determine this instead of persisting it?
    @Override public void CurrentScope(CurrentScope scope) { encoder.encode(scope.getScope()); }
    
    //@Override public void DynamicSymbol(DynamicSymbol dsym) { encode(dsym.getSymbolName()); }
    @Override public void DynamicSymbol(DynamicSymbol dsym) {  }
    
    @Override public void Fixnum(Fixnum fixnum) { encoder.encode(fixnum.value); }
    
    @Override public void Float(org.jruby.ir.operands.Float flote) { encoder.encode(flote.value); }
    
    @Override public void GlobalVariable(GlobalVariable variable) { encoder.encode(variable.getName()); }
    
    @Override public void Hash(Hash hash) {
        encoder.encode(hash.pairs.size());
        for (KeyValuePair pair: hash.pairs) {
            encoder.encode(pair.getKey());
            encoder.encode(pair.getValue());
        }
    }
    
    @Override public void IRException(IRException irexception) { encoder.encode((byte) irexception.getType().ordinal()); }
    
    @Override public void Label(Label label) { encoder.encode(label.label); }
    
    @Override public void LocalVariable(LocalVariable variable) {
        encoder.encode(variable.getName());
        encoder.encode(variable.getScopeDepth());
        encoder.encode(variable.getOffset());
    }
    
    @Override public void MethAddr(MethAddr methaddr) { encoder.encode(methaddr.getName()); }
    
    @Override public void MethodHandle(MethodHandle methodhandle) {
        encoder.encode(methodhandle.getReceiver());
        encoder.encode(methodhandle.getMethodNameOperand());
    }
    
    @Override public void Nil(Nil nil) {} // No data
    
    @Override public void NthRef(NthRef nthref) { encoder.encode(nthref.matchNumber); }
    
    @Override public void ObjectClass(ObjectClass objectclass) {} // No data
    
    @Override public void Range(Range range) {
        encoder.encode(range.getBegin());
        encoder.encode(range.getEnd());
        encoder.encode(range.isExclusive());
    }
    
    @Override public void Regexp(Regexp regexp) {
        RegexpOptions options = regexp.options;
        
        encode(regexp.getRegexp());
        encoder.encode((byte) options.getKCode().ordinal());
        encoder.encode(options.isKcodeDefault());
    }
    
    @Override public void ScopeModule(ScopeModule scope) { encoder.encode(scope.getScope()); }
    
    @Override public void Self(Self self) {} // No data
    
    @Override public void Splat(Splat splat) { encode(splat.getArray()); }
    
    @Override public void StandardError(StandardError standarderror) {} // No data
    
    @Override public void StringLiteral(StringLiteral stringliteral) { encoder.encode(stringliteral.string); }
    
    @Override public void SValue(SValue svalue) { encode(svalue.getArray()); }
    
    @Override public void Symbol(Symbol symbol) { encoder.encode(symbol.getName()); }

    // FIXME: There are two forms of constructors for both temp vars and they are not clear what there actual
    // requirements are (prefix/offset/name question).
    @Override public void TemporaryClosureVariable(TemporaryClosureVariable variable) {
        encoder.encode(variable.getName());
        encoder.encode(variable.offset);
    }
    
    @Override public void TemporaryVariable(TemporaryVariable variable) { 
        encoder.encode(variable.getName()); 
        encoder.encode(variable.offset); 
    }
    
    @Override public void UndefinedValue(UndefinedValue undefinedvalue) {} // No data
    
    @Override public void UnexecutableNil(UnexecutableNil unexecutablenil) {} // No data
    
    @Override public void WrappedIRClosure(WrappedIRClosure scope) { encoder.encode(scope.getClosure()); }
}