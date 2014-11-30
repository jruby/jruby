/*
 * Adapted from https://github.com/rubinius/rubinius-core-api.
 *
 * Copyright (c) 2011, Evan Phoenix
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of the Evan Phoenix nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jruby.truffle.runtime.rubinius;

import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyBasicObject;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class RubiniusChannel extends RubyBasicObject {
	private final LinkedBlockingQueue<RubyBasicObject> queue = new LinkedBlockingQueue<>();

	public RubiniusChannel(RubyClass cls) {
		super(cls);
	}

	public RubyBasicObject allocate(RubyClass klazz) {
		return new RubiniusChannel(klazz);
	}

	public void send(RubyBasicObject value) {
		queue.add(value);
	}

	public Object receive() {
		try {
			return queue.take();
		} catch (InterruptedException ie) {
			return false;
		}
	}

	public Object receive_timeout(long time) {
		try {
			if (time == -1) {
				return queue.take();
			} else {
				return queue.poll(time, TimeUnit.NANOSECONDS);
			}
		} catch (InterruptedException ie) {
			return false;
		}
	}

	public Object try_receive() {
		RubyBasicObject result = queue.poll();
		if (result == null)
			return getContext().getCoreLibrary().getNilObject();
		return result;
	}
}
