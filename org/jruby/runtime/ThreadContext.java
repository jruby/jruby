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
import org.ablaf.common.ISourcePosition;
import org.ablaf.internal.lexer.DefaultLexerPosition;

/**
 * @author jpetersen
 * @version $Revision$
 */
public class ThreadContext {
    private final Ruby ruby;

    private BlockStack blockStack;
    private RubyStack dynamicVarsStack;

    private RubyThread currentThread;

    private ScopeStack scopeStack;
    private FrameStack frameStack;
    private IStack iterStack;

    private ISourcePosition sourcePosition = new DefaultLexerPosition(null, 0, 0);

    /**
     * Constructor for Context.
     */
    public ThreadContext(Ruby ruby) {
        this.ruby = ruby;

        this.blockStack = new BlockStack();
        this.dynamicVarsStack = new RubyStack();
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
     * Sets the currentThread.
     * @param currentThread The currentThread to set
     */
    public void setCurrentThread(RubyThread currentThread) {
        this.currentThread = currentThread;
    }

    public IRubyObject eval(INode node) {
        return EvaluateVisitor.createVisitor(ruby.getTopSelf()).eval(node);
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

    public int getSourceLine() {
        return getPosition().getLine();
    }

    public ISourcePosition getPosition() {
        return sourcePosition;
    }

    public void setPosition(String file, int line) {
        setPosition(new DefaultLexerPosition(file, line, 0));
    }

    public void setPosition(ISourcePosition position) {
        sourcePosition = position;
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

        Scope oldScope = (Scope) getScopeStack().getTop();
        getScopeStack().setTop(currentBlock.getScope());

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

        getIterStack().push(currentBlock.getIter());

        IRubyObject[] args = prepareArguments(value, self, currentBlock.getVar(), checkArguments);

        try {
            while (true) {
                try {
                    return method.call(ruby, self, null, args, false);
                } catch (RedoJump rExcptn) {
                }
            }
        } catch (NextJump nExcptn) {
            return ruby.getNil();
        } catch (ReturnJump rExcptn) {
            return rExcptn.getReturnValue();
        } finally {
            getIterStack().pop();
            ruby.popClass();
            getDynamicVarsStack().pop();

            getBlockStack().setCurrent(currentBlock);
            getFrameStack().pop();

            ruby.setNamespace(oldNamespace);

            getScopeStack().setTop(oldScope);
            getDynamicVarsStack().pop();
        }
    }

    private IRubyObject[] prepareArguments(IRubyObject value, IRubyObject self, INode blockVariableNode, boolean checkArguments) {
        value = prepareBlockVariable(value, self, blockVariableNode, checkArguments);

        if (blockVariableNode == null) {
            if (checkArguments && value instanceof RubyArray && ((RubyArray) value).getLength() == 1) {
                value = ((RubyArray) value).entry(0);
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
                    throw new ArgumentError(ruby, arrayValue.getLength(), 0);
                }
            }
            if (!(blockVariableNode instanceof MultipleAsgnNode)) {
                if (arrayValue.getLength() == 1) {
                    value = arrayValue.entry(0);
                }
            }
        }
        new AssignmentVisitor(ruby, self).assign(blockVariableNode, value, checkArguments);
        return value;
    }
}