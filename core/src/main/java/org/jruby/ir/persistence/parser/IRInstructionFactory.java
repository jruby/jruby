package org.jruby.ir.persistence.parser;

import java.util.ArrayList;
import java.util.List;

import org.jruby.ir.IRManager;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.AliasInstr;
import org.jruby.ir.instructions.BEQInstr;
import org.jruby.ir.instructions.BNEInstr;
import org.jruby.ir.instructions.BlockGivenInstr;
import org.jruby.ir.instructions.BranchInstr;
import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.instructions.CheckArityInstr;
import org.jruby.ir.instructions.ConstMissingInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.DefineInstanceMethodInstr;
import org.jruby.ir.instructions.InheritanceSearchConstInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.LabelInstr;
import org.jruby.ir.instructions.LineNumberInstr;
import org.jruby.ir.instructions.PutGlobalVarInstr;
import org.jruby.ir.instructions.PutInstr;
import org.jruby.ir.instructions.ReceiveClosureInstr;
import org.jruby.ir.instructions.ReceivePreReqdArgInstr;
import org.jruby.ir.instructions.ReceiveSelfInstr;
import org.jruby.ir.instructions.ReturnInstr;
import org.jruby.ir.instructions.SearchConstInstr;
import org.jruby.ir.instructions.ThreadPollInstr;
import org.jruby.ir.instructions.defined.BackrefIsMatchDataInstr;
import org.jruby.ir.operands.BooleanLiteral;
import org.jruby.ir.operands.GlobalVariable;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.parser.dummy.MultipleParamInstr;
import org.jruby.ir.persistence.parser.dummy.SingleParamInstr;
import org.jruby.runtime.CallType;

public enum IRInstructionFactory {
    INSTANCE;    
    
    public LabelInstr createLabel(String l) {
        Label label = new Label(l);
        return new LabelInstr(label);
    }
    
    public JumpInstr createJump(Label target) {
        return new JumpInstr(target);
    }

    public ThreadPollInstr createTreadPoll() {
        return new ThreadPollInstr();
    }
    
    public Instr createInstrWithSingleParam(SingleParamInstr instr) {
        Operation operation = instr.getOperation();
        Object param = instr.getParameter();
        switch (operation) {
        case LINE_NUM:
            return createLineNum(param);
        case RETURN:
            return createReturn(param);

        default:
            throw new UnsupportedOperationException();
        }
    }
    
    private LineNumberInstr createLineNum(Object param) {
        Integer number = (Integer) param;
        IRScope currentScope = IRParsingContext.INSTANCE.getCurrentScope();
        return new LineNumberInstr(currentScope, number);
    }
    
    private ReturnInstr createReturn(Object param) {
        Operand returnValue = (Operand) param;
        // FIXME: May have method to return as well 
        return new ReturnInstr(returnValue);
    }
    
    public Instr createInstrWithMultipleParams(MultipleParamInstr instr) {
        Operation operation = instr.getOperation();
        List<Object> params = instr.getParameters();
        switch (operation) {
        case CHECK_ARITY:
            return createCheckArity(params);
        case DEF_INST_METH:
            return createDefInstMeth(params);
        case BEQ:
            return createBEQ(params);
        case B_FALSE:
            return createBFalse(params);
        case B_TRUE:
            return createBTrue(params);
        case B_NIL:
            return createBNil(params);
        case B_UNDEF:
            return createBUndef(params);
        case BNE:
            return createBNE(params);

        default:
            throw new UnsupportedOperationException(operation.toString());
        }
    }
    
    private CheckArityInstr createCheckArity(List<Object> params) {
        int required = (Integer) params.get(0);
        int opt = (Integer) params.get(1);
        int rest = (Integer) params.get(2);
        
        return new CheckArityInstr(required, opt, rest);
    }

    private DefineInstanceMethodInstr createDefInstMeth(List<Object> params) {
        Operand container = (Operand) params.get(0);
        Label nameLable = (Label) params.get(1);
        BooleanLiteral isInstanceMethodLiteral = (BooleanLiteral) params.get(2);
        Integer lineNumber = (Integer) params.get(3);
        
        IRManager manager = IRParsingContext.INSTANCE.getIRManager();
        // FIXME: Or there may be other scopes rather than current?
        IRScope currentScope = IRParsingContext.INSTANCE.getCurrentScope(); 
        // FIXME: No closure support so far
        IRMethod method = new IRMethod(manager, currentScope, nameLable.label, isInstanceMethodLiteral.isTrue(), lineNumber, null);
        
        return new DefineInstanceMethodInstr(container, method);
    }
    
    private BranchInstr createBEQ(List<Object> params) {
        Operand arg1 = (Operand) params.get(0);
        Operand arg2 = (Operand) params.get(1);
        Label target = (Label) params.get(2);
        
        return BEQInstr.create(arg1, arg2, target);
    }

    private BranchInstr createBFalse(List<Object> params) {
        Operand arg1 = (Operand) params.get(0);
        Operand arg2 = IRParsingContext.INSTANCE.getIRManager().getFalse();
        Label target = (Label) params.get(1);
        
        return BEQInstr.create(arg1, arg2, target);
    }

    private BranchInstr createBTrue(List<Object> params) {
        Operand arg1 = (Operand) params.get(0);
        Operand arg2 = IRParsingContext.INSTANCE.getIRManager().getTrue();
        Label target = (Label) params.get(1);
        
        return BEQInstr.create(arg1, arg2, target);
    }
    
    private BranchInstr createBNil(List<Object> params) {
        Operand arg1 = (Operand) params.get(0);
        Operand arg2 = IRParsingContext.INSTANCE.getIRManager().getNil();
        Label target = (Label) params.get(1);
        
        return BEQInstr.create(arg1, arg2, target);
    }

    private BranchInstr createBUndef(List<Object> params) {
        Operand arg1 = (Operand) params.get(0);
        Operand arg2 = UndefinedValue.UNDEFINED;
        Label target = (Label) params.get(1);
        
        return BEQInstr.create(arg1, arg2, target);
    }
    
    private BranchInstr createBNE(List<Object> params) {
        Operand arg1 = (Operand) params.get(0);
        Operand arg2 = (Operand) params.get(1);
        Label target = (Label) params.get(2);
        
        return BNEInstr.create(arg1, arg2, target);
    }
    
    public Instr createReturnInstrWithNoParams(Variable result, String operationName) {
        Operation operation = NonIRObjectFactory.INSTANCE.createOperation(operationName);
        switch (operation) {
        case BLOCK_GIVEN:
            return createBlockGiven(result);
        case BACKREF_IS_MATCH_DATA:
            return createBackrefIsMatchData(result);
        case RECV_SELF:
            return createReceiveSelf(result);
        case RECV_CLOSURE:
            return createReceiveClosure(result);

        default:
            throw new UnsupportedOperationException();
        }
    }
    
    private BlockGivenInstr createBlockGiven(Variable result) {
        return new BlockGivenInstr(result);
    }

    private BackrefIsMatchDataInstr createBackrefIsMatchData(Variable result) {
        return new BackrefIsMatchDataInstr(result);
    }
    
    private ReceiveSelfInstr createReceiveSelf(Variable result) {
        return new ReceiveSelfInstr(result);
    }
    
    private ReceiveClosureInstr createReceiveClosure(Variable result) {
        return new ReceiveClosureInstr(result);
    }
    
    public Instr createReturnInstrWithSingleParam(Variable result, SingleParamInstr instr) {
        Operation operation = instr.getOperation();
        Object param = instr.getParameter();
        switch (operation) {
        case COPY:
            return createCopy(result, param);
        case RECV_PRE_REQD_ARG:
            return createRecvPreReqdArg(result, param);

        default:
            throw new UnsupportedOperationException();
        }
    }
    
    public CopyInstr createCopy(Variable result, Object param) {
        return new CopyInstr(result, (Operand) param);
    }
    
    private ReceivePreReqdArgInstr createRecvPreReqdArg(Variable result, Object param) {
        Integer argIndex = (Integer) param;
        return new ReceivePreReqdArgInstr(result, argIndex);
    }

    public Instr createReturnInstrWithMultipleParams(Variable result, MultipleParamInstr instr) {
        Operation operation = instr.getOperation();
        List<Object> params = instr.getParameters();
        switch (operation) {
        case ALIAS:
            return createAlias(result, params);
        case CALL:
            return createCall(result, params);
        case CONST_MISSING:
            return createConstMissing(result, params);
        case INHERITANCE_SEARCH_CONST:
            return createInheritanceSearchConstInstr(result, params);
        case SEARCH_CONST:
            return createSearchConst(result, params);

        default:
            throw new UnsupportedOperationException();
        }
    }

    private AliasInstr createAlias(Variable receiver, List<Object> params) {
        Operand newName = (Operand) params.get(0);
        Operand oldName = (Operand) params.get(1);
        
        return new AliasInstr(receiver, newName, oldName);
    }
    
    @SuppressWarnings("unchecked")
    private CallInstr createCall(Variable result, List<Object> params) {
        // FIXME: Persisted as label so far
        Label callTypeLabel = (Label) params.get(0);
        CallType callType = CallType.valueOf(callTypeLabel.label);
        
        MethAddr methAddr = (MethAddr) params.get(1);
        Operand receiver = (Operand) params.get(2);
        
        @SuppressWarnings("rawtypes")
        ArrayList argsList = (ArrayList) params.get(3);
        Operand[] args = null;
        if(argsList != null) {
            args = new Operand[argsList.size()];
            argsList.toArray(args);
        } else {
            args = Operand.EMPTY_ARRAY;
        }
        // FIXME: No closure support so far(WrappedIRClosure)
        return CallInstr.create(callType, result, methAddr, receiver, args, null);
    }
    
    private ConstMissingInstr createConstMissing(Variable result, List<Object> params) {
        Operand currentModule = (Operand) params.get(0);
        Label missingConstLabel = (Label) params.get(1);
        
        return new ConstMissingInstr(result, currentModule, missingConstLabel.label);
    }
    
    public InheritanceSearchConstInstr createInheritanceSearchConstInstr(Variable result, List<Object> params) {
        Operand currentModule = (Operand) params.get(0);
        Label constNameLabel = (Label) params.get(1);
        BooleanLiteral noPrivateConstsLiteral = (BooleanLiteral) params.get(2);
        
        return new InheritanceSearchConstInstr(result, currentModule, constNameLabel.label, noPrivateConstsLiteral.isTrue());
    }
    
    private SearchConstInstr createSearchConst(Variable result, List<Object> params) {
        Label constNameLabel = (Label) params.get(0);
        Operand startingScope = (Operand) params.get(1);
        BooleanLiteral noPrivateConstsLiteral = (BooleanLiteral) params.get(2);
        
        return new SearchConstInstr(result, constNameLabel.label, startingScope, noPrivateConstsLiteral.isTrue());
    }
    
    public PutInstr createPutInstr(SingleParamInstr instr, Operand value) {
        Operation operation = instr.getOperation();
        Operand varOperand = (Operand) instr.getParameter();
        
        switch (operation) {        
        case PUT_GLOBAL_VAR:
            return createPutGlobalVar(varOperand, value);

        default:
            throw new UnsupportedOperationException();
        }
    }
    
    private PutGlobalVarInstr createPutGlobalVar(Operand varOperand, Operand value) {
        GlobalVariable var = (GlobalVariable) varOperand;
        return new PutGlobalVarInstr(var.getName(), value);
    }
}
