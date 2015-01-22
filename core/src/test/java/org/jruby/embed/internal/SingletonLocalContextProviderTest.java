
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

        SingletonLocalContextProviderStub provider = newProviderStub();
        provider.isGlobalRuntimeReady = false;
        assertNotNull( provider.getRubyInstanceConfig() );
        assertFalse( provider.runtimeInitialized ); // make sure we do not call getRuntime()

        provider.isGlobalRuntimeReady = true;
        assertNotNull( provider.getRubyInstanceConfig() );
        assertSame(Ruby.getGlobalRuntime().getInstanceConfig(), provider.getRubyInstanceConfig());

        final AtomicReference<Ruby> globalRuntimeHolder = new AtomicReference<Ruby>();
        provider = new SingletonLocalContextProviderStub() {

            {
                isGlobalRuntimeReady = false;
            }

            @Override
            Ruby getGlobalRuntime(AbstractLocalContextProvider provider) {
                if ( isGlobalRuntimeReady ) return globalRuntimeHolder.get();

                final Ruby globalRuntime = super.getGlobalRuntime(provider);
                isGlobalRuntimeReady = true;
                globalRuntimeHolder.set(globalRuntime);
                return globalRuntime;
            }

        };

        final RubyInstanceConfig config = provider.getRubyInstanceConfig();
        assertNotNull( config );
        assertNull( globalRuntimeHolder.get() );

        provider.getRuntime();
        assertNotNull( provider.getRuntime() );
        assertSame( config, provider.getRubyInstanceConfig() );
        assertNotNull( globalRuntimeHolder.get() );
        assertSame( config, globalRuntimeHolder.get().getInstanceConfig() );
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

    private SingletonLocalContextProviderStub newProviderStub() {
        return new SingletonLocalContextProviderStub();
    }

    private static class SingletonLocalContextProviderStub extends SingletonLocalContextProvider {

        Boolean isGlobalRuntimeReady = null;

        private SingletonLocalContextProviderStub() {
            super( LocalVariableBehavior.TRANSIENT, true );
        }

        SingletonLocalContextProviderStub(LocalVariableBehavior behavior) {
            super( behavior );
        }

        SingletonLocalContextProviderStub(LocalVariableBehavior behavior, boolean lazy) {
            super( behavior, lazy );
        }

        boolean runtimeInitialized = false;

        @Override
        public Ruby getRuntime() {
            runtimeInitialized = true;
            return super.getRuntime();
        }

        @Override
        boolean isGlobalRuntimeReady() {
            if ( isGlobalRuntimeReady != null ) {
                return isGlobalRuntimeReady.booleanValue();
            }
            return super.isGlobalRuntimeReady();
        }

        //@Override
        //Ruby getGlobalRuntime(AbstractLocalContextProvider provider) {
        //    if ( globalRuntime != null ) return globalRuntime;
        //    return super.getGlobalRuntime(provider);
        //}

    }

}