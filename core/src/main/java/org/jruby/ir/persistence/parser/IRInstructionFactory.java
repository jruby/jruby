package org.jruby.ir.persistence.parser;

import java.util.ArrayList;
import java.util.List;

import org.jruby.ir.IRClassBody;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.AliasInstr;
import org.jruby.ir.instructions.AttrAssignInstr;
import org.jruby.ir.instructions.BEQInstr;
import org.jruby.ir.instructions.BNEInstr;
import org.jruby.ir.instructions.BlockGivenInstr;
import org.jruby.ir.instructions.BranchInstr;
import org.jruby.ir.instructions.BreakInstr;
import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.instructions.CheckArgsArrayArityInstr;
import org.jruby.ir.instructions.CheckArityInstr;
import org.jruby.ir.instructions.ClosureReturnInstr;
import org.jruby.ir.instructions.ConstMissingInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.DefineClassInstr;
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

public class IRInstructionFactory {
    
    private final IRParsingContext context;

    public IRInstructionFactory(IRParsingContext context) {
        this.context = context;        
    }
    
    public LabelInstr createLabel(String l) {
        Label label = new Label(l);
        return new LabelInstr(label);
    }
    
    public JumpInstr createJump(Label target) {
        return new JumpInstr(target);
    }

    public ThreadPollInstr createThreadPoll() {
        return new ThreadPollInstr();
    }
    
    public Instr createInstrWithSingleParam(SingleParamInstr instr) {
        Operation operation = instr.getOperation();
        Object param = instr.getParameter();
        switch (operation) {
        case BREAK:
            return createBreak(param);
        case CLOSURE_RETURN:
            return createClosureReturn(param);
        case LINE_NUM:
            return createLineNum(param);
        case RETURN:
            return createReturn(param);

        default:
            throw new UnsupportedOperationException();
        }
    }

    private BreakInstr createBreak(Object param) {
        Operand rv = (Operand) param;
        // Scope is null if name was not persisted
        IRScope s = null;
        
        return new BreakInstr(rv, s);
    }
    
    private Instr createClosureReturn(Object param) {
        Operand rv = (Operand) param;
        
        return new ClosureReturnInstr(rv);
    }

    private LineNumberInstr createLineNum(Object param) {
        Integer number = (Integer) param;
        IRScope currentScope = context.getCurrentScope();
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
        case ATTR_ASSIGN:
            return createAttrAssign(params);
        case CHECK_ARGS_ARRAY_ARITY:
            return createCheckArgsArrayArity(params);
        case CHECK_ARITY:
            return createCheckArity(params);
        case DEF_INST_METH:
            return createDefInstMeth(params);
        case BEQ:
            return createBEQ(params);
        case B_FALSE:
            return createBFalse(params);
        case BREAK:
            return createBreak(params);
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

    @SuppressWarnings("unchecked")
    private AttrAssignInstr createAttrAssign(List<Object> params) {
        MethAddr methAddr = (MethAddr) params.get(0);
        Operand receiver = (Operand) params.get(1);
        
        @SuppressWarnings("rawtypes")
        List argsList = (ArrayList) params.get(2);
        Operand[] args = null;
        if(argsList != null) {
            args = new Operand[argsList.size()];
            argsList.toArray(args);
        } else {
            args = Operand.EMPTY_ARRAY;
        }
        
        return new AttrAssignInstr(receiver, methAddr, args);
    }
    
    private CheckArgsArrayArityInstr createCheckArgsArrayArity(List<Object> params) {
        Operand argsArray = (Operand) params.get(0);
        int required = (Integer) params.get(1);
        int opt = (Integer) params.get(2);
        int rest = (Integer) params.get(3);
        
        return new CheckArgsArrayArityInstr(argsArray, required, opt, rest);
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
        
        IRManager manager = context.getIRManager();
        // FIXME: Or there may be other scopes rather than current?
        IRScope currentScope = context.getCurrentScope(); 
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
        Operand arg2 = context.getIRManager().getFalse();
        Label target = (Label) params.get(1);
        
        return BEQInstr.create(arg1, arg2, target);
    }
    
    private BreakInstr createBreak(List<Object> params) {
        Operand rv = (Operand) params.get(0);
        
        Label scopeNameLabel = (Label) params.get(1);
        String scopeName = scopeNameLabel.label;
        IRScope s = context.getScopeByName(scopeName);
        
        return new BreakInstr(rv, s);
    }

    private BranchInstr createBTrue(List<Object> params) {
        Operand arg1 = (Operand) params.get(0);
        Operand arg2 = context.getIRManager().getTrue();
        Label target = (Label) params.get(1);
        
        return BEQInstr.create(arg1, arg2, target);
    }
    
    private BranchInstr createBNil(List<Object> params) {
        Operand arg1 = (Operand) params.get(0);
        Operand arg2 = context.getIRManager().getNil();
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
        case DEF_CLASS:
            return createDefClass(result, params);
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
    
    private DefineClassInstr createDefClass(Variable result, List<Object> params) {
        
        Label  classNameLabel = (Label) params.get(0);
        IRClassBody irClassBody = (IRClassBody) context.getScopeByName(classNameLabel.label);
        
        Operand container = (Operand) params.get(1);
        Operand superClass = (Operand) params.get(2);
        
        return new DefineClassInstr(result, irClassBody, container, superClass);
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
