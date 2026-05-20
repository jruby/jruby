/*
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
 * Copyright (C) 2009-2012 Yoko Harada <yokolet@gmail.com>
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
import java.util.HashSet;
import java.util.Set;

import org.jruby.RubyClass;
import org.jruby.embed.variable.BiVariable.Type;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * An implementation of BiVariable for a Ruby constant.
 *
 * @author Yoko Harada
 *         &lt;<a href="mailto:yokolet@gmail.com">yokolet@gmail.com</a>&gt;
 */
class ConstantAccessor extends VariableAccessor {
	private static final String VALID_NAME = "[A-Z]([a-zA-Z]|_)([a-zA-Z]|_|\\d)*";

	ConstantAccessor() {
		super(VALID_NAME, true, Type.Constant);
	}

	@Override
	Collection<String> keySet(BiVariableMap map, IRubyObject receiver) {
		if (receiver == null) {
			final Set<String> result = new HashSet<>();
			final RubyClass klazz = map.getRuntime().getTopSelf().getMetaClass();
			result.addAll(klazz.getConstantNames());
			result.addAll(klazz.getSuperClass().getConstantNames());
			return result;
		} else
			return receiver.getMetaClass().getConstantNames();
	}

	@Override
	boolean containsKey(BiVariableMap map, IRubyObject receiver, String name) {
		if (receiver == null) {
			// avoid creating a large temp collection just to look for a single value
			final RubyClass klazz = map.getRuntime().getTopSelf().getMetaClass();
			if (klazz.getConstantNames().contains(name))
				return true;
			return klazz.getSuperClass().getConstantNames().contains(name);
		} else
			return receiver.getMetaClass().getConstantNames().contains(name);
	}

	@Override
	IRubyObject retrieve(final BiVariableMap map, final IRubyObject receiver, final String name) {
		if (receiver == null) {
			final RubyClass klazz = map.getRuntime().getTopSelf().getMetaClass();
			if (klazz.getConstantNames().contains(name))
				return klazz.getConstant(name);
			// also obtain constants from top self's superclass. this isn't entirely
			// consistent (they cannot be removed) but should be useful for getting
			// top-level constants
			return klazz.getSuperClass().getConstant(name);
		} else {
			final RubyClass klazz = receiver.getMetaClass();
			if (klazz.getConstantNames().contains(name))
				return klazz.getConstant(name);
			return null;
		}
	}

	@Override
	void inject(final BiVariableMap map, final IRubyObject receiver, final String name, final IRubyObject value) {
		if (receiver == null)
			map.getRuntime().getTopSelf().getMetaClass().storeConstant(name, value);
		else
			receiver.getMetaClass().storeConstant(name, value);
		map.getRuntime().getConstantInvalidator(name).invalidate();
	}

	@Override
	void remove(final BiVariableMap map, final IRubyObject receiver, final String name) {
		final IRubyObject rubyName = JavaUtil.convertJavaToRuby(map.getRuntime(), name);
		final ThreadContext context = map.getRuntime().getCurrentContext();
		if (receiver == null) {
			final RubyClass klazz = map.getRuntime().getTopSelf().getMetaClass();
			// remove_const throws an exception if the named constant is missing, so we
			// always need to check for existence first
			if (klazz.getConstantNames().contains(name))
				klazz.remove_const(context, rubyName);
		} else {
			final RubyClass klazz = receiver.getMetaClass();
			if (klazz.getConstantNames().contains(name))
				klazz.remove_const(context, rubyName);
		}
		// we should probably invalidate the constant here. otherwise code might keep
		// using the previous value
		map.getRuntime().getConstantInvalidator(name).invalidate();
	}
}
