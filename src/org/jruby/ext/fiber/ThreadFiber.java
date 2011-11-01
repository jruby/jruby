package org.jruby.ext.fiber;

import java.util.concurrent.locks.LockSupport;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyLocalJumpError.Reason;
import org.jruby.anno.JRubyClass;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name = "Fiber")
public class ThreadFiber extends Fiber {
    private volatile IRubyObject result;
    private Runnable runnable;
    private ThreadFiberState state = ThreadFiberState.NOT_STARTED;
    private ThreadFiber transferredFrom;
    private ThreadFiber transferredTo;
    private volatile Thread waiter = null;
    private volatile Thread fiber = null;

    public ThreadFiber(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }
    
    protected void initFiber(ThreadContext context) {
        final Ruby runtime = context.runtime;
        
        this.result = runtime.getNil();
        this.waiter = Thread.currentThread();
        
        this.runnable = new Runnable() {

            public void run() {
                ThreadContext context = runtime.getCurrentContext();
                context.setFiber(ThreadFiber.this);
                // initialize fiber state and transfer back to launching thread
                fiber = Thread.currentThread();
                state = ThreadFiberState.YIELDED;
                LockSupport.unpark(waiter);
                LockSupport.park();
                try {
                    // first resume, dive into the block
                    result = block.yieldArray(context, result, null, null);
                } catch (JumpException.RetryJump rtry) {
                    // FIXME: technically this should happen before the block is executed
                    parent.raise(new IRubyObject[]{runtime.newSyntaxError("Invalid retry").getException()}, Block.NULL_BLOCK);
                } catch (JumpException.BreakJump brk) {
                    parent.raise(new IRubyObject[]{runtime.newLocalJumpError(Reason.BREAK, runtime.getNil(), "break from proc-closure").getException()}, Block.NULL_BLOCK);
                } catch (JumpException.ReturnJump ret) {
                    parent.raise(new IRubyObject[]{runtime.newLocalJumpError(Reason.RETURN, runtime.getNil(), "unexpected return").getException()}, Block.NULL_BLOCK);
                } catch (RaiseException re) {
                    // re-raise exception in parent thread
                    parent.raise(new IRubyObject[]{re.getException()}, Block.NULL_BLOCK);
                } finally {
                    state = ThreadFiberState.FINISHED;
                    LockSupport.unpark(waiter);
                }
            }
        };
        // submit job and wait to be resumed
        context.runtime.getExecutor().execute(runnable);
        LockSupport.park();
    }

    protected IRubyObject resumeOrTransfer(ThreadContext context, IRubyObject arg, boolean transfer) {
        result = arg;
        try {
            switch (state) {
                case NOT_STARTED:
                    if (isRoot()) {
                        state = ThreadFiberState.RUNNING;
                        return result;
                    }
                    context.runtime.getExecutor().execute(runnable);
                case YIELDED:
                    if (!transfer && transferredTo != null) {
                        throw context.getRuntime().newFiberError("double resume");
                    }
                    if (transfer) {
                        transferredFrom = (ThreadFiber)context.getFiber();
                        transferredFrom.transferredTo = this;
                    }
                    // update result and transfer to fiber
                    waiter = Thread.currentThread();
                    LockSupport.unpark(fiber);
                    LockSupport.park();
                    // back from fiber, poll events and proceed out of resume
                    context.pollThreadEvents();
                    if (transfer) {
                        if (!transferredFrom.isRoot()) {
                            result = transferredFrom.yield(context, result);
                        }
                        transferredFrom.transferredTo = null;
                        transferredFrom = null;
                    }
                    return result;
                case RUNNING:
                    if (transfer && context.getFiber() == this) {
                        return result;
                    }
                    throw context.getRuntime().newFiberError("double resume");
                case FINISHED:
                    throw context.getRuntime().newFiberError("dead fiber called");
                default:
                    throw context.getRuntime().newFiberError("fiber in an unknown state");
            }
        } catch (OutOfMemoryError oome) {
            if (oome.getMessage().equals("unable to create new native thread")) {
                throw context.runtime.newThreadError("too many threads, can't create a new Fiber");
            }
            throw oome;
        }
    }

    public IRubyObject yield(ThreadContext context, IRubyObject res) {
        result = res;
        state = ThreadFiberState.YIELDED;
        LockSupport.unpark(waiter);
        LockSupport.park();
        // back into fiber
        context.pollThreadEvents();
        state = ThreadFiberState.RUNNING;
        return result;
    }
    
    public boolean isAlive() {
        return state != ThreadFiberState.FINISHED;
    }
    
}
