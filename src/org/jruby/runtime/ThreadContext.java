/*
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with JRuby; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.runtime;

import org.jruby.IncludedModuleWrapper;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyThread;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.Node;
import org.jruby.ast.StarNode;
import org.jruby.ast.ZeroArgNode;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.ast.util.ListNodeUtil;
import org.jruby.evaluator.AssignmentVisitor;
import org.jruby.evaluator.EvaluateVisitor;
import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.LocalJumpError;
import org.jruby.exceptions.NameError;
import org.jruby.exceptions.NextJump;
import org.jruby.exceptions.RedoJump;
import org.jruby.lexer.yacc.SourcePosition;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.ArrayStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

/**
 * @author jpetersen
 * @version $Revision$
 */
public class ThreadContext {
    private final Ruby runtime;

    private BlockStack blockStack;
    private ArrayStack dynamicVarsStack;

    private RubyThread thread;

    private ArrayStack classStack;
    private ScopeStack scopeStack;
    private FrameStack frameStack;
    private Stack iterStack;

    private RubyModule wrapper;

    private SourcePosition sourcePosition = SourcePosition.getInstance("", 0);

    /**
     * Constructor for Context.
     */
    public ThreadContext(Ruby runtime) {
        this.runtime = runtime;

        this.blockStack = new BlockStack();
        this.dynamicVarsStack = new ArrayStack();
        this.classStack = new ArrayStack();
        this.scopeStack = new ScopeStack(runtime);
        this.frameStack = new FrameStack(this);
        this.iterStack = new Stack();

        pushDynamicVars();
    }

    public BlockStack getBlockStack() {
        return blockStack;
    }

    public DynamicVariableSet getCurrentDynamicVars() {
        return (DynamicVariableSet) dynamicVarsStack.peek();
    }

    public void pushDynamicVars() {
        dynamicVarsStack.push(new DynamicVariableSet());
    }

    public void popDynamicVars() {
        dynamicVarsStack.pop();
    }

    public List getDynamicNames() {
        return getCurrentDynamicVars().names();
    }

    public RubyThread getThread() {
        return thread;
    }

    public void setThread(RubyThread thread) {
        this.thread = thread;
    }

    public IRubyObject eval(Node node) {
        return EvaluateVisitor.createVisitor(runtime.getTopSelf()).eval(node);
    }

    public ScopeStack getScopeStack() {
        return scopeStack;
    }

    public FrameStack getFrameStack() {
        return frameStack;
    }

    public Stack getIterStack() {
        return iterStack;
    }

    public Frame getCurrentFrame() {
        return (Frame) getFrameStack().peek();
    }

    public Iter getCurrentIter() {
        return (Iter) getIterStack().peek();
    }

    public Scope currentScope() {
        return getScopeStack().current();
    }

    public SourcePosition getPosition() {
        return sourcePosition;
    }

    public void setPosition(String file, int line) {
        setPosition(SourcePosition.getInstance(file, line));
    }

    public void setPosition(SourcePosition position) {
        sourcePosition = position;
    }

    public IRubyObject getBackref() {
        if (getScopeStack().hasLocalVariables()) {
            return getScopeStack().getValue(1);
        }
        return runtime.getNil();
    }

    public Visibility getCurrentVisibility() {
        return getScopeStack().current().getVisibility();
    }

    public IRubyObject callSuper(IRubyObject[] args) {
    	Frame frame = getCurrentFrame();
    	
        if (frame.getLastClass() == null) {
            throw new NameError(runtime,
                "superclass method '" + frame.getLastFunc() + "' must be enabled by enableSuper().");
        }
        getIterStack().push(getCurrentIter().isNot() ? Iter.ITER_NOT : Iter.ITER_PRE);
        try {
            RubyClass superClass = frame.getLastClass().getSuperClass();
            return superClass.call(frame.getSelf(), frame.getLastFunc(),
                                   args, CallType.SUPER);
        } finally {
            getIterStack().pop();
        }
    }

    public IRubyObject yield(IRubyObject value, IRubyObject self, RubyModule klass, boolean yieldProc, boolean aValue) {
        if (! isBlockGiven()) {
            throw new LocalJumpError(runtime, "yield called out of block");
        }

        pushDynamicVars();
        Block currentBlock = getBlockStack().getCurrent();

        getFrameStack().push(currentBlock.getFrame());

        Scope oldScope = (Scope) getScopeStack().getTop();
        getScopeStack().setTop(currentBlock.getScope());

        getBlockStack().pop();

        dynamicVarsStack.push(currentBlock.getDynamicVariables());

        pushClass((klass != null) ? klass : currentBlock.getKlass());

        if (klass == null) {
            self = currentBlock.getSelf();
        }
        
        Node blockVar = currentBlock.getVar();
        if (blockVar != null) {
            if (blockVar instanceof ZeroArgNode) {
                // Better not have arguments for a no-arg block.
                if (yieldProc && arrayLength(value) != 0) { 
                    throw new ArgumentError(runtime, "wrong # of arguments(" + 
                            ((RubyArray)value).getLength() + "for 0)");
                }
            } else if (blockVar instanceof MultipleAsgnNode) {
                if (!aValue) {
                    value = sValueToMRHS(value, ((MultipleAsgnNode)blockVar).getHeadNode());
                }

                value = mAssign(self, (MultipleAsgnNode)blockVar, (RubyArray)value, yieldProc);
            } else {
                if (aValue) {
                    int length = arrayLength(value);
                    
                    if (length == 0) {
                        value = runtime.getNil();
                    } else if (length == 1) {
                        value = ((RubyArray)value).first(null);
                    } else {
                        // XXXEnebo - Should be warning not error.
                        //throw new ArgumentError(runtime, "wrong # of arguments(" + 
                        //        length + "for 1)");
                    }
                } else if (value == null) { 
                    // XXXEnebo - Should be warning not error.
                    //throw new ArgumentError(runtime, "wrong # of arguments(0 for 1)");
                }

                new AssignmentVisitor(runtime, self).assign(blockVar, value, yieldProc); 
            }
        }

        ICallable method = currentBlock.getMethod();

        if (method == null) {
            return runtime.getNil();
        }

        getIterStack().push(currentBlock.getIter());
        
        IRubyObject[] args = ArgsUtil.arrayify(runtime, value);

        try {
            while (true) {
                try {
                    return method.call(runtime, self, null, args, false);
                } catch (RedoJump rExcptn) {
                }
            }
        } catch (NextJump nExcptn) {
            IRubyObject nextValue = nExcptn.getNextValue();
            return nextValue == null ? runtime.getNil() : nextValue;
        } finally {
            getIterStack().pop();
            popClass();
            dynamicVarsStack.pop();

            getBlockStack().setCurrent(currentBlock);
            getFrameStack().pop();

            getScopeStack().setTop(oldScope);
            dynamicVarsStack.pop();
        }
    }
    
    public IRubyObject mAssign(IRubyObject self, MultipleAsgnNode node, RubyArray value, boolean pcall) {
        // Assign the values.
        int valueLen = value.getLength();
        int varLen = ListNodeUtil.getLength(node.getHeadNode());
        
        Iterator iter = node.getHeadNode() != null ? node.getHeadNode().iterator() : Collections.EMPTY_LIST.iterator();
        for (int i = 0; i < valueLen && iter.hasNext(); i++) {
            Node lNode = (Node) iter.next();
            new AssignmentVisitor(runtime, self).assign(lNode, value.entry(i), pcall);
        }

        if (pcall && iter.hasNext()) {
            throw new ArgumentError(runtime, "Wrong # of arguments (" + valueLen + " for " + varLen + ")");
        }

        if (node.getArgsNode() != null) {
            if (node.getArgsNode() instanceof StarNode) {
                // no check for '*'
            } else if (varLen < valueLen) {
                ArrayList newList = new ArrayList(value.getList().subList(varLen, valueLen));
                new AssignmentVisitor(runtime, self).assign(node.getArgsNode(), RubyArray.newArray(runtime, newList), pcall);
            } else {
                new AssignmentVisitor(runtime, self).assign(node.getArgsNode(), RubyArray.newArray(runtime, 0), pcall);
            }
        } else if (pcall && valueLen < varLen) {
            throw new ArgumentError(runtime, "Wrong # of arguments (" + valueLen + " for " + varLen + ")");
        }

        while (iter.hasNext()) {
            new AssignmentVisitor(runtime, self).assign((Node)iter.next(), runtime.getNil(), pcall);
        }
        
        return value;
    }

    private IRubyObject sValueToMRHS(IRubyObject value, Node leftHandSide) {
        if (value == null) {
            return RubyArray.newArray(runtime, 0);
        }
        
        if (leftHandSide == null) {
            return RubyArray.newArray(runtime, value);
        }
        
        IRubyObject newValue = value.convertToType("Array", "to_ary", false);

        if (newValue.isNil()) {
            return RubyArray.newArray(runtime, value);
        }
        
        return newValue;
    }
    
    private int arrayLength(IRubyObject node) {
        return node instanceof RubyArray ? ((RubyArray)node).getLength() : 0;
    }
    
    public boolean isBlockGiven() {
        return getCurrentFrame().isBlockGiven();
    }

    public void pollThreadEvents() {
        getThread().pollThreadEvents();
    }

    public void pushClass(RubyModule newClass) {
        classStack.push(newClass);
    }

    public void popClass() {
        classStack.pop();
    }

    public RubyModule getRubyClass() {
        if (classStack.depth() == 0) {
            return null;
        }

        RubyModule rubyClass = (RubyModule) classStack.peek();
        if (rubyClass.isIncluded()) {
            return ((IncludedModuleWrapper) rubyClass).getDelegate();
        }
        return rubyClass;
    }

    public RubyModule getWrapper() {
        return wrapper;
    }

    public void setWrapper(RubyModule wrapper) {
        this.wrapper = wrapper;
    }

    public IRubyObject getDynamicValue(String name) {
        IRubyObject result = ((DynamicVariableSet) dynamicVarsStack.peek()).get(name);

        return result == null ? runtime.getNil() : result;
    }
}
