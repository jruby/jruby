
package org.jruby.embed.internal;

import org.jruby.RubyInstanceConfig;
import org.jruby.embed.LocalVariableBehavior;
import java.util.concurrent.atomic.AtomicReference;
import org.jruby.Ruby;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author kares
 */
public class SingletonLocalContextProviderTest {

    @Test
    public void testGetRuntime() {
        SingletonLocalContextProvider provider = newProvider();
        final Ruby providerRuntime = provider.getRuntime();
        assertEquals(true, Ruby.isGlobalRuntimeReady());
        assertEquals(Ruby.getGlobalRuntime(), providerRuntime);
    }

    @Test
    public void testGetRubyInstanceConfig() {
        if ( Ruby.isGlobalRuntimeReady() ) {
            SingletonLocalContextProvider provider = newProvider();
            assertTrue(Ruby.getGlobalRuntime().getInstanceConfig() == provider.getRubyInstanceConfig());
        }

        SingletonLocalContextProviderStub provider = new SingletonLocalContextProviderStub();
        provider.stub.isGlobalRuntimeReady = false;
        provider.stub.globalRuntimeHolder.set(null);
        assertNotNull( provider.getRubyInstanceConfig() );
        assertFalse( provider.stub.runtimeInitialized ); // make sure we do not call getRuntime()

        provider.stub.isGlobalRuntimeReady = true;
        provider.stub.globalRuntimeHolder.set(Ruby.getGlobalRuntime());
        assertNotNull( provider.getRubyInstanceConfig() );
        assertSame(Ruby.getGlobalRuntime().getInstanceConfig(), provider.getRubyInstanceConfig());

        provider = new SingletonLocalContextProviderStub();
        provider.stub.isGlobalRuntimeReady = false;
        provider.stub.globalRuntimeHolder.set(null);

        final RubyInstanceConfig config = provider.getRubyInstanceConfig();
        assertNotNull( config );
        assertNull( provider.stub.globalRuntimeHolder.get() );

        //provider.getRuntime();
        assertNotNull( provider.getRuntime() );
        assertSame( config, provider.getRubyInstanceConfig() );
        assertNotNull( provider.stub.globalRuntimeHolder.get() );
        assertSame( config, provider.stub.globalRuntimeHolder.get().getInstanceConfig() );
    }

    @Test
    public void testIsRuntimeInitialized() {
        SingletonLocalContextProvider provider = newProvider();
        boolean result = provider.isRuntimeInitialized();
        if ( Ruby.isGlobalRuntimeReady() ) {
            assertTrue(result);
        }
        else assertFalse(result);
    }

    @Test
    public void testTerminate() {
        SingletonLocalContextProvider provider = newProvider();
        //try {
        provider.terminate();
        //}
        //catch (Exception e) {
        //    fail();
        //}
    }

    private SingletonLocalContextProvider newProvider() {
        return new SingletonLocalContextProvider(LocalVariableBehavior.TRANSIENT, true);
    }

    private static class SingletonLocalContextProviderStub extends SingletonLocalContextProvider {
		GlobalContextStub stub = new GlobalContextStub();

		private SingletonLocalContextProviderStub() {
			super(LocalVariableBehavior.TRANSIENT, true);
		}

		@Override
		protected LocalContext getLocalContext() {
			return stub;
		}
	}
}