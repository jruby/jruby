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

import org.jruby.runtime.profile.IProfileData;
import java.util.ArrayList;
import org.jruby.runtime.profile.ProfileData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jruby.runtime.scope.ManyVarsDynamicScope;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyContinuation.Continuation;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.RubyThread;
import org.jruby.ast.executable.RuntimeCache;
import org.jruby.compiler.JITCompiler;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.exceptions.JumpException.ReturnJump;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.libraries.FiberLibrary.Fiber;
import org.jruby.parser.BlockStaticScope;
import org.jruby.parser.LocalStaticScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JavaNameMangler;

public final class ThreadContext {
    public static ThreadContext newContext(Ruby runtime) {
        ThreadContext context = new ThreadContext(runtime);
        return context;
    }
    
    private final static int INITIAL_SIZE = 10;
    private final static int INITIAL_FRAMES_SIZE = 10;
    
    /** The number of calls after which to do a thread event poll */
    private final static int CALL_POLL_COUNT = 0xFFF;

    // runtime, nil, and runtimeCache cached here for speed of access from any thread
    public final Ruby runtime;
    public final IRubyObject nil;
    public final RuntimeCache runtimeCache;
    
    // Is this thread currently with in a function trace?
    private boolean isWithinTrace;
    
    // Is this thread currently doing an defined? defined should set things like $!
    private boolean isWithinDefined;
    
    private RubyThread thread;
    private Fiber fiber;
    
    private RubyModule[] parentStack = new RubyModule[INITIAL_SIZE];
    private int parentIndex = -1;
    
    private Frame[] frameStack = new Frame[INITIAL_FRAMES_SIZE];
    private int frameIndex = -1;

    private Backtrace[] backtrace = new Backtrace[INITIAL_FRAMES_SIZE];
    private int backtraceIndex = -1;

    public static class Backtrace {
        public Backtrace() {
        }
        public Backtrace(String klass, String method, String filename, int line) {
            this.method = method;
            this.filename = filename;
            this.line = line;
            this.klass = klass;
        }
        @Override
        public String toString() {
            return klass + "#" + method + " at " + filename + ":" + line;
        }
        @Override
        public Backtrace clone() {
            return new Backtrace(klass, method, filename, line);
        }
        public static void update(Backtrace backtrace, String klass, String method, ISourcePosition position) {
            backtrace.method = method;
            backtrace.filename = position.getFile();
            backtrace.line = position.getLine();
            backtrace.klass = klass;
        }
        public static void update(Backtrace backtrace, String klass, String method, String file, int line) {
            backtrace.method = method;
            backtrace.filename = file;
            backtrace.line = line;
            backtrace.klass = klass;
        }
        
        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getKlass() {
            return klass;
        }

        public void setKlass(String klass) {
            this.klass = klass;
        }

        public int getLine() {
            return line;
        }

        public void setLine(int line) {
            this.line = line;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }
        public String klass;
        public String method;
        public String filename;
        public int line;
    }
    
    // List of active dynamic scopes.  Each of these may have captured other dynamic scopes
    // to implement closures.
    private DynamicScope[] scopeStack = new DynamicScope[INITIAL_SIZE];
    private int scopeIndex = -1;

    private static final Continuation[] EMPTY_CATCHTARGET_STACK = new Continuation[0];
    private Continuation[] catchStack = EMPTY_CATCHTARGET_STACK;
    private int catchIndex = -1;
    
    private boolean isProfiling = false;
    // The flat profile data for this thread
	private IProfileData profileData;
	
    // In certain places, like grep, we don't use real frames for the
    // call blocks. This has the effect of not setting the backref in
    // the correct frame - this delta is activated to the place where
    // the grep is running in so that the backref will be set in an
    // appropriate place.
    private int rubyFrameDelta = 0;
    private boolean eventHooksEnabled = true;
    
    /**
     * Constructor for Context.
     */
    private ThreadContext(Ruby runtime) {
        this.runtime = runtime;
        this.nil = runtime.getNil();
        if (runtime.getInstanceConfig().isProfilingEntireRun())
            startProfiling();

        this.runtimeCache = runtime.getRuntimeCache();
        
        // TOPLEVEL self and a few others want a top-level scope.  We create this one right
        // away and then pass it into top-level parse so it ends up being the top level.
        StaticScope topStaticScope = new LocalStaticScope(null);
        pushScope(new ManyVarsDynamicScope(topStaticScope, null));

        Frame[] stack = frameStack;
        int length = stack.length;
        for (int i = 0; i < length; i++) {
            stack[i] = new Frame();
        }
        Backtrace[] stack2 = backtrace;
        int length2 = stack2.length;
        for (int i = 0; i < length2; i++) {
            stack2[i] = new Backtrace();
        }
        ThreadContext.pushBacktrace(this, "", "", "", 0);
        ThreadContext.pushBacktrace(this, "", "", "", 0);
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

        // associate the thread with this context, unless we're clearing the reference
        if (thread != null) {
            thread.setContext(this);
        }
    }
    
    public Fiber getFiber() {
        return fiber;
    }
    
    public void setFiber(Fiber fiber) {
        this.fiber = fiber;
    }
    
    //////////////////// CATCH MANAGEMENT ////////////////////////
    private void expandCatchIfNecessary() {
        int newSize = catchStack.length * 2;
        if (newSize == 0) newSize = 1;
        Continuation[] newCatchStack = new Continuation[newSize];

        System.arraycopy(catchStack, 0, newCatchStack, 0, catchStack.length);
        catchStack = newCatchStack;
    }
    
    public void pushCatch(Continuation catchTarget) {
        int index = ++catchIndex;
        if (index == catchStack.length) {
            expandCatchIfNecessary();
        }
        catchStack[index] = catchTarget;
    }
    
    public void popCatch() {
        catchIndex--;
    }

    /**
     * Find the active Continuation for the given tag. Must be called with an
     * interned string.
     *
     * @param tag The interned string to search for
     * @return The continuation associated with this tag
     */
    public Continuation getActiveCatch(Object tag) {
        for (int i = catchIndex; i >= 0; i--) {
            Continuation c = catchStack[i];
            if (runtime.is1_9()) {
                if (c.tag == tag) return c;
            } else {
                if (c.tag.equals(tag)) return c;
            }
        }
        return null;
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
        stack[index].updateFrame(clazz, self, name, block, callNumber);
        if (index + 1 == stack.length) {
            expandFramesIfNecessary();
        }
    }
    
    private void pushEvalFrame(IRubyObject self) {
        int index = ++this.frameIndex;
        Frame[] stack = frameStack;
        stack[index].updateFrameForEval(self, callNumber);
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
        stack[index].updateFrame(name);
        if (index + 1 == stack.length) {
            expandFramesIfNecessary();
        }
    }
    
    public void pushFrame() {
        int index = ++this.frameIndex;
        Frame[] stack = frameStack;
        if (index + 1 == stack.length) {
            expandFramesIfNecessary();
        }
    }
    
    public void popFrame() {
        Frame frame = frameStack[frameIndex--];
        
        frame.clear();
    }
        
    private void popFrameReal(Frame oldFrame) {
        frameStack[frameIndex--] = oldFrame;
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

    /////////////////// BACKTRACE ////////////////////

    private static void expandBacktraceIfNecessary(ThreadContext context) {
        int newSize = context.backtrace.length * 2;
        context.backtrace = fillNewBacktrace(context, new Backtrace[newSize], newSize);
    }

    private static Backtrace[] fillNewBacktrace(ThreadContext context, Backtrace[] newBacktrace, int newSize) {
        System.arraycopy(context.backtrace, 0, newBacktrace, 0, context.backtrace.length);

        for (int i = context.backtrace.length; i < newSize; i++) {
            newBacktrace[i] = new Backtrace();
        }

        return newBacktrace;
    }

    public static void pushBacktrace(ThreadContext context, String klass, String method, ISourcePosition position) {
        int index = ++context.backtraceIndex;
        Backtrace[] stack = context.backtrace;
        Backtrace.update(stack[index], klass, method, position);
        if (index + 1 == stack.length) {
            ThreadContext.expandBacktraceIfNecessary(context);
        }
    }

    public static void pushBacktrace(ThreadContext context, String klass, String method, String file, int line) {
        int index = ++context.backtraceIndex;
        Backtrace[] stack = context.backtrace;
        Backtrace.update(stack[index], klass, method, file, line);
        if (index + 1 == stack.length) {
            ThreadContext.expandBacktraceIfNecessary(context);
        }
    }

    public static void popBacktrace(ThreadContext context) {
        context.backtraceIndex--;
    }

    /**
     * Search the frame stack for the given JumpTarget. Return true if it is
     * found and false otherwise. Skip the given number of frames before
     * beginning the search.
     * 
     * @param target The JumpTarget to search for
     * @param skipFrames The number of frames to skip before searching
     * @return
     */
    public boolean isJumpTargetAlive(int target, int skipFrames) {
        for (int i = frameIndex - skipFrames; i >= 0; i--) {
            if (frameStack[i].getJumpTarget() == target) return true;
        }
        return false;
    }
    
    public String getFrameName() {
        return getCurrentFrame().getName();
    }
    
    public IRubyObject getFrameSelf() {
        return getCurrentFrame().getSelf();
    }
    
    public int getFrameJumpTarget() {
        return getCurrentFrame().getJumpTarget();
    }
    
    public RubyModule getFrameKlazz() {
        return getCurrentFrame().getKlazz();
    }
    
    public Block getFrameBlock() {
        return getCurrentFrame().getBlock();
    }
    
    public String getFile() {
        return backtrace[backtraceIndex].filename;
    }
    
    public int getLine() {
        return backtrace[backtraceIndex].line;
    }
    
    public void setFile(String file) {
        backtrace[backtraceIndex].filename = file;
    }
    
    public void setLine(int line) {
        backtrace[backtraceIndex].line = line;
    }
    
    public void setFileAndLine(String file, int line) {
        backtrace[backtraceIndex].filename = file;
        backtrace[backtraceIndex].line = line;
    }

    public void setFileAndLine(ISourcePosition position) {
        backtrace[backtraceIndex].filename = position.getFile();
        backtrace[backtraceIndex].line = position.getStartLine();
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
    
    public int callNumber = 0;

    public int getCurrentTarget() {
        return callNumber;
    }
    
    public void callThreadPoll() {
        if ((callNumber++ & CALL_POLL_COUNT) == 0) pollThreadEvents();
    }

    public static void callThreadPoll(ThreadContext context) {
        if ((context.callNumber++ & CALL_POLL_COUNT) == 0) context.pollThreadEvents();
    }
    
    public void trace(RubyEvent event, String name, RubyModule implClass) {
        trace(event, name, implClass, backtrace[backtraceIndex].filename, backtrace[backtraceIndex].line);
    }

    public void trace(RubyEvent event, String name, RubyModule implClass, String file, int line) {
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
    
    private static void addBackTraceElement(Ruby runtime, RubyArray backtrace, RubyStackTraceElement element) {
        RubyString str = RubyString.newString(runtime, element.getFileName() + ":" + element.getLineNumber() + ":in `" + element.getMethodName() + "'");
        backtrace.append(str);
    }
    
    /**
     * Create an Array with backtrace information.
     * @param runtime
     * @param level
     * @param nativeException
     * @return an Array with the backtrace
     */
    public IRubyObject createCallerBacktrace(Ruby runtime, int level) {
        RubyStackTraceElement[] trace = gatherCallerBacktrace(level);

        // scrub out .java core class names and replace with Ruby equivalents
        OUTER: for (int i = 0; i < trace.length; i++) {
            RubyStackTraceElement element = trace[i];
            String classDotMethod = element.getClassName() + "." + element.getMethodName();
            if (runtime.getBoundMethods().containsKey(classDotMethod)) {
                String rubyName = runtime.getBoundMethods().get(classDotMethod);
                // find first Ruby file+line
                RubyStackTraceElement rubyElement = null;
                for (int j = i; j < trace.length; j++) {
                    rubyElement = trace[j];
                    if (!rubyElement.getFileName().endsWith(".java")) {
                        trace[i] = new RubyStackTraceElement(
                                element.getClassName(),
                                rubyName,
                                rubyElement.getFileName(),
                                rubyElement.getLineNumber(),
                                rubyElement.isBinding(),
                                rubyElement.getFrameType());
                        continue OUTER;
                    }
                }
                // no match, leave it as is

            }
        }
        
        RubyArray backtrace = runtime.newArray(trace.length - level);

        for (int i = level; i < trace.length; i++) {
            addBackTraceElement(runtime, backtrace, trace[i]);
        }
        
        return backtrace;
    }
    
    public RubyStackTraceElement[] gatherCallerBacktrace(int level) {
        Thread nativeThread = thread.getNativeThread();

        // Future thread or otherwise unforthgiving thread impl.
        if (nativeThread == null) return new RubyStackTraceElement[] {};

        Backtrace[] copy = new Backtrace[backtraceIndex + 1];

        System.arraycopy(backtrace, 0, copy, 0, backtraceIndex + 1);
        RubyStackTraceElement[] trace = gatherHybridBacktrace(
                runtime,
                copy,
                nativeThread.getStackTrace(),
                false);

        return trace;
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

    public boolean isEventHooksEnabled() {
        return eventHooksEnabled;
    }

    public void setEventHooksEnabled(boolean flag) {
        eventHooksEnabled = flag;
    }

    public static class RubyStackTraceElement {
        private final StackTraceElement element;
        private final boolean binding;
        private final FrameType frameType;

        public RubyStackTraceElement(StackTraceElement element) {
            this.element = element;
            this.binding = false;
            this.frameType = FrameType.METHOD;
        }

        public RubyStackTraceElement(String cls, String method, String file, int line, boolean binding) {
            this(cls, method, file, line, binding, FrameType.METHOD);
        }

        public RubyStackTraceElement(String cls, String method, String file, int line, boolean binding, FrameType frameType) {
            this.element = new StackTraceElement(cls, method, file, line);
            this.binding = binding;
            this.frameType = frameType;
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

        public FrameType getFrameType() {
            return frameType;
        }

        public String toString() {
            return element.toString();
        }
    }
    
    /**
     * Create an Array with backtrace information.
     * @param runtime
     * @param level
     * @param nativeException
     * @return an Array with the backtrace
     */
    public Backtrace[] createBacktrace2(int level, boolean nativeException) {
        Backtrace[] newTrace = new Backtrace[backtraceIndex + 1];
        for (int i = 0; i <= backtraceIndex; i++) {
            newTrace[i] = backtrace[i].clone();
        }
        return newTrace;
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

    public static RubyStackTraceElement[] gatherRawBacktrace(Ruby runtime, StackTraceElement[] stackTrace) {
        List trace = new ArrayList(stackTrace.length);
        
        for (int i = 0; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            trace.add(new RubyStackTraceElement(element));
        }

        RubyStackTraceElement[] rubyStackTrace = new RubyStackTraceElement[trace.size()];
        return (RubyStackTraceElement[])trace.toArray(rubyStackTrace);
    }

    private Frame pushFrameForBlock(Binding binding) {
        Frame lastFrame = getNextFrame();
        Frame f = pushFrame(binding.getFrame());
        f.setVisibility(binding.getVisibility());
        
        return lastFrame;
    }

    private Frame pushFrameForEval(Binding binding) {
        Frame lastFrame = getNextFrame();
        Frame f = pushFrame(binding.getFrame());
        f.setVisibility(binding.getVisibility());
        return lastFrame;
    }
    
    public enum FrameType { METHOD, BLOCK, EVAL, CLASS, ROOT }
    public static final Map<String, FrameType> INTERPRETED_FRAMES = new HashMap<String, FrameType>();
    
    static {
        INTERPRETED_FRAMES.put(ASTInterpreter.class.getName() + ".INTERPRET_METHOD", FrameType.METHOD);
        INTERPRETED_FRAMES.put(ASTInterpreter.class.getName() + ".INTERPRET_EVAL", FrameType.EVAL);
        INTERPRETED_FRAMES.put(ASTInterpreter.class.getName() + ".INTERPRET_CLASS", FrameType.CLASS);
        INTERPRETED_FRAMES.put(ASTInterpreter.class.getName() + ".INTERPRET_BLOCK", FrameType.BLOCK);
        INTERPRETED_FRAMES.put(ASTInterpreter.class.getName() + ".INTERPRET_ROOT", FrameType.ROOT);
    }

    public static RubyStackTraceElement[] gatherHybridBacktrace(Ruby runtime, Backtrace[] backtraceFrames, StackTraceElement[] stackTrace, boolean fullTrace) {
        List trace = new ArrayList(stackTrace.length);

        // a running index into the Ruby backtrace stack, incremented for each
        // interpreter frame we encounter in the Java backtrace.
        int rubyFrameIndex = backtraceFrames == null ? -1 : backtraceFrames.length - 1;

        // no Java trace, can't generate hybrid trace
        // TODO: Perhaps just generate the interpreter trace? Is this path ever hit?
        if (stackTrace == null) return null;

        // walk the Java stack trace, peeling off Java elements or Ruby elements
        for (int i = 0; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];

            if (element.getFileName() != null &&
                    (element.getFileName().endsWith(".rb") ||
                    element.getFileName().equals("-e") ||
                    // FIXME: Formalize jitted method structure so this isn't quite as hacky
                    element.getClassName().startsWith(JITCompiler.RUBY_JIT_PREFIX) ||
                    element.getMethodName().contains("$RUBY$") ||
                    element.getMethodName().contains("__file__"))) {
                if (element.getLineNumber() == -1) continue;
                
                String methodName = element.getMethodName();
                String className = element.getClassName();

                // FIXME: Formalize jitted method structure so this isn't quite as hacky
                if (className.startsWith(JITCompiler.RUBY_JIT_PREFIX)) {
                    // pull out and demangle the method name
                    methodName = className.substring(JITCompiler.RUBY_JIT_PREFIX.length() + 1, className.lastIndexOf("_"));
                    methodName = JavaNameMangler.demangleMethodName(methodName);
                    
                    trace.add(new RubyStackTraceElement(className, methodName, element.getFileName(), element.getLineNumber(), false));

                    // if it's a synthetic call, use it but gobble up parent calls
                    // TODO: need to formalize this better
                    if (element.getMethodName().contains("$RUBY$SYNTHETIC")) {
                        // gobble up at least one parent, and keep going if there's more synthetic frames
                        while (element.getMethodName().indexOf("$RUBY$SYNTHETIC") != -1) {
                            i++;
                            element = stackTrace[i];
                        }
                    }
                    continue;
                }

                // AOT formatted method names, need to be cleaned up
                int RUBYindex = methodName.indexOf("$RUBY$");
                if (RUBYindex >= 0) {
                    // if it's a synthetic call, use it but gobble up parent calls
                    // TODO: need to formalize this better
                    methodName = methodName.substring(RUBYindex);
                    if (methodName.startsWith("$RUBY$SYNTHETIC")) {
                        methodName = methodName.substring("$RUBY$SYNTHETIC".length());
                        methodName = JavaNameMangler.demangleMethodName(methodName);
                        if (methodName == "__file__") methodName = "(root)";
                        trace.add(new RubyStackTraceElement(className, methodName, element.getFileName(), element.getLineNumber(), false));

                        // gobble up at least one parent, and keep going if there's more synthetic frames
                        while (element.getMethodName().indexOf("$RUBY$SYNTHETIC") != -1) {
                            i++;
                            element = stackTrace[i];
                        }
                        continue;
                    }

                    // not a synthetic body
                    methodName = methodName.substring("$RUBY$".length());
                    methodName = JavaNameMangler.demangleMethodName(methodName);
                    trace.add(new RubyStackTraceElement(className, methodName, element.getFileName(), element.getLineNumber(), false));
                    continue;
                }

                // last attempt at AOT compiled backtrace element, looking for __file__
                if (methodName.equals("__file__")) {
                    methodName = "(root)";
                    trace.add(new RubyStackTraceElement(className, methodName, element.getFileName(), element.getLineNumber(), false));
                    continue;
                }
            }

            String dotClassMethod = element.getClassName() + "." + element.getMethodName();
            if (fullTrace || runtime.getBoundMethods().containsKey(dotClassMethod)) {
                String filename = element.getFileName();
                int lastDot = element.getClassName().lastIndexOf('.');
                if (lastDot != -1) {
                    filename = element.getClassName().substring(0, lastDot + 1).replaceAll("\\.", "/") + filename;
                }
                trace.add(new RubyStackTraceElement(element.getClassName(), element.getMethodName(), filename, element.getLineNumber(), false));

                if (!fullTrace) {
                    // we want to fall through below to add Ruby trace info as well
                    continue;
                }
            }

            // try to mine out a Ruby frame using our list of interpreter entry-point markers
            String classMethod = element.getClassName() + "." + element.getMethodName();
            FrameType frameType = INTERPRETED_FRAMES.get(classMethod);
            if (frameType != null && rubyFrameIndex >= 0) {
                // Frame matches one of our markers for "interpreted" calls
                Backtrace rubyElement = backtraceFrames[rubyFrameIndex];
                trace.add(new RubyStackTraceElement(rubyElement.klass, rubyElement.method, rubyElement.filename, rubyElement.line + 1, false));
                rubyFrameIndex--;
                continue;
            } else {
                // frames not being included...
//                RubyString str = RubyString.newString(runtime, createRubyBacktraceString(element));
//                traceArray.append(str);
                continue;
            }
        }

        RubyStackTraceElement[] rubyStackTrace = new RubyStackTraceElement[trace.size()];
        return (RubyStackTraceElement[])trace.toArray(rubyStackTrace);
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
    }
    
    public void postMethodBacktraceAndScope() {
        postMethodScopeOnly();
    }
    
    public void preMethodBacktraceOnly(String name) {
    }

    public void preMethodBacktraceDummyScope(RubyModule clazz, String name, StaticScope staticScope) {
        RubyModule implementationClass = staticScope.getModule();
        // FIXME: This is currently only here because of some problems with IOOutputStream writing to a "bare" runtime without a proper scope
        if (implementationClass == null) {
            implementationClass = clazz;
        }
        pushScope(staticScope.getDummyScope());
        pushRubyClass(implementationClass);
    }
    
    public void postMethodBacktraceOnly() {
    }

    public void postMethodBacktraceDummyScope() {
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
        return new Binding(frame, getRubyClass(), getCurrentScope(), backtrace[backtraceIndex].clone());
    }

    /**
     * Return a binding representing the current call's state but with a specified self
     * @param self the self object to use
     * @return the current binding, using the specified self
     */
    public Binding currentBinding(IRubyObject self) {
        Frame frame = getCurrentFrame();
        return new Binding(self, frame, frame.getVisibility(), getRubyClass(), getCurrentScope(), backtrace[backtraceIndex].clone());
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
        return new Binding(self, frame, visibility, getRubyClass(), getCurrentScope(), backtrace[backtraceIndex].clone());
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
        return new Binding(self, frame, frame.getVisibility(), getRubyClass(), scope, backtrace[backtraceIndex].clone());
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
        return new Binding(self, frame, visibility, getRubyClass(), scope, backtrace[backtraceIndex].clone());
    }

    /**
     * Return a binding representing the previous call's state
     * @return the current binding
     */
    public Binding previousBinding() {
        Frame frame = getPreviousFrame();
        return new Binding(frame, getPreviousRubyClass(), getCurrentScope(), backtrace[backtraceIndex].clone());
    }

    /**
     * Return a binding representing the previous call's state but with a specified self
     * @param self the self object to use
     * @return the current binding, using the specified self
     */
    public Binding previousBinding(IRubyObject self) {
        Frame frame = getPreviousFrame();
        return new Binding(self, frame, frame.getVisibility(), getPreviousRubyClass(), getCurrentScope(), backtrace[backtraceIndex].clone());
    }

    /**
     * Get the profile data for this thread (ThreadContext).
     *
     * @return the thread's profile data
     */
    public IProfileData getProfileData() {
        if (profileData == null)
            profileData = new ProfileData(this);
        return profileData;
    }

    private int currentMethodSerial = 0;
    
    public int profileEnter(int nextMethod) {
        int previousMethodSerial = currentMethodSerial;
        currentMethodSerial = nextMethod;
        if (isProfiling)
            getProfileData().profileEnter(nextMethod);
        return previousMethodSerial;
    }

    public int profileExit(int nextMethod, long startTime) {
        int previousMethodSerial = currentMethodSerial;
        currentMethodSerial = nextMethod;
        if (isProfiling)
            getProfileData().profileExit(nextMethod, startTime);
        return previousMethodSerial;
    }
    
    public void startProfiling() {
        isProfiling = true;
    }
    
    public void stopProfiling() {
        isProfiling = false;
    }
    
    public boolean isProfiling() {
        return isProfiling;
    }
}
