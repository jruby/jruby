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

/**
 * A spinlock.
 * 
 * @author cnutter
 */
public class AtomicSpinlock {
	private volatile int counter;
	
	public AtomicSpinlock() {
	}
	
	public AtomicSpinlock(int start) {
		counter = start;
	}
	
	public synchronized void increment() {
		counter++;
		notify();
	}
	
	public synchronized void decrement() {
		counter--;
		notify();
	}
	
	public synchronized void waitForZero(long timeout) throws InterruptedException {
		// TODO: timeout isn't really doing what I want here.
		while (counter > 0) {
			timeWait(timeout);
		}
	}
	
	public synchronized void waitForValue(long timeout, int value) throws InterruptedException {
		while (counter != value) {
			timeWait(timeout);
		}
	}
	
	private void timeWait(long timeout) throws InterruptedException {
		if (timeout > 0) wait(timeout);
		else wait();
	}
}
