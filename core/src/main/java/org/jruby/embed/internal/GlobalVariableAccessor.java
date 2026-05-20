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

import java.util.Set;

import org.jruby.embed.variable.BiVariable.Type;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * An implementation of VariableAccessor for a Ruby global variable.
 *
 * @author Yoko Harada
 *         &lt;<a href="mailto:yokolet@gmail.com">yokolet@gmail.com</a>&gt;
 */
class GlobalVariableAccessor extends VariableAccessor {
	private static final String VALID_NAME = "\\$(([a-zA-Z]|_|\\d)*|-[a-zA-Z]|[!-~&&[^#%()-\\{\\}\\[\\]\\|\\^]])";

	GlobalVariableAccessor() {
		super(VALID_NAME, false, Type.GlobalVariable);
	}

	Set<String> keySet(final BiVariableMap map, final IRubyObject receiver) {
		return map.getRuntime().getGlobalVariables().getNames();
	}

	@Override
	IRubyObject retrieve(final BiVariableMap map, final IRubyObject receiver, final String name) {
		return map.getRuntime().getGlobalVariables().get(name);
	}

	@Override
	void inject(final BiVariableMap map, final IRubyObject receiver, final String name, final IRubyObject value) {
		map.getRuntime().getGlobalVariables().set(name, value);
	}

	@Override
	void remove(final BiVariableMap map, final IRubyObject receiver, final String name) {
		map.getRuntime().getGlobalVariables().clear(name);
	}
}
