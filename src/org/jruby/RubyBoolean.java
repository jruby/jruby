/*
 * RubyBoolean.java - No description
 * Created on 09. Juli 2001, 21:38
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */

package org.jruby;

import org.jruby.runtime.marshal.MarshalStream;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyBoolean extends RubyObject {
	private final boolean value;

	public RubyBoolean(Ruby runtime, boolean value) {
		super(runtime, null, // Don't initialize with class
		false); // Don't put in object space
		this.value = value;
	}
	
	public boolean isImmediate() {
		return true;
	}

	public Class getJavaClass() {
		return Boolean.TYPE;
	}

	public RubyClass getMetaClass() {
		return value
			? getRuntime().getClasses().getTrueClass()
			: getRuntime().getClasses().getFalseClass();
	}

	public boolean isTrue() {
		return value;
	}

	public boolean isFalse() {
		return !value;
	}

	public static RubyClass createFalseClass(Ruby runtime) {
		RubyClass falseClass =
			runtime.defineClass("FalseClass", runtime.getClasses().getObjectClass());

		falseClass.defineMethod(
			"type",
			runtime.callbackFactory().getMethod(RubyBoolean.class, "type"));

		runtime.defineGlobalConstant("FALSE", runtime.getFalse());

		return falseClass;
	}

	public static RubyClass createTrueClass(Ruby runtime) {
		RubyClass trueClass =
			runtime.defineClass("TrueClass", runtime.getClasses().getObjectClass());

		trueClass.defineMethod(
			"type",
			runtime.callbackFactory().getMethod(RubyBoolean.class, "type"));

		runtime.defineGlobalConstant("TRUE", runtime.getTrue());

		return trueClass;
	}

	public static RubyBoolean newBoolean(Ruby runtime, boolean value) {
        return value ? runtime.getTrue() : runtime.getFalse();
	}

	/** false_type
	 *  true_type
	 *
	 */
	public RubyClass type() {
		return getMetaClass();
	}

	public void marshalTo(MarshalStream output) throws java.io.IOException {
		output.write(isTrue() ? 'T' : 'F');
	}
}

