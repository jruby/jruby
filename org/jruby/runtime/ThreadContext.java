package org.jruby.runtime;

import java.util.HashMap;
import java.util.Map;

import org.jruby.*;
import org.jruby.ast.ZeroArgNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.exceptions.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.evaluator.EvaluateVisitor;
import org.jruby.evaluator.AssignmentVisitor;
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

    public IRubyObject yield(IRubyObject value, IRubyObject self, RubyModule klass, boolean checkArguments) {
        if (! ruby.isBlockGiven()) {
            throw new RaiseException(ruby, ruby.getExceptions().getLocalJumpError(), "yield called out of block");
        }

        pushDynamicVars();
        Block currentBlock = getBlockStack().getCurrent();

        getFrameStack().push(currentBlock.getFrame());

        Namespace oldNamespace = ruby.getNamespace();
        ruby.setNamespace(getCurrentFrame().getNamespace());

        Scope oldScope = (Scope) ruby.getScope().getTop();
        ruby.getScope().setTop(currentBlock.getScope());

        getBlockStack().pop();

        getDynamicVarsStack().push(currentBlock.getDynamicVariables());

        ruby.pushClass((klass != null) ? klass : currentBlock.getKlass());

        if (klass == null) {
            self = currentBlock.getSelf();
        }

        if (value == null) {
            value = RubyArray.newArray(ruby, 0);
        }

        ICallable method = currentBlock.getMethod();

        if (method == null) {
            return ruby.getNil();
        }

        INode blockVar = currentBlock.getVar();

        if (blockVar != null) {
            if (blockVar instanceof ZeroArgNode) {
                if (checkArguments && value instanceof RubyArray && ((RubyArray) value).getLength() != 0) {
                    throw new ArgumentError(ruby, "wrong # of arguments (" + ((RubyArray) value).getLength() + " for 0)");
                }
            } else {
                if (!(blockVar instanceof MultipleAsgnNode)) {
                    if (checkArguments && value instanceof RubyArray && ((RubyArray) value).getLength() == 1) {
                        value = ((RubyArray) value).entry(0);
                    }
                }
                new AssignmentVisitor(ruby, self.toRubyObject()).assign(blockVar, value.toRubyObject(), checkArguments);
            }
        } else {
            if (checkArguments && value instanceof RubyArray && ((RubyArray) value).getLength() == 1) {
                value = ((RubyArray) value).entry(0);
            }
        }

        ruby.getIterStack().push(currentBlock.getIter());

        RubyObject[] args;
        if (value instanceof RubyArray) {
            args = ((RubyArray) value).toJavaArray();
        } else {
            args = new RubyObject[] { value.toRubyObject() };
        }

        try {
            while (true) {
                try {
                    return method.call(ruby, self.toRubyObject(), null, args, false);
                } catch (RedoJump rExcptn) {
                }
            }
        } catch (NextJump nExcptn) {
            return ruby.getNil();
        } catch (ReturnException rExcptn) {
            return rExcptn.getReturnValue();
        } finally {
            getIterStack().pop();
            ruby.popClass();
            ruby.popDynamicVars();

            getBlockStack().setCurrent(currentBlock);
            getFrameStack().pop();

            ruby.setNamespace(oldNamespace);

            ruby.getScope().setTop(oldScope);
            getDynamicVarsStack().pop();
        }
    }
}