/*
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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

import org.jruby.ast.ZeroArgNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.evaluator.EvaluateVisitor;
import org.jruby.evaluator.AssignmentVisitor;
import org.jruby.util.collections.ArrayStack;
import org.jruby.util.collections.CollectionFactory;
import org.jruby.util.collections.IStack;
import org.jruby.util.Asserts;
import org.jruby.util.IdUtil;
import org.jruby.Ruby;
import org.jruby.ThreadClass;
import org.jruby.RubyModule;
import org.jruby.RubyClass;
import org.jruby.RubyArray;
import org.jruby.IncludedModuleWrapper;
import org.jruby.exceptions.NameError;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.NextJump;
import org.jruby.exceptions.RedoJump;
import org.jruby.exceptions.ArgumentError;
import org.ablaf.ast.INode;
import org.ablaf.common.ISourcePosition;
import org.ablaf.internal.lexer.DefaultLexerPosition;

import java.util.List;
import java.util.Iterator;

/**
 * @author jpetersen
 * @version $Revision$
 */
public class ThreadContext {
    private final Ruby runtime;

    private BlockStack blockStack;
    private ArrayStack dynamicVarsStack;

    private ThreadClass thread;

    private ArrayStack classStack;
    private ScopeStack scopeStack;
    private FrameStack frameStack;
    private IStack iterStack;

    private RubyModule wrapper;

    private ISourcePosition sourcePosition = DefaultLexerPosition.getInstance("", 0);

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
        this.iterStack = CollectionFactory.getInstance().newStack();

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

    public ThreadClass getThread() {
        return thread;
    }

    public void setThread(ThreadClass thread) {
        this.thread = thread;
    }

    public IRubyObject eval(INode node) {
        return EvaluateVisitor.createVisitor(runtime.getTopSelf()).eval(node);
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

    public String getSourceFile() {
        return getPosition().getFile();
    }

    public Scope currentScope() {
        return getScopeStack().current();
    }

    public int getSourceLine() {
        return getPosition().getLine();
    }

    public ISourcePosition getPosition() {
        return sourcePosition;
    }

    public void setPosition(String file, int line) {
        setPosition(DefaultLexerPosition.getInstance(file, line));
    }

    public void setPosition(ISourcePosition position) {
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
        if (getCurrentFrame().getLastClass() == null) {
            throw new NameError(
                runtime,
                "superclass method '" + getCurrentFrame().getLastFunc() + "' must be enabled by enableSuper().");
        }
        getIterStack().push(getCurrentIter().isNot() ? Iter.ITER_NOT : Iter.ITER_PRE);
        try {
            RubyClass superClass = getCurrentFrame().getLastClass().getSuperClass();
            return superClass.call(getCurrentFrame().getSelf(),
                                   getCurrentFrame().getLastFunc(),
                                   args,
                                   CallType.SUPER);
        } finally {
            getIterStack().pop();
        }
    }

    public IRubyObject yield(IRubyObject value, IRubyObject self, RubyModule klass, boolean checkArguments) {
        if (! isBlockGiven()) {
            throw new RaiseException(runtime, runtime.getExceptions().getLocalJumpError(), "yield called out of block");
        }

        pushDynamicVars();
        Block currentBlock = getBlockStack().getCurrent();

        getFrameStack().push(currentBlock.getFrame());

        Namespace oldNamespace = runtime.getNamespace();
        runtime.setNamespace(getCurrentFrame().getNamespace());

        Scope oldScope = (Scope) getScopeStack().getTop();
        getScopeStack().setTop(currentBlock.getScope());

        getBlockStack().pop();

        dynamicVarsStack.push(currentBlock.getDynamicVariables());

        pushClass((klass != null) ? klass : currentBlock.getKlass());

        if (klass == null) {
            self = currentBlock.getSelf();
        }
        if (value == null) {
            value = RubyArray.newArray(runtime, 0);
        }

        ICallable method = currentBlock.getMethod();
        if (method == null) {
            return runtime.getNil();
        }

        getIterStack().push(currentBlock.getIter());

        IRubyObject[] args = prepareArguments(value, self, currentBlock.getVar(), checkArguments);

        try {
            while (true) {
                try {
                    return method.call(runtime, self, null, args, false);
                } catch (RedoJump rExcptn) {
                }
            }
        } catch (NextJump nExcptn) {
            return runtime.getNil();
        /*} catch (BreakJump rExcptn) {
            return runtime.getNil(); */
        } finally {
            getIterStack().pop();
            popClass();
            dynamicVarsStack.pop();

            getBlockStack().setCurrent(currentBlock);
            getFrameStack().pop();

            runtime.setNamespace(oldNamespace);

            getScopeStack().setTop(oldScope);
            dynamicVarsStack.pop();
        }
    }

    public boolean isBlockGiven() {
        return getCurrentFrame().isBlockGiven();
    }

    private IRubyObject[] prepareArguments(IRubyObject value, IRubyObject self, INode blockVariableNode, boolean checkArguments) {
        value = prepareBlockVariable(value, self, blockVariableNode, checkArguments);

        if (blockVariableNode == null) {
            if (checkArguments && value instanceof RubyArray && ((RubyArray) value).getLength() == 1) {
                value = ((RubyArray) value).first();
            }
        }

        return arrayify(value);
    }

    private IRubyObject[] arrayify(IRubyObject value) {
        if (value instanceof RubyArray) {
            return ((RubyArray) value).toJavaArray();
        } else {
            return new IRubyObject[] { value };
        }
    }

    private IRubyObject prepareBlockVariable(IRubyObject value, IRubyObject self, INode blockVariableNode, boolean checkArguments) {
        if (blockVariableNode == null) {
            return value;
        }

        if (checkArguments && value instanceof RubyArray) {
            RubyArray arrayValue = ((RubyArray) value);
            if (blockVariableNode instanceof ZeroArgNode) {
                if (arrayValue.getLength() != 0) {
                    throw new ArgumentError(runtime, arrayValue.getLength(), 0);
                }
            }
            if (!(blockVariableNode instanceof MultipleAsgnNode)) {
                if (arrayValue.getLength() == 1) {
                    value = arrayValue.first();
                }
            }
        }
        new AssignmentVisitor(runtime, self).assign(blockVariableNode, value, checkArguments);
        return value;
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
        if (result == null) {
            return runtime.getNil();
        }
        return result;
    }

    public IRubyObject getConstant(IRubyObject self, String name) {
        Asserts.isTrue(IdUtil.isConstant(name));

        // First search the current class hierarchy (the class-stack/namespace stack) ..?
        Namespace initial = getCurrentFrame().getNamespace();
        for (Namespace ns = initial; ns != null && ns.getParent() != null; ns = ns.getParent()) {
            if (ns.getModule().hasInstanceVariable(name)) {
                return ns.getModule().getInstanceVariable(name);
            }
        }
        // Then search the inheritance hierarchy ..?
        return initial.getModule().getConstant(name);
    }

    public RubyArray moduleNesting() {
        RubyArray result = RubyArray.newArray(runtime);
        Iterator iter = classStack.contents();
        while (iter.hasNext()) {
            RubyModule module = (RubyModule) iter.next();
            if (! (module instanceof RubyClass)) {
                 result.append(module);
            }
        }
        return result;
    }
}