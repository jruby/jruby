package org.jruby.ir.persistence.read.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jruby.ir.IRScope;
import org.jruby.ir.IRScopeType;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.read.parser.dummy.DummyInstrFactory;
import org.jruby.ir.persistence.read.parser.dummy.InstrWithParams;

import beaver.Symbol;


public class PersistedIRParserLogic {
    
    private final IRScopeFactory scopeBuilder;
    private final IRInstructionFactory instrFactory;
    private final IROperandFactory operandFactory;
    
    private static final NonIRObjectFactory NON_IR_OBJECT_FACTORY = NonIRObjectFactory.INSTANCE;
    private static final DummyInstrFactory DUMMY_INSTR_FACTORY = DummyInstrFactory.INSTANCE;
    
    private final IRParsingContext context;

    PersistedIRParserLogic(IRParsingContext context) {
        this.context = context;
        
        scopeBuilder = new IRScopeFactory(context);
        instrFactory = new IRInstructionFactory(context);
        operandFactory = new IROperandFactory(context);
    }
    
    Symbol getToplevelScope() {
        final IRScope scope = context.getToplevelScope();
        
        return new Symbol(scope);
    }
    
    Symbol createScope(String typeString, List<Object> parameters) {
        final IRScopeType type = NON_IR_OBJECT_FACTORY.createScopeType(typeString);
        final IRScope irScope = scopeBuilder.createScope(type, parameters);
        
        context.addToScopes(irScope);
        
        return new Symbol(irScope);
    }
    
    Symbol addToScope(IRScope scope, List<Instr> instrs) {
        // we need to iterate throw scopes
        // because IRScope#addInst has obvious side effect and for that reason
        // so we can't just use IRScope#getInsts and assign it to instrs 
        for (Instr instr : instrs) {
            scope.addInstr(instr);
        }
        
        return new Symbol(scope);
    }
    
    Symbol enterScope(String name) {
        final IRScope scope = context.getScopeByName(name);
        context.setCurrentScope(scope);
        return new Symbol(scope);
    }
    
    Symbol addFirstInstruction(Instr i) {
        final List<Object> lst = new ArrayList<Object>();
        lst.add(i);
        return new Symbol(lst);
    }
    
    Symbol addFollowingInstructions(List<Instr> lst, Instr i, Symbol _symbol_lst) {
        lst.add(i);
        return _symbol_lst;
    }
    
    Symbol markAsDeadIfNeeded(Symbol instrSymbol, Object marker) {
        if(marker != null) {
            Instr currentInstr =  (Instr) instrSymbol.value;
            currentInstr.markDead();
        }
        return instrSymbol;
    }
    
    Symbol createInstrWithoutParams(String operationName) {
        Instr instr = instrFactory.createInstrWithoutParams(operationName);
        return new Symbol(instr);
    }
    
    Symbol createInstrWithParams(InstrWithParams dummy) {
        Instr instr = instrFactory.createInstrWithParams(dummy);
        return new Symbol(instr);
    }
    
    Symbol markHasUnusedResultIfNeeded(Symbol instrSymbol, Object marker) {
        if(marker != null) {
            Instr currentInstr =  (Instr) instrSymbol.value;
            currentInstr.markUnusedResult();
        }
        return instrSymbol;
    }
    
    Symbol createReturnInstrWithNoParams(Operand result, String operationName) {
        Instr instr = instrFactory.createReturnInstrWithNoParams((Variable) result, operationName);
        return new Symbol(instr);
    }
    
    Symbol createReturnInstrWithParams(Operand result, InstrWithParams dummy) {
        Instr instr = instrFactory.createReturnInstrWithParams((Variable) result, dummy);
        return new Symbol(instr);
    }
    
    Symbol createInstrWithParams(String name, List<Object> params) {
        InstrWithParams dummy = DUMMY_INSTR_FACTORY.createInstrWithParam(name, params);
        return new Symbol(dummy);
    }
    
    Symbol createNull() {
        return new Symbol(null);
    }
    
    Symbol createList(List<Object> params) {        
        if(params == null) {
            params = Collections.emptyList();
        }
        return new Symbol(params);
    }
    
    Symbol createOperandWithoutParameters(String operandName) {
        Operand operand = operandFactory.createOperandWithoutParameters(operandName);
        
        return new Symbol(operand);
    }
    
    Symbol createOperandWithParameters(String operandName, List<Object> params) {
        Operand operand = operandFactory.createOperandWithParameters(operandName, params);
        
        return new Symbol(operand);
    }
}

