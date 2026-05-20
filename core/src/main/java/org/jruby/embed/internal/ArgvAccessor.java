/**
 * **** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2011 Yoko Harada <yokolet@gmail.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed.internal;

import java.util.Collection;
import java.util.Set;

import org.jruby.RubyArray;
import org.jruby.embed.variable.BiVariable.Type;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author yoko
 */
class ArgvAccessor extends VariableAccessor {
	private static final String NAME = "ARGV";

	public ArgvAccessor() {
		super(NAME, false, Type.Argv);
	}

	@Override
	Set<String> keySet(BiVariableMap map, IRubyObject receiver) {
		return Set.of(NAME);
	}

	@Override
	IRubyObject retrieve(BiVariableMap map, IRubyObject receiver, String name) {
		return map.getRuntime().getTopSelf().getMetaClass().getConstant(NAME);
	}

	@Override
	void inject(BiVariableMap map, IRubyObject receiver, String name, IRubyObject value) {
		// FIXME cast string[] and lists to RubyArray
		// FIXME probably simply need to reject non-arrays here
		if (value instanceof RubyArray)
			map.getRuntime().getTopSelf().getMetaClass().storeConstant(NAME, value);
		/*
		 * FIXME straight-forward except for weird casting of the java object: it gets
		 * converted to an array if possible... or just ignored if not. more precisely:
		 * Collection and String[] are converted to a ruby array (of string for
		 * String[], and of potentially weird things for Collection). everything else is
		 * silently turned into an empty array, no warnings or anything :/
		 */
		map.getRuntime().getConstantInvalidator(NAME).invalidate();
	}

	@Override
	void remove(BiVariableMap map, IRubyObject receiver, String name) {
		// cannot remove ARGV, so we just set it to an empty list instead
		inject(map, receiver, name, RubyArray.newArray(map.getRuntime()));
	}
}
