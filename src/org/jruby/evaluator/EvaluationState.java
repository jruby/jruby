/*
 * Created on Sep 11, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.jruby.evaluator;

import java.util.Stack;

import org.jruby.IRuby;
import org.jruby.ast.Node;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;


class EvaluationState {
	private IRubyObject result;
	private Object target; // for returns
	public final IRuby runtime;
	public final ThreadContext threadContext;
	private IRubyObject self;
	public final EvaluateVisitor evaluator;
	private Stack nodeStackStack = new Stack();
	private Stack nodeVisitorStackStack = new Stack();
	private EvaluationEvent event;
	
	private Stack returnPoints = new Stack();
    
    public EvaluationState(IRuby runtime, EvaluateVisitor evaluator) {
        this.runtime = runtime;
        this.evaluator = evaluator;
        
        result = runtime.getNil();
        threadContext = runtime.getCurrentContext();
    }
	
	/**
	 * Mark the current stack position as "wanting return". When a return is encountered,
	 * the stack will be returned to this point for evaluation to continue.
	 */
	public void addReturnPoint() {
		returnPoints.push(new Integer(getCurrentNodeStack().size()));
	}
	
	/**
	 * Return to the nearest recorded return point in the stack. Currently if we bubble over to the
	 * next lowest level in the stack of stacks, it means someone is immediately waiting to receive
	 * the return value. As such, this stack will only apply to the current top-level node/visitor
	 * stacks, and will be cleared upon crossing from one layer to another.
	 */
	public void returnToNearestReturnPoint() {
		if (returnPoints.isEmpty()) {
			getCurrentNodeStack().clear();
			getCurrentNodeVisitorStack().clear();
		} else {
			Integer nearestReturnPoint = (Integer)returnPoints.pop();
			while (getCurrentNodeStack().size() > nearestReturnPoint.intValue()) {
				popCurrentNodeAndVisitor();
			}
		}
	}
	
	/**
	 * Return the current top stack of nodes
	 * @return
	 */
	public Stack getCurrentNodeStack() {
		return (Stack)nodeStackStack.peek();
	}
	
	/**
	 * Pop and discard the current top stacks of nodes and visitors
	 */
	public void popCurrentNodeStacks() {
		nodeStackStack.pop();
		nodeVisitorStackStack.pop();
		returnPoints.clear();
	}
	
	/**
	 * Pop and discard the top item on the current top stacks of nodes and visitors
	 */
	public void popCurrentNodeAndVisitor() {
		getCurrentNodeStack().pop();
		getCurrentNodeVisitorStack().pop();
	}
	
	/**
	 * Push down a new pair of node and visitor stacks
	 */
	public void pushCurrentNodeStacks() {
		nodeStackStack.push(new Stack());
		nodeVisitorStackStack.push(new Stack());
	}
	
	/**
	 * Get the current top stack of node visitors
	 * @return
	 */
	public Stack getCurrentNodeVisitorStack() {
		return (Stack)nodeVisitorStackStack.peek();
	}
	
	/**
	 * Add a node and visitor to the top node and visitor stacks, using the default visitor for the given node.
	 * The default visitor is determined by calling node.accept() with the current state's evaluator.
	 * 
	 * @param ctx
	 */
	public void addNodeInstruction(InstructionContext ctx) {
        Node node = (Node)ctx;
		addNodeInstruction(node, node.accept(evaluator));
		returnPoints.clear();
	}
	
	/**
	 * Add the specified node and visitor to the top of the node and visitor stacks.
	 * The node will be passed to the visitor when it is evaluated.
	 * 
	 * @param node
	 * @param visitor
	 */
	public void addNodeInstruction(InstructionContext ctx, Instruction visitor) {
        Node node = (Node)ctx;
		getCurrentNodeStack().push(node);
		getCurrentNodeVisitorStack().push(visitor);
	}
	
	/**
	 * Call the topmost visitor in the current top visitor stack with the topmost node in the current top node stack.
	 */
	public void executeNext() {
        // FIXME: Poll from somewhere else in the code? This polls per-node, perhaps per newline?
        threadContext.pollThreadEvents();
        
		Node node = (Node)getCurrentNodeStack().pop();
		Instruction snv = (Instruction)getCurrentNodeVisitorStack().pop();
		
		if (node != null) {
			snv.execute(this, node);
		}
	}
	
	/**
	 * @param result The result to set.
	 */
	public void setResult(IRubyObject result) {
		this.result = result;
	}
    
	/**
	 * @return Returns the result.
	 */
	public IRubyObject getResult() {
		return result;
	}
    
	public void clearResult() {
		this.result = runtime.getNil();
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

	public static class EvaluationEvent {
		public static final EvaluationEvent Return = new EvaluationEvent(0);
		public static final EvaluationEvent Retry = new EvaluationEvent(1);
		public static final EvaluationEvent Redo = new EvaluationEvent(2);
		public static final EvaluationEvent Next = new EvaluationEvent(3);
		public static final EvaluationEvent Break = new EvaluationEvent(4);
		public static final EvaluationEvent Continue = new EvaluationEvent(5);
		
		private int id;
		
		public EvaluationEvent(int id) {
			this.id = id;
		}
	}

	
	/**
	 * @return Returns the event.
	 */
	public EvaluationEvent getEvent() {
		return event;
	}
	
	/**
	 * @param event The event to set.
	 */
	public void setEvent(EvaluationEvent event) {
		this.event = event;
	}
	/**
	 * @return Returns the target.
	 */
	public Object getTarget() {
		return target;
	}
	/**
	 * @param target The target to set.
	 */
	public void setTarget(Object target) {
		this.target = target;
	}

    public IRubyObject begin(Node node) {
        clearResult();
        
        if (node != null) {
        	try {
        		// for each call to internalEval, push down new stacks (to isolate eval runs that still want to be logically separate
                pushCurrentNodeStacks();
                
                addNodeInstruction(node);
        		
                // TODO: once we're ready to have an external entity run this loop (i.e. thread scheduler) move this out
        		while (hasNext()) {        	        
        	        // invoke the next instruction
        	        executeNext();
        		}
        	} catch (StackOverflowError soe) {
        		// TODO: perhaps a better place to catch this (although it will go away)
        		throw runtime.newSystemStackError("stack level too deep");
        	} finally {
        		popCurrentNodeStacks();
        	}
        }
        
        return getResult();
    }

    private boolean hasNext() {
        return !getCurrentNodeStack().isEmpty();
    }
}