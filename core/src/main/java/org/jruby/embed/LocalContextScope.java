/**
 * **** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2009-2011 Yoko Harada <yokolet@gmail.com>, CloudBees, Inc.
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
package org.jruby.embed;

import org.jruby.Ruby;
import org.jruby.embed.internal.LocalContext;

/**
 * LocalContextState defines four scopes to maintain {@link LocalContext}.
 *
 * <p>
 * A single {@link ScriptingContainer} can be configured to act as a facade for multiple {@link Ruby} runtimes
 * (or really multiple Ruby VMs, since each {@link Ruby} instance is a Ruby "VM"), and this enum controls
 * that behaviour. (this behaviour is bit like that of {@link ThreadLocal} &mdash; it changes its behaviour
 * silently depending on the calling thread, an act of multiplexing.)
 *
 * <p>
 * When you think of this multiplexing behaviour, there are two sets of states that need separate attention.
 * One is {@link Ruby} instance, which represents the whole VM, classes, global variables, etc. Then
 * there's {@linkplain ScriptingContainer#getAttributeMap() attributes} and so-called
 * {@linkplain ScriptingContainer#getVarMap() variables}, which are really a special scope induced by the
 * scripting container for better JSR-223 interop.
 * In this documentation, we refer to the former as "the runtime" and the latter as "the variables",
 * but the variables shouldn't be confused with the global variables in Ruby's semantics, which belongs
 * to the runtime.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 * @author Kohsuke Kawaguchi
 * @see https://github.com/jruby/jruby/wiki/RedBridge
 */
public enum LocalContextScope {
    /**
     * Uses a VM-wide singleton runtime and variables.
     *
     * <p>
     * All the {@link ScriptingContainer}s that are created with this scope will share a single
     * runtime and a single set of variables. Therefore one container can
     * {@link ScriptingContainer#put(String, Object) set a value} and another container will see the same value.
     */
    SINGLETON,

    /**
     * {@link ScriptingContainer} will not do any multiplexing at all.
     *
     * <p>
     * {@link ScriptingContainer} will get one runtime and one set of variables, and regardless of the calling
     * thread it'll use this same pair.
     * <p>
     * If you have multiple threads calling single {@link ScriptingContainer} instance,
     * then you may need to take caution as a variable set by one thread will be visible to another thread.
     * <p>
     * If you aren't using the variables of {@link ScriptingContainer}, this is normally what you want.
     */
    SINGLETHREAD,

    /**
     * Maintain separate runtimes and variable maps for each calling thread.
     *
     * <p>
     * Known as the "apartment thread model", this mode makes {@link ScriptingContainer} lazily
     * creates a runtime and a variable map separately for each calling thread. Therefore, despite
     * the fact that multiple threads call on the same object, they are completely isolated from each other.
     * (the flip side of the coin is that no ruby objects can be shared between threads as they belong
     * to different "ruby VM".)
     */
    THREADSAFE,

    /**
     * Use a single runtime but variable maps are thread local.
     *
     * <p>
     * In this mode, there'll be a single runtime dedicated for each {@link ScriptingContainer},
     * but a separate map of variables are created for each calling thread through {@link ThreadLocal}.
     *
     * In a situation where you have multiple threads calling one {@link ScriptingContainer}, this means
     * ruby code will see multiple threads calling them, and therefore they need to be thread safe.
     * But because variables are thread local, if your program does something like (1) set a few variables,
     * (2) evaluate a script that refers to those variables, then you won't have to worry about values from
     * multiple threads mixing with each other.
     */
    CONCURRENT
}
