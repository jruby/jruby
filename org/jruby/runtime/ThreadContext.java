package org.jruby.runtime;

import java.util.HashMap;
import java.util.Map;

import org.jruby.Ruby;
import org.jruby.RubyThread;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.evaluator.EvaluateVisitor;
import org.jruby.util.RubyStack;
import org.jruby.util.collections.CollectionFactory;
import org.jruby.util.collections.IStack;
import org.ablaf.ast.INode;

/**
 * @author jpetersen
 * @version $Revision$
 */
public class ThreadContext {
    private final Ruby ruby;

    private BlockStack blockStack;
    private RubyStack dynamicVarsStack;

    /* Thread related stuff */
    private RubyThread currentThread;
    private Map localVariables;

    private ScopeStack scopeStack;
    private FrameStack frameStack;
    private IStack iterStack;

    /**
     * Constructor for Context.
     */
    public ThreadContext(Ruby ruby) {
        this.ruby = ruby;

        this.blockStack = new BlockStack();
        this.dynamicVarsStack = new RubyStack();

        this.localVariables = new HashMap();

        this.scopeStack = new ScopeStack(ruby);
        this.frameStack = new FrameStack(ruby);
        this.iterStack = CollectionFactory.getInstance().newStack();

        pushDynamicVars();
    }

    public BlockStack getBlockStack() {
        return blockStack;
    }

    public RubyStack getDynamicVarsStack() {
        return dynamicVarsStack;
    }

    public Map getCurrentDynamicVars() {
        return (Map)dynamicVarsStack.peek();
    }

    public void pushDynamicVars() {
        dynamicVarsStack.push(new HashMap());
    }

    /**
     * Returns the currentThread.
     * @return RubyThread
     */
    public RubyThread getCurrentThread() {
        return currentThread;
    }

    /**
     * Returns the localVariables.
     * @return Map
     */
    public Map getLocalVariables() {
        return localVariables;
    }

    /**
     * Sets the currentThread.
     * @param currentThread The currentThread to set
     */
    public void setCurrentThread(RubyThread currentThread) {
        this.currentThread = currentThread;
    }

    public IRubyObject eval(Ruby ruby1, INode node) {
        return EvaluateVisitor.createVisitor(ruby.getRubyTopSelf()).eval(node);
    }

    public ScopeStack getScopeStack() {
        return scopeStack;
    }

    public FrameStack getFrameStack() {
        return frameStack;
    }

    public IStack getIterStack() {
        return iterStack;
    }

    public Frame getCurrentFrame() {
        return (Frame) getFrameStack().peek();
    }

    public Iter getCurrentIter() {
        return (Iter) getIterStack().peek();
    }
}