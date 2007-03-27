package org.jruby.internal.runtime;

import org.jruby.Ruby;
import org.jruby.RubyProc;
import org.jruby.RubyThread;
import org.jruby.RubyThreadGroup;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.ThreadKill;
import org.jruby.runtime.Block;
import org.jruby.runtime.Frame;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyRunnable implements Runnable {
    private Ruby runtime;
    private Frame currentFrame;
    private RubyProc proc;
    private IRubyObject[] arguments;
    private RubyThread rubyThread;
    
    public RubyRunnable(RubyThread rubyThread, IRubyObject[] args, Block currentBlock) {
        this.rubyThread = rubyThread;
        this.runtime = rubyThread.getRuntime();
        ThreadContext tc = runtime.getCurrentContext();
        
        proc = runtime.newProc(false, currentBlock);
        currentFrame = tc.getCurrentFrame();
        this.arguments = args;
    }
    
    public RubyThread getRubyThread() {
        return rubyThread;
    }
    
    public void run() {
        runtime.getThreadService().registerNewThread(rubyThread);
        ThreadContext context = runtime.getCurrentContext();
        
        context.preRunThread(currentFrame);
        
        // Call the thread's code
        try {
            rubyThread.notifyStarted();
            
            IRubyObject result = proc.call(arguments);
            rubyThread.cleanTerminate(result);
        } catch (ThreadKill tk) {
            // notify any killer waiting on our thread that we're going bye-bye
            synchronized (rubyThread.killLock) {
                rubyThread.killLock.notifyAll();
            }
        } catch (RaiseException e) {
            rubyThread.exceptionRaised(e);
        } finally {
            runtime.getThreadService().setCritical(false);
            ((RubyThreadGroup)rubyThread.group()).remove(rubyThread);
        }
    }
}
