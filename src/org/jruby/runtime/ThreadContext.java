/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
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

import java.util.HashMap;
import java.util.Map;
import org.jruby.runtime.scope.ManyVarsDynamicScope;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyKernel.CatchTarget;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.RubyThread;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.exceptions.JumpException.ReturnJump;
import org.jruby.internal.runtime.JumpTarget;
import org.jruby.internal.runtime.methods.DefaultMethod;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.libraries.FiberLibrary.Fiber;
import org.jruby.parser.BlockStaticScope;
import org.jruby.parser.LocalStaticScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

public final class ThreadContext {
    public static synchronized ThreadContext newContext(Ruby runtime) {
        ThreadContext context = new ThreadContext(runtime);
        //        if(runtime.getInstanceConfig().isSamplingEnabled()) {
        //    org.jruby.util.SimpleSampler.registerThreadContext(context);
        //}

        return context;
    }
    
    private final static int INITIAL_SIZE = 10;
    private final static String UNKNOWN_NAME = "(unknown)";
    
    private final Ruby runtime;
    
    // Is this thread currently with in a function trace?
    private boolean isWithinTrace;
    
    // Is this thread currently doing an defined? defined should set things like $!
    private boolean isWithinDefined;
    
    private RubyThread thread;
    private Fiber fiber;
    
    //private UnsynchronizedStack parentStack;
    private RubyModule[] parentStack = new RubyModule[INITIAL_SIZE];
    private int parentIndex = -1;
    
    //private UnsynchronizedStack frameStack;
    private Frame[] frameStack = new Frame[INITIAL_SIZE];
    private int frameIndex = -1;
    
    // List of active dynamic scopes.  Each of these may have captured other dynamic scopes
    // to implement closures.
    private DynamicScope[] scopeStack = new DynamicScope[INITIAL_SIZE];
    private int scopeIndex = -1;
    
    private CatchTarget[] catchStack = new CatchTarget[INITIAL_SIZE];
    private int catchIndex = -1;
    
    // File where current executing unit is being evaluated
    private String file = "";
    
    // Line where current executing unit is being evaluated
    private int line = 0;

    // In certain places, like grep, we don't use real frames for the
    // call blocks. This has the effect of not setting the backref in
    // the correct frame - this delta is activated to the place where
    // the grep is running in so that the backref will be set in an
    // appropriate place.
    private int rubyFrameDelta = 0;
    
    /**
     * Constructor for Context.
     */
    private ThreadContext(Ruby runtime) {
        this.runtime = runtime;
        
        // TOPLEVEL self and a few others want a top-level scope.  We create this one right
        // away and then pass it into top-level parse so it ends up being the top level.
        StaticScope topStaticScope = new LocalStaticScope(null);
        pushScope(new ManyVarsDynamicScope(topStaticScope, null));
            
        for (int i = 0; i < frameStack.length; i++) {
            frameStack[i] = new Frame();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        thread.dispose();
    }
    
    CallType lastCallType;
    
    Visibility lastVisibility;
    
    IRubyObject lastExitStatus;
    
    public final Ruby getRuntime() {
        return runtime;
    }
    
    public IRubyObject getErrorInfo() {
        return thread.getErrorInfo();
    }
    
    public IRubyObject setErrorInfo(IRubyObject errorInfo) {
        thread.setErrorInfo(errorInfo);
        return errorInfo;
    }
    
    public ReturnJump returnJump(IRubyObject value) {
        return new ReturnJump(getFrameJumpTarget(), value);
    }
    
    /**
     * Returns the lastCallStatus.
     * @return LastCallStatus
     */
    public void setLastCallStatus(CallType callType) {
        lastCallType = callType;
    }

    public CallType getLastCallType() {
        return lastCallType;
    }

    public void setLastVisibility(Visibility visibility) {
        lastVisibility = visibility;
    }

    public Visibility getLastVisibility() {
        return lastVisibility;
    }
    
    public void setLastCallStatusAndVisibility(CallType callType, Visibility visibility) {
        lastCallType = callType;
        lastVisibility = visibility;
    }
    
    public IRubyObject getLastExitStatus() {
        return lastExitStatus;
    }
    
    public void setLastExitStatus(IRubyObject lastExitStatus) {
        this.lastExitStatus = lastExitStatus;
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
    
    public DynamicScope getPreviousScope() {
        return scopeStack[scopeIndex - 1];
    }
    
    private void expandFramesIfNecessary() {
        int newSize = frameStack.length * 2;
        frameStack = fillNewFrameStack(new Frame[newSize], newSize);
    }

    private Frame[] fillNewFrameStack(Frame[] newFrameStack, int newSize) {
        System.arraycopy(frameStack, 0, newFrameStack, 0, frameStack.length);

        for (int i = frameStack.length; i < newSize; i++) {
            newFrameStack[i] = new Frame();
        }
        
        return newFrameStack;
    }
    
    private void expandParentsIfNecessary() {
        int newSize = parentStack.length * 2;
        RubyModule[] newParentStack = new RubyModule[newSize];

        System.arraycopy(parentStack, 0, newParentStack, 0, parentStack.length);

        parentStack = newParentStack;
    }
    
    public void pushScope(DynamicScope scope) {
        int index = ++scopeIndex;
        DynamicScope[] stack = scopeStack;
        stack[index] = scope;
        if (index + 1 == stack.length) {
            expandScopesIfNecessary();
        }
    }
    
    public void popScope() {
        scopeStack[scopeIndex--] = null;
    }
    
    private void expandScopesIfNecessary() {
        int newSize = scopeStack.length * 2;
        DynamicScope[] newScopeStack = new DynamicScope[newSize];

        System.arraycopy(scopeStack, 0, newScopeStack, 0, scopeStack.length);

        scopeStack = newScopeStack;
    }
    
    public RubyThread getThread() {
        return thread;
    }
    
    public void setThread(RubyThread thread) {
        this.thread = thread;
    }
    
    public Fiber getFiber() {
        return fiber;
    }
    
    public void setFiber(Fiber fiber) {
        this.fiber = fiber;
    }
    
//    public IRubyObject getLastline() {
//        IRubyObject value = getCurrentScope().getLastLine();
//        
//        // DynamicScope does not preinitialize these values since they are virtually never used.
//        return value == null ? runtime.getNil() : value;
//    }
//    
//    public void setLastline(IRubyObject value) {
//        getCurrentScope().setLastLine(value);
//    }
    
    //////////////////// CATCH MANAGEMENT ////////////////////////
    private void expandCatchIfNecessary() {
        int newSize = catchStack.length * 2;
        CatchTarget[] newCatchStack = new CatchTarget[newSize];

        System.arraycopy(catchStack, 0, newCatchStack, 0, catchStack.length);
        catchStack = newCatchStack;
    }
    
    public void pushCatch(CatchTarget catchTarget) {
        int index = ++catchIndex;
        CatchTarget[] stack = catchStack;
        stack[index] = catchTarget;
        if (index + 1 == stack.length) {
            expandCatchIfNecessary();
        }
    }
    
    public void popCatch() {
        catchIndex--;
    }
    
    public CatchTarget[] getActiveCatches() {
        int index = catchIndex;
        if (index < 0) return new CatchTarget[0];
        
        CatchTarget[] activeCatches = new CatchTarget[index + 1];
        System.arraycopy(catchStack, 0, activeCatches, 0, index + 1);
        return activeCatches;
    }
    
    //////////////////// FRAME MANAGEMENT ////////////////////////
    private void pushFrameCopy() {
        int index = ++this.frameIndex;
        Frame[] stack = frameStack;
        Frame currentFrame = stack[index - 1];
        stack[index].updateFrame(currentFrame);
        if (index + 1 == stack.length) {
            expandFramesIfNecessary();
        }
    }
    
    private Frame pushFrame(Frame frame) {
        int index = ++this.frameIndex;
        Frame[] stack = frameStack;
        stack[index] = frame;
        if (index + 1 == stack.length) {
            expandFramesIfNecessary();
        }
        return frame;
    }
    
    private void pushCallFrame(RubyModule clazz, String name, 
                               IRubyObject self, Block block) {
        int index = ++this.frameIndex;
        Frame[] stack = frameStack;
        stack[index].updateFrame(clazz, self, name, block, file, line);
        if (index + 1 == stack.length) {
            expandFramesIfNecessary();
        }
    }
    
    private void pushEvalFrame(IRubyObject self) {
        int index = ++this.frameIndex;
        Frame[] stack = frameStack;
        stack[index].updateFrameForEval(self, file, line);
        if (index + 1 == stack.length) {
            expandFramesIfNecessary();
        }
    }
    
    private void pushBacktraceFrame(String name) {
        pushFrame(name);        
    }
    
    private void pushFrame(String name) {
        int index = ++this.frameIndex;
        Frame[] stack = frameStack;
        stack[index].updateFrame(name, file, line);
        if (index + 1 == stack.length) {
            expandFramesIfNecessary();
        }
    }
    
    private void pushFrame() {
        int index = ++this.frameIndex;
        Frame[] stack = frameStack;
        stack[index].updateFrame(file, line);
        if (index + 1 == stack.length) {
            expandFramesIfNecessary();
        }
    }
    
    private void popFrame() {
        Frame frame = frameStack[frameIndex--];
        setFileAndLine(frame);
        
        frame.clear();
    }
        
    private void popFrameReal(Frame oldFrame) {
        int index = frameIndex;
        Frame frame = frameStack[index];
        frameStack[index] = oldFrame;
        frameIndex = index - 1;
        setFileAndLine(frame);
    }
    
    public Frame getCurrentFrame() {
        return frameStack[frameIndex];
    }

    public int getRubyFrameDelta() {
        return this.rubyFrameDelta;
    }
    
    public void setRubyFrameDelta(int newDelta) {
        this.rubyFrameDelta = newDelta;
    }

    public Frame getCurrentRubyFrame() {
        return frameStack[frameIndex-rubyFrameDelta];
    }
    
    public Frame getNextFrame() {
        int index = frameIndex;
        Frame[] stack = frameStack;
        if (index + 1 == stack.length) {
            expandFramesIfNecessary();
        }
        return stack[index + 1];
    }
    
    public Frame getPreviousFrame() {
        int index = frameIndex;
        return index < 1 ? null : frameStack[index - 1];
    }
    
    public int getFrameCount() {
        return frameIndex + 1;
    }

    public Frame[] getFrames(int delta) {
        int top = frameIndex + delta;
        Frame[] frames = new Frame[top + 1];
        for (int i = 0; i <= top; i++) {
            frames[i] = frameStack[i].duplicateForBacktrace();
        }
        return frames;
    }
    
    public String getFrameName() {
        return getCurrentFrame().getName();
    }
    
    public IRubyObject getFrameSelf() {
        return getCurrentFrame().getSelf();
    }
    
    public JumpTarget getFrameJumpTarget() {
        return getCurrentFrame().getJumpTarget();
    }
    
    @Deprecated
    public void setFrameJumpTarget(JumpTarget target) {
    }
    
    public RubyModule getFrameKlazz() {
        return getCurrentFrame().getKlazz();
    }
    
    public Block getFrameBlock() {
        return getCurrentFrame().getBlock();
    }
    
    public String getFile() {
        return file;
    }
    
    public int getLine() {
        return line;
    }
    
    public void setFile(String file) {
        this.file = file;
    }
    
    public void setLine(int line) {
        this.line = line;
    }
    
    public void setFileAndLine(String file, int line) {
        this.file = file;
        this.line = line;
    }
    
    public void setFileAndLine(Frame frame) {
        this.file = frame.getFile();
        this.line = frame.getLine();
    }

    public void setFileAndLine(ISourcePosition position) {
        this.file = position.getFile();
        this.line = position.getStartLine();
    }
    
    public Visibility getCurrentVisibility() {
        return getCurrentFrame().getVisibility();
    }
    
    public Visibility getPreviousVisibility() {
        return getPreviousFrame().getVisibility();
    }
    
    public void setCurrentVisibility(Visibility visibility) {
        getCurrentFrame().setVisibility(visibility);
    }
    
    public void pollThreadEvents() {
        thread.pollThreadEvents(this);
    }
    
    int calls = 0;
    
    public void callThreadPoll() {
        if ((calls++ & 0xFF) == 0) pollThreadEvents();
    }
    
    public void trace(RubyEvent event, String name, RubyModule implClass) {
        runtime.callEventHooks(this, event, file, line, name, implClass);
    }
    
    public void pushRubyClass(RubyModule currentModule) {
        // FIXME: this seems like a good assertion, but it breaks compiled code and the code seems
        // to run without it...
        //assert currentModule != null : "Can't push null RubyClass";
        
        int index = ++parentIndex;
        RubyModule[] stack = parentStack;
        stack[index] = currentModule;
        if (index + 1 == stack.length) {
            expandParentsIfNecessary();
        }
    }
    
    public RubyModule popRubyClass() {
        int index = parentIndex;
        RubyModule[] stack = parentStack;
        RubyModule ret = stack[index];
        stack[index] = null;
        parentIndex = index - 1;
        return ret;
    }
    
    public RubyModule getRubyClass() {
        assert parentIndex != -1 : "Trying to get RubyClass from empty stack";
        RubyModule parentModule = parentStack[parentIndex];
        return parentModule.getNonIncludedClass();
    }

    public RubyModule getPreviousRubyClass() {
        assert parentIndex != 0 : "Trying to get RubyClass from too-shallow stack";
        RubyModule parentModule = parentStack[parentIndex - 1];
        return parentModule.getNonIncludedClass();
    }
    
    public boolean getConstantDefined(String internedName) {
        IRubyObject value = getConstant(internedName);

        return value != null;
    }
    
    /**
     * Used by the evaluator and the compiler to look up a constant by name
     */
    public IRubyObject getConstant(String internedName) {
        return getCurrentScope().getStaticScope().getConstant(runtime, internedName, runtime.getObject());
    }
    
    /**
     * Used by the evaluator and the compiler to set a constant by name
     * This is for a null const decl
     */
    public IRubyObject setConstantInCurrent(String internedName, IRubyObject result) {
        RubyModule module;

        if ((module = getCurrentScope().getStaticScope().getModule()) != null) {
            module.fastSetConstant(internedName, result);
            return result;
        }

        // TODO: wire into new exception handling mechanism
        throw runtime.newTypeError("no class/module to define constant");
    }
    
    /**
     * Used by the evaluator and the compiler to set a constant by name.
     * This is for a Colon2 const decl
     */
    public IRubyObject setConstantInModule(String internedName, IRubyObject target, IRubyObject result) {
        if (!(target instanceof RubyModule)) {
            throw runtime.newTypeError(target.toString() + " is not a class/module");
        }
        RubyModule module = (RubyModule)target;
        module.fastSetConstant(internedName, result);
        
        return result;
    }
    
    /**
     * Used by the evaluator and the compiler to set a constant by name
     * This is for a Colon2 const decl
     */
    public IRubyObject setConstantInObject(String internedName, IRubyObject result) {
        runtime.getObject().fastSetConstant(internedName, result);
        
        return result;
    }
    
    @Deprecated
    private static void addBackTraceElement(RubyArray backtrace, RubyStackTraceElement frame, RubyStackTraceElement previousFrame) {
        addBackTraceElement(backtrace.getRuntime(), backtrace, frame, previousFrame);
    }
    
    private static void addBackTraceElement(Ruby runtime, RubyArray backtrace, Frame frame, Frame previousFrame) {
        if (frame != previousFrame && // happens with native exceptions, should not filter those out
                frame.getLine() == previousFrame.getLine() &&
                frame.getName() != null && 
                frame.getName().equals(previousFrame.getName()) &&
                frame.getFile().equals(previousFrame.getFile())) {
            return;
        }
        
        RubyString traceLine;
        if (previousFrame.getName() != null) {
            traceLine = RubyString.newString(runtime, frame.getFile() + ':' + (frame.getLine() + 1) + ":in `" + previousFrame.getName() + '\'');
        } else if (runtime.is1_9()) {
            // TODO: This probably isn't the best hack, but it works until we can have different
            // root frame setup for 1.9 easily.
            traceLine = RubyString.newString(runtime, frame.getFile() + ':' + (frame.getLine() + 1) + ":in `<main>'");
        } else {
            traceLine = RubyString.newString(runtime, frame.getFile() + ':' + (frame.getLine() + 1));
        }
        
        backtrace.append(traceLine);
    }
    
    private static void addBackTraceElement(Ruby runtime, RubyArray backtrace, RubyStackTraceElement frame, RubyStackTraceElement previousFrame) {
        if (frame != previousFrame && // happens with native exceptions, should not filter those out
                frame.getLineNumber() == previousFrame.getLineNumber() &&
                frame.getMethodName() != null &&
                frame.getMethodName().equals(previousFrame.getMethodName()) &&
                frame.getFileName() != null &&
                frame.getFileName().equals(previousFrame.getFileName())) {
            return;
        }
        
        RubyString traceLine;
        String fileName = frame.getFileName();
        if (fileName == null) fileName = "";
        if (previousFrame.getMethodName() == UNKNOWN_NAME) {
            traceLine = RubyString.newString(runtime, fileName + ':' + (frame.getLineNumber()));
        } else {
            traceLine = RubyString.newString(runtime, fileName + ':' + (frame.getLineNumber()) + ":in `" + previousFrame.getMethodName() + '\'');
        }
        
        backtrace.append(traceLine);
    }
    
    private static void addBackTraceElement(RubyArray backtrace, RubyStackTraceElement frame, RubyStackTraceElement previousFrame, FrameType frameType) {
        if (frame != previousFrame && // happens with native exceptions, should not filter those out
                frame.getMethodName() != null && 
                frame.getMethodName().equals(previousFrame.getMethodName()) &&
                frame.getFileName().equals(previousFrame.getFileName()) &&
                frame.getLineNumber() == previousFrame.getLineNumber()) {
            return;
        }
        
        StringBuilder buf = new StringBuilder(60);
        buf.append(frame.getFileName()).append(':').append(frame.getLineNumber());
        
        if (previousFrame.getMethodName() != null) {
            switch (frameType) {
            case METHOD:
                buf.append(":in `");
                buf.append(previousFrame.getMethodName());
                buf.append('\'');
                break;
            case BLOCK:
                buf.append(":in `");
                buf.append("block in " + previousFrame.getMethodName());
                buf.append('\'');
                break;
            case EVAL:
                buf.append(":in `");
                buf.append("eval in " + previousFrame.getMethodName());
                buf.append('\'');
                break;
            case CLASS:
                buf.append(":in `");
                buf.append("class in " + previousFrame.getMethodName());
                buf.append('\'');
                break;
            case ROOT:
                buf.append(":in `<toplevel>'");
                break;
            }
        }
        
        backtrace.append(backtrace.getRuntime().newString(buf.toString()));
    }
    
    /**
     * Create an Array with backtrace information.
     * @param runtime
     * @param level
     * @param nativeException
     * @return an Array with the backtrace
     */
    public static IRubyObject createBacktraceFromFrames(Ruby runtime, RubyStackTraceElement[] backtraceFrames) {
        return createBacktraceFromFrames(runtime, backtraceFrames, true);
    }
    
    /**
     * Create an Array with backtrace information.
     * @param runtime
     * @param level
     * @param nativeException
     * @return an Array with the backtrace
     */
    public IRubyObject createCallerBacktrace(Ruby runtime, int level) {
        int traceSize = frameIndex - level + 1;
        RubyArray backtrace = runtime.newArray(traceSize);

        for (int i = traceSize - 1; i > 0; i--) {
            addBackTraceElement(runtime, backtrace, frameStack[i], frameStack[i - 1]);
        }
        
        return backtrace;
    }
    
    /**
     * Create an Array with backtrace information.
     * @param runtime
     * @param level
     * @param nativeException
     * @return an Array with the backtrace
     */
    public static IRubyObject createBacktraceFromFrames(Ruby runtime, RubyStackTraceElement[] backtraceFrames, boolean cropAtEval) {
        RubyArray backtrace = runtime.newArray();
        
        if (backtraceFrames == null || backtraceFrames.length <= 0) return backtrace;
        
        int traceSize = backtraceFrames.length;

        for (int i = 0; i < traceSize - 1; i++) {
            RubyStackTraceElement frame = backtraceFrames[i];
            // We are in eval with binding break out early
            // FIXME: This is broken with the new backtrace stuff
            if (cropAtEval && frame.isBinding()) break;

            addBackTraceElement(runtime, backtrace, frame, backtraceFrames[i + 1]);
        }
        
        return backtrace;
    }
    
    /**
     * Create an Array with backtrace information.
     * @param runtime
     * @param level
     * @param nativeException
     * @return an Array with the backtrace
     */
    public Frame[] createBacktrace(int level, boolean nativeException) {
        int traceSize = frameIndex - level + 1;
        Frame[] traceFrames;
        
        if (traceSize <= 0) return null;
        
        if (nativeException) {
            // assert level == 0;
            traceFrames = new Frame[traceSize + 1];
            traceFrames[traceSize] = frameStack[frameIndex];
        } else {
            traceFrames = new Frame[traceSize];
        }
        
        System.arraycopy(frameStack, 0, traceFrames, 0, traceSize);
        
        return traceFrames;
    }

    public static class RubyStackTraceElement {
        private StackTraceElement element;
        private boolean binding;

        public RubyStackTraceElement(String cls, String method, String file, int line, boolean binding) {
            element = new StackTraceElement(cls, method, file, line);
            this.binding = binding;
        }

        public StackTraceElement getElement() {
            return element;
        }

        public boolean isBinding() {
            return binding;
        }

        public String getClassName() {
            return element.getClassName();
        }

        public String getFileName() {
            return element.getFileName();
        }

        public int getLineNumber() {
            return element.getLineNumber();
        }

        public String getMethodName() {
            return element.getMethodName();
        }
    }
    
    /**
     * Create an Array with backtrace information.
     * @param runtime
     * @param level
     * @param nativeException
     * @return an Array with the backtrace
     */
    public RubyStackTraceElement[] createBacktrace2(int level, boolean nativeException) {
        int traceSize = frameIndex - level + 1;
        RubyStackTraceElement[] newTrace;
        
        if (traceSize <= 0) return null;

        int totalSize = traceSize;
        if (nativeException) {
            // assert level == 0;
            totalSize = traceSize + 1;
        }
        newTrace = new RubyStackTraceElement[totalSize];

        return buildTrace(newTrace);
    }

    private RubyStackTraceElement[] buildTrace(RubyStackTraceElement[] newTrace) {
        for (int i = 0; i < newTrace.length; i++) {
            Frame current = frameStack[i];
            String klazzName = getClassNameFromFrame(current);
            String methodName = getMethodNameFromFrame(current);
            newTrace[newTrace.length - 1 - i] = 
                    new RubyStackTraceElement(klazzName, methodName, current.getFile(), current.getLine() + 1, current.isBindingFrame());
        }
        
        return newTrace;
    }

    private String getClassNameFromFrame(Frame current) {
        String klazzName;
        if (current.getKlazz() == null) {
            klazzName = UNKNOWN_NAME;
        } else {
            klazzName = current.getKlazz().getName();
        }
        return klazzName;
    }
    
    private String getMethodNameFromFrame(Frame current) {
        String methodName = current.getName();
        if (current.getName() == null) {
            methodName = UNKNOWN_NAME;
        }
        return methodName;
    }
    
    private static String createRubyBacktraceString(StackTraceElement element) {
        return element.getFileName() + ":" + element.getLineNumber() + ":in `" + element.getMethodName() + "'";
    }
    
    public static String createRawBacktraceStringFromThrowable(Throwable t) {
        StackTraceElement[] javaStackTrace = t.getStackTrace();
        
        StringBuffer buffer = new StringBuffer();
        if (javaStackTrace != null && javaStackTrace.length > 0) {
            StackTraceElement element = javaStackTrace[0];

            buffer
                    .append(createRubyBacktraceString(element))
                    .append(": ")
                    .append(t.toString())
                    .append("\n");
            for (int i = 1; i < javaStackTrace.length; i++) {
                element = javaStackTrace[i];
                
                buffer
                        .append("\tfrom ")
                        .append(createRubyBacktraceString(element));
                if (i + 1 < javaStackTrace.length) buffer.append("\n");
            }
        }
        
        return buffer.toString();
    }
    
    public static IRubyObject createRawBacktrace(Ruby runtime, StackTraceElement[] stackTrace, boolean filter) {
        RubyArray traceArray = RubyArray.newArray(runtime);
        for (int i = 0; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            
            if (filter) {
                if (element.getClassName().startsWith("org.jruby") ||
                        element.getLineNumber() < 0) {
                    continue;
                }
            }
            RubyString str = RubyString.newString(runtime, createRubyBacktraceString(element));
            traceArray.append(str);
        }
        
        return traceArray;
    }
    
    public static IRubyObject createRubyCompiledBacktrace(Ruby runtime, StackTraceElement[] stackTrace) {
        RubyArray traceArray = RubyArray.newArray(runtime);
        for (int i = 0; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            int index = element.getMethodName().indexOf("$RUBY$");
            if (index < 0) continue;
            String unmangledMethod = element.getMethodName().substring(index + 6);
            RubyString str = RubyString.newString(runtime, element.getFileName() + ":" + element.getLineNumber() + ":in `" + unmangledMethod + "'");
            traceArray.append(str);
        }
        
        return traceArray;
    }

    private Frame pushFrameForBlock(Binding binding) {
        Frame lastFrame = getNextFrame();
        Frame f = pushFrame(binding.getFrame());

        // set the binding's frame's "previous" file and line to current, so
        // trace will show who called the block
        f.setFileAndLine(file, line);
        
        setFileAndLine(binding.getFile(), binding.getLine());
        f.setVisibility(binding.getVisibility());
        
        return lastFrame;
    }

    private Frame pushFrameForEval(Binding binding) {
        Frame lastFrame = getNextFrame();
        Frame f = pushFrame(binding.getFrame());
        setFileAndLine(binding.getFile(), binding.getLine());
        f.setVisibility(binding.getVisibility());
        return lastFrame;
    }
    
    public enum FrameType { METHOD, BLOCK, EVAL, CLASS, ROOT }
    public static final Map<String, FrameType> INTERPRETED_FRAMES = new HashMap<String, FrameType>();
    
    static {
        INTERPRETED_FRAMES.put(DefaultMethod.class.getName() + ".interpretedCall", FrameType.METHOD);
        
        INTERPRETED_FRAMES.put(InterpretedBlock.class.getName() + ".evalBlockBody", FrameType.BLOCK);
        
        INTERPRETED_FRAMES.put(ASTInterpreter.class.getName() + ".evalWithBinding", FrameType.EVAL);
        INTERPRETED_FRAMES.put(ASTInterpreter.class.getName() + ".evalSimple", FrameType.EVAL);
        
        INTERPRETED_FRAMES.put(ASTInterpreter.class.getName() + ".evalClassDefinitionBody", FrameType.CLASS);
        
        INTERPRETED_FRAMES.put(Ruby.class.getName() + ".runInterpreter", FrameType.ROOT);
    }
    
    public static IRubyObject createRubyHybridBacktrace(Ruby runtime, RubyStackTraceElement[] backtraceFrames, RubyStackTraceElement[] stackTrace, boolean debug) {
        RubyArray traceArray = RubyArray.newArray(runtime);
        ThreadContext context = runtime.getCurrentContext();
        
        int rubyFrameIndex = backtraceFrames.length - 1;
        for (int i = 0; i < stackTrace.length; i++) {
            RubyStackTraceElement element = stackTrace[i];
            
            // look for mangling markers for compiled Ruby in method name
            int index = element.getMethodName().indexOf("$RUBY$");
            if (index >= 0) {
                String unmangledMethod = element.getMethodName().substring(index + 6);
                RubyString str = RubyString.newString(runtime, element.getFileName() + ":" + element.getLineNumber() + ":in `" + unmangledMethod + "'");
                traceArray.append(str);
                
                // if it's not a rescue or ensure, there's a frame associated, so decrement
                if (!(element.getMethodName().contains("__rescue__") || element.getMethodName().contains("__ensure__"))) {
                    rubyFrameIndex--;
                }
                continue;
            }
            
            // look for __file__ method name for compiled roots
            if (element.getMethodName().equals("__file__")) {
                RubyString str = RubyString.newString(runtime, element.getFileName() + ":" + element.getLineNumber() + ": `<toplevel>'");
                traceArray.append(str);
                rubyFrameIndex--;
                continue;
            }
            
            // look for mangling markers for bound, unframed methods in class name
            index = element.getClassName().indexOf("$RUBYINVOKER$");
            if (index >= 0) {
                // unframed invokers have no Ruby frames, so pull from class name
                // but use current frame as file and line
                String unmangledMethod = element.getClassName().substring(index + 13);
                Frame current = context.frameStack[rubyFrameIndex];
                RubyString str = RubyString.newString(runtime, current.getFile() + ":" + (current.getLine() + 1) + ":in `" + unmangledMethod + "'");
                traceArray.append(str);
                continue;
            }
            
            // look for mangling markers for bound, framed methods in class name
            index = element.getClassName().indexOf("$RUBYFRAMEDINVOKER$");
            if (index >= 0) {
                // framed invokers will have Ruby frames associated with them
                addBackTraceElement(traceArray, backtraceFrames[rubyFrameIndex], backtraceFrames[rubyFrameIndex - 1], FrameType.METHOD);
                rubyFrameIndex--;
                continue;
            }
            
            // try to mine out a Ruby frame using our list of interpreter entry-point markers
            String classMethod = element.getClassName() + "." + element.getMethodName();
            FrameType frameType = INTERPRETED_FRAMES.get(classMethod);
            if (frameType != null) {
                // Frame matches one of our markers for "interpreted" calls
                if (rubyFrameIndex == 0) {
                    addBackTraceElement(traceArray, backtraceFrames[rubyFrameIndex], backtraceFrames[rubyFrameIndex], frameType);
                } else {
                    addBackTraceElement(traceArray, backtraceFrames[rubyFrameIndex], backtraceFrames[rubyFrameIndex - 1], frameType);
                    rubyFrameIndex--;
                }
                continue;
            } else {
                // Frame is extraneous runtime information, skip it unless debug
                if (debug) {
                    RubyString str = RubyString.newString(runtime, createRubyBacktraceString(element.getElement()));
                    traceArray.append(str);
                }
                continue;
            }
        }
        
        return traceArray;
    }
    
    public void preAdoptThread() {
        pushFrame();
        pushRubyClass(runtime.getObject());
        getCurrentFrame().setSelf(runtime.getTopSelf());
    }
    
    public void preCompiledClass(RubyModule type, StaticScope staticScope) {
        pushRubyClass(type);
        pushFrameCopy();
        getCurrentFrame().setSelf(type);
        getCurrentFrame().setVisibility(Visibility.PUBLIC);
        staticScope.setModule(type);
        pushScope(DynamicScope.newDynamicScope(staticScope));
    }

    public void preCompiledClassDummyScope(RubyModule type, StaticScope staticScope) {
        pushRubyClass(type);
        pushFrameCopy();
        getCurrentFrame().setSelf(type);
        getCurrentFrame().setVisibility(Visibility.PUBLIC);
        staticScope.setModule(type);
        pushScope(staticScope.getDummyScope());
    }

    public void postCompiledClass() {
        popScope();
        popRubyClass();
        popFrame();
    }
    
    public void preScopeNode(StaticScope staticScope) {
        pushScope(DynamicScope.newDynamicScope(staticScope, getCurrentScope()));
    }

    public void postScopeNode() {
        popScope();
    }

    public void preClassEval(StaticScope staticScope, RubyModule type) {
        pushRubyClass(type);
        pushFrameCopy();
        getCurrentFrame().setSelf(type);
        getCurrentFrame().setVisibility(Visibility.PUBLIC);
        pushScope(DynamicScope.newDynamicScope(staticScope, null));
    }
    
    public void postClassEval() {
        popScope();
        popRubyClass();
        popFrame();
    }
    
    public void preBsfApply(String[] names) {
        // FIXME: I think we need these pushed somewhere?
        LocalStaticScope staticScope = new LocalStaticScope(null);
        staticScope.setVariables(names);
        pushFrame();
    }
    
    public void postBsfApply() {
        popFrame();
    }
    
    public void preMethodFrameAndScope(RubyModule clazz, String name, IRubyObject self, Block block, 
            StaticScope staticScope) {
        RubyModule implementationClass = staticScope.getModule();
        // FIXME: This is currently only here because of some problems with IOOutputStream writing to a "bare" runtime without a proper scope
        if (implementationClass == null) {
            implementationClass = clazz;
        }
        pushCallFrame(clazz, name, self, block);
        pushScope(DynamicScope.newDynamicScope(staticScope));
        pushRubyClass(implementationClass);
    }
    
    public void preMethodFrameAndDummyScope(RubyModule clazz, String name, IRubyObject self, Block block, 
            StaticScope staticScope) {
        RubyModule implementationClass = staticScope.getModule();
        // FIXME: This is currently only here because of some problems with IOOutputStream writing to a "bare" runtime without a proper scope
        if (implementationClass == null) {
            implementationClass = clazz;
        }
        pushCallFrame(clazz, name, self, block);
        pushScope(staticScope.getDummyScope());
        pushRubyClass(implementationClass);
    }

    public void preMethodNoFrameAndDummyScope(RubyModule clazz, StaticScope staticScope) {
        RubyModule implementationClass = staticScope.getModule();
        // FIXME: This is currently only here because of some problems with IOOutputStream writing to a "bare" runtime without a proper scope
        if (implementationClass == null) {
            implementationClass = clazz;
        }
        pushScope(staticScope.getDummyScope());
        pushRubyClass(implementationClass);
    }
    
    public void postMethodFrameAndScope() {
        popRubyClass();
        popScope();
        popFrame();
    }
    
    public void preMethodFrameOnly(RubyModule clazz, String name, IRubyObject self, Block block) {
        pushRubyClass(clazz);
        pushCallFrame(clazz, name, self, block);
    }
    
    public void postMethodFrameOnly() {
        popFrame();
        popRubyClass();
    }
    
    public void preMethodScopeOnly(RubyModule clazz, StaticScope staticScope) {
        RubyModule implementationClass = staticScope.getModule();
        // FIXME: This is currently only here because of some problems with IOOutputStream writing to a "bare" runtime without a proper scope
        if (implementationClass == null) {
            implementationClass = clazz;
        }
        pushScope(DynamicScope.newDynamicScope(staticScope));
        pushRubyClass(implementationClass);
    }
    
    public void postMethodScopeOnly() {
        popRubyClass();
        popScope();
    }
    
    public void preMethodBacktraceAndScope(String name, RubyModule clazz, StaticScope staticScope) {
        preMethodScopeOnly(clazz, staticScope);
        pushBacktraceFrame(name);
    }
    
    public void postMethodBacktraceAndScope() {
        postMethodScopeOnly();
        popFrame();
    }
    
    public void preMethodBacktraceOnly(String name) {
        pushBacktraceFrame(name);
    }

    public void preMethodBacktraceDummyScope(RubyModule clazz, String name, StaticScope staticScope) {
        pushBacktraceFrame(name);
        RubyModule implementationClass = staticScope.getModule();
        // FIXME: This is currently only here because of some problems with IOOutputStream writing to a "bare" runtime without a proper scope
        if (implementationClass == null) {
            implementationClass = clazz;
        }
        pushScope(staticScope.getDummyScope());
        pushRubyClass(implementationClass);
    }
    
    public void postMethodBacktraceOnly() {
        popFrame();
    }

    public void postMethodBacktraceDummyScope() {
        popFrame();
        popRubyClass();
        popScope();
    }
    
    public void prepareTopLevel(RubyClass objectClass, IRubyObject topSelf) {
        pushFrame();
        setCurrentVisibility(Visibility.PRIVATE);
        
        pushRubyClass(objectClass);
        
        Frame frame = getCurrentFrame();
        frame.setSelf(topSelf);
        
        getCurrentScope().getStaticScope().setModule(objectClass);
    }
    
    public void preNodeEval(RubyModule rubyClass, IRubyObject self, String name) {
        pushRubyClass(rubyClass);
        pushEvalFrame(self);
    }

    public void preNodeEval(RubyModule rubyClass, IRubyObject self) {
        pushRubyClass(rubyClass);
        pushEvalFrame(self);
    }
    
    public void postNodeEval() {
        popFrame();
        popRubyClass();
    }
    
    // XXX: Again, screwy evaling under previous frame's scope
    public void preExecuteUnder(RubyModule executeUnderClass, Block block) {
        Frame frame = getCurrentFrame();
        
        pushRubyClass(executeUnderClass);
        DynamicScope scope = getCurrentScope();
        StaticScope sScope = new BlockStaticScope(scope.getStaticScope());
        sScope.setModule(executeUnderClass);
        pushScope(DynamicScope.newDynamicScope(sScope, scope));
        pushCallFrame(frame.getKlazz(), frame.getName(), frame.getSelf(), block);
        getCurrentFrame().setVisibility(getPreviousFrame().getVisibility());
    }
    
    public void postExecuteUnder() {
        popFrame();
        popScope();
        popRubyClass();
    }
    
    public void preMproc() {
        pushFrame();
    }
    
    public void postMproc() {
        popFrame();
    }
    
    public void preRunThread(Frame[] currentFrames) {
        for (Frame frame : currentFrames) {
            pushFrame(frame);
        }
        setFileAndLine(getCurrentFrame());
    }
    
    public void preTrace() {
        setWithinTrace(true);
        pushFrame();
    }
    
    public void postTrace() {
        popFrame();
        setWithinTrace(false);
    }
    
    public Frame preForBlock(Binding binding, RubyModule klass) {
        Frame lastFrame = preYieldNoScope(binding, klass);
        pushScope(binding.getDynamicScope());
        return lastFrame;
    }
    
    public Frame preYieldSpecificBlock(Binding binding, StaticScope scope, RubyModule klass) {
        Frame lastFrame = preYieldNoScope(binding, klass);
        // new scope for this invocation of the block, based on parent scope
        pushScope(DynamicScope.newDynamicScope(scope, binding.getDynamicScope()));
        return lastFrame;
    }
    
    public Frame preYieldLightBlock(Binding binding, DynamicScope emptyScope, RubyModule klass) {
        Frame lastFrame = preYieldNoScope(binding, klass);
        // just push the same empty scope, since we won't use one
        pushScope(emptyScope);
        return lastFrame;
    }
    
    public Frame preYieldNoScope(Binding binding, RubyModule klass) {
        pushRubyClass((klass != null) ? klass : binding.getKlass());
        return pushFrameForBlock(binding);
    }
    
    public void preEvalScriptlet(DynamicScope scope) {
        pushScope(scope);
    }
    
    public void postEvalScriptlet() {
        popScope();
    }
    
    public Frame preEvalWithBinding(Binding binding) {
        binding.getFrame().setIsBindingFrame(true);
        Frame lastFrame = pushFrameForEval(binding);
        pushRubyClass(binding.getKlass());
        return lastFrame;
    }
    
    public void postEvalWithBinding(Binding binding, Frame lastFrame) {
        binding.getFrame().setIsBindingFrame(false);
        popFrameReal(lastFrame);
        popRubyClass();
    }
    
    public void postYield(Binding binding, Frame lastFrame) {
        popScope();
        popFrameReal(lastFrame);
        popRubyClass();
    }
    
    public void postYieldLight(Binding binding, Frame lastFrame) {
        popScope();
        popFrameReal(lastFrame);
        popRubyClass();
    }
    
    public void postYieldNoScope(Frame lastFrame) {
        popFrameReal(lastFrame);
        popRubyClass();
    }
    
    public void preScopedBody(DynamicScope scope) {
        pushScope(scope);
    }
    
    public void postScopedBody() {
        popScope();
    }
    
    /**
     * Is this thread actively tracing at this moment.
     *
     * @return true if so
     * @see org.jruby.Ruby#callTraceFunction(String, ISourcePosition, IRubyObject, String, IRubyObject)
     */
    public boolean isWithinTrace() {
        return isWithinTrace;
    }
    
    /**
     * Set whether we are actively tracing or not on this thread.
     *
     * @param isWithinTrace true is so
     * @see org.jruby.Ruby#callTraceFunction(String, ISourcePosition, IRubyObject, String, IRubyObject)
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

    /**
     * Return a binding representing the current call's state
     * @return the current binding
     */
    public Binding currentBinding() {
        Frame frame = getCurrentFrame();
        return new Binding(frame, getRubyClass(), getCurrentScope(), file, line);
    }

    /**
     * Return a binding representing the current call's state but with a specified self
     * @param self the self object to use
     * @return the current binding, using the specified self
     */
    public Binding currentBinding(IRubyObject self) {
        Frame frame = getCurrentFrame();
        return new Binding(self, frame, frame.getVisibility(), getRubyClass(), getCurrentScope(), file, line);
    }

    /**
     * Return a binding representing the current call's state but with the
     * specified visibility and self.
     * @param self the self object to use
     * @param visibility the visibility to use
     * @return the current binding using the specified self and visibility
     */
    public Binding currentBinding(IRubyObject self, Visibility visibility) {
        Frame frame = getCurrentFrame();
        return new Binding(self, frame, visibility, getRubyClass(), getCurrentScope(), file, line);
    }

    /**
     * Return a binding representing the current call's state but with the
     * specified scope and self.
     * @param self the self object to use
     * @param visibility the scope to use
     * @return the current binding using the specified self and scope
     */
    public Binding currentBinding(IRubyObject self, DynamicScope scope) {
        Frame frame = getCurrentFrame();
        return new Binding(self, frame, frame.getVisibility(), getRubyClass(), scope, file, line);
    }

    /**
     * Return a binding representing the current call's state but with the
     * specified visibility, scope, and self. For shared-scope binding
     * consumers like for loops.
     * 
     * @param self the self object to use
     * @param visibility the visibility to use
     * @param scope the scope to use
     * @return the current binding using the specified self, scope, and visibility
     */
    public Binding currentBinding(IRubyObject self, Visibility visibility, DynamicScope scope) {
        Frame frame = getCurrentFrame();
        return new Binding(self, frame, visibility, getRubyClass(), scope, file, line);
    }

    /**
     * Return a binding representing the previous call's state
     * @return the current binding
     */
    public Binding previousBinding() {
        Frame frame = getPreviousFrame();
        Frame current = getCurrentFrame();
        return new Binding(frame, getPreviousRubyClass(), getCurrentScope(), current.getFile(), current.getLine());
    }

    /**
     * Return a binding representing the previous call's state but with a specified self
     * @param self the self object to use
     * @return the current binding, using the specified self
     */
    public Binding previousBinding(IRubyObject self) {
        Frame frame = getPreviousFrame();
        Frame current = getCurrentFrame();
        return new Binding(self, frame, frame.getVisibility(), getPreviousRubyClass(), getCurrentScope(), current.getFile(), current.getLine());
    }
}
