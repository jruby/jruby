/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

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
import org.jruby.evaluator.AssignmentVisitor;
import org.jruby.exceptions.LocalJumpError;
import org.jruby.exceptions.NameError;
import org.jruby.exceptions.NextJump;
import org.jruby.exceptions.RedoJump;
import org.jruby.lexer.yacc.SourcePosition;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author jpetersen
 * @version $Revision$
 */
public class ThreadContext {
    private final Ruby runtime;

    private BlockStack blockStack;
    private Stack dynamicVarsStack;

    private RubyThread thread;

	// Location of where we are executing from.
	private RubyModule parentModule = null;
	
	// Last Location we executed from.  This exists because when we push new location for a module
	// function like Module#nesting, parentModule gets set to Module instead of where it was called
	// from.  lastParentModule will contain that information.
	private RubyModule lastParentModule = null;
	
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
        this.dynamicVarsStack = new Stack();
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

        RubyModule oldParent = setRubyClass((klass != null) ? klass : currentBlock.getKlass()); 

        try {
            if (klass == null) {
                self = currentBlock.getSelf();
            }
        
            Node blockVar = currentBlock.getVar();
            if (blockVar != null) {
                if (blockVar instanceof ZeroArgNode) {
                    // Better not have arguments for a no-arg block.
                    if (yieldProc && arrayLength(value) != 0) { 
                        throw runtime.newArgumentError("wrong # of arguments(" + 
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
                            //throw runtime.newArgumentError("wrong # of arguments(" + 
                            //        length + "for 1)");
                        }
                    } else if (value == null) { 
                        // XXXEnebo - Should be warning not error.
                        //throw runtime.newArgumentError("wrong # of arguments(0 for 1)");
                    }

                    new AssignmentVisitor(runtime, self).assign(blockVar, value, yieldProc); 
                }
            }

            ICallable method = currentBlock.getMethod();

            if (method == null) {
                return runtime.getNil();
            }

            getIterStack().push(currentBlock.getIter());
        
            IRubyObject[] args = ArgsUtil.arrayify(value);

            try {
                while (true) {
                    try {
                        return method.call(runtime, self, null, args, false);
                    } catch (RedoJump rExcptn) {}
                }
            } catch (NextJump nExcptn) {
                IRubyObject nextValue = nExcptn.getNextValue();
                return nextValue == null ? runtime.getNil() : nextValue;
            } finally {
                getIterStack().pop();
            }
        } finally {
            dynamicVarsStack.pop();

            getBlockStack().setCurrent(currentBlock);
            getFrameStack().pop();

            getScopeStack().setTop(oldScope);
            dynamicVarsStack.pop();
			setRubyClass(oldParent);
        }
    }
    
    public IRubyObject mAssign(IRubyObject self, MultipleAsgnNode node, RubyArray value, boolean pcall) {
        // Assign the values.
        int valueLen = value.getLength();
        int varLen = node.getHeadNode() == null ? 0 : node.getHeadNode().size();
        
        Iterator iter = node.getHeadNode() != null ? node.getHeadNode().iterator() : Collections.EMPTY_LIST.iterator();
        for (int i = 0; i < valueLen && iter.hasNext(); i++) {
            Node lNode = (Node) iter.next();
            new AssignmentVisitor(runtime, self).assign(lNode, value.entry(i), pcall);
        }

        if (pcall && iter.hasNext()) {
            throw runtime.newArgumentError("Wrong # of arguments (" + valueLen + " for " + varLen + ")");
        }

        if (node.getArgsNode() != null) {
            if (node.getArgsNode() instanceof StarNode) {
                // no check for '*'
            } else if (varLen < valueLen) {
                ArrayList newList = new ArrayList(value.getList().subList(varLen, valueLen));
                new AssignmentVisitor(runtime, self).assign(node.getArgsNode(), runtime.newArray(newList), pcall);
            } else {
                new AssignmentVisitor(runtime, self).assign(node.getArgsNode(), runtime.newArray(0), pcall);
            }
        } else if (pcall && valueLen < varLen) {
            throw runtime.newArgumentError("Wrong # of arguments (" + valueLen + " for " + varLen + ")");
        }

        while (iter.hasNext()) {
            new AssignmentVisitor(runtime, self).assign((Node)iter.next(), runtime.getNil(), pcall);
        }
        
        return value;
    }

    private IRubyObject sValueToMRHS(IRubyObject value, Node leftHandSide) {
        if (value == null) {
            return runtime.newArray(0);
        }
        
        if (leftHandSide == null) {
            return runtime.newArray(value);
        }
        
        IRubyObject newValue = value.convertToType("Array", "to_ary", false);

        if (newValue.isNil()) {
            return runtime.newArray(value);
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

	public RubyModule setRubyClass(RubyModule currentModule) {
		lastParentModule = this.parentModule;
		this.parentModule = currentModule;
		
		return lastParentModule;
	}
	
	public RubyModule getLastRubyClass() {
		return lastParentModule;
	}
	
    public RubyModule getRubyClass() {
        if (parentModule == null) {
            return null;
        }

        if (parentModule.isIncluded()) {
            return ((IncludedModuleWrapper) parentModule).getDelegate();
        }
        return parentModule;
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
    
    public Block beginCallArgs() {
        Block currentBlock = getBlockStack().getCurrent();

        if (getCurrentIter().isPre()) {
            getBlockStack().pop();
        }
        getIterStack().push(Iter.ITER_NOT);
        return currentBlock;
    }

    public void endCallArgs(Block currentBlock) {
        getBlockStack().setCurrent(currentBlock);
        getIterStack().pop();
    }


}
