/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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

import com.headius.backport9.stack.StackWalker;
import com.headius.backport9.stack.impl.StackWalker8;
import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyContinuation;
import org.jruby.RubyMatchData;
import org.jruby.RubyProc;
import org.jruby.exceptions.CatchThrow;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.RubyRegexp;
import org.jruby.RubyThread;
import org.jruby.ast.executable.RuntimeCache;
import org.jruby.exceptions.Unrescuable;
import org.jruby.ext.fiber.ThreadFiber;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.ir.JIT;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.backtrace.BacktraceData;
import org.jruby.runtime.backtrace.BacktraceElement;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.backtrace.TraceType;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.profile.ProfileCollection;
import org.jruby.runtime.scope.ManyVarsDynamicScope;
import org.jruby.util.RecursiveComparator;
import org.jruby.util.RubyDateFormatter;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.jruby.RubyBasicObject.NEVER;

public final class ThreadContext {

    private static final Logger LOG = LoggerFactory.getLogger(ThreadContext.class);

    public static ThreadContext newContext(Ruby runtime) {
        return new ThreadContext(runtime);
    }

    private final static int INITIAL_SIZE = 10;
    private final static int INITIAL_FRAMES_SIZE = 10;

    /** The number of calls after which to do a thread event poll */
    private final static int CALL_POLL_COUNT = 0xFFF;

    // runtime, nil, and runtimeCache cached here for speed of access from any thread
    public final Ruby runtime;
    public final IRubyObject nil;
    public final RubyBoolean tru;
    public final RubyBoolean fals;
    public final RuntimeCache runtimeCache;

    // Thread#set_trace_func for specific threads events.  We need this because successive
    // Thread.set_trace_funcs will end up replacing the current one (as opposed to add_trace_func).
    private Ruby.CallTraceFuncHook traceFuncHook = null;

    // Is this thread currently with in a function trace?
    private boolean isWithinTrace;

    private RubyThread thread;
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

    private static final CatchThrow[] EMPTY_CATCHTARGET_STACK = new CatchThrow[0];
    private CatchThrow[] catchStack = EMPTY_CATCHTARGET_STACK;
    private int catchIndex = -1;

    private boolean isProfiling = false;

    // The flat profile data for this thread
	// private ProfileData profileData;

    private ProfileCollection profileCollection;

    private boolean eventHooksEnabled = true;

    CallType lastCallType;

    Visibility lastVisibility;

    IRubyObject lastExitStatus;

    // These two fields are required to support explicit call protocol
    // (via IR instructions) for blocks.
    private Throwable savedExcInLambda;  // See handleBreakAndReturnsInLambda in IRRuntimeHelpers

    /**
     * This fields is no longer initialized, is null by default!
     * Use {@link #getSecureRandom()} instead.
     * @deprecated
     */
    @Deprecated
    public transient SecureRandom secureRandom;

    private static boolean tryPreferredPRNG = true;
    private static boolean trySHA1PRNG = true;

    private RubyModule privateConstantReference;

    public final JavaSites sites;

    private RubyMatchData matchData;

    @SuppressWarnings("deprecation")
    public SecureRandom getSecureRandom() {
        SecureRandom secureRandom = this.secureRandom;

        // Try preferred PRNG, which defaults to NativePRNGNonBlocking
        if (secureRandom == null && tryPreferredPRNG) {
            try {
                secureRandom = SecureRandom.getInstance(Options.PREFERRED_PRNG.load());
            } catch (Exception e) {
                tryPreferredPRNG = false;
            }
        }

        // Try SHA1PRNG
        if (secureRandom == null && trySHA1PRNG) {
            try {
                secureRandom = SecureRandom.getInstance("SHA1PRNG");
            } catch (Exception e) {
                trySHA1PRNG = false;
            }
        }

        // Just let JDK do whatever it does
        if (secureRandom == null) {
            secureRandom = new SecureRandom();
        }

        this.secureRandom = secureRandom;

        return secureRandom;
    }

    private Encoding[] encodingHolder;

    /**
     * Constructor for Context.
     */
    private ThreadContext(Ruby runtime) {
        this.runtime = runtime;
        this.nil = runtime.getNil();
        this.tru = runtime.getTrue();
        this.fals = runtime.getFalse();
        this.savedExcInLambda = null;

        if (runtime.getInstanceConfig().isProfilingEntireRun()) {
            startProfiling();
        }

        this.runtimeCache = runtime.getRuntimeCache();
        this.sites = runtime.sites;

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

    public Throwable getSavedExceptionInLambda() {
        return savedExcInLambda;
    }

    public void setSavedExceptionInLambda(Throwable e) {
        savedExcInLambda = e;
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

    @JIT
    public DynamicScope pushNewScope(StaticScope staticScope) {
        DynamicScope scope = staticScope.construct(null);
        pushScope(scope);
        return scope;
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
        return thread.getFiberCurrentThread();
    }

    public RubyDateFormatter getRubyDateFormatter() {
        if (dateFormatter == null)
            dateFormatter = new RubyDateFormatter(this);
        return dateFormatter;
    }

    public void setThread(RubyThread thread) {
        this.thread = thread;

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
        this.fiber = new WeakReference<ThreadFiber>(fiber);
    }

    /**
     * Fibers must use the same recursion guards as their parent thread.
     */
    public void useRecursionGuardsFrom(ThreadContext context) {
        this.symToGuards = context.symToGuards;
    }

    public void setRootFiber(ThreadFiber rootFiber) {
        this.rootFiber = rootFiber;
    }

    //////////////////// CATCH MANAGEMENT ////////////////////////
    private void expandCatchStack() {
        int newSize = catchStack.length * 2;
        if (newSize == 0) newSize = 1;
        CatchThrow[] newCatchStack = new CatchThrow[newSize];

        System.arraycopy(catchStack, 0, newCatchStack, 0, catchStack.length);
        catchStack = newCatchStack;
    }

    @Deprecated
    public void pushCatch(RubyContinuation.Continuation catchTarget) {
        pushCatch((CatchThrow) catchTarget);
    }

    public void pushCatch(CatchThrow catchTarget) {
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
    public CatchThrow getActiveCatch(Object tag) {
        for (int i = catchIndex; i >= 0; i--) {
            CatchThrow c = catchStack[i];
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
        stack[index].updateFrame(clazz, self, name, block);
        if (index + 1 == stack.length) {
            expandFrameStack();
        }
    }

    private void pushCallFrame(RubyModule clazz, String name,
                               IRubyObject self, Visibility visibility, Block block) {
        int index = ++this.frameIndex;
        Frame[] stack = frameStack;
        stack[index].updateFrame(clazz, self, name, visibility, block);
        if (index + 1 == stack.length) {
            expandFrameStack();
        }
    }

    private void pushBackrefFrame() {
        int index = ++this.frameIndex;
        Frame[] stack = frameStack;
        stack[index].updateFrameForBackref();
        if (index + 1 == stack.length) {
            expandFrameStack();
        }
    }

    private void popBackrefFrame() {
        Frame[] stack = frameStack;
        int index = frameIndex--;
        Frame frame = stack[index];

        // if the frame was captured, we must replace it but not clear
        if (frame.isCaptured()) {
            stack[index] = new Frame();
        } else {
            frame.clearFrameForBackref();
        }
    }

    private void pushEvalFrame(IRubyObject self) {
        int index = ++this.frameIndex;
        Frame[] stack = frameStack;
        stack[index].updateFrameForEval(self);
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
        if (frame.captured) {
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
     * Set the $~ (backref) "global" to nil.
     *
     * @return nil
     */
    public IRubyObject clearBackRef() {
        return getCurrentFrame().setBackRef(nil);
    }

    /**
     * Update the current frame's backref using the current thread-local match, or clear it if that match is null.
     *
     * @return The current match, or nil
     */
    public IRubyObject updateBackref() {
        RubyMatchData match = matchData;

        if (match == null) {
            return clearBackRef();
        }

        match.use();

        return getCurrentFrame().setBackRef(match);
    }

    /**
     * Set the $~ (backref) "global" to the given RubyMatchData value. The value will be marked as "in use" since it
     * can now be seen across threads that share the current frame.
     *
     * @param match the value to set
     * @return the value passed in
     */
    public IRubyObject setBackRef(RubyMatchData match) {
        match.use();
        return getCurrentFrame().setBackRef(match);
    }

    /**
     * Get the value of the $~ (backref) "global".
     *
     * @return the value of $~
     */
    public IRubyObject getBackRef() {
        return frameStack[frameIndex].getBackRef(nil);
    }

    /**
     * MRI: rb_reg_last_match
     */
    public IRubyObject last_match() {
        return RubyRegexp.nth_match(0, frameStack[frameIndex].getBackRef(nil));
    }

    /**
     * MRI: rb_reg_match_pre
     */
    public IRubyObject match_pre() {
        return RubyRegexp.match_pre(frameStack[frameIndex].getBackRef(nil));
    }


    /**
     * MRI: rb_reg_match_post
     */
    public IRubyObject match_post() {
        return RubyRegexp.match_post(frameStack[frameIndex].getBackRef(nil));
    }

    /**
     * MRI: rb_reg_match_last
     */
    public IRubyObject match_last() {
        return RubyRegexp.match_last(frameStack[frameIndex].getBackRef(nil));
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
     * Check if a scope is present on the call stack.
     * This is the IR equivalent of isJumpTargetAlive
     *
     * @param scope the scope to look for
     * @return true if it exists. otherwise false.
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

    /**
     * Poll for thread events that should be fired before a blocking call.
     *
     * See vm_check_ints_blocking and RUBY_VM_CHECK_INTS_BLOCKING in CRuby.
     */
    public void blockingThreadPoll() {
        thread.blockingThreadPoll(this);
    }

    public static void callThreadPoll(ThreadContext context) {
        if ((context.callNumber++ & CALL_POLL_COUNT) == 0) context.pollThreadEvents();
    }

    public void trace(RubyEvent event, String name, RubyModule implClass) {
        trace(event, name, implClass, backtrace[backtraceIndex].filename, backtrace[backtraceIndex].line + 1);
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

    /**
     * Render the current backtrace as a string to the given StringBuilder. This will honor the currently-configured
     * backtrace format and content.
     *
     * @param sb the StringBuilder to which to render the backtrace
     */
    public void renderCurrentBacktrace(StringBuilder sb) {
        TraceType traceType = runtime.getInstanceConfig().getTraceType();
        BacktraceData backtraceData = traceType.getBacktrace(this);
        traceType.getFormat().renderBacktrace(backtraceData.getBacktrace(runtime), sb, false);
    }

    public static final StackWalker WALKER = StackWalker.getInstance();
    public static final StackWalker WALKER8 = new StackWalker8();

    /**
     * Create an Array with backtrace information for Kernel#caller
     * @param level
     * @param length
     * @return an Array with the backtrace
     */
    public IRubyObject createCallerBacktrace(int level, int length, Stream<StackWalker.StackFrame> stackStream) {
        runtime.incrementCallerCount();

        RubyStackTraceElement[] fullTrace = getPartialTrace(level, length, stackStream);

        int traceLength = safeLength(level, length, fullTrace);

        // MRI started returning [] instead of nil some time after 1.9 (#4891)
        if (traceLength < 0) return runtime.newEmptyArray();

        final IRubyObject[] traceArray = new IRubyObject[traceLength];

        for (int i = 0; i < traceLength; i++) {
            traceArray[i] = RubyStackTraceElement.to_s_mri(this, fullTrace[i + level]);
        }

        RubyArray backTrace = RubyArray.newArrayMayCopy(runtime, traceArray);
        if (RubyInstanceConfig.LOG_CALLERS) TraceType.logCaller(backTrace);
        return backTrace;
    }

    /**
     * Create an array containing Thread::Backtrace::Location objects for the
     * requested caller trace level and length.
     *
     * @param level the level at which the trace should start
     * @param length the length of the trace
     * @return an Array with the backtrace locations
     */
    public IRubyObject createCallerLocations(int level, Integer length, Stream<StackWalker.StackFrame> stackStream) {
        runtime.incrementCallerCount();

        RubyStackTraceElement[] fullTrace = getPartialTrace(level, length, stackStream);

        int traceLength = safeLength(level, length, fullTrace);

        // MRI started returning [] instead of nil some time after 1.9 (#4891)
        if (traceLength < 0) return runtime.newEmptyArray();

        RubyArray backTrace = RubyThread.Location.newLocationArray(runtime, fullTrace, level, traceLength);
        if (RubyInstanceConfig.LOG_CALLERS) TraceType.logCaller(backTrace);
        return backTrace;
    }

    private RubyStackTraceElement[] getFullTrace(Integer length, Stream<StackWalker.StackFrame> stackStream) {
        if (length != null && length == 0) return RubyStackTraceElement.EMPTY_ARRAY;
        return TraceType.Gather.CALLER.getBacktraceData(this, stackStream).getBacktrace(runtime);
    }

    private RubyStackTraceElement[] getPartialTrace(int level, Integer length, Stream<StackWalker.StackFrame> stackStream) {
        if (length != null && length == 0) return RubyStackTraceElement.EMPTY_ARRAY;
        return TraceType.Gather.CALLER.getBacktraceData(this, stackStream).getPartialBacktrace(runtime, level + length);
    }

    private static int safeLength(int level, Integer length, RubyStackTraceElement[] trace) {
        final int baseLength = trace.length - level;
        return Math.min(length, baseLength);
    }

    /**
     * Return a single RubyStackTraceElement representing the nearest Ruby stack trace element.
     *
     * Used for warnings and Kernel#__dir__.
     *
     * @return the nearest stack trace element
     */
    public RubyStackTraceElement getSingleBacktrace(int level) {
        runtime.incrementWarningCount();

        RubyStackTraceElement[] trace = WALKER.walk(stream -> getPartialTrace(level, 1, stream));

        if (RubyInstanceConfig.LOG_WARNINGS) TraceType.logWarning(trace);

        return trace.length == 0 ? null : trace[trace.length - 1];
    }

    /**
     * Same as calling getSingleBacktrace(0);
     *
     * @see #getSingleBacktrace(int)
     */
    public RubyStackTraceElement getSingleBacktrace() {
        return getSingleBacktrace(0);
    }

    public boolean isEventHooksEnabled() {
        return eventHooksEnabled;
    }

    public void setEventHooksEnabled(boolean flag) {
        eventHooksEnabled = flag;
    }

    /**
     * Create a snapshot Array with current backtrace information.
     * @return the backtrace
     */
    public Stream<BacktraceElement> getBacktrace() {
        return getBacktrace(0);
    }

    public final Stream<BacktraceElement> getBacktrace(int level) {
        int backtraceIndex = this.backtraceIndex;
        if ( level < 0 ) level = backtraceIndex + 1 + level;
        int end = backtraceIndex - level;
        BacktraceElement[] backtrace = this.backtrace;
        return IntStream.rangeClosed(0, end).mapToObj(i -> backtrace[backtraceIndex - i]);
    }

    public static String createRawBacktraceStringFromThrowable(final Throwable ex, final boolean color) {
        return WALKER.walk(ex.getStackTrace(), stream ->
            TraceType.printBacktraceJRuby(null,
                    new BacktraceData(stream, Stream.empty(), true, true, false, false).getBacktraceWithoutRuby(),
                    ex.getClass().getName(),
                    ex.getLocalizedMessage(),
                    color));
    }

    private Frame pushFrameForBlock(Binding binding) {
        Frame lastFrame = getNextFrame();

        Frame bindingFrame = binding.getFrame();
        bindingFrame.setVisibility(binding.getVisibility());
        pushFrame(bindingFrame);

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

    public void preMethodFrameOnly(RubyModule clazz, String name, IRubyObject self, Visibility visiblity, Block block) {
        pushCallFrame(clazz, name, self, visiblity, block);
    }

    public void preMethodFrameOnly(RubyModule clazz, String name, IRubyObject self) {
        pushCallFrame(clazz, name, self, Block.NULL_BLOCK);
    }

    public void preBackrefMethod() {
        pushBackrefFrame();
    }

    public void postMethodFrameOnly() {
        popFrame();
    }

    public void postBackrefMethod() {
        popBackrefFrame();
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

        getCurrentStaticScope().setModule(objectClass);
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

    public Frame preYieldSpecificBlock(Binding binding, StaticScope scope) {
        Frame lastFrame = preYieldNoScope(binding);
        // new scope for this invocation of the block, based on parent scope
        pushScope(DynamicScope.newDynamicScope(scope, binding.getDynamicScope()));
        return lastFrame;
    }

    public Frame preYieldNoScope(Binding binding) {
        return pushFrameForBlock(binding);
    }

    @JIT
    public Frame preYieldNoScope(Block block) {
        return pushFrameForBlock(block.getBinding());
    }

    public void preEvalScriptlet(DynamicScope scope) {
        pushScope(scope);
    }

    public void postEvalScriptlet() {
        popScope();
    }

    public Frame preEvalWithBinding(Binding binding) {
        return pushFrameForBlock(binding);
    }

    public void postEvalWithBinding(Binding binding, Frame lastFrame) {
        popFrameReal(lastFrame);
    }

    public void postYield(Binding binding, Frame lastFrame) {
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
        profileCollection = runtime.getProfilingService().newProfileCollection( this );
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
        // profiled method is added in the MethodEnhancer (if necessary)
        // @see BuiltinProfilingService.DefaultMethodEnhancer
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

    public void setExceptionRequiresBacktrace(boolean exceptionRequiresBacktrace) {
        if (runtime.isDebug()) return; // leave true default
        this.exceptionRequiresBacktrace = exceptionRequiresBacktrace;
    }

    @JIT
    public void exceptionBacktraceOn() {
        setExceptionRequiresBacktrace(true);
    }

    @JIT
    public void exceptionBacktraceOff() {
        setExceptionRequiresBacktrace(false);
    }

    private Map<String, Map<IRubyObject, IRubyObject>> symToGuards;

    // Thread#set_trace_func of nil will not only remove the one via set_trace_func but also any which
    // were added via add_trace_func.
    public IRubyObject clearThreadTraceFunctions() {
        // We called Thread#set_trace_func.  Remove it here since all thread trace funcs are going away.
        if (traceFuncHook != null) traceFuncHook = null;

        runtime.removeAllCallEventHooksFor(this);

        return nil;
    }

    public IRubyObject addThreadTraceFunction(IRubyObject trace_func, boolean useContextHook) {
        if (!(trace_func instanceof RubyProc)) throw runtime.newTypeError("trace_func needs to be Proc.");

        Ruby.CallTraceFuncHook hook;

        if (useContextHook) {
            hook = traceFuncHook;
            if (hook == null) {
                hook = new Ruby.CallTraceFuncHook(this);
                traceFuncHook = hook;
            }
        } else {
            hook = new Ruby.CallTraceFuncHook(this);
        }
        runtime.setTraceFunction(hook, (RubyProc) trace_func);

        return trace_func;
    }


    public IRubyObject setThreadTraceFunction(IRubyObject trace_func) {
        return addThreadTraceFunction(trace_func, true);
    }

    public void setPrivateConstantReference(RubyModule privateConstantReference) {
        this.privateConstantReference = privateConstantReference;
    }

    public RubyModule getPrivateConstantReference() {
        return privateConstantReference;
    }

    private static class RecursiveError extends Error implements Unrescuable {
        public RecursiveError(Object tag) {
            this.tag = tag;
        }
        public final Object tag;

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    public interface RecursiveFunctionEx<T> {
        IRubyObject call(ThreadContext context, T state, IRubyObject obj, boolean recur);
    }

    public <T> IRubyObject safeRecurse(RecursiveFunctionEx<T> func, T state, IRubyObject obj, String name, boolean outer) {
        Map<IRubyObject, IRubyObject> guards = safeRecurseGetGuards(name);

        boolean outermost = outer && !guards.containsKey(NEVER);

        // check guards
        if (guards.containsKey(obj)) {
            if (outer && !outermost) {
                throw new RecursiveError(guards);
            }
            return func.call(this, state, obj, true);
        } else {
            if (outermost) {
                return safeRecurseOutermost(func, state, obj, guards);
            } else {
                return safeRecurseInner(func, state, obj, guards);
            }
        }
    }

    private <T> IRubyObject safeRecurseOutermost(RecursiveFunctionEx<T> func, T state, IRubyObject obj, Map<IRubyObject, IRubyObject> guards) {
        boolean recursed = false;
        guards.put(NEVER, NEVER);

        try {
            return safeRecurseInner(func, state, obj, guards);
        } catch (RecursiveError re) {
            if (re.tag != guards) {
                throw re;
            }
            recursed = true;
            guards.remove(NEVER);
            return func.call(this, state, obj, true);
        } finally {
            if (!recursed) guards.remove(NEVER);
        }
    }

    private Map<IRubyObject, IRubyObject> safeRecurseGetGuards(String name) {
        Map<String, Map<IRubyObject, IRubyObject>> symToGuards = this.symToGuards;
        if (symToGuards == null) {
            this.symToGuards = symToGuards = new HashMap<>();
        }

        Map<IRubyObject, IRubyObject> guards = symToGuards.get(name);
        if (guards == null) {
            guards = new IdentityHashMap<>();
            symToGuards.put(name, guards);
        }

        return guards;
    }

    private <T> IRubyObject safeRecurseInner(RecursiveFunctionEx<T> func, T state, IRubyObject obj, Map<IRubyObject, IRubyObject> guards) {
        try {
            guards.put(obj, obj);
            return func.call(this, state, obj, false);
        } finally {
            guards.remove(obj);
        }
    }

    private Set<RecursiveComparator.Pair> recursiveSet;

    // Do we have to generate a backtrace when we generate an exception on this thread or can we
    // MAYBE omit creating the backtrace for the exception (only some rescue forms and only for
    // descendents of StandardError are eligible).
    public boolean exceptionRequiresBacktrace = true;

    public Encoding[] encodingHolder() {
        if (encodingHolder == null) encodingHolder = new Encoding[1];
        return encodingHolder;
    }

    /**
     * Set the thread-local MatchData specific to this context. This is different from the frame backref since frames
     * may be shared by several executing contexts at once (see jruby/jruby#4868).
     *
     * @param localMatch the new thread-local MatchData or null
     */
    public void setLocalMatch(RubyMatchData localMatch) {
        matchData = localMatch;
    }

    /**
     * Set the thread-local MatchData specific to this context to null.
     *
     * @see #setLocalMatch(RubyMatchData)
     */
    public void clearLocalMatch() {
        matchData = null;
    }

    /**
     * Get the thread-local MatchData specific to this context. This is different from the frame backref since frames
     * may be shared by several executing contexts at once (see jruby/jruby#4868).
     *
     * @return the current thread-local MatchData, or null if none
     */
    public RubyMatchData getLocalMatch() {
        return matchData;
    }

    /**
     * Get the thread-local MatchData specific to this context or nil if none.
     *
     * @see #getLocalMatch()
     *
     * @return the current thread-local MatchData, or nil if none
     */
    public IRubyObject getLocalMatchOrNil() {
        RubyMatchData matchData = this.matchData;
        if (matchData != null) return matchData;
        return nil;
    }

    @Deprecated
    public IRubyObject setBackRef(IRubyObject match) {
        if (match.isNil()) return clearBackRef();

        return setBackRef((RubyMatchData) match);
    }
}
