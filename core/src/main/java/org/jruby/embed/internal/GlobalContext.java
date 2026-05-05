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
 * Copyright (C) 2009-2011 Yoko Harada <yokolet@gmail.com>
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

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.embed.LocalVariableBehavior;

/**
 * {@link LocalContext} instance that uses the {@link Ruby#getGlobalRuntime()
 * global runtime}. Each instance still has its own {@link BiVariableMap
 * variables} and attributes.
 * <p>
 * Used by {@link ConcurrentLocalContextProvider} with one instance per thread,
 * thus providing one set of variables per thread. A singleton instance is also
 * held by {@link SingletonLocalContextProvider}, providing one global set of
 * variables across the classloader that loaded JRuby.
 */
class GlobalContext extends LocalContext {
	public GlobalContext(RubyInstanceConfig config, LocalVariableBehavior behavior) {
		this(config, behavior, false);
	}

	public GlobalContext(RubyInstanceConfig config, LocalVariableBehavior behavior, boolean lazy) {
		super(config, behavior, lazy);
	}

	@Override
	Ruby getRuntime() {
		return getGlobalRuntime();
	}

	@Override
	boolean isInitialized() {
		return Ruby.isGlobalRuntimeReady();
	}

	@Override
	RubyInstanceConfig getRubyInstanceConfig() {
		return getGlobalRuntimeConfig();
	}

	/**
	 * Get a reference to the Classloader-global Ruby runtime. If nothing else
	 * messes with the global runtime (the common case), then it will be configured
	 * according to the {@link RubyInstanceConfig} provided in the constructor.
	 * <p>
	 * If some other thread initializes a global runtime first, or if
	 * {@link Ruby#useAsGlobalRuntime()} or
	 * {@code JRuby.runtime.use_as_global_runtime} is called, then this method
	 * behaves like {@link Ruby#getGlobalRuntime()} and its
	 * {@link RubyInstanceConfig} may differ from the {@link LocalContextProvider}.
	 */
	Ruby getGlobalRuntime() {
		// this method can run concurrently in several threads in
		// ConcurrentLocalContextProvider
		if (isInitialized())
			return Ruby.getGlobalRuntime();

		// global runtime not yet set. create one with the requested configuration
		Ruby runtime = Ruby.newInstance(config);
		if (runtime != Ruby.getGlobalRuntime())
			// boundary case: while we weren't looking, some other thread initialized the
			// global runtime. when only a single LocalContextProvider is used, the other
			// thread will have created it with the same configuration. even if it's
			// configured differently, we can use that other runtime (and should, for
			// consistency). but we do need to cleanly dispose of the one we just created.
			runtime.tearDown();
		return Ruby.getGlobalRuntime();
	}

	RubyInstanceConfig getGlobalRuntimeConfig() {
		// make sure we do not yet initialize the runtime here
		if (isInitialized())
			return getGlobalRuntime().getInstanceConfig();

		return config;
	}
}
