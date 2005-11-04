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
 * Copyright (C) 2004-2005 Charles O Nutter <headius@headius.com>
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

import org.jruby.IRuby;
import org.jruby.IncludedModuleWrapper;
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
import org.jruby.exceptions.JumpException;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.SourcePositionFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author jpetersen
 */
public class ThreadContext {
    private final IRuby runtime;

    private BlockStack blockStack;
    private Stack dynamicVarsStack;

    private RubyThread thread;
    
    private Stack parentStack;
	
    private ScopeStack scopeStack;
    private Stack frameStack;
    private Stack iterStack;

    private RubyModule wrapper;

    private ISourcePosition sourcePosition = new SourcePositionFactory().getDummyPosition();

    /**
     * Constructor for Context.
     */
    public ThreadContext(IRuby runtime) {
        this.runtime = runtime;

        this.blockStack = new BlockStack();
        this.dynamicVarsStack = new Stack();
        this.scopeStack = new ScopeStack();
        this.frameStack = new Stack();
        this.iterStack = new Stack();
        this.parentStack = new Stack();

        pushDynamicVars();
    }
    
    Visibility lastVis;
    CallType lastCallType;

    /**
     * Returns the lastCallStatus.
     * @return LastCallStatus
     */
    public void setLastCallStatus(Visibility vis, CallType callType) {
        lastVis = vis;
        lastCallType = callType;
    }
    
    public Visibility getLastVisibility() {
        return lastVis;
    }
    
    public CallType getLastCallType() {
        return lastCallType;
    }
    
    public void pushBlock(Block block) {
        blockStack.push(block);
    }
    
    public Block popBlock() {
        return (Block)blockStack.pop();
    }
    
    public Block peekBlock() {
        return (Block)blockStack.peek();
    }
    
    public void setCurrentBlock(Block block) {
        blockStack.setCurrent(block);
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
    
    public void pushScope(List localNames) {
        scopeStack.push(new Scope(runtime, localNames));
    }
    
    public void pushScope() {
        scopeStack.push(new Scope(runtime));
    }
    
    public void popScope() {
        scopeStack.pop();
    }
    
    public void pushFrameCopy() {
        frameStack.push(((Frame) frameStack.peek()).duplicate());
    }
    
    public void pushFrame(IRubyObject self, IRubyObject[] args, 
            String lastFunc, RubyModule lastClass) {
        frameStack.push(new Frame(this, self, args, lastFunc, lastClass));
    }
    
    public void pushFrame() {
        frameStack.push(new Frame(this));
    }
    
    public void pushFrame(Frame frame) {
        frameStack.push(frame);
    }
    
    public void pushFrame(Iter iter) {
        frameStack.push(new Frame(this, iter));
    }
    
    public void popFrame() {
        Frame frame = (Frame)frameStack.pop();

        setPosition(frame.getPosition());
    }
    
    public Frame getCurrentFrame() {
        return (Frame) frameStack.peek();
    }
    
    public Frame getPreviousFrame() {
        int size = frameStack.size();
        return size <= 1 ? null : (Frame) frameStack.elementAt(size - 2);
    }
    
    public Iter popIter() {
        return (Iter) iterStack.pop();
    }
    
    public void pushIter(Iter iter) {
        iterStack.push(iter);
    }

    public Iter getCurrentIter() {
        return (Iter) iterStack.peek();
    }

    public Scope currentScope() {
        return (Scope) scopeStack.peek();
    }

    public ISourcePosition getPosition() {
        return sourcePosition;
    }

    public void setPosition(ISourcePosition position) {
        sourcePosition = position;
    }

    public IRubyObject getBackref() {
        if (currentScope().hasLocalVariables()) {
            return currentScope().getValue(1);
        }
        return runtime.getNil();
    }

    public Visibility getCurrentVisibility() {
        return currentScope().getVisibility();
    }

    public IRubyObject callSuper(IRubyObject[] args) {
    	Frame frame = getCurrentFrame();
    	
        if (frame.getLastClass() == null) {
            throw runtime.newNameError("superclass method '" + frame.getLastFunc() + "' must be enabled by enableSuper().");
        }
        iterStack.push(getCurrentIter().isNot() ? Iter.ITER_NOT : Iter.ITER_PRE);
        try {
            RubyClass superClass = frame.getLastClass().getSuperClass();

            // Modules do not directly inherit Object so we have hacks like this
            if (superClass == null) {
            	superClass = runtime.getObject();
            }
            return frame.getSelf().callMethod(superClass, frame.getLastFunc(),
                                   args, CallType.SUPER);
        } finally {
            iterStack.pop();
        }
    }

    public IRubyObject yield(IRubyObject value, IRubyObject self, RubyModule klass, boolean yieldProc, boolean aValue) {
        if (! isBlockGiven()) {
            throw runtime.newLocalJumpError("yield called out of block");
        }

        Block currentBlock = (Block) blockStack.pop();

        frameStack.push(currentBlock.getFrame());

        Scope oldScope = (Scope) scopeStack.peek();
        scopeStack.setTop(currentBlock.getScope());

        dynamicVarsStack.push(currentBlock.getDynamicVariables());

        pushRubyClass((klass != null) ? klass : currentBlock.getKlass()); 

        iterStack.push(currentBlock.getIter());
            
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
                            value = ((RubyArray)value).first(IRubyObject.NULL_ARRAY);
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

            IRubyObject[] args = ArgsUtil.arrayify(value);

            while (true) {
                try {
                    return currentBlock.getMethod().call(runtime, self, null, args, false);
                } catch (JumpException je) {
                	if (je.getJumpType() == JumpException.JumpType.RedoJump) {
                		// do nothing, allow loop to redo
                	} else {
                		throw je;
                	}
                }
            }
        } catch (JumpException je) {
        	if (je.getJumpType() == JumpException.JumpType.NextJump) {
	            IRubyObject nextValue = (IRubyObject)je.getPrimaryData();
	            return nextValue == null ? runtime.getNil() : nextValue;
        	} else {
        		throw je;
        	}
        } finally {
            iterStack.pop();
            dynamicVarsStack.pop();
            frameStack.pop();
            
            blockStack.push(currentBlock);
            scopeStack.setTop(oldScope);
			popRubyClass();
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
        return getCurrentFrame().isBlockGiven() && blockStack.peek() != null;
    }

    public void pollThreadEvents() {
        getThread().pollThreadEvents();
    }

    public RubyModule pushRubyClass(RubyModule currentModule) {
        RubyModule previousModule = null;
        if (!parentStack.isEmpty()) {
            previousModule = (RubyModule)parentStack.peek();
        }
        
        parentStack.push(currentModule);
        
        return previousModule;
    }
    
    public RubyModule popRubyClass() {
        return (RubyModule)parentStack.pop();
    }
	
	public RubyModule getLastRubyClass() {
        if (parentStack.size() >= 2) {
            return (RubyModule)parentStack.get(parentStack.size() - 2);
        }
        
        return null;
	}
	
    public RubyModule getRubyClass() {
        if (parentStack.isEmpty()) {
            return null;
        }
        
        RubyModule parentModule = (RubyModule)parentStack.peek();
        
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
        Block currentBlock = (Block) blockStack.peek();

        if (getCurrentIter().isPre()) {
            blockStack.pop();
        }
        iterStack.push(Iter.ITER_NOT);
        return currentBlock;
    }

    public void endCallArgs(Block currentBlock) {
        blockStack.setCurrent(currentBlock);
        iterStack.pop();
    }

    private void addBackTraceElement(RubyArray backtrace, Frame frame, Frame previousFrame) {
        StringBuffer sb = new StringBuffer(100);
        ISourcePosition position = frame.getPosition();
    
        sb.append(position.getFile()).append(':').append(position.getEndLine());
    
        if (previousFrame != null && previousFrame.getLastFunc() != null) {
            sb.append(":in `").append(previousFrame.getLastFunc()).append('\'');
        } else if (previousFrame == null && frame.getLastFunc() != null) {
            sb.append(":in `").append(frame.getLastFunc()).append('\'');
        }
    
        backtrace.append(backtrace.getRuntime().newString(sb.toString()));
    }

    /** 
     * Create an Array with backtrace information.
     * @param runtime
     * @param level
     * @param nativeException
     * @return an Array with the backtrace 
     */
    public IRubyObject createBacktrace(int level, boolean nativeException) {
        RubyArray backtrace = runtime.newArray();
        int traceSize = frameStack.size() - level - 1;
        
        if (traceSize <= 0) {
        	return backtrace;
        }
        
        if (nativeException) {
            // assert level == 0;
            addBackTraceElement(backtrace, (Frame) frameStack.elementAt(frameStack.size() - 1), null);
        }
        
        for (int i = traceSize; i > 0; i--) {
            addBackTraceElement(backtrace, (Frame) frameStack.elementAt(i), (Frame) frameStack.elementAt(i-1));
        }
    
        return backtrace;
    }
}
