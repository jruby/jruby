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

import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Utility class that wraps a {@link ThreadLocal} holding a {@link LocalContext}
 * for each thread. Uses the {@link Cleaner} API to remove this
 * {@code LocalContext} once either the corresponding {@code Thread} has
 * terminated, or the {@code ThreadLocalContext} instance itself becomes GC'd.
 */
class ThreadLocalContext {
	private final ConcurrentHashMap<LocalContextCleaningAction, Object> contextRefs = new ConcurrentHashMap<>();
	private final Cleaner cleaner = Cleaner.create();
	private final Supplier<LocalContext> localContextFactory;

	public ThreadLocalContext(final Supplier<LocalContext> localContextFactory) {
		this.localContextFactory = localContextFactory;
	}

	private volatile ThreadLocal<AtomicReference<LocalContextCleaningAction>> contextHolder = new ThreadLocal<AtomicReference<LocalContextCleaningAction>>() {
		@Override
		protected AtomicReference<LocalContextCleaningAction> initialValue() {
			final LocalContextCleaningAction ctx = new LocalContextCleaningAction(contextRefs,
					localContextFactory.get());
			final AtomicReference<LocalContextCleaningAction> ref = new AtomicReference<>(ctx);
			// register cleaner to run as soon as the *reference* to it gets GC'd. that
			// happens when either the Thread terminates, or the ThreadLocal itself gets
			// GC'd (i.e. when this class gets GC'd because terminate() was never called).
			// see ThreadLocal JavaDoc: ref will stay reachable "as long as the thread is
			// alive and the ThreadLocal instance is accessible"
			final Cleanable cleanable = cleaner.register(ref, ctx);
			if (contextHolder == null)
				// boundary case if we're already terminating: clean up immediately, because
				// there is no more cleanup thread to do that later
				// the returned context will be null, but that's to be expected when operating
				// on an object that has been terminated
				cleanable.clean();
			return ref;
		}
	};

	public LocalContext get() {
		return contextHolder.get().get().get();
	}

	public void terminate() {
		contextHolder = null;
		for (final LocalContextCleaningAction ref : contextRefs.keySet())
			ref.run();
	}

	/*
	 * Runnable that actually performs the cleanup for per-thread LocalContext
	 * instances. MUST be static because these are registered with a Cleaner, so
	 * everything that is GC-reachable from them will stay reachable until the
	 * cleaning action has been run.
	 */
	private static class LocalContextCleaningAction extends AtomicReference<LocalContext> implements Runnable {
		private static final long serialVersionUID = 1L;

		private final ConcurrentHashMap<LocalContextCleaningAction, Object> contextRefs;

		private LocalContextCleaningAction(final ConcurrentHashMap<LocalContextCleaningAction, Object> contextRefs,
				final LocalContext context) {
			super(context);
			this.contextRefs = contextRefs;
			contextRefs.put(this, this);
		}

		@Override
		public void run() {
			// terminate() vs. clean() relies on this being safe to run multiple times, and
			// possibly concurrently
			// both AtomicReference.getAndSet() and ConcurrentHashMap.remove() are fully
			// thread safe, so the current implementation is fine
			final LocalContext lc = getAndSet(null);
			if (lc != null)
				lc.remove();
			contextRefs.remove(this);
		}
	}
}
