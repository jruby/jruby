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
 * Copyright (C) 2006 Michael Studman <codehaus@michaelstudman.com>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
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
import org.jruby.parser.LocalStaticScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.SinglyLinkedList;

/**
 * @author jpetersen
 */
public class ThreadContext {
    private final static int INITIAL_SIZE = 50;
    
    private final IRuby runtime;

    // Is this thread currently with in a function trace? 
    private boolean isWithinTrace;
    
    // Is this thread currently doing an defined? defined should set things like $!
    private boolean isWithinDefined;

    private Block blockStack;

    private RubyThread thread;
    
    //private UnsynchronizedStack parentStack;
    private RubyModule[] parentStack = new RubyModule[INITIAL_SIZE];
    private int parentIndex = -1;
	
    //private UnsynchronizedStack frameStack;
    private Frame[] frameStack = new Frame[INITIAL_SIZE];
    private int frameIndex = -1;
    //private UnsynchronizedStack iterStack;
    private Iter[] iterStack = new Iter[INITIAL_SIZE];
    private int iterIndex = -1;
    //private UnsynchronizedStack crefStack;
    private SinglyLinkedList[] crefStack = new SinglyLinkedList[INITIAL_SIZE];
    private int crefIndex = -1;
    
    // List of active dynamic scopes.  Each of these may have captured other dynamic scopes
    // to implement closures.
    private DynamicScope[] scopeStack = new DynamicScope[INITIAL_SIZE];
    private int scopeIndex = -1;
    
    private String[] catchStack = new String[INITIAL_SIZE];
    private int catchIndex = -1;

    private int[] bindingFrameStack = new int[INITIAL_SIZE];
    private int bindingFrameIndex = -1;

    private RubyModule wrapper;

    private ISourcePosition sourcePosition = new SourcePositionFactory(null).getDummyPosition();

    /**
     * Constructor for Context.
     */
    public ThreadContext(IRuby runtime) {
        this.runtime = runtime;
        
        // TOPLEVEL self and a few others want a top-level scope.  We create this one right
        // away and then pass it into top-level parse so it ends up being the top level.
       pushScope(new DynamicScope(new LocalStaticScope(null), null));
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
        //System.out.println("IN RESTORE BLOCK (" + block.getDynamicScope() + ")");
        pushFrame(block.getFrame());

        setCRef(block.getCRef());
        
        getCurrentFrame().setScope(block.getScope());


        if (block.getDynamicScope() != null) {
            pushScope(block.getDynamicScope().cloneScope());
        }

        pushRubyClass((klass != null) ? klass : block.getKlass()); 

        pushIter(block.getIter());
    }

    private void flushBlockState(Block block) {
        //System.out.println("FLUSH");
        popIter();
        
        // For loop eval has no dynamic scope
        if (block.getDynamicScope() != null) {
            popScope();
        }
        popFrame();
        
        unsetCRef();
        
        popRubyClass();
    }

    public void printScope() {
        System.out.println("SCOPE STACK:");
        for (int i = 0; i <= scopeIndex; i++) {
            System.out.println(scopeStack[i]);
        }
    }
    
    public DynamicScope getCurrentScope() {
        return scopeStack[scopeIndex];
    }
    
    private void expandFramesIfNecessary() {
        if (frameIndex + 1 == frameStack.length) {
            int newSize = frameStack.length * 2;
            Frame[] newFrameStack = new Frame[newSize];
            
            System.arraycopy(frameStack, 0, newFrameStack, 0, frameStack.length);
            
            frameStack = newFrameStack;
        }
    }
    
    private void expandParentsIfNecessary() {
        if (parentIndex + 1 == parentStack.length) {
            int newSize = parentStack.length * 2;
            RubyModule[] newParentStack = new RubyModule[newSize];
            
            System.arraycopy(parentStack, 0, newParentStack, 0, parentStack.length);
            
            parentStack = newParentStack;
        }
    }
    
    private void expandItersIfNecessary() {
        if (iterIndex + 1 == iterStack.length) {
            int newSize = iterStack.length * 2;
            Iter[] newIterStack = new Iter[newSize];
            
            System.arraycopy(iterStack, 0, newIterStack, 0, iterStack.length);
            
            iterStack = newIterStack;
        }
    }
    
    private void expandCrefsIfNecessary() {
        if (crefIndex + 1 == crefStack.length) {
            int newSize = crefStack.length * 2;
            SinglyLinkedList[] newCrefStack = new SinglyLinkedList[newSize];
            
            System.arraycopy(crefStack, 0, newCrefStack, 0, crefStack.length);
            
            crefStack = newCrefStack;
        }
    }
    
    public void pushScope(DynamicScope scope) {
        scopeStack[++scopeIndex] = scope;
        expandScopesIfNecessary();
    }
    
    public void popScope() {
        scopeStack[scopeIndex--] = null;
    }

    private void expandScopesIfNecessary() {
        if (scopeIndex + 1 == scopeStack.length) {
            int newSize = scopeStack.length * 2;
            DynamicScope[] newScopeStack = new DynamicScope[newSize];
            
            System.arraycopy(scopeStack, 0, newScopeStack, 0, scopeStack.length);
            
            scopeStack = newScopeStack;
        }
    }

    public RubyThread getThread() {
        return thread;
    }

    public void setThread(RubyThread thread) {
        this.thread = thread;
    }

    public IRubyObject getLastline() {
        return getCurrentScope().getLastLine();
    }

    public void setLastline(IRubyObject value) {
        getCurrentScope().setLastLine(value);
    }
    
    //////////////////// CATCH MANAGEMENT ////////////////////////
    private void expandCatchIfNecessary() {
        if (catchIndex + 1 == catchStack.length) {
            int newSize = catchStack.length * 2;
            String[] newCatchStack = new String[newSize];
    
            System.arraycopy(catchStack, 0, newCatchStack, 0, catchStack.length);
            catchStack = newCatchStack;
        }
    }
    
    public void pushCatch(String catchSymbol) {
        catchStack[++catchIndex] = catchSymbol;
        expandCatchIfNecessary();
    }

    public void popCatch() {
        catchIndex--;
    }

    public String[] getActiveCatches() {
        if (catchIndex < 0) return new String[0];

        String[] activeCatches = new String[catchIndex + 1];
        System.arraycopy(catchStack, 0, activeCatches, 0, catchIndex + 1);
        return activeCatches;
    }
    
    //////////////////// FRAME MANAGEMENT ////////////////////////
    private void pushFrameCopy() {
        pushFrame(getCurrentFrame().duplicate());
    }
    
    private void pushCallFrame(IRubyObject self, IRubyObject[] args, 
            String lastFunc, RubyModule lastClass) {
        Iter iter = getCurrentFrame().getCallingZSuper() ? getCurrentFrame().getIter() : getCurrentIter();
        pushFrame(new Frame(this, self, args, lastFunc, lastClass, getPosition(), iter, getCurrentBlock()));        
    }
    
    private void pushFrame() {
        pushFrame(new Frame(this, getCurrentIter(), getCurrentBlock()));
    }
    
    private void pushFrameNoBlock() {
        pushFrame(new Frame(this, Iter.ITER_NOT, null));
    }
    
    private void pushFrame(Frame frame) {
        frameStack[++frameIndex] = frame;
        expandFramesIfNecessary();
    }
    
    private void popFrame() {
        Frame frame = (Frame)frameStack[frameIndex--];

        setPosition(frame.getPosition());
    }
    
    public Frame getCurrentFrame() {
        return (Frame)frameStack[frameIndex];
    }
    
    public Frame getPreviousFrame() {
        int size = frameIndex + 1;
        return size <= 1 ? null : (Frame) frameStack[size - 2];
    }
    
    public int getFrameCount() {
        return frameIndex + 1;
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
    
    public void setFrameSelf(IRubyObject self) {
        getCurrentFrame().setSelf(self);
    }

    public IRubyObject getFramePreviousSelf() {
        return getPreviousFrame().getSelf();
    }

    public void setSelfToPrevious() {
        getCurrentFrame().setSelf(getPreviousFrame().getSelf());
    }

    public RubyModule getFrameLastClass() {
        return getCurrentFrame().getLastClass();
    }
    
    public RubyModule getPreviousFrameLastClass() {
        return getPreviousFrame().getLastClass();
    }
    
    public ISourcePosition getFramePosition() {
        return getCurrentFrame().getPosition();
    }
    
    public ISourcePosition getPreviousFramePosition() {
        return getPreviousFrame().getPosition();
    }

    private void expandBindingFrameIfNecessary() {
        if (bindingFrameIndex + 1 == bindingFrameStack.length) {
            int newSize = bindingFrameStack.length * 2;
            int[] newbindingFrameStack = new int[newSize];
    
            System.arraycopy(bindingFrameStack, 0, newbindingFrameStack, 0, bindingFrameStack.length);
            bindingFrameStack = newbindingFrameStack;
        }
    }
    
    public void pushBindingFrame(int bindingDepth) {
        bindingFrameStack[++bindingFrameIndex] = bindingDepth;
        expandBindingFrameIfNecessary();
    }

    public void popBindingFrame() {
        bindingFrameIndex--;
    }


    public int currentBindingFrame() {
        if(bindingFrameIndex == -1) {
            return 0;
        } else {
            return bindingFrameStack[bindingFrameIndex];
        }
    }

    
    /////////////////////////// ITER MANAGEMENT //////////////////////////
    private Iter popIter() {
        Iter ret = (Iter) iterStack[iterIndex];
        iterStack[iterIndex--] = null;
        return ret;
    }
    
    private void pushIter(Iter iter) {
        iterStack[++iterIndex] = iter;
        expandItersIfNecessary();
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
        pushIter(isBlockGiven() ? Iter.ITER_PRE : Iter.ITER_NOT);
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
        return (Iter) iterStack[iterIndex];
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
        IRubyObject value = getCurrentScope().getBackRef();
        
        // DynamicScope does not preinitialize these values since they are virtually
        // never used.
        return value == null ? runtime.getNil() : value; 
    }

    public void setBackref(IRubyObject backref) {
        getCurrentScope().setBackRef(backref);
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

    public IRubyObject callSuper(IRubyObject[] args, boolean zSuper) {
        Frame frame = getCurrentFrame();
        
        frame.setCallingZSuper(zSuper);        
        
        if (frame.getLastClass() == null) {
            String name = frame.getLastFunc();
            throw runtime.newNameError("superclass method '" + name + "' must be enabled by enableSuper().", name);
        }
        setNoBlockIfNoBlock();
        try {
            RubyClass superClass = frame.getLastClass().getSuperClass();

            // Modules do not directly inherit Object so we have hacks like this
            if (superClass == null) {
                // TODO cnutter: I believe modules, the only ones here to have no superclasses, should have Module as their superclass
            	superClass = runtime.getClass("Module");
            }
            return frame.getSelf().callMethod(this, superClass,
                                   frame.getLastFunc(), args, CallType.SUPER);
        } finally {
            clearNoBlock();
            // must reset to false after calling so there's no screwy handling
            // of other calls from this frame
            frame.setCallingZSuper(false);            
        }
    }    

    public IRubyObject callSuper(IRubyObject[] args) {
        return callSuper(args, false);
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
            postYield(currentBlock);
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
            postYield(yieldBlock);
            postProcBlockCall();
        }
    }
    
    private IRubyObject yieldInternal(Block yieldBlock, IRubyObject value, IRubyObject self, RubyModule klass, boolean aValue) {
        if (klass == null) {
            self = yieldBlock.getSelf();
        }
        
        // FIXME: during refactoring, it was determined that all calls to yield are passing false for yieldProc; is this still needed?
        IRubyObject[] args = getBlockArgs(value, self, false, aValue, yieldBlock);
        
        while (true) {
            try {
                // FIXME: is it appropriate to use the current frame's (the block's frame's) lastClass?
                IRubyObject result = yieldBlock.getMethod().call(runtime.getCurrentContext(), self, getCurrentFrame().getLastClass(), null, args, false);
                
                return result;
            } catch (JumpException je) {
                if (je.getJumpType() == JumpException.JumpType.RedoJump) {
                    // do nothing, allow loop to redo
                } else {
                    throw je;
                }
            }
        }
    }

    private IRubyObject[] getBlockArgs(IRubyObject value, IRubyObject self, boolean yieldProc, boolean aValue, Block currentBlock) {
        Node blockVar = currentBlock.getVar();
        //FIXME: block arg handling is mucked up in strange ways and NEED to
        // be fixed. Especially with regard to Enumerable. See RubyEnumerable#eachToList too.
        if(blockVar == null) {
            return new IRubyObject[]{value};
        }
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
                    runtime.getWarnings().warn("multiple values for a block parameter (" + length + " for 1)");
                }
            } else if (value == null) { 
                runtime.getWarnings().warn("multiple values for a block parameter (0 for 1)");
            }

            AssignmentVisitor.assign(this, getFrameSelf(), blockVar, value, yieldProc); 
        }
        return ArgsUtil.arrayify(value);
    }
    
    public IRubyObject mAssign(IRubyObject self, MultipleAsgnNode node, RubyArray value, boolean pcall) {
        // Assign the values.
        int valueLen = value.getLength();
        int varLen = node.getHeadNode() == null ? 0 : node.getHeadNode().size();
        
        Iterator iter = node.getHeadNode() != null ? node.getHeadNode().iterator() : Collections.EMPTY_LIST.iterator();
        for (int i = 0; i < valueLen && iter.hasNext(); i++) {
            Node lNode = (Node) iter.next();
            AssignmentVisitor.assign(this, getFrameSelf(), lNode, value.entry(i), pcall);
        }

        if (pcall && iter.hasNext()) {
            throw runtime.newArgumentError("Wrong # of arguments (" + valueLen + " for " + varLen + ")");
        }

        if (node.getArgsNode() != null) {
            if (node.getArgsNode() instanceof StarNode) {
                // no check for '*'
            } else if (varLen < valueLen) {
                ArrayList newList = new ArrayList(value.getList().subList(varLen, valueLen));
                AssignmentVisitor.assign(this, getFrameSelf(), node.getArgsNode(), runtime.newArray(newList), pcall);
            } else {
                AssignmentVisitor.assign(this, getFrameSelf(), node.getArgsNode(), runtime.newArray(0), pcall);
            }
        } else if (pcall && valueLen < varLen) {
            throw runtime.newArgumentError("Wrong # of arguments (" + valueLen + " for " + varLen + ")");
        }

        while (iter.hasNext()) {
            AssignmentVisitor.assign(this, getFrameSelf(), (Node)iter.next(), runtime.getNil(), pcall);
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
        return (SinglyLinkedList)crefStack[crefIndex];
    }
    
    public void setCRef(SinglyLinkedList newCRef) {
        crefStack[++crefIndex] = newCRef;
        expandCrefsIfNecessary();
    }
    
    public void unsetCRef() {
        crefStack[crefIndex--] = null;
    }
    
    public SinglyLinkedList pushCRef(RubyModule newModule) {
        if (crefIndex == -1) {
            crefStack[++crefIndex] = new SinglyLinkedList(newModule, null);
        } else {
            crefStack[crefIndex] = new SinglyLinkedList(newModule, (SinglyLinkedList)crefStack[crefIndex]);
        }
        
        return (SinglyLinkedList)peekCRef();
    }
    
    public RubyModule popCRef() {
        assert !(crefIndex == -1) : "Tried to pop from empty CRef stack";
        
        RubyModule module = (RubyModule)peekCRef().getValue();
        
        SinglyLinkedList next = ((SinglyLinkedList)crefStack[crefIndex--]).getNext();
        
        if (next != null) {
            crefStack[++crefIndex] = next;
        } else {
            crefStack[crefIndex+1] = null;
        }
        
        return module;
    }

    public void pushRubyClass(RubyModule currentModule) {
        assert currentModule != null : "Can't push null RubyClass";
        
        parentStack[++parentIndex] = currentModule;
        expandParentsIfNecessary();
    }
    
    public RubyModule popRubyClass() {
        RubyModule ret = (RubyModule)parentStack[parentIndex];
        parentStack[parentIndex--] = null;
        return ret;
    }
	
    public RubyModule getRubyClass() {
        assert !(parentIndex == -1) : "Trying to get RubyClass from empty stack";
        
        RubyModule parentModule = (RubyModule)parentStack[parentIndex];

        return parentModule.getNonIncludedClass();
    }

    public RubyModule getWrapper() {
        return wrapper;
    }

    public void setWrapper(RubyModule wrapper) {
        this.wrapper = wrapper;
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
        int base = currentBindingFrame();
        int traceSize = frameIndex - level;
        
        if (traceSize <= 0) {
        	return backtrace;
        }
        
        if (nativeException) {
            // assert level == 0;
            addBackTraceElement(backtrace, (Frame) frameStack[frameIndex], null);
        }
        
        for (int i = traceSize; i > currentBindingFrame(); i--) {
            addBackTraceElement(backtrace, (Frame) frameStack[i], (Frame) frameStack[i-1]);
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
        getCurrentFrame().newScope();
        pushRubyClass(runtime.getObject());
        pushCRef(runtime.getObject());
        getCurrentFrame().setSelf(runtime.getTopSelf());
    }

    public void preClassEval(StaticScope staticScope, RubyModule type) {
        pushCRef(type);
        pushRubyClass(type); 
        pushFrameCopy();
        getCurrentFrame().newScope();
        pushScope(new DynamicScope(staticScope, getCurrentScope()));
    }
    
    public void postClassEval() {
        popCRef();
        popScope();
        popRubyClass();
        popFrame();
    }
    
    public void preBsfApply(String[] names) {
        // FIXME: I think we need these pushed somewhere?
        LocalStaticScope staticScope = new LocalStaticScope(null);
        staticScope.setVariables(names);
        pushFrameNoBlock();
        getCurrentFrame().newScope();
    }
    
    public void postBsfApply() {
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
    
    public void preDefMethodInternalCall(RubyModule lastClass, IRubyObject recv, String name, IRubyObject[] args, boolean noSuper, SinglyLinkedList cref, StaticScope staticScope) {
        RubyModule implementationClass = (RubyModule)cref.getValue();
        setCRef(cref);
        setInBlockIfBlock();
        pushCallFrame(recv, args, name, noSuper ? null : lastClass);
        getCurrentFrame().newScope();
        pushScope(new DynamicScope(staticScope, getCurrentScope()));
        pushRubyClass(implementationClass);
    }
    
    public void postDefMethodInternalCall() {
        popRubyClass();
        popScope();
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
        getCurrentFrame().newScope();
        setCurrentVisibility(Visibility.PRIVATE);
    }
    
    public void preInitBuiltinClasses(RubyClass objectClass, IRubyObject topSelf) {
        pushRubyClass(objectClass);
        setCRef(objectClass.getCRef());
        
        Frame frame = getCurrentFrame();
        frame.setSelf(topSelf);
    }
    
    public void preNodeEval(RubyModule newWrapper, RubyModule rubyClass, IRubyObject self) {
        setWrapper(newWrapper);
        pushRubyClass(rubyClass);
        pushCallFrame(self, IRubyObject.NULL_ARRAY, null, null);
        
        setCRef(rubyClass.getCRef());
    }
    
    public void postNodeEval(RubyModule newWrapper) {
        popFrame();
        popRubyClass();
        setWrapper(newWrapper);
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
    
    public void preTrace() {
        pushFrameNoBlock();
    }
    
    public void postTrace() {
        popFrame();
    }
    
    public void preBlockPassEval(Block block) {
        pushBlock(block);
        setBlockAvailable();
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
    
    private void preYieldSpecificBlock(Block specificBlock, RubyModule klass) {
        restoreBlockState(specificBlock, klass);
    }

    public void preEvalWithBinding(Block block) {
        pushBindingFrame(frameIndex);
        pushFrame(block.getFrame());
        setCRef(block.getCRef());        
        getCurrentFrame().setScope(block.getScope());
        pushRubyClass(block.getKlass()); 
        pushIter(block.getIter());
    }

    public void postEvalWithBinding(Block block) {
        popIter();
        popFrame();
        unsetCRef();
        popRubyClass();
        popBindingFrame();
    }
    
    public void postYield(Block block) {
        flushBlockState(block);
    }

    public void preRootNode(DynamicScope scope) {
        pushScope(scope);
        getCurrentFrame().newScope();
    }

    public void postRootNode() {
        popScope();
    }

    /**
     * Is this thread actively tracing at this moment.
     * 
     * @return true if so
     * @see org.jruby.IRuby#callTraceFunction(String, ISourcePosition, IRubyObject, String, IRubyObject)
     */
    public boolean isWithinTrace() {
        return isWithinTrace;
    }

    /**
     * Set whether we are actively tracing or not on this thread.
     * 
     * @param isWithinTrace true is so
     * @see org.jruby.IRuby#callTraceFunction(String, ISourcePosition, IRubyObject, String, IRubyObject)
     */
    public void setWithinTrace(boolean isWithinTrace) {
        this.isWithinTrace = isWithinTrace;
    }
    
    /**
     * Is this thread actively in defined? at the moment.
     * 
     * @return true if within defined?
     */
    public boolean isWithinDefined() {
        return isWithinDefined;
    }
    
    /**
     * Set whether we are actively within defined? or not.
     * 
     * @param isWithinDefined true if so
     */
    public void setWithinDefined(boolean isWithinDefined) {
        this.isWithinDefined = isWithinDefined;
    }
}
