/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.embed.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import org.jruby.RubyInstanceConfig;
import org.jruby.embed.LocalVariableBehavior;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ThreadLocalContextTest {
	private ThreadLocalContext factory;
	private Semaphore threadReady;
	private Semaphore threadCanExit;
	private AtomicReference<Canary> canary;
	private AtomicReference<PhantomReference<LocalContext>> ref;
	private ReferenceQueue<LocalContext> q;

	@Before
	public void init() {
		final RubyInstanceConfig config = new RubyInstanceConfig();
		factory = new ThreadLocalContext(() -> new DummyLocalContext(config));
		threadReady = new Semaphore(0);
		threadCanExit = new Semaphore(0);
		canary = new AtomicReference<>();
		ref = new AtomicReference<>();
		q = new ReferenceQueue<>();
	}

	@After
	public void terminate() {
		factory.terminate(); // get rid of cleaner thread
	}

	/** Simply checks that different threads get different LocalContexts. */
	@Test
	public void testContextPerThread() throws InterruptedException {
		// there is only one LocalContext per thread
		final LocalContext a = factory.get();
		assertNotNull(a);
		assertEquals(a, factory.get());

		final Cage thread = new Cage(() -> {
			final LocalContext c = factory.get();
			// this thread also gets only one LocalContext
			assertNotNull(c);
			assertEquals(c, factory.get());
			// ...but it's different from the main thread's context
			assertNotEquals(a, c);
		});
		thread.start();
		thread.checkAndJoin();
	}

	/**
	 * Checks background cleanup when a thread has terminated, but
	 * {@link ThreadLocalContext#terminate()} hasn't been called yet.
	 */
	@Test
	public void testBackgroundCleanup() throws InterruptedException {
		Cage thread = new Cage(() -> {
			createCanary();
			waitForMainThread();
		});
		thread.start();

		try {
			checkSpuriousTermination();

			// terminate the thread and wait for background cleanup. keep poking the GC to
			// speed things up
			// this test can in theory fail spuriously because the GC is free to ignore
			// System.gc() indefinitely, and also to delay cleaning up the LocalContext.
			// in practice, the first System.gc() ends up collecting stuff and
			// LocalContext.remove() gets called.
			threadCanExit.release();
			thread.checkAndJoin();

			// at this point, remove() must have been called, but the LocalContext must also
			// have been garbage-collected. it isn't acceptable for it to stay reachable and
			// thus leak memory once the thread has terminated.
			checkCleanup();
			checkGarbageCollection();
		} finally {
			// always get rid of thread. better be safe than sorry
			thread.interrupt();
			thread.join();
		}
	}

	/**
	 * Checks eager cleanup when {@link ThreadLocalContext#terminate()} is called
	 * while there are still threads (potentially) using their LocalContext.
	 */
	@Test
	public void testTerminate() throws InterruptedException {
		// same procedure as testBackgroundCleanup() but it explicitly calls terminate()
		// while the thread is still active
		Cage thread = new Cage(() -> {
			createCanary();
			waitForMainThread();
			// at this point the LocalContext should have become invalid
			assertFalse("LocalContext not removed", canary.get().isAlive());
		});
		thread.start();

		try {
			checkSpuriousTermination();

			// eagerly terminate the ThreadLocalContext with the thread still alive. this
			// must cause the LocalContext to be cleaned up immediately. (it doesn't
			// actually get garbage-collected because it is still referenced by the
			// ThreadLocal.)
			factory.terminate();
			checkCleanup();

			// now release the thread, which then verifies that its LocalContext has become
			// invalid before terminating. now the thread is gone, the LocalContext also has
			// to be garbage-collected.
			threadCanExit.release();
			thread.checkAndJoin();
			checkGarbageCollection();
		} finally {
			// always get rid of thread. better be safe than sorry
			thread.interrupt();
			thread.join();
		}
	}

	private void createCanary() {
		final DummyLocalContext ctx = (DummyLocalContext) factory.get();
		assertNotNull(ctx);
		canary.set(ctx.getCanary());
		ref.set(new PhantomReference<>(ctx, q));
		threadReady.release();
	}

	private void waitForMainThread() {
		try { // wait until we can exit
			threadCanExit.acquire();
		} catch (InterruptedException e) {
			fail("interrupted");
		}
	}

	private void checkSpuriousTermination() throws InterruptedException {
		threadReady.acquire(); // wait for thread to create the reference

		// thread is still running and ThreadLocalContext hasn't been terminated either,
		// so LocalContext must stick around through GC cycles
		System.gc();
		Thread.sleep(100);
		assertTrue("LocalContext removed prematurely", canary.get().isAlive());
	}

	private void checkCleanup() throws InterruptedException {
		// this code keeps trying for 10s: we'd rather have the test take longer than
		// fail spuriously...
		for (int i = 0; i < 100 && canary.get().isAlive(); i++) {
			System.gc();
			Thread.sleep(100);
		}
		assertFalse("LocalContext not removed", canary.get().isAlive());
	}

	private void checkGarbageCollection() throws InterruptedException {
		// this code keeps trying for 10s: we'd rather have the test take longer than
		// fail spuriously...
		Reference<?> clearedRef = null;
		for (int i = 0; i < 100 && clearedRef == null; i++) {
			System.gc();
			clearedRef = q.remove(100);
		}
		assertNotNull("LocalContext not garbage-collected", clearedRef);
		assertEquals("some other object was garbage-collected?!", ref.get(), clearedRef);
		assertTrue("reference wasn't cleared?!", ref.get().refersTo(null));
	}

	private class Cage extends Thread {
		private AssertionError exception;

		public Cage(final Runnable run) {
			super(run);
		}

		@Override
		public void run() {
			try {
				super.run();
			} catch (final AssertionError e) {
				exception = e;
			}
		}

		private void checkAndJoin() throws InterruptedException {
			join();
			if (exception != null)
				throw exception;
		}
	}

	private static class Canary {
		private volatile boolean alive = true;

		private void kill() {
			alive = false;
		}

		public boolean isAlive() {
			return alive;
		}
	}

	private class DummyLocalContext extends LocalContext {
		private Canary canary;

		public DummyLocalContext(RubyInstanceConfig config) {
			super(config, LocalVariableBehavior.TRANSIENT, false);
			this.canary = new Canary();
		}

		public Canary getCanary() {
			return canary;
		}

		@Override
		public void remove() {
			canary.kill();
			super.remove();
		}
	}
}
