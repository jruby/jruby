/*
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

package org.jruby.evaluator;

import org.ablaf.ast.INode;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.ZeroArgNode;
import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.NextJump;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.RedoJump;
import org.jruby.exceptions.ReturnException;
import org.jruby.runtime.Block;
import org.jruby.runtime.Frame;
import org.jruby.runtime.FrameStack;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.Iter;
import org.jruby.runtime.Namespace;
import org.jruby.runtime.RubyVarmap;
import org.jruby.runtime.Scope;
import org.jruby.runtime.ScopeStack;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.CollectionFactory;
import org.jruby.util.collections.IStack;

public class Evaluator {
    private final Ruby ruby;

    private ScopeStack scope;
    private FrameStack frameStack;
    private IStack iterStack = CollectionFactory.getInstance().newStack();

    public Evaluator(Ruby ruby) {
        this.ruby = ruby;
        scope = new ScopeStack(ruby);
        frameStack = new FrameStack(ruby);
    }

    public ScopeStack getScope() {
        return scope;
    }

    public FrameStack getFrameStack() {
        return frameStack;
    }

    public Frame getCurrentFrame() {
        return (Frame) getFrameStack().peek();
    }

    public IStack getIterStack() {
        return iterStack;
    }

    public Iter getCurrentIter() {
        return (Iter) getIterStack().peek();
    }

    public IRubyObject eval(INode node) {
        return EvaluateVisitor.createVisitor(ruby.getRubyTopSelf()).eval(node);
    }
    
    public IRubyObject yield(IRubyObject value, IRubyObject self, RubyModule klass, boolean checkArguments) {
        if (!ruby.isBlockGiven()) {
            throw new RaiseException(ruby, ruby.getExceptions().getLocalJumpError(), "yield called out of block");
        }

        RubyVarmap.push(ruby);
        Block currentBlock = ruby.getBlock().getCurrent();

        getFrameStack().push(currentBlock.getFrame());

        Namespace oldNamespace = ruby.getNamespace();
        ruby.setNamespace(getCurrentFrame().getNamespace());

        Scope oldScope = (Scope)getScope().getTop();
        getScope().setTop(currentBlock.getScope());

        ruby.getBlock().pop();

        ruby.setDynamicVars(currentBlock.getDynamicVars());

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

        getIterStack().push(currentBlock.getIter());

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
            RubyVarmap.pop(ruby);

            ruby.getBlock().setCurrent(currentBlock);
            getFrameStack().pop();

            ruby.setNamespace(oldNamespace);

            getScope().setTop(oldScope);
        }
    }
}
