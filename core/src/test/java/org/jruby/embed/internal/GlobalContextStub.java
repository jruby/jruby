package org.jruby.embed.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.jruby.Ruby;
import org.jruby.embed.LocalVariableBehavior;

public class GlobalContextStub extends GlobalContext {
	AtomicReference<Ruby> globalRuntimeHolder = new AtomicReference<Ruby>();
	boolean runtimeInitialized;
	Boolean isGlobalRuntimeReady;

	public GlobalContextStub() {
		super(AbstractLocalContextProvider.getGlobalRuntimeConfigOrNew(), LocalVariableBehavior.TRANSIENT, true);
	}

	@Override
	public Ruby getRuntime() {
		runtimeInitialized = true;
		return super.getRuntime();
	}

	@Override
	boolean isInitialized() {
		if (isGlobalRuntimeReady != null)
			return isGlobalRuntimeReady.booleanValue();
		return super.isInitialized();
	}

	@Override
	Ruby getGlobalRuntime() {
		if (isGlobalRuntimeReady)
			return globalRuntimeHolder.get();

		final Ruby globalRuntime = super.getGlobalRuntime();
		isGlobalRuntimeReady = true;
		globalRuntimeHolder.set(globalRuntime);
		return globalRuntime;
	}
}
