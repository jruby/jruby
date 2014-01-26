/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.runtime;

import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyContinuation.Continuation;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.RubyThread;
import org.jruby.ast.executable.RuntimeCache;
import org.jruby.exceptions.JumpException.ReturnJump;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.ext.fiber.ThreadFiber;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.backtrace.TraceType;
import org.jruby.runtime.backtrace.TraceType.Gather;
import org.jruby.runtime.backtrace.BacktraceElement;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.profile.ProfileData;
import org.jruby.runtime.scope.ManyVarsDynamicScope;
import org.jruby.util.RecursiveComparator;
import org.jruby.util.RubyDateFormatter;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public final class ThreadContext {

    private static final Logger LOG = LoggerFactory.getLogger("ThreadContext");

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
    public final boolean is19;
    
    // Is this thread currently with in a function trace?
    private boolean isWithinTrace;
    
    private RubyThread thread;
    private RubyThread rootThread; // thread for fiber purposes
    private WeakReference<ThreadFiber> fiber = new WeakReference<ThreadFiber>(null);
    private ThreadFiber rootFiber; // hard anchor for root threads' fibers
    // Cache format string because it is expensive to create on demand
    private RubyDateFormatter dateFormatter;
    
    private RubyModule[] parentStack = new RubyModule[INITIAL_SIZE];
    private int parentIndex = -1;
    
    private Frame[] frameStack = new Frame[INITIAL_FRAMES_SIZE];
    private int frameIndex = -1;

    private BacktraceElement[] backtrace = new BacktraceElement[INITIAL_FRAMES_SIZE];
    private int backtraceIndex = -1;
    
    // List of active dynamic scopes.  Each of these may have captured other dynamic scopes
    // to implement closures.
    private DynamicScope[] scopeStack = new DynamicScope[INITIAL_SIZE];
    private int scopeIndex = -1;

    private static final Continuation[] EMPTY_CATCHTARGET_STACK = new Continuation[0];
    private Continuation[] catchStack = EMPTY_CATCHTARGET_STACK;
    private int catchIndex = -1;
    
    private boolean isProfiling = false;
    // The flat profile data for this thread
	private ProfileData profileData;
	
    private boolean eventHooksEnabled = true;

    CallType lastCallType;

    Visibility lastVisibility;

    IRubyObject lastExitStatus;

    public final SecureRandom secureRandom;

    private static boolean trySHA1PRNG = true;

    {
        SecureRandom sr;
        try {
            sr = trySHA1PRNG ?
                    SecureRandom.getInstance("SHA1PRNG") :
                    new SecureRandom();
        } catch (Exception e) {
            trySHA1PRNG = false;
            sr = new SecureRandom();
        }
        secureRandom = sr;
    }
    
    /**
     * Constructor for Context.
     */
    private ThreadContext(Ruby runtime) {
        this.runtime = runtime;
        this.nil = runtime.getNil();
        this.is19 = runtime.is1_9();
        if (runtime.getInstanceConfig().isProfilingEntireRun())
            startProfiling();

        this.runtimeCache = runtime.getRuntimeCache();
        
        // TOPLEVEL self and a few others want a top-level scope.  We create this one right
        // away and then pass it into top-level parse so it ends up being the top level.
        StaticScope topStaticScope = runtime.getStaticScopeFactory().newLocalScope(null);
        pushScope(new ManyVarsDynamicScope(topStaticScope, null));

        Frame[] stack = frameStack;
        int length = stack.length;
        for (int i = 0; i < length; i++) {
            stack[i] = new Frame();
        }
        BacktraceElement[] stack2 = backtrace;
        int length2 = stack2.length;
        for (int i = 0; i < length2; i++) {
            stack2[i] = new BacktraceElement();
        }
        ThreadContext.pushBacktrace(this, "", "", 0);
        ThreadContext.pushBacktrace(this, "", "", 0);
    }

    @Override
    protected void finalize() throws Throwable {
        if (thread != null) {
            thread.dispose();
        }
    }

    /**
     * Retrieve the runtime associated with this context.
     *
     * Note that there's no reason to call this method rather than accessing the
     * runtime field directly.
     *
     * @see ThreadContext#runtime
     *
     * @return the runtime associated with this context
     */
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
        LOG.debug("SCOPE STACK:");
        for (int i = 0; i <= scopeIndex; i++) {
            LOG.debug("{}", scopeStack[i]);
        }
    }

    public DynamicScope getCurrentScope() {
        return scopeStack[scopeIndex];
    }

    public StaticScope getCurrentStaticScope() {
        return scopeStack[scopeIndex].getStaticScope();
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
    
    public RubyThread getFiberCurrentThread() {
        if (rootThread != null) return rootThread;
        return thread;
    }
    
    public RubyDateFormatter getRubyDateFormatter() {
        if (dateFormatter == null)
            dateFormatter = new RubyDateFormatter(this);
        return dateFormatter;
    }
    
    public void setThread(RubyThread thread) {
        this.thread = thread;
        this.rootThread = thread; // may be reset by fiber

        // associate the thread with this context, unless we're clearing the reference
        if (thread != null) {
            thread.setContext(this);
        }
    }
    
    public ThreadFiber getFiber() {
        ThreadFiber f = fiber.get();
        
        if (f == null) return rootFiber;
        
        return f;
    }
    
    public void setFiber(ThreadFiber fiber) {
        this.fiber = new WeakReference(fiber);
    }
    
    public void setRootFiber(ThreadFiber rootFiber) {
        this.rootFiber = rootFiber;
    }
    
    public void setRootThread(RubyThread rootThread) {
        this.rootThread = rootThread;
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
    
    public void pushFrame() {
        int index = ++this.frameIndex;
        Frame[] stack = frameStack;
        if (index + 1 == stack.length) {
            expandFramesIfNecessary();
        }
    }
    
    public void popFrame() {
        Frame[] stack = frameStack;
        int index = frameIndex--;
        Frame frame = stack[index];
        
        // if the frame was captured, we must replace it but not clear
        if (frame.isCaptured()) {
            stack[index] = new Frame();
        } else {
            frame.clear();
        }
    }
        
    private void popFrameReal(Frame oldFrame) {
        frameStack[frameIndex--] = oldFrame;
    }
    
    public Frame getCurrentFrame() {
        return frameStack[frameIndex];
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
    
    /**
     * Set the $~ (backref) "global" to the given value.
     * 
     * @param match the value to set
     * @return the value passed in
     */
    public IRubyObject setBackRef(IRubyObject match) {
        return getCurrentFrame().setBackRef(match);
    }
    
    /**
     * Get the value of the $~ (backref) "global".
     * 
     * @return the value of $~
     */
    public IRubyObject getBackRef() {
        return getCurrentFrame().getBackRef(nil);
    }
    
    /**
     * Set the $_ (lastlne) "global" to the given value.
     * 
     * @param last the value to set
     * @return the value passed in
     */
    public IRubyObject setLastLine(IRubyObject last) {
        return getCurrentFrame().setLastLine(last);
    }
    
    /**
     * Get the value of the $_ (lastline) "global".
     * 
     * @return the value of $_
     */
    public IRubyObject getLastLine() {
        return getCurrentFrame().getLastLine(nil);
    }

    /////////////////// BACKTRACE ////////////////////

    private static void expandBacktraceIfNecessary(ThreadContext context) {
        int newSize = context.backtrace.length * 2;
        context.backtrace = fillNewBacktrace(context, new BacktraceElement[newSize], newSize);
    }

    private static BacktraceElement[] fillNewBacktrace(ThreadContext context, BacktraceElement[] newBacktrace, int newSize) {
        System.arraycopy(context.backtrace, 0, newBacktrace, 0, context.backtrace.length);

        for (int i = context.backtrace.length; i < newSize; i++) {
            newBacktrace[i] = new BacktraceElement();
        }

        return newBacktrace;
    }

    public static void pushBacktrace(ThreadContext context, String method, ISourcePosition position) {
        int index = ++context.backtraceIndex;
        BacktraceElement[] stack = context.backtrace;
        BacktraceElement.update(stack[index], method, position);
        if (index + 1 == stack.length) {
            ThreadContext.expandBacktraceIfNecessary(context);
        }
    }

    public static void pushBacktrace(ThreadContext context, String method, String file, int line) {
        int index = ++context.backtraceIndex;
        BacktraceElement[] stack = context.backtrace;
        BacktraceElement.update(stack[index], method, file, line);
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

    /**
     * Check if a static scope is present on the call stack.
     * This is the IR equivalent of isJumpTargetAlive
     *
     * @param s the static scope to look for
     * @return true if it exists
     *         false if not
     **/
    public boolean scopeExistsOnCallStack(StaticScope s) {
        DynamicScope[] stack = scopeStack;
        for (int i = scopeIndex; i >= 0; i--) {
           if (stack[i].getStaticScope() == s) return true;
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
    
    public void setLine(int line) {
        backtrace[backtraceIndex].line = line;
    }
    
    public void setFileAndLine(String file, int line) {
        BacktraceElement b = backtrace[backtraceIndex];
        b.filename = file;
        b.line = line;
    }

    public void setFileAndLine(ISourcePosition position) {
        BacktraceElement b = backtrace[backtraceIndex];
        b.filename = position.getFile();
        b.line = position.getStartLine();
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

    @Deprecated
    public boolean getConstantDefined(String internedName) {
        return getCurrentStaticScope().isConstantDefined(internedName);
    }
    
    /**
     * Used by the evaluator and the compiler to look up a constant by name
     */
    @Deprecated
    public IRubyObject getConstant(String internedName) {
        return getCurrentStaticScope().getConstant(internedName);
    }

    /**
     * Used by the evaluator and the compiler to set a constant by name
     * This is for a null const decl
     */
    @Deprecated
    public IRubyObject setConstantInCurrent(String internedName, IRubyObject result) {
        return getCurrentStaticScope().setConstant(internedName, result);
    }
    
    /**
     * Used by the evaluator and the compiler to set a constant by name.
     * This is for a Colon2 const decl
     */
    @Deprecated
    public IRubyObject setConstantInModule(String internedName, IRubyObject target, IRubyObject result) {
        if (!(target instanceof RubyModule)) {
            throw runtime.newTypeError(target.toString() + " is not a class/module");
        }
        RubyModule module = (RubyModule)target;
        module.setConstant(internedName, result);
        
        return result;
    }
    
    /**
     * Used by the evaluator and the compiler to set a constant by name
     * This is for a Colon2 const decl
     */
    @Deprecated
    public IRubyObject setConstantInObject(String internedName, IRubyObject result) {
        runtime.getObject().setConstant(internedName, result);
        
        return result;
    }
    
    private static void addBackTraceElement(Ruby runtime, RubyArray backtrace, RubyStackTraceElement element) {
        RubyString str = RubyString.newString(runtime, element.mriStyleString());
        backtrace.append(str);
    }
    
    /**
     * Create an Array with backtrace information for Kernel#caller
     * @param runtime
     * @param level
     * @return an Array with the backtrace
     */
    public IRubyObject createCallerBacktrace(int level, StackTraceElement[] stacktrace) {
        return createCallerBacktrace(level, null, stacktrace);
    }

    /**
     * Create an Array with backtrace information for Kernel#caller
     * @param runtime
     * @param level
     * @param length
     * @return an Array with the backtrace
     */
    public IRubyObject createCallerBacktrace(int level, Integer length, StackTraceElement[] stacktrace) {
        runtime.incrementCallerCount();
        
        RubyStackTraceElement[] trace = getTraceSubset(level, length, stacktrace);
        
        if (trace == null) return nil;
        
        RubyArray newTrace = runtime.newArray(trace.length);

        for (int i = level; i - level < trace.length; i++) {
            addBackTraceElement(runtime, newTrace, trace[i - level]);
        }
        
        if (RubyInstanceConfig.LOG_CALLERS) TraceType.dumpCaller(newTrace);
        
        return newTrace;
    }

    /**
     * Create an array containing Thread::Backtrace::Location objects for the
     * requested caller trace level and length.
     * 
     * @param level the level at which the trace should start
     * @param length the length of the trace
     * @return an Array with the backtrace locations
     */
    public IRubyObject createCallerLocations(int level, Integer length, StackTraceElement[] stacktrace) {
        RubyStackTraceElement[] trace = getTraceSubset(level, length, stacktrace);
        
        if (trace == null) return nil;
        
        return RubyThread.Location.newLocationArray(runtime, trace);
    }
    
    private RubyStackTraceElement[] getTraceSubset(int level, Integer length, StackTraceElement[] stacktrace) {
        runtime.incrementCallerCount();
        
        if (length != null && length == 0) return new RubyStackTraceElement[0];
        
        RubyStackTraceElement[] trace =
                TraceType.Gather.CALLER.getBacktraceData(this, stacktrace, false).getBacktrace(runtime);
        
        int traceLength = safeLength(level, length, trace);
        
        if (traceLength < 0) return null;
        
        trace = Arrays.copyOfRange(trace, level, level + traceLength);
        
        if (RubyInstanceConfig.LOG_CALLERS) TraceType.dumpCaller(trace);
        
        return trace;
    }
    
    private static int safeLength(int level, Integer length, RubyStackTraceElement[] trace) {
        int baseLength = trace.length - level;
        return length != null ? Math.min(length, baseLength) : baseLength;
    }

    /**
     * Create an Array with backtrace information for a built-in warning
     * @param runtime
     * @return an Array with the backtrace
     */
    public RubyStackTraceElement[] createWarningBacktrace(Ruby runtime) {
        runtime.incrementWarningCount();

        RubyStackTraceElement[] trace = gatherCallerBacktrace();

        if (RubyInstanceConfig.LOG_WARNINGS) TraceType.dumpWarning(trace);

        return trace;
    }
    
    public RubyStackTraceElement[] gatherCallerBacktrace() {
        return Gather.CALLER.getBacktraceData(this, false).getBacktrace(runtime);
    }
    
    /**
     * Create an Array with backtrace information.
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
    
    /**
     * Create an Array with backtrace information.
     * @param level
     * @param nativeException
     * @return an Array with the backtrace
     */
    public BacktraceElement[] createBacktrace2(int level, boolean nativeException) {
        BacktraceElement[] backtraceClone = backtrace.clone();
        int backtraceIndex = this.backtraceIndex;
        BacktraceElement[] newTrace = new BacktraceElement[backtraceIndex + 1];
        for (int i = 0; i <= backtraceIndex; i++) {
            newTrace[i] = backtraceClone[i];
        }
        return newTrace;
    }
    
    private static String createRubyBacktraceString(StackTraceElement element) {
        return element.getFileName() + ":" + element.getLineNumber() + ":in `" + element.getMethodName() + "'";
    }
    
    public static String createRawBacktraceStringFromThrowable(Throwable t) {
        StackTraceElement[] javaStackTrace = t.getStackTrace();
        
        StringBuilder buffer = new StringBuilder();
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
    
    public void preAdoptThread() {
        pushFrame();
        pushRubyClass(runtime.getObject());
        getCurrentFrame().setSelf(runtime.getTopSelf());
    }

    public void preExtensionLoad(IRubyObject self) {
        pushFrame();
        pushRubyClass(runtime.getObject());
        getCurrentFrame().setSelf(self);
        getCurrentFrame().setVisibility(Visibility.PUBLIC);
    }

    public void postExtensionLoad() {
        popFrame();
        popRubyClass();
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
        getCurrentFrame().setName(null);

        pushScope(DynamicScope.newDynamicScope(staticScope, null));
    }
    
    public void postClassEval() {
        popScope();
        popRubyClass();
        popFrame();
    }
    
    public void preBsfApply(String[] names) {
        // FIXME: I think we need these pushed somewhere?
        StaticScope staticScope = runtime.getStaticScopeFactory().newLocalScope(null);
        staticScope.setVariables(names);
        pushFrame();
    }
    
    public void postBsfApply() {
        popFrame();
    }

    public void preMethodFrameAndClass(RubyModule implClass, String name, IRubyObject self, Block block, StaticScope staticScope) {
        RubyModule ssModule = staticScope.getModule();
        // FIXME: This is currently only here because of some problems with IOOutputStream writing to a "bare" runtime without a proper scope
        if (ssModule == null) ssModule = implClass;
        pushRubyClass(ssModule);
        pushCallFrame(implClass, name, self, block);
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
        StaticScope sScope = runtime.getStaticScopeFactory().newBlockScope(scope.getStaticScope());
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
        Frame lastFrame = pushFrameForEval(binding);
        pushRubyClass(binding.getKlass());
        return lastFrame;
    }
    
    public void postEvalWithBinding(Binding binding, Frame lastFrame) {
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
     * @see org.jruby.Ruby#callEventHooks(ThreadContext, RubyEvent, String, int, String, org.jruby.runtime.builtin.IRubyObject)
     */
    public boolean isWithinTrace() {
        return isWithinTrace;
    }
    
    /**
     * Set whether we are actively tracing or not on this thread.
     *
     * @param isWithinTrace true is so
     * @see org.jruby.Ruby#callEventHooks(ThreadContext, RubyEvent, String, int, String, org.jruby.runtime.builtin.IRubyObject)
     */
    public void setWithinTrace(boolean isWithinTrace) {
        this.isWithinTrace = isWithinTrace;
    }
    
    /**
     * Return a binding representing the current call's state
     * @return the current binding
     */
    public Binding currentBinding() {
        Frame frame = getCurrentFrame().capture();
        return new Binding(frame, parentIndex < 0 ? frame.getKlazz() : getRubyClass(), getCurrentScope(), backtrace[backtraceIndex].clone());
    }

    /**
     * Return a binding representing the current call's state but with a specified self
     * @param self the self object to use
     * @return the current binding, using the specified self
     */
    public Binding currentBinding(IRubyObject self) {
        Frame frame = getCurrentFrame().capture();
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
        Frame frame = getCurrentFrame().capture();
        return new Binding(self, frame, visibility, getRubyClass(), getCurrentScope(), backtrace[backtraceIndex].clone());
    }

    /**
     * Return a binding representing the current call's state but with the
     * specified scope and self.
     * @param self the self object to use
     * @param scope the scope to use
     * @return the current binding using the specified self and scope
     */
    public Binding currentBinding(IRubyObject self, DynamicScope scope) {
        Frame frame = getCurrentFrame().capture();
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
        Frame frame = getCurrentFrame().capture();
        return new Binding(self, frame, visibility, getRubyClass(), scope, backtrace[backtraceIndex].clone());
    }

    /**
     * Return a binding representing the previous call's state
     * @return the current binding
     */
    @Deprecated
    public Binding previousBinding() {
        Frame frame = getPreviousFrame();
        return new Binding(frame, getPreviousRubyClass(), getCurrentScope(), backtrace[backtraceIndex].clone());
    }

    /**
     * Return a binding representing the previous call's state but with a specified self
     * @param self the self object to use
     * @return the current binding, using the specified self
     */
    @Deprecated
    public Binding previousBinding(IRubyObject self) {
        Frame frame = getPreviousFrame();
        return new Binding(self, frame, frame.getVisibility(), getPreviousRubyClass(), getCurrentScope(), backtrace[backtraceIndex].clone());
    }

    /**
     * Get the profile data for this thread (ThreadContext).
     *
     * @return the thread's profile data
     */
    public ProfileData getProfileData() {
        if (profileData == null) {
            profileData = new ProfileData(this);
        }
        return profileData;
    }

    public void startProfiling() {
        isProfiling = true;
        // use new profiling data every time profiling is started, useful in 
        // case users keep a reference to previous data after profiling stop
        profileData = new ProfileData(this);
    }
    
    public void stopProfiling() {
        isProfiling = false;
    }
    
    public boolean isProfiling() {
        return isProfiling;
    }
    
    private int currentMethodSerial = 0;
    
    public int profileEnter(int nextMethod) {
        int previousMethodSerial = currentMethodSerial;
        currentMethodSerial = nextMethod;
        if (isProfiling()) {
            getProfileData().profileEnter(nextMethod);
        }
        return previousMethodSerial;
    }

    public int profileEnter(String name, DynamicMethod nextMethod) {
        if (isProfiling()) {
            getProfileData().addProfiledMethod(name, nextMethod);
        }
        return profileEnter((int) nextMethod.getSerialNumber());
    }
    
    public int profileExit(int nextMethod, long startTime) {
        int previousMethodSerial = currentMethodSerial;
        currentMethodSerial = nextMethod;
        if (isProfiling()) {
            getProfileData().profileExit(nextMethod, startTime);
        }
        return previousMethodSerial;
    }
    
    public Set<RecursiveComparator.Pair> getRecursiveSet() {
        return recursiveSet;
    }
    
    public void setRecursiveSet(Set<RecursiveComparator.Pair> recursiveSet) {
        this.recursiveSet = recursiveSet;
    }

    @Deprecated
    public void setFile(String file) {
        backtrace[backtraceIndex].filename = file;
    }
    
    private Set<RecursiveComparator.Pair> recursiveSet;
    
    @Deprecated
    private org.jruby.util.RubyDateFormat dateFormat;
    
    @Deprecated
    public org.jruby.util.RubyDateFormat getRubyDateFormat() {
        if (dateFormat == null) dateFormat = new org.jruby.util.RubyDateFormat("-", Locale.US, runtime.is1_9());
        
        return dateFormat;
    }
    
}
