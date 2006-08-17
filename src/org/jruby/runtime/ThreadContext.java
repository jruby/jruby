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

import org.jruby.IRuby;
import org.jruby.IncludedModuleWrapper;
import org.jruby.RubyArray;
import org.jruby.RubyBinding;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyThread;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.Node;
import org.jruby.ast.StarNode;
import org.jruby.ast.ZeroArgNode;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.evaluator.AssignmentVisitor;
import org.jruby.evaluator.EvaluationState;
import org.jruby.exceptions.JumpException;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.SourcePositionFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.UnsynchronizedStack;
import org.jruby.util.collections.SinglyLinkedList;

/**
 * @author jpetersen
 */
public class ThreadContext {
    private final IRuby runtime;

    private Block blockStack;
    private UnsynchronizedStack dynamicVarsStack;

    private RubyThread thread;
    
    private UnsynchronizedStack parentStack;
	
    private UnsynchronizedStack frameStack;
    private UnsynchronizedStack iterStack;
    private UnsynchronizedStack crefStack;

    private RubyModule wrapper;

    private ISourcePosition sourcePosition = new SourcePositionFactory(null).getDummyPosition();

    /**
     * Constructor for Context.
     */
    public ThreadContext(IRuby runtime) {
        this.runtime = runtime;

        this.dynamicVarsStack = new UnsynchronizedStack();
        this.frameStack = new UnsynchronizedStack();
        this.iterStack = new UnsynchronizedStack();
        this.parentStack = new UnsynchronizedStack();
        this.crefStack = new UnsynchronizedStack();

        pushDynamicVars();
    }
    
    Visibility lastVis;
    CallType lastCallType;
    
    public IRuby getRuntime() {
        return runtime;
    }

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
    
    private void pushBlock(Block block) {
        block.setNext(this.blockStack);
        this.blockStack = block;
    }
    
    private Block popBlock() {
        if (blockStack == null) {
            return null;
        }
        
        Block current = blockStack;
        blockStack = (Block)blockStack.getNext();
        
        return current;
    }
    
    public Block getCurrentBlock() {
        return blockStack;
    }
    
    public boolean isBlockGiven() {
        return getCurrentFrame().isBlockGiven();
    }

    public boolean isFBlockGiven() {
        Frame previous = getPreviousFrame();
        if (previous == null) {
            return false;
        }
        return previous.isBlockGiven();
    }
    
    private void restoreBlockState(Block block, RubyModule klass) {
        pushFrame(block.getFrame());

        setCRef(block.getCRef());
        
        getCurrentFrame().setScope(block.getScope());

        pushDynamicVars(block.getDynamicVariables());

        pushRubyClass((klass != null) ? klass : block.getKlass()); 

        pushIter(block.getIter());
    }

    private void flushBlockState() {
        popIter();
        popDynamicVars();
        popFrame();
        
        unsetCRef();
        
        popRubyClass();
    }

    public DynamicVariableSet getCurrentDynamicVars() {
        return (DynamicVariableSet) dynamicVarsStack.peek();
    }

    private void pushDynamicVars() {
        dynamicVarsStack.push(new DynamicVariableSet());
    }

    private void pushDynamicVars(DynamicVariableSet dynVars) {
        dynamicVarsStack.push(dynVars);
    }

    private void popDynamicVars() {
        dynamicVarsStack.pop();
    }

    public RubyThread getThread() {
        return thread;
    }

    public void setThread(RubyThread thread) {
        this.thread = thread;
    }

    public IRubyObject getLastline() {
        return getFrameScope().getLastLine();
    }

    public void setLastline(IRubyObject value) {
        getFrameScope().setLastLine(value);
    }
    
    //////////////////// FRAME MANAGEMENT ////////////////////////
    private void pushFrameCopy() {
        pushFrame(getCurrentFrame().duplicate());
    }
    
    private void pushCallFrame(IRubyObject self, IRubyObject[] args, 
            String lastFunc, RubyModule lastClass) {
        pushFrame(new Frame(this, self, args, lastFunc, lastClass, getPosition(), getCurrentIter(), getCurrentBlock()));
    }
    
    private void pushFrame() {
        pushFrame(new Frame(this, getCurrentIter(), getCurrentBlock()));
    }
    
    private void pushFrameNoBlock() {
        pushFrame(new Frame(this, Iter.ITER_NOT, null));
    }
    
    private void pushFrame(Frame frame) {
        frameStack.push(frame);
    }
    
    private void popFrame() {
        Frame frame = (Frame)frameStack.pop();

        setPosition(frame.getPosition());
    }
    
    public Frame getCurrentFrame() {
        return (Frame)frameStack.peek();
    }
    
    public Frame getPreviousFrame() {
        int size = frameStack.size();
        return size <= 1 ? null : (Frame) frameStack.get(size - 2);
    }
    
    public int getFrameCount() {
        return frameStack.size();
    }
    
    public EvaluationState getFrameEvalState() {
        return getCurrentFrame().getEvalState();
    }
    
    public String getFrameLastFunc() {
        return getCurrentFrame().getLastFunc();
    }
    
    public Iter getFrameIter() {
        return getCurrentFrame().getIter();
    }
    
    public void setFrameIter(Iter iter) {
        getCurrentFrame().setIter(iter);
    }
    
    public Iter getPreviousFrameIter() {
        return getPreviousFrame().getIter();
    }
    
    public IRubyObject[] getFrameArgs() {
        return getCurrentFrame().getArgs();
    }
    
    public void setFrameArgs(IRubyObject[] args) {
        getCurrentFrame().setArgs(args);
    }
    
    public IRubyObject getFrameSelf() {
        return getCurrentFrame().getSelf();
    }
    
    public RubyModule getFrameLastClass() {
        return getCurrentFrame().getLastClass();
    }
    
    public ISourcePosition getFramePosition() {
        return getCurrentFrame().getPosition();
    }
    
    public ISourcePosition getPreviousFramePosition() {
        return getPreviousFrame().getPosition();
    }
    
    /////////////////////////// ITER MANAGEMENT //////////////////////////
    private Iter popIter() {
        return (Iter) iterStack.pop();
    }
    
    private void pushIter(Iter iter) {
        iterStack.push(iter);
    }
    
    public void setNoBlock() {
        pushIter(Iter.ITER_NOT);
    }
    
    private void setNoBlockIfNoBlock() {
        pushIter(getCurrentIter().isNot() ? Iter.ITER_NOT : Iter.ITER_PRE);
    }
    
    public void clearNoBlock() {
        popIter();
    }
    
    public void setBlockAvailable() {
        pushIter(Iter.ITER_PRE);
    }
    
    public void clearBlockAvailable() {
        popIter();
    }
    
    public void setIfBlockAvailable() {
        pushIter(getRuntime().getCurrentContext().isBlockGiven() ? Iter.ITER_PRE : Iter.ITER_NOT);
    }
    
    public void clearIfBlockAvailable() {
        popIter();
    }
    
    public void setInBlockIfBlock() {
        pushIter(getCurrentIter().isPre() ? Iter.ITER_CUR : Iter.ITER_NOT);
    }
    
    public void setInBlock() {
        pushIter(Iter.ITER_CUR);
    }
    
    public void clearInBlock() {
        popIter();
    }

    public Iter getCurrentIter() {
        return (Iter) iterStack.peek();
    }
    
    public UnsynchronizedStack getIterStack() {
        return iterStack;
    }

    public Scope getFrameScope() {
        return getCurrentFrame().getScope();
    }

    public ISourcePosition getPosition() {
        return sourcePosition;
    }
    
    public String getSourceFile() {
        return sourcePosition.getFile();
    }
    
    public int getSourceLine() {
        return sourcePosition.getEndLine();
    }

    public void setPosition(ISourcePosition position) {
        sourcePosition = position;
    }

    public IRubyObject getBackref() {
        return getFrameScope().getBackref();
    }

    public void setBackref(IRubyObject backref) {
        getFrameScope().setBackref(backref);
    }

    public Visibility getCurrentVisibility() {
        return getFrameScope().getVisibility();
    }

    public Visibility getPreviousVisibility() {
        return getPreviousFrame().getScope().getVisibility();
    }
    
    public void setCurrentVisibility(Visibility vis) {
        getFrameScope().setVisibility(vis);
    }

    public IRubyObject callSuper(IRubyObject[] args) {
    	Frame frame = getCurrentFrame();
    	
        if (frame.getLastClass() == null) {
            throw runtime.newNameError("superclass method '" + frame.getLastFunc() + "' must be enabled by enableSuper().");
        }
        setNoBlockIfNoBlock();
        try {
            RubyClass superClass = frame.getLastClass().getSuperClass();

            // Modules do not directly inherit Object so we have hacks like this
            if (superClass == null) {
                // TODO cnutter: I believe modules, the only ones here to have no superclasses, should have Module as their superclass
            	superClass = runtime.getClass("Module");
            }
            return frame.getSelf().callMethod(superClass, frame.getLastFunc(),
                                   args, CallType.SUPER);
        } finally {
            clearNoBlock();
        }
    }

    public IRubyObject yield(IRubyObject value) {
        return yieldCurrentBlock(value, null, null, false);
    }

    /**
     * Yield to the block passed to the current frame.
     * 
     * @param value The value to yield, either a single value or an array of values
     * @param self The current self
     * @param klass
     * @param yieldProc
     * @param aValue
     * @return
     */
    public IRubyObject yieldCurrentBlock(IRubyObject value, IRubyObject self, RubyModule klass, boolean aValue) {
        if (! isBlockGiven()) {
            throw runtime.newLocalJumpError("yield called out of block");
        }
        
        Block currentBlock = preYieldCurrentBlock(klass);

        try {
            return yieldInternal(currentBlock, value, self, klass, aValue);
        } catch (JumpException je) {
        	if (je.getJumpType() == JumpException.JumpType.NextJump) {
	            IRubyObject nextValue = (IRubyObject)je.getPrimaryData();
	            return nextValue == null ? runtime.getNil() : nextValue;
        	} else {
        		throw je;
        	}
        } finally {
            postYieldCurrentBlock(currentBlock);
        }
    }

    /**
     * Yield to a specific block.
     * 
     * @param yieldBlock The block to which to yield
     * @param value
     * @param self
     * @param klass
     * @param yieldProc
     * @param aValue
     * @return
     */
    public IRubyObject yieldSpecificBlock(Block yieldBlock, IRubyObject value, IRubyObject self, RubyModule klass, boolean aValue) {
        preProcBlockCall();
        preYieldSpecificBlock(yieldBlock, klass);
        try {
            return yieldInternal(yieldBlock, value, self, klass, aValue);
        } catch (JumpException je) {
            if (je.getJumpType() == JumpException.JumpType.NextJump) {
                IRubyObject nextValue = (IRubyObject)je.getPrimaryData();
                return nextValue == null ? runtime.getNil() : nextValue;
            } else {
                throw je;
            }
        } finally {
            postYieldSpecificBlock();
            postProcBlockCall();
        }
    }
    
    private IRubyObject yieldInternal(Block yieldBlock, IRubyObject value, IRubyObject self, RubyModule klass, boolean aValue) {
        // block is executed under its own self, so save the old one (use a stack?)
        IRubyObject oldSelf = getCurrentFrame().getEvalState().getSelf();
        
        try {
            setCRef(yieldBlock.getCRef());
            
            if (klass == null) {
                self = yieldBlock.getSelf();               
            }
    
            getCurrentFrame().getEvalState().setSelf(getCurrentFrame().getSelf()); 
            
            // FIXME: during refactoring, it was determined that all calls to yield are passing false for yieldProc; is this still needed?
            IRubyObject[] args = getBlockArgs(value, self, false, aValue, yieldBlock);
            while (true) {
                try {
                    // FIXME: is it appropriate to use the current frame's (the block's frame's) lastClass?
                    IRubyObject result = yieldBlock.getMethod().call(runtime, self, getCurrentFrame().getLastClass(), null, args, false);
                    
                    return result;
                } catch (JumpException je) {
                    if (je.getJumpType() == JumpException.JumpType.RedoJump) {
                        // do nothing, allow loop to redo
                    } else {
                        throw je;
                    }
                }
            }
        } finally {
            getCurrentFrame().getEvalState().setSelf(oldSelf);
            unsetCRef();
        }
    }

    private IRubyObject[] getBlockArgs(IRubyObject value, IRubyObject self, boolean yieldProc, boolean aValue, Block currentBlock) {
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

                new AssignmentVisitor(getCurrentFrame().getEvalState()).assign(blockVar, value, yieldProc); 
            }
        }

        IRubyObject[] args = ArgsUtil.arrayify(value);
        return args;
    }
    
    public IRubyObject mAssign(IRubyObject self, MultipleAsgnNode node, RubyArray value, boolean pcall) {
        // Assign the values.
        int valueLen = value.getLength();
        int varLen = node.getHeadNode() == null ? 0 : node.getHeadNode().size();
        
        Iterator iter = node.getHeadNode() != null ? node.getHeadNode().iterator() : Collections.EMPTY_LIST.iterator();
        for (int i = 0; i < valueLen && iter.hasNext(); i++) {
            Node lNode = (Node) iter.next();
            new AssignmentVisitor(getCurrentFrame().getEvalState()).assign(lNode, value.entry(i), pcall);
        }

        if (pcall && iter.hasNext()) {
            throw runtime.newArgumentError("Wrong # of arguments (" + valueLen + " for " + varLen + ")");
        }

        if (node.getArgsNode() != null) {
            if (node.getArgsNode() instanceof StarNode) {
                // no check for '*'
            } else if (varLen < valueLen) {
                ArrayList newList = new ArrayList(value.getList().subList(varLen, valueLen));
                new AssignmentVisitor(getCurrentFrame().getEvalState()).assign(node.getArgsNode(), runtime.newArray(newList), pcall);
            } else {
                new AssignmentVisitor(getCurrentFrame().getEvalState()).assign(node.getArgsNode(), runtime.newArray(0), pcall);
            }
        } else if (pcall && valueLen < varLen) {
            throw runtime.newArgumentError("Wrong # of arguments (" + valueLen + " for " + varLen + ")");
        }

        while (iter.hasNext()) {
            new AssignmentVisitor(getCurrentFrame().getEvalState()).assign((Node)iter.next(), runtime.getNil(), pcall);
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

    public void pollThreadEvents() {
        getThread().pollThreadEvents();
    }
    
    public SinglyLinkedList peekCRef() {
        return (SinglyLinkedList)crefStack.peek();
    }
    
    public void setCRef(SinglyLinkedList newCRef) {
        crefStack.push(newCRef);
    }
    
    public void unsetCRef() {
        crefStack.pop();
    }
    
    public SinglyLinkedList pushCRef(RubyModule newModule) {
        if (crefStack.isEmpty()) {
            crefStack.push(new SinglyLinkedList(newModule, null));
        } else {
            crefStack.push(new SinglyLinkedList(newModule, (SinglyLinkedList)crefStack.pop()));
        }
        
        return (SinglyLinkedList)peekCRef();
    }
    
    public RubyModule popCRef() {
        if (crefStack.isEmpty()) {
            Thread.dumpStack();
            return null;
        }
        
        RubyModule module = (RubyModule)peekCRef().getValue();
        
        SinglyLinkedList next = ((SinglyLinkedList)crefStack.pop()).getNext();
        
        if (next != null) {
            crefStack.push(next);
        }
        
        return module;
    }

    public void dumpRubyClasses() {
        for (int i = parentStack.size() - 1; i >= 0; i--) {
            System.out.println("parent: " + parentStack.get(i));
        }
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
    
    public boolean getConstantDefined(String name) {
        IRubyObject result = null;
        
        // flipped from while to do to search current class first
        for (SinglyLinkedList cbase = peekCRef(); cbase != null; cbase = cbase.getNext()) {
          result = ((RubyModule) cbase.getValue()).getConstantAt(name);
          if (result != null || runtime.getLoadService().autoload(name) != null) {
              return true;
          }
        } 
        
        return false;
    }

    public IRubyObject getConstant(String name) {
        //RubyModule self = state.threadContext.getRubyClass();
        SinglyLinkedList cbase = peekCRef();
        IRubyObject result = null;
        
        // flipped from while to do to search current class first
        do {
          RubyModule klass = (RubyModule) cbase.getValue();
          
          // Not sure how this can happen
          //if (NIL_P(klass)) return rb_const_get(CLASS_OF(self), id);
          result = klass.getConstantAt(name);
          if (result == null) {
              if (runtime.getLoadService().autoload(name) != null) {
                  continue;
              }
          } else {
              return result;
          }
          cbase = cbase.getNext();
        } while (cbase != null);

        
        //System.out.println("CREF is " + state.threadContext.getCRef().getValue());  
        return ((RubyModule) peekCRef().getValue()).getConstant(name);
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
            addBackTraceElement(backtrace, (Frame) frameStack.get(frameStack.size() - 1), null);
        }
        
        for (int i = traceSize; i > 0; i--) {
            addBackTraceElement(backtrace, (Frame) frameStack.get(i), (Frame) frameStack.get(i-1));
        }
    
        return backtrace;
    }
    
    public void beginCallArgs() {
        //Block block = getCurrentBlock();

        //if (getCurrentIter().isPre() && getCurrentBlock() != null) {
            //pushdownBlocks((Block)getCurrentBlock().getNext());
        //}
        setNoBlock();
        //return block;
    }

    public void endCallArgs(){//Block block) {
        //setCurrentBlock(block);
        clearNoBlock();
        //if (getCurrentIter().isPre() && !blockStack.isEmpty()) {
            //popupBlocks();
        //}
    }
    
    public void preAdoptThread() {
        setNoBlock();
        pushFrameNoBlock();
        getCurrentFrame().newScope(null);
        pushRubyClass(runtime.getObject());
        pushCRef(runtime.getObject());
    }

    public void preClassEval(String[] localNames, RubyModule type) {
        pushCRef(type);
        pushRubyClass(type); 
        pushFrameCopy();
        getCurrentFrame().newScope(localNames);
        pushDynamicVars();
    }
    
    public void postClassEval() {
        popCRef();
        popDynamicVars();
        popRubyClass();
        popFrame();
    }
    
    public void preScopedBody(String[] localNames) {
        assert false;
        getCurrentFrame().newScope(localNames);
    }
    
    public void postScopedBody() {
    }
    
    public void preBsfApply(String[] localNames) {
        pushFrameNoBlock();
        pushDynamicVars();
        getCurrentFrame().newScope(localNames);
    }
    
    public void postBsfApply() {
        popDynamicVars();
        popFrame();
    }

    public void preMethodCall(RubyModule implementationClass, RubyModule lastClass, IRubyObject recv, String name, IRubyObject[] args, boolean noSuper) {
        pushRubyClass((RubyModule)implementationClass.getCRef().getValue());
        setInBlockIfBlock();
        pushCallFrame(recv, args, name, noSuper ? null : lastClass);
    }
    
    public void postMethodCall() {
        popFrame();
        clearInBlock();
        popRubyClass();
    }
    
    public void preDefMethodInternalCall(RubyModule lastClass, IRubyObject recv, String name, IRubyObject[] args, boolean noSuper, SinglyLinkedList cref) {
        RubyModule implementationClass = (RubyModule)cref.getValue();
        setCRef(cref);
        setInBlockIfBlock();
        pushCallFrame(recv, args, name, noSuper ? null : lastClass);
        getCurrentFrame().newScope(null);
        pushDynamicVars();
        pushRubyClass(implementationClass);
    }
    
    public void postDefMethodInternalCall() {
        popRubyClass();
        popDynamicVars();
        popFrame();
        clearInBlock();
        unsetCRef();
    }
    
    // NEW! Push a scope into the frame, since this is now required to use it
    // XXX: This is screwy...apparently Ruby runs internally-implemented methods in their own frames but in the *caller's* scope
    public void preReflectedMethodInternalCall(RubyModule implementationClass, RubyModule lastClass, IRubyObject recv, String name, IRubyObject[] args, boolean noSuper) {
        pushRubyClass((RubyModule)implementationClass.getCRef().getValue());
        setInBlockIfBlock();
        pushCallFrame(recv, args, name, noSuper ? null : lastClass);
        getCurrentFrame().setScope(getPreviousFrame().getScope());
    }
    
    public void postReflectedMethodInternalCall() {
        popFrame();
        clearInBlock();
        popRubyClass();
    }
    
    public void preInitCoreClasses() {
        setNoBlock();
        pushFrameNoBlock();
        getCurrentFrame().newScope(null);
        setCurrentVisibility(Visibility.PRIVATE);
    }
    
    public void preInitBuiltinClasses(RubyClass objectClass, IRubyObject topSelf) {
        pushRubyClass(objectClass);
        setCRef(objectClass.getCRef());
        
        Frame frame = getCurrentFrame();
        frame.setSelf(topSelf);
        frame.getEvalState().setSelf(topSelf);
    }
    
    public void preNodeEval(RubyModule newWrapper, RubyModule rubyClass, IRubyObject self) {
        pushDynamicVars();
        setWrapper(newWrapper);
        pushRubyClass(rubyClass);
        pushCallFrame(self, IRubyObject.NULL_ARRAY, null, null);
        getCurrentFrame().newScope(null);
        setCRef(rubyClass.getCRef());
    }
    
    public void postNodeEval(RubyModule newWrapper) {
        popFrame();
        popRubyClass();
        setWrapper(newWrapper);
        popDynamicVars();
        unsetCRef();
    }
    
    // XXX: Again, screwy evaling under previous frame's scope
    public void preExecuteUnder(RubyModule executeUnderClass) {
        Frame frame = getCurrentFrame();
        
        pushRubyClass(executeUnderClass);
        pushCRef(executeUnderClass);
        pushCallFrame(null, frame.getArgs(), frame.getLastFunc(), frame.getLastClass());
        getCurrentFrame().setScope(getPreviousFrame().getScope());
    }
    
    public void postExecuteUnder() {
        popFrame();
        popRubyClass();
        popCRef();
    }
    
    public void preMproc() {
        setInBlock();
        pushFrame();
    }
    
    public void postMproc() {
        popFrame();
        clearInBlock();
    }
    
    public void preRunThread(Frame currentFrame, Block currentBlock) {
        pushFrame(currentFrame);
        // create a new eval state for the the block frame (since it has been adopted by the created thread)
        // XXX: this is kind of a hack, since eval state holds ThreadContext, and when it's created it's in the other thread :(
        // we'll want to revisit these issues of block ownership since the block is created in one thread and used in another
        //currentBlock.getFrame().setEvalState(new EvaluationState(runtime, currentBlock.getFrame().getSelf()));
        blockStack = currentBlock;
    }
    
    public void preKernelEval() {
        // we pop here and push in the post so the eval runs under the previous frame
        // pop the frame created for us by the method call
        popFrame();
    }
    
    public void postKernelEval() {
        // push a dummy frame back to the stack for the method call to pop
        pushFrameNoBlock();
    }
    
    public void preTrace() {
        pushFrameNoBlock();
    }
    
    public void postTrace() {
        popFrame();
    }
    
    public void preBlockPassEval(Block block) {
        pushBlock(block);
        setBlockAvailable();
        
        if (getCurrentFrame().getIter() == Iter.ITER_NOT) {
            getCurrentFrame().setIter(Iter.ITER_PRE);
        }
    }
    
    public void postBlockPassEval() {
        clearBlockAvailable();
        popBlock();
    }
    
    public void preForLoopEval(Block block) {
        pushBlock(block);
        setBlockAvailable();
    }
    
    public void postForLoopEval() {
        clearBlockAvailable();
        popBlock();
    }
    
    public void preIterEval(Block block) {
        pushBlock(block);
    }
    
    public void postIterEval() {
        popBlock();
    }
    
    public void preToProc(Block block) {
        setBlockAvailable();
        pushBlock(block);
    }
    
    public void postToProc() {
        clearBlockAvailable();
        popBlock();
    }
    
    public void preProcBlockCall() {
        setInBlock();
        getCurrentFrame().setIter(Iter.ITER_CUR);
    }
    
    public void postProcBlockCall() {
        clearInBlock();
    }

    private Block preYieldCurrentBlock(RubyModule klass) {
        Block currentBlock = getCurrentFrame().getBlockArg();

        restoreBlockState(currentBlock, klass);

        return currentBlock;
    }

    private void postYieldCurrentBlock(Block currentBlock) {
        flushBlockState();
    }
    
    private void preYieldSpecificBlock(Block specificBlock, RubyModule klass) {
        restoreBlockState(specificBlock, klass);
    }
    
    private void postYieldSpecificBlock() {
        flushBlockState();
    }

    public void preEvalWithBinding(RubyBinding binding) {
        Block bindingBlock = binding.getBlock();

        restoreBlockState(bindingBlock, null);
    }

    public void postEvalWithBinding() {
        flushBlockState();
    }
}
