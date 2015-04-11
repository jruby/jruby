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

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyContinuation.Continuation;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.RubyThread;
import org.jruby.ast.executable.RuntimeCache;
import org.jruby.ext.fiber.ThreadFiber;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.backtrace.BacktraceElement;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.backtrace.TraceType;
import org.jruby.runtime.backtrace.TraceType.Gather;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.profile.ProfileCollection;
import org.jruby.runtime.scope.ManyVarsDynamicScope;
import org.jruby.util.RecursiveComparator;
import org.jruby.util.RubyDateFormatter;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

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

    // Is this thread currently with in a function trace?
    private boolean isWithinTrace;
    
    private RubyThread thread;
    private RubyThread rootThread; // thread for fiber purposes
    private static final WeakReference<ThreadFiber> NULL_FIBER_REF = new WeakReference<ThreadFiber>(null);
    private WeakReference<ThreadFiber> fiber = NULL_FIBER_REF;
    private ThreadFiber rootFiber; // hard anchor for root threads' fibers
    // Cache format string because it is expensive to create on demand
    private RubyDateFormatter dateFormatter;

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
	// private ProfileData profileData;

    private ProfileCollection profileCollection;
	
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

        if (runtime.getInstanceConfig().isProfilingEntireRun()) {
            startProfiling();
        }

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

    private void expandFrameStack() {
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

    public void pushScope(DynamicScope scope) {
        int index = ++scopeIndex;
        DynamicScope[] stack = scopeStack;
        stack[index] = scope;
        if (index + 1 == stack.length) {
            expandScopeStack();
        }
    }
    
    public void popScope() {
        scopeStack[scopeIndex--] = null;
    }
    
    private void expandScopeStack() {
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
    private void expandCatchStack() {
        int newSize = catchStack.length * 2;
        if (newSize == 0) newSize = 1;
        Continuation[] newCatchStack = new Continuation[newSize];

        System.arraycopy(catchStack, 0, newCatchStack, 0, catchStack.length);
        catchStack = newCatchStack;
    }
    
    public void pushCatch(Continuation catchTarget) {
        int index = ++catchIndex;
        if (index == catchStack.length) {
            expandCatchStack();
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
            if (c.tag == tag) return c;
        }

        // if this is a fiber, search prev for tag
        ThreadFiber fiber = getFiber();
        ThreadFiber prev;
        if (fiber != null && (prev = fiber.getData().getPrev()) != null) {
            return prev.getThread().getContext().getActiveCatch(tag);
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
            expandFrameStack();
        }
    }
    
    private Frame pushFrame(Frame frame) {
        int index = ++this.frameIndex;
        Frame[] stack = frameStack;
        stack[index] = frame;
        if (index + 1 == stack.length) {
            expandFrameStack();
        }
        return frame;
    }

    public void pushEvalSimpleFrame(IRubyObject executeObject) {
        Frame frame = getCurrentFrame();
        pushCallFrame(frame.getKlazz(), frame.getName(), executeObject, Block.NULL_BLOCK);
    }
    
    private void pushCallFrame(RubyModule clazz, String name, 
                               IRubyObject self, Block block) {
        int index = ++this.frameIndex;
        Frame[] stack = frameStack;
        stack[index].updateFrame(clazz, self, name, block, callNumber);
        if (index + 1 == stack.length) {
            expandFrameStack();
        }
    }
    
    private void pushEvalFrame(IRubyObject self) {
        int index = ++this.frameIndex;
        Frame[] stack = frameStack;
        stack[index].updateFrameForEval(self, callNumber);
        if (index + 1 == stack.length) {
            expandFrameStack();
        }
    }
    
    public void pushFrame() {
        int index = ++this.frameIndex;
        Frame[] stack = frameStack;
        if (index + 1 == stack.length) {
            expandFrameStack();
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
            expandFrameStack();
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

    private static void expandBacktraceStack(ThreadContext context) {
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

    public static void pushBacktrace(ThreadContext context, String method, String file, int line) {
        int index = ++context.backtraceIndex;
        BacktraceElement[] stack = context.backtrace;
        BacktraceElement.update(stack[index], method, file, line);
        if (index + 1 == stack.length) {
            ThreadContext.expandBacktraceStack(context);
        }
    }

    public static void popBacktrace(ThreadContext context) {
        context.backtraceIndex--;
    }

    public boolean hasAnyScopes() {
        return scopeIndex > -1;
    }

    /**
     * Check if a static scope is present on the call stack.
     * This is the IR equivalent of isJumpTargetAlive
     *
     * @param scope the static scope to look for
     * @return true if it exists
     *         false if not
     **/
    public boolean scopeExistsOnCallStack(DynamicScope scope) {
        DynamicScope[] stack = scopeStack;
        for (int i = scopeIndex; i >= 0; i--) {
           if (stack[i] == scope) return true;
        }
        return false;
    }

    public String getFrameName() {
        return getCurrentFrame().getName();
    }
    
    public IRubyObject getFrameSelf() {
        return getCurrentFrame().getSelf();
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
        b.line = position.getLine();
    }
    
    public Visibility getCurrentVisibility() {
        return getCurrentFrame().getVisibility();
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

    /**
     * Used by the evaluator and the compiler to look up a constant by name
     */
    @Deprecated
    public IRubyObject getConstant(String internedName) {
        return getCurrentStaticScope().getConstant(internedName);
    }

    private static void addBackTraceElement(Ruby runtime, RubyArray backtrace, RubyStackTraceElement element) {
        RubyString str = RubyString.newString(runtime, element.mriStyleString());
        backtrace.append(str);
    }
    
    /**
     * Create an Array with backtrace information for Kernel#caller
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
        getCurrentFrame().setSelf(runtime.getTopSelf());
    }

    public void preExtensionLoad(IRubyObject self) {
        pushFrame();
        getCurrentFrame().setSelf(self);
        getCurrentFrame().setVisibility(Visibility.PUBLIC);
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

    public void preMethodFrameAndScope(RubyModule clazz, String name, IRubyObject self, Block block,
            StaticScope staticScope) {
        pushCallFrame(clazz, name, self, block);
        pushScope(DynamicScope.newDynamicScope(staticScope));
    }
    
    public void preMethodFrameAndDummyScope(RubyModule clazz, String name, IRubyObject self, Block block,
            StaticScope staticScope) {
        pushCallFrame(clazz, name, self, block);
        pushScope(staticScope.getDummyScope());
    }

    public void preMethodNoFrameAndDummyScope(StaticScope staticScope) {
        pushScope(staticScope.getDummyScope());
    }
    
    public void postMethodFrameAndScope() {
        popScope();
        popFrame();
    }
    
    public void preMethodFrameOnly(RubyModule clazz, String name, IRubyObject self, Block block) {
        pushCallFrame(clazz, name, self, block);
    }
    
    public void postMethodFrameOnly() {
        popFrame();
    }
    
    public void preMethodScopeOnly(StaticScope staticScope) {
        pushScope(DynamicScope.newDynamicScope(staticScope));
    }
    
    public void postMethodScopeOnly() {
        popScope();
    }
    
    public void preMethodBacktraceAndScope(String name, StaticScope staticScope) {
        preMethodScopeOnly(staticScope);
    }
    
    public void postMethodBacktraceAndScope() {
        postMethodScopeOnly();
    }
    
    public void preMethodBacktraceOnly(String name) {
    }

    public void preMethodBacktraceDummyScope(String name, StaticScope staticScope) {
        pushScope(staticScope.getDummyScope());
    }
    
    public void postMethodBacktraceOnly() {
    }

    public void postMethodBacktraceDummyScope() {
        popScope();
    }
    
    public void prepareTopLevel(RubyClass objectClass, IRubyObject topSelf) {
        pushFrame();
        setCurrentVisibility(Visibility.PRIVATE);
        Frame frame = getCurrentFrame();
        frame.setSelf(topSelf);
        
        getCurrentScope().getStaticScope().setModule(objectClass);
    }
    
    public void preNodeEval(IRubyObject self) {
        pushEvalFrame(self);
    }
    
    public void postNodeEval() {
        popFrame();
    }

    public void preExecuteUnder(IRubyObject executeUnderObj, RubyModule executeUnderClass, Block block) {
        Frame frame = getCurrentFrame();
        
        DynamicScope scope = getCurrentScope();
        StaticScope sScope = runtime.getStaticScopeFactory().newBlockScope(scope.getStaticScope());
        sScope.setModule(executeUnderClass);
        pushScope(DynamicScope.newDynamicScope(sScope, scope));
        pushCallFrame(frame.getKlazz(), frame.getName(), executeUnderObj, block);
        getCurrentFrame().setVisibility(getPreviousFrame().getVisibility());
    }

    public void postExecuteUnder() {
        popFrame();
        popScope();
    }

    public void preTrace() {
        setWithinTrace(true);
        pushFrame();
    }
    
    public void postTrace() {
        popFrame();
        setWithinTrace(false);
    }
    
    public Frame preForBlock(Binding binding) {
        Frame lastFrame = preYieldNoScope(binding);
        pushScope(binding.getDynamicScope());
        return lastFrame;
    }
    
    public Frame preYieldSpecificBlock(Binding binding, StaticScope scope) {
        Frame lastFrame = preYieldNoScope(binding);
        // new scope for this invocation of the block, based on parent scope
        pushScope(DynamicScope.newDynamicScope(scope, binding.getDynamicScope()));
        return lastFrame;
    }
    
    public Frame preYieldLightBlock(Binding binding, DynamicScope emptyScope) {
        Frame lastFrame = preYieldNoScope(binding);
        // just push the same empty scope, since we won't use one
        pushScope(emptyScope);
        return lastFrame;
    }
    
    public Frame preYieldNoScope(Binding binding) {
        return pushFrameForBlock(binding);
    }
    
    public void preEvalScriptlet(DynamicScope scope) {
        pushScope(scope);
    }
    
    public void postEvalScriptlet() {
        popScope();
    }
    
    public Frame preEvalWithBinding(Binding binding) {
        return pushFrameForEval(binding);
    }
    
    public void postEvalWithBinding(Binding binding, Frame lastFrame) {
        popFrameReal(lastFrame);
    }
    
    public void postYield(Binding binding, Frame lastFrame) {
        popScope();
        popFrameReal(lastFrame);
    }
    
    public void postYieldLight(Binding binding, Frame lastFrame) {
        popScope();
        popFrameReal(lastFrame);
    }
    
    public void postYieldNoScope(Frame lastFrame) {
        popFrameReal(lastFrame);
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
        BacktraceElement elt = backtrace[backtraceIndex];
        return new Binding(frame, getCurrentScope(), elt.getMethod(), elt.getFilename(), elt.getLine());
    }

    /**
     * Return a binding representing the current call's state but with a specified self
     * @param self the self object to use
     * @return the current binding, using the specified self
     */
    public Binding currentBinding(IRubyObject self) {
        Frame frame = getCurrentFrame().capture();
        BacktraceElement elt = backtrace[backtraceIndex];
        return new Binding(self, frame, frame.getVisibility(), getCurrentScope(), elt.getMethod(), elt.getFilename(), elt.getLine());
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
        BacktraceElement elt = backtrace[backtraceIndex];
        return new Binding(self, frame, visibility, getCurrentScope(), elt.getMethod(), elt.getFilename(), elt.getLine());
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
        BacktraceElement elt = backtrace[backtraceIndex];
        return new Binding(self, frame, frame.getVisibility(), scope, elt.getMethod(), elt.getFilename(), elt.getLine());
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
        BacktraceElement elt = backtrace[backtraceIndex];
        return new Binding(self, frame, visibility, scope, elt.getMethod(), elt.getFilename(), elt.getLine());
    }

    /**
     * Get the profile collection for this thread (ThreadContext).
     *
     * @return the thread's profile collection
     *
     */
    public ProfileCollection getProfileCollection() {
        return profileCollection;
    }

    public void startProfiling() {
        isProfiling = true;
        // use new profiling data every time profiling is started, useful in
        // case users keep a reference to previous data after profiling stop
        profileCollection = getRuntime().getProfilingService().newProfileCollection( this );
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
            getProfileCollection().profileEnter(nextMethod);
        }
        return previousMethodSerial;
    }

    public int profileEnter(String name, DynamicMethod nextMethod) {
        if (isProfiling()) {
            // TODO This can be removed, because the profiled method will be added in the MethodEnhancer if necessary
            getRuntime().getProfiledMethods().addProfiledMethod( name, nextMethod );
        }
        return profileEnter((int) nextMethod.getSerialNumber());
    }
    
    public int profileExit(int nextMethod, long startTime) {
        int previousMethodSerial = currentMethodSerial;
        currentMethodSerial = nextMethod;
        if (isProfiling()) {
            getProfileCollection().profileExit(nextMethod, startTime);
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
        if (dateFormat == null) dateFormat = new org.jruby.util.RubyDateFormat("-", Locale.US, true);
        
        return dateFormat;
    }
}
