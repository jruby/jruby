/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ext.fiber;

import java.dyn.Coroutine;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyLocalJumpError.Reason;
import org.jruby.anno.JRubyClass;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
@JRubyClass(name = "Fiber")
public class CoroutineFiber extends Fiber {
    private volatile IRubyObject slot;
    private CoroutineFiberState state;
    private CoroutineFiber lastFiber;
    private Coroutine coro;
    private ThreadContext context;
    private JumpException coroException;

    public CoroutineFiber(Ruby runtime, RubyClass type, Coroutine coro) {
        super(runtime, type);
        this.root = coro != null;
        this.coro = coro;
        assert root ^ runtime.getCurrentContext().getFiber() != null;
    }
    
    protected void initFiber(ThreadContext context) {
        final Ruby runtime = context.runtime;
        
        this.slot = runtime.getNil();
        this.context = root ? context : ThreadContext.newContext(runtime);
        this.context.setFiber(this);
        this.context.setThread(context.getThread());

        this.state = CoroutineFiberState.SUSPENDED_YIELD;
        if (coro == null) {
            coro = new Coroutine() {

                @Override
                protected void run() {
                    try {
                        // first resume, dive into the block
                        slot = block.yieldArray(CoroutineFiber.this.context, slot, null, null);
                    } catch (JumpException t) {
                        coroException = t;
                    } finally {
                        state = CoroutineFiberState.FINISHED;
                    }
                }
            };
        }
    }

    protected IRubyObject resumeOrTransfer(ThreadContext context, IRubyObject arg, boolean transfer) {
        CoroutineFiber current = (CoroutineFiber)context.getFiber();
        Ruby runtime = context.runtime;
        slot = arg;

        if (context.getThread() != parent) {
            throw context.runtime.newFiberError("resuming fiber from different thread: " + this);
        }

        switch (state) {
            case SUSPENDED_YIELD:
                if (transfer) {
                    current.state = CoroutineFiberState.SUSPENDED_TRANSFER;
                } else {
                    current.state = CoroutineFiberState.SUSPENDED_RESUME;
                    lastFiber = (CoroutineFiber)context.getFiber();
                }
                state = CoroutineFiberState.RUNNING;
                runtime.getThreadService().setCurrentContext(context);
                context.setThread(context.getThread());
                Coroutine.yieldTo(coro);
                break;
            case SUSPENDED_TRANSFER:
                if (!transfer) {
                    throw runtime.newFiberError("double resume");
                }
                current.state = CoroutineFiberState.SUSPENDED_TRANSFER;
                state = CoroutineFiberState.RUNNING;
                runtime.getThreadService().setCurrentContext(context);
                context.setThread(context.getThread());
                Coroutine.yieldTo(coro);
                break;
            case FINISHED:
                throw runtime.newFiberError("dead fiber called");
            default:
                throw runtime.newFiberError("fiber in an invalid state: " + state);
        }

        try {
            if (coroException != null) {
                throw coroException;
            }
        } catch (JumpException.RetryJump rtry) {
            // FIXME: technically this should happen before the block is executed
            context.getThread().raise(new IRubyObject[]{runtime.newSyntaxError("Invalid retry").getException()}, Block.NULL_BLOCK);
        } catch (JumpException.BreakJump brk) {
            context.getThread().raise(new IRubyObject[]{runtime.newLocalJumpError(Reason.BREAK, runtime.getNil(), "break from proc-closure").getException()}, Block.NULL_BLOCK);
        } catch (JumpException.ReturnJump ret) {
            context.getThread().raise(new IRubyObject[]{runtime.newLocalJumpError(Reason.RETURN, runtime.getNil(), "unexpected return").getException()}, Block.NULL_BLOCK);
        }

        // back from fiber, poll events and proceed out of resume
        context.pollThreadEvents();
        return slot;
    }

    public IRubyObject yield(ThreadContext context, IRubyObject arg) {
        assert !root;
        if (lastFiber.state != CoroutineFiberState.SUSPENDED_RESUME) {
            if (lastFiber.state == CoroutineFiberState.SUSPENDED_TRANSFER) {
                throw context.runtime.newFiberError("a Fiber that was transferred to cannot yield");
            }
            throw context.runtime.newFiberError("invalid state of last Fiber at yield: " + lastFiber.state);
        }
        slot = arg;
        state = CoroutineFiberState.SUSPENDED_YIELD;
        lastFiber.state = CoroutineFiberState.RUNNING;
        context.runtime.getThreadService().setCurrentContext(lastFiber.context);
        Object o = lastFiber.context;
        lastFiber.context.setThread(context.getThread());
        Coroutine.yieldTo(lastFiber.coro);
        // back from fiber, poll events and proceed out of resume
        context.pollThreadEvents();
        return slot;
    }
    
    public boolean isAlive() {
        return state != CoroutineFiberState.FINISHED;
    }
    
}
