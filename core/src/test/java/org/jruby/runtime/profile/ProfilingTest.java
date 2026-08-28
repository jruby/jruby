package org.jruby.runtime.profile;

import junit.framework.TestCase;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.load.LoadService;
import org.jruby.util.JRubyThreadContextTest;

/**
 * @author Andre Kullmann
 */
public class ProfilingTest extends TestCase {

    public ProfilingTest(final String testName) {
        super(testName);
    }


    /**
     * Tests the {@link org.jruby.runtime.profile.ProfilingServiceLookup} too
     */
    public void testNoProfilingServerAvailableIfProfilingIsDisabled() {
        RubyInstanceConfig config = Ruby.getGlobalRuntime().getInstanceConfig();

        RubyInstanceConfig configOne = new RubyInstanceConfig(config);
        configOne.setProfilingMode(RubyInstanceConfig.ProfilingMode.OFF );

        Ruby ruby = Ruby.newInstance( configOne );

        assertNull(ruby.getProfilingService());

    }
    /**
     * Tests the {@link org.jruby.runtime.profile.ProfilingServiceLookup} too
     */
    public void testProfilingServiceLookupWorks() {
        try {
            RubyInstanceConfig config = Ruby.getGlobalRuntime().getInstanceConfig();

            RubyInstanceConfig configOne = new RubyInstanceConfig(config);

            configOne.setProfilingService( TestProfilingService.class.getName() );
            configOne.setProfilingMode(RubyInstanceConfig.ProfilingMode.SERVICE);
            Ruby ruby = Ruby.newInstance( configOne );

            assertNotNull( ruby.getProfilingService() );
            assertTrue(ruby.getProfilingService() instanceof TestProfilingService);
        } catch( RaiseException e ) {
            //e.printStackTrace();
            // TODO how to mock org.jruby.exceptions.RaiseException: (LoadError) no such file to load -- jruby/profiler/shutdown_hook
        }
    }

    /**
     * Tests if the profiling service can be configured as java property
     */
    public void testProfilingServiceAsJavaProperty() {

        // java -Djruby.cli.profiling.mode=SERVICE -Djruby.cli.profiling.service=org.jruby.runtime.profile.builtin.BuiltinProfilingService -cp jruby.jar org.jruby.main.Main 

        // TODO how to test it ??
    }
}
