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

import org.jruby.RubyThread;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author cnutter
 */
public class NativeThread {
	private Thread nativeThread;
	public RubyThread rubyThread;
	
	public NativeThread(RubyThread rubyThread, IRubyObject[] args) {
		this.rubyThread = rubyThread;
		
		nativeThread = new RubyNativeThread(rubyThread, args);
	}
	
	public NativeThread(RubyThread rubyThread, Thread nativeThread) {
		this.rubyThread = rubyThread;
		this.nativeThread = nativeThread;
	}

	public void start() {
		nativeThread.start();
	}

	public void interrupt() {
		nativeThread.interrupt();
	}

	public boolean isAlive() {
		return nativeThread.isAlive();
	}

	public void join() throws InterruptedException {
		nativeThread.join();
	}

	public int getPriority() {
		return nativeThread.getPriority();
	}

	public void setPriority(int priority) {
		nativeThread.setPriority(priority);
	}
	
	public boolean isCurrent() {
		return Thread.currentThread() == nativeThread;
	}
	
	public boolean isInterrupted() {
		return nativeThread.isInterrupted();
	}
}
