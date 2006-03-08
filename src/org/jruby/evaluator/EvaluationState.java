/*
 * Created on Sep 11, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.jruby.evaluator;

import java.util.ArrayList;
import java.util.Iterator;

import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.RubyException;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.BreakNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.Node;
import org.jruby.ast.RedoNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.UnsynchronizedStack;

public class EvaluationState {
	//private IRubyObject result;
    private UnsynchronizedStack results = new UnsynchronizedStack();
	public final IRuby runtime;
	private IRubyObject self;
	public final EvaluateVisitor evaluator;
    private UnsynchronizedStack instructionBundleStacks = new UnsynchronizedStack();
    private JumpException currentException;
    private boolean handlingException;
    
    private class InstructionBundle {
        Instruction instruction;
        InstructionContext instructionContext;
        boolean ensured;
        boolean redoable;
        boolean breakable;
        boolean rescuable;
        boolean retriable;
        
        public InstructionBundle(Instruction i, InstructionContext ic) {
            instruction = i;
            instructionContext = ic;
        }
    }
    
    public EvaluationState(IRuby runtime, IRubyObject self) {
        this.runtime = runtime;
        this.evaluator = EvaluateVisitor.getInstance();
        
        results.push(runtime.getNil());
        
        setSelf(self);
    }
    
    // FIXME: exceptions thrown during aggregation cause this to leak
    public void aggregateResult() {
        results.push(runtime.getNil());
    }
    
    public void dumpStack() {
        for (int k = instructionBundleStacks.size() - 1; k >= 0; k--) {
            UnsynchronizedStack s = (UnsynchronizedStack)instructionBundleStacks.get(k);
            
            for (int i = s.size() - 1; i >= 0; i--) {
                System.err.println("at " + ((InstructionBundle)s.get(i)).instructionContext);
            }
        }
    }
    
    public IRubyObject deaggregateResult() {
        return (IRubyObject)results.pop();
    }
    
    public Instruction peekCurrentInstruction() {
        return peekCurrentInstructionBundle().instruction;
    }
    
    public InstructionContext peekCurrentInstructionContext() {
        return peekCurrentInstructionBundle().instructionContext;
    }
    
    public InstructionBundle peekCurrentInstructionBundle() {
        return (InstructionBundle)getCurrentInstructionStack().peek();
    }
	
	/**
	 * Return the current top stack of nodes
	 * @return
	 */
	public UnsynchronizedStack getCurrentInstructionStack() {
		return (UnsynchronizedStack)instructionBundleStacks.peek();
	}
	
	/**
	 * Pop and discard the current top stacks of nodes and visitors
	 */
	public void popCurrentInstructionStack() {
        instructionBundleStacks.pop();
	}
	
	/**
	 * Pop and discard the top item on the current top stacks of nodes and visitors
	 */
	public void popCurrentInstruction() {
		getCurrentInstructionStack().pop();
	}
	
	/**
	 * Push down a new pair of node and visitor stacks
	 */
	public void pushCurrentInstructionStack() {
		instructionBundleStacks.push(new UnsynchronizedStack());
	}
	
	/**
	 * Add a node and visitor to the top node and visitor stacks, using the default visitor for the given node.
	 * The default visitor is determined by calling node.accept() with the current state's evaluator.
	 * 
	 * @param ctx
	 */
	public void addNodeInstruction(InstructionContext ctx) {
        Node node = (Node)ctx;
		addInstruction(node, node.accept(evaluator));
	}
	
	/**
	 * Add the specified node and visitor to the top of the node and visitor stacks.
	 * The node will be passed to the visitor when it is evaluated.
	 * 
	 * @param node
	 * @param visitor
	 */
	public void addInstruction(InstructionContext ctx, Instruction visitor) {
        InstructionBundle ib = new InstructionBundle(visitor, ctx);
		getCurrentInstructionStack().push(ib);
	}
    
    public void addInstructionBundle(InstructionBundle ib) {
        getCurrentInstructionStack().push(ib);
    }
    
    private static class RedoMarker implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
        }
    }
    
    private static final RedoMarker redoMarker = new RedoMarker();
    
    public void addRedoMarker(Node redoableNode) {
        InstructionBundle ib = new InstructionBundle(redoMarker, redoableNode);
        
        ib.redoable = true;
        
        addInstructionBundle(ib);
    }
    
    public void addBreakableInstruction(InstructionContext ic, Instruction i) {
        InstructionBundle ib = new InstructionBundle(i, ic);
        
        ib.breakable = true;
        
        addInstructionBundle(ib);
    }
    
    public void addEnsuredInstruction(InstructionContext ic, Instruction i) {
        InstructionBundle ib = new InstructionBundle(i, ic);
        
        ib.ensured = true;
        
        addInstructionBundle(ib);
    }
    
    public void addRetriableInstruction(InstructionContext ic) {
        InstructionBundle ib = new InstructionBundle(retrier, ic);
        
        ib.retriable = true;
        
        addInstructionBundle(ib);
    }
    
    public void addRescuableInstruction(InstructionContext ic, Instruction i) {
        InstructionBundle ib = new InstructionBundle(i, ic);
        
        ib.rescuable = true;
        
        addInstructionBundle(ib);
    }
	
	/**
	 * Call the topmost visitor in the current top visitor stack with the topmost node in the current top node stack.
	 */
	public void executeNext() {
        // FIXME: Poll from somewhere else in the code? This polls per-node, perhaps per newline?
        getThreadContext().pollThreadEvents();
        
        InstructionBundle ib = (InstructionBundle)getCurrentInstructionStack().pop();
		
		if (ib != null) {
			ib.instruction.execute(this, ib.instructionContext);
		}
	}
	
	/**
	 * @param result The result to set.
	 */
	public void setResult(IRubyObject result) {
		results.set(results.size() - 1, result);
	}
    
	/**
	 * @return Returns the result.
	 */
	public IRubyObject getResult() {
		return (IRubyObject)results.peek();
	}
    
	public void clearResult() {
		setResult(runtime.getNil());
	}
    
	/**
	 * @param self The self to set.
	 */
	public void setSelf(IRubyObject self) {
		this.self = self;
	}
    
	/**
	 * @return Returns the self.
	 */
	public IRubyObject getSelf() {
		return self;
	}
    
    private static class ExceptionRethrower implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            throw state.getCurrentException();
        }
    }
    private static final ExceptionRethrower exceptionRethrower = new ExceptionRethrower();
    
    private static class ExceptionContinuer implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            state.handlingException = false;
        }
    }
    private static final ExceptionContinuer exceptionContinuer = new ExceptionContinuer();
    
    private static class RaiseRethrower implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            RaiseException re = new RaiseException((RubyException)state.getResult());
            
            throw re;
        }
    }
    private static final RaiseRethrower raiseRethrower = new RaiseRethrower();
    
    private static class Retrier implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            // dummy, only used to store the current "retriable" node and clear exceptions after a rescue block
            state.runtime.getGlobalVariables().set("$!", state.runtime.getNil());
        }
    }
    private static final Retrier retrier = new Retrier();
    
    // works like old recursive evaluation, for Assignment and Defined visitors.
    public IRubyObject begin(Node node) {
        clearResult();
        
        if (node != null) {
            try {
                // for each call to internalEval, push down new stacks (to isolate eval runs that still want to be logically separate
                pushCurrentInstructionStack();
                
                addNodeInstruction(node);
                
                // TODO: once we're ready to have an external entity run this loop (i.e. thread scheduler) move this out
                masterLoop: while (hasNext()) {                 
                    // invoke the next instruction
                    try {
                        executeNext();
                    } catch (JumpException je) {
                        if (je.getJumpType() == JumpException.JumpType.BreakJump) {
                            handleBreak(je);
                        } else if (je.getJumpType() == JumpException.JumpType.RaiseJump) {
                            handleRaise(je);
                        } else if (je.getJumpType() == JumpException.JumpType.RetryJump) {
                            handleRetry(je);
                        } else if (je.getJumpType() == JumpException.JumpType.ReturnJump) {
                            handleReturn(je);
                        } else if (je.getJumpType() == JumpException.JumpType.ThrowJump) {
                            handleThrow(je);
                        }
                    }
                    
                    if (currentException != null && !handlingException) {
                        if (currentException.getJumpType() == JumpException.JumpType.RedoJump) {
                            handleRedo(currentException);
                        } else if (currentException.getJumpType() == JumpException.JumpType.NextJump) {
                            handleNext(currentException);
                        }
                    }
                }
            } catch (StackOverflowError soe) {
                // TODO: perhaps a better place to catch this (although it will go away)
                throw runtime.newSystemStackError("stack level too deep");
            } finally {
                popCurrentInstructionStack();
            }
        }
        
        return getResult();
    }
    
    private void handleNext(JumpException je) {
        NextNode iVisited = (NextNode)je.getSecondaryData();
        
        while (!(getCurrentInstructionStack().isEmpty() || peekCurrentInstructionBundle().redoable)) {
            InstructionBundle ib = peekCurrentInstructionBundle();
            if (ib.ensured) {
                // exec ensured node, return to "nexting" afterwards
                popCurrentInstruction();
                handlingException = true;
                addInstruction(iVisited, exceptionContinuer);
                addInstructionBundle(ib);
                return;
            }
            popCurrentInstruction();
        }
        
        if (getCurrentInstructionStack().isEmpty()) {
            // rethrow next to previous level
            throw je;
        } else {
            // pop the redoable and continue
            popCurrentInstruction();
            setCurrentException(null);
            handlingException = false;
        }
    }
    
    private void handleRedo(JumpException je) {
        RedoNode iVisited = (RedoNode)je.getSecondaryData();
        
        while (!(getCurrentInstructionStack().isEmpty() || peekCurrentInstructionBundle().redoable)) {
            InstructionBundle ib = peekCurrentInstructionBundle();
            if (ib.ensured) {
                // exec ensured node, return to "redoing" afterwards
                popCurrentInstruction();
                handlingException = true;
                addInstruction(iVisited, exceptionRethrower);
                addInstructionBundle(ib);
                return;
            }
            popCurrentInstruction();
        }
        
        if (getCurrentInstructionStack().isEmpty()) {
            // rethrow next to previous level
            throw je;
        } else {
            // pop the redoable leave the redo body
            Node nodeToRedo = (Node)peekCurrentInstructionContext();
            popCurrentInstruction();
            addRedoMarker(nodeToRedo);
            addNodeInstruction(nodeToRedo);
            setCurrentException(null);
            handlingException = false;
        }
    }
    
    private void handleBreak(JumpException je) {
        BreakNode iVisited = (BreakNode)je.getSecondaryData();
        
//      pop everything but nearest breakable
        while (!(getCurrentInstructionStack().isEmpty() || peekCurrentInstructionBundle().breakable)) {
            InstructionBundle ib = peekCurrentInstructionBundle();
            if (ib.ensured) {
                // exec ensured node, return to "breaking" afterwards
                popCurrentInstruction();
                addInstruction(iVisited, exceptionRethrower);
                addInstructionBundle(ib);
                return;
            }
            popCurrentInstruction();
        }
        
        if (getCurrentInstructionStack().isEmpty()) {
            // rethrow to next level
            throw je;
        } else {
            // pop breakable and push previously-calculated break value
            popCurrentInstruction();
            setResult((IRubyObject)getCurrentException().getPrimaryData());
            setCurrentException(null);
        }
    }
    
    private void handleRaise(JumpException je) {
        RaiseException re = (RaiseException)je;
        RubyException raisedException = re.getException();
        setResult(raisedException);
        // TODO: Rubicon TestKernel dies without this line.  A cursory glance implies we
        // falsely set $! to nil and this sets it back to something valid.  This should 
        // get fixed at the same time we address bug #1296484.
        runtime.getGlobalVariables().set("$!", raisedException);

        // FIXME: don't use the raise rethrower; work with the exception rethrower like all other handlers do
        
//      pop everything but nearest rescuable
        while (!(getCurrentInstructionStack().isEmpty() || peekCurrentInstructionBundle().rescuable)) {
            InstructionBundle ib = peekCurrentInstructionBundle();
            if (ib.ensured) {
                // exec ensured node, return to "breaking" afterwards
                popCurrentInstruction();
                addInstruction(ib.instructionContext, raiseRethrower);
                addInstructionBundle(ib);
                return;
            }
            popCurrentInstruction();
        }
        
        if (getCurrentInstructionStack().isEmpty()) {
            // no rescuers, throw exception to next level
            throw re;
        }
        
        // we're at rescuer now
        RescueNode iVisited = (RescueNode)peekCurrentInstructionBundle().instructionContext;
        popCurrentInstruction();
        RescueBodyNode rescueNode = iVisited.getRescueNode();

        while (rescueNode != null) {
            Node exceptionNodes = rescueNode.getExceptionNodes();
            ListNode exceptionNodesList;
            
            // need to make these iterative
            if (exceptionNodes instanceof SplatNode) {                    
                exceptionNodesList = (ListNode) begin(exceptionNodes);
            } else {
                exceptionNodesList = (ListNode) exceptionNodes;
            }
            
            if (isRescueHandled(raisedException, exceptionNodesList)) {
                addRetriableInstruction(iVisited);
                addNodeInstruction(rescueNode);
                return;
            }
            
            rescueNode = rescueNode.getOptRescueNode();
        }

        // no takers; bubble up
        throw je;
    }
    
    private void handleRetry(JumpException je) {
//      pop everything but nearest rescuable
        while (!(getCurrentInstructionStack().isEmpty() || peekCurrentInstructionBundle().retriable)) {
            InstructionBundle ib = peekCurrentInstructionBundle();
            
            // ensured fires when retrying a method?
            if (ib.ensured) {
                // exec ensured node, return to "breaking" afterwards
                popCurrentInstruction();
                addInstruction(ib.instructionContext, exceptionRethrower);
                addInstructionBundle(ib);
                return;
            }
            
            popCurrentInstruction();
        }
        
        if (getCurrentInstructionStack().isEmpty()) {
            throw je;
        }
        
        InstructionBundle ib = peekCurrentInstructionBundle();
        
        popCurrentInstruction();
        
        // re-run the retriable node, clearing any exceptions
        runtime.getGlobalVariables().set("$!", runtime.getNil());
        setCurrentException(null);
        addNodeInstruction(ib.instructionContext);
    }
    
    private void handleReturn(JumpException je) {
        // make sure ensures fire
        while (!getCurrentInstructionStack().isEmpty()) {
            InstructionBundle ib = peekCurrentInstructionBundle();
            
            if (ib.ensured) {
                // exec ensured node, return to "breaking" afterwards
                popCurrentInstruction();
                setResult((IRubyObject)je.getSecondaryData());
                addInstruction(ib.instructionContext, exceptionRethrower);
                addInstructionBundle(ib);
                return;
            }
            
            popCurrentInstruction();
        }
        throw je;
    }
    
    private void handleThrow(JumpException je) {
        while (!getCurrentInstructionStack().isEmpty()) {
            InstructionBundle ib = peekCurrentInstructionBundle();
            
            if (ib.ensured) {
                // exec ensured node, return to "breaking" afterwards
                popCurrentInstruction();
                setCurrentException(je);
                addInstruction(ib.instructionContext, exceptionRethrower);
                addInstructionBundle(ib);
                return;
            }
            
            popCurrentInstruction();
        }
        throw je;
    }
    
    private boolean isRescueHandled(RubyException currentException, ListNode exceptionNodes) {
        if (exceptionNodes == null) {
            return currentException.isKindOf(runtime.getClass("StandardError"));
        }

        Block tmpBlock = getThreadContext().beginCallArgs();

        IRubyObject[] args = null;
        try {
            args = setupArgs(runtime, getThreadContext(), exceptionNodes);
        } finally {
            getThreadContext().endCallArgs(tmpBlock);
        }

        for (int i = 0; i < args.length; i++) {
            if (! args[i].isKindOf(runtime.getClass("Module"))) {
                throw runtime.newTypeError("class or module required for rescue clause");
            }
            if (args[i].callMethod("===", currentException).isTrue())
                return true;
        }
        return false;
    }

    private IRubyObject[] setupArgs(IRuby runtime, ThreadContext context, Node node) {
        if (node == null) {
            return IRubyObject.NULL_ARRAY;
        }

        if (node instanceof ArrayNode) {
            ISourcePosition position = context.getPosition();
            ArrayList list = new ArrayList(((ArrayNode) node).size());
            
            for (Iterator iter=((ArrayNode)node).iterator(); iter.hasNext();){
                final Node next = (Node) iter.next();
                if (next instanceof SplatNode) {
                    list.addAll(((RubyArray) begin(next)).getList());
                } else {
                    list.add(begin(next));
                }
            }

            context.setPosition(position);

            return (IRubyObject[]) list.toArray(new IRubyObject[list.size()]);
        }

        return ArgsUtil.arrayify(begin(node));
    }
    
    public void begin2(Node node) {
        clearResult();
    
        // for each call to internalEval, push down new stacks (to isolate eval runs that still want to be logically separate
        pushCurrentInstructionStack();
        
        addNodeInstruction(node);
    }

    public boolean hasNext() {
        return !getCurrentInstructionStack().isEmpty();
    }

    // Had to make it work this way because eval states are sometimes created in one thread for use in another...
    // For example, block creation for a new Thread; block, frame, and evalstate for that Thread are created in the caller
    // but used in the new Thread.
    public ThreadContext getThreadContext() {
        return runtime.getCurrentContext();
    }

    public JumpException getCurrentException() {
        return currentException;
    }

    public void setCurrentException(JumpException currentException) {
        this.currentException = currentException;
    }
}