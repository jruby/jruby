/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import org.jruby.runtime.marshal.MarshalStream;

/**
 *
 * @author  jpetersen
 */
public class RubyBoolean extends RubyObject {
	private final IRuby runtime;
	
	private final boolean value;

	public RubyBoolean(IRuby runtime, boolean value) {
		super(runtime, null, // Don't initialize with class
		false); // Don't put in object space
		this.value = value;
		this.runtime = runtime;
	}
	
	public IRuby getRuntime() {
		return runtime;
	}
	
	public boolean isImmediate() {
		return true;
	}

	public Class getJavaClass() {
		return Boolean.TYPE;
	}

	public RubyClass getMetaClass() {
		return value
			? getRuntime().getClass("TrueClass")
			: getRuntime().getClass("FalseClass");
	}

	public boolean isTrue() {
		return value;
	}

	public boolean isFalse() {
		return !value;
	}

    public RubyFixnum id() {
        return getRuntime().newFixnum(value ? 2 : 0);
    }

    public static RubyClass createFalseClass(IRuby runtime) {
		RubyClass falseClass = runtime.defineClass("FalseClass", runtime.getObject());

		falseClass.defineFastMethod("type", runtime.callbackFactory(RubyBoolean.class).getMethod("type"));

		runtime.defineGlobalConstant("FALSE", runtime.getFalse());

		return falseClass;
	}

	public static RubyClass createTrueClass(IRuby runtime) {
		RubyClass trueClass = runtime.defineClass("TrueClass", runtime.getObject());

		trueClass.defineFastMethod("type", runtime.callbackFactory(RubyBoolean.class).getMethod("type"));

		runtime.defineGlobalConstant("TRUE", runtime.getTrue());

		return trueClass;
	}

	public static RubyBoolean newBoolean(IRuby runtime, boolean value) {
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

