package org.jruby.ir.persistence.parser;

import java.util.ArrayList;
import java.util.List;

import org.jcodings.Encoding;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScopeType;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.LabelInstr;
import org.jruby.ir.instructions.PutInstr;
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
import org.jruby.ir.operands.Float;
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
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.UnexecutableNil;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.persistence.parser.dummy.DummyInstrFactory;
import org.jruby.ir.persistence.parser.dummy.MultipleParamInstr;
import org.jruby.ir.persistence.parser.dummy.SingleParamInstr;
import org.jruby.parser.IRStaticScope;
import org.jruby.parser.IRStaticScopeFactory;
import org.jruby.parser.IRStaticScopeType;
import org.jruby.parser.StaticScope;
import org.jruby.util.RegexpOptions;

import beaver.Symbol;


public class PersistedIRParserLogic {
    
    private static final IRScopeBuilder SCOPE_BUILDER = IRScopeBuilder.INSTANCE;
    private final IRInstructionFactory instrFactory;
    private static final IROperandFactory OPERAND_FACTORY = IROperandFactory.INSTANCE;
    
    private static final NonIRObjectFactory NON_IR_OBJECT_FACTORY = NonIRObjectFactory.INSTANCE;
    private static final DummyInstrFactory DUMMY_INSTR_FACTORY = DummyInstrFactory.INSTANCE;
    
    private final IRParsingContext context;

    PersistedIRParserLogic(IRParsingContext context) {
        this.context = context;
        instrFactory = new IRInstructionFactory(context);
    }
    
    Symbol getToplevelScope() {
        IRScope scope = context.getToplevelScope();
        return new Symbol(scope);
    }
    
    Symbol createScope(IRScopeType type, IRScope lexicalParent, String name, String lineNumberString, IRStaticScope staticScope) {
        IRManager manager = context.getIRManager();
        IRScope irScope = SCOPE_BUILDER.createScope(type, lexicalParent, name, lineNumberString, staticScope, manager);
        context.addToScopes(irScope);
        return new Symbol(irScope);
    }
    
    Symbol createScopeType(String type) {
        IRScopeType scopeType = NON_IR_OBJECT_FACTORY.createScopeType(type);
        return new Symbol(scopeType);
    }
    
    Symbol findLexicalParent(String name) {
        IRScope parent = context.getScopeByName(name);
        context.setCurrentScope(parent);
        return new Symbol(parent);
    }
    
    Symbol buildStaticScope(IRStaticScopeType type, String[] names) {
        // Use a side effect form 'findLexicalParent'
        IRScope currentScope = context.getCurrentScope();
        StaticScope parent = null;
        if(currentScope != null) {
            parent = currentScope.getStaticScope();
        }
        IRStaticScope staticScope = IRStaticScopeFactory.newStaticScope(parent, type, names);
        return new Symbol(staticScope);
    }
    
    Symbol createStaticScopeType(String name) {
        IRStaticScopeType staticScopeType = NON_IR_OBJECT_FACTORY.createStaticScopeType(name);
        return new Symbol(staticScopeType);
    }
    
    Symbol addToScope(IRScope scope, List<Instr> instrs) {
        IRScope irScope = SCOPE_BUILDER.addToScope(scope, instrs);
        return new Symbol(irScope);
    }
    
    Symbol enterScope(String name) {
        IRScope scope = context.getScopeByName(name);
        context.setCurrentScope(scope);
        return new Symbol(scope);
    }
    
    Symbol addFirstInstruction(Instr i) {
        List<Object> lst = new ArrayList<Object>();
        lst.add(i);
        return new Symbol(lst);
    }
    
    Symbol addFollowingInstrctions(List<Instr> lst, Instr i, Symbol _symbol_lst) {
        lst.add(i);
        return _symbol_lst;
    }
    
    Symbol createLabelInstr(String string) {
        Label label = context.getLabel(string);
        if(label == null) {
            label = OPERAND_FACTORY.createLabel(string);
            context.addLabel(label);
        }
        LabelInstr labelInstr = instrFactory.createLabel(label);
        return new Symbol(labelInstr);
    }
    
    Symbol createInstrWithoutParams(String operationName) {
        Instr instr = instrFactory.createInstrWithoutParams(operationName);
        return new Symbol(instr);
    }
    
    Symbol createJump(Label target) {
        JumpInstr jump = instrFactory.createJump(target);
        return new Symbol(jump);
    }
    
    Symbol createInstrWithSingleParam(SingleParamInstr dummy) {
        Instr instr = instrFactory.createInstrWithSingleParam(dummy);
        return new Symbol(instr);
    }
    
    Symbol createInstrWithMultipleParams(MultipleParamInstr dummy) {
        Instr instr = instrFactory.createInstrWithMultipleParams(dummy);
        return new Symbol(instr);
    }
    
    Symbol createCopy(Variable result, Object param) {
        CopyInstr copyInstr = instrFactory.createCopy(result, param);
        return new Symbol(copyInstr);
    }
    
    Symbol createReturnInstrWithNoParams(Variable result, String operationName) {
        Instr instr = instrFactory.createReturnInstrWithNoParams(result, operationName);
        return new Symbol(instr);
    }
    
    Symbol createReturnInstrWithSingleParam(Variable result, SingleParamInstr dummy) {
        Instr instr = instrFactory.createReturnInstrWithSingleParam(result, dummy);
        return new Symbol(instr);
    }
    
    Symbol createReturnInstrWithMultipleParams(Variable result, MultipleParamInstr dummy) {
        Instr instr = instrFactory.createReturnInstrWithMultipleParams(result, dummy);
        return new Symbol(instr);
    }
    
    Symbol createPutInstr(SingleParamInstr dummy, Operand value) {
        PutInstr putInstr = instrFactory.createPutInstr(dummy, value);
        return new Symbol(putInstr);
    }
    
    Symbol createPutInstr(MultipleParamInstr dummy, Operand value) {
        PutInstr putInstr = instrFactory.createPutInstr(dummy, value);
        return new Symbol(putInstr);
    }
    
    Symbol createSingleParamInstr(String name, Object param) {
        SingleParamInstr dummy = DUMMY_INSTR_FACTORY.createSingleParamInstr(name, param);
        return new Symbol(dummy);
    }
    
    Symbol createMultipleParamInstr(String name, List<Object> params) {
        MultipleParamInstr dummy = DUMMY_INSTR_FACTORY.createMultipleParamInstr(name, params);
        return new Symbol(dummy);
    }
    
    Symbol addFirstParam(Object p1, Object p2) {
        ArrayList<Object> lst = new ArrayList<Object>();
        lst.add(p1);
        lst.add(p2);
        return new Symbol(lst);
    }
    
    Symbol addFollowingParam(List<Object> list, Object p, Symbol _symbol_list) {
        list.add(p);
        return _symbol_list;
    }
    
    Symbol createArray(List<Operand> operands) {
        Array array = OPERAND_FACTORY.createArray(operands);
        return new Symbol(array);
    }
    
    Symbol createAsString(Operand source) {
        AsString asString = OPERAND_FACTORY.createAsString(source);
        return new Symbol(asString);
    }
    
    Symbol createBacktickString(List<Operand> pieces) {
        BacktickString backtickString = OPERAND_FACTORY.createBacktickString(pieces);
        return new Symbol(backtickString);
    }
    
    Symbol createArgsPush(Operand a1, Operand a2) {
        CompoundArray argsPush = OPERAND_FACTORY.createArgsPush(a1, a2);
        return new Symbol(argsPush);
    }
    
    Symbol createArgsCat(Operand a1, Operand a2) {
        CompoundArray argsCat = OPERAND_FACTORY.createArgsCat(a1, a2);
        return new Symbol(argsCat);
    }
    
    Symbol createCompoundString(Encoding encoding, List<Operand> pieces) {
        CompoundString compoundString = OPERAND_FACTORY.createCompoundString(encoding, pieces);
        return new Symbol(compoundString);
    }
    
    Symbol createEncoding(String name) {
        Encoding encoding = NON_IR_OBJECT_FACTORY.createEncoding(name);
        return new Symbol(encoding);
    }
    
    Symbol createCurrentScope(String name) {
        // We dont care about name
        IRScope scope = context.getCurrentScope();
        CurrentScope currentScope = OPERAND_FACTORY.createCurrentScope(scope);
        return new Symbol(currentScope);
    }
    
    Symbol createDynamicSymbol(CompoundString compoundString) {
        DynamicSymbol dynamicSymbol = OPERAND_FACTORY.createDynamicSymbol(compoundString);
        return new Symbol(dynamicSymbol);
    }
    
    Symbol createHash(KeyValuePair[] pairs) {
        Hash hash = OPERAND_FACTORY.createHash(pairs);
        return new Symbol(hash);
    }
    
    Symbol createKeyValuePair(Operand key, Operand value) {
        KeyValuePair keyValuePair = OPERAND_FACTORY.createKeyValuePair(key, value);
        return new Symbol(keyValuePair);
    }
    
    Symbol createBignum(String bignumString) {
        Bignum bignum = OPERAND_FACTORY.createBignum(bignumString);
        return new Symbol(bignum);
    }
    
    Symbol createFixnum(String valueString) {
        Fixnum fixnum = OPERAND_FACTORY.createFixnum(valueString);
        return new Symbol(fixnum);
    }
    
    Symbol createFloat(String valueString) {
        Float floatInstance = OPERAND_FACTORY.createFloat(valueString);
        return new Symbol(floatInstance);
    }
    
    Symbol createNil() {
        Nil nil = OPERAND_FACTORY.createNil();
        return new Symbol(nil);
    }
    
    Symbol createUnexecutableNil() {
        UnexecutableNil unexecutableNil = OPERAND_FACTORY.createUnexecutableNil();
        return new Symbol(unexecutableNil);
    }
    
    Symbol createTrueLiteral() {
        BooleanLiteral trueLiteral = OPERAND_FACTORY.createTrueLiteral();
        return new Symbol(trueLiteral);
    }
    
    Symbol createFalseLiteral() {
        BooleanLiteral falseLiteral = OPERAND_FACTORY.createFalseLiteral();
        return new Symbol(falseLiteral);
    }
    
    Symbol createIRException(String type) {
        IRException irException = NON_IR_OBJECT_FACTORY.createIRException(type);
        return new Symbol(irException);
    }
    
    Symbol createLabel(String labelValue) {
        Label label = context.getLabel(labelValue);
        if(label == null) {
            if(labelValue.contains("LBL")) {
            label = context.getCurrentScope().getNewLabel();
            } else {
                label = OPERAND_FACTORY.createLabel(labelValue);
            }
            context.addLabel(label);
        }
        return new Symbol(label);
    }
    
    Symbol createMethodHandle(Operand methodName, Operand receiver) {
        MethodHandle methodHandle = OPERAND_FACTORY.createMethodHandle(methodName, receiver);
        return new Symbol(methodHandle);
    }
    
    Symbol createObjectClass() {
        ObjectClass objectClass = OPERAND_FACTORY.createObjectClass();
        return new Symbol(objectClass);
    }
    
    Symbol createRange(Operand begin, Operand end, boolean isExclusive) {
        Range range = OPERAND_FACTORY.createRange(begin, end, isExclusive);
        return new Symbol(range);
    }
    
    Symbol createBackref(String name) {
        Backref backref = OPERAND_FACTORY.createBackref(name);
        return new Symbol(backref);
    }
    
    Symbol createGlobalVariable(String name) {
        GlobalVariable globalVariable = OPERAND_FACTORY.createGlobalVariable(name);
        return new Symbol(globalVariable);
    }
    
    Symbol createMethAddr(String name) {
        MethAddr methAddr = OPERAND_FACTORY.createMethAddr(name);
        return new Symbol(methAddr);
    }
    
    Symbol createUnknownSuperTarget() {
        MethAddr unknownSuperTarget = OPERAND_FACTORY.createUnknownSuperTarget();
        return new Symbol(unknownSuperTarget);
    }
    
    Symbol createNthRef(String matchNumberString) {
        NthRef nthRef = OPERAND_FACTORY.createNthRef(matchNumberString);
        return new Symbol(nthRef);
    }
    
    Symbol createSymbol(String name) {
        org.jruby.ir.operands.Symbol symbol = OPERAND_FACTORY.createSymbol(name);
        return new Symbol(symbol);
    }
    
    Symbol createRegexp(Operand regexp, RegexpOptions options) {
        Regexp createdRegexp = OPERAND_FACTORY.createRegexp(regexp, options);
        return new Symbol(createdRegexp);
    }
    
    Symbol createScopeModule(String name) {
        IRScope currentScope = context.getCurrentScope();
        ScopeModule scopeModule = OPERAND_FACTORY.createScopeModule(currentScope);
        return new Symbol(scopeModule);
    }
    
    Symbol createRegexpOptions(String kcodeString, String[] options) {
        RegexpOptions regexpOptions = NON_IR_OBJECT_FACTORY.createRegexpOptions(kcodeString, options);
        return new Symbol(regexpOptions);
    }
    
    Symbol createSplat(Operand array) {
        Splat splat = OPERAND_FACTORY.createSplat(array);
        return new Symbol(splat);
    }
    
    Symbol createStandardError() {
        StandardError standardError = OPERAND_FACTORY.createStandardError();
        return new Symbol(standardError);
    }
    
    Symbol createStringLiteral(String s) {
        StringLiteral stringLiteral = OPERAND_FACTORY.createStringLiteral(s);
        return new Symbol(stringLiteral);
    }
    
    Symbol createSValue(Operand array) {
        SValue sValue = OPERAND_FACTORY.createSValue(array);
        return new Symbol(sValue);
    }
    
    Symbol createUndefininedValue() {
        UndefinedValue undefininedValue = OPERAND_FACTORY.createUndefininedValue();
        return new Symbol(undefininedValue);
    }
    
    Symbol createSelf() {
        Self self = OPERAND_FACTORY.createSelf();
        return new Symbol(self);
    }
    
    Symbol createLocalVariable(String name, String scopeDepthString, String locationString) {
        IRScope currentScope = context.getCurrentScope();
        LocalVariable localVariable = OPERAND_FACTORY.createLocalVariable(name, scopeDepthString, currentScope);
        return new Symbol(localVariable);
    }
    
    Symbol createClosureLocalVariable(String name, String scopeDepthString, String locationString) {
        IRClosure scope = (IRClosure) context.getCurrentScope();
        ClosureLocalVariable closureLocalVariable = OPERAND_FACTORY.createClosureLocalVariable(scope, name, scopeDepthString, locationString);
        return new Symbol(closureLocalVariable);
    }
    
    Symbol createTemporaryVariable(String name) {
        name = "%" + name;
        IRScope currentScope = context.getCurrentScope();
        TemporaryVariable temporaryVariable = (TemporaryVariable) context.getVariablesByName(name);
        if(temporaryVariable == null) {
            temporaryVariable = OPERAND_FACTORY.createTemporaryVariable(currentScope, name);
            context.addVariable(temporaryVariable);
        }
        return new Symbol(temporaryVariable);
    }
    
    Symbol createWrapperIRClosure(String name) {
        IRClosure closure = (IRClosure) context.getScopeByName(name);
        WrappedIRClosure wrappedIRClosure = new WrappedIRClosure(closure);
        return new Symbol(wrappedIRClosure);
    }
}
