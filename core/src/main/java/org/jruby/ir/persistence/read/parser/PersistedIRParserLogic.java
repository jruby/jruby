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
    
    IRScope getToplevelScope() {
        final IRScope scope = context.getToplevelScope();
        
        return scope;
    }
    
    IRScope createScope(String typeString, List<Object> parameters) {
        final IRScopeType type = NON_IR_OBJECT_FACTORY.createScopeType(typeString);
        final IRScope irScope = scopeBuilder.createScope(type, parameters);
        
        context.addToScopes(irScope);
        
        return irScope;
    }
    
    IRScope addToScope(IRScope scope, List<Instr> instrs) {
        // we need to iterate throw scopes
        // because IRScope#addInst has obvious side effect and for that reason
        // so we can't just use IRScope#getInsts and assign it to instrs 
        for (Instr instr : instrs) {
            scope.addInstr(instr);
        }
        
        return scope;
    }
    
    IRScope enterScope(String name) {
        final IRScope scope = context.getScopeByName(name);
        context.setCurrentScope(scope);
        return scope;
    }
    
    List<Object> addFirstInstruction(Instr i) {
        final List<Object> lst = new ArrayList<Object>();
        lst.add(i);
        return lst;
    }
    
    Object addFollowingInstructions(List<Instr> lst, Instr i, Object _symbol_lst) {
        lst.add(i);
        return _symbol_lst;
    }
    
    Object markAsDeadIfNeeded(Instr instrObject, Object marker) {
        // FIXME: This was symbol in beaver picking up deadness and then getting its instr from symbol and marking it
        // I am just pretending the instr to be marked is here.
        if(marker != null) {
            instrObject.markDead();
        }
        return instrObject;
    }
    
    Instr createInstrWithoutParams(String operationName) {
        Instr instr = instrFactory.createInstrWithoutParams(operationName);
        return instr;
    }
    
    Instr createInstrWithParams(InstrWithParams dummy) {
        Instr instr = instrFactory.createInstrWithParams(dummy);
        return instr;
    }
    
    Object markHasUnusedResultIfNeeded(Instr instrObject, Object marker) {
        //FIXME: See markAsDead...same thing done here.
        if(marker != null) {
            instrObject.markUnusedResult();
        }
        return instrObject;
    }
    
    Instr createReturnInstrWithNoParams(Operand result, String operationName) {
        Instr instr = instrFactory.createReturnInstrWithNoParams((Variable) result, operationName);
        return instr;
    }
    
    Instr createReturnInstrWithParams(Operand result, InstrWithParams dummy) {
        Instr instr = instrFactory.createReturnInstrWithParams((Variable) result, dummy);
        return instr;
    }
    
    InstrWithParams createInstrWithParams(String name, List<Object> params) {
        InstrWithParams dummy = DUMMY_INSTR_FACTORY.createInstrWithParam(name, params);
        return dummy;
    }
    
    Object createNull() {
        return null;
    }
    
    List<Object> createList(List<Object> params) {        
        if(params == null) {
            params = Collections.emptyList();
        }
        return params;
    }
    
    Operand createOperandWithoutParameters(String operandName) {
        Operand operand = operandFactory.createOperandWithoutParameters(operandName);
        
        return operand;
    }
    
    Operand createOperandWithParameters(String operandName, List<Object> params) {
        Operand operand = operandFactory.createOperandWithParameters(operandName, params);
        
        return operand;
    }
}

