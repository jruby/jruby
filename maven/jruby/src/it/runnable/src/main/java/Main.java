public class Main extends org.jruby.Main {

    public static void main(String[] args) {
	Main main = new Main();
        
        try {
            org.jruby.Main.Status status = main.run(args);
            if (status.isExit()) {
                System.exit(status.getStatus());
            }
        } catch (org.jruby.exceptions.RaiseException rj) {
            System.exit(-1);//TODO handleRaiseException(rj));
        } catch (Throwable t) {
            // print out as a nice Ruby backtrace
            System.err.println(org.jruby.runtime.ThreadContext.createRawBacktraceStringFromThrowable(t));
            while ((t = t.getCause()) != null) {
                System.err.println("Caused by:");
                System.err.println(org.jruby.runtime.ThreadContext.createRawBacktraceStringFromThrowable(t));
            }
            System.exit(1);
        }
    }

    public Main() {
	this(new org.jruby.RubyInstanceConfig());
    }

    private Main(org.jruby.RubyInstanceConfig config) {
	super(config);
        config.setHardExit(true);
	config.setCurrentDirectory( "uri:classloader://" );
	config.setJRubyHome( "uri:classloader://META-INF/jruby.home" );
	config.setLoadPaths( java.util.Arrays.asList("uri:classloader:/") );
	java.util.Map env = new java.util.HashMap( System.getenv() );
	env.put( "JARS_HOME", "uri:classloader:/" );
	// needed for jruby version before 1.7.19
	env.put( "BUNDLE_DISABLE_SHARED_GEMS", "true" );
	config.setEnvironment( env );
    }
}
