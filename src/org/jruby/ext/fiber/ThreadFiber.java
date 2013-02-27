package org.jruby.ext.fiber;

import java.util.concurrent.Exchanger;

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
    private final Exchanger<IRubyObject> exchanger = new Exchanger<IRubyObject>();
    private volatile ThreadFiberState state = ThreadFiberState.NOT_STARTED;

    public ThreadFiber(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }
    
    protected void initFiber(ThreadContext context) {
        final Ruby runtime = context.runtime;
        
        Runnable runnable = new Runnable() {

            public void run() {
                // initialize and yield back to launcher
                ThreadContext context = runtime.getCurrentContext();
                context.setThread(parent);
                context.setFiber(ThreadFiber.this);
                IRubyObject result = yield(context, context.nil);

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
                    try {
                        // ensure we do a final exchange to release any waiters
                        exchanger.exchange(result);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }
        };

        // submit job and wait to be resumed
        context.runtime.getExecutor().execute(runnable);
        try {
            exchanger.exchange(context.nil);
        } catch (InterruptedException ie) {
            throw runtime.newConcurrencyError("interrupted while waiting for fiber to start");
        }
    }

    protected IRubyObject resumeOrTransfer(ThreadContext context, IRubyObject arg, boolean transfer) {
        try {
            switch (state) {
                case NOT_STARTED:
                    if (isRoot()) {
                        state = ThreadFiberState.RUNNING;
                        return arg;
                    } else if (context.getThread() != parent) {
                        throw context.runtime.newFiberError("resuming fiber from different thread");
                    }
                    throw context.runtime.newRuntimeError("BUG: resume before fiber is started");
                case YIELDED:
                    if (!transfer && transferredTo != null) {
                        throw context.runtime.newFiberError("double resume");
                    }

                    // update transfer fibers
                    if (transfer) {
                        transferredFrom = (ThreadFiber)context.getFiber();
                        transferredFrom.transferredTo = this;
                    }

                    // transfer to fiber
                    exchanger.exchange(arg);
                    arg = exchanger.exchange(context.nil);

                    // back from fiber, poll events
                    context.pollThreadEvents();

                    // complete transfer
                    if (transfer) {
                        if (!transferredFrom.isRoot()) {
                            arg = transferredFrom.yield(context, arg);
                        }
                        transferredFrom.transferredTo = null;
                        transferredFrom = null;
                    }

                    // return new result
                    return arg;
                case RUNNING:
                    if (transfer && context.getFiber() == this) {
                        return arg;
                    }
                    throw context.runtime.newFiberError("double resume");
                case FINISHED:
                    throw context.runtime.newFiberError("dead fiber called");
                default:
                    throw context.runtime.newFiberError("fiber in an unknown state");
            }
        } catch (OutOfMemoryError oome) {
            if (oome.getMessage().equals("unable to create new native thread")) {
                throw context.runtime.newThreadError("too many threads, can't create a new Fiber");
            }
            throw oome;
        } catch (InterruptedException ie) {
            throw context.runtime.newConcurrencyError("interrupted waiting for fiber");
        }
    }

    public IRubyObject yield(ThreadContext context, IRubyObject res) {
        try {
            state = ThreadFiberState.YIELDED;
            exchanger.exchange(res);
            res = exchanger.exchange(context.nil);
            
            // back into fiber
            context.pollThreadEvents();
            state = ThreadFiberState.RUNNING;
            return res;
        } catch (InterruptedException ie) {
            throw context.runtime.newConcurrencyError("interrupted while waiting for fiber to start");
        }
    }
    
    public boolean isAlive() {
        return state != ThreadFiberState.FINISHED;
    }
    
}
