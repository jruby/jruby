/*
 *  Copyright (C) 2004 Charles O Nutter
 * 
 * Charles O Nutter <headius@headius.com>
 *
 *  JRuby - http://jruby.sourceforge.net
 *
 *  This file is part of JRuby
 *
 *  JRuby is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  JRuby is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with JRuby; if not, write to
 *  the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA  02111-1307 USA
 */
package org.jruby.internal.runtime;

import org.jruby.Ruby;
import org.jruby.RubyProc;
import org.jruby.RubyThread;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.ThreadKill;
import org.jruby.runtime.Block;
import org.jruby.runtime.Frame;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyNativeThread extends Thread {
	private Ruby runtime;
    private Frame currentFrame;
    private Block currentBlock;
    private RubyProc proc;
    private IRubyObject[] arguments;
    private RubyThread rubyThread;
    private boolean criticalized;
    private AtomicSpinlock spinlock;
    
	protected RubyNativeThread(RubyThread rubyThread, IRubyObject[] args) {
		super(rubyThread.getRuntime().getThreadService().getRubyThreadGroup(), "Ruby Thread" + rubyThread.hash());
		this.rubyThread = rubyThread;
		
		runtime = rubyThread.getRuntime();
		proc = RubyProc.newProc(runtime);
        currentFrame = runtime.getCurrentFrame();
        currentBlock = runtime.getBlockStack().getCurrent();
        this.arguments = args;
	}
	
	public RubyThread getRubyThread() {
		return rubyThread;
	}
	
	public void run() {
        rubyThread.notifyStarted();

        runtime.getThreadService().registerNewThread(rubyThread);
        ThreadContext context = runtime.getCurrentContext();
        context.getFrameStack().push(currentFrame);
        context.getBlockStack().setCurrent(currentBlock);

        // Call the thread's code
        try {
            proc.call(arguments);
        } catch (ThreadKill tk) {
            // notify any killer waiting on our thread that we're going bye-bye
            synchronized (rubyThread.killLock) {
            	rubyThread.killLock.notifyAll();
            }
        } catch (RaiseException e) {
            rubyThread.exceptionRaised(e);
        }
    }
}